package net.dex.sessionlogin.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.dex.sessionlogin.DexSessionLogin;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.session.Session;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AccountManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("dexsessionlogin");
    private static final Path ACCOUNTS_FILE = CONFIG_DIR.resolve("accounts.json");

    private final List<Account> accounts = new ArrayList<>();
    private Account activeAccount = null;

    public AccountManager() {
        loadAccounts();
    }

    public synchronized void loadAccounts() {
        accounts.clear();
        if (Files.exists(ACCOUNTS_FILE)) {
            try (Reader reader = Files.newBufferedReader(ACCOUNTS_FILE)) {
                Type listType = new TypeToken<List<Account>>() {}.getType();
                List<Account> loaded = GSON.fromJson(reader, listType);
                if (loaded != null) {
                    accounts.addAll(loaded);
                }
            } catch (Exception e) {
                DexSessionLogin.LOGGER.error("Failed to load accounts from {}: {}", ACCOUNTS_FILE, e.getMessage());
            }
        }
    }

    public synchronized void saveAccounts() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            try (Writer writer = Files.newBufferedWriter(ACCOUNTS_FILE)) {
                GSON.toJson(accounts, writer);
            }
        } catch (Exception e) {
            DexSessionLogin.LOGGER.error("Failed to save accounts to {}: {}", ACCOUNTS_FILE, e.getMessage());
        }
    }

    public synchronized List<Account> getAccounts() {
        return new ArrayList<>(accounts);
    }

    public synchronized void addOrUpdateAccount(Account account) {
        accounts.removeIf(a -> a.getUuid().equals(account.getUuid()) || a.getToken().equals(account.getToken()));
        accounts.add(0, account);
        saveAccounts();
    }

    public synchronized void removeAccount(Account account) {
        accounts.remove(account);
        if (activeAccount != null && activeAccount.equals(account)) {
            activeAccount = null;
        }
        saveAccounts();
    }

    public synchronized void switchAccount(Account account) {
        this.activeAccount = account;
        Session session = createSession(account.getUsername(), account.getUuid(), account.getToken());
        DexSessionLogin.setCurrentSession(session);
    }

    public synchronized void switchToDirectSession(String username, UUID uuid, String token) {
        this.activeAccount = null;
        Session session = createSession(username, uuid, token);
        DexSessionLogin.setCurrentSession(session);
    }

    public synchronized void restoreOriginalSession() {
        this.activeAccount = null;
        DexSessionLogin.restoreOriginalSession();
    }

    public synchronized Account getActiveAccount() {
        return activeAccount;
    }

    public static Session createSession(String username, UUID uuid, String token) {
        return new Session(
                username,
                uuid,
                token,
                Optional.empty(),
                Optional.empty()
        );
    }
}
