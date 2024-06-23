package me.niko302.silktouchspawners;

import me.niko302.silktouchspawners.commands.Commands;
import me.niko302.silktouchspawners.commands.CommandTabCompleter;
import me.niko302.silktouchspawners.commands.CustomItemCommand;
import me.niko302.silktouchspawners.config.ConfigManager;
import me.niko302.silktouchspawners.listeners.BlockBreakListener;
import me.niko302.silktouchspawners.listeners.BlockPlaceListener;
import me.niko302.silktouchspawners.listeners.SpawnerInteractListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class silktouchspawners extends JavaPlugin implements Listener {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // Initialize ConfigManager
        configManager = new ConfigManager(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

        // Register commands and tab completer
        Commands commandExecutor = new Commands(this);
        getCommand("reloadconfig").setExecutor(commandExecutor);
        getCommand("silktouchspawners").setExecutor(commandExecutor);
        getCommand("silktouchspawners").setTabCompleter(new CommandTabCompleter());

        // Register custom item command
        CustomItemCommand customItemCommand = new CustomItemCommand(this);
        getCommand("givecustomitem").setExecutor(customItemCommand);

        int pluginId = 22326; // BSTATS logging player activity
        Metrics metrics = new Metrics(this, pluginId);

        getLogger().info("SilkTouchSpawners has started successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SilkTouchSpawners has stopped.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Check if the event was cancelled by another plugin
        if (event.isCancelled()) {
            return;
        }
        // Custom logic can be added here if needed
    }
}