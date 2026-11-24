import java.util.*;import java.util.zip.*;import org.objectweb.asm.*;import org.bukkit.entity.Player;import org.bukkit.potion.PotionEffectType;
public class PotionTest {
 public static void main(String[]args)throws Exception{
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V21,Opcodes.ACC_PUBLIC,"ru/elytrix/duelsbridge/Effects",null,"java/lang/Object",null);
  w.visitField(Opcodes.ACC_PUBLIC,"applied","Ljava/util/Map;",null,null).visitEnd();
  MethodVisitor init=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);init.visitCode();init.visitVarInsn(Opcodes.ALOAD,0);init.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);init.visitInsn(Opcodes.RETURN);init.visitMaxs(0,0);init.visitEnd();
  try(ZipFile z=new ZipFile(args[0])){new ClassReader(z.getInputStream(z.getEntry("ru/elytrix/duelsbridge/Effects.class"))).accept(new ClassVisitor(Opcodes.ASM9){public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){if(n.equals("clear"))return w.visitMethod(Opcodes.ACC_PUBLIC,n,d,s,e);return null;}},0);}w.visitEnd();byte[] bytes=w.toByteArray();Class<?> c=new ClassLoader(PotionTest.class.getClassLoader()){Class<?> load(){return defineClass(null,bytes,0,bytes.length);}}.load();Object o=c.getConstructor().newInstance();UUID id=UUID.randomUUID();PotionEffectType effect=new PotionEffectType();Map<UUID,Set<PotionEffectType>> map=new HashMap<>();map.put(id,new HashSet<>(Set.of(effect)));c.getField("applied").set(o,map);int[] calls={0};Player player=new Player(){public UUID getUniqueId(){return id;}public void removePotionEffect(PotionEffectType t){if(t==effect)calls[0]++;}};
  c.getMethod("clear",Player.class).invoke(o,player);if(calls[0]!=1||!map.isEmpty())throw new AssertionError();System.out.println("Actual transformed Effects.clear executed with void Bukkit API");
 }
}
