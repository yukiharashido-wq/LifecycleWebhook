package dev.khoa.lifecyclewebhook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.logging.Logger;

public class WebhookClient {
    private final String url;
    private final Logger log;
    private final HttpClient http = HttpClient.newHttpClient();

    public WebhookClient(String url, Logger log) {
        this.url = url;
        this.log = log;
    }

    public void postEmbed(JsonObject embed) {
        if (url == null || url.isBlank()) return;
        try {
            JsonObject body = new JsonObject();
            JsonArray embeds = new JsonArray();
            embeds.add(embed);
            body.add("embeds", embeds);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> {
                    if (resp.statusCode() >= 300) {
                        log.warning("Webhook status " + resp.statusCode() + ": " + resp.body());
                    }
                })
                .exceptionally(ex -> { log.warning("Webhook error: " + ex.getMessage()); return null; });
        } catch (Exception e) {
            log.warning("Webhook exception: " + e.getMessage());
        }
    }

    public static EmbedBuilder embed() { return new EmbedBuilder(); }

    public static class EmbedBuilder {
        private final JsonObject o = new JsonObject();
        private final JsonArray fields = new JsonArray();
        public EmbedBuilder title(String t){ o.addProperty("title", t); return this; }
        public EmbedBuilder description(String d){ o.addProperty("description", d); return this; }
        public EmbedBuilder color(int c){ o.addProperty("color", c); return this; }
        public EmbedBuilder field(String name, String value, boolean inline){
            JsonObject f = new JsonObject();
            f.addProperty("name", name);
            f.addProperty("value", value);
            f.addProperty("inline", inline);
            fields.add(f);
            return this;
        }
        public EmbedBuilder timestampNow(){
            o.addProperty("timestamp", Instant.now().toString());
            return this;
        }
        public JsonObject build(){
            if (fields.size() > 0) o.add("fields", fields);
            return o;
        }
    }
}
