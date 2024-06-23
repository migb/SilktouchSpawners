package me.niko302.silktouchspawners.listeners;

import me.niko302.silktouchspawners.config.ConfigManager;
import me.niko302.silktouchspawners.silktouchspawners;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BlockBreakListener implements Listener {
    private final silktouchspawners plugin;
    private final Set<UUID> warnedPlayers = new HashSet<>();

    public BlockBreakListener(silktouchspawners plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Check if the event was cancelled by another plugin
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (block.getType() == Material.SPAWNER && player.hasPermission("silktouchspawners.mine")) {
            ItemMeta meta = tool.getItemMeta();
            boolean hasSilkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH);
            boolean hasRequiredLore = meta != null && meta.hasLore() && meta.getLore().contains(plugin.getConfigManager().getRequiredLore());

            if (hasSilkTouch || hasRequiredLore) {
                handleSpawnerBreak(event, block, player, tool);
            } else {
                if (warnedPlayers.contains(player.getUniqueId())) {
                    event.setCancelled(true); // Cancel the event to prevent breaking the spawner
                    player.sendMessage(ConfigManager.translateColorCodes(plugin.getConfigManager().getSpawnerBreakFailureMessage()));
                } else {
                    event.setCancelled(true); // Cancel the event to prevent breaking the spawner
                    player.sendMessage(ConfigManager.translateColorCodes(plugin.getConfigManager().getNoPermissionBreakSpawner()));
                    player.sendMessage(ConfigManager.translateColorCodes(plugin.getConfigManager().getNoPermissionWarning()));
                    warnedPlayers.add(player.getUniqueId());
                }
            }
        }
    }

    private void handleSpawnerBreak(BlockBreakEvent event, Block block, Player player, ItemStack tool) {
        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        EntityType entityType = spawner.getSpawnedType();

        if (entityType == null || entityType == EntityType.UNKNOWN) {
            entityType = EntityType.PIG;
        }

        ItemStack spawnerItem = new ItemStack(Material.SPAWNER);
        ItemMeta spawnerMeta = spawnerItem.getItemMeta();

        String formattedName = formatSpawnerName(plugin.getConfigManager().getSpawnerNameFormat(), entityType.name());
        spawnerMeta.setDisplayName(formattedName);

        // Set custom lore
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfigManager().getSpawnerLore()) {
            lore.add(ConfigManager.translateColorCodes(line.replace("{mobtype}", entityType.name())));
        }
        spawnerMeta.setLore(lore);

        // Add item flag to hide default Minecraft lore
        spawnerMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        // Save the spawner type to the item's persistent data container
        NamespacedKey key = new NamespacedKey(plugin, "spawnerType");
        spawnerMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, entityType.name());

        spawnerItem.setItemMeta(spawnerMeta);

        boolean spawnerToInventory = plugin.getConfig().getBoolean("spawner-to-inventory-on-drop", true);
        if (spawnerToInventory && player.getInventory().addItem(spawnerItem).isEmpty()) {
            player.sendMessage(ConfigManager.translateColorCodes(plugin.getConfigManager().getSpawnerBreakSuccessMessage(entityType.name())));
        } else {
            block.getWorld().dropItemNaturally(block.getLocation(), spawnerItem);
            player.sendMessage(ConfigManager.translateColorCodes(plugin.getConfigManager().getSpawnerDropMessage()));
        }

        event.setExpToDrop(0);
        warnedPlayers.remove(player.getUniqueId()); // Reset warning status after successful break
    }

    private String formatSpawnerName(String format, String mobType) {
        String name = format.replace("{mobtype}", mobType);
        return ConfigManager.translateColorCodes(name);
    }
}