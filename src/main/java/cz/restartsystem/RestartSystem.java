package cz.restartsystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RestartSystem extends JavaPlugin {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private BukkitTask mainTimerTask;
    private int secondsRemaining = 0;
    private Map<Integer, String> announcements = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadAnnouncements();

        Objects.requireNonNull(getCommand("restarting")).setExecutor(new RestartCommand(this));
        Objects.requireNonNull(getCommand("restarting")).setTabCompleter(new RestartTabCompleter(this));
        
        getLogger().info("RestartSystem byl uspesne aktivovan!");
    }

    @Override
    public void onDisable() {
        cancelRestart();
    }

    public void loadAnnouncements() {
        announcements.clear();
        List<String> rawAnnounce = getConfig().getStringList("announce");
        for (String entry : rawAnnounce) {
            String[] split = entry.split(",", 2);
            if (split.length == 2) {
                try {
                    int sec = Integer.parseInt(split[0].trim());
                    announcements.put(sec, split[1].trim());
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public boolean isRestarting() {
        return mainTimerTask != null;
    }

    public void startRestart(int seconds, List<String> broadcastTemplate, String customReason) {
        cancelRestart();
        this.secondsRemaining = seconds;

        sendFormattedBroadcast(broadcastTemplate, secondsRemaining, customReason);

        mainTimerTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            secondsRemaining--;

            if (announcements.containsKey(secondsRemaining)) {
                String msg = announcements.get(secondsRemaining);
                Bukkit.broadcastMessage(color(msg));
            }

            if (secondsRemaining <= 0) {
                executeServerRestart();
            }
        }, 20L, 20L);
    }

    public void cancelRestart() {
        if (mainTimerTask != null) {
            mainTimerTask.cancel();
            mainTimerTask = null;
            secondsRemaining = 0;
        }
    }

    private void executeServerRestart() {
        cancelRestart();
        String kickMsg = color(getConfig().getString("kick-message", "&cServer se restartuje!"));
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(kickMsg);
        }

        Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.spigot().restart(), 10L);
    }

    public void sendFormattedBroadcast(List<String> template, int seconds, String customReason) {
        String formattedTime = formatTime(seconds);
        for (String line : template) {
            String processed = line.replace("{time}", formattedTime);
            if (customReason != null) {
                processed = processed.replace("{reason}", customReason);
            }
            Bukkit.broadcastMessage(color(processed));
        }
    }

    public String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        String minText = "";
        if (minutes > 0) {
            if (minutes == 1) {
                minText = getConfig().getString("time-formatting.minutes.one", "1 minuta");
            } else if (minutes >= 2 && minutes <= 4) {
                minText = getConfig().getString("time-formatting.minutes.few", "{count} minuty").replace("{count}", String.valueOf(minutes));
            } else {
                minText = getConfig().getString("time-formatting.minutes.many", "{count} minut").replace("{count}", String.valueOf(minutes));
            }
        }

        String secText = "";
        if (seconds > 0) {
            if (seconds == 1) {
                secText = getConfig().getString("time-formatting.seconds.one", "1 sekunda");
            } else if (seconds >= 2 && seconds <= 4) {
                secText = getConfig().getString("time-formatting.seconds.few", "{count} sekundy").replace("{count}", String.valueOf(seconds));
            } else {
                secText = getConfig().getString("time-formatting.seconds.many", "{count} sekund").replace("{count}", String.valueOf(seconds));
            }
        }

        if (!minText.isEmpty() && !secText.isEmpty()) {
            return minText + getConfig().getString("time-formatting.connector", " a ") + secText;
        } else if (!minText.isEmpty()) {
            return minText;
        } else {
            return secText.isEmpty() ? "0 sekund" : secText;
        }
    }

    public String color(String message) {
        if (message == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String color = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + color).toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
