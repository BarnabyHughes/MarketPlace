package me.barnaby.trial.discord;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Handles sending purchase transaction logs to a Discord webhook.
 */
public class DiscordWebhookLogger {

    private final String webhookUrl;
    private final String embedTitle;
    private final String embedDescriptionTemplate;
    private final String embedColor; // Hex color code (e.g., "#00FF00")

    /**
     * Constructs a DiscordWebhookLogger.
     *
     * @param webhookUrl              The webhook URL to send messages to.
     * @param embedTitle              The title of the Discord embed.
     * @param embedDescriptionTemplate The description template with placeholders.
     * @param embedColor              The hex color code of the embed.
     */
    public DiscordWebhookLogger(String webhookUrl, String embedTitle, String embedDescriptionTemplate, String embedColor) {
        this.webhookUrl = webhookUrl;
        this.embedTitle = embedTitle;
        this.embedDescriptionTemplate = embedDescriptionTemplate;
        this.embedColor = embedColor;
    }

    /**
     * Sends a purchase transaction log to Discord via a webhook.
     *
     * @param buyerName Name of the player who bought the item.
     * @param sellerName Name of the player who sold the item.
     * @param itemName Name of the purchased item.
     * @param amount Quantity of the purchased item.
     * @param price Price of the item.
     * @param time The timestamp of the transaction.
     */
    public void sendPurchaseLog(String buyerName, String sellerName, String itemName, int amount, double price, String time) {
        // Convert embed color from hex (e.g., "#00FF00") to an integer value
        int colorValue;
        try {
            colorValue = Integer.parseInt(embedColor.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            colorValue = 0xFFFFFF; // Default to white if parsing fails
        }

        // Replace placeholders in the embed description template
        String description = embedDescriptionTemplate
                .replace("%item%", itemName)
                .replace("%amount%", String.valueOf(amount))
                .replace("%price%", String.valueOf(price))
                .replace("%time%", time)
                .replace("%buyer%", buyerName)
                .replace("%seller%", sellerName);

        // Construct the JSON payload for the Discord webhook
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

    /**
     * Escapes JSON special characters to prevent formatting issues.
     *
     * @param text The input string to escape.
     * @return The escaped string safe for JSON.
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
