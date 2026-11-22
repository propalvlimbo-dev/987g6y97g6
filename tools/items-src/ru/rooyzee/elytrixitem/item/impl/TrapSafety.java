package ru.rooyzee.elytrixitem.item.impl;

import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;

/** Placement safety and delayed, collision-aware block restoration. */
public final class TrapSafety {
    private TrapSafety() {}

    // Called before the existing protected-block, existing-trap and WorldGuard checks.
    public static boolean prepare(Object listener, Player owner, World world,
                                  int minX, int maxX, int minY, int maxY,
                                  int minZ, int maxZ, Map<Block, BlockData> plan) {
        BlockData air = Bukkit.createBlockData(Material.AIR);
        for (Player player : world.getPlayers()) {
            BoundingBox b = player.getBoundingBox();
            if (b.getMaxX() <= minX || b.getMinX() >= maxX + 1.0 ||
                b.getMaxZ() <= minZ || b.getMinZ() >= maxZ + 1.0 ||
                b.getMaxY() <= minY || b.getMinY() >= maxY + 4.0) continue;
            int left = Math.max(minX, (int) Math.floor(b.getMinX()) - 1);
            int right = Math.min(maxX, (int) Math.floor(b.getMaxX() - 1e-7) + 1);
            int bottom = Math.max(world.getMinHeight(), (int) Math.floor(b.getMinY()));
            int top = Math.min(world.getMaxHeight() - 1, (int) Math.floor(b.getMaxY() - 1e-7));
            int front = Math.max(minZ, (int) Math.floor(b.getMinZ()) - 1);
            int back = Math.min(maxZ, (int) Math.floor(b.getMaxZ() - 1e-7) + 1);
            for (int x = left; x <= right; x++) for (int y = bottom; y <= top; y++) for (int z = front; z <= back; z++) {
                Block block = world.getBlockAt(x, y, z);
                BlockData proposed = plan.get(block);
                boolean intersects = b.getMaxX() > x && b.getMinX() < x + 1.0 &&
                    b.getMaxY() > y && b.getMinY() < y + 1.0 &&
                    b.getMaxZ() > z && b.getMinZ() < z + 1.0;
                if (proposed != null) {
                    // Never place a schematic block into a player.
                    if (intersects && !proposed.getMaterial().isAir()) return false;
                } else if (!block.isPassable()) {
                    // This is deliberate player clearance, not pasted schematic air.
                    plan.put(block, air);
                }
            }
        }
        return true;
    }

    private static boolean occupied(Block block) {
        int x = block.getX(), y = block.getY(), z = block.getZ();
        for (Player player : block.getWorld().getPlayers()) {
            BoundingBox b = player.getBoundingBox();
            if (b.getMaxX() > x && b.getMinX() < x + 1.0 &&
                b.getMaxY() > y && b.getMinY() < y + 1.0 &&
                b.getMaxZ() > z && b.getMinZ() < z + 1.0) return true;
        }
        return false;
    }

    // Restores original blocks unless doing so would enclose a player. In that case
    // keep the cell empty and retry later, but never overwrite someone else's block.
    public static boolean restore(BlockState state, boolean force, boolean physics) {
        Block block = state.getBlock();
        if (state.getType().isAir() || !occupied(block)) return state.update(force, physics);
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ElytrixItem");
        if (plugin == null || !plugin.isEnabled()) return false;
        block.setType(Material.AIR, false);
        Bukkit.getScheduler().runTaskLater(plugin, new DeferredRestore(state, force, physics, plugin), 10L);
        return true;
    }

    private static final class DeferredRestore implements Runnable {
        private final BlockState state;
        private final boolean force, physics;
        private final Plugin plugin;
        DeferredRestore(BlockState state, boolean force, boolean physics, Plugin plugin) {
            this.state = state; this.force = force; this.physics = physics; this.plugin = plugin;
        }
        public void run() {
            Block block = state.getBlock();
            if (!block.getType().isAir()) return;
            if (occupied(block)) {
                if (plugin.isEnabled()) Bukkit.getScheduler().runTaskLater(plugin, this, 10L);
            } else {
                state.update(force, physics);
            }
        }
    }
}
