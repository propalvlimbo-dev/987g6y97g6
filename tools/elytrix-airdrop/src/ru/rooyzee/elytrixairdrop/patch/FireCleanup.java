package ru.rooyzee.elytrixairdrop.patch;
import org.bukkit.entity.Entity;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.airdrops.Airdrop;
/** Fire owns and removes its displays itself; leave dropped items and unrelated entities untouched. */
public final class FireCleanup {
 private FireCleanup() {}
 public static void cleanup(Entity entity,ActiveAirdrop active){if(active.getType()!=Airdrop.FIRE)entity.remove();}
}
