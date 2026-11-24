package dev.enco.greatcombat.api.events;
import org.bukkit.entity.Player;import org.bukkit.event.*;
public final class CombatPreStartEvent extends Event implements Cancellable {
 private static final HandlerList handlers=new HandlerList(); private boolean cancelled; private final Player damager,target;
 public CombatPreStartEvent(Player a,Player b){damager=a;target=b;} public Player getDamager(){return damager;} public Player getTarget(){return target;}
 public boolean isCancelled(){return cancelled;} public void setCancelled(boolean c){cancelled=c;} public HandlerList getHandlers(){return handlers;} public static HandlerList getHandlerList(){return handlers;}
}
