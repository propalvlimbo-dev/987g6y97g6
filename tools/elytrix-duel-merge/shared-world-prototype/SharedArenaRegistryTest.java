package ru.elytrix.duelsbridge.shared;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static ru.elytrix.duelsbridge.shared.SharedArenaRegistry.*;

public class SharedArenaRegistryTest {
    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
    static Point p(double x) { return new Point("duel",x,101,0,0,0); }
    public static void main(String[] args) throws Exception {
        List<Point> points=Arrays.asList(p(0),p(1),p(2),p(3),p(4),p(5),p(6),p(7));
        List<Pair> pairs=SharedArenaRegistry.pairs("duel",points);
        check(pairs.size()==4 && pairs.get(3).firstNumber()==7,"eight points");
        List<Pair> gaps=SharedArenaRegistry.pairs("duel",Arrays.asList(p(0),p(1),null,p(3),p(4),p(5)));
        check(gaps.size()==2 && gaps.get(1).firstNumber()==5,"gaps must not shift numbering");
        try { SharedArenaRegistry.pairs("duel",List.of(p(0),new Point("other",0,0,0,0,0))); throw new AssertionError("wrong world accepted"); }
        catch (IllegalArgumentException expected) { }
        SharedArenaRegistry r=new SharedArenaRegistry(); UUID[] ids=new UUID[5];
        for(int i=0;i<ids.length;i++)ids[i]=UUID.randomUUID();
        for(int i=0;i<4;i++)check(r.reserve(ids[i],pairs).lease().pair().firstNumber()==1+i*2,"slot order");
        check(r.reserve(ids[0],gaps).status()==Status.RESERVED,"existing lease is stable");
        check(r.reserve(ids[4],pairs).status()==Status.FULL,"full");
        check(r.reserve(ids[4],gaps).status()==Status.CONFIG_CHANGED,"reject config changes during battle");
        r.release(ids[2]);check(r.reserve(ids[4],pairs).lease().pair().firstNumber()==5,"reuse");
        r.release(ids[2]);check(r.occupied()==4,"stale release");
        for(UUID id:ids)r.release(id);
        check(r.reserve(ids[0],gaps).lease().pair().firstNumber()==1,"allow reconfiguration after drain");
        check(new SharedArenaRegistry().reserve(ids[0],List.of()).status()==Status.INVALID,"no pairs");
        SharedArenaRegistry concurrent=new SharedArenaRegistry();
        ExecutorService pool=Executors.newFixedThreadPool(16);CountDownLatch gate=new CountDownLatch(1);
        AtomicInteger success=new AtomicInteger();List<Future<?>> futures=new ArrayList<>();
        for(int i=0;i<64;i++)futures.add(pool.submit(()->{try{gate.await();}catch(InterruptedException ex){throw new RuntimeException(ex);} if(concurrent.reserve(UUID.randomUUID(),pairs).status()==Status.RESERVED)success.incrementAndGet();}));
        gate.countDown();for(Future<?> f:futures)f.get();pool.shutdown();
        check(success.get()==4 && concurrent.occupied()==4,"atomic reservations");
        System.out.println("Shared arena registry tests passed");
    }
}
