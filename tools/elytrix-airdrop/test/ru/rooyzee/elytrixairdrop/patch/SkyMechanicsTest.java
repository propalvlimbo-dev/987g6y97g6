package ru.rooyzee.elytrixairdrop.patch;
import java.util.Random;
public class SkyMechanicsTest {
 public static void main(String[] args){
  Random random=new Random(12); boolean ten=false, thirty=false;
  for(int i=0;i<10000;i++){
   long delay=LaunchMath.delayMillis(random);double y=LaunchMath.upwardVelocity(random);
   if(delay<10000||delay>30000||delay%1000!=0||y<0.95||y>=1.05)throw new AssertionError(delay+" "+y);
   ten|=delay==10000;thirty|=delay==30000;
  }
  if(!ten||!thirty)throw new AssertionError("endpoints unreachable");
  System.out.println("Sky launch: 10–30 second interval and vertical velocity OK");
 }
}
