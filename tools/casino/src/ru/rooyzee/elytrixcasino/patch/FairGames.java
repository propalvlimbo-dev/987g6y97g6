package ru.rooyzee.elytrixcasino.patch;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.rooyzee.elytrixcasino.automate.SlotSymbol;
import ru.rooyzee.elytrixcasino.miner.MinerSettings.CoinTier;
import ru.rooyzee.elytrixcasino.roulette.Sector;

public final class FairGames {
    private FairGames() {}
    public static List<Sector> roulette() {
        return Arrays.asList(new Sector(0,63,"&c",Material.RED_WOOL),new Sector(2,24,"&a",Material.LIME_WOOL),new Sector(3,9,"&b",Material.LIGHT_BLUE_WOOL),new Sector(5,4,"&#F8BEFB",Material.GOLD_BLOCK));
    }
    public static List<CoinTier> coins() {
        return Arrays.asList(new CoinTier(Material.SUNFLOWER,"&eМонеты",1.4,5),new CoinTier(Material.GOLD_NUGGET,"&6Самородки",1.4,5),new CoinTier(Material.GOLD_INGOT,"&6Слиток",1.4,5),new CoinTier(Material.EMERALD,"&aИзумруд",1.4,5));
    }
    public static long payout(double amount) { return amount <= 0 ? 0 : (long)Math.floor(amount); }
    public static List<SlotSymbol> symbols(List<SlotSymbol> original) {
        LinkedHashMap<String,SlotSymbol> chosen=new LinkedHashMap<>();
        if(original!=null) for(SlotSymbol s:original) {
            if(s!=null && s.getId()!=null && !chosen.containsKey(s.getId()) && chosen.size()<5)
                chosen.put(s.getId(),new SlotSymbol(s.getId(),s.getMaterial(),1,s.getName(),pay(chosen.size()),pay(chosen.size()),pay(chosen.size())));
        }
        Material[] fallback={Material.ROTTEN_FLESH,Material.BREAD,Material.APPLE,Material.GOLDEN_CARROT,Material.CAKE};
        for(int i=0;chosen.size()<5 && i<fallback.length;i++) {
            String id="elytrix_fallback_"+i;
            chosen.putIfAbsent(id,new SlotSymbol(id,fallback[i],1,"&f"+fallback[i].name(),pay(chosen.size()),pay(chosen.size()),pay(chosen.size())));
        }
        // Never depend on a malformed custom symbol list for the pay table.
        if(chosen.size()<5) throw new IllegalStateException("Not enough slot symbols");
        return Collections.unmodifiableList(new ArrayList<>(chosen.values()));
    }
    private static double pay(int i) { return new double[]{1.2,1.5,2.0,3.0,6.0}[i]; }
    public static boolean skipLore(List<?> lore,Object value) { return false; }
    public static SlotSymbol[][] grid(List<SlotSymbol> symbols,ThreadLocalRandom rng) {
        ArrayList<SlotSymbol> s=new ArrayList<>(symbols);
        Collections.shuffle(s,rng);
        SlotSymbol[][] grid=new SlotSymbol[5][5];
        for(int r=0;r<5;r++) for(int c=0;c<5;c++) grid[r][c]=s.get((2*r+c)%5);
        if(rng.nextBoolean()) {
            int roll=rng.nextInt(100); int index=roll<30?0:roll<64?1:roll<84?2:roll<95?3:4;
            SlotSymbol win=symbols.get(index); int line=rng.nextInt(12);
            for(int i=0;i<5;i++) {
                int row=line<5?line:i;
                int col=line<5?i:line<10?line-5:line==10?i:4-i;
                grid[row][col]=win;
            }
        }
        return grid;
    }
    public static void closeIfViewing(Player p,Inventory inv) { if (viewing(p,inv)) p.closeInventory(); }
    public static boolean viewing(Player p,Inventory inv) { return p.isOnline() && p.getOpenInventory().getTopInventory()==inv; }
}
