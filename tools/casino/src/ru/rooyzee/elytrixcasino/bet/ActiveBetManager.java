package ru.rooyzee.elytrixcasino.bet;
import java.util.*;
public final class ActiveBetManager {
    private final Map<UUID,EnumMap<GameType,ActiveBet>> bets = new HashMap<>();
    public synchronized ActiveBet getActiveBet(UUID id) { EnumMap<GameType,ActiveBet> b=bets.get(id); return b==null||b.isEmpty()?null:b.values().iterator().next(); }
    private synchronized ActiveBet get(UUID id,GameType type) { EnumMap<GameType,ActiveBet> b=bets.get(id); return b==null?null:b.get(type); }
    public ActiveBet getAutomateBet(UUID id) { return get(id,GameType.AUTOMATE); }
    public ActiveBet getMinerBet(UUID id) { return get(id,GameType.MINER); }
    public ActiveBet getRouletteBet(UUID id) { return get(id,GameType.ROULETTE); }
    public synchronized boolean hasActiveBet(UUID id) { return getActiveBet(id)!=null; }
    public synchronized boolean tryAcquire(UUID id,GameType type,String machine,long amount) {
        if(id==null||type==null||amount<=0) return false;
        EnumMap<GameType,ActiveBet> b=bets.computeIfAbsent(id,k->new EnumMap<>(GameType.class));
        if(b.containsKey(type)) return false;
        b.put(type,new ActiveBet(id,type,machine,amount)); return true;
    }
    public synchronized void release(UUID id) { bets.remove(id); }
    public synchronized void release(UUID id,GameType type) { EnumMap<GameType,ActiveBet> b=bets.get(id); if(b!=null) { b.remove(type); if(b.isEmpty()) bets.remove(id); } }
    public void releaseAutomate(UUID id) { release(id,GameType.AUTOMATE); }
    public void releaseMiner(UUID id) { release(id,GameType.MINER); }
    public void releaseRoulette(UUID id) { release(id,GameType.ROULETTE); }
    public synchronized void clearAll() { bets.clear(); }
}
