package ru.rooyzee.elytrixairdrop.patch;
import org.bukkit.util.Vector;
public class SkyMechanicsTest {
 public static void main(String[] args){
  check(SkyMechanics.outward(1,5,0,9,0,9),-1,0);
  check(SkyMechanics.outward(9,5,0,9,0,9),1,0);
  check(SkyMechanics.outward(5,1,0,9,0,9),0,-1);
  check(SkyMechanics.outward(5,9,0,9,0,9),0,1);
  System.out.println("Sky mechanics: outward direction and bounded velocity OK");
 }
 static void check(Vector v,int x,int z){if(Math.signum(v.getX())!=x||Math.signum(v.getZ())!=z||v.getY()>=0||v.length()>4)throw new AssertionError(v.toString());}
}
