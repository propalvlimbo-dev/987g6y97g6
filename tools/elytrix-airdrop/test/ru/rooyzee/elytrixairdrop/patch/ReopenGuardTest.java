package ru.rooyzee.elytrixairdrop.patch;
import java.util.UUID;
public class ReopenGuardTest {
 public static void main(String[] args){
  ReopenGuard g=new ReopenGuard();UUID a=UUID.randomUUID(),b=UUID.randomUUID();
  if(!g.queue(a,1000)||g.queue(a,1000))throw new AssertionError("only one queued task");
  g.start(a);if(g.queue(a,1001))throw new AssertionError("reentrant close");g.finish(a);
  for(int i=1;i<4;i++){if(!g.queue(a,1000+i*200))throw new AssertionError("retry "+i);g.start(a);g.finish(a);}
  if(g.queue(a,1800))throw new AssertionError("reopen loop must be bounded");
  if(!g.queue(b,1800))throw new AssertionError("player isolation");g.cancel(b);
  if(!g.queue(a,11000))throw new AssertionError("window expiry");g.clear(a);
  if(!g.queue(a,11001))throw new AssertionError("quit cleanup");g.clearAll();
  System.out.println("Peaceful reopen guard: per-player, queued, reentrant, rate-limited, cleanup OK");
 }
}
