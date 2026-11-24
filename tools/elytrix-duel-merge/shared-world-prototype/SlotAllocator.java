package ru.elytrix.duelsbridge.shared;

import java.util.*;

/**
 * Allocation prototype only. Not wired into the plugin or packaged in its JAR.
 * All operations must run on the server thread. Pair indices are zero-based
 * positions in the ordered list of configured points.
 */
public final class SlotAllocator {
    private List<Integer> configured = List.of();
    private final Map<UUID, Integer> battles = new HashMap<>();
    private final Map<Integer, UUID> owners = new HashMap<>();

    /** Configuration changes are refused until all ongoing battles are released. */
    public boolean configure(List<Boolean> present) {
        Objects.requireNonNull(present);
        if (!battles.isEmpty()) return false;
        List<Integer> pairs = new ArrayList<>();
        for (int i = 0; i + 1 < present.size(); i += 2) {
            if (Boolean.TRUE.equals(present.get(i)) && Boolean.TRUE.equals(present.get(i + 1))) pairs.add(i);
        }
        configured = List.copyOf(pairs);
        return true;
    }

    /** Idempotently reserve a complete pair or return empty if all are occupied. */
    public OptionalInt reserve(UUID battleId) {
        Objects.requireNonNull(battleId);
        Integer existing = battles.get(battleId);
        if (existing != null) return OptionalInt.of(existing);
        for (int pair : configured) {
            if (owners.containsKey(pair)) continue;
            owners.put(pair, battleId);
            battles.put(battleId, pair);
            return OptionalInt.of(pair);
        }
        return OptionalInt.empty();
    }

    /** Release by battle identity; a repeated or stale release cannot free a new owner. */
    public void release(UUID battleId) {
        Integer pair = battles.remove(Objects.requireNonNull(battleId));
        if (pair != null) owners.remove(pair, battleId);
    }
}
