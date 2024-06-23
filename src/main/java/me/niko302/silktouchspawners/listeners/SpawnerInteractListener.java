package me.niko302.silktouchspawners.listeners;

import me.niko302.silktouchspawners.silktouchspawners;
import me.niko302.silktouchspawners.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SpawnerInteractListener implements Listener {

    private final silktouchspawners plugin;

    public SpawnerInteractListener(silktouchspawners plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Ensure the event is triggered by the main hand only
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.NETHER_STAR && item.hasItemMeta() && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().contains(ConfigManager.translateColorCodes(plugin.getConfigManager().getRequiredLoreOneTimeUse()))) {
                Block block = event.getClickedBlock();
                if (block != null && block.getType() == Material.SPAWNER) {
                    event.setCancelled(true);

                    // Trigger a BlockBreakEvent to ensure the player can break the block
                    BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
                    plugin.getServer().getPluginManager().callEvent(blockBreakEvent);

                    if (!blockBreakEvent.isCancelled()) {
                        // Call the handleSpawnerBreak method to handle the spawner drop logic
                        plugin.getBlockBreakListener().handleSpawnerBreak(blockBreakEvent, block, player, item, true);

                        // Remove only one Nether Star from the player's hand
                        if (item.getAmount() > 1) {
                            item.setAmount(item.getAmount() - 1);
                        } else {
                            player.getInventory().setItemInMainHand(null);
                        }
                    }
                }
            }
        } else if (item.getType().name().endsWith("_SPAWN_EGG")) {
            if (!plugin.getConfig().getBoolean("allow-changing-spawners-with-mob-eggs-globally", false) &&
                    !player.hasPermission("silktouchspawners.changespawner")) {
                event.setCancelled(true);
                String message = plugin.getConfigManager().getNoPermissionChangeSpawner();
                if (message != null && !message.isEmpty()) {
                    player.sendMessage(ConfigManager.translateColorCodes(message));
                }
            }
        }
    }
}