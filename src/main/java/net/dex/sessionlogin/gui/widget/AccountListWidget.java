package net.dex.sessionlogin.gui.widget;

import com.mojang.authlib.GameProfile;
import net.dex.sessionlogin.DexSessionLogin;
import net.dex.sessionlogin.account.Account;
import net.dex.sessionlogin.gui.DexAccountsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.Supplier;

public class AccountListWidget extends AlwaysSelectedEntryListWidget<AccountListWidget.AccountEntry> {
    private final DexAccountsScreen parent;

    public AccountListWidget(DexAccountsScreen parent, MinecraftClient client, int width, int height, int y, int itemHeight) {
        super(client, width, height, y, itemHeight);
        this.parent = parent;
        refreshAccounts();
    }

    @Override
    public int getRowWidth() {
        return Math.min(320, this.width - 40);
    }

    @Override
    protected int getScrollbarX() {
        return this.getRowLeft() + this.getRowWidth() + 6;
    }

    public void refreshAccounts() {
        this.clearEntries();
        List<Account> accounts = DexSessionLogin.getAccountManager().getAccounts();
        for (Account account : accounts) {
            this.addEntry(new AccountEntry(account));
        }
        if (!this.children().isEmpty() && this.getSelectedOrNull() == null) {
            this.setSelected(this.children().get(0));
        }
    }

    public class AccountEntry extends AlwaysSelectedEntryListWidget.Entry<AccountEntry> {
        private final Account account;
        private final Supplier<SkinTextures> skinSupplier;
        private long lastClickTime = 0;

        public AccountEntry(Account account) {
            this.account = account;
            this.skinSupplier = client.getSkinProvider().supplySkinTextures(
                    new GameProfile(account.getUuid(), account.getUsername()),
                    true
            );
        }

        public Account getAccount() {
            return account;
        }

        @Override
        public Text getNarration() {
            return Text.literal(account.getUsername());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            int x = this.getX();
            int y = this.getY();
            int width = this.getWidth();

            boolean isActive = DexSessionLogin.getCurrentSession() != null &&
                    account.getUuid().equals(DexSessionLogin.getCurrentSession().getUuidOrNull());

            PlayerSkinDrawer.draw(context, skinSupplier.get(), x + 6, y + 4, 24);

            Text usernameText = Text.literal(account.getUsername()).formatted(Formatting.BOLD, Formatting.WHITE);
            context.drawTextWithShadow(client.textRenderer, usernameText, x + 36, y + 6, 0xFFFFFF);

            String uuidStr = account.getUuid().toString();
            context.drawTextWithShadow(client.textRenderer, Text.literal(uuidStr).formatted(Formatting.GRAY), x + 36, y + 18, 0x888888);

            if (isActive) {
                Text activeText = Text.literal("[ACTIVE]").formatted(Formatting.GREEN, Formatting.BOLD);
                int textWidth = client.textRenderer.getWidth(activeText);
                context.drawTextWithShadow(client.textRenderer, activeText, x + width - textWidth - 10, y + 12, 0x55FF55);
            }
        }

        @Override
        public boolean mouseClicked(Click click, boolean bl) {
            parent.selectAccount(this.account);
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < 300) {
                parent.useSelectedAccount();
            }
            lastClickTime = currentTime;
            return super.mouseClicked(click, bl);
        }
    }
}
