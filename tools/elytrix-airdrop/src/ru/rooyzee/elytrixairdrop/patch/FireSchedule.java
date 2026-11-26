package ru.rooyzee.elytrixairdrop.patch;
/** Dynamic total and cadence; the number already launched is never reset. */
public final class FireSchedule {
 private FireSchedule() {}
 public static int target(int players){return players<=3?14:players==4?18:players<=6?24:36;}
 public static boolean carriesLoot(int index,boolean firstInPair){return (index%2==0)==firstInPair;}
 public static int cooledBlocks(int total,long elapsed,long duration){if(duration<=0)throw new IllegalArgumentException();return (int)((long)total*Math.max(0L,Math.min(duration,elapsed))/duration);}
 public static long intervalMillis(int players,int launched,long remainingMillis){
  int left=Math.max(1,target(players)-launched);
  return Math.max(2000L,Math.min(25000L,Math.max(0L,remainingMillis)/left));
 }
}
