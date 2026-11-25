package ru.rooyzee.elytrixairdrop.patch;
import java.util.*;
import java.util.function.BiPredicate;
/** Weighted category selection with no identical prize until the available pool is exhausted. */
public final class WeightedLoot {
 private WeightedLoot() {}
 public static <T> List<T> draw(List<List<T>> source, int amount, int nearby, Random random, BiPredicate<T,T> same){
  List<List<T>> pool=copy(source); List<T> result=new ArrayList<>();
  int[] weights=nearby<=3?new int[]{65,24,9,2}:nearby<=6?new int[]{55,27,14,4}:new int[]{45,30,18,7};
  boolean bonus=amount>=3 && hasRare(pool);
  for(int i=0;i<amount;i++){
   if(empty(pool))pool=copy(source);
   if(empty(pool))break;
   int first=(bonus && hasRare(pool))?1:0; bonus=false;
   int total=0;for(int c=first;c<4;c++)if(!pool.get(c).isEmpty())total+=weights[c];
   if(total==0){first=0;for(int c=0;c<4;c++)if(!pool.get(c).isEmpty())total+=weights[c];}
   int roll=random.nextInt(total), category=first;
   for(int c=first;c<4;c++){if(pool.get(c).isEmpty())continue; if(roll<weights[c]){category=c;break;} roll-=weights[c];}
   List<T> list=pool.get(category);T picked=list.get(random.nextInt(list.size()));result.add(picked);
   for(List<T> tier:pool)tier.removeIf(item->same.test(item,picked));
  }
  Collections.shuffle(result,random);return result;
 }
 private static <T> boolean hasRare(List<List<T>> p){return !p.get(1).isEmpty()||!p.get(2).isEmpty()||!p.get(3).isEmpty();}
 private static <T> boolean empty(List<List<T>> p){for(List<T> l:p)if(!l.isEmpty())return false;return true;}
 private static <T> List<List<T>> copy(List<List<T>> source){List<List<T>> p=new ArrayList<>();for(int i=0;i<4;i++)p.add(new ArrayList<>(source.get(i)));return p;}
}
