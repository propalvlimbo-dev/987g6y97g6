package ru.rooyzee.elytrixairdrop.patch;
import java.util.Random;
public final class LaunchMath {
 private LaunchMath() {}
 public static long delayMillis(Random random){return (10+random.nextInt(21))*1000L;}
 public static double upwardVelocity(Random random){return 0.95+random.nextDouble()*0.10;}
}
