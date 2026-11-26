package ru.rooyzee.elytrixairdrop.patch;
public class FireScheduleTest {
 public static void main(String[] args){
  if(FireSchedule.target(1)!=7||FireSchedule.target(3)!=7||FireSchedule.target(4)!=9||FireSchedule.target(5)!=12||FireSchedule.target(7)!=18)throw new AssertionError("target");
  long slow=FireSchedule.intervalMillis(3,4,80000),fast=FireSchedule.intervalMillis(5,4,80000);
  if(fast>=slow)throw new AssertionError("more players must accelerate remaining meteors");
  if(FireSchedule.intervalMillis(8,0,1000)!=2000)throw new AssertionError("lower limit");
  if(FireSchedule.intervalMillis(1,0,900000)!=25000)throw new AssertionError("upper limit");
  int launched=0;long last=0,end=182000;
  for(long now=10000;now<end-1500;now+=1000)if(launched<FireSchedule.target(3) && (launched==0 || now-last>=FireSchedule.intervalMillis(3,launched,end-now))){launched++;last=now;}
  if(launched!=7)throw new AssertionError("quiet event count "+launched);
  int before=launched;long lastAt=last;
  for(long now=lastAt+1000;now<end-1500;now+=1000)if(launched<FireSchedule.target(5) && now-last>=FireSchedule.intervalMillis(5,launched,end-now)){launched++;last=now;}
  if(launched<=before)throw new AssertionError("dynamic late increase");
  System.out.println("Fire schedule targets and dynamic acceleration OK");
 }
}
