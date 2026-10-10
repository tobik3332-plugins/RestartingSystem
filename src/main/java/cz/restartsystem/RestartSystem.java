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

public final class RestartSystem extends JavaPlugin {

    private int timeLeft;
    private BukkitTask timerTask;
    private boolean isRunning = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PluginCommand command = getCommand("restartsytem");
        if (command != null) {
            command.setExecutor(new RestartCommand(this));
            command.setTabCompleter(new RestartTabCompleter(this));
        }

        getLogger().info("RestartingSystem has been enabled.");
    }

    @Override
    public void onDisable() {
        cancelRestart(false);
        getLogger().info("RestartingSystem has been disabled.");
    }

    public void loadAnnouncements() {
        reloadConfig();
    }

    public void startRestart(int time, List<Integer> customWarnings, String customCommand) {
        if (isRunning) {
            return;
        }

        this.timeLeft = time > 0 ? time : getConfig().getInt("restart-time", 300);
        this.isRunning = true;

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    executeRestart(customCommand);
                    cancel();
                    return;
                }

                List<Integer> warnings = (customWarnings != null && !customWarnings.isEmpty()) 
                        ? customWarnings 
                        : getConfig().getIntegerList("time-warnings");

                if (warnings.contains(timeLeft)) {
                    String msgKey = getWarningMessageKey(timeLeft);
                    String formattedTime = formatTime(timeLeft);
                    String msg = color(getConfig().getString("messages." + msgKey, "")).replace("%time%", formattedTime);
                    
                    Bukkit.broadcastMessage(msg);
                    playWarningSound();
                }

                timeLeft--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    public void cancelRestart(boolean notify) {
        if (!isRunning) {
            return;
        }

        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        isRunning = false;

        if (notify) {
            Bukkit.broadcastMessage(color(getConfig().getString("messages.restart-cancelled", "")));
        }
    }

    private void executeRestart(String customCommand) {
        isRunning = false;
        Bukkit.broadcastMessage(color(getConfig().getString("messages.restart-now", "")));

        // Odeslání Discord webhooku při spuštění restartu
        sendDiscordWebhook();

        String cmd = (customCommand != null && !customCommand.isEmpty()) 
                ? customCommand 
                : getConfig().getString("command", "restart");

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

    private void playWarningSound() {
        if (!getConfig().getBoolean("sound.enabled", true)) {
            return;
        }

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

    private String getWarningMessageKey(int seconds) {
        if (seconds >= 60) {
            int minutes = seconds / 60;
            return (minutes == 1) ? "time-warning-minute" : "time-warning-minutes";
        } else {
            return (seconds == 1) ? "time-warning-second" : "time-warning-seconds";
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

    public String color(String message) {
        if (message == null) return "";
        String prefix = getConfig().getString("messages.prefix", "&e[System] &r");
        return ChatColor.translateAlternateColorCodes('&', message.replace("%prefix%", prefix));
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }
}
