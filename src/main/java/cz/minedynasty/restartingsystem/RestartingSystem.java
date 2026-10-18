package cz.minedynasty.restartingsystem;

import cz.minedynasty.restartingsystem.commands.RestartingCommand;
import cz.minedynasty.restartingsystem.managers.ConfigManager;
import cz.minedynasty.restartingsystem.managers.DiscordWebhookManager;
import cz.minedynasty.restartingsystem.managers.RestartManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RestartingSystem extends JavaPlugin {

    private ConfigManager configManager;
    private DiscordWebhookManager discordWebhookManager;
    private RestartManager restartManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.discordWebhookManager = new DiscordWebhookManager(this);
        this.restartManager = new RestartManager(this);

        RestartingCommand cmd = new RestartingCommand(this);
        if (getCommand("restarting") != null) {
            getCommand("restarting").setExecutor(cmd);
            getCommand("restarting").setTabCompleter(cmd);
        }

        getLogger().info("RestartingSystem byl úspěšně aktivován!");
    }

    @Override
    public void onDisable() {
        if (restartManager != null && restartManager.isRunning()) {
            restartManager.cancelRestart(false);
        }
        getLogger().info("RestartingSystem byl vypnut.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DiscordWebhookManager getDiscordWebhookManager() {
        return discordWebhookManager;
    }

    public RestartManager getRestartManager() {
        return restartManager;
    }
}
