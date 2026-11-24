package ru.elytrix.duelsbridge;
import java.nio.file.*;import java.util.*;import java.util.zip.*;import org.objectweb.asm.*;import org.bukkit.*;import dev.epicduels.model.Arena;
public class SharedWorldIntegrationTest {
 public static void main(String[] args)throws Exception {
  byte[] bytes;
  try(ZipFile jar=new ZipFile(args[0])){
   ClassWriter w=new ClassWriter(0);w.visit(Opcodes.V21,Opcodes.ACC_PUBLIC,"dev/epicduels/manager/ArenaManager",null,"java/lang/Object",null);
   w.visitField(Opcodes.ACC_PUBLIC,"plugin","Ldev/epicduels/EpicDuels;",null,null).visitEnd();
   MethodVisitor init=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);init.visitCode();init.visitVarInsn(Opcodes.ALOAD,0);init.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);init.visitInsn(Opcodes.RETURN);init.visitMaxs(1,1);init.visitEnd();
   new ClassReader(jar.getInputStream(jar.getEntry("dev/epicduels/manager/ArenaManager.class"))).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex){return (name.equals("createInstanceWorld")&&desc.startsWith("(Ldev/epicduels/model/Arena;Ljava/lang/String;")||name.equals("deleteInstanceWorld"))?w.visitMethod(access,name,desc,sig,ex):null;}},0);w.visitEnd();bytes=w.toByteArray();
  }
  byte[] code=bytes;Class<?> c=new ClassLoader(SharedWorldIntegrationTest.class.getClassLoader()){Class<?> define(){return defineClass(null,code,0,code.length);}}.define();
  Object manager=c.getConstructor().newInstance();SharedBattleWorldTest.Plugin plugin=new SharedBattleWorldTest.Plugin();c.getField("plugin").set(manager,plugin);
  World world=()->"duel";Bukkit.worlds.put("duel",world);plugin.a.stored=List.of(new Location(world,0,101,0),new Location(world,1,101,0));SharedBattleWorldTest.Battle duel=new SharedBattleWorldTest.Battle();plugin.m.map.put("unique-token",duel);
  Path container=Files.createTempDirectory("shared-integration");Bukkit.worldContainer=container.toFile();Path protectedWorld=Files.createDirectories(container.resolve("duel"));Path marker=Files.writeString(protectedWorld.resolve("keep"),"keep");
  Object future=c.getMethod("createInstanceWorld",Arena.class,String.class).invoke(manager,new Arena(),"unique-token");
  if(((java.util.concurrent.CompletableFuture<?>)future).join()!=world)throw new AssertionError("did not use existing world");
  try(var paths=Files.list(container)){if(paths.count()!=1)throw new AssertionError("a copy was created");}
  c.getMethod("deleteInstanceWorld",String.class).invoke(manager,"unique-token");
  if(!Files.exists(marker)||SharedBattleWorld.full(plugin))throw new AssertionError("cleanup touched world or retained slot");
  System.out.println("Actual transformed create/cleanup methods use the shared world");
 }
}
