package ru.elytrix.bots;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

final class TrainingPlayer extends ServerPlayer {
    private net.minecraft.world.phys.Vec3 pendingVelocity;
    void acceptKnockback(net.minecraft.world.phys.Vec3 velocity) { pendingVelocity = velocity; }
    TrainingPlayer(MinecraftServer server, ServerLevel world, GameProfile profile) {
        super(server, world, profile, ClientInformation.createDefault());
    }
    @Override public boolean allowsListing() { return false; }
    @Override public void tick() {
        if (pendingVelocity != null) { setDeltaMovement(pendingVelocity); pendingVelocity = null; }
        super.tick();
        doTick();
    }
}
