package ru.rooyzee.elytrixcasino.patch;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import dev.by1337.virtualentity.api.virtual.display.VirtualTextDisplay;
import dev.by1337.virtualentity.api.virtual.player.VirtualPlayer;
import dev.by1337.virtualentity.api.entity.EntityAnimation;
import ru.rooyzee.elytrixcasino.roulette.Sector;

/** Non-blocking outcome reaction, then delegate to the original walk-home logic. */
public final class RouletteReactions {
    public interface Actions {
        boolean isSpawned();
        void startHome(Location target,Object mode,Runnable onArrival);
        void dance(int step);
    }
    private static final Map<Actions,Object> pending=new IdentityHashMap<>();
    private static final String[] ZERO={"*улыбается*","*хихикает*","*ехидно улыбнулся*"};
    private static final String[] TWO={"*лицо спокойное*","*подмигнул*","*рад за вас*"};
    private static final String[] THREE={"*надорвался карман*","*лицо слегка недовольно*","*скрежет зубами*"};
    public static String reaction(double multiplier,int random) {
        String[] choices = multiplier==0?ZERO:multiplier==2?TWO:multiplier==3?THREE:null;
        if(choices!=null) return choices[Math.floorMod(random,choices.length)];
        return multiplier==5?"*очень рад за вас*":"";
    }
    public static void cancel(Actions actions) { pending.remove(actions); }
    public static float danceYaw(float base,int step) { return base + (step%2==0 ? 35f : -35f); }
    public static void motion(VirtualPlayer npc,int step) {
        try { if(npc!=null) npc.playAnimation(EntityAnimation.SWING_MAIN_ARM); } catch(Throwable ignored) {}
    }
    public static void schedule(Actions actions,Location target,Object mode,Runnable onArrival,
                                Sector result,Plugin plugin,VirtualTextDisplay display) {
        Object token=new Object(); pending.put(actions,token);
        double mult=result.getMultiplier();
        RouletteHologram.react(display,reaction(mult,ThreadLocalRandom.current().nextInt(3)));
        int delay=mult==5?100:40;
        try {
            if(mult==5) for(int i=0;i<10;i++) {
                final int step=i;
                Bukkit.getScheduler().runTaskLater(plugin,()->{
                    if(pending.get(actions)==token && actions.isSpawned()) try { actions.dance(step); } catch(Throwable ignored) {}
                },1L+i*10L);
            }
            Bukkit.getScheduler().runTaskLater(plugin,()->{
                if(pending.get(actions)!=token) return;
                pending.remove(actions);
                RouletteHologram.clearReaction(display);
                if(actions.isSpawned()) actions.startHome(target,mode,onArrival);
            },delay);
        } catch(Throwable error) {
            // A failed scheduler must not leave a paid round stuck at the wheel.
            if(pending.get(actions)==token) pending.remove(actions);
            RouletteHologram.clearReaction(display);
            if(actions.isSpawned()) actions.startHome(target,mode,onArrival);
        }
    }
}
