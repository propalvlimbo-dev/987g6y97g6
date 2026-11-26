package ru.rooyzee.elytrixairdrop.patch;
/** Dynamic total and cadence; the number already launched is never reset. */
public final class FireSchedule {
 private FireSchedule() {}
 public static int target(int players){return players<=3?7:players==4?9:players<=6?12:18;}
 public static long intervalMillis(int players,int launched,long remainingMillis){
  int left=Math.max(1,target(players)-launched);
  return Math.max(2000L,Math.min(25000L,Math.max(0L,remainingMillis)/left));
 }
}
