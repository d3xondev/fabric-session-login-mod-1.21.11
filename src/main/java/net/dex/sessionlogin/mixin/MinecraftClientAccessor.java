package net.dex.sessionlogin.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("session")
    @Mutable
    void setSession(Session session);

    @Accessor("profileKeys")
    @Mutable
    void setProfileKeys(ProfileKeys profileKeys);

    @Accessor("userApiService")
    @Mutable
    void setUserApiService(UserApiService userApiService);
}
