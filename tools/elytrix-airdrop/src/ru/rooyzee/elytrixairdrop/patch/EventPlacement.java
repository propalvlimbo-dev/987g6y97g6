package ru.rooyzee.elytrixairdrop.patch;
import org.bukkit.Location;
import ru.rooyzee.elytrixairdrop.airdrops.Airdrop;
public final class EventPlacement {
 private EventPlacement() {}
 public static Location adjust(Location loc,Airdrop type){return type==Airdrop.PEACEFUL?loc.clone().add(0,2,0):loc;}
}
