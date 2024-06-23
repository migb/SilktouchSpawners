package me.niko302.silktouchspawners.commands;

import lombok.RequiredArgsConstructor;
import me.niko302.silktouchspawners.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CustomItemCommand implements TabExecutor {

    private final ConfigManager configManager;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("Usage: /givecustomitem <player> <itemname>");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);

        if (targetPlayer == null) {
            sender.sendMessage("Player not found.");
            return true;
        }

        configManager.getCustomItem(args[1]).ifPresentOrElse(customItem -> {
            if (targetPlayer.getInventory().addItem(customItem).isEmpty()) {
                sender.sendMessage("Gave custom item to " + targetPlayer.getName() + ".");
                return;
            }

            targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), customItem);
            targetPlayer.sendMessage(configManager.getSpawnerDropMessage());
        }, () -> sender.sendMessage("Custom item not found."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toSet()), new ArrayList<>());
        }

        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], configManager.getCustomItems().keySet(), new ArrayList<>());
        }

        return new ArrayList<>();
    }
}