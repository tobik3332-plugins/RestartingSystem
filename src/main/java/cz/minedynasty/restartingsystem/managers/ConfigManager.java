package cz.minedynasty.restartingsystem.managers;

import cz.minedynasty.restartingsystem.RestartingSystem;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final RestartingSystem plugin;
    private FileConfiguration config;

    public ConfigManager(RestartingSystem plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
