package me.niko302.silktouchspawners.commands;

import me.niko302.silktouchspawners.silktouchspawners;
import me.niko302.silktouchspawners.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomItemCommand implements CommandExecutor {

    private final silktouchspawners plugin;

    public CustomItemCommand(silktouchspawners plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("Usage: /givecustomitem <player> <itemname>");
            return false;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage("Player not found.");
            return false;
        }

        ItemStack customItem = plugin.getConfigManager().getCustomItem(args[1]);
        if (customItem == null) {
            sender.sendMessage("Custom item not found.");
            return false;
        }

        if (targetPlayer.getInventory().addItem(customItem).isEmpty()) {
            sender.sendMessage("Gave custom item to " + targetPlayer.getName() + ".");
        } else {
            targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), customItem);
            targetPlayer.sendMessage(ConfigManager.translateColorCodes(plugin.getConfigManager().getSpawnerDropMessage()));
        }

        return true;
    }
}