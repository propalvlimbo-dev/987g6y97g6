import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;

/** Transform the existing bridge class hierarchy and data-folder calls. */
public final class MergeTransform {
 static final String BRIDGE="ru/elytrix/duelsbridge/ElytrixDuelsBridge";
 static final String EPIC="dev/epicduels/EpicDuels";
 static final String FILES="ru/elytrix/duelsbridge/BridgeFiles";
 public static void main(String[] args) throws Exception {
  Path root=Path.of(args[0]); int[] changed={0,0,0,0,0,0};
  try(var paths=Files.walk(root)) {
   for(Path p:paths.filter(x->x.toString().endsWith(".class")).toList()) {
    String rel=root.relativize(p).toString().replace('\\','/');
    if(!rel.startsWith("ru/elytrix/duelsbridge/") && !rel.startsWith("dev/epicduels/")) continue;
    if(p.getFileName().toString().startsWith("BridgeFiles") || p.getFileName().toString().startsWith("LegacyDataMigration") || p.getFileName().toString().startsWith("SafeWorldCopy") || p.getFileName().toString().startsWith("ArenaBoundary") || p.getFileName().toString().startsWith("TextFilter") || p.getFileName().toString().startsWith("MaceHeightDamage")) continue;
    ClassReader r=new ClassReader(Files.readAllBytes(p));
    ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);
    r.accept(new ClassVisitor(Opcodes.ASM9,w) {
     String current;
     @Override public void visit(int version,int access,String name,String signature,String parent,String[] interfaces) {
      current=name;
      if(name.equals(BRIDGE)) { if(!parent.equals("org/bukkit/plugin/java/JavaPlugin"))throw new IllegalStateException("Unexpected superclass"); parent=EPIC;changed[0]++; }
      super.visit(version,access,name,signature,parent,interfaces);
     }
     @Override public void visitEnd() {
      if(current.equals("ru/elytrix/duelsbridge/ArenaRules")) for(String type:List.of("PlayerMoveEvent","PlayerTeleportEvent")) {
       MethodVisitor m=super.visitMethod(Opcodes.ACC_PUBLIC,"elytrixHeight"+type,"(Lorg/bukkit/event/player/"+type+";)V",null,null);
       AnnotationVisitor a=m.visitAnnotation("Lorg/bukkit/event/EventHandler;",true);a.visitEnum("priority","Lorg/bukkit/event/EventPriority;","MONITOR");a.visit("ignoreCancelled",true);a.visitEnd();
       m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitVarInsn(Opcodes.ALOAD,1);m.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/MaceHeightDamage","onMovement","(Lru/elytrix/duelsbridge/ArenaRules;Lorg/bukkit/event/player/PlayerMoveEvent;)V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
      }
      super.visitEnd();
     }
     @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
      MethodVisitor mv=super.visitMethod(access,name,desc,sig,ex);
      return new MethodVisitor(Opcodes.ASM9,mv) {
       boolean dropPotionResult;
       int arenaNameCalls;
       @Override public void visitCode() {
        super.visitCode();
        int textArg = current.equals(BRIDGE) && name.equals("colorize") && desc.equals("(Ljava/lang/String;)Ljava/lang/String;") ? 0 : current.equals("dev/epicduels/gui/PresentationText") && name.equals("render") ? 0 : current.equals("dev/epicduels/manager/GUIManager") && name.equals("colorize") ? 1 : -1;
        if(textArg>=0) { super.visitVarInsn(Opcodes.ALOAD,textArg);super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/TextFilter","clean","(Ljava/lang/String;)Ljava/lang/String;",false);super.visitVarInsn(Opcodes.ASTORE,textArg); }
        if(current.equals("dev/epicduels/gui/PresentationText") && name.equals("render") && desc.equals("(Ljava/lang/String;Ljava/util/Map;)Lnet/kyori/adventure/text/Component;")) {
         super.visitVarInsn(Opcodes.ALOAD,1);super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/TextFilter","displayValues","(Ljava/util/Map;)Ljava/util/Map;",false);super.visitVarInsn(Opcodes.ASTORE,1);
        }
        if(current.equals("dev/epicduels/i18n/Messages") && name.equals("unparsed") && desc.equals("(Ljava/lang/String;Ljava/lang/Object;)Lnet/kyori/adventure/text/minimessage/tag/resolver/TagResolver;")) {
         super.visitVarInsn(Opcodes.ALOAD,0);super.visitVarInsn(Opcodes.ALOAD,1);super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/TextFilter","displayToken","(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;",false);super.visitVarInsn(Opcodes.ASTORE,1);
        }
        if(current.equals("ru/elytrix/duelsbridge/ArenaRules$Bounds") && name.equals("<init>") && desc.equals("(Ljava/lang/String;Lorg/bukkit/Location;Lorg/bukkit/Location;Ljava/util/List;DDD)V")) {
         for(int i=0;i<2;i++) {super.visitVarInsn(Opcodes.ALOAD,4);super.visitVarInsn(Opcodes.ALOAD,2+i);super.visitInsn(i==0?Opcodes.ICONST_0:Opcodes.ICONST_1);super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/ArenaBoundary","first","(Ljava/util/List;Lorg/bukkit/Location;I)Lorg/bukkit/Location;",false);super.visitVarInsn(Opcodes.ASTORE,2+i);}
        }
        if(current.equals("ru/elytrix/duelsbridge/ArenaRules") && name.equals("start") && desc.equals("()V")) {
         super.visitVarInsn(Opcodes.ALOAD,0);super.visitVarInsn(Opcodes.ALOAD,0);super.visitFieldInsn(Opcodes.GETFIELD,current,"plugin","L"+BRIDGE+";");super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/MaceHeightDamage","start","(L"+current+";L"+BRIDGE+";)V",false);
        }
        if(current.equals(BRIDGE) && name.equals("onEnable") && desc.equals("()V")) {
          super.visitVarInsn(Opcodes.ALOAD,0); super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/LegacyDataMigration","migrate","(L"+BRIDGE+";)V",false);
          super.visitVarInsn(Opcodes.ALOAD,0); super.visitMethodInsn(Opcodes.INVOKESPECIAL,EPIC,"onEnable","()V",false);changed[1]++;
        }
       }
       @Override public void visitInsn(int opcode) {
        if(dropPotionResult) { if(opcode!=Opcodes.POP)throw new IllegalStateException("Unexpected removePotionEffect result use"); dropPotionResult=false; return; }
        if(current.equals(BRIDGE) && name.equals("round") && desc.equals("(D)Ljava/lang/String;") && opcode==Opcodes.ARETURN) {
         super.visitVarInsn(Opcodes.DLOAD,0);super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/ArenaBoundary","displayLimit","(Ljava/lang/String;D)Ljava/lang/String;",false);
        }
        if(current.equals("ru/elytrix/duelsbridge/ArenaRules") && name.equals("maxHeight") && desc.equals("(Ljava/lang/Object;)D") && opcode==Opcodes.DRETURN) {
         super.visitVarInsn(Opcodes.ALOAD,1);super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/ArenaBoundary","effectiveHeight","(DLjava/lang/Object;)D",false);
        }
        if(current.equals("ru/elytrix/duelsbridge/Duels") && name.equals("arenasReport") && desc.equals("()Ljava/lang/String;") && opcode==Opcodes.ARETURN)
         super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/TextFilter","displayArenaReport","(Ljava/lang/String;)Ljava/lang/String;",false);
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
        if(owner.equals("org/bukkit/entity/Player") && method.equals("removePotionEffect") && descriptor.equals("(Lorg/bukkit/potion/PotionEffectType;)Z")) {
         if(!current.equals("ru/elytrix/duelsbridge/Effects") || !name.equals("clear"))throw new IllegalStateException("Unexpected potion effect call");
         descriptor="(Lorg/bukkit/potion/PotionEffectType;)V";dropPotionResult=true;changed[5]++;
        }
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
        boolean arenaLabel = descriptor.equals("()Ljava/lang/String;") && (current.equals("ru/elytrix/duelsbridge/Arenas") && Set.of("command","list","setPoint","readyReport").contains(name) && owner.equals("ru/elytrix/duelsbridge/Arenas") && method.equals("arenaName") || current.equals("dev/epicduels/manager/GUIManager") && name.equals("openSpectateMenu") && method.equals("getArenaName"));
        if(current.equals("dev/epicduels/manager/GUIManager") && owner.equals("dev/epicduels/model/Arena") && method.equals("getName") && descriptor.equals("()Ljava/lang/String;")) {
         if(name.equals("openArenaSelect"))arenaLabel=arenaNameCalls++==0;
         else if(name.equals("openArenaList"))arenaLabel=true;
        }
        if(arenaLabel)super.visitMethodInsn(Opcodes.INVOKESTATIC,"ru/elytrix/duelsbridge/TextFilter","displayArena","(Ljava/lang/String;)Ljava/lang/String;",false);
       }
      };
     }
    },0);
    Files.write(p,w.toByteArray());
   }
  }
  System.out.println("super,enable,disable,constructor,config/data,potion API = "+Arrays.toString(changed));
  if(changed[0]!=1 || changed[1]!=1 || changed[2]<1 || changed[3]!=1 || changed[4]<20 || changed[5]!=1) throw new IllegalStateException("Unexpected transform counts");
 }
}
