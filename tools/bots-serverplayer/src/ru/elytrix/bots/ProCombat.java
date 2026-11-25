package ru.elytrix.bots;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Extra server-side footwork for Pro only; actual attacks still use the original ServerPlayer attack. */
public final class ProCombat {
    private ProCombat() {}
    public static float input(float original,String mode) { return "pro".equals(mode) ? 0f : original; }
    public static double[] movement(double dx,double dz,double vx,double vz,double oldX,double oldZ,int tick,int phase) {
        double distance=Math.hypot(dx,dz); if(distance<0.001) { dx=0; dz=1; distance=1; }
        double px=dx+Math.max(-0.55,Math.min(0.55,vx*2.0));
        double pz=dz+Math.max(-0.55,Math.min(0.55,vz*2.0));
        double norm=Math.hypot(px,pz); if(norm<0.001) {px=dx;pz=dz;norm=distance;}
        double nx=px/norm,nz=pz/norm;
        double toward=distance>2.25?0.25:distance<1.25?-0.16:0.015;
        int direction=(Math.floorDiv(tick+phase,7)&1)==0?1:-1;
        double side=distance<5?0.28*direction:0.06*direction;
        double x=oldX*0.20+nx*toward-nz*side,z=oldZ*0.20+nz*toward+nx*side;
        double speed=Math.hypot(x,z); if(speed>0.39){x*=0.39/speed;z*=0.39/speed;}
        return new double[]{x,z};
    }
    public static void tick(Player bot,Player owner,String mode) {
        if(!"pro".equals(mode) || owner.isDead() || bot.isDead() || !bot.getWorld().equals(owner.getWorld())) return;
        Location from=bot.getLocation(),to=owner.getLocation();
        double dx=to.getX()-from.getX(),dz=to.getZ()-from.getZ();
        if(dx*dx+dz*dz>4096) return;
        int tick=Bukkit.getCurrentTick(),phase=bot.getUniqueId().hashCode()&31;
        // Full visual rotation over eight ticks. The existing AI faces the target again before a real attack.
        bot.setRotation(Math.floorMod(tick+phase,8)*45f,from.getPitch());
        // Leave normal hit knockback alone while the bot has damage immunity ticks.
        if(bot.getNoDamageTicks()>bot.getMaximumNoDamageTicks()/2) return;
        Vector v=bot.getVelocity(),target=owner.getVelocity();
        double[] m=movement(dx,dz,target.getX(),target.getZ(),v.getX(),v.getZ(),tick,phase);
        bot.setVelocity(new Vector(m[0],v.getY(),m[1]));
    }
}
