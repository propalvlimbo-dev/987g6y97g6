package ru.rooyzee.elytrixairdrop.patch;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.airdrops.types.ActiveFireAirdrop;
/** Narrow exception to the legacy blanket region ban. FireArena validates the final destination. */
public final class FirePearls {
 private FirePearls(){}
 private static boolean participant(Main plugin,Player player){
  ActiveAirdrop active=plugin.getAirdropManager().getActive();
  return active instanceof ActiveFireAirdrop && ((ActiveFireAirdrop)active).isArenaParticipant(player);
 }
 public static boolean allowThrow(Main plugin,PlayerInteractEvent event){
  return event.getItem()!=null && event.getItem().getType()==Material.ENDER_PEARL && participant(plugin,event.getPlayer());
 }
 public static boolean allowTeleport(Main plugin,PlayerTeleportEvent event){
  return event.getCause()==PlayerTeleportEvent.TeleportCause.ENDER_PEARL && participant(plugin,event.getPlayer());
 }
}
