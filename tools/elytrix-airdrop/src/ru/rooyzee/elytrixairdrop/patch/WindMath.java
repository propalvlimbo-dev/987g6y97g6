package ru.rooyzee.elytrixairdrop.patch;

/** Horizontal impulse and common, changing direction for the sky event. */
public final class WindMath {
 public static final double IMPULSE=0.025;
 public static final double MAX_ALONG_WIND=0.14;
 private WindMath() {}
 public static int nextDirection(int current,int randomOffset){
  if(current<0 || current>=8 || randomOffset<1 || randomOffset>7)throw new IllegalArgumentException();
  return (current+randomOffset)%8;
 }
 public static double directionX(int index){return Math.cos(index*Math.PI/4.0);}
 public static double directionZ(int index){return Math.sin(index*Math.PI/4.0);}
 /** Only adds wind when the player is below the along-wind limit; never slows or overwrites other velocity. */
 public static double impulse(double vx,double vz,double dx,double dz){
  double projection=vx*dx+vz*dz;
  return Math.max(0,Math.min(IMPULSE,MAX_ALONG_WIND-projection));
 }
}
