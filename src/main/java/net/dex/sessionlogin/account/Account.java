package net.dex.sessionlogin.account;

import java.util.Objects;
import java.util.UUID;

public class Account {
    private String username;
    private UUID uuid;
    private String token;
    private long addedAt;

    public Account() {
    }

    public Account(String username, UUID uuid, String token) {
        this.username = username;
        this.uuid = uuid;
        this.token = token;
        this.addedAt = System.currentTimeMillis();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account account)) return false;
        return Objects.equals(uuid, account.uuid) || Objects.equals(token, account.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, token);
    }
}
