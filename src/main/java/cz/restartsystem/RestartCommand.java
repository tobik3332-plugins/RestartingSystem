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
            sender.sendMessage(plugin.color("&cPouziti: /restarting <cas|reload|cancel> [duvod|custom] [vlastni text]"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("restarting.admin")) {
                sender.sendMessage(plugin.color("&cNemas opravneni k tomuto prikazu!"));
                return true;
            }
            plugin.reloadConfig();
            plugin.loadAnnouncements();
            sender.sendMessage(plugin.color("&a[RestartSystem] Konfigurace byla uspesne reloadovana."));
            return true;
        }

        if (args[0].equalsIgnoreCase("cancel")) {
            if (!sender.hasPermission("restarting.cancel")) {
                sender.sendMessage(plugin.color("&cNemas opravneni ke zruseni restartu!"));
                return true;
            }
            if (!plugin.isRestarting()) {
                sender.sendMessage(plugin.color("&cZadny restart aktualne neprobiha."));
                return true;
            }
            plugin.cancelRestart();
            sender.sendMessage(plugin.color(plugin.getConfig().getString("cancel-message", "&aRestart zrusen.")));
            return true;
        }

        if (!sender.hasPermission("restarting.restart")) {
            sender.sendMessage(plugin.color("&cNemas opravneni k vyvolani restartu!"));
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.color("&cNeplatny cas! Musi byt zadano cislo v sekundach."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.color("&cMusis zadat duvod restartu!"));
            return true;
        }

        String reasonKey = args[1].toLowerCase();

        if (reasonKey.equalsIgnoreCase("custom")) {
            if (!sender.hasPermission("restarting.restart.custom")) {
                sender.sendMessage(plugin.color("&cNemas opravneni pro vlastni duvod restartu!"));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(plugin.color("&cMusis napsat text vlastniho duvodu!"));
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
            sender.sendMessage(plugin.color("&cTento duvod v konfiguraci neexistuje!"));
            return true;
        }

        List<String> template = plugin.getConfig().getStringList("prompts." + reasonKey);
        plugin.startRestart(seconds, template, null);

        return true;
    }
}
