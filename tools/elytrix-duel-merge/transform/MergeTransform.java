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
    if(p.getFileName().toString().startsWith("BridgeFiles") || p.getFileName().toString().startsWith("LegacyDataMigration") || p.getFileName().toString().startsWith("SafeWorldCopy") || p.getFileName().toString().startsWith("ArenaBoundary") || p.getFileName().toString().startsWith("TextFilter")) continue;
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
        int textArg = current.equals(BRIDGE) && name.equals("colorize") && desc.equals("(Ljava/lang/String;)Ljava/lang/String;") ? 0 : current.equals("dev/epicduels/gui/PresentationText") && name.equals("render") ? 0 : current.equals("dev/epicduels/manager/GUIManager") && name.equals("colorize") ? 1 : -1;
        if(textArg>=0) { super.visitVarInsn(Opcodes.ALOAD,textArg);super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/TextFilter","clean","(Ljava/lang/String;)Ljava/lang/String;",false);super.visitVarInsn(Opcodes.ASTORE,textArg); }
        if(current.equals("ru/elytrix/duelsbridge/ArenaRules$Bounds") && name.equals("<init>") && desc.equals("(Ljava/lang/String;Lorg/bukkit/Location;Lorg/bukkit/Location;Ljava/util/List;DDD)V")) {
         for(int i=0;i<2;i++) {super.visitVarInsn(Opcodes.ALOAD,4);super.visitVarInsn(Opcodes.ALOAD,2+i);super.visitInsn(i==0?Opcodes.ICONST_0:Opcodes.ICONST_1);super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/ArenaBoundary","first","(Ljava/util/List;Lorg/bukkit/Location;I)Lorg/bukkit/Location;",false);super.visitVarInsn(Opcodes.ASTORE,2+i);}
        }
        if(current.equals(BRIDGE) && name.equals("onEnable") && desc.equals("()V")) {
          super.visitVarInsn(Opcodes.ALOAD,0); super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/LegacyDataMigration","migrate","(L"+BRIDGE+";)V",false);
          super.visitVarInsn(Opcodes.ALOAD,0); super.visitMethodInsn(Opcodes.INVOKESPECIAL,EPIC,"onEnable","()V",false);changed[1]++;
        }
       }
       @Override public void visitInsn(int opcode) {
        if(current.equals("dev/epicduels/i18n/Messages") && name.equals("raw") && desc.equals("(Ljava/lang/String;)Ljava/lang/String;") && opcode==Opcodes.ARETURN)
         super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/TextFilter","clean","(Ljava/lang/String;)Ljava/lang/String;",false);
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
        if(opcode==Opcodes.INVOKESTATIC && owner.equals("net/kyori/adventure/text/Component") && method.equals("text") && descriptor.equals("(Ljava/lang/String;Lnet/kyori/adventure/text/format/TextColor;[Lnet/kyori/adventure/text/format/TextDecoration;)Lnet/kyori/adventure/text/TextComponent;"))
          super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/TextFilter","decorations","([Lnet/kyori/adventure/text/format/TextDecoration;)[Lnet/kyori/adventure/text/format/TextDecoration;",false);
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
