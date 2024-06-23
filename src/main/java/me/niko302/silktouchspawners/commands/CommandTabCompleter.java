package me.niko302.silktouchspawners.commands;

import me.niko302.silktouchspawners.silktouchspawners;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandTabCompleter implements TabCompleter {

    private final silktouchspawners plugin;

    public CommandTabCompleter(silktouchspawners plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("silktouchspawners") || alias.equalsIgnoreCase("sts")) {
            if (args.length == 1) {
                // /silktouchspawners <subcommand>
                return Arrays.asList("give", "givecustomitem", "reloadconfig");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("givecustomitem")) {
                    // /silktouchspawners give <player> or /silktouchspawners givecustomitem <player>
                    return Bukkit.getOnlinePlayers().stream()
                            .map(player -> player.getName())
                            .collect(Collectors.toList());
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("give")) {
                    // /silktouchspawners give <player> <spawnertype>
                    return Arrays.stream(EntityType.values())
                            .filter(EntityType::isAlive)
                            .map(Enum::name)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());
                } else if (args[0].equalsIgnoreCase("givecustomitem")) {
                    // /silktouchspawners givecustomitem <player> <itemname>
                    return plugin.getConfigManager().getCustomItemNames().stream()
                            .collect(Collectors.toList());
                }
            } else if (args.length == 4) {
                if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("givecustomitem")) {
                    // /silktouchspawners give <player> <spawnertype> <amount> or /silktouchspawners givecustomitem <player> <itemname> <amount>
                    return Arrays.asList("1", "2", "3", "4", "5");
                }
            }
        }
        return Collections.emptyList();
    }
}
