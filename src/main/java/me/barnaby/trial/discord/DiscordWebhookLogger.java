package me.barnaby.trial.discord;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DiscordWebhookLogger {

    private final String webhookUrl;
    private final String embedTitle;
    private final String embedDescriptionTemplate;
    private final String embedColor; // Hex color code (e.g., "#00FF00")

    public DiscordWebhookLogger(String webhookUrl, String embedTitle, String embedDescriptionTemplate, String embedColor) {
        this.webhookUrl = webhookUrl;
        this.embedTitle = embedTitle;
        this.embedDescriptionTemplate = embedDescriptionTemplate;
        this.embedColor = embedColor;
    }

    public void sendPurchaseLog(String buyerName, String sellerName, String itemName, int amount, double price, String time) {
        // Convert embed color from hex (e.g., "#00FF00") to an integer value
        int colorValue;
        try {
            colorValue = Integer.parseInt(embedColor.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            colorValue = 0xFFFFFF; // default to white if parsing fails
        }

        // Replace placeholders in the embed description template
        String description = embedDescriptionTemplate
                .replace("%item%", itemName)
                .replace("%amount%", String.valueOf(amount))
                .replace("%price%", String.valueOf(price))
                .replace("%time%", time)
                .replace("%buyer%", buyerName)
                .replace("%seller%", sellerName);

        String jsonPayload = "{\"embeds\":[{" +
                "\"title\":\"" + escapeJson(embedTitle) + "\"," +
                "\"description\":\"" + escapeJson(description) + "\"," +
                "\"color\":" + colorValue +
                "}]}";

        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.addRequestProperty("Content-Type", "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes("UTF-8"));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_NO_CONTENT && responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed to send Discord webhook. Response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // A simple method to escape JSON special characters.
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

