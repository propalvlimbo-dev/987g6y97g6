import java.nio.file.*;
import org.objectweb.asm.*;
public final class WorldCopyTransform {
 public static void main(String[] args) throws Exception {
  Path p=Path.of(args[0],"dev/epicduels/manager/ArenaManager.class");
  ClassReader r=new ClassReader(Files.readAllBytes(p)); ClassWriter w=new ClassWriter(r,0); int[] found={0};
  r.accept(new ClassVisitor(Opcodes.ASM9,w) {
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions) {
    if(name.equals("copyDirectory") && desc.equals("(Ljava/nio/file/Path;Ljava/nio/file/Path;)V")) {
     found[0]++;
     MethodVisitor m=super.visitMethod(access,name,desc,sig,exceptions);
     m.visitCode();m.visitVarInsn(Opcodes.ALOAD,1);m.visitVarInsn(Opcodes.ALOAD,2);
     m.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/SafeWorldCopy","copy",desc,false);
     m.visitInsn(Opcodes.RETURN);m.visitMaxs(2,3);m.visitEnd();return null;
    }
    return super.visitMethod(access,name,desc,sig,exceptions);
   }
  },0);
  if(found[0]!=1)throw new IllegalStateException("copyDirectory was not uniquely found");
  Files.write(p,w.toByteArray()); System.out.println("Replaced arena world copy function");
 }
}
