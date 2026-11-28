import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 static final String B="ru/rooyzee/elytrixtrader/", I=B+"model/TraderInstance", H=B+"patch/TraderHologram", S=B+"patch/TraderScope";
 static int regionOverlap=0,regionRadius=0,regionVectors=0,regionContains=0,hitInstall=0,hitStart=0,hitRemove=0,hitStop=0,fallback=0,disable=0,galaxy=0,schematic=0,filters=0,starts=0,updates=0,removes=0,installs=0,guards=0,old=0;
 static void prepend(MethodNode m,InsnList a){m.instructions.insert(a);}
 static InsnList one(String method){InsnList a=new InsnList();a.add(new VarInsnNode(ALOAD,0));a.add(new MethodInsnNode(INVOKESTATIC,H,method,"(L"+I+";)V",false));return a;}
 static byte[] patch(byte[] bytes){ClassNode c=new ClassNode();new ClassReader(bytes).accept(c,0);
  for(MethodNode m:c.methods){
   if(c.name.equals(B+"config/ConfigLoader") && m.name.equals("loadTraders")){
    for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)n;if(call.owner.equals("java/io/File") && call.name.equals("listFiles") && call.desc.endsWith(")[Ljava/io/File;")){m.instructions.insert(n,new MethodInsnNode(INVOKESTATIC,S,"files","([Ljava/io/File;)[Ljava/io/File;",false));filters++;}}
   }
   if(c.name.equals(B+"virtual/TraderNpc") && m.name.equals("buildHolograms")){m.instructions.clear();m.tryCatchBlocks.clear();m.localVariables=null;m.instructions.add(new InsnNode(RETURN));old++;}
   if(c.name.equals(B+"Main") && m.name.equals("onEnable")){InsnList a=new InsnList();a.add(new VarInsnNode(ALOAD,0));a.add(new MethodInsnNode(INVOKESTATIC,H,"install","(L"+B+"Main;)V",false));a.add(new VarInsnNode(ALOAD,0));a.add(new MethodInsnNode(INVOKESTATIC,B+"patch/TraderHitbox","install","(L"+B+"Main;)V",false));prepend(m,a);installs++;hitInstall++;}
   if(c.name.equals(B+"Main") && m.name.equals("onDisable")){InsnList a=new InsnList();a.add(new VarInsnNode(ALOAD,0));a.add(new MethodInsnNode(INVOKESTATIC,B+"patch/TraderGalaxy","stopAll","(L"+B+"Main;)V",false));a.add(new VarInsnNode(ALOAD,0));a.add(new MethodInsnNode(INVOKESTATIC,B+"patch/TraderHitbox","stopAll","(L"+B+"Main;)V",false));prepend(m,a);disable++;hitStop++;}
   if(c.name.equals(I)){
    if(m.name.equals("spawn") && m.desc.equals("()V"))for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof FieldInsnNode){FieldInsnNode f=(FieldInsnNode)n;if(f.owner.equals(I)&&f.name.equals("alive")&&f.getOpcode()==PUTFIELD){InsnList a=new InsnList();a.add(new VarInsnNode(ALOAD,0));a.add(new FieldInsnNode(GETFIELD,I,"plugin","L"+B+"Main;"));a.add(new VarInsnNode(ALOAD,0));a.add(new MethodInsnNode(INVOKESTATIC,H,"start","(L"+B+"Main;L"+I+";)V",false));a.add(new VarInsnNode(ALOAD,0));a.add(new FieldInsnNode(GETFIELD,I,"plugin","L"+B+"Main;"));a.add(new VarInsnNode(ALOAD,0));a.add(new MethodInsnNode(INVOKESTATIC,B+"patch/TraderGalaxy","start","(L"+B+"Main;L"+I+";)V",false));a.add(new VarInsnNode(ALOAD,0));a.add(new FieldInsnNode(GETFIELD,I,"plugin","L"+B+"Main;"));a.add(new VarInsnNode(ALOAD,0));a.add(new MethodInsnNode(INVOKESTATIC,B+"patch/TraderHitbox","start","(L"+B+"Main;L"+I+";)V",false));m.instructions.insert(n,a);starts++;galaxy++;hitStart++;}}
    if(m.name.equals("tick"))for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode && ((MethodInsnNode)n).name.equals("updateHologramText")){m.instructions.insert(n,one("update"));updates++;}
    if((m.name.equals("despawn") && m.desc.equals("(Z)V"))||m.name.equals("finalizeDespawn")||m.name.equals("hideHolograms")||m.name.equals("despawnAnimated")){prepend(m,one("remove"));InsnList g=new InsnList();g.add(new VarInsnNode(ALOAD,0));g.add(new MethodInsnNode(INVOKESTATIC,B+"patch/TraderGalaxy","remove","(L"+I+";)V",false));prepend(m,g);InsnList hit=new InsnList();hit.add(new VarInsnNode(ALOAD,0));hit.add(new MethodInsnNode(INVOKESTATIC,B+"patch/TraderHitbox","remove","(L"+I+";)V",false));prepend(m,hit);removes++;hitRemove++;}
   }
   if(c.name.equals(B+"region/RegionManager") && (m.name.equals("createRegion")||m.name.equals("wouldOverlap"))){
    if(m.name.equals("createRegion")){AbstractInsnNode first=m.instructions.getFirst();while(first!=null&&first.getOpcode()<0)first=first.getNext();if(!(first instanceof IntInsnNode)||((IntInsnNode)first).operand!=30)throw new IllegalStateException("Unexpected region radius");((IntInsnNode)first).operand=8;regionRadius++;}
    if(m.name.equals("wouldOverlap")){
     for(AbstractInsnNode n:m.instructions.toArray())if(n.getOpcode()==DCMPG){AbstractInsnNode branch=n.getNext();while(branch!=null&&branch.getOpcode()<0)branch=branch.getNext();if(!(branch instanceof JumpInsnNode)||branch.getOpcode()!=IFGE)continue;
      AbstractInsnNode start=n;for(int j=0;j<10;j++){do{start=start.getPrevious();}while(start!=null&&start.getOpcode()<0);}if(!(start instanceof VarInsnNode)||start.getOpcode()!=DLOAD||((VarInsnNode)start).var!=6)throw new IllegalStateException("Unexpected overlap pattern");
      InsnList q=new InsnList();q.add(new VarInsnNode(ALOAD,5));q.add(new MethodInsnNode(INVOKEVIRTUAL,B+"region/TraderRegion","center","()Lorg/bukkit/Location;",false));q.add(new VarInsnNode(ALOAD,5));q.add(new MethodInsnNode(INVOKEVIRTUAL,B+"region/TraderRegion","radius","()I",false));q.add(new VarInsnNode(ALOAD,1));q.add(new VarInsnNode(ILOAD,2));q.add(new MethodInsnNode(INVOKESTATIC,B+"patch/TraderBounds","overlaps","(Lorg/bukkit/Location;ILorg/bukkit/Location;I)Z",false));q.add(new JumpInsnNode(IFEQ,((JumpInsnNode)branch).label));m.instructions.insertBefore(start,q);
      AbstractInsnNode cur=start;while(cur!=branch){AbstractInsnNode next=cur.getNext();m.instructions.remove(cur);cur=next;}m.instructions.remove(branch);regionOverlap++;break;
     }
    }
    int radiusVar=m.name.equals("createRegion")?4:2;
    for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode){MethodInsnNode v=(MethodInsnNode)n;if(v.owner.equals("com/sk89q/worldedit/math/BlockVector3")&&v.name.equals("at")&&v.desc.startsWith("(III)")){
     AbstractInsnNode cur=n;for(int j=0;j<5;j++){do{cur=cur.getPrevious();}while(cur!=null&&cur.getOpcode()<0);}if(cur==null)throw new IllegalStateException("vector pattern");
     boolean min=cur.getOpcode()==ICONST_0,max=cur instanceof IntInsnNode&&((IntInsnNode)cur).operand==255;if(!min&&!max)throw new IllegalStateException("vector Y not recognised");
     InsnList y=new InsnList();y.add(new VarInsnNode(ALOAD,1));y.add(new MethodInsnNode(INVOKEVIRTUAL,"org/bukkit/Location","getBlockY","()I",false));y.add(new VarInsnNode(ILOAD,radiusVar));y.add(new InsnNode(min?ISUB:IADD));m.instructions.insertBefore(cur,y);m.instructions.remove(cur);regionVectors++;
    }}
   }
   if(c.name.equals(B+"region/TraderRegion") && (m.name.equals("contains")||m.name.equals("containsBlock"))){
    m.instructions.clear();m.tryCatchBlocks.clear();m.localVariables=null;
    m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new FieldInsnNode(GETFIELD,c.name,"center","Lorg/bukkit/Location;"));
    m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new FieldInsnNode(GETFIELD,c.name,"radius","I"));
    if(m.name.equals("contains")){m.instructions.add(new VarInsnNode(ALOAD,1));m.instructions.add(new MethodInsnNode(INVOKESTATIC,B+"patch/TraderBounds","contains","(Lorg/bukkit/Location;ILorg/bukkit/Location;)Z",false));}
    else{m.instructions.add(new VarInsnNode(ILOAD,1));m.instructions.add(new VarInsnNode(ILOAD,2));m.instructions.add(new VarInsnNode(ILOAD,3));m.instructions.add(new VarInsnNode(ALOAD,4));m.instructions.add(new MethodInsnNode(INVOKESTATIC,B+"patch/TraderBounds","containsBlock","(Lorg/bukkit/Location;IIIILorg/bukkit/World;)Z",false));}
    m.instructions.add(new InsnNode(IRETURN));regionContains++;
   }
   if(c.name.equals(B+"listener/TradeListener") && m.name.equals("onInteract") && m.desc.equals("(Lorg/bukkit/event/player/PlayerInteractEvent;)V")){m.instructions.clear();m.tryCatchBlocks.clear();m.localVariables=null;m.instructions.add(new InsnNode(RETURN));fallback++;}
   if(c.name.equals(B+"trader/TraderManager") && m.name.equals("spawnInternal")){for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)n;if(call.owner.equals(B+"model/SchematicConfig") && call.name.equals("enabled") && call.desc.equals("()Z")){InsnList no=new InsnList();no.add(new InsnNode(POP));no.add(new InsnNode(ICONST_0));m.instructions.insertBefore(n,no);m.instructions.remove(n);schematic++;}}InsnList a=new InsnList();LabelNode ok=new LabelNode();a.add(new VarInsnNode(ALOAD,1));a.add(new MethodInsnNode(INVOKESTATIC,S,"allowed","(L"+B+"model/TraderConfig;)Z",false));a.add(new JumpInsnNode(IFNE,ok));a.add(new InsnNode(ACONST_NULL));a.add(new InsnNode(ARETURN));a.add(ok);a.add(new FrameNode(F_SAME,0,null,0,null));prepend(m,a);guards++;}
  }
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();
 }
 public static void main(String[] args)throws Exception{Path root=Paths.get(args[0]);String[] names={"config/ConfigLoader","editor/TradeEditorManager","virtual/TraderNpc","Main","model/TraderInstance","trader/TraderManager","listener/TradeListener","region/RegionManager","region/TraderRegion"};for(String n:names){Path p=root.resolve(B+n+".class");Files.write(p,patch(Files.readAllBytes(p)));}if(regionOverlap!=1||regionRadius!=1||regionVectors!=4||regionContains!=2||hitInstall!=1||hitStart!=1||hitStop!=1||hitRemove!=4||fallback!=1||disable!=1||galaxy!=1||schematic!=1||filters!=1||starts!=1||updates!=1||removes!=4||installs!=1||guards!=1||old!=1)throw new IllegalStateException("galaxy="+galaxy+" schematic="+schematic+" "+filters+" "+starts+" "+updates+" "+removes+" "+installs+" "+guards+" "+old);System.out.println("Hooks verified");}
}
