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

public class AddAccountDialog extends Screen {
    private final Screen parent;
    private TextFieldWidget tokenField;
    private ButtonWidget addButton;
    private CheckboxWidget switchNowCheckbox;
    private Text statusMessage = Text.empty();
    private boolean isProcessing = false;

    public AddAccountDialog(Screen parent) {
        super(Text.literal("Add Account"));
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
                Text.literal("Session Token")
        );
        tokenField.setMaxLength(32767);
        tokenField.setPlaceholder(Text.literal("Paste session token / access token here...").formatted(Formatting.GRAY));
        this.addDrawableChild(tokenField);
        this.setInitialFocus(tokenField);

        switchNowCheckbox = CheckboxWidget.builder(Text.literal("Switch to this account immediately"), this.textRenderer)
                .pos(centerX - 150, centerY + 5)
                .checked(true)
                .build();
        this.addDrawableChild(switchNowCheckbox);

        addButton = ButtonWidget.builder(Text.literal("Add Account"), button -> {
            String token = tokenField.getText().trim();
            if (token.isEmpty()) {
                statusMessage = Text.literal("Token cannot be empty").formatted(Formatting.RED);
                return;
            }

            isProcessing = true;
            addButton.active = false;
            statusMessage = Text.literal("Validating token with Mojang API...").formatted(Formatting.YELLOW);

            MojangApiService.fetchProfileAsync(token).whenComplete((profile, throwable) -> {
                if (this.client != null) {
                    this.client.execute(() -> {
                        isProcessing = false;
                        addButton.active = true;
                        if (throwable != null || profile == null) {
                            statusMessage = Text.literal("Invalid session token!").formatted(Formatting.RED);
                        } else {
                            Account account = new Account(profile.name(), profile.uuid(), token);
                            DexSessionLogin.getAccountManager().addOrUpdateAccount(account);
                            if (switchNowCheckbox.isChecked()) {
                                DexSessionLogin.getAccountManager().switchAccount(account);
                            }
                            if (this.client != null) {
                                this.client.setScreen(parent);
                            }
                        }
                    });
                }
            });
        }).dimensions(centerX - 150, centerY + 35, 145, 20).build();
        this.addDrawableChild(addButton);

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
                Text.literal("Add Account via Session Token").formatted(Formatting.BOLD, Formatting.GOLD),
                this.width / 2,
                this.height / 2 - 60,
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
