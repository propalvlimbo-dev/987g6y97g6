import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.bukkit.Bukkit;

/** Execute the actual patched static deletion method, with its actual visitor class. */
public final class WorldDeletionBytecodeTest {
 public static void main(String[] args)throws Exception {
  byte[] manager,visitor;
  try(ZipFile jar=new ZipFile(args[0])) {
   byte[] original=jar.getInputStream(jar.getEntry("dev/epicduels/manager/ArenaManager.class")).readAllBytes();
   visitor=jar.getInputStream(jar.getEntry("dev/epicduels/manager/ArenaManager$2.class")).readAllBytes();
   ClassWriter w=new ClassWriter(0);w.visit(Opcodes.V21,Opcodes.ACC_PUBLIC,"dev/epicduels/manager/ArenaManager",null,"java/lang/Object",null);
   new ClassReader(original).accept(new ClassVisitor(Opcodes.ASM9){
    @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
     return name.equals("deleteWorldFolder")?w.visitMethod(access,name,desc,sig,ex):null;
    }
   },0);w.visitEnd();manager=w.toByteArray();
  }
  byte[] managerBytes=manager, visitorBytes=visitor;
  ClassLoader loader=new ClassLoader(WorldDeletionBytecodeTest.class.getClassLoader()) {
   @Override protected Class<?> findClass(String name)throws ClassNotFoundException {
    byte[] b=name.equals("dev.epicduels.manager.ArenaManager")?managerBytes:name.equals("dev.epicduels.manager.ArenaManager$2")?visitorBytes:null;
    if(b==null)throw new ClassNotFoundException(name);return defineClass(name,b,0,b.length);
   }
  };
  Class<?> c=loader.loadClass("dev.epicduels.manager.ArenaManager");
  var delete=c.getMethod("deleteWorldFolder",File.class);
  Path root=Files.createTempDirectory("actual-delete");Bukkit.worldContainer=root.toFile();
  Path world=Files.createDirectories(root.resolve("duel"));Path keep=Files.writeString(world.resolve("keep.dat"),"important");
  Path instance=Files.createDirectories(root.resolve("arena_instance_test"));Files.writeString(instance.resolve("temp.dat"),"temporary");
  delete.invoke(null,world.toFile());if(!Files.exists(keep))throw new AssertionError("world was deleted");
  delete.invoke(null,root.toFile());if(!Files.exists(keep))throw new AssertionError("parent was deleted");
  delete.invoke(null,instance.toFile());if(Files.exists(instance))throw new AssertionError("ordinary instance was not deleted");
  System.out.println("Actual transformed folder deletion tested with protected world and instance");
 }
}
