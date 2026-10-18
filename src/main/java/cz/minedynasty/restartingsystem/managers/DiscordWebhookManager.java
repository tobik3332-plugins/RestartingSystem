package cz.minedynasty.restartingsystem.managers;

import cz.minedynasty.restartingsystem.RestartingSystem;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhookManager {

    private final RestartingSystem plugin;

    public DiscordWebhookManager(RestartingSystem plugin) {
        this.plugin = plugin;
    }

    public void sendWebhookMessage() {
        boolean enabled = plugin.getConfigManager().getConfig().getBoolean("discord.enabled", false);
        String webhookUrl = plugin.getConfigManager().getConfig().getString("discord.webhook-url", "");
        String message = plugin.getConfigManager().getConfig().getString("discord.message", "");

        if (!enabled || webhookUrl.isEmpty()) {
            return;
        }

        // Při vypínání serveru odesíláme synchronně, aby server neukončil proces dříve, než zpráva odejde
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(5000); // 5 sekund timeout
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);

            String jsonPayload = "{\"content\": \"" + escapeJson(message) + "\"}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                plugin.getLogger().warning("Discord webhook odeslán s chybovým kódem: " + responseCode);
            } else {
                plugin.getLogger().info("Discord webhook byl úspěšně odeslán.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Nepodařilo se odeslat Discord Webhook: " + e.getMessage());
        }
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
