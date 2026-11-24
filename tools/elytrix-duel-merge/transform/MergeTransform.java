import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;

/** Transform the existing bridge class hierarchy and data-folder calls. */
public final class MergeTransform {
 static final String BRIDGE="ru/elytrix/duelsbridge/ElytrixDuelsBridge";
 static final String EPIC="dev/epicduels/EpicDuels";
 static final String FILES="ru/elytrix/duelsbridge/BridgeFiles";
 public static void main(String[] args) throws Exception {
  Path root=Path.of(args[0]); int[] changed={0,0,0,0,0};
  try(var paths=Files.walk(root)) {
   for(Path p:paths.filter(x->x.toString().endsWith(".class")).toList()) {
    String rel=root.relativize(p).toString().replace('\\','/');
    if(!rel.startsWith("ru/elytrix/duelsbridge/") && !rel.startsWith("dev/epicduels/")) continue;
    if(p.getFileName().toString().startsWith("BridgeFiles") || p.getFileName().toString().startsWith("LegacyDataMigration") || p.getFileName().toString().startsWith("SafeWorldCopy")) continue;
    ClassReader r=new ClassReader(Files.readAllBytes(p));
    ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);
    r.accept(new ClassVisitor(Opcodes.ASM9,w) {
     String current;
     @Override public void visit(int version,int access,String name,String signature,String parent,String[] interfaces) {
      current=name;
      if(name.equals(BRIDGE)) { if(!parent.equals("org/bukkit/plugin/java/JavaPlugin"))throw new IllegalStateException("Unexpected superclass"); parent=EPIC;changed[0]++; }
      super.visit(version,access,name,signature,parent,interfaces);
     }
     @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
      MethodVisitor mv=super.visitMethod(access,name,desc,sig,ex);
      return new MethodVisitor(Opcodes.ASM9,mv) {
       @Override public void visitCode() {
        super.visitCode();
        if(current.equals(BRIDGE) && name.equals("onEnable") && desc.equals("()V")) {
          super.visitVarInsn(Opcodes.ALOAD,0); super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/LegacyDataMigration","migrate","(L"+BRIDGE+";)V",false);
          super.visitVarInsn(Opcodes.ALOAD,0); super.visitMethodInsn(Opcodes.INVOKESPECIAL,EPIC,"onEnable","()V",false);changed[1]++;
        }
       }
       @Override public void visitInsn(int opcode) {
        if(current.equals(BRIDGE) && name.equals("onDisable") && desc.equals("()V") && opcode==Opcodes.RETURN) {
          super.visitVarInsn(Opcodes.ALOAD,0); super.visitMethodInsn(Opcodes.INVOKESPECIAL,EPIC,"onDisable","()V",false);changed[2]++;
        }
        super.visitInsn(opcode);
       }
       @Override public void visitLdcInsn(Object value) {
        if(value instanceof String s && s.contains("EpicDuels")) value=s.replace("EpicDuels","ElytrixDuel");
        super.visitLdcInsn(value);
       }
       @Override public void visitInvokeDynamicInsn(String n,String d,Handle bootstrap,Object... values) {
        for(int i=0;i<values.length;i++) if(values[i] instanceof String s && s.contains("EpicDuels")) values[i]=s.replace("EpicDuels","ElytrixDuel");
        super.visitInvokeDynamicInsn(n,d,bootstrap,values);
       }
       @Override public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean itf) {
        if(current.equals(BRIDGE) && name.equals("<init>") && opcode==Opcodes.INVOKESPECIAL && owner.equals("org/bukkit/plugin/java/JavaPlugin") && method.equals("<init>")) { owner=EPIC;changed[3]++; }
        if(owner.equals(BRIDGE) && opcode==Opcodes.INVOKEVIRTUAL && Map.of(
          "getConfig","()Lorg/bukkit/configuration/file/FileConfiguration;",
          "getDataFolder","()Ljava/io/File;", "saveDefaultConfig","()V", "saveConfig","()V").containsKey(method) && Map.of(
          "getConfig","()Lorg/bukkit/configuration/file/FileConfiguration;",
          "getDataFolder","()Ljava/io/File;", "saveDefaultConfig","()V", "saveConfig","()V").get(method).equals(descriptor)) {
            opcode=Opcodes.INVOKESTATIC; owner=FILES; descriptor="(L"+BRIDGE+";"+descriptor.substring(1);itf=false;changed[4]++;
        }
        super.visitMethodInsn(opcode,owner,method,descriptor,itf);
       }
      };
     }
    },0);
    Files.write(p,w.toByteArray());
   }
  }
  System.out.println("super,enable,disable,constructor,config/data calls = "+Arrays.toString(changed));
  if(changed[0]!=1 || changed[1]!=1 || changed[2]<1 || changed[3]!=1 || changed[4]<20) throw new IllegalStateException("Unexpected transform counts");
 }
}
