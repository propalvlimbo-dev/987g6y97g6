package ru.rooyzee.elytrixairdrop.patch;
import java.util.Random;
public final class LaunchMath {
 private LaunchMath() {}
 public static long delayMillis(Random random){return (6+random.nextInt(10))*1000L;}
 public static double upwardVelocity(Random random){return 1.05+random.nextDouble()*0.10;}
}
