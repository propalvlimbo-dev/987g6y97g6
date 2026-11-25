import java.io.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.objectweb.asm.util.*;
public final class ProBytecodeTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static ClassNode read(String jar,String path)throws Exception {
  try(ZipFile z=new ZipFile(jar)){ClassNode node=new ClassNode();new ClassReader(z.getInputStream(z.getEntry(path)).readAllBytes()).accept(node,0);return node;}
 }
 static MethodNode method(ClassNode n,String name){return n.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
 static String code(MethodNode m){Textifier text=new Textifier();m.accept(new TraceMethodVisitor(text));StringWriter s=new StringWriter();text.print(new PrintWriter(s));return s.toString();}
 public static void main(String[] args)throws Exception {
  ClassNode before=read(args[0],"ru/elytrix/bots/BotsPlugin.class"), after=read(args[1],"ru/elytrix/bots/BotsPlugin.class");
  MethodNode behave=method(after,"behave");new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,behave);
  check(code(method(before,"behave")).equals(code(behave)),"combat behaviour must match tested base for all modes");
  check(code(method(before,"face")).equals(code(method(after,"face"))),"no synthetic rotation");
  ClassNode arena=read(args[1],"ru/elytrix/bots/TrainingArena.class");
  boolean unbreakable=false,critFlag=false,critSound=false;
  for(AbstractInsnNode i:method(arena,"trainingSword").instructions)if(i instanceof MethodInsnNode m&&m.name.equals("setUnbreakable"))unbreakable=true;
  for(AbstractInsnNode i:method(arena,"privateHitSound").instructions){
   if(i instanceof MethodInsnNode m&&m.name.equals("isCritical"))critFlag=true;
   if(i instanceof FieldInsnNode f&&f.name.equals("ENTITY_PLAYER_ATTACK_CRIT"))critSound=true;
  }
  check(unbreakable&&critFlag&&critSound,"sword and critical sound still wired");
  try(ZipFile z=new ZipFile(args[1])){check(z.getEntry("ru/elytrix/bots/ProCombat.class")==null,"artificial velocity helper removed");}
  System.out.println("Original Pro/Noob movement and attacks restored; critical cue and sword preserved");
 }
}
