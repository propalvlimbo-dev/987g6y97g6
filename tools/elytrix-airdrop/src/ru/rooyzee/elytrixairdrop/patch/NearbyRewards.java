package ru.rooyzee.elytrixairdrop.patch;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Counts participants in a spherical 20-block radius around the event. */
public final class NearbyRewards {
    private NearbyRewards() {}
    public static int players(Location center) {
        if (center == null) return 0;
        World world = center.getWorld();
        if (world == null) return 0;
        int count = 0;
        for (Player player : world.getPlayers()) {
            if (player.getGameMode() != GameMode.SPECTATOR && player.getLocation().distanceSquared(center) <= 400.0) count++;
        }
        return count;
    }
    public static int rewardCount(int players) {
        return players <= 3 ? 3 : players <= 6 ? 5 : 8;
    }
}
