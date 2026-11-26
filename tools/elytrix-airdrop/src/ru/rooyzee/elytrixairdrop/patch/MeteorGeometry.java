package ru.rooyzee.elytrixairdrop.patch;

/** Shared, deterministic voxel outline for both the flight and the landed meteor. */
public final class MeteorGeometry {
 public static final int RADIUS=4;
 private MeteorGeometry(){}
 public static boolean contains(int x,int y,int z){
  int d=x*x+y*y+z*z;
  if(d>RADIUS*RADIUS)return false;
  if(x==0&&z==0)return true; // Keep top and ground contact.
  if(d<12)return true;
  int hash=(x*73428767)^(y*912931)^(z*438289);
  hash^=hash>>>16;
  return (hash&7)!=0;
 }
 /** Accelerating throughout the flight; no easing out on impact. */
 public static double flightProgress(int age,int duration){
  double t=Math.max(0.0,Math.min(1.0,age/(double)duration));
  return t*t;
 }
}
