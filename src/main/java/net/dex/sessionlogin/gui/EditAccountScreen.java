package net.dex.sessionlogin.gui;

import net.dex.sessionlogin.DexSessionLogin;
import net.dex.sessionlogin.account.Account;
import net.dex.sessionlogin.account.AccountManager;
import net.dex.sessionlogin.service.MojangApiService;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class EditAccountScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget nameField;
    private TextFieldWidget skinUrlField;
    private ButtonWidget nameButton;
    private ButtonWidget skinButton;
    private Text statusMessage = Text.empty();

    public EditAccountScreen(Screen parent) {
        super(Text.literal("Edit Account"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        nameField = new TextFieldWidget(
                this.textRenderer,
                centerX - 100,
                centerY - 50,
                200,
                20,
                Text.literal("New Username")
        );
        nameField.setMaxLength(16);
        nameField.setPlaceholder(Text.literal("Enter new username...").formatted(Formatting.GRAY));
        this.addDrawableChild(nameField);

        skinUrlField = new TextFieldWidget(
                this.textRenderer,
                centerX - 100,
                centerY - 5,
                200,
                20,
                Text.literal("Skin URL")
        );
        skinUrlField.setMaxLength(2048);
        skinUrlField.setPlaceholder(Text.literal("Enter direct image URL (.png)...").formatted(Formatting.GRAY));
        this.addDrawableChild(skinUrlField);

        Session session = DexSessionLogin.getCurrentSession();
        String token = session != null ? session.getAccessToken() : "";

        nameButton = ButtonWidget.builder(Text.literal("Change Name"), button -> {
            String newName = nameField.getText().trim();
            if (newName.isEmpty()) {
                statusMessage = Text.literal("Please input a name").formatted(Formatting.RED);
                return;
            }
            if (!newName.matches("^[a-zA-Z0-9_]{3,16}$")) {
                statusMessage = Text.literal("Name must be 3-16 alphanumeric characters").formatted(Formatting.RED);
                return;
            }

            nameButton.active = false;
            statusMessage = Text.literal("Changing username...").formatted(Formatting.YELLOW);

            MojangApiService.changeNameAsync(token, newName).whenComplete((code, ex) -> {
                if (this.client != null) {
                    this.client.execute(() -> {
                        nameButton.active = true;
                        if (code == 200) {
                            statusMessage = Text.literal("Successfully changed name to " + newName).formatted(Formatting.GREEN);
                            Session updatedSession = AccountManager.createSession(newName, session.getUuidOrNull(), token);
                            DexSessionLogin.setCurrentSession(updatedSession);
                            Account activeAcc = DexSessionLogin.getAccountManager().getActiveAccount();
                            if (activeAcc != null) {
                                activeAcc.setUsername(newName);
                                DexSessionLogin.getAccountManager().saveAccounts();
                            }
                        } else if (code == 429) {
                            statusMessage = Text.literal("Rate limit reached. Try again later.").formatted(Formatting.RED);
                        } else if (code == 403) {
                            statusMessage = Text.literal("Name unavailable or already changed in last 30 days").formatted(Formatting.RED);
                        } else {
                            statusMessage = Text.literal("Failed to change name (HTTP " + code + ")").formatted(Formatting.RED);
                        }
                    });
                }
            });
        }).dimensions(centerX - 100, centerY + 25, 97, 20).build();
        this.addDrawableChild(nameButton);

        skinButton = ButtonWidget.builder(Text.literal("Change Skin"), button -> {
            String skinUrl = skinUrlField.getText().trim();
            if (skinUrl.isEmpty()) {
                statusMessage = Text.literal("Please input a skin URL").formatted(Formatting.RED);
                return;
            }

            skinButton.active = false;
            statusMessage = Text.literal("Changing skin...").formatted(Formatting.YELLOW);

            MojangApiService.changeSkinAsync(token, skinUrl).whenComplete((code, ex) -> {
                if (this.client != null) {
                    this.client.execute(() -> {
                        skinButton.active = true;
                        if (code == 200) {
                            statusMessage = Text.literal("Successfully updated skin!").formatted(Formatting.GREEN);
                        } else if (code == 429) {
                            statusMessage = Text.literal("Rate limit reached. Try again later.").formatted(Formatting.RED);
                        } else {
                            statusMessage = Text.literal("Failed to change skin (HTTP " + code + ")").formatted(Formatting.RED);
                        }
                    });
                }
            });
        }).dimensions(centerX + 3, centerY + 25, 97, 20).build();
        this.addDrawableChild(skinButton);

        ButtonWidget backButton = ButtonWidget.builder(Text.literal("Back"), button -> {
            if (this.client != null) {
                this.client.setScreen(parent);
            }
        }).dimensions(centerX - 100, centerY + 55, 200, 20).build();
        this.addDrawableChild(backButton);

        if (DexSessionLogin.isOriginalSession()) {
            nameButton.active = false;
            skinButton.active = false;
            statusMessage = Text.literal("Cannot modify original launcher session").formatted(Formatting.YELLOW);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Edit Account (Name & Skin)").formatted(Formatting.BOLD, Formatting.AQUA),
                this.width / 2,
                this.height / 2 - 80,
                0xFFFFFF
        );

        context.drawTextWithShadow(this.textRenderer, Text.literal("Username:"), this.width / 2 - 100, this.height / 2 - 62, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Skin URL:"), this.width / 2 - 100, this.height / 2 - 17, 0xAAAAAA);

        if (!statusMessage.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    statusMessage,
                    this.width / 2,
                    this.height / 2 + 85,
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
