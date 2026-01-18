package powermonitor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;

public class SlackService {
    static final HttpClient client = HttpClient.newHttpClient();
    public static final Logger LOGGER = Logger.getLogger(SlackService.class.getName());

    String slackUrl;
    String channel;

    public SlackService(String slackUrl, String slackChannel) {
        this.slackUrl = slackUrl;
        this.channel = channel;
    }

    public CompletableFuture<Void> sendNotification(String text) {
        if (slackUrl != null) {
            try {
                String payload = String.format(
                        "{\"channel\": \"%s\", \"username\": \"homecontrol\", \"text\": \"%s\", \"icon_emoji\": \":electric_plug:\"}",
                        "#" + channel,
                        escapeJson(text)
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(slackUrl))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            if (response.statusCode() != 200) {
                                LOGGER.severe("Non-200 response from Slack: " + response.statusCode() + " Body: " + response.body());
                            }
                        }).exceptionally(e -> {
                            LOGGER.severe("Posting to Slack failed for text: " + text + " - " + e.getMessage());
                            return null;
                        });
            } catch (Exception e) {
                LOGGER.severe("Failed to build Slack request for text: " + text + " - " + e.getMessage());
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
