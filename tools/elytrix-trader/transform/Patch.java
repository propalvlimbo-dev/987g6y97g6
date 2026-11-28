import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 static final String B="ru/rooyzee/elytrixtrader/", I=B+"model/TraderInstance", H=B+"patch/TraderHologram", S=B+"patch/TraderScope";
 static int filters=0,starts=0,updates=0,removes=0,installs=0,guards=0,old=0;
 static void prepend(MethodNode m,InsnList a){m.instructions.insert(a);}
 static InsnList one(String method){InsnList a=new InsnList();a.add(new VarInsnNode(ALOAD,0));a.add(new MethodInsnNode(INVOKESTATIC,H,method,"(L"+I+";)V",false));return a;}
 static byte[] patch(byte[] bytes){ClassNode c=new ClassNode();new ClassReader(bytes).accept(c,0);
  for(MethodNode m:c.methods){
   if(c.name.equals(B+"config/ConfigLoader") && m.name.equals("loadTraders")){
    for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)n;if(call.owner.equals("java/io/File") && call.name.equals("listFiles") && call.desc.endsWith(")[Ljava/io/File;")){m.instructions.insert(n,new MethodInsnNode(INVOKESTATIC,S,"files","([Ljava/io/File;)[Ljava/io/File;",false));filters++;}}
   }
   if(c.name.equals(B+"virtual/TraderNpc") && m.name.equals("buildHolograms")){m.instructions.clear();m.tryCatchBlocks.clear();m.localVariables=null;m.instructions.add(new InsnNode(RETURN));old++;}
   if(c.name.equals(B+"Main") && m.name.equals("onEnable")){InsnList a=new InsnList();a.add(new VarInsnNode(ALOAD,0));a.add(new MethodInsnNode(INVOKESTATIC,H,"install","(L"+B+"Main;)V",false));prepend(m,a);installs++;}
   if(c.name.equals(I)){
    if(m.name.equals("spawn") && m.desc.equals("()V"))for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof FieldInsnNode){FieldInsnNode f=(FieldInsnNode)n;if(f.owner.equals(I)&&f.name.equals("alive")&&f.getOpcode()==PUTFIELD){InsnList a=new InsnList();a.add(new VarInsnNode(ALOAD,0));a.add(new FieldInsnNode(GETFIELD,I,"plugin","L"+B+"Main;"));a.add(new VarInsnNode(ALOAD,0));a.add(new MethodInsnNode(INVOKESTATIC,H,"start","(L"+B+"Main;L"+I+";)V",false));m.instructions.insert(n,a);starts++;}}
    if(m.name.equals("tick"))for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode && ((MethodInsnNode)n).name.equals("updateHologramText")){m.instructions.insert(n,one("update"));updates++;}
    if((m.name.equals("despawn") && m.desc.equals("(Z)V"))||m.name.equals("finalizeDespawn")||m.name.equals("hideHolograms")||m.name.equals("despawnAnimated")){prepend(m,one("remove"));removes++;}
   }
   if(c.name.equals(B+"trader/TraderManager") && m.name.equals("spawnInternal")){InsnList a=new InsnList();LabelNode ok=new LabelNode();a.add(new VarInsnNode(ALOAD,1));a.add(new MethodInsnNode(INVOKESTATIC,S,"allowed","(L"+B+"model/TraderConfig;)Z",false));a.add(new JumpInsnNode(IFNE,ok));a.add(new InsnNode(ACONST_NULL));a.add(new InsnNode(ARETURN));a.add(ok);a.add(new FrameNode(F_SAME,0,null,0,null));prepend(m,a);guards++;}
  }
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();
 }
 public static void main(String[] args)throws Exception{Path root=Paths.get(args[0]);String[] names={"config/ConfigLoader","editor/TradeEditorManager","virtual/TraderNpc","Main","model/TraderInstance","trader/TraderManager"};for(String n:names){Path p=root.resolve(B+n+".class");Files.write(p,patch(Files.readAllBytes(p)));}if(filters!=1||starts!=1||updates!=1||removes!=4||installs!=1||guards!=1||old!=1)throw new IllegalStateException(filters+" "+starts+" "+updates+" "+removes+" "+installs+" "+guards+" "+old);System.out.println("Hooks verified");}
}
