package me.niko302.silktouchspawners.listeners;

import me.niko302.silktouchspawners.config.ConfigManager;
import me.niko302.silktouchspawners.silktouchspawners;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class BlockPlaceListener implements Listener {

    private final silktouchspawners plugin;

    public BlockPlaceListener(silktouchspawners plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // Check if the event was cancelled by another plugin
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlockPlaced();
        ItemStack item = event.getItemInHand();

        if (block.getType() == Material.SPAWNER && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
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
            if (message != null && !message.isEmpty()) {
                event.getPlayer().sendMessage(ConfigManager.translateColorCodes(message));
            }
        }
    }
}