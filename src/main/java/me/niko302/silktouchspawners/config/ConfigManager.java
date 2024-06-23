package me.niko302.silktouchspawners.config;

import me.niko302.silktouchspawners.silktouchspawners;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final silktouchspawners plugin;
    private FileConfiguration config;
    private String spawnerNameFormat;
    private List<String> spawnerLore;
    private String requiredLore;
    private String noPermissionBreakSpawner;
    private String noPermissionWarning;
    private String spawnerBreakSuccessMessage;
    private String spawnerPlaceSuccessMessage;
    private String spawnerDropMessage;
    private String spawnerBreakFailureMessage;
    private String noPermissionChangeSpawner;
    private boolean allowChangingSpawnersGlobally;
    private boolean spawnerToInventoryOnDrop;
    private final Map<String, ItemStack> customItems = new HashMap<>();

    public ConfigManager(silktouchspawners plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
        loadConfig();
    }

    public void saveDefaultConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadConfig();
    }

    private void loadConfig() {
        spawnerNameFormat = translateColorCodes(config.getString("spawner-name", "&808080{mobtype} &FFFFFF Spawner"));
        spawnerLore = config.getStringList("spawner-lore");
        requiredLore = config.getString("required-lore", "Special Lore");
        noPermissionBreakSpawner = translateColorCodes(config.getString("messages.no-permission-break-spawner", "&cYou do not have permission to break this spawner."));
        noPermissionWarning = translateColorCodes(config.getString("messages.no-permission-warning", "&cIf you try to break it again without the proper requirements, the spawner will be broken and not dropped."));
        spawnerBreakSuccessMessage = translateColorCodes(config.getString("messages.spawner-break-success", "&aYou successfully mined a {mobtype} Spawner!"));
        spawnerPlaceSuccessMessage = translateColorCodes(config.getString("messages.spawner-place-success", "&aYou placed a {mobtype} Spawner!"));
        spawnerDropMessage = translateColorCodes(config.getString("messages.spawner-drop-message", "&cYour inventory is full. The spawner has been dropped on the ground."));
        spawnerBreakFailureMessage = translateColorCodes(config.getString("messages.spawner-break-failure", "&cThe spawner was broken and not dropped because you didn't have the proper requirements."));
        noPermissionChangeSpawner = translateColorCodes(config.getString("messages.no-permission-change-spawner", "&cYou do not have permission to change the type of mob."));
        allowChangingSpawnersGlobally = config.getBoolean("allow-changing-spawners-globally", true);
        spawnerToInventoryOnDrop = config.getBoolean("spawner-to-inventory-on-drop", true);

        // Load custom items
        ConfigurationSection customItemsSection = config.getConfigurationSection("custom-items");
        if (customItemsSection != null) {
            for (String key : customItemsSection.getKeys(false)) {
                ConfigurationSection itemSection = customItemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = loadCustomItem(itemSection);
                    customItems.put(key, item);
                }
            }
        }
    }

    private ItemStack loadCustomItem(ConfigurationSection section) {
        Material material = Material.valueOf(section.getString("itemtype", "DIAMOND_PICKAXE"));
        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(translateColorCodes(section.getString("name", "")));

            List<String> lore = new ArrayList<>();
            String requiredLore = getRequiredLore();
            for (String line : section.getStringList("lore")) {
                lore.add(translateColorCodes(line.replace("<required-lore>", requiredLore)));
            }
            meta.setLore(lore);

            if (section.contains("custommodeldata")) {
                meta.setCustomModelData(section.getInt("custommodeldata"));
            }

            ConfigurationSection enchantsSection = section.getConfigurationSection("enchants");
            if (enchantsSection != null) {
                for (String enchantKey : enchantsSection.getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantKey));
                    int level = enchantsSection.getInt(enchantKey);
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, level, true);
                    }
                }
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack getCustomItem(String name) {
        return customItems.get(name);
    }

    public String getSpawnerNameFormat() {
        return spawnerNameFormat;
    }

    public List<String> getSpawnerLore() {
        return spawnerLore;
    }

    public String getRequiredLore() {
        return requiredLore;
    }

    public String getNoPermissionBreakSpawner() {
        return noPermissionBreakSpawner;
    }

    public String getNoPermissionWarning() {
        return noPermissionWarning;
    }

    public String getSpawnerBreakSuccessMessage(String mobType) {
        return spawnerBreakSuccessMessage.replace("{mobtype}", mobType);
    }

    public String getSpawnerPlaceSuccessMessage(String mobType) {
        return spawnerPlaceSuccessMessage.replace("{mobtype}", mobType);
    }

    public String getSpawnerDropMessage() {
        return spawnerDropMessage;
    }

    public String getSpawnerBreakFailureMessage() {
        return spawnerBreakFailureMessage;
    }

    public String getNoPermissionChangeSpawner() {
        return noPermissionChangeSpawner;
    }

    public boolean isAllowChangingSpawnersGlobally() {
        return allowChangingSpawnersGlobally;
    }

    public boolean isSpawnerToInventoryOnDrop() {
        return spawnerToInventoryOnDrop;
    }

    public static String translateColorCodes(String message) {
        StringBuilder result = new StringBuilder();
        StringBuilder currentText = new StringBuilder();
        ChatColor currentColor = ChatColor.WHITE;

        for (int i = 0; i < message.length(); i++) {
            if (message.charAt(i) == '&' && i + 7 < message.length()) {
                // Flush the current text with the current color
                if (currentText.length() > 0) {
                    result.append(currentColor).append(currentText);
                    currentText.setLength(0);
                }

                // Parse the new color
                String colorCode = message.substring(i + 1, i + 7);
                try {
                    Color color = Color.decode("#" + colorCode);
                    currentColor = ChatColor.of(color);
                } catch (NumberFormatException e) {
                    // Invalid color code, continue with the previous color
                }
                i += 6; // Skip the color code
            } else {
                currentText.append(message.charAt(i));
            }
        }

        // Add the remaining text
        if (currentText.length() > 0) {
            result.append(currentColor).append(currentText);
        }

        return result.toString();
    }
}