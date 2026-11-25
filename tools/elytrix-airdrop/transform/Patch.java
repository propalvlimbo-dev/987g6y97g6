import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public class Patch implements Opcodes {
  static String base="ru/rooyzee/elytrixairdrop/", runtime=base+"patch/LootRuntime";
  static int hooks=0;
  public static void main(String[] args) throws Exception {
    Path root=Paths.get(args[0]);
    patch(root,base+"Main");
    patch(root,base+"listeners/ProtectionListener");
    patch(root,base+"managers/AirdropManager");
    patch(root,base+"airdrops/types/ActivePeacefulAirdrop");
    patch(root,base+"airdrops/types/ActiveFireAirdrop");
    patch(root,base+"airdrops/types/ActiveSkyAirdrop");
    if(hooks != 15) throw new IllegalStateException("Unexpected hook count: "+hooks);
    System.out.println("Applied "+hooks+" bytecode hooks");
  }
  static void patch(Path root,String name) throws Exception {
    Path p=root.resolve(name+".class"); ClassNode c=new ClassNode(); new ClassReader(Files.readAllBytes(p)).accept(c,0);
    for(MethodNode m:c.methods) {
      if(name.equals(base+"Main") && m.name.equals("onEnable")) {
        for(AbstractInsnNode n=m.instructions.getFirst();n!=null;n=n.getNext()) if(n.getOpcode()==RETURN) {
          InsnList x=new InsnList(); x.add(new VarInsnNode(ALOAD,0)); x.add(new MethodInsnNode(INVOKESTATIC,base+"patch/EventBar","install","(L"+base+"Main;)V",false)); x.add(new VarInsnNode(ALOAD,0)); x.add(new MethodInsnNode(INVOKESTATIC,runtime,"install","(L"+base+"Main;)V",false)); x.add(new VarInsnNode(ALOAD,0)); x.add(new MethodInsnNode(INVOKESTATIC,base+"patch/AdminControls","install","(L"+base+"Main;)V",false)); x.add(new VarInsnNode(ALOAD,0)); x.add(new MethodInsnNode(INVOKESTATIC,base+"patch/PeacefulChest","install","(L"+base+"Main;)V",false)); x.add(new VarInsnNode(ALOAD,0)); x.add(new MethodInsnNode(INVOKESTATIC,base+"patch/SkyMechanics","install","(L"+base+"Main;)V",false)); m.instructions.insertBefore(n,x); hooks++; break;
        }
      }
      if(name.equals(base+"listeners/ProtectionListener") && (m.name.equals("onTeleport") || m.name.equals("onPearlThrow"))) {
        InsnList x=new InsnList(); LabelNode keep=new LabelNode();
        x.add(new VarInsnNode(ALOAD,0)); x.add(new FieldInsnNode(GETFIELD,name,"plugin","L"+base+"Main;"));
        x.add(new MethodInsnNode(INVOKESTATIC,base+"patch/SkyMechanics","skyActive","(L"+base+"Main;)Z",false));
        x.add(new JumpInsnNode(IFEQ,keep)); x.add(new InsnNode(RETURN)); x.add(keep); x.add(new FrameNode(F_SAME,0,null,0,null));
        m.instructions.insert(x); hooks++;
      }
      if(name.equals(base+"managers/AirdropManager") && m.name.equals("start")) {
        for(AbstractInsnNode n=m.instructions.getFirst();n!=null;n=n.getNext()) if(n instanceof MethodInsnNode && ((MethodInsnNode)n).name.equals("pasteAndBackup")) {
          AbstractInsnNode loc=n.getPrevious(); while(loc!=null && loc.getOpcode()<0)loc=loc.getPrevious();
          if(!(loc instanceof VarInsnNode) || loc.getOpcode()!=ALOAD || ((VarInsnNode)loc).var!=6)throw new IllegalStateException("placement pattern");
          InsnList x=new InsnList(); x.add(new VarInsnNode(ALOAD,1)); x.add(new MethodInsnNode(INVOKESTATIC,base+"patch/EventPlacement","adjust","(Lorg/bukkit/Location;L"+base+"airdrops/Airdrop;)Lorg/bukkit/Location;",false)); x.add(new InsnNode(DUP)); x.add(new VarInsnNode(ASTORE,6));
          m.instructions.insert(loc,x);hooks++;break;
        }
      }
      if(name.endsWith("ActivePeacefulAirdrop") && m.name.equals("onStart")) {
        AbstractInsnNode start=null;
        for(AbstractInsnNode n=m.instructions.getFirst();n!=null;n=n.getNext()) if(n instanceof TypeInsnNode && n.getOpcode()==NEW && ((TypeInsnNode)n).desc.equals("java/util/ArrayList")) {start=n;break;}
        if(start==null) throw new IllegalStateException("peaceful onStart pool not found");
        m.instructions.insertBefore(start,new InsnNode(RETURN));
        for(AbstractInsnNode n=start;n!=null;) {AbstractInsnNode next=n.getNext();m.instructions.remove(n);n=next;}
        m.localVariables=null; hooks++;
      }
      if(name.endsWith("ActivePeacefulAirdrop") && m.name.equals("openLoot")) {
        InsnList x=new InsnList(); x.add(new VarInsnNode(ALOAD,0)); x.add(new VarInsnNode(ALOAD,0)); x.add(new FieldInsnNode(GETFIELD,name,"pendingLoot","Ljava/util/List;")); x.add(new MethodInsnNode(INVOKESTATIC,runtime,"preparePeaceful","(L"+base+"airdrops/ActiveAirdrop;Ljava/util/List;)V",false)); m.instructions.insert(x); hooks++;
      }
      boolean fire=name.endsWith("ActiveFireAirdrop") && m.name.equals("createMaskedLootInventory");
      boolean sky=name.endsWith("ActiveSkyAirdrop") && m.name.equals("openShulker");
      boolean peace=name.endsWith("ActivePeacefulAirdrop") && m.name.equals("openLoot");
      if(fire || sky || peace) {
        for(AbstractInsnNode n=m.instructions.getFirst();n!=null;n=n.getNext()) {
          if(n instanceof MethodInsnNode) {
            MethodInsnNode call=(MethodInsnNode)n;
            if(call.owner.equals("org/bukkit/Bukkit") && call.name.equals("createInventory") && call.desc.endsWith("Lorg/bukkit/inventory/Inventory;")) {
              InsnList x=new InsnList(); x.add(new InsnNode(DUP)); x.add(new MethodInsnNode(INVOKESTATIC,runtime,"track","(Lorg/bukkit/inventory/Inventory;)V",false)); m.instructions.insert(n,x); hooks++;
            }
            if((fire || sky) && call.owner.equals("java/util/Random") && call.name.equals("nextInt")) {
              AbstractInsnNode after=nextOp(n);
              if(after instanceof MethodInsnNode && ((MethodInsnNode)after).owner.equals("java/util/List") && ((MethodInsnNode)after).name.equals("get")) { ((MethodInsnNode)after).name="remove"; hooks++; }
            }
            if((fire || sky) && call.owner.equals("org/bukkit/Bukkit") && call.name.equals("getOnlinePlayers")) {
              AbstractInsnNode size=nextOp(n); if(!(size instanceof MethodInsnNode) || !((MethodInsnNode)size).name.equals("size")) throw new IllegalStateException("online count pattern");
              InsnList x=new InsnList(); x.add(new VarInsnNode(ALOAD,0)); x.add(new MethodInsnNode(INVOKEVIRTUAL,name,"getCenter","()Lorg/bukkit/Location;",false)); x.add(new MethodInsnNode(INVOKESTATIC,base+"patch/NearbyRewards","players","(Lorg/bukkit/Location;)I",false)); m.instructions.insertBefore(n,x); m.instructions.remove(size); AbstractInsnNode next=n.getNext(); m.instructions.remove(n); n=next; hooks++;
            }
          }
          if((fire || sky) && n instanceof VarInsnNode && n.getOpcode()==ISTORE && ((VarInsnNode)n).var==(fire?8:5)) {
            InsnList x=new InsnList(); x.add(new VarInsnNode(ALOAD,0)); x.add(new MethodInsnNode(INVOKESTATIC,runtime,"perContainer","(L"+base+"airdrops/ActiveAirdrop;)I",false)); x.add(new VarInsnNode(ISTORE,fire?8:5)); AbstractInsnNode last=x.getLast(); m.instructions.insert(n,x); n=last; hooks++;
          }
        }
      }
    }
    ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS); c.accept(w); Files.write(p,w.toByteArray());
  }
  static AbstractInsnNode nextOp(AbstractInsnNode n) { do {n=n.getNext();} while(n!=null && n.getOpcode()<0); return n; }
}
