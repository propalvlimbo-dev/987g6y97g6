import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class GalaxyBytecodeTest {
 static final String BASE="ru/rooyzee/elytrixtrader/";
 static ClassNode read(ZipFile z,String name)throws Exception{ClassNode c=new ClassNode();new ClassReader(z.getInputStream(z.getEntry(BASE+name+".class"))).accept(c,0);return c;}
 public static void main(String[] args)throws Exception{
  try(ZipFile z=new ZipFile(args[0])){
   int start=0,remove=0,stop=0,schematic=0,hitStart=0,hitRemove=0,hitStop=0,hitInstall=0;
   for(String name:new String[]{"model/TraderInstance","Main","trader/TraderManager"})for(MethodNode m:read(z,name).methods)for(AbstractInsnNode n:m.instructions){
    if(!(n instanceof MethodInsnNode))continue;MethodInsnNode c=(MethodInsnNode)n;
    if(c.owner.equals(BASE+"patch/TraderGalaxy")){if(c.name.equals("start"))start++;if(c.name.equals("remove"))remove++;if(c.name.equals("stopAll"))stop++;}
    if(c.owner.equals(BASE+"patch/TraderHitbox")){if(c.name.equals("start"))hitStart++;if(c.name.equals("remove"))hitRemove++;if(c.name.equals("stopAll"))hitStop++;if(c.name.equals("install"))hitInstall++;}
    if(name.equals("trader/TraderManager") && m.name.equals("spawnInternal") && c.owner.equals(BASE+"model/SchematicConfig") && c.name.equals("enabled"))schematic++;
   }
   if(start!=1||remove!=4||stop!=1||schematic!=0)throw new AssertionError(start+" "+remove+" "+stop+" "+schematic);
   if(hitStart!=1||hitRemove!=4||hitStop!=1||hitInstall!=1)throw new AssertionError("Bad hitbox lifecycle");
   boolean fallbackDisabled=false;
   for(MethodNode m:read(z,"listener/TradeListener").methods)if(m.name.equals("onInteract")){
    int real=0;for(AbstractInsnNode n:m.instructions)if(n.getOpcode()>=0){real++;if(n.getOpcode()!=Opcodes.RETURN)throw new AssertionError("Block/air fallback remains");}fallbackDisabled=real==1;
   }
   if(!fallbackDisabled)throw new AssertionError("Missing disabled block fallback");
   int rad=0,ys=0,overlap=0,contains=0;
   for(MethodNode m:read(z,"region/RegionManager").methods)if(m.name.equals("createRegion")||m.name.equals("wouldOverlap")){
    if(m.name.equals("createRegion")){AbstractInsnNode first=m.instructions.getFirst();while(first!=null&&first.getOpcode()<0)first=first.getNext();if(first instanceof IntInsnNode && ((IntInsnNode)first).operand==8)rad++;}
    for(AbstractInsnNode ins:m.instructions)if(ins instanceof MethodInsnNode){MethodInsnNode c=(MethodInsnNode)ins;if(c.owner.equals("org/bukkit/Location")&&c.name.equals("getBlockY"))ys++;if(c.owner.equals(BASE+"patch/TraderBounds")&&c.name.equals("overlaps"))overlap++;}
   }
   for(MethodNode m:read(z,"region/TraderRegion").methods)if(m.name.equals("contains")||m.name.equals("containsBlock"))for(AbstractInsnNode ins:m.instructions)if(ins instanceof MethodInsnNode && ((MethodInsnNode)ins).owner.equals(BASE+"patch/TraderBounds"))contains++;
   if(rad!=1||ys!=4||overlap!=1||contains!=2)throw new AssertionError("Region hooks: "+rad+" "+ys+" "+overlap+" "+contains);
   int spawn=0,teleport=0;
   for(MethodNode m:read(z,"patch/TraderGalaxy$Orbit").methods)for(AbstractInsnNode n:m.instructions){
    if(n instanceof TypeInsnNode && ((TypeInsnNode)n).desc.contains("VirtualExperienceOrb"))throw new AssertionError("Old orbs remain");
    if(n instanceof MethodInsnNode){MethodInsnNode c=(MethodInsnNode)n;
     if(c.owner.contains("VirtualExperienceOrb"))throw new AssertionError("Old orb calls remain");
     if(c.owner.equals("org/bukkit/World") && c.name.equals("spawn"))spawn++;
     if(c.owner.equals("org/bukkit/entity/BlockDisplay") && c.name.equals("teleport"))teleport++;
     if(c.owner.equals("org/bukkit/block/Block") && c.name.startsWith("set"))throw new AssertionError("World blocks modified");
    }
   }
   if(spawn!=1 || teleport!=1)throw new AssertionError("Missing block animation");
   if(z.getEntry("org/bukkit/Bukkit.class")!=null || z.getEntry(BASE+"patch/TraderGalaxy.class")==null)throw new AssertionError("Bad packaging");
   System.out.println("Region, hitbox, disabled block fallback, flying blocks and no-schematic hooks OK");
  }
 }
}
