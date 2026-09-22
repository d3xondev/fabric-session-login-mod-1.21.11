package net.dex.sessionlogin.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MojangApiService {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public record ProfileData(String name, UUID uuid, String rawUuid) {}

    public static ProfileData fetchProfile(String token) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch profile: HTTP " + response.statusCode() + " - " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        String name = json.get("name").getAsString();
        String id = json.get("id").getAsString();
        UUID uuid = parseUndashedUuid(id);

        return new ProfileData(name, uuid, id);
    }

    public static CompletableFuture<ProfileData> fetchProfileAsync(String token) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("HTTP " + response.statusCode());
                    }
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    String name = json.get("name").getAsString();
                    String id = json.get("id").getAsString();
                    return new ProfileData(name, parseUndashedUuid(id), id);
                });
    }

    public static CompletableFuture<Boolean> validateTokenAsync(String token, String expectedName, UUID expectedUuid) {
        return fetchProfileAsync(token)
                .thenApply(profile -> {
                    boolean nameMatches = expectedName == null || expectedName.equalsIgnoreCase(profile.name());
                    boolean uuidMatches = expectedUuid == null || expectedUuid.equals(profile.uuid());
                    return nameMatches && uuidMatches;
                })
                .exceptionally(t -> false);
    }

    public static CompletableFuture<Integer> changeSkinAsync(String token, String skinUrl) {
        String jsonPayload = String.format("{\"variant\":\"classic\",\"url\":\"%s\"}", skinUrl);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/skins"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .exceptionally(t -> -1);
    }

    public static CompletableFuture<Integer> changeNameAsync(String token, String newName) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/name/" + newName))
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(10))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .exceptionally(t -> -1);
    }

    public static UUID parseUndashedUuid(String id) {
        if (id == null) return null;
        if (id.length() == 32) {
            String dashed = id.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                    "$1-$2-$3-$4-$5"
            );
            return UUID.fromString(dashed);
        }
        return UUID.fromString(id);
    }
}
