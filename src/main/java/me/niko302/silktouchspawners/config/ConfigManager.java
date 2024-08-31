package me.niko302.silktouchspawners.config;

import lombok.AccessLevel;
import lombok.Getter;
import me.niko302.silktouchspawners.SilktouchSpawners;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
public class ConfigManager {

    @Getter(AccessLevel.NONE)
    private final Pattern hexColorExtractor = Pattern.compile("#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})");

    private final SilktouchSpawners plugin;
    private final Map<String, ItemStack> customItems = new HashMap<>();
    private final Map<String, String> spawnerNameFormatOverrides = new HashMap<>();
    private FileConfiguration config;
    private String spawnerNameFormat;
    private List<String> spawnerLore;
    private String requiredLore;
    private String requiredLoreOneTimeUse;
    private boolean requireSilkTouch;
    private String prefix;
    private String noPermissionWarning;
    private String spawnerDropMessage;
    private String spawnerBreakFailureMessage;
    private String noPermissionChangeSpawner;
    private String noPermissionBreakSpawnerProtected;
    private boolean allowChangingSpawnersGlobally;
    private boolean spawnerToInventoryOnDrop;

    @Getter(AccessLevel.NONE)
    private String spawnerBreakSuccessMessage;

    @Getter(AccessLevel.NONE)
    private String spawnerPlaceSuccessMessage;

    public ConfigManager(SilktouchSpawners plugin) {
        this.plugin = plugin;

        saveDefaultConfig();
        loadConfig();
    }

    public void saveDefaultConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        loadConfig();
    }

    private void loadConfig() {
        spawnerNameFormat = color(config.getString("spawner-name", "#808080{mobtype} #FFFFFF Spawner"));

        spawnerLore = new ArrayList<>();
        config.getStringList("spawner-lore").forEach(line -> spawnerLore.add(color(line)));

        requiredLore = color(config.getString("required-lore", "Special Lore"));
        requiredLoreOneTimeUse = color(config.getString("required-lore-one-time-use", "One Time Use Lore"));
        requireSilkTouch = config.getBoolean("require-silk-touch", true);
        prefix = color(config.getString("messages.prefix", "#FF4500[#FFFFFFSilktouchSpawners#FF4500] "));
        noPermissionWarning = color(prefix + config.getString("messages.no-permission-warning", "&cIf you try to break it again without the proper requirements, the spawner will be broken and not dropped."));
        spawnerBreakSuccessMessage = color(prefix + config.getString("messages.spawner-break-success", "&aYou successfully mined a {mobtype} Spawner!"));
        spawnerPlaceSuccessMessage = color(prefix + config.getString("messages.spawner-place-success", "&aYou placed a {mobtype} Spawner!"));
        spawnerDropMessage = color(prefix + config.getString("messages.spawner-drop-message", "&cYour inventory is full. The spawner has been dropped on the ground."));
        spawnerBreakFailureMessage = color(prefix + config.getString("messages.spawner-break-failure", "&cThe spawner was broken and not dropped because you didn't have the proper requirements."));
        noPermissionChangeSpawner = color(prefix + config.getString("messages.no-permission-change-spawner", "&cYou do not have permission to change the type of mob."));
        noPermissionBreakSpawnerProtected = color(prefix + config.getString("messages.no-permission-break-spawner-protected", "&cYou cannot take the spawner here because it is protected."));
        allowChangingSpawnersGlobally = config.getBoolean("allow-changing-spawners-globally", true);
        spawnerToInventoryOnDrop = config.getBoolean("spawner-to-inventory-on-drop", true);

        // Load entity overrides
        spawnerNameFormatOverrides.putAll(loadSpawnerNameFormatOverrides());

        // Load custom items
        ConfigurationSection customItemsSection = config.getConfigurationSection("custom-items");

        if (customItemsSection == null) {
            return;
        }

        for (String key : customItemsSection.getKeys(false)) {
            ConfigurationSection itemSection = customItemsSection.getConfigurationSection(key);

            if (itemSection == null) {
                continue;
            }

            ItemStack item = loadCustomItem(itemSection);
            customItems.put(key, item);
        }
    }

    private String color(String message) {
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);

        Matcher matcher = hexColorExtractor.matcher(coloredMessage);

        while (matcher.find()) {
            String hexColor = matcher.group();
            coloredMessage = coloredMessage.replace(hexColor, ChatColor.of(hexColor).toString());
        }

        return coloredMessage;
    }

    private ItemStack loadCustomItem(ConfigurationSection section) {
        Material material = Material.valueOf(section.getString("itemtype", "DIAMOND_PICKAXE"));
        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(section.getString("name", "")));

            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                lore.add(color(line.replace("{required-lore-one-time-use}", requiredLoreOneTimeUse).replace("{required-lore}", requiredLore)));
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

    public Map<String, String> loadSpawnerNameFormatOverrides() {
        return Optional.ofNullable(config.getConfigurationSection("mobtype-override"))
                .map(section -> section.getKeys(false)
                        .stream()
                        .filter(key -> section.getString(key) != null)
                        .collect(Collectors.toMap(
                                key -> key,
                                key -> color(section.getString(key))
                        ))
                ).orElseGet(HashMap::new);
    }

    public Optional<ItemStack> getCustomItem(String name) {
        return Optional.ofNullable(customItems.get(name));
    }

    public String getSpawnerBreakSuccessMessage(String mobType) {
        return spawnerBreakSuccessMessage.replace("{mobtype}", mobType);
    }

    public String getSpawnerPlaceSuccessMessage(String mobType) {
        return spawnerPlaceSuccessMessage.replace("{mobtype}", mobType);
    }

}