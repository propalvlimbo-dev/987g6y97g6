import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class BytecodeTest implements Opcodes {
 static final String BASE="ru/rooyzee/elytrixairdrop/";
 public static void main(String[] args)throws Exception {
  try(ZipFile jar=new ZipFile(args[0])){
   for(String type:new String[]{"ActiveFireAirdrop","ActiveSkyAirdrop"}){
    ClassNode c=read(jar,BASE+"airdrops/types/"+type); String method=type.contains("Fire")?"createMaskedLootInventory":"openShulker";
    int remove=0, count=0, online=0, masks=0;
    for(MethodNode m:c.methods)if(m.name.equals(method))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)i;if(call.owner.equals("java/util/List")&&call.name.equals("remove")&&call.desc.equals("(I)Ljava/lang/Object;"))remove++;if(call.name.equals("perContainer"))count++;if(call.name.equals("getOnlinePlayers"))online++;if(call.name.equals("track"))masks++;}
    if(remove!=1||count!=1||online!=0||masks!=1)throw new AssertionError(type+" "+remove+" "+count+" "+online+" "+masks);
   }
   ClassNode manager=read(jar,BASE+"managers/AirdropManager");boolean adjust=false;
   for(MethodNode m:manager.methods)if(m.name.equals("start"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(BASE+"patch/EventPlacement") && ((MethodInsnNode)i).name.equals("adjust"))adjust=true;
   if(!adjust)throw new AssertionError("placement hook missing");
   ClassNode peace=read(jar,BASE+"airdrops/types/ActivePeacefulAirdrop");boolean generate=false,tracked=false;
   for(MethodNode m:peace.methods)if(m.name.equals("openLoot"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)i;if(call.name.equals("preparePeaceful"))generate=true;if(call.name.equals("track"))tracked=true;}
   if(!generate||!tracked)throw new AssertionError("peaceful hooks missing");
  }
  System.out.println("Bytecode: placement, masked inventories and draw-without-replacement OK");
 }
 static ClassNode read(ZipFile jar,String name)throws Exception {ClassNode node=new ClassNode();try(InputStream in=jar.getInputStream(jar.getEntry(name+".class"))){new ClassReader(in).accept(node,0);}return node;}
}
