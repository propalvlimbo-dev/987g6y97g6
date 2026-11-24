package ru.elytrix.bots;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

final class TrainingPlayer extends ServerPlayer {
    void jumpForCombat() { jumpFromGround(); }
    long nextAttackTick;
    long nextStrafeTick, sprintResumeTick;
    int strafeDirection = 1;
    boolean criticalPlanned = java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.32;
    private net.minecraft.world.phys.Vec3 pendingVelocity;
    void acceptKnockback(net.minecraft.world.phys.Vec3 velocity) { pendingVelocity = velocity; }
    TrainingPlayer(MinecraftServer server, ServerLevel world, GameProfile profile) {
        super(server, world, profile, ClientInformation.createDefault());
        this.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f);
    }
    @Override public boolean allowsListing() { return false; }
    @Override public void tick() {
        if (pendingVelocity != null) { setDeltaMovement(pendingVelocity); pendingVelocity = null; }
        super.tick();
        double previousY = getY();
        doTick();
        // Real clients report falling via movement packets; this bot has no client.
        if (onGround()) fallDistance = 0;
        else if (getY() < previousY) fallDistance += previousY - getY();
    }
}
