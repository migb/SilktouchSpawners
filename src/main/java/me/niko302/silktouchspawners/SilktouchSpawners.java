package me.niko302.silktouchspawners;

import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import com.jeff_media.updatechecker.UserAgentBuilder;
import lombok.Getter;
import me.niko302.silktouchspawners.commands.CustomItemCommand;
import me.niko302.silktouchspawners.commands.SilktouchSpawnersCommand;
import me.niko302.silktouchspawners.config.ConfigManager;
import me.niko302.silktouchspawners.listeners.SpawnerListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@Getter
public class SilktouchSpawners extends JavaPlugin implements Listener {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // Initialize ConfigManager
        configManager = new ConfigManager(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new SpawnerListener(this), this);

        // Register commands and tab completer
        getCommand("silktouchspawners").setExecutor(new SilktouchSpawnersCommand(this));
        getCommand("givecustomitem").setExecutor(new CustomItemCommand(configManager));

        new Metrics(this, 22326);

        new UpdateChecker(this, UpdateCheckSource.SPIGOT, "117535")
                .setNotifyRequesters(false)
                .setNotifyOpsOnJoin(false)
                .setUserAgent(UserAgentBuilder.getDefaultUserAgent())
                .checkEveryXHours(12)
                .onSuccess((commandSenders, latestVersion) -> {
                    String messagePrefix = "&8[&6Silktouch Spawners&8] ";
                    String currentVersion = getDescription().getVersion();

                    if (currentVersion.equalsIgnoreCase(latestVersion)) {
                        String updateMessage = color(messagePrefix + "&aYou are using the latest version of SilktouchSpawner!");

                        Bukkit.getConsoleSender().sendMessage(updateMessage);
                        Bukkit.getOnlinePlayers().stream().filter(ServerOperator::isOp).forEach(player -> player.sendMessage(updateMessage));
                        return;
                    }

                    List<String> updateMessages = List.of(
                            color(messagePrefix + "&cYour version of SilktouchSpawner is outdated!"),
                            color(String.format(messagePrefix + "&cYou are using %s, latest is %s!", currentVersion, latestVersion)),
                            color(messagePrefix + "&cDownload latest here:"),
                            color("&6https://www.spigotmc.org/resources/silktouchspawners-1-20.117535/")
                    );

                    Bukkit.getConsoleSender().sendMessage(updateMessages.toArray(new String[]{}));
                    Bukkit.getOnlinePlayers().stream().filter(ServerOperator::isOp).forEach(player -> player.sendMessage(updateMessages.toArray(new String[]{})));
                })
                .onFail((commandSenders, e) -> {}).checkNow();
    }

    @Override
    public void onDisable() {

    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

}