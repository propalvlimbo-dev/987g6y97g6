import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class BytecodeTest implements Opcodes {
 static final String BASE="ru/rooyzee/elytrixairdrop/";
 public static void main(String[] args)throws Exception {
  try(ZipFile jar=new ZipFile(args[0])){
   for(String type:new String[]{"ActiveSkyAirdrop"}){
    ClassNode c=read(jar,BASE+"airdrops/types/"+type); String method=type.contains("Fire")?"createMaskedLootInventory":"openShulker";
    int remove=0, count=0, online=0, masks=0;
    for(MethodNode m:c.methods)if(m.name.equals(method))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)i;if(call.owner.equals("java/util/List")&&call.name.equals("remove")&&call.desc.equals("(I)Ljava/lang/Object;"))remove++;if(call.name.equals("perContainer"))count++;if(call.name.equals("getOnlinePlayers"))online++;if(call.name.equals("track"))masks++;}
    if(remove!=1||count!=1||online!=0||masks!=1)throw new AssertionError(type+" "+remove+" "+count+" "+online+" "+masks);
   }
   ClassNode fire=read(jar,BASE+"airdrops/types/ActiveFireAirdrop");boolean fireTrack=false,fireRoll=false;
   for(MethodNode m:fire.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode a=(MethodInsnNode)i;if(a.owner.equals(BASE+"patch/LootRuntime")&&a.name.equals("track"))fireTrack=true;if(a.owner.equals(BASE+"services/LootFileService")&&a.name.equals("rollRewards"))fireRoll=true;}
   if(!fireTrack||!fireRoll)throw new AssertionError("fire reward hooks missing");
   boolean damage=false,drop=false;
   for(MethodNode m:fire.methods)if(m.name.equals("impactSmall"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode a=(MethodInsnNode)i;if(a.name.equals("damage"))damage=true;if(a.name.equals("dropItemNaturally"))drop=true;if(a.name.equals("setType")||a.name.equals("createExplosion"))throw new AssertionError("small impact changes terrain");}
   if(!damage||!drop)throw new AssertionError("small meteor impact missing");
   ClassNode protection=read(jar,BASE+"listeners/ProtectionListener");int bypass=0;
   for(MethodNode m:protection.methods)if(m.name.equals("onTeleport")||m.name.equals("onPearlThrow"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(BASE+"patch/SkyMechanics") && ((MethodInsnNode)i).name.equals("skyActive"))bypass++;
   if(bypass!=2)throw new AssertionError("sky teleport hooks "+bypass);
   ClassNode main=read(jar,BASE+"Main");boolean chest=false, sky=false;
   for(MethodNode m:main.methods)if(m.name.equals("onEnable"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode a=(MethodInsnNode)i;if(a.owner.equals(BASE+"patch/PeacefulChest")&&a.name.equals("install"))chest=true;if(a.owner.equals(BASE+"patch/SkyMechanics")&&a.name.equals("install"))sky=true;}
   if(!chest||!sky)throw new AssertionError("mechanics installation missing");
   ClassNode safeChest=read(jar,BASE+"patch/PeacefulChest");
   for(MethodNode m:safeChest.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode a=(MethodInsnNode)i;if(a.name.equals("openInventory"))throw new AssertionError("unsafe reopen remains");}
   ClassNode skyPatch=read(jar,BASE+"patch/SkyMechanics");
   for(MethodNode m:skyPatch.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode a=(MethodInsnNode)i;if(a.name.equals("setX") || a.name.equals("setZ") || a.name.equals("runTaskLater"))throw new AssertionError("sideways wind or delayed push remains");}
   ClassNode manager=read(jar,BASE+"managers/AirdropManager");boolean adjust=false;
   for(MethodNode m:manager.methods)if(m.name.equals("start"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(BASE+"patch/EventPlacement") && ((MethodInsnNode)i).name.equals("adjust"))adjust=true;
   if(!adjust)throw new AssertionError("placement hook missing");
   boolean safeCleanup=false;for(MethodNode m:manager.methods)if(m.name.equals("stop"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(BASE+"patch/FireCleanup"))safeCleanup=true;
   if(!safeCleanup)throw new AssertionError("fire item cleanup still destructive");
   ClassNode peace=read(jar,BASE+"airdrops/types/ActivePeacefulAirdrop");boolean generate=false,tracked=false;
   for(MethodNode m:peace.methods)if(m.name.equals("openLoot"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)i;if(call.name.equals("preparePeaceful"))generate=true;if(call.name.equals("track"))tracked=true;}
   if(!generate||!tracked)throw new AssertionError("peaceful hooks missing");
  }
  System.out.println("Bytecode: placement, masked inventories and draw-without-replacement sky teleport hooks, no chest reopen, sideways wind or delayed push OK");
 }
 static ClassNode read(ZipFile jar,String name)throws Exception {ClassNode node=new ClassNode();try(InputStream in=jar.getInputStream(jar.getEntry(name+".class"))){new ClassReader(in).accept(node,0);}return node;}
}
