package me.niko302.silktouchspawners.commands;

import me.niko302.silktouchspawners.SilktouchSpawners;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SilktouchSpawnersCommand implements TabExecutor {

    private final SilktouchSpawners plugin;

    public SilktouchSpawnersCommand(SilktouchSpawners plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            displayHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            handleGiveCommand(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("givecustomitem")) {
            handleGiveCustomItemCommand(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("reloadconfig")) {
            if (!sender.hasPermission("silktouchspawners.reload")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }

            plugin.getConfigManager().reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
            return true;
        }

        displayHelp(sender);
        return true;
    }

    private void displayHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Available commands:");
        sender.sendMessage(ChatColor.YELLOW + "/silktouchspawners give <player> <spawnertype> [amount]");
        sender.sendMessage(ChatColor.YELLOW + "/silktouchspawners givecustomitem <player> <itemname> [amount]");
        sender.sendMessage(ChatColor.YELLOW + "/silktouchspawners reloadconfig");
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("silktouchspawners.give")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

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

            String entityName =
                    plugin.getConfigManager().getSpawnerNameFormatOverrides().getOrDefault(entityType.name(), entityType.name());
            String formattedName = plugin.getConfigManager().getSpawnerNameFormat().replace("{mobtype}", entityName);

            spawnerMeta.setDisplayName(formattedName);

            List<String> lore = new ArrayList<>();
            for (String line : plugin.getConfigManager().getSpawnerLore()) {
                lore.add(line.replace("{mobtype}", entityName));
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
                return;
            }

            targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), spawnerItem);
            targetPlayer.sendMessage(plugin.getConfigManager().getSpawnerDropMessage());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid spawner type specified.");
        }
    }

    private void handleGiveCustomItemCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("silktouchspawners.give")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

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

        final int finalAmount = amount;

        plugin.getConfigManager().getCustomItem(itemName).ifPresentOrElse(customItem -> {
            customItem.setAmount(finalAmount);

            if (targetPlayer.getInventory().addItem(customItem).isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + "Gave " + finalAmount + " " + itemName + " to " + targetPlayer.getName() + ".");
                return;
            }

            targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), customItem);
            targetPlayer.sendMessage(plugin.getConfigManager().getSpawnerDropMessage());
        }, () -> sender.sendMessage(ChatColor.RED + "Custom item not found."));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("give", "givecustomitem", "reloadconfig"), new ArrayList<>());
        }

        if (args.length == 2) {
            if (!args[0].equalsIgnoreCase("give") && !args[0].equalsIgnoreCase("givecustomitem")) {
                return new ArrayList<>();
            }

            return StringUtil.copyPartialMatches(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toSet()), new ArrayList<>());
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                return StringUtil.copyPartialMatches(args[2], Arrays.stream(EntityType.values()).map(Enum::name).collect(Collectors.toSet()), new ArrayList<>());
            }

            if (args[0].equalsIgnoreCase("givecustomitem")) {
                return StringUtil.copyPartialMatches(args[2], plugin.getConfigManager().getCustomItems().keySet(), new ArrayList<>());
            }

            return new ArrayList<>();
        }

        if (args.length == 4) {
            if (!args[0].equalsIgnoreCase("give") && !args[0].equalsIgnoreCase("givecustomitem")) {
                return new ArrayList<>();
            }

            return Arrays.asList("1", "2", "3", "4", "5");
        }

        return new ArrayList<>();
    }

}