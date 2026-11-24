package ru.elytrix.duelsbridge.shared;

import java.util.*;

/**
 * World-independent battle lease registry. This is not yet connected to Bukkit.
 * A lease covers preparation, combat, result display, and successful player
 * restoration; it is released explicitly, never by timeout or world occupancy.
 */
public final class SharedArenaRegistry {
    public record Point(String world, double x, double y, double z, float yaw, float pitch) {
        public Point {
            Objects.requireNonNull(world);
            if (world.isBlank() || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch))
                throw new IllegalArgumentException("Invalid point");
        }
    }
    public record Pair(int firstNumber, Point first, Point second) {
        public Pair {
            if (firstNumber < 1 || (firstNumber & 1) == 0 || first == null || second == null
                    || !first.world().equals(second.world())) throw new IllegalArgumentException("Invalid pair");
        }
    }
    public record Lease(UUID battle, Pair pair) {}
    public enum Status { RESERVED, FULL, INVALID, CONFIG_CHANGED }
    public record Result(Status status, Lease lease) {}

    private List<Pair> config = List.of();
    private final Map<UUID, Lease> byBattle = new HashMap<>();
    private final Map<Integer, UUID> owners = new HashMap<>();

    /**
     * Creates numbered pairs without collapsing gaps: 1-2, 3-4, etc.
     * Rejects points in any other world instead of teleporting them there.
     */
    public static List<Pair> pairs(String world, List<Point> points) {
        Objects.requireNonNull(world); Objects.requireNonNull(points);
        List<Pair> result = new ArrayList<>();
        for (int i = 0; i + 1 < points.size(); i += 2) {
            Point first = points.get(i), second = points.get(i + 1);
            if (first == null || second == null) continue;
            if (!world.equals(first.world()) || !world.equals(second.world()))
                throw new IllegalArgumentException("Pair in another world: " + (i + 1));
            result.add(new Pair(i + 1, first, second));
        }
        return List.copyOf(result);
    }

    /** Reservations are atomic; updated point configurations wait until leases are released. */
    public synchronized Result reserve(UUID battle, List<Pair> requested) {
        Objects.requireNonNull(battle); Objects.requireNonNull(requested);
        Lease previous = byBattle.get(battle);
        if (previous != null) return new Result(Status.RESERVED, previous);
        List<Pair> snapshot = List.copyOf(requested);
        if (!valid(snapshot)) return new Result(Status.INVALID, null);
        if (!byBattle.isEmpty() && !config.equals(snapshot)) return new Result(Status.CONFIG_CHANGED, null);
        if (byBattle.isEmpty()) config = snapshot;
        for (Pair pair : config) {
            if (owners.containsKey(pair.firstNumber())) continue;
            Lease lease = new Lease(battle, pair);
            owners.put(pair.firstNumber(), battle);
            byBattle.put(battle, lease);
            return new Result(Status.RESERVED, lease);
        }
        return new Result(Status.FULL, null);
    }

    private boolean valid(List<Pair> pairs) {
        if (pairs.isEmpty()) return false;
        String world = pairs.get(0).first().world();
        Set<Integer> numbers = new HashSet<>();
        for (Pair pair : pairs)
            if (!pair.first().world().equals(world) || !numbers.add(pair.firstNumber())) return false;
        return true;
    }
    public synchronized Optional<Lease> find(UUID battle) { return Optional.ofNullable(byBattle.get(battle)); }
    public synchronized void release(UUID battle) {
        Lease lease = byBattle.remove(Objects.requireNonNull(battle));
        if (lease != null) owners.remove(lease.pair().firstNumber(), battle);
    }
    public synchronized int occupied() { return byBattle.size(); }
}
