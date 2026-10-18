package cz.minedynasty.restartingsystem.commands;

import cz.minedynasty.restartingsystem.RestartingSystem;
import cz.minedynasty.restartingsystem.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RestartingCommand implements CommandExecutor, TabCompleter {

    private final RestartingSystem plugin;

    public RestartingCommand(RestartingSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendMsg(sender, "messages.usage");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            if (!sender.hasPermission("restarting.admin")) {
                sendMsg(sender, "messages.no-permission");
                return true;
            }
            plugin.getConfigManager().reloadConfig();
            sendMsg(sender, "messages.reload-success");
            return true;
        }

        if (sub.equals("cancel")) {
            if (!sender.hasPermission("restarting.cancel")) {
                sendMsg(sender, "messages.no-permission");
                return true;
            }
            if (!plugin.getRestartManager().isRunning()) {
                sendMsg(sender, "messages.no-restart-running");
                return true;
            }
            plugin.getRestartManager().cancelRestart(true);
            return true;
        }

        long time;
        try {
            time = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            sendMsg(sender, "messages.invalid-time");
            return true;
        }

        if (args.length < 2) {
            sendMsg(sender, "messages.no-reason-provided");
            return true;
        }

        String reasonKey = args[1].toLowerCase();

        if (reasonKey.equals("custom")) {
            if (!sender.hasPermission("restarting.restart.custom")) {
                sendMsg(sender, "messages.no-permission");
                return true;
            }

            if (args.length < 3) {
                sendMsg(sender, "messages.no-custom-text-provided");
                return true;
            }

            String customText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            plugin.getRestartManager().startRestart(time, "custom", customText);
            return true;
        } else {
            if (!sender.hasPermission("restarting.restart")) {
                sendMsg(sender, "messages.no-permission");
                return true;
            }

            ConfigurationSection prompts = plugin.getConfigManager().getConfig().getConfigurationSection("prompts");
            if (prompts == null || !prompts.contains(reasonKey)) {
                sendMsg(sender, "messages.unknown-reason");
                return true;
            }

            plugin.getRestartManager().startRestart(time, reasonKey, null);
            return true;
        }
    }

    private void sendMsg(CommandSender sender, String configPath) {
        String msg = plugin.getConfigManager().getConfig().getString(configPath, "");
        if (!msg.isEmpty()) {
            sender.sendMessage(ColorUtils.color(msg));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("restarting.admin")) completions.add("reload");
            if (sender.hasPermission("restarting.cancel")) completions.add("cancel");

            if (sender.hasPermission("restarting.restart") || sender.hasPermission("restarting.restart.custom")) {
                List<String> exampleTimes = plugin.getConfigManager().getConfig().getStringList("example-time");
                completions.addAll(exampleTimes);
            }
            return filter(completions, args[0]);
        }

        if (args.length == 2 && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("cancel")) {
            if (sender.hasPermission("restarting.restart")) {
                ConfigurationSection prompts = plugin.getConfigManager().getConfig().getConfigurationSection("prompts");
                if (prompts != null) {
                    completions.addAll(prompts.getKeys(false));
                }
            }
            if (sender.hasPermission("restarting.restart.custom")) {
                completions.add("custom");
            }
            return filter(completions, args[1]);
        }

        return completions;
    }

    private List<String> filter(List<String> list, String input) {
        List<String> filtered = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(input.toLowerCase())) {
                filtered.add(s);
            }
        }
        return filtered;
    }
}
