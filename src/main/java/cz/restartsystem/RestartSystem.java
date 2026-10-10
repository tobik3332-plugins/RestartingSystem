package cz.restartsystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RestartSystem extends JavaPlugin {

    private int timeLeft;
    private BukkitTask timerTask;
    private boolean isRestarting = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        PluginCommand command = getCommand("restartsytem");
        if (command != null) {
            RestartCommand cmdHandler = new RestartCommand(this);
            command.setExecutor(cmdHandler);
            command.setTabCompleter(new RestartTabCompleter());
        }

        startRestartTimer();
        getLogger().info("RestartingSystem by tobik3332 enabled!");
    }

    @Override
    public void onDisable() {
        cancelRestartTimer();
        getLogger().info("RestartingSystem disabled!");
    }

    public void startRestartTimer() {
        cancelRestartTimer();
        
        timeLeft = getConfig().getInt("restart-time", 300);
        isRestarting = true;

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    executeRestart();
                    cancel();
                    return;
                }

                checkAndSendWarning(timeLeft);
                timeLeft--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    public void cancelRestartTimer() {
        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel();
        }
        isRestarting = false;
    }

    private void checkAndSendWarning(int seconds) {
        List<Integer> warnings = getConfig().getIntegerList("time-warnings");
        if (warnings.contains(seconds)) {
            String messageKey = getWarningMessageKey(seconds);
            String formattedTime = formatTime(seconds);
            String msg = getMsg(messageKey).replace("%time%", formattedTime);
            
            Bukkit.broadcastMessage(msg);
            playSound();
        }
    }

    private String getWarningMessageKey(int seconds) {
        if (seconds >= 60) {
            int minutes = seconds / 60;
            return (minutes == 1) ? "messages.time-warning-minute" : "messages.time-warning-minutes";
        } else {
            return (seconds == 1) ? "messages.time-warning-second" : "messages.time-warning-seconds";
        }
    }

    private String formatTime(int totalSeconds) {
        if (totalSeconds >= 60) {
            int minutes = totalSeconds / 60;
            return minutes + getConfig().getString("time-format.minutes", "m");
        } else {
            return totalSeconds + getConfig().getString("time-format.seconds", "s");
        }
    }

    private void playSound() {
        if (!getConfig().getBoolean("sound.enabled", true)) return;

        String soundName = getConfig().getString("sound.sound-name", "ENTITY_EXPERIENCE_ORB_PICKUP");
        float volume = (float) getConfig().getDouble("sound.volume", 1.0);
        float pitch = (float) getConfig().getDouble("sound.pitch", 1.0);

        try {
            Sound sound = Sound.valueOf(soundName);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid sound name in config: " + soundName);
        }
    }

    public void executeRestart() {
        isRestarting = false;
        Bukkit.broadcastMessage(getMsg("messages.restart-now"));
        
        // Odeslání zprávy přes Discord Webhook při spuštění restartu
        sendDiscordWebhook();

        String cmd = getConfig().getString("command", "restart");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    private void sendDiscordWebhook() {
        FileConfiguration config = getConfig();

        if (!config.getBoolean("discord.enabled", false)) {
            return;
        }

        String webhookUrl = config.getString("discord.webhook-url");
        String messageContent = config.getString("discord.message", "Server se restartuje.");

        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_DISCORD_WEBHOOK_URL_HERE")) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("User-Agent", "RestartingSystem-Plugin");
                connection.setDoOutput(true);

                String jsonPayload = "{\"content\": \"" + escapeJson(messageContent) + "\"}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    getLogger().warning("Nepodařilo se odeslat Discord webhook. HTTP kód: " + responseCode);
                }

                connection.disconnect();
            } catch (Exception e) {
                getLogger().severe("Chyba při odesílání Discord webhooku: " + e.getMessage());
            }
        });
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }

    public String getMsg(String path) {
        String prefix = getConfig().getString("messages.prefix", "&e[System] &r");
        String msg = getConfig().getString(path, "");
        msg = msg.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public void setTimeLeft(int seconds) {
        this.timeLeft = seconds;
    }

    public boolean isRestarting() {
        return isRestarting;
    }
}
