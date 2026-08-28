package cz.restartsystem;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RestartCommand implements CommandExecutor {

    private final RestartSystem plugin;

    public RestartCommand(RestartSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.usage")));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("restarting.admin")) {
                sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission")));
                return true;
            }
            plugin.reloadConfig();
            plugin.loadAnnouncements();
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.reload-success")));
            return true;
        }

        if (args[0].equalsIgnoreCase("cancel")) {
            if (!sender.hasPermission("restarting.cancel")) {
                sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission")));
                return true;
            }
            if (!plugin.isRestarting()) {
                sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-restart-running")));
                return true;
            }
            plugin.cancelRestart(true);
            return true;
        }

        if (!sender.hasPermission("restarting.restart")) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.invalid-time")));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-reason-provided")));
            return true;
        }

        String reasonKey = args[1].toLowerCase();

        if (reasonKey.equalsIgnoreCase("custom")) {
            if (!sender.hasPermission("restarting.restart.custom")) {
                sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission")));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-custom-text-provided")));
                return true;
            }
            
            StringBuilder customReason = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                customReason.append(args[i]).append(" ");
            }

            List<String> template = plugin.getConfig().getStringList("custom-prompts.prompt");
            plugin.startRestart(seconds, template, customReason.toString().trim());
            return true;
        }

        if (!plugin.getConfig().contains("prompts." + reasonKey)) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.unknown-reason")));
            return true;
        }

        List<String> template = plugin.getConfig().getStringList("prompts." + reasonKey);
        plugin.startRestart(seconds, template, null);

        return true;
    }
}
