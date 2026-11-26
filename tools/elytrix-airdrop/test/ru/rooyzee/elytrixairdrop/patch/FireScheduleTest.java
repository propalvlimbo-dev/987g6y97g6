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
  int count=0;for(int x=-4;x<=4;x++)for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)if(MeteorGeometry.contains(x,y,z)){count++;if(x*x+y*y+z*z>16)throw new AssertionError("not round");}
  if(count<200||count>=257||!MeteorGeometry.contains(0,-4,0)||!MeteorGeometry.contains(0,0,0)||MeteorGeometry.contains(4,4,4))throw new AssertionError("meteor geometry "+count);
  double previous=0;for(int i=1;i<=35;i++){double p=MeteorGeometry.flightProgress(i,35),step=p-previous;if(i>1 && step<((2.0*i-3)/(35*35))-0.000001)throw new AssertionError("flight decelerates");previous=p;}if(previous!=1.0)throw new AssertionError("flight endpoint");
  for(int i=0;i<10000;i++){int wave=PeacefulWave.size();if(wave!=1 && wave!=2)throw new AssertionError("wave size");}
  System.out.println("Fire schedule and 1-2 item peaceful waves OK");
 }
}
