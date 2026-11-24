import java.nio.file.*;
import org.objectweb.asm.*;
public final class WorldCopyTransform {
 public static void main(String[] args) throws Exception {
  Path p=Path.of(args[0],"dev/epicduels/manager/ArenaManager.class");
  ClassReader r=new ClassReader(Files.readAllBytes(p)); ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS); int[] found={0,0,0};
  r.accept(new ClassVisitor(Opcodes.ASM9,w) {
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions) {
    if(name.equals("copyDirectory") && desc.equals("(Ljava/nio/file/Path;Ljava/nio/file/Path;)V")) {
     found[0]++;
     MethodVisitor m=super.visitMethod(access,name,desc,sig,exceptions);
     m.visitCode();m.visitVarInsn(Opcodes.ALOAD,1);m.visitVarInsn(Opcodes.ALOAD,2);
     m.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/SafeWorldCopy","copy",desc,false);
     m.visitInsn(Opcodes.RETURN);m.visitMaxs(2,3);m.visitEnd();return null;
    }
    if(name.equals("deleteInstanceWorld") && desc.equals("(Ljava/lang/String;)V") || name.equals("deleteWorldFolder") && desc.equals("(Ljava/io/File;)V")) {
     boolean byName=name.equals("deleteInstanceWorld");found[byName?1:2]++;
     MethodVisitor m=super.visitMethod(access,name,desc,sig,exceptions);
     return new MethodVisitor(Opcodes.ASM9,m) {
      @Override public void visitCode() {
       super.visitCode();Label allowed=new Label();
       super.visitVarInsn(Opcodes.ALOAD,byName?1:0);
       super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/SharedWorldSafety",byName?"protectedWorld":"protectedPath",byName?"(Ljava/lang/String;)Z":"(Ljava/io/File;)Z",false);
       super.visitJumpInsn(Opcodes.IFEQ,allowed);super.visitInsn(Opcodes.RETURN);
       super.visitLabel(allowed);super.visitFrame(Opcodes.F_SAME,0,null,0,null);
      }
     };
    }
    return super.visitMethod(access,name,desc,sig,exceptions);
   }
  },0);
  if(found[0]!=1 || found[1]!=1 || found[2]!=1)throw new IllegalStateException("copy or deletion methods not uniquely found");
  Files.write(p,w.toByteArray()); System.out.println("Replaced arena world copy function");
 }
}
