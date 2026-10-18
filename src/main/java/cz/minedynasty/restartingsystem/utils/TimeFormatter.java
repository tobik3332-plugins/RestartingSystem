package cz.minedynasty.restartingsystem.utils;

import cz.minedynasty.restartingsystem.managers.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class TimeFormatter {

    public static String formatTime(long totalSeconds, ConfigManager configManager) {
        FileConfiguration config = configManager.getConfig();

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        List<String> parts = new ArrayList<>();

        if (hours > 0) {
            parts.add(formatUnit(hours, config, "hours"));
        }
        if (minutes > 0) {
            parts.add(formatUnit(minutes, config, "minutes"));
        }
        if (seconds > 0 || parts.isEmpty()) {
            parts.add(formatUnit(seconds, config, "seconds"));
        }

        String connector = config.getString("time-formatting.connector", " a ");

        if (parts.size() == 1) {
            return parts.get(0);
        } else if (parts.size() == 2) {
            return parts.get(0) + connector + parts.get(1);
        } else {
            return parts.get(0) + ", " + parts.get(1) + connector + parts.get(2);
        }
    }

    private static String formatUnit(long amount, FileConfiguration config, String unitKey) {
        String path = "time-formatting." + unitKey + ".";
        String template;

        if (amount == 1) {
            template = config.getString(path + "one", "1 " + unitKey);
        } else if (amount >= 2 && amount <= 4) {
            template = config.getString(path + "few", "{count} " + unitKey);
        } else {
            template = config.getString(path + "many", "{count} " + unitKey);
        }

        return template.replace("{count}", String.valueOf(amount));
    }
}
