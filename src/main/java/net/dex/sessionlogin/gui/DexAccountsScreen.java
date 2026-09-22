package net.dex.sessionlogin.gui;

import net.dex.sessionlogin.DexSessionLogin;
import net.dex.sessionlogin.account.Account;
import net.dex.sessionlogin.gui.widget.AccountListWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DexAccountsScreen extends Screen {
    private final Screen parent;
    private AccountListWidget accountList;
    private Account selectedAccount;

    private ButtonWidget useButton;
    private ButtonWidget deleteButton;
    private ButtonWidget restoreButton;
    private Text feedbackMessage = Text.empty();

    public DexAccountsScreen(Screen parent) {
        super(Text.literal("DexSessionLogin"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int listWidth = Math.min(420, this.width - 40);
        int listHeight = this.height - 130;
        int listY = 40;

        accountList = new AccountListWidget(
                this,
                this.client,
                listWidth,
                listHeight,
                listY,
                32
        );
        accountList.setX((this.width - listWidth) / 2);
        this.addDrawableChild(accountList);

        int buttonWidth = 100;
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

        ButtonWidget addButton = ButtonWidget.builder(Text.literal("Add Account"), button -> {
            if (this.client != null) {
                this.client.setScreen(new AddAccountDialog(this));
            }
        }).dimensions(row1StartX + buttonWidth + buttonSpacing, row1Y, buttonWidth, 20).build();
        this.addDrawableChild(addButton);

        deleteButton = ButtonWidget.builder(Text.literal("Delete"), button -> {
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

        ButtonWidget directButton = ButtonWidget.builder(Text.literal("Direct Login"), button -> {
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
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("DexSessionLogin - Accounts").formatted(Formatting.BOLD, Formatting.GOLD),
                this.width / 2,
                10,
                0xFFFFFF
        );

        Session currentSession = DexSessionLogin.getCurrentSession();
        String currentName = currentSession != null ? currentSession.getUsername() : "Unknown";
        Text activeInfo = Text.literal("Logged in as: ").formatted(Formatting.GRAY)
                .append(Text.literal(currentName).formatted(Formatting.WHITE, Formatting.BOLD))
                .append(DexSessionLogin.isOriginalSession() ? Text.literal(" (Original)").formatted(Formatting.DARK_GRAY) : Text.literal(" (Custom Session)").formatted(Formatting.GREEN));

        context.drawCenteredTextWithShadow(this.textRenderer, activeInfo, this.width / 2, 25, 0xFFFFFF);

        if (!feedbackMessage.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, feedbackMessage, this.width / 2, this.height - 90, 0xFFFFFF);
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
