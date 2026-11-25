import java.util.zip.ZipFile;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import org.objectweb.asm.tree.analysis.*;
public final class ProBytecodeTest {
 static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 static ClassNode read(String jar,String path)throws Exception {try(ZipFile z=new ZipFile(jar)){ClassNode n=new ClassNode();new ClassReader(z.getInputStream(z.getEntry(path)).readAllBytes()).accept(n,0);return n;}}
 static MethodNode method(ClassNode c,String n){return c.methods.stream().filter(m->m.name.equals(n)).findFirst().orElseThrow();}
 public static void main(String[] args)throws Exception {
  ClassNode node=read(args[0],"ru/elytrix/bots/BotsPlugin.class");MethodNode behave=method(node,"behave");new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,behave);
  int forward=0,spin=0,sprint=0,attack=0,noobDelay=0,proDelay=0;boolean noobMiss=false,cooldown=false,visible=false;
  for(AbstractInsnNode i:behave.instructions){
   if(i instanceof MethodInsnNode m){if(m.owner.endsWith("/ProMotion")&&m.name.equals("forward"))forward++;if(m.owner.endsWith("/ProMotion")&&m.name.equals("spin"))spin++;if(m.owner.endsWith("/ProMotion")&&m.name.equals("sprint"))sprint++;if(m.owner.endsWith("/TrainingPlayer")&&m.name.equals("attack"))attack++;if(m.name.equals("getAttackStrengthScale"))cooldown=true;if(m.name.equals("hasLineOfSight"))visible=true;}
   if(i instanceof IntInsnNode n&&n.operand==26)noobDelay++;if(i instanceof IntInsnNode n&&n.operand==11)proDelay++;
   if(i instanceof LdcInsnNode n&&Double.valueOf(0.35).equals(n.cst))noobMiss=true;
  }
  check(forward==1&&spin==1&&sprint==1&&attack==1&&cooldown&&visible,"hooks retain ordinary attack, cooldown and sight");check(noobDelay==1&&proDelay==1&&noobMiss,"mode timing unchanged");
  ClassNode helper=read(args[0],"ru/elytrix/bots/ProMotion.class");for(MethodNode m:helper.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode call)check(!call.name.equals("setVelocity")&&!call.name.equals("teleport"),"no forced speed or teleport");
  ClassNode arena=read(args[0],"ru/elytrix/bots/TrainingArena.class");boolean sword=false,crit=false;
  for(AbstractInsnNode i:method(arena,"trainingSword").instructions)if(i instanceof MethodInsnNode m&&m.name.equals("setUnbreakable"))sword=true;
  for(AbstractInsnNode i:method(arena,"privateHitSound").instructions)if(i instanceof FieldInsnNode f&&f.name.equals("ENTITY_PLAYER_ATTACK_CRIT"))crit=true;
  check(sword&&crit,"unbreakable sword and critical sound retained");System.out.println("Verified pro hooks, vanilla attack, noob and prior features");
 }
}
