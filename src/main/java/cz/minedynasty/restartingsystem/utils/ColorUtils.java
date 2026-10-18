package cz.minedynasty.restartingsystem.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static Component color(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        if (input.contains("<") && input.contains(">")) {
            return MiniMessage.miniMessage().deserialize(input);
        }

        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hexCode.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(builder, replacement.toString());
        }
        matcher.appendTail(builder);

        String colored = builder.toString().replace('&', '§');
        return LegacyComponentSerializer.legacySection().deserialize(colored);
    }
}
