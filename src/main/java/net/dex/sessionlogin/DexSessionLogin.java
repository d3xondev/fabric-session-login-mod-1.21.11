package net.dex.sessionlogin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.dex.sessionlogin.account.AccountManager;
import net.dex.sessionlogin.mixin.MinecraftClientAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
import java.nio.file.Path;

public class DexSessionLogin implements ClientModInitializer {
    public static final String MOD_ID = "dexsessionlogin";
    public static final String MOD_NAME = "DexSessionLogin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static AccountManager accountManager;
    private static Session originalSession;

    @Override
    public void onInitializeClient() {
        accountManager = new AccountManager();

        String modVersion = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("1.0.0");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (originalSession == null && client.getSession() != null) {
                originalSession = client.getSession();
            }
        });

        LOGGER.info("{} v{} initialized successfully.", MOD_NAME, modVersion);
    }

    public static AccountManager getAccountManager() {
        return accountManager;
    }

    public static Session getOriginalSession() {
        return originalSession;
    }

    public static Session getCurrentSession() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getSession() != null) {
            return client.getSession();
        }
        return originalSession;
    }

    public static void setCurrentSession(Session session) {
        if (session == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        Runnable task = () -> {
            ((MinecraftClientAccessor) client).setSession(session);
            try {
                YggdrasilAuthenticationService authService = new YggdrasilAuthenticationService(Proxy.NO_PROXY);
                UserApiService userApiService = authService.createUserApiService(session.getAccessToken());
                ((MinecraftClientAccessor) client).setUserApiService(userApiService);
                Path profileKeysPath = client.runDirectory.toPath().resolve("profilekeys");
                ProfileKeys keys = ProfileKeys.create(userApiService, session, profileKeysPath);
                ((MinecraftClientAccessor) client).setProfileKeys(keys);
            } catch (Exception e) {
                ((MinecraftClientAccessor) client).setProfileKeys(ProfileKeys.MISSING);
            }
        };

        if (client.isOnThread()) {
            task.run();
        } else {
            client.execute(task);
        }
    }

    public static void restoreOriginalSession() {
        if (originalSession != null) {
            setCurrentSession(originalSession);
        }
    }

    public static boolean isOriginalSession() {
        Session cur = getCurrentSession();
        if (cur == null || originalSession == null) return true;
        if (cur.getUuidOrNull() == null || originalSession.getUuidOrNull() == null) return false;
        return cur.getUuidOrNull().equals(originalSession.getUuidOrNull())
                && cur.getAccessToken().equals(originalSession.getAccessToken());
    }
}
