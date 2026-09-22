package net.dex.sessionlogin.gui;

import net.dex.sessionlogin.DexSessionLogin;
import net.dex.sessionlogin.account.Account;
import net.dex.sessionlogin.gui.widget.AccountListWidget;
import net.dex.sessionlogin.service.MojangApiService;
import net.dex.sessionlogin.service.SkinManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.session.Session;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;
import java.util.function.Supplier;

public class DexAccountsScreen extends Screen {
    private final Screen parent;
    private AccountListWidget accountList;
    private Account selectedAccount;

    private ButtonWidget useButton;
    private ButtonWidget deleteButton;
    private ButtonWidget restoreButton;
    private Text feedbackMessage = Text.empty();

    private Boolean isCurrentSessionValid = null;
    private String lastCheckedToken = null;
    private boolean isChecking = false;

    public DexAccountsScreen(Screen parent) {
        super(Text.literal("DexSessionLogin"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int listY = 52;
        int listHeight = this.height - listY - 80;

        accountList = new AccountListWidget(
                this,
                this.client,
                this.width,
                listHeight,
                listY,
                36
        );
        accountList.position(this.width, listHeight, 0, listY);
        this.addDrawableChild(accountList);

        int buttonWidth = 115;
        int buttonSpacing = 6;
        int row1Y = this.height - 75;
        int row2Y = this.height - 50;
        int row3Y = this.height - 25;

        int row1TotalWidth = (buttonWidth * 3) + (buttonSpacing * 2);
        int row1StartX = (this.width - row1TotalWidth) / 2;

        useButton = ButtonWidget.builder(Text.literal("Use Account"), button -> useSelectedAccount())
                .dimensions(row1StartX, row1Y, buttonWidth, 20)
                .build();
        this.addDrawableChild(useButton);

        ButtonWidget addButton = ButtonWidget.builder(Text.literal("Add Session ID"), button -> {
            if (this.client != null) {
                this.client.setScreen(new AddAccountDialog(this));
            }
        }).dimensions(row1StartX + buttonWidth + buttonSpacing, row1Y, buttonWidth, 20).build();
        this.addDrawableChild(addButton);

        deleteButton = ButtonWidget.builder(Text.literal("Delete Account"), button -> {
            if (selectedAccount != null) {
                DexSessionLogin.getAccountManager().removeAccount(selectedAccount);
                selectedAccount = null;
                accountList.refreshAccounts();
                updateButtons();
                feedbackMessage = Text.literal("Account removed.").formatted(Formatting.YELLOW);
            }
        }).dimensions(row1StartX + (buttonWidth + buttonSpacing) * 2, row1Y, buttonWidth, 20).build();
        this.addDrawableChild(deleteButton);

        int row2TotalWidth = (buttonWidth * 3) + (buttonSpacing * 2);
        int row2StartX = (this.width - row2TotalWidth) / 2;

        ButtonWidget directButton = ButtonWidget.builder(Text.literal("Enter Session ID"), button -> {
            if (this.client != null) {
                this.client.setScreen(new DirectTokenLoginScreen(this));
            }
        }).dimensions(row2StartX, row2Y, buttonWidth, 20).build();
        this.addDrawableChild(directButton);

        ButtonWidget editButton = ButtonWidget.builder(Text.literal("Edit Account"), button -> {
            if (this.client != null) {
                this.client.setScreen(new EditAccountScreen(this));
            }
        }).dimensions(row2StartX + buttonWidth + buttonSpacing, row2Y, buttonWidth, 20).build();
        this.addDrawableChild(editButton);

        restoreButton = ButtonWidget.builder(Text.literal("Restore Original"), button -> {
            DexSessionLogin.getAccountManager().restoreOriginalSession();
            accountList.refreshAccounts();
            updateButtons();
            feedbackMessage = Text.literal("Restored launcher session.").formatted(Formatting.GREEN);
        }).dimensions(row2StartX + (buttonWidth + buttonSpacing) * 2, row2Y, buttonWidth, 20).build();
        this.addDrawableChild(restoreButton);

        ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(this.width / 2 - 75, row3Y, 150, 20)
                .build();
        this.addDrawableChild(doneButton);

        updateButtons();
    }

    public void selectAccount(Account account) {
        this.selectedAccount = account;
        updateButtons();
    }

    public void useSelectedAccount() {
        if (selectedAccount != null) {
            DexSessionLogin.getAccountManager().switchAccount(selectedAccount);
            accountList.refreshAccounts();
            updateButtons();
            feedbackMessage = Text.literal("Active account switched to: " + selectedAccount.getUsername()).formatted(Formatting.GREEN);
        }
    }

    private void updateButtons() {
        boolean hasSelection = selectedAccount != null;
        useButton.active = hasSelection;
        deleteButton.active = hasSelection;
        restoreButton.active = !DexSessionLogin.isOriginalSession();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("DexSessionLogin - Account Manager").formatted(Formatting.BOLD, Formatting.GOLD),
                this.width / 2,
                6,
                0xFFFFFFFF
        );

        Session currentSession = DexSessionLogin.getCurrentSession();
        String currentName = currentSession != null ? currentSession.getUsername() : "Unknown";
        String currentToken = currentSession != null ? currentSession.getAccessToken() : "";
        UUID currentUuid = currentSession != null ? currentSession.getUuidOrNull() : null;

        if (currentToken != null && !currentToken.equals(lastCheckedToken)) {
            lastCheckedToken = currentToken;
            isCurrentSessionValid = null;
            isChecking = false;
        }

        if (isCurrentSessionValid == null && !isChecking && currentToken != null && !currentToken.isEmpty()) {
            isChecking = true;
            MojangApiService.validateTokenAsync(currentToken, currentName, currentUuid).thenAccept(valid -> {
                isCurrentSessionValid = valid;
                isChecking = false;
            });
        }

        int cardWidth = Math.min(340, this.width - 20);
        int cardX = (this.width - cardWidth) / 2;
        int cardY = 19;
        context.fill(cardX, cardY, cardX + cardWidth, cardY + 28, 0x66000000);

        UUID headUuid = currentUuid != null ? currentUuid : UUID.nameUUIDFromBytes(("OfflinePlayer:" + currentName).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Supplier<SkinTextures> skinSupplier = SkinManager.getSkin(headUuid, currentName);
        PlayerSkinDrawer.draw(context, skinSupplier.get(), cardX + 6, cardY + 4, 20);

        Text userPrefix = Text.literal("Logged in: ").formatted(Formatting.GRAY);
        Text userName = Text.literal(currentName).formatted(Formatting.BOLD, Formatting.GREEN);
        Text sessionType = DexSessionLogin.isOriginalSession()
                ? Text.literal(" (Original)").formatted(Formatting.DARK_GRAY)
                : Text.literal(" (Custom Session)").formatted(Formatting.GREEN);

        context.drawTextWithShadow(
                this.textRenderer,
                Text.empty().append(userPrefix).append(userName).append(sessionType),
                cardX + 32,
                cardY + 5,
                0xFFFFFFFF
        );

        Text statusBadge;
        if (isCurrentSessionValid == null) {
            statusBadge = Text.literal("[... Checking]").formatted(Formatting.GRAY);
        } else if (isCurrentSessionValid) {
            statusBadge = Text.literal("[✔] Valid Session").formatted(Formatting.GREEN);
        } else {
            statusBadge = Text.literal("[✘] Invalid Session").formatted(Formatting.RED);
        }
        context.drawTextWithShadow(this.textRenderer, statusBadge, cardX + 32, cardY + 16, 0xFFFFFFFF);

        if (!feedbackMessage.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, feedbackMessage, this.width / 2, this.height - 90, 0xFFFFFFFF);
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
