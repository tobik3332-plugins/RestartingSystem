package cz.restartsystem;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RestartTabCompleter implements TabCompleter {

    private final RestartSystem plugin;

    public RestartTabCompleter(RestartSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("restartsytem.admin")) {
                completions.add("start");
                completions.add("cancel");
                completions.add("set");
                completions.add("time");
                completions.add("reload");
            }
        }

        return completions;
    }
}
