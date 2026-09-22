package net.dex.sessionlogin;

import net.dex.sessionlogin.account.AccountManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DexSessionLogin implements ClientModInitializer {
    public static final String MOD_ID = "dexsessionlogin";
    public static final String MOD_NAME = "DexSessionLogin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static AccountManager accountManager;
    private static Session originalSession;
    private static Session currentSession;
    private static boolean overrideSession = false;

    @Override
    public void onInitializeClient() {
        accountManager = new AccountManager();

        String modVersion = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("1.0.0");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (originalSession == null && client.getSession() != null) {
                originalSession = client.getSession();
                currentSession = originalSession;
                overrideSession = true;
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
        return currentSession != null ? currentSession : originalSession;
    }

    public static void setCurrentSession(Session session) {
        currentSession = session;
        overrideSession = true;
    }

    public static void restoreOriginalSession() {
        if (originalSession != null) {
            currentSession = originalSession;
        }
    }

    public static boolean isOverriding() {
        return overrideSession && currentSession != null;
    }

    public static boolean isOriginalSession() {
        if (currentSession == null || originalSession == null) return true;
        return currentSession.getUuidOrNull().equals(originalSession.getUuidOrNull())
                && currentSession.getAccessToken().equals(originalSession.getAccessToken());
    }
}
