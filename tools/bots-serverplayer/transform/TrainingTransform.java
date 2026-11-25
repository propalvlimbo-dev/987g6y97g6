import java.nio.file.*;
import org.objectweb.asm.*;
public final class TrainingTransform {
 public static void main(String[] args) throws Exception {
  Path path=Path.of(args[0]); byte[] input=Files.readAllBytes(path); ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_MAXS); int[] hits=new int[8];
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
      if(name.equals("remove") && desc.equals("(Ljava/util/UUID;)V")) { hits[4]++; mv.visitVarInsn(Opcodes.ALOAD,1); mv.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/bots/ProMotion","clear","(Ljava/util/UUID;)V",false); }
      if(name.equals("onDisable") && desc.equals("()V")) { hits[1]++; mv.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/bots/TrainingArena","shutdown","()V",false); }
      if(name.equals("openMenu") && desc.equals("(Lorg/bukkit/entity/Player;I)V")) { hits[2]++; mv.visitVarInsn(Opcodes.ALOAD,1); mv.visitVarInsn(Opcodes.ILOAD,2); mv.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/bots/TrainingArena","menuPage","(Lorg/bukkit/entity/Player;I)I",false); mv.visitVarInsn(Opcodes.ISTORE,2); }
     }
     public void visitFieldInsn(int op,String owner,String field,String d) {
      if(name.equals("behave") && op==Opcodes.PUTFIELD && owner.equals("ru/elytrix/bots/TrainingPlayer") && field.equals("zza") && d.equals("F")) {
       hits[5]++; mv.visitVarInsn(Opcodes.ALOAD,3); mv.visitVarInsn(Opcodes.DLOAD,9); mv.visitVarInsn(Opcodes.LLOAD,12);
       mv.visitVarInsn(Opcodes.ALOAD,1); mv.visitFieldInsn(Opcodes.GETFIELD,"ru/elytrix/bots/TrainingPlayer","nextAttackTick","J");
       mv.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/bots/ProMotion","forward","(FLjava/lang/String;DJJ)F",false);
      }
      super.visitFieldInsn(op,owner,field,d);
     }
     public void visitMethodInsn(int op,String owner,String method,String d,boolean itf) {
      if(name.equals("behave") && owner.equals("ru/elytrix/bots/TrainingPlayer") && method.equals("setSprinting") && d.equals("(Z)V")) {
       hits[7]++; mv.visitVarInsn(Opcodes.ALOAD,3); mv.visitVarInsn(Opcodes.DLOAD,9); mv.visitVarInsn(Opcodes.LLOAD,12);
       mv.visitVarInsn(Opcodes.ALOAD,1); mv.visitFieldInsn(Opcodes.GETFIELD,"ru/elytrix/bots/TrainingPlayer","nextAttackTick","J");
       mv.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/bots/ProMotion","sprint","(ZLjava/lang/String;DJJ)Z",false);
      }
      super.visitMethodInsn(op,owner,method,d,itf);
      if(name.equals("behave") && owner.equals("ru/elytrix/bots/TrainingPlayer") && method.equals("setSprinting") && d.equals("(Z)V")) {
       hits[6]++; mv.visitVarInsn(Opcodes.ALOAD,8); mv.visitVarInsn(Opcodes.ALOAD,2); mv.visitVarInsn(Opcodes.ALOAD,3); mv.visitVarInsn(Opcodes.LLOAD,12);
       mv.visitVarInsn(Opcodes.ALOAD,1); mv.visitFieldInsn(Opcodes.GETFIELD,"ru/elytrix/bots/TrainingPlayer","nextAttackTick","J"); mv.visitVarInsn(Opcodes.ILOAD,21);
       mv.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/bots/ProMotion","spin","(Lorg/bukkit/entity/Player;Lorg/bukkit/entity/Player;Ljava/lang/String;JJZ)V",false);
      }
      if(name.equals("onEnable") && method.equals("runTaskTimer") && owner.equals("org/bukkit/scheduler/BukkitScheduler")) {
       // Original return value is consumed by POP next. Keep it on stack while installing.
       hits[3]++; mv.visitVarInsn(Opcodes.ALOAD,0); mv.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/bots/TrainingArena","install","(Lorg/bukkit/plugin/java/JavaPlugin;)V",false);
      }
     }
    };
   }
  },0);
  for(int h:hits) if(h!=1) throw new IllegalStateException("Unexpected transform counts: "+java.util.Arrays.toString(hits));
  Files.write(path,out.toByteArray()); System.out.println("Training hooks "+java.util.Arrays.toString(hits));
 }
}
