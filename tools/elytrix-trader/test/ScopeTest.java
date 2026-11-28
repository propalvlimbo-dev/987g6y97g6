import java.io.File;
import ru.rooyzee.elytrixtrader.patch.TraderScope;
public class ScopeTest {
 public static void main(String[] args){
  File[] f=TraderScope.files(new File[]{new File("experienced.yml"),new File("other.yml"),new File("EXPERIENCED.YML"),null});
  if(f.length!=2 || !f[0].getName().equals("experienced.yml") || !f[1].getName().equals("EXPERIENCED.YML"))throw new AssertionError();
  if(TraderScope.files(null)!=null || TraderScope.allowed(null))throw new AssertionError();
  System.out.println("Trader file filtering OK");
 }
}
