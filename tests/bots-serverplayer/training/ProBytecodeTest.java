import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

public final class ProBytecodeTest {
 static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 public static void main(String[] args)throws Exception {
  byte[] bytes;
  try(ZipFile z=new ZipFile(args[0])) { bytes=z.getInputStream(z.getEntry("ru/elytrix/bots/BotsPlugin.class")).readAllBytes(); }
  ClassNode node=new ClassNode(); new ClassReader(bytes).accept(node,0);
  MethodNode behave=node.methods.stream().filter(m->m.name.equals("behave")).findFirst().orElseThrow();
  new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,behave);
  int inputs=0,footwork=0,attack=0,noobDelay=0; boolean noobMiss=false;
  for(AbstractInsnNode i:behave.instructions) {
   if(i instanceof MethodInsnNode m) {
    if(m.owner.equals("ru/elytrix/bots/ProCombat") && m.name.equals("input")) inputs++;
    if(m.owner.equals("ru/elytrix/bots/ProCombat") && m.name.equals("tick")) footwork++;
    if(m.owner.equals("ru/elytrix/bots/TrainingPlayer") && m.name.equals("attack")) attack++;
   }
   if(i instanceof IntInsnNode n && n.operand==26) noobDelay++;
   if(i instanceof LdcInsnNode n && Double.valueOf(0.35).equals(n.cst)) noobMiss=true;
  }
  check(inputs==2 && footwork==1 && attack==1,"pro hooks and original server attack");
  check(noobDelay==1 && noobMiss,"noob timing and miss probability remain");
  byte[] arenaBytes; try(ZipFile z=new ZipFile(args[0])) { arenaBytes=z.getInputStream(z.getEntry("ru/elytrix/bots/TrainingArena.class")).readAllBytes(); }
  ClassNode arena=new ClassNode(); new ClassReader(arenaBytes).accept(arena,0);
  MethodNode sword=arena.methods.stream().filter(m->m.name.equals("trainingSword")).findFirst().orElseThrow();
  MethodNode sound=arena.methods.stream().filter(m->m.name.equals("privateHitSound")).findFirst().orElseThrow();
  boolean unbreakable=false,criticalFlag=false,criticalSound=false;
  for(AbstractInsnNode i:sword.instructions) if(i instanceof MethodInsnNode m && m.name.equals("setUnbreakable")) unbreakable=true;
  for(AbstractInsnNode i:sound.instructions) {
   if(i instanceof MethodInsnNode m && m.name.equals("isCritical")) criticalFlag=true;
   if(i instanceof FieldInsnNode f && f.name.equals("ENTITY_PLAYER_ATTACK_CRIT")) criticalSound=true;
  }
  check(unbreakable && criticalFlag && criticalSound,"unbreakable sword and critical cue wired");
  System.out.println("Transformed behaviour, original attack, noob, critical cue and sword verified");
 }
}
