package net.dex.sessionlogin.gui;

import net.dex.sessionlogin.DexSessionLogin;
import net.dex.sessionlogin.account.Account;
import net.dex.sessionlogin.service.MojangApiService;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DirectTokenLoginScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget tokenField;
    private ButtonWidget loginButton;
    private CheckboxWidget saveCheckbox;
    private Text statusMessage = Text.empty();

    public DirectTokenLoginScreen(Screen parent) {
        super(Text.literal("Login with Session ID"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        tokenField = new TextFieldWidget(
                this.textRenderer,
                centerX - 150,
                centerY - 25,
                300,
                20,
                Text.literal("Session ID Input")
        );
        tokenField.setMaxLength(32767);
        tokenField.setPlaceholder(Text.literal("Paste Session ID here...").formatted(Formatting.GRAY));
        this.addDrawableChild(tokenField);
        this.setInitialFocus(tokenField);

        saveCheckbox = CheckboxWidget.builder(Text.literal("Save Session ID to Accounts list"), this.textRenderer)
                .pos(centerX - 150, centerY + 5)
                .checked(true)
                .build();
        this.addDrawableChild(saveCheckbox);

        loginButton = ButtonWidget.builder(Text.literal("Login"), button -> {
            String token = tokenField.getText().trim();
            if (token.isEmpty()) {
                statusMessage = Text.literal("Session ID cannot be empty").formatted(Formatting.RED);
                return;
            }

            loginButton.active = false;
            statusMessage = Text.literal("Validating Session ID with Mojang...").formatted(Formatting.YELLOW);

            MojangApiService.fetchProfileAsync(token).whenComplete((profile, throwable) -> {
                if (this.client != null) {
                    this.client.execute(() -> {
                        loginButton.active = true;
                        if (throwable != null || profile == null) {
                            statusMessage = Text.literal("Invalid Session ID!").formatted(Formatting.RED);
                        } else {
                            if (saveCheckbox.isChecked()) {
                                Account account = new Account(profile.name(), profile.uuid(), token);
                                DexSessionLogin.getAccountManager().addOrUpdateAccount(account);
                                DexSessionLogin.getAccountManager().switchAccount(account);
                            } else {
                                DexSessionLogin.getAccountManager().switchToDirectSession(profile.name(), profile.uuid(), token);
                            }
                            statusMessage = Text.literal("Logged in as " + profile.name()).formatted(Formatting.GREEN);
                            this.client.setScreen(parent);
                        }
                    });
                }
            });
        }).dimensions(centerX - 150, centerY + 35, 145, 20).build();
        this.addDrawableChild(loginButton);

        ButtonWidget cancelButton = ButtonWidget.builder(Text.literal("Cancel"), button -> {
            if (this.client != null) {
                this.client.setScreen(parent);
            }
        }).dimensions(centerX + 5, centerY + 35, 145, 20).build();
        this.addDrawableChild(cancelButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Login with Session ID").formatted(Formatting.BOLD, Formatting.GOLD),
                this.width / 2,
                this.height / 2 - 60,
                0xFFFFFF
        );

        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Enter Session ID:").formatted(Formatting.WHITE),
                this.width / 2 - 150,
                this.height / 2 - 38,
                0xFFFFFF
        );

        if (!statusMessage.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    statusMessage,
                    this.width / 2,
                    this.height / 2 + 65,
                    0xFFFFFF
            );
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
