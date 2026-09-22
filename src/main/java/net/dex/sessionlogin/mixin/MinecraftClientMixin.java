package net.dex.sessionlogin.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.dex.sessionlogin.DexSessionLogin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;
import java.util.UUID;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    @Final
    public File runDirectory;

    @Unique
    private UUID lastProfileKeysUuid = null;

    @Unique
    private String lastProfileKeysToken = null;

    @Unique
    private ProfileKeys cachedProfileKeys = null;

    @Inject(method = "getSession", at = @At("HEAD"), cancellable = true)
    private void onGetSession(CallbackInfoReturnable<Session> cir) {
        if (!DexSessionLogin.isOverriding()) {
            return;
        }

        cir.setReturnValue(DexSessionLogin.getCurrentSession());
    }

    @Inject(method = "getProfileKeys", at = @At("HEAD"), cancellable = true)
    private void onGetProfileKeys(CallbackInfoReturnable<ProfileKeys> cir) {
        if (!DexSessionLogin.isOverriding()) {
            return;
        }

        Session currentSession = DexSessionLogin.getCurrentSession();
        if (currentSession == null) {
            return;
        }

        UUID currentUuid = currentSession.getUuidOrNull();
        String currentToken = currentSession.getAccessToken();

        if (lastProfileKeysUuid == null ||
                !lastProfileKeysUuid.equals(currentUuid) ||
                lastProfileKeysToken == null ||
                !lastProfileKeysToken.equals(currentToken)) {

            lastProfileKeysUuid = currentUuid;
            lastProfileKeysToken = currentToken;

            DexSessionLogin.LOGGER.info("Session updated, refreshing ProfileKeys for: {}", currentSession.getUsername());

            try {
                YggdrasilAuthenticationService authService = new YggdrasilAuthenticationService(Proxy.NO_PROXY);
                UserApiService userApiService = authService.createUserApiService(currentToken);
                Path profileKeysPath = this.runDirectory.toPath().resolve("profilekeys");
                cachedProfileKeys = ProfileKeys.create(userApiService, currentSession, profileKeysPath);
            } catch (Exception e) {
                DexSessionLogin.LOGGER.error("Failed to create ProfileKeys for session: {}", e.getMessage());
                cachedProfileKeys = null;
            }
        }

        if (cachedProfileKeys != null) {
            cir.setReturnValue(cachedProfileKeys);
        }
    }
}
