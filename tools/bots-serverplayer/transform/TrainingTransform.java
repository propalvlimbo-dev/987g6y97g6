import java.nio.file.*;
import org.objectweb.asm.*;
public final class TrainingTransform {
 public static void main(String[] args) throws Exception {
  Path path=Path.of(args[0]); byte[] input=Files.readAllBytes(path); ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_MAXS); int[] hits=new int[9];
  new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9,out) {
   public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions) {
    MethodVisitor mv=super.visitMethod(access,name,desc,signature,exceptions);
    return new MethodVisitor(Opcodes.ASM9,mv) {
     public void visitCode() {
      super.visitCode();
      if(name.equals("onCommand") && desc.equals("(Lorg/bukkit/command/CommandSender;Lorg/bukkit/command/Command;Ljava/lang/String;[Ljava/lang/String;)Z")) {
       hits[0]++; mv.visitVarInsn(Opcodes.ALOAD,1); mv.visitVarInsn(Opcodes.ALOAD,4);
       mv.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/bots/TrainingArena","command","(Lorg/bukkit/command/CommandSender;[Ljava/lang/String;)Z",false);
       Label next=new Label(); mv.visitJumpInsn(Opcodes.IFEQ,next); mv.visitInsn(Opcodes.ICONST_1); mv.visitInsn(Opcodes.IRETURN); mv.visitLabel(next); mv.visitFrame(Opcodes.F_SAME,0,null,0,null);
      }
      if(name.equals("onDisable") && desc.equals("()V")) { hits[1]++; mv.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/bots/TrainingArena","shutdown","()V",false); }
      if(name.equals("openMenu") && desc.equals("(Lorg/bukkit/entity/Player;I)V")) { hits[2]++; mv.visitVarInsn(Opcodes.ALOAD,1); mv.visitVarInsn(Opcodes.ILOAD,2); mv.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/bots/TrainingArena","menuPage","(Lorg/bukkit/entity/Player;I)I",false); mv.visitVarInsn(Opcodes.ISTORE,2); }
     }
     public void visitFieldInsn(int op,String owner,String field,String d) {
      if(name.equals("behave") && op==Opcodes.PUTFIELD && owner.equals("ru/elytrix/bots/TrainingPlayer") && (field.equals("zza")||field.equals("xxa")) && d.equals("F")) {
       hits[field.equals("zza")?4:5]++; mv.visitVarInsn(Opcodes.ALOAD,3); mv.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/bots/ProCombat","input","(FLjava/lang/String;)F",false);
      }
      super.visitFieldInsn(op,owner,field,d);
     }
     public void visitIntInsn(int op,int value) {
      if(name.equals("behave") && op==Opcodes.BIPUSH && value==11) { hits[6]++; value=8; }
      super.visitIntInsn(op,value);
     }
     public void visitLdcInsn(Object v) {
      if((name.equals("behave")||name.equals("setMode")) && v instanceof Double && ((Double)v)==0.32d) { hits[7]++; v=0.65d; }
      super.visitLdcInsn(v);
     }
     public void visitMethodInsn(int op,String owner,String method,String d,boolean itf) {
      super.visitMethodInsn(op,owner,method,d,itf);
      if(name.equals("behave") && owner.equals("ru/elytrix/bots/TrainingPlayer") && method.equals("setSprinting") && d.equals("(Z)V")) {
       hits[8]++; mv.visitVarInsn(Opcodes.ALOAD,8); mv.visitVarInsn(Opcodes.ALOAD,2); mv.visitVarInsn(Opcodes.ALOAD,3);
       mv.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/bots/ProCombat","tick","(Lorg/bukkit/entity/Player;Lorg/bukkit/entity/Player;Ljava/lang/String;)V",false);
      }
      if(name.equals("onEnable") && method.equals("runTaskTimer") && owner.equals("org/bukkit/scheduler/BukkitScheduler")) {
       // Original return value is consumed by POP next. Keep it on stack while installing.
       hits[3]++; mv.visitVarInsn(Opcodes.ALOAD,0); mv.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/bots/TrainingArena","install","(Lorg/bukkit/plugin/java/JavaPlugin;)V",false);
      }
     }
    };
   }
  },0);
  for(int i=0;i<hits.length;i++) if(hits[i]!=(i==7?2:1)) throw new IllegalStateException("Unexpected transform counts: "+java.util.Arrays.toString(hits));
  Files.write(path,out.toByteArray()); System.out.println("Training hooks "+java.util.Arrays.toString(hits));
 }
}
