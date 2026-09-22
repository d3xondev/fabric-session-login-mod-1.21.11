package net.dex.sessionlogin.service;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SkinManager {
    private static final Map<UUID, Supplier<SkinTextures>> CACHE = new ConcurrentHashMap<>();

    public static Supplier<SkinTextures> getSkin(UUID uuid, String username) {
        if (uuid == null) {
            return () -> DefaultSkinHelper.getSteve();
        }

        Supplier<SkinTextures> existing = CACHE.get(uuid);
        if (existing != null) {
            return existing;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        GameProfile baseProfile = new GameProfile(uuid, username != null ? username : "Player");
        Supplier<SkinTextures> fallbackSupplier = client.getSkinProvider().supplySkinTextures(baseProfile, false);
        CACHE.put(uuid, fallbackSupplier);

        CompletableFuture.supplyAsync(() -> {
            try {
                ProfileResult result = client.getApiServices().sessionService().fetchProfile(uuid, false);
                if (result != null && result.profile() != null) {
                    return result.profile();
                }
            } catch (Exception ignored) {
            }
            return baseProfile;
        }).thenAcceptAsync(profile -> {
            Supplier<SkinTextures> realSkinSupplier = client.getSkinProvider().supplySkinTextures(profile, false);
            CACHE.put(uuid, realSkinSupplier);
        }, client);

        return fallbackSupplier;
    }

    public static void invalidate(UUID uuid) {
        if (uuid != null) {
            CACHE.remove(uuid);
        }
    }
}
