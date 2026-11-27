import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
/** Static integration checks: descriptor, hook, curated IDs and the target item API. */
public class PresetBytecodeTest {
 public static void main(String[] args)throws Exception{
  String base="ru/rooyzee/elytrixairdrop/";
  try(ZipFile output=new ZipFile(args[0]);ZipFile items=new ZipFile(args[1])){
   String yaml=new String(output.getInputStream(output.getEntry("plugin.yml")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
   if(!yaml.contains("airdroplootpreset:")||!yaml.contains("ElytrixItem")||!yaml.contains("permission: elytrixairdrop.admin"))throw new AssertionError("descriptor");
   ClassNode main=read(output,base+"Main");boolean hook=false;
   for(MethodNode m:main.methods)if(m.name.equals("onEnable"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(base+"patch/LootPreset") && ((MethodInsnNode)i).name.equals("install"))hook=true;
   if(!hook)throw new AssertionError("preset install hook");
   ClassNode preset=read(output,base+"patch/LootPreset");Set<String> ids=new HashSet<>();
   for(MethodNode m:preset.methods)if(m.name.equals("create"))for(AbstractInsnNode i:m.instructions)if(i instanceof LdcInsnNode && ((LdcInsnNode)i).cst instanceof String)ids.add((String)((LdcInsnNode)i).cst);
   String[] required={"anti_pearl","explosive_trap","potion_healt","potion_lightness","potion_voina","potion_disappearances","potion_sportsmena","potion_salvation","arrow1","arrow2","arrow3","arrow5","arrow_levit","totem1","totem2","totem3","shar1","shar2","tyagun","collector_sword","infinite_firework","experience_pot","kirkaraskola","shalker1","shalker2","shalker3","shalker4","shalker5","elytra1","shar3","shar4","totem4","totem5"};
   Set<String> itemConstants=new HashSet<>();Enumeration<? extends ZipEntry> entries=items.entries();
   while(entries.hasMoreElements()){
    ZipEntry e=entries.nextElement();if(!e.getName().endsWith(".class"))continue;
    ClassNode node=new ClassNode();try(InputStream in=items.getInputStream(e)){new ClassReader(in).accept(node,0);}
    for(FieldNode f:node.fields)if(f.value instanceof String)itemConstants.add((String)f.value);
    for(MethodNode m:node.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof LdcInsnNode && ((LdcInsnNode)i).cst instanceof String)itemConstants.add((String)((LdcInsnNode)i).cst);
   }
   for(String id:required)if(!ids.contains(id)||!itemConstants.contains(id))throw new AssertionError("Missing preset/item ID "+id);
   ClassNode itemMain=read(items,"ru/rooyzee/elytrixitem/Main");boolean registry=false;
   for(MethodNode m:itemMain.methods)if(m.name.equals("getItemRegistry"))registry=true;
   ClassNode itemInterface=read(items,"ru/rooyzee/elytrixitem/item/CustomItem");boolean create=false,stack=false;
   for(MethodNode m:itemInterface.methods){if(m.name.equals("createItem"))create=true;if(m.name.equals("isStackable"))stack=true;}
   if(!registry||!create||!stack)throw new AssertionError("ElytrixItem API");
   System.out.println("Preset descriptor, hook and "+required.length+" ElytrixItem IDs/API OK (static check)");
  }
 }
 static ClassNode read(ZipFile jar,String name)throws Exception{ZipEntry e=jar.getEntry(name+".class");if(e==null)throw new AssertionError(name);ClassNode c=new ClassNode();try(InputStream in=jar.getInputStream(e)){new ClassReader(in).accept(c,0);}return c;}
}
