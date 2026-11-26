package ru.rooyzee.elytrixairdrop.patch;
public class FireScheduleTest {
 public static void main(String[] args){
  if(FireSchedule.target(1)!=14||FireSchedule.target(3)!=14||FireSchedule.target(4)!=18||FireSchedule.target(5)!=24||FireSchedule.target(7)!=36)throw new AssertionError("target");
  long slow=FireSchedule.intervalMillis(3,4,80000),fast=FireSchedule.intervalMillis(5,4,80000);
  if(fast>=slow)throw new AssertionError("more players must accelerate remaining meteors");
  if(FireSchedule.intervalMillis(8,0,1000)!=2000)throw new AssertionError("lower limit");
  if(FireSchedule.intervalMillis(1,0,900000)!=25000)throw new AssertionError("upper limit");
  int launched=0;long last=0,end=182000;
  for(long now=10000;now<end-3500;now+=1000)if(launched<FireSchedule.target(3) && (launched==0 || now-last>=FireSchedule.intervalMillis(3,launched,end-now))){launched++;last=now;}
  if(launched!=14)throw new AssertionError("quiet event count "+launched);
  int before=launched;long lastAt=last;
  for(long now=lastAt+1000;now<end-3500;now+=1000)if(launched<FireSchedule.target(5) && now-last>=FireSchedule.intervalMillis(5,launched,end-now)){launched++;last=now;}
  if(launched<=before)throw new AssertionError("dynamic late increase");
  if(FireSchedule.cooledBlocks(122,0,180000)!=0 || FireSchedule.cooledBlocks(122,90000,180000)!=61 || FireSchedule.cooledBlocks(122,180000,180000)!=122)throw new AssertionError("cooling");
  for(int i=0;i<100;i+=2){if(FireSchedule.carriesLoot(i,true)==FireSchedule.carriesLoot(i+1,true)||FireSchedule.carriesLoot(i,false)==FireSchedule.carriesLoot(i+1,false))throw new AssertionError("loot pairing");}
  for(int i=0;i<10000;i++){int wave=PeacefulWave.size();if(wave!=1 && wave!=2)throw new AssertionError("wave size");}
  System.out.println("Fire schedule and 1-2 item peaceful waves OK");
 }
}
