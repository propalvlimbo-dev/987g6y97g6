import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Material;
import dev.by1337.virtualentity.api.virtual.display.VirtualTextDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.*;
import ru.rooyzee.elytrixcasino.bet.*;
import ru.rooyzee.elytrixcasino.automate.SlotSymbol;
import ru.rooyzee.elytrixcasino.patch.*;
import ru.rooyzee.elytrixcasino.roulette.Sector;
import ru.rooyzee.elytrixcasino.miner.MinerSettings.CoinTier;
public class CasinoTests {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 static int lines(SlotSymbol[][] b){int n=0;for(int l=0;l<12;l++){String id=null;boolean same=true;for(int i=0;i<5;i++){int r=l<5?l:i,c=l<5?i:l<10?l-5:l==10?i:4-i;String x=b[r][c].getId();if(id==null)id=x;else if(!id.equals(x))same=false;}if(same)n++;}return n;}
 static class P implements Player {boolean online=true,closed=false; Inventory top;UUID id=UUID.randomUUID();public UUID getUniqueId(){return id;}public boolean isOnline(){return online;}public InventoryView openInventory(Inventory inventory){top=inventory;return ()->top;}public InventoryView getOpenInventory(){return ()->top;}public void closeInventory(){closed=true;top=null;}}
 static class Spin extends BukkitRunnable implements SpinRegistry.Finishable {int finished;final P p;final String machine;final Inventory inv;Spin(P p,String machine,Inventory inv){this.p=p;this.machine=machine;this.inv=inv;}public UUID spinPlayer(){return p.id;}public String spinMachine(){return machine;}public Inventory spinInventory(){return inv;}public void run(){} public BukkitTask runTaskTimer(Plugin p,long d,long t){return new BukkitTask(){};}public void finishNow(){finished++;}}
 enum State {IDLE,WALKING,SPINNING,RETURNING}
 static class Display implements VirtualTextDisplay {int updates;String text;public void setText(Component c){updates++;text=c.contents();}}
 static class Actions implements RouletteReactions.Actions {boolean spawned=true;int dances,returns;public boolean isSpawned(){return spawned;}public void startHome(org.bukkit.Location l,Object mode,Runnable end){returns++;end.run();}public void dance(int step){dances++;}}
 static class TestScheduler implements BukkitScheduler {
   final TreeMap<Long,List<Runnable>> tasks=new TreeMap<>();boolean fail;
   public BukkitTask runTaskLater(Plugin plugin,Runnable task,long delay){if(fail)throw new IllegalStateException("scheduler unavailable");tasks.computeIfAbsent(delay,k->new ArrayList<>()).add(task);return new BukkitTask(){};}
   void through(long tick){while(!tasks.isEmpty()&&tasks.firstKey()<=tick)for(Runnable r:tasks.pollFirstEntry().getValue())r.run();}
 }
 public static void main(String[] args)throws Exception {
  UUID id=UUID.randomUUID();ActiveBetManager m=new ActiveBetManager();
  for(GameType type:GameType.values())check(m.tryAcquire(id,type,"same",100),"parallel modes");
  check(!m.tryAcquire(id,GameType.AUTOMATE,"other",100),"duplicate spin");
  m.releaseMiner(id);check(m.getMinerBet(id)==null&&m.getAutomateBet(id)!=null&&m.getRouletteBet(id)!=null,"isolated release");
  UUID other=UUID.randomUUID();check(m.tryAcquire(other,GameType.AUTOMATE,"same",100),"shared physical machine");
  m.releaseAutomate(id);check(m.getAutomateBet(other)!=null,"other player remains");
  UUID race=UUID.randomUUID();AtomicInteger wins=new AtomicInteger();ExecutorService pool=Executors.newFixedThreadPool(12);for(int i=0;i<1000;i++)pool.submit(()->{if(m.tryAcquire(race,GameType.MINER,"m",100))wins.incrementAndGet();});pool.shutdown();check(pool.awaitTermination(10,TimeUnit.SECONDS)&&wins.get()==1,"atomic acquire");
  List<SlotSymbol> original=new ArrayList<>();for(int i=0;i<10;i++)original.add(new SlotSymbol("s"+i,Material.APPLE,99,"test",999,999,999));
  List<SlotSymbol> symbols=FairGames.symbols(original);check(symbols.size()==5&&symbols.get(0).pay(5)==1.2&&symbols.get(4).pay(5)==6.0,"slot payouts override config");
  int winners=0; double paid=0;for(int i=0;i<200000;i++){SlotSymbol[][] grid=FairGames.grid(symbols,ThreadLocalRandom.current());int n=lines(grid);check(n==0||n==1,"unintended multiple line: "+n);winners+=n;if(n==1) for(int l=0;l<12;l++){int row=l<5?l:0,col=l<5?0:l<10?l-5:l==10?0:4;String symbolId=grid[row][col].getId();boolean same=true;for(int x=1;x<5;x++){int r=l<5?l:x,c=l<5?x:l<10?l-5:l==10?x:4-x;if(!grid[r][c].getId().equals(symbolId))same=false;}if(same)paid+=grid[row][col].pay(5);}}
  check(paid/200000>0.93&&paid/200000<0.97,"slot RTP "+paid/200000);
  check(winners>97000&&winners<103000,"slot 50 percent");
  List<Sector> sectors=FairGames.roulette();double rtp=0;int weight=0;for(Sector s:sectors){weight+=s.getWeight();rtp+=s.getMultiplier()*s.getWeight();}check(weight==100&&rtp==95,"roulette RTP");
  int coins=0;for(CoinTier t:FairGames.coins()){coins+=t.getCount();check(t.getMultiplier()==1.4,"miner reward");check(t.rewardForBet(1)==1&&t.rewardForBet(10)==14,"rounding");}check(coins==20,"miner board");
  P p=new P();Inventory a=new Inventory(){}, b=new Inventory(){};p.top=a;check(FairGames.viewing(p,a)&&!FairGames.viewing(p,b),"sound exact inventory");FairGames.closeIfViewing(p,b);check(!p.closed,"don't close different game");FairGames.closeIfViewing(p,a);check(p.closed,"close on shutdown");
  P otherPlayer=new P(); Spin s1=new Spin(p,"machine-A",a),s2=new Spin(otherPlayer,"machine-A",b);
  SpinRegistry.schedule(s1,null,1,1);SpinRegistry.schedule(s2,null,1,1);
  check(!SpinRegistry.reopen(p,"machine-B"),"other machine should not show old spin");
  check(SpinRegistry.reopen(p,"machine-A")&&p.top==a,"reopen own spin");
  check(SpinRegistry.reopen(otherPlayer,"machine-A")&&otherPlayer.top==b,"personal inventory");
  SpinRegistry.done(s1);check(!SpinRegistry.reopen(p,"machine-A"),"finished spin removed");
  check(SpinRegistry.reopen(otherPlayer,"machine-A"),"other player still spinning");
  SpinRegistry.finishAll();SpinRegistry.finishAll();check(s1.finished==0&&s2.finished==1,"finish once");
  Display display=new Display();List<String> defaults=Arrays.asList("default","second line");
  RouletteHologram.update(display,State.IDLE,defaults);check(display.text.equals("default\nsecond line"),"default hologram");
  RouletteHologram.update(display,State.WALKING,defaults);check(display.text.equals("&f*идёт крутить рулетку*"),"walking text");
  int updates=display.updates;RouletteHologram.update(display,State.WALKING,defaults);check(display.updates==updates,"no repeated update");
  RouletteHologram.update(display,State.SPINNING,defaults);check(display.text.equals("&f*крутит рулетку*"),"spinning text");
  RouletteHologram.watch(display);RouletteHologram.update(display,State.SPINNING,defaults);check(display.text.equals("&f*следит за рулеткой*"),"watching text");
  RouletteHologram.update(display,State.RETURNING,defaults);check(display.text.isEmpty(),"return hides hologram");
  RouletteHologram.update(display,State.IDLE,defaults);check(display.text.equals("default\nsecond line"),"restore default");
  check(RouletteReactions.reaction(0,0).equals("*улыбается*") && RouletteReactions.reaction(0,1).equals("*хихикает*") && RouletteReactions.reaction(0,2).equals("*ехидно улыбнулся*"),"zero reactions");
  check(RouletteReactions.reaction(2,0).equals("*лицо спокойное*") && RouletteReactions.reaction(2,1).equals("*подмигнул*") && RouletteReactions.reaction(2,2).equals("*рад за вас*"),"two reactions");
  check(RouletteReactions.reaction(3,0).equals("*надорвался карман*") && RouletteReactions.reaction(3,1).equals("*лицо слегка недовольно*") && RouletteReactions.reaction(3,2).equals("*скрежет зубами*"),"three reactions");
  TestScheduler scheduler=new TestScheduler();org.bukkit.Bukkit.scheduler=scheduler;Actions action=new Actions();int[] arrived={0};
  RouletteReactions.schedule(action,new org.bukkit.Location(),new Object(),()->arrived[0]++,new Sector(5,1,"",Material.GOLD_BLOCK),null,display);
  RouletteHologram.update(display,State.RETURNING,defaults);check(display.text.equals("&f*очень рад за вас*"),"jackpot reaction");
  scheduler.through(99);check(action.dances==10&&action.returns==0,"dance five seconds before return");
  scheduler.through(100);check(action.returns==1&&arrived[0]==1,"return after dance");
  RouletteHologram.update(display,State.RETURNING,defaults);check(display.text.isEmpty(),"hide during walk home");
  scheduler=new TestScheduler();org.bukkit.Bukkit.scheduler=scheduler;Actions standard=new Actions();
  RouletteReactions.schedule(standard,new org.bukkit.Location(),new Object(),()->{},new Sector(3,1,"",Material.LIME_WOOL),null,display);
  RouletteHologram.update(display,State.RETURNING,defaults);check(display.text.startsWith("&f*"),"result reaction shown");
  scheduler.through(39);check(standard.returns==0&&standard.dances==0,"normal reaction waits");scheduler.through(40);check(standard.returns==1,"normal result returns home");
  VirtualTextDisplay broken=c->{throw new RuntimeException("unavailable");};RouletteHologram.update(broken,State.RETURNING,defaults);
  scheduler=new TestScheduler();org.bukkit.Bukkit.scheduler=scheduler;Actions cancelled=new Actions();
  RouletteReactions.schedule(cancelled,new org.bukkit.Location(),new Object(),()->{},new Sector(0,1,"",Material.RED_WOOL),null,display);
  RouletteReactions.cancel(cancelled);scheduler.through(100);check(cancelled.returns==0,"no stale return after despawn");
  scheduler=new TestScheduler();scheduler.fail=true;org.bukkit.Bukkit.scheduler=scheduler;Actions fallback=new Actions();
  RouletteReactions.schedule(fallback,new org.bukkit.Location(),new Object(),()->{},new Sector(2,1,"",Material.LIME_WOOL),null,display);check(fallback.returns==1,"return if scheduling fails");
  System.out.println("Casino isolated tests passed; slot winners="+winners+"/200000");
 }
}
