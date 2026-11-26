package ru.rooyzee.elytrixairdrop.patch;
import java.util.Random;
public class SkyMechanicsTest {
 public static void main(String[] args){
  Random random=new Random(12); boolean six=false, fifteen=false;
  for(int i=0;i<10000;i++){
   long delay=LaunchMath.delayMillis(random);double y=LaunchMath.upwardVelocity(random);
   if(delay<6000||delay>15000||delay%1000!=0||y<1.05||y>=1.15)throw new AssertionError(delay+" "+y);
   six|=delay==6000;fifteen|=delay==15000;
  }
  if(!six||!fifteen)throw new AssertionError("endpoints unreachable");
  System.out.println("Sky launch: 6–15 second interval and vertical velocity OK");
 }
}
