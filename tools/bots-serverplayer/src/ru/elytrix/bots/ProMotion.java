package ru.elytrix.bots;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Pro-only timing refinements. No reach changes, velocity overrides, teleports or anti-cheat bypasses. */
public final class ProMotion {
    private static final Map<UUID,State> states=new HashMap<>();
    private static final class State { long nextSpin; long start=-1; State(long now){ nextSpin=now+120+ThreadLocalRandom.current().nextInt(101); } }
    private ProMotion() {}
    public static void clear(UUID id) { states.remove(id); }
    private static boolean pause(String mode,double distanceSquared,long now,long nextAttack) {
        long remaining=nextAttack-now;
        return "pro".equals(mode) && distanceSquared<=12.25 && (remaining==10 || remaining==9);
    }
    public static float forward(float original,String mode,double distanceSquared,long now,long nextAttack) {
        return pause(mode,distanceSquared,now,nextAttack) ? 0f : original;
    }
    public static boolean sprint(boolean original,String mode,double distanceSquared,long now,long nextAttack) {
        return original && !pause(mode,distanceSquared,now,nextAttack);
    }
    public static void spin(Player bot,Player owner,String mode,long now,long nextAttack,boolean chasing) {
        UUID id=owner.getUniqueId();
        if(!"pro".equals(mode)) { clear(id); return; }
        State state=states.computeIfAbsent(id,ignored->new State(now));
        if(owner.isDead() || bot.isDead() || !bot.getWorld().equals(owner.getWorld())) { state.start=-1; return; }
        Location from=bot.getLocation(), target=owner.getLocation();
        double dx=from.getX()-target.getX(), dz=from.getZ()-target.getZ();
        if(chasing || dx*dx+dz*dz>12.25) { state.start=-1; return; }
        if(state.start<0 && now>=state.nextSpin && nextAttack-now>=8 && nextAttack-now<=11) {
            state.start=now; state.nextSpin=now+180+ThreadLocalRandom.current().nextInt(101);
        }
        if(state.start>=0) {
            long elapsed=now-state.start;
            if(elapsed<0 || elapsed>=8) { state.start=-1; return; }
            // Each tick the ordinary AI first faces the opponent; attacks face it again before hitting.
            bot.setRotation(from.getYaw()+(elapsed+1)*45f,from.getPitch());
        }
    }
}
