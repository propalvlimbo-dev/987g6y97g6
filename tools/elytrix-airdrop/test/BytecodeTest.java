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
   boolean debrisHook=false,crater=false,guard=false,fragmentSpawn=false,fragmentCleanup=false,damageValue=false;
   for(MethodNode m:fire.methods)for(AbstractInsnNode i:m.instructions){
    if(m.name.equals("impactSmall") && i instanceof LdcInsnNode && Double.valueOf(14.0).equals(((LdcInsnNode)i).cst))damageValue=true;
    if(i instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)i;
     if(m.name.equals("impactSmall")&&call.name.equals("spawnDebris"))debrisHook=true;
     if(m.name.equals("carveImpact")&&call.name.equals("setType"))crater=true;
     if(m.name.equals("carveImpact")&&call.name.equals("containsKey"))guard=true;
     if(m.name.equals("spawnDebris")&&call.name.equals("spawn"))fragmentSpawn=true;
     if(m.name.equals("spawnDebris")&&(call.name.equals("setType")||call.name.equals("dropItemNaturally")))throw new AssertionError("debris mutates terrain or drops items");
     if(m.name.equals("onStop")&&call.name.equals("remove"))fragmentCleanup=true;
    }
   }
   if(!debrisHook||!crater||!guard||!fragmentSpawn||!fragmentCleanup||!damageValue)throw new AssertionError("impact visual, terrain guard or damage missing");
   ClassNode flight=read(jar,BASE+"airdrops/types/ActiveFireAirdrop$Flight");boolean shatter=false,transfers=false;
   boolean quickSmall=false,arenaStart=false,arenaStop=false,arenaLoot=false,emptyRelease=false;
   for(MethodNode m:flight.methods)if(m.name.equals("advance"))for(AbstractInsnNode i:m.instructions)if(i instanceof IntInsnNode && i.getOpcode()==BIPUSH && ((IntInsnNode)i).operand==20)quickSmall=true;
   for(MethodNode m:fire.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode a=(MethodInsnNode)i;
    if(a.owner.equals(BASE+"patch/FireArena") && a.name.equals("start") && m.name.equals("onStart"))arenaStart=true;
    if(a.owner.equals(BASE+"patch/FireArena") && a.name.equals("close")){
     if(m.name.equals("onStop"))arenaStop=true;if(m.name.equals("handleLootClick"))arenaLoot=true;if(m.name.equals("createLoot"))emptyRelease=true;
    }
   }
   if(!quickSmall||!arenaStart||!arenaStop||!arenaLoot||!emptyRelease)throw new AssertionError("fire arena lifecycle or accelerated small meteor missing");
   for(MethodNode m:fire.methods)if(m.name.equals("animate"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).name.equals("shatter"))shatter=true;
   for(MethodNode m:flight.methods)if(m.name.equals("shatter"))for(AbstractInsnNode i:m.instructions)if(i instanceof TypeInsnNode && ((TypeInsnNode)i).desc.equals(BASE+"airdrops/types/ActiveFireAirdrop$Debris"))transfers=true;
   if(!shatter||!transfers)throw new AssertionError("meteor displays must shatter into temporary debris");
   ClassNode holo=read(jar,BASE+"managers/HologramManager");boolean noBackground=false,transparent=false,namedSky=false,scale=false,shadow=false;
   for(MethodNode m:holo.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)i;
    if(call.name.equals("setDefaultBackground"))noBackground=true;if(call.name.equals("setBackgroundColor"))transparent=true;if(call.name.equals("setTransformation"))scale=true;if(call.name.equals("setShadowed"))shadow=true;
    if(call.owner.startsWith("eu/decentsoftware/"))throw new AssertionError("unexpected DecentHolograms API dependency");
   }
   ClassNode skyClass=read(jar,BASE+"airdrops/types/ActiveSkyAirdrop");
   for(MethodNode m:skyClass.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)i;
    if(call.owner.equals(BASE+"managers/HologramManager")&&call.name.equals("createSky"))namedSky=true;
    if(call.owner.equals("eu/decentsoftware/holograms/api/DHAPI"))throw new AssertionError("sky still creates old holograms");
   }
   boolean moved=false;for(MethodNode m:fire.methods)if(m.name.equals("onStart"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).name.equals("moveHologram"))moved=true;
   if(!noBackground||!transparent||!namedSky||!moved||!scale||!shadow)throw new AssertionError("transparent holograms, sky creation or fire positioning missing");
   ClassNode protection=read(jar,BASE+"listeners/ProtectionListener");int bypass=0;
   for(MethodNode m:protection.methods)if(m.name.equals("onTeleport")||m.name.equals("onPearlThrow"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(BASE+"patch/SkyMechanics") && ((MethodInsnNode)i).name.equals("skyActive"))bypass++;
   if(bypass!=2)throw new AssertionError("sky teleport hooks "+bypass);
   int pearlThrow=0,pearlTeleport=0;
   for(MethodNode m:protection.methods)if(m.name.equals("onTeleport")||m.name.equals("onPearlThrow"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode a=(MethodInsnNode)i;if(a.owner.equals(BASE+"patch/FirePearls")&&a.name.equals("allowThrow")&&m.name.equals("onPearlThrow"))pearlThrow++;if(a.owner.equals(BASE+"patch/FirePearls")&&a.name.equals("allowTeleport")&&m.name.equals("onTeleport"))pearlTeleport++;}
   if(pearlThrow!=1||pearlTeleport!=1)throw new AssertionError("fire pearl exception missing");
   ClassNode main=read(jar,BASE+"Main");boolean chest=false, sky=false;
   for(MethodNode m:main.methods)if(m.name.equals("onEnable"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode a=(MethodInsnNode)i;if(a.owner.equals(BASE+"patch/PeacefulChest")&&a.name.equals("install"))chest=true;if(a.owner.equals(BASE+"patch/SkyMechanics")&&a.name.equals("install"))sky=true;}
   if(!chest||!sky)throw new AssertionError("mechanics installation missing");
   ClassNode arenaClass=read(jar,BASE+"patch/FireArena"),wallClass=read(jar,BASE+"patch/FireWall");boolean fromClipboard=false,wallStart=false,privateBlocks=false,hidden=false,recover=false;
   for(MethodNode m:arenaClass.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode a=(MethodInsnNode)i;if(a.name.equals("getFullBlock")&&a.owner.contains("Clipboard"))fromClipboard=true;if(a.owner.equals(BASE+"patch/FireWall")&&a.name.equals("start"))wallStart=true;}
   for(MethodNode m:wallClass.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode a=(MethodInsnNode)i;if(a.name.equals("sendBlockChange"))privateBlocks=true;if(a.name.equals("setVisibleByDefault"))hidden=true;}
   for(MethodNode m:main.methods)if(m.name.equals("onEnable"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(BASE+"patch/FireWall")&&((MethodInsnNode)i).name.equals("recover"))recover=true;
   if(!fromClipboard||!wallStart||!privateBlocks||!hidden||!recover)throw new AssertionError("schematic contour, private wall or legacy recovery hook missing");
   ClassNode safeChest=read(jar,BASE+"patch/PeacefulChest");
   boolean scheduled=false,reopened=false,guarded=false,reasonCheck=false;
   for(MethodNode m:safeChest.methods)for(AbstractInsnNode i:m.instructions){
    if(i instanceof MethodInsnNode){MethodInsnNode a=(MethodInsnNode)i;
     if(m.name.equals("close")&&a.name.equals("openInventory"))throw new AssertionError("synchronous reopen in close handler");
     if(m.name.equals("close")&&a.name.equals("runTaskLater"))scheduled=true;
     if(m.name.equals("close")&&a.name.equals("getReason"))reasonCheck=true;
     if(m.name.equals("reopen")&&a.name.equals("openInventory"))reopened=true;
     if(m.name.equals("close")&&a.owner.equals(BASE+"patch/ReopenGuard")&&a.name.equals("queue"))guarded=true;
    }
   }
   if(!scheduled||!reopened||!guarded||!reasonCheck)throw new AssertionError("guarded deferred peaceful reopen missing");
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
   boolean lowOnlineSilenced=false, schedulerOnline=false, retryField=false;
   for(MethodNode m:manager.methods)if(m.name.startsWith("lambda$startScheduler$"))for(AbstractInsnNode i:m.instructions){
    if(i instanceof LdcInsnNode && "Airdrop delayed: online < 5".equals(((LdcInsnNode)i).cst)){
      AbstractInsnNode next=i.getNext();while(next!=null && next.getOpcode()<0)next=next.getNext();
      if(next==null || next.getOpcode()!=POP2)throw new AssertionError("low online log still active");lowOnlineSilenced=true;
    }
    if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals("org/bukkit/Bukkit") && ((MethodInsnNode)i).name.equals("getOnlinePlayers"))schedulerOnline=true;
    if(i instanceof FieldInsnNode && ((FieldInsnNode)i).name.equals("nextRetry"))retryField=true;
   }
   if(!lowOnlineSilenced || !schedulerOnline || !retryField)throw new AssertionError("scheduler low online suppression/check missing");
   int timeoutHooks=0; for(MethodNode m:manager.methods)if(m.name.startsWith("lambda$startScheduler$"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(BASE+"patch/PeacefulMechanics") && ((MethodInsnNode)i).name.equals("endTime"))timeoutHooks++;
   boolean life=false,oldLaunch=false,grace=false,installed=false;
   for(MethodNode m:peace.methods){for(AbstractInsnNode i:m.instructions){
    if(m.name.equals("onTick") && i instanceof MethodInsnNode){MethodInsnNode a=(MethodInsnNode)i;if(a.name.equals("lifetime") && a.owner.equals(BASE+"patch/PeacefulMechanics"))life=true;if(a.owner.equals(BASE+"services/LaunchService") && a.name.equals("launch"))oldLaunch=true;}
    if(m.name.equals("handleLootClick") && i instanceof LdcInsnNode && Long.valueOf(300L).equals(((LdcInsnNode)i).cst))grace=true;
   }}
   for(MethodNode m:main.methods)if(m.name.equals("onEnable"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(BASE+"patch/PeacefulMechanics") && ((MethodInsnNode)i).name.equals("install"))installed=true;
   if(timeoutHooks!=2 || !life || oldLaunch || !grace || !installed)throw new AssertionError("peaceful timed launch or grace hooks missing");
   int limitChanges=0;boolean waveSize=false;
   for(MethodNode m:peace.methods)if(m.name.equals("spawnWave")||m.name.equals("handleLootClick"))for(AbstractInsnNode i:m.instructions){
    if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(BASE+"patch/PeacefulWave") && ((MethodInsnNode)i).name.equals("size"))waveSize=true;
    if(i instanceof IntInsnNode && ((IntInsnNode)i).operand==48 && i.getOpcode()==BIPUSH)limitChanges++;
   }
   if(!waveSize||limitChanges!=2)throw new AssertionError("peaceful 1-2 wave or limit missing");
  }
  System.out.println("Bytecode: placement, masked inventories and draw-without-replacement sky teleport hooks, guarded deferred chest reopen, no sideways wind or delayed push OK");
 }
 static ClassNode read(ZipFile jar,String name)throws Exception {ClassNode node=new ClassNode();try(InputStream in=jar.getInputStream(jar.getEntry(name+".class"))){new ClassReader(in).accept(node,0);}return node;}
}
