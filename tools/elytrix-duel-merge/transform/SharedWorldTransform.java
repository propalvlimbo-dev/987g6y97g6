import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;

/** Bytecode integration for the shared-world implementation; asserts every hook. */
public final class SharedWorldTransform {
 static final String CORE="dev/epicduels/", BRIDGE="ru/elytrix/duelsbridge/", HOOK=BRIDGE+"SharedBattleWorld", PLUGIN=BRIDGE+"ElytrixDuelsBridge";
 static final Map<String,Integer> seen=new LinkedHashMap<>();
 static void found(String key){seen.merge(key,1,Integer::sum);}
 public static void main(String[] args)throws Exception{
  Path root=Path.of(args[0]);
  try(var all=Files.walk(root)) {for(Path path:all.filter(p->p.toString().endsWith(".class")).toList()){
   String rel=root.relativize(path).toString().replace('\\','/');
   if(!rel.startsWith(CORE)&&!rel.startsWith(BRIDGE))continue;
   if(rel.startsWith(HOOK))continue;
   ClassReader r=new ClassReader(Files.readAllBytes(path));ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);
   r.accept(new ClassVisitor(Opcodes.ASM9,w){String c;
    @Override public void visit(int v,int a,String n,String s,String sup,String[] i){c=n;super.visit(v,a,n,s,sup,i);}
    @Override public void visitEnd(){
     if(c.equals(CORE+"listener/FriendlyFireListener")){
      for(String event:List.of("PotionSplashEvent","LingeringPotionSplashEvent")){
       String descriptor="(Lorg/bukkit/event/entity/"+event+";)V";
       MethodVisitor m=super.visitMethod(Opcodes.ACC_PUBLIC,"elytrix"+event,descriptor,null,null);
       AnnotationVisitor a=m.visitAnnotation("Lorg/bukkit/event/EventHandler;",true);a.visitEnum("priority","Lorg/bukkit/event/EventPriority;","HIGHEST");a.visit("ignoreCancelled",true);a.visitEnd();
       m.visitCode();m.visitVarInsn(Opcodes.ALOAD,1);m.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,event.equals("PotionSplashEvent")?"splash":"lingering",descriptor,false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();found("potionListener");
      }
     }
     super.visitEnd();
    }
    @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex){
     String k=null;
     if(c.equals(CORE+"manager/ArenaManager")){
      if(name.equals("createInstanceWorld")&&desc.equals("(Ldev/epicduels/model/Arena;Ljava/lang/String;)Ljava/util/concurrent/CompletableFuture;"))k="create";
      if(name.equals("deleteInstanceWorld")&&desc.equals("(Ljava/lang/String;)V"))k="delete";
      if(name.equals("ensureArenaWorldLoaded")&&desc.equals("(Ldev/epicduels/model/Arena;)V"))k="load";
     }
     if(c.equals(BRIDGE+"Arenas")){
      if(name.equals("onTeleport")&&desc.equals("(Lorg/bukkit/event/player/PlayerTeleportEvent;)V"))k="redirect";
      if(name.equals("full")&&desc.equals("()Z"))k="full";
      if(name.equals("reserve")&&desc.equals("(Ljava/util/List;)I"))k="reserve";
      if(name.equals("release")&&desc.equals("(Ljava/util/List;)V"))k="release";
     }
     if(c.equals(CORE+"manager/DuelManager") && name.equals("resetPlayerPlacedBlocks"))k="roundCleanup";
     if(c.equals(BRIDGE+"ArenaRules") && name.equals("leaveEnabled") && desc.equals("()Z"))k="leaveEnabled";
     MethodVisitor target=super.visitMethod(access,name,desc,sig,ex);
     if(k!=null){String kind=k;found(kind);return new MethodVisitor(Opcodes.ASM9){
      @Override public AnnotationVisitor visitAnnotation(String d,boolean visible){return target.visitAnnotation(d,visible);}
      @Override public void visitEnd(){target.visitCode();
       if(kind.equals("create")){target.visitVarInsn(Opcodes.ALOAD,0);target.visitFieldInsn(Opcodes.GETFIELD,CORE+"manager/ArenaManager","plugin","L"+CORE+"EpicDuels;");target.visitTypeInsn(Opcodes.CHECKCAST,PLUGIN);target.visitVarInsn(Opcodes.ALOAD,1);target.visitVarInsn(Opcodes.ALOAD,2);target.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"create","(L"+PLUGIN+";Ljava/lang/Object;Ljava/lang/String;)Ljava/util/concurrent/CompletableFuture;",false);target.visitInsn(Opcodes.ARETURN);}
       else if(kind.equals("delete")){target.visitVarInsn(Opcodes.ALOAD,1);target.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"release","(Ljava/lang/String;)V",false);target.visitInsn(Opcodes.RETURN);}
       else if(kind.equals("redirect")||kind.equals("full")||kind.equals("reserve")){target.visitVarInsn(Opcodes.ALOAD,0);target.visitFieldInsn(Opcodes.GETFIELD,BRIDGE+"Arenas","plugin","L"+PLUGIN+";");if(kind.equals("redirect"))target.visitVarInsn(Opcodes.ALOAD,1);target.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,kind.equals("redirect")?"teleport":kind,kind.equals("redirect")?"(L"+PLUGIN+";Lorg/bukkit/event/player/PlayerTeleportEvent;)V":kind.equals("full")?"(L"+PLUGIN+";)Z":"(L"+PLUGIN+";)I",false);target.visitInsn(kind.equals("redirect")?Opcodes.RETURN:Opcodes.IRETURN);}
       else if(kind.equals("leaveEnabled")){target.visitInsn(Opcodes.ICONST_1);target.visitInsn(Opcodes.IRETURN);}
       else target.visitInsn(Opcodes.RETURN);
       target.visitMaxs(0,0);target.visitEnd();}
     };}
     return new MethodVisitor(Opcodes.ASM9,target){
      @Override public void visitCode(){super.visitCode();if(c.equals(CORE+"listener/FriendlyFireListener")&&name.equals("onDamage")){super.visitVarInsn(Opcodes.ALOAD,1);super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"isolate","(Lorg/bukkit/event/entity/EntityDamageByEntityEvent;)V",false);found("isolate");}}
      @Override public void visitInsn(int opcode){
       if(c.equals(CORE+"manager/TeamDuelManager")&&name.equals("failStart")&&opcode==Opcodes.RETURN){super.visitVarInsn(Opcodes.ALOAD,1);super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,CORE+"model/TeamDuelInstance","getInstanceWorldName","()Ljava/lang/String;",false);super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"release","(Ljava/lang/String;)V",false);found("teamFail");}
       super.visitInsn(opcode);
      }
      @Override public void visitMethodInsn(int op,String owner,String n,String d,boolean itf){
       if(owner.equals("org/bukkit/entity/Player")&&n.equals("teleport")&&d.equals("(Lorg/bukkit/Location;)Z") && (c.equals(CORE+"manager/DuelManager")&&(name.equals("lambda$startDuel$3")||name.equals("lambda$startNextRound$5")) || c.equals(CORE+"manager/TeamDuelManager")&&name.equals("lambda$startTeamDuel$4"))){
        super.visitVarInsn(Opcodes.ALOAD,0);super.visitVarInsn(Opcodes.ALOAD,name.equals("lambda$startDuel$3")?3:1);super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"checkedTeleport","(Lorg/bukkit/entity/Player;Lorg/bukkit/Location;Ljava/lang/Object;Ljava/lang/Object;)Z",false);found("checkedTeleport");return;
       }
       if((c.equals(CORE+"model/DuelInstance")||c.equals(CORE+"model/TeamDuelInstance"))&&name.equals("<init>")&&owner.equals("java/lang/String")&&n.equals("substring")&&d.equals("(II)Ljava/lang/String;")){super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"fullId","(Ljava/lang/String;II)Ljava/lang/String;",false);found("fullId");return;}
       if(c.equals(BRIDGE+"ArenaRules")&&name.equals("resolve")&&owner.equals(BRIDGE+"Arenas")&&n.equals("pairPointsIn")){super.visitVarInsn(Opcodes.ALOAD,1);super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"points","(L"+BRIDGE+"Arenas;Lorg/bukkit/World;Lorg/bukkit/entity/Player;)Ljava/util/List;",false);found("bounds");return;}
       if(c.equals(CORE+"listener/PlayerListener")&&name.equals("onPlayerTeleport")&&n.equals("getInstanceWorldName")&&d.equals("()Ljava/lang/String;")){super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"realWorld","(Ljava/lang/Object;)Ljava/lang/String;",false);found("worldCheck");return;}
       if(c.equals(CORE+"listener/PlayerListener")&&name.equals("onPlayerRespawn")&&owner.equals("org/bukkit/event/player/PlayerRespawnEvent")&&n.equals("setRespawnLocation")){super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"respawn","(Lorg/bukkit/event/player/PlayerRespawnEvent;Lorg/bukkit/Location;)V",false);found("respawn");return;}
       if((c.equals(CORE+"listener/WorldProtectionListener")||c.equals(CORE+"listener/LobbyProtectionListener")||c.equals(BRIDGE+"ArenaGuard")||c.equals(BRIDGE+"Effects"))&&owner.equals("java/lang/String")&&n.equals("startsWith")&&d.equals("(Ljava/lang/String;)Z")){super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"startsWith","(Ljava/lang/String;Ljava/lang/String;)Z",false);found("protection");return;}
       super.visitMethodInsn(op,owner,n,d,itf);
      }
     };
    }
   },0);Files.write(path,w.toByteArray());
  }}
  for(String key:List.of("create","delete","load","redirect","full","reserve","release","roundCleanup","leaveEnabled","isolate","teamFail","bounds","worldCheck","respawn","protection","fullId","potionListener","checkedTeleport"))if(!seen.containsKey(key))throw new IllegalStateException("Missing shared-world hook: "+key);
  System.out.println("Shared-world hooks: "+seen);
 }
}
