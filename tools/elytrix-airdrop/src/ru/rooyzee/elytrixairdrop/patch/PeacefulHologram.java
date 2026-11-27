package ru.rooyzee.elytrixairdrop.patch;
import org.bukkit.Location;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
/** Anchor the bottom text just above the actual chest, not the schematic origin. */
public final class PeacefulHologram {
 private PeacefulHologram(){}
 public static void place(Main plugin,ActiveAirdrop event,Location chest){
  if(chest==null || chest.getWorld()==null)return;
  plugin.getHologramManager().moveHologram(event.getHologramName(),chest.clone().add(0.5,1.55,0.5));
 }
}
