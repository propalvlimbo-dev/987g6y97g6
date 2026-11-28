import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class GalaxyBytecodeTest {
 static final String BASE="ru/rooyzee/elytrixtrader/";
 static ClassNode read(ZipFile z,String name)throws Exception{ClassNode c=new ClassNode();new ClassReader(z.getInputStream(z.getEntry(BASE+name+".class"))).accept(c,0);return c;}
 public static void main(String[] args)throws Exception{
  try(ZipFile z=new ZipFile(args[0])){
   int start=0,remove=0,stop=0,schematic=0;
   for(String name:new String[]{"model/TraderInstance","Main","trader/TraderManager"})for(MethodNode m:read(z,name).methods)for(AbstractInsnNode n:m.instructions){
    if(!(n instanceof MethodInsnNode))continue;MethodInsnNode c=(MethodInsnNode)n;
    if(c.owner.equals(BASE+"patch/TraderGalaxy")){if(c.name.equals("start"))start++;if(c.name.equals("remove"))remove++;if(c.name.equals("stopAll"))stop++;}
    if(name.equals("trader/TraderManager") && m.name.equals("spawnInternal") && c.owner.equals(BASE+"model/SchematicConfig") && c.name.equals("enabled"))schematic++;
   }
   if(start!=1||remove!=4||stop!=1||schematic!=0)throw new AssertionError(start+" "+remove+" "+stop+" "+schematic);
   int orbCreate=0,orbTrack=0;
   for(MethodNode m:read(z,"patch/TraderGalaxy$Orbit").methods)for(AbstractInsnNode n:m.instructions){
    if(n instanceof TypeInsnNode && ((TypeInsnNode)n).desc.contains("BlockDisplay"))throw new AssertionError("Old ground blocks remain");
    if(n instanceof MethodInsnNode){MethodInsnNode c=(MethodInsnNode)n;
     if(c.owner.equals("dev/by1337/virtualentity/api/virtual/VirtualExperienceOrb") && c.name.equals("create"))orbCreate++;
     if(c.owner.equals("dev/by1337/virtualentity/api/tracker/PlayerTracker") && c.name.equals("tick"))orbTrack++;
     if(c.owner.equals("org/bukkit/World") && (c.name.equals("spawn") || c.name.equals("setBlock")))throw new AssertionError("Unexpected world modification");
    }
   }
   if(orbCreate!=1 || orbTrack!=1)throw new AssertionError("Missing orb lifecycle");
   if(z.getEntry("dev/by1337/virtualentity/api/virtual/VirtualExperienceOrb.class")!=null)throw new AssertionError("Test stub leaked");
   if(z.getEntry("org/bukkit/Bukkit.class")!=null || z.getEntry(BASE+"patch/TraderGalaxy.class")==null)throw new AssertionError("Bad packaging");
   System.out.println("Galaxy and no-schematic bytecode hooks OK");
  }
 }
}
