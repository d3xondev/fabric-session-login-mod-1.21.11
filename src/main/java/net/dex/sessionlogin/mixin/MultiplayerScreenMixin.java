package net.dex.sessionlogin.mixin;

import net.dex.sessionlogin.DexSessionLogin;
import net.dex.sessionlogin.gui.DexAccountsScreen;
import net.dex.sessionlogin.gui.EditAccountScreen;
import net.dex.sessionlogin.service.MojangApiService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {

    @Unique
    private static Boolean isSessionValid = null;
    @Unique
    private static String lastValidatedToken = null;
    @Unique
    private static boolean isValidationRunning = false;

    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        int sessionLoginButtonX = this.width - 100;
        int editButtonX = this.width - 160;
        int buttonY = 5;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Session Login"), button -> {
            MinecraftClient.getInstance().setScreen(new DexAccountsScreen(this));
        }).dimensions(sessionLoginButtonX, buttonY, 95, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), button -> {
            MinecraftClient.getInstance().setScreen(new EditAccountScreen(this));
        }).dimensions(editButtonX, buttonY, 55, 20).build());

        this.addDrawable((context, mouseX, mouseY, delta) -> {
            Session session = DexSessionLogin.getCurrentSession();
            String username = session != null ? session.getUsername() : "Unknown";
            String token = session != null ? session.getAccessToken() : "";
            UUID uuid = session != null ? session.getUuidOrNull() : null;

            if (token != null && !token.equals(lastValidatedToken)) {
                lastValidatedToken = token;
                isSessionValid = null;
                isValidationRunning = false;
            }

            if (isSessionValid == null && !isValidationRunning && token != null && !token.isEmpty()) {
                isValidationRunning = true;
                MojangApiService.validateTokenAsync(token, username, uuid).thenAccept(valid -> {
                    isSessionValid = valid;
                    isValidationRunning = false;
                });
            }

            Text statusText;
            if (isSessionValid == null) {
                statusText = Text.literal("[... Checking]").formatted(Formatting.GRAY);
            } else if (isSessionValid) {
                statusText = Text.literal("[✔] Valid").formatted(Formatting.GREEN);
            } else {
                statusText = Text.literal("[✘] Invalid").formatted(Formatting.RED);
            }

            Text display = Text.literal("DexSession: ").formatted(Formatting.GOLD)
                    .append(Text.literal(username).formatted(Formatting.WHITE, Formatting.BOLD))
                    .append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
                    .append(statusText);

            context.drawText(this.textRenderer, display, 8, 10, 0xFFFFFFFF, true);
        });
    }
}
