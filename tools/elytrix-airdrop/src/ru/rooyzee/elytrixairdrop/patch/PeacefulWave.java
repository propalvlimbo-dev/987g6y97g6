package ru.rooyzee.elytrixairdrop.patch;
import java.util.concurrent.ThreadLocalRandom;
public final class PeacefulWave {
 private PeacefulWave() {}
 public static int size(){return ThreadLocalRandom.current().nextInt(1,3);}
}
