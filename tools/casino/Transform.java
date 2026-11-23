import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

public class Transform implements Opcodes {
 static final String P="ru/rooyzee/elytrixcasino/", F=P+"patch/FairGames", R=P+"patch/SpinRegistry", AM=P+"bet/ActiveBetManager", AG=P+"gui/AutomateGui", INNER=AG+"$1";
 static Map<String,Integer> counts=new TreeMap<>();
 static void hit(String key){counts.merge(key,1,Integer::sum);}
 static MethodNode method(ClassNode c,String name){return c.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow(()->new IllegalStateException(c.name+"."+name));}
 static void replace(MethodNode m,InsnList list){m.instructions=list;m.tryCatchBlocks.clear();m.localVariables=null;}
 static void directStatic(ClassNode c,String name,String owner,String target,String desc,int returnOp){
   MethodNode m=method(c,name);InsnList a=new InsnList();a.add(new MethodInsnNode(INVOKESTATIC,owner,target,desc,false));a.add(new InsnNode(returnOp));replace(m,a);hit(c.name+"."+name);
 }
 static byte[] transform(byte[] raw){
  ClassNode c=new ClassNode();new ClassReader(raw).accept(c,0); String name=c.name;
  String game=name.startsWith(P+"gui/AutomateGui")?"Automate":name.equals(P+"gui/MinerGui")?"Miner":(name.equals(P+"gui/RouletteGui")||name.equals(P+"roulette/Roulette"))?"Roulette":null;
  for(MethodNode m:c.methods){
   int online=0, loreAdds=0;
   for(AbstractInsnNode i=m.instructions.getFirst();i!=null;){AbstractInsnNode next=i.getNext();
    if(i instanceof MethodInsnNode call){
     if(name.equals(AG)&&m.name.equals("buildComboItems")&&call.owner.equals("java/util/List")&&call.name.equals("add")) {
       loreAdds++; if(loreAdds==2||loreAdds==3){call.setOpcode(INVOKESTATIC);call.owner=F;call.name="skipLore";call.desc="(Ljava/util/List;Ljava/lang/Object;)Z";call.itf=false;hit("slot.lore");}
     }
     if(game!=null&&call.owner.equals(AM)&&call.desc.equals("(Ljava/util/UUID;)V")&&call.name.equals("release")){call.name="release"+game;hit("release."+game);}
     if(game!=null&&call.owner.equals(AM)&&call.name.equals("getActiveBet")){call.name="get"+game+"Bet";hit("check."+game);}
     if(name.equals(INNER)&&m.name.equals("run")&&call.owner.equals("org/bukkit/entity/Player")&&call.name.equals("isOnline")) {
       online++;if(online>=2){InsnList add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(new FieldInsnNode(GETFIELD,INNER,"val$inventory","Lorg/bukkit/inventory/Inventory;"));m.instructions.insertBefore(call,add);call.setOpcode(INVOKESTATIC);call.owner=F;call.name="viewing";call.desc="(Lorg/bukkit/entity/Player;Lorg/bukkit/inventory/Inventory;)Z";call.itf=false;hit("spin.sounds");}
     }
     if(name.equals(AG)&&m.name.equals("startSpin")&&call.name.equals("runTaskTimer")&&call.owner.equals(INNER)){
       call.setOpcode(INVOKESTATIC);call.owner=R;call.name="schedule";call.desc="(Lorg/bukkit/scheduler/BukkitRunnable;Lorg/bukkit/plugin/Plugin;JJ)Lorg/bukkit/scheduler/BukkitTask;";hit("spin.schedule");
     }
     if(name.equals(INNER)&&m.name.equals("run")&&call.owner.equals(AG)&&call.name.equals("finish")) {
       // Keep finish arguments on stack; unregister using a separate top-of-stack argument.
       InsnList a=new InsnList();a.add(new VarInsnNode(ALOAD,0));a.add(new MethodInsnNode(INVOKESTATIC,R,"done","(L"+R+"$Finishable;)V",false));m.instructions.insertBefore(call,a);hit("spin.done");
     }
     if((name.equals(AG)&&m.name.equals("finish")||name.equals(P+"roulette/Roulette")&&m.name.equals("finish")||name.equals(P+"miner/MinerSettings$CoinTier")&&m.name.equals("rewardForBet"))&&call.owner.equals("java/lang/Math")&&call.name.equals("round")&&call.desc.equals("(D)J")){
       call.owner=F;call.name="payout";hit("payout.floor");
     }
     if(name.equals(P+"gui/MinerGui")&&m.name.equals("shutdown")&&call.owner.equals(P+"miner/MinerSession")&&call.name.equals("getBet")){call.name="getCollectedCoins";hit("miner.shutdown");}
    }
    i=next;
   }
  }
  if(name.equals(P+"automate/Automate")){
   MethodNode m=method(c,"isBusy");InsnList a=new InsnList();a.add(new InsnNode(ICONST_0));a.add(new InsnNode(IRETURN));replace(m,a);hit("automate.busy");
  }
  if(name.equals(P+"automate/AutomateSettings")){
   MethodNode m=method(c,"getSymbols");InsnList a=new InsnList();a.add(new VarInsnNode(ALOAD,0));a.add(new FieldInsnNode(GETFIELD,c.name,"symbols","Ljava/util/List;"));a.add(new MethodInsnNode(INVOKESTATIC,F,"symbols","(Ljava/util/List;)Ljava/util/List;",false));a.add(new InsnNode(ARETURN));replace(m,a);hit("slot.symbols");
   m=method(c,"getMaxMultiplier");a=new InsnList();a.add(new LdcInsnNode(6.0));a.add(new InsnNode(DRETURN));replace(m,a);hit("slot.cap");
   m=method(c,"getTotalWeight");a=new InsnList();a.add(new InsnNode(ICONST_5));a.add(new InsnNode(IRETURN));replace(m,a);hit("slot.weight");
  }
  if(name.equals(AG)){
   MethodNode m=method(c,"createGrid");InsnList a=new InsnList();a.add(new VarInsnNode(ALOAD,0));a.add(new FieldInsnNode(GETFIELD,AG,"plugin","L"+P+"Main;"));a.add(new MethodInsnNode(INVOKEVIRTUAL,P+"Main","getAutomateSettings","()L"+P+"automate/AutomateSettings;",false));a.add(new MethodInsnNode(INVOKEVIRTUAL,P+"automate/AutomateSettings","getSymbols","()Ljava/util/List;",false));a.add(new VarInsnNode(ALOAD,1));a.add(new MethodInsnNode(INVOKESTATIC,F,"grid","(Ljava/util/List;Ljava/util/concurrent/ThreadLocalRandom;)[[L"+P+"automate/SlotSymbol;",false));a.add(new InsnNode(ARETURN));replace(m,a);hit("slot.grid");
  }
  if(name.equals(P+"roulette/RouletteSettings")){
   directStatic(c,"getSectors",F,"roulette","()Ljava/util/List;",ARETURN);
   MethodNode m=method(c,"getTotalWeight");InsnList a=new InsnList();a.add(new IntInsnNode(BIPUSH,100));a.add(new InsnNode(IRETURN));replace(m,a);hit("roulette.weight");
  }
  if(name.equals(P+"miner/MinerSettings")){
   directStatic(c,"getCoinTiers",F,"coins","()Ljava/util/List;",ARETURN);
   for(String getter:new String[]{"getMinesCount","getEmptyCount"}){MethodNode m=method(c,getter);InsnList a=new InsnList();a.add(new IntInsnNode(BIPUSH,getter.equals("getMinesCount")?14:11));a.add(new InsnNode(IRETURN));replace(m,a);hit("miner."+getter);}
  }
  if(name.equals(P+"Main")) for(String method:new String[]{"onDisable","reload"}) {MethodNode m=method(c,method);m.instructions.insert(new MethodInsnNode(INVOKESTATIC,R,"finishAll","()V",false));hit("spin."+method);}
  if(name.equals(INNER)){
   c.interfaces.add(R+"$Finishable"); MethodNode m=new MethodNode(ACC_PUBLIC,"finishNow","()V",null,null);InsnList a=m.instructions;
   a.add(new VarInsnNode(ALOAD,0));a.add(new FieldInsnNode(GETFIELD,INNER,"val$player","Lorg/bukkit/entity/Player;"));a.add(new VarInsnNode(ALOAD,0));a.add(new FieldInsnNode(GETFIELD,INNER,"val$inventory","Lorg/bukkit/inventory/Inventory;"));a.add(new MethodInsnNode(INVOKESTATIC,F,"closeIfViewing","(Lorg/bukkit/entity/Player;Lorg/bukkit/inventory/Inventory;)V",false));
   a.add(new VarInsnNode(ALOAD,0));a.add(new MethodInsnNode(INVOKEVIRTUAL,INNER,"cancel","()V",false));
   String[][] fields={{"this$0","L"+AG+";"},{"val$player","Lorg/bukkit/entity/Player;"},{"val$automate","L"+P+"automate/Automate;"},{"val$holder","L"+P+"gui/AutomateHolder;"},{"val$inventory","Lorg/bukkit/inventory/Inventory;"},{"val$grid","[[L"+P+"automate/SlotSymbol;"},{"val$bet","J"}};
   for(String[] f:fields){a.add(new VarInsnNode(ALOAD,0));a.add(new FieldInsnNode(GETFIELD,INNER,f[0],f[1]));}
   a.add(new MethodInsnNode(INVOKEVIRTUAL,AG,"finish","(Lorg/bukkit/entity/Player;L"+P+"automate/Automate;L"+P+"gui/AutomateHolder;Lorg/bukkit/inventory/Inventory;[[L"+P+"automate/SlotSymbol;J)V",false));a.add(new InsnNode(RETURN));c.methods.add(m);hit("spin.force");
  }
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();
 }
 public static void main(String[] args)throws Exception{
  Path input=Path.of(args[0]),classes=Path.of(args[1]),output=Path.of(args[2]);
  try(ZipFile z=new ZipFile(input.toFile());ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(output))){
   Enumeration<? extends ZipEntry> en=z.entries();while(en.hasMoreElements()){
    ZipEntry entry=en.nextElement();String n=entry.getName();if(n.equals(P+"bet/ActiveBetManager.class"))continue;
    byte[] data=z.getInputStream(entry).readAllBytes();if(n.endsWith(".class"))data=transform(data);
    if(n.equals("plugin.yml"))data=new String(data,java.nio.charset.StandardCharsets.UTF_8).replace("version: '2.2.1'","version: '2.2.1-fix1'").getBytes(java.nio.charset.StandardCharsets.UTF_8);
    out.putNextEntry(new ZipEntry(n));out.write(data);out.closeEntry();
   }
   for(String dir:new String[]{"ru/rooyzee/elytrixcasino/bet","ru/rooyzee/elytrixcasino/patch"}) try(var files=Files.walk(classes.resolve(dir))){for(Path p:files.filter(Files::isRegularFile).toList()){out.putNextEntry(new ZipEntry(classes.relativize(p).toString()));out.write(Files.readAllBytes(p));out.closeEntry();}}
  }
  Map<String,Integer> expected=Map.ofEntries(Map.entry("release.Automate",3),Map.entry("release.Miner",5),Map.entry("release.Roulette",3),Map.entry("check.Automate",1),Map.entry("check.Miner",1),Map.entry("check.Roulette",2),Map.entry("spin.sounds",2),Map.entry("spin.schedule",1),Map.entry("spin.done",1),Map.entry("payout.floor",3),Map.entry("miner.shutdown",1),Map.entry("spin.force",1),Map.entry("spin.reload",1),Map.entry("spin.onDisable",1),Map.entry("slot.grid",1),Map.entry("slot.symbols",1),Map.entry("slot.weight",1),Map.entry("slot.cap",1),Map.entry("slot.lore",2),Map.entry("automate.busy",1),Map.entry("roulette.weight",1),Map.entry(P+"roulette/RouletteSettings.getSectors",1),Map.entry(P+"miner/MinerSettings.getCoinTiers",1),Map.entry("miner.getMinesCount",1),Map.entry("miner.getEmptyCount",1));
  if(!counts.equals(expected))throw new IllegalStateException("Unexpected patch counts: "+counts+" expected "+expected);
  System.out.println("Patch counts: "+counts);
 }
}
