package cz.minedynasty.restartingsystem.managers;

import cz.minedynasty.restartingsystem.RestartingSystem;
import cz.minedynasty.restartingsystem.utils.ColorUtils;
import cz.minedynasty.restartingsystem.utils.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestartManager {

    private final RestartingSystem plugin;
    private BukkitTask task;
    private long remainingSeconds;
    private boolean isRunning = false;
    private final Map<Long, String> announceMap = new HashMap<>();

    public RestartManager(RestartingSystem plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void startRestart(long seconds, String reasonType, String customReasonText) {
        if (isRunning) {
            cancelRestart(false);
        }

        this.remainingSeconds = seconds;
        this.isRunning = true;

        // Načtení oznámení z konfigurace
        loadAnnounceMap();

        // Prompt se pošle POUZE JEDNOU hned při spuštění příkazu
        showPrompt(reasonType, customReasonText);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (remainingSeconds <= 0) {
                    executeRestart();
                    cancel();
                    return;
                }

                // Odeslání zprávy podle nastaveného času v announce
                if (announceMap.containsKey(remainingSeconds)) {
                    String msg = announceMap.get(remainingSeconds);
                    Bukkit.broadcast(ColorUtils.color(msg));
                }

                remainingSeconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void cancelRestart(boolean notify) {
        if (task != null) {
            task.cancel();
            task = null;
        }
        isRunning = false;

        if (notify) {
            List<String> cancelLines = plugin.getConfigManager().getConfig().getStringList("prompt-cancel.prompt");
            for (String line : cancelLines) {
                Bukkit.broadcast(ColorUtils.color(line));
            }
        }
    }

    private void showPrompt(String reasonType, String customReasonText) {
        List<String> promptLines;
        String formattedTime = TimeFormatter.formatTime(remainingSeconds, plugin.getConfigManager());

        if ("custom".equalsIgnoreCase(reasonType)) {
            promptLines = plugin.getConfigManager().getConfig().getStringList("custom-prompts.prompt");
        } else {
            promptLines = plugin.getConfigManager().getConfig().getStringList("prompts." + reasonType);
        }

        if (promptLines.isEmpty()) {
            return;
        }

        for (String line : promptLines) {
            String processed = line.replace("{time}", formattedTime);
            if ("custom".equalsIgnoreCase(reasonType) && customReasonText != null) {
                processed = processed.replace("{reason}", customReasonText);
            }
            Bukkit.broadcast(ColorUtils.color(processed));
        }
    }

    private void executeRestart() {
        // Discord Webhook se odesílá až po dokončení odpočtu
        plugin.getDiscordWebhookManager().sendWebhookMessage();

        String kickMsgRaw = plugin.getConfigManager().getConfig().getString("kick-message", "Server se restartuje!");
        net.kyori.adventure.text.Component kickComponent = ColorUtils.color(kickMsgRaw);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kick(kickComponent);
        }

        Bukkit.shutdown();
    }

    private void loadAnnounceMap() {
        announceMap.clear();
        List<String> list = plugin.getConfigManager().getConfig().getStringList("announce");
        for (String entry : list) {
            String[] parts = entry.split(",", 2);
            if (parts.length == 2) {
                try {
                    long sec = Long.parseLong(parts[0].trim());
                    announceMap.put(sec, parts[1].trim());
                } catch (NumberFormatException ignored) {}
            }
        }
    }
}
