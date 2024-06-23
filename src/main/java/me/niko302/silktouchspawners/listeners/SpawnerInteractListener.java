package me.niko302.silktouchspawners.listeners;

import me.niko302.silktouchspawners.silktouchspawners;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnerInteractListener implements Listener {

    private final silktouchspawners plugin;

    public SpawnerInteractListener(silktouchspawners plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().name().endsWith("_SPAWN_EGG")) {
            if (!plugin.getConfig().getBoolean("allow-changing-spawners-with-mob-eggs-globally", false) &&
                    !player.hasPermission("silktouchspawners.changespawner")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission-change-spawner")));
            }
        }
    }
}