package me.niko302.silktouchspawners.commands;

import me.niko302.silktouchspawners.silktouchspawners;
import me.niko302.silktouchspawners.config.ConfigManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class Commands implements CommandExecutor {

    private final silktouchspawners plugin;

    public Commands(silktouchspawners plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            displayHelp(sender);
            return true;
        }

        if (command.getName().equalsIgnoreCase("silktouchspawners")) {
            if (args[0].equalsIgnoreCase("give")) {
                handleGiveCommand(sender, args);
                return true;
            } else if (args[0].equalsIgnoreCase("givecustomitem")) {
                handleGiveCustomItemCommand(sender, args);
                return true;
            } else if (args[0].equalsIgnoreCase("reloadconfig")) {
                handleReloadConfigCommand(sender);
                return true;
            } else {
                displayHelp(sender);
                return true;
            }
        }
        return false;
    }

    private void displayHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Available commands:");
        sender.sendMessage(ChatColor.YELLOW + "/silktouchspawners give <player> <spawnertype> [amount]");
        sender.sendMessage(ChatColor.YELLOW + "/silktouchspawners givecustomitem <player> <itemname> [amount]");
        sender.sendMessage(ChatColor.YELLOW + "/silktouchspawners reloadconfig");
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (sender.hasPermission("silktouchspawners.give")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /silktouchspawners give <player> <spawnertype> [amount]");
                return;
            }

            Player targetPlayer = Bukkit.getPlayer(args[1]);
            String spawnerType = args[2].toUpperCase();
            int amount = 1;

            if (args.length == 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount specified.");
                    return;
                }
            }

            if (targetPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return;
            }

            try {
                EntityType entityType = EntityType.valueOf(spawnerType);

                ItemStack spawnerItem = new ItemStack(Material.SPAWNER, amount);
                ItemMeta spawnerMeta = spawnerItem.getItemMeta();

                String formattedName = formatSpawnerName(plugin.getConfigManager().getSpawnerNameFormat(), entityType.name());
                spawnerMeta.setDisplayName(formattedName);

                List<String> lore = new ArrayList<>();
                for (String line : plugin.getConfigManager().getSpawnerLore()) {
                    lore.add(ConfigManager.translateColorCodes(line.replace("{mobtype}", entityType.name())));
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

                if (targetPlayer.getInventory().addItem(spawnerItem).isEmpty()) {
                    sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " " + spawnerType + " spawner(s) to " + targetPlayer.getName() + ".");
                } else {
                    targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), spawnerItem);
                    targetPlayer.sendMessage(ConfigManager.translateColorCodes(plugin.getConfigManager().getSpawnerDropMessage()));
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Invalid spawner type specified.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
        }
    }

    private void handleGiveCustomItemCommand(CommandSender sender, String[] args) {
        if (sender.hasPermission("silktouchspawners.give")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /silktouchspawners givecustomitem <player> <itemname> [amount]");
                return;
            }

            Player targetPlayer = Bukkit.getPlayer(args[1]);
            String itemName = args[2];
            int amount = 1;

            if (args.length == 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount specified.");
                    return;
                }
            }

            if (targetPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return;
            }

            ItemStack customItem = plugin.getConfigManager().getCustomItem(itemName);
            if (customItem == null) {
                sender.sendMessage(ChatColor.RED + "Custom item not found.");
                return;
            }

            customItem.setAmount(amount);

            if (targetPlayer.getInventory().addItem(customItem).isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " " + itemName + " to " + targetPlayer.getName() + ".");
            } else {
                targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), customItem);
                targetPlayer.sendMessage(ConfigManager.translateColorCodes(plugin.getConfigManager().getSpawnerDropMessage()));
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
        }
    }

    private void handleReloadConfigCommand(CommandSender sender) {
        if (sender.hasPermission("silktouchspawners.reload")) {
            plugin.getConfigManager().reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
        }
    }

    private String formatSpawnerName(String format, String mobType) {
        String name = format.replace("{mobtype}", mobType);
        return ConfigManager.translateColorCodes(name);
    }
}