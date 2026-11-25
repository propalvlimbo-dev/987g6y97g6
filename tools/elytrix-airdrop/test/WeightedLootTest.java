import java.util.*;
import ru.rooyzee.elytrixairdrop.patch.WeightedLoot;
public class WeightedLootTest {
 public static void main(String[] args){
  List<List<String>> tiers=Arrays.asList(Arrays.asList("sword","shovel","shovel","pearl","helm","axe"),Arrays.asList("rare1","rare2"),Arrays.asList("epic"),Arrays.asList("legend"));
  for(int seed=0;seed<1000;seed++){
   List<String> out=WeightedLoot.draw(tiers,8,0,new Random(seed),String::equals);
   if(out.size()!=8 || new HashSet<>(out).size()!=8)throw new AssertionError("duplicates "+out);
   if(out.stream().noneMatch(x->x.startsWith("rare")||x.equals("epic")||x.equals("legend")))throw new AssertionError("no rare "+out);
  }
  List<List<String>> one=Arrays.asList(Arrays.asList("shovel"),Collections.emptyList(),Collections.emptyList(),Collections.emptyList());
  if(WeightedLoot.draw(one,10,0,new Random(1),String::equals).size()!=10)throw new AssertionError("small pool");
  System.out.println("WeightedLoot: variety, rare categories and small pool OK");
 }
}
