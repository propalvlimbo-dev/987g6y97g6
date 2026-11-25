import ru.elytrix.bots.ProCombat;
public final class ProCombatTest {
 static void check(boolean value,String text) { if(!value) throw new AssertionError(text); }
 public static void main(String[] args) {
  check(ProCombat.input(0.65f,"noob")==0.65f,"noob input unchanged");
  check(ProCombat.input(0.25f,"combo")==0.25f && ProCombat.input(1f,"custom")==1f,"other modes unchanged");
  check(ProCombat.input(1f,"pro")==0f,"pro uses custom footwork");
  double[] pursuit=ProCombat.movement(0,4,0,0,0,0,0,0);
  check(pursuit[1]>0 && Math.abs(pursuit[0])>0,"chase with strafe");
  double[] reverse=ProCombat.movement(0,4,0,0,0,0,7,0);
  check(pursuit[0]*reverse[0]<0,"evasion switches directions");
  double[] retreat=ProCombat.movement(0,0.8,0,0,0,0,0,0);
  check(retreat[1]<0,"retreat at close range");
  double[] speed=ProCombat.movement(1,1,100,-100,9,9,0,0);
  check(Math.hypot(speed[0],speed[1])<=0.390001,"bounded speed");
  double[] zero=ProCombat.movement(0,0,0,0,0,0,0,0);
  check(Double.isFinite(zero[0]) && Double.isFinite(zero[1]),"zero-distance safe");
  System.out.println("Pro pursuit, evasion, speed and mode isolation passed");
 }
}
