package ru.rooyzee.elytrixairdrop.patch;
import java.util.*;
/** Bounded per-player reopen attempts, including failed/interfering inventory opens. */
public final class ReopenGuard {
 private static final int LIMIT=4;
 private static final long WINDOW_MS=10_000;
 private final Map<UUID,ArrayDeque<Long>> attempts=new HashMap<>();
 private final Set<UUID> queued=new HashSet<>(),opening=new HashSet<>();
 public boolean queue(UUID id,long now){
  if(queued.contains(id)||opening.contains(id))return false;
  ArrayDeque<Long> times=attempts.computeIfAbsent(id,k->new ArrayDeque<>());
  while(!times.isEmpty()&&now-times.peekFirst()>=WINDOW_MS)times.removeFirst();
  if(times.size()>=LIMIT)return false;
  times.addLast(now);queued.add(id);return true;
 }
 public void start(UUID id){queued.remove(id);opening.add(id);}
 public void finish(UUID id){opening.remove(id);}
 public void cancel(UUID id){queued.remove(id);}
 public void clear(UUID id){queued.remove(id);opening.remove(id);attempts.remove(id);}
 public void clearAll(){queued.clear();opening.clear();attempts.clear();}
}
