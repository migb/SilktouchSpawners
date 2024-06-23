package me.niko302.silktouchspawners.listeners;

import me.niko302.silktouchspawners.SilktouchSpawners;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class SpawnerListener implements Listener {

    private final SilktouchSpawners plugin;
    private final Set<UUID> warnedPlayers = new HashSet<>();

    public SpawnerListener(SilktouchSpawners plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) {
            return;
        }

        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        EntityType entityType = spawner.getSpawnedType();

        if (entityType == null || entityType == EntityType.UNKNOWN) {
            entityType = EntityType.PIG;
        }

        ItemMeta meta = tool.getItemMeta();

        if (meta == null) {
            return;
        }

        // Check if the event is triggered by one-time use item
        if (meta.hasLore() && meta.getLore().contains(plugin.getConfigManager().getRequiredLoreOneTimeUse())) {
            return;
        }

        if (player.hasPermission("silktouchspawners.mine.all") || player.hasPermission("silktouchspawners.mine." + entityType.name().toLowerCase())) {
            boolean hasSilkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH);
            boolean hasRequiredLore = meta.hasLore() && meta.getLore().contains(plugin.getConfigManager().getRequiredLore());

            if ((plugin.getConfigManager().isRequireSilkTouch() && hasSilkTouch) || hasRequiredLore) {
                handleSpawnerBreak(event, block, player, false);
                return;
            }
        }

        if (!warnedPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getNoPermissionWarning());
            warnedPlayers.add(player.getUniqueId());
            return;
        }

        event.setCancelled(false);
        warnedPlayers.remove(player.getUniqueId());
        player.sendMessage(plugin.getConfigManager().getSpawnerBreakFailureMessage());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) {
            return;
        }

        Block block = event.getBlockPlaced();
        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "spawnerType");
        String spawnerType = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        EntityType entityType = EntityType.PIG; // Default to PIG

        if (spawnerType != null) {
            try {
                entityType = EntityType.valueOf(spawnerType);
            } catch (IllegalArgumentException e) {
                // If the spawnerType is invalid, default to PIG
            }
        }

        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        spawner.setSpawnedType(entityType);
        spawner.update();

        String message = plugin.getConfigManager().getSpawnerPlaceSuccessMessage(entityType.name());

        if (message == null || message.isEmpty()) {
            return;
        }

        event.getPlayer().sendMessage(message);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Ensure the event is triggered by the main hand only
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();

        if (item.getType().name().endsWith("_SPAWN_EGG")) {
            if (plugin.getConfig().getBoolean("allow-changing-spawners-with-mob-eggs-globally", false) ||
                    player.hasPermission("silktouchspawners.changespawner")) {
                return;
            }

            event.setCancelled(true);

            String message = plugin.getConfigManager().getNoPermissionChangeSpawner();

            if (message == null || !message.isEmpty()) {
                return;
            }

            player.sendMessage(message);
            return;
        }

        if (item.getType() != Material.NETHER_STAR || meta == null ||
                (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        if (!meta.hasLore() || !meta.getLore().contains(plugin.getConfigManager().getRequiredLoreOneTimeUse())) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null || block.getType() != Material.SPAWNER) {
            return;
        }

        // Trigger a BlockBreakEvent to ensure the player can break the block
        BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
        plugin.getServer().getPluginManager().callEvent(blockBreakEvent);

        if (!blockBreakEvent.isCancelled()) {
            event.setCancelled(true);


            // Remove only one Nether Star from the player's hand
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Call the handleSpawnerBreak method to handle the spawner drop logic
                handleSpawnerBreak(blockBreakEvent, block, player, true);

                item.setAmount(item.getAmount() - 1);
            });
        }
    }

    public void handleSpawnerBreak(BlockBreakEvent event, Block block, Player player, boolean isOneTimeUse) {
        if (!(block.getState() instanceof CreatureSpawner)) {
            return;
        }

        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        EntityType entityType = spawner.getSpawnedType();

        if (entityType == null || entityType == EntityType.UNKNOWN) {
            entityType = EntityType.PIG;
        }

        ItemStack spawnerItem = new ItemStack(Material.SPAWNER);
        ItemMeta spawnerMeta = spawnerItem.getItemMeta();

        if (spawnerMeta == null) {
            return;
        }

        String formattedName = plugin.getConfigManager().getSpawnerNameFormat().replace("{mobtype}", entityType.name());
        spawnerMeta.setDisplayName(formattedName);

        // Set custom lore
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfigManager().getSpawnerLore()) {
            lore.add(line.replace("{mobtype}", entityType.name()));
        }

        spawnerMeta.setLore(lore);

        // Hide default Minecraft lore
        try {
            spawnerMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        } catch (NoSuchFieldError e) {
            // Clear default Minecraft lore for older versions
            if (spawnerMeta.getLore() == null) {
                spawnerMeta.setLore(new ArrayList<>());
            }
        }

        // Save the spawner type to the item's persistent data container
        NamespacedKey key = new NamespacedKey(plugin, "spawnerType");
        spawnerMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, entityType.name());
        spawnerItem.setItemMeta(spawnerMeta);

        boolean spawnerToInventory = plugin.getConfig().getBoolean("spawner-to-inventory-on-drop", true);

        if (spawnerToInventory && player.getInventory().addItem(spawnerItem).isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getSpawnerBreakSuccessMessage(entityType.name()));
        } else {
            block.getWorld().dropItemNaturally(block.getLocation(), spawnerItem);
            player.sendMessage(plugin.getConfigManager().getSpawnerDropMessage());
        }

        event.setExpToDrop(0);
        warnedPlayers.remove(player.getUniqueId()); // Reset warning status after successful break

        // Manually break the block for one-time use items
        if (!isOneTimeUse) {
            return;
        }

        block.setType(Material.AIR);
    }

}