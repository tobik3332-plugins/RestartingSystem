package cz.restartsystem;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RestartTabCompleter implements TabCompleter {

    private final RestartSystem plugin;

    public RestartTabCompleter(RestartSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("restarting.restart")) {
                completions.addAll(plugin.getConfig().getStringList("example-time"));
            }
            if (sender.hasPermission("restarting.admin")) {
                completions.add("reload");
            }
            if (sender.hasPermission("restarting.cancel") && plugin.isRestarting()) {
                completions.add("cancel");
            }
            return filter(completions, args[0]);
        }

        if (args.length == 2 && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("cancel")) {
            if (sender.hasPermission("restarting.restart")) {
                ConfigurationSection section = plugin.getConfig().getConfigurationSection("prompts");
                if (section != null) {
                    completions.addAll(section.getKeys(false));
                }
            }
            if (sender.hasPermission("restarting.restart.custom")) {
                completions.add("custom");
            }
            return filter(completions, args[1]);
        }

        return List.of();
    }

    private List<String> filter(List<String> list, String input) {
        List<String> result = new ArrayList<>();
        for (String item : list) {
            if (item.toLowerCase().startsWith(input.toLowerCase())) {
                result.add(item);
            }
        }
        return result;
    }
}
