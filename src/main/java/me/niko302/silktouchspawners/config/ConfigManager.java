package me.niko302.silktouchspawners.config;

import me.niko302.silktouchspawners.silktouchspawners;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.md_5.bungee.api.ChatColor;

import java.awt.Color;
import java.util.*;

public class ConfigManager {

    private final silktouchspawners plugin;
    private FileConfiguration config;
    private String spawnerNameFormat;
    private List<String> spawnerLore;
    private String requiredLore;
    private boolean requireSilkTouch;
    private String prefix;
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
        requiredLore = translateColorCodes(config.getString("required-lore", "Special Lore"));
        requireSilkTouch = config.getBoolean("require-silk-touch", true);
        prefix = translateColorCodes(config.getString("messages.prefix", "&FF4500[&FFFFFFSilktouchSpawners&FF4500] "));
        noPermissionBreakSpawner = translateColorCodes(prefix + config.getString("messages.no-permission-break-spawner", "&cYou do not have permission to break this spawner."));
        noPermissionWarning = translateColorCodes(prefix + config.getString("messages.no-permission-warning", "&cIf you try to break it again without the proper requirements, the spawner will be broken and not dropped."));
        spawnerBreakSuccessMessage = translateColorCodes(prefix + config.getString("messages.spawner-break-success", "&aYou successfully mined a {mobtype} Spawner!"));
        spawnerPlaceSuccessMessage = translateColorCodes(prefix + config.getString("messages.spawner-place-success", "&aYou placed a {mobtype} Spawner!"));
        spawnerDropMessage = translateColorCodes(prefix + config.getString("messages.spawner-drop-message", "&cYour inventory is full. The spawner has been dropped on the ground."));
        spawnerBreakFailureMessage = translateColorCodes(prefix + config.getString("messages.spawner-break-failure", "&cThe spawner was broken and not dropped because you didn't have the proper requirements."));
        noPermissionChangeSpawner = translateColorCodes(prefix + config.getString("messages.no-permission-change-spawner", "&cYou do not have permission to change the type of mob."));
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
            for (String line : section.getStringList("lore")) {
                lore.add(translateColorCodes(line.replace("{required-lore}", requiredLore)));
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

    public Set<String> getCustomItemNames() {
        return customItems.keySet();
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

    public boolean isRequireSilkTouch() {
        return requireSilkTouch;
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
        ChatColor currentColor = ChatColor.WHITE;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;
        boolean obfuscated = false;

        for (int i = 0; i < message.length(); i++) {
            if (message.charAt(i) == '&' && i + 1 < message.length()) {
                char nextChar = message.charAt(i + 1);
                if (isHexDigit(nextChar) && i + 6 < message.length() && isHexColor(message.substring(i + 1, i + 7))) {
                    // Parse the new RGB color
                    String colorCode = message.substring(i + 1, i + 7);
                    try {
                        Color color = Color.decode("#" + colorCode);
                        currentColor = ChatColor.of(color);
                        result.append(currentColor);
                        bold = italic = underline = strikethrough = obfuscated = false; // Reset formatting
                    } catch (NumberFormatException e) {
                        // Invalid color code, continue with the previous color
                    }
                    i += 6; // Skip the color code
                } else if (isValidChatColorCode(nextChar)) {
                    currentColor = ChatColor.getByChar(nextChar);
                    result.append(currentColor);
                    bold = italic = underline = strikethrough = obfuscated = false; // Reset formatting
                    i++; // Skip the color code
                } else if (nextChar == 'l' || nextChar == 'L') {
                    bold = true;
                    result.append(ChatColor.BOLD);
                    i++;
                } else if (nextChar == 'o' || nextChar == 'O') {
                    italic = true;
                    result.append(ChatColor.ITALIC);
                    i++;
                } else if (nextChar == 'n' || nextChar == 'N') {
                    underline = true;
                    result.append(ChatColor.UNDERLINE);
                    i++;
                } else if (nextChar == 'm' || nextChar == 'M') {
                    strikethrough = true;
                    result.append(ChatColor.STRIKETHROUGH);
                    i++;
                } else if (nextChar == 'k' || nextChar == 'K') {
                    obfuscated = true;
                    result.append(ChatColor.MAGIC);
                    i++;
                } else if (nextChar == 'r' || nextChar == 'R') {
                    currentColor = ChatColor.RESET;
                    result.append(currentColor);
                    bold = italic = underline = strikethrough = obfuscated = false; // Reset formatting
                    i++;
                } else {
                    result.append('&').append(nextChar);
                    i++;
                }
            } else {
                result.append(message.charAt(i));
            }
        }

        return result.toString();
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean isHexColor(String color) {
        return color.matches("[0-9A-Fa-f]{6}");
    }

    private static boolean isValidChatColorCode(char c) {
        return "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(c) > -1;
    }
}