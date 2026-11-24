import java.nio.file.*;import org.objectweb.asm.*;
public class ArenaBoundaryTransform {
 static final String RULES="ru/elytrix/duelsbridge/ArenaRules",PLUGIN="ru/elytrix/duelsbridge/ElytrixDuelsBridge",HELPER="ru/elytrix/duelsbridge/ArenaBoundary",BOUNDS="Lru/elytrix/duelsbridge/ArenaRules$Bounds;";
 public static void main(String[] args)throws Exception{
  Path path=Path.of(args[0],RULES+".class");ClassReader r=new ClassReader(Files.readAllBytes(path));ClassWriter w=new ClassWriter(r,0);int[] found={0,0,0};
  r.accept(new ClassVisitor(Opcodes.ASM9,w){
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex){
    int kind=name.equals("onMove")&&desc.equals("(Lorg/bukkit/event/player/PlayerMoveEvent;)V")?0:name.equals("onTeleport")&&desc.equals("(Lorg/bukkit/event/player/PlayerTeleportEvent;)V")?1:name.equals("pullBack")&&desc.equals("(Lorg/bukkit/entity/Player;"+BOUNDS+")V")?2:-1;
    if(kind<0)return super.visitMethod(access,name,desc,sig,ex);
    found[kind]++; MethodVisitor out=super.visitMethod(access,name,desc,sig,ex);
    return new MethodVisitor(Opcodes.ASM9,out){
     @Override public void visitCode(){} @Override public void visitInsn(int op){} @Override public void visitIntInsn(int op,int v){} @Override public void visitTableSwitchInsn(int min,int max,Label def,Label... labels){} @Override public void visitLookupSwitchInsn(Label def,int[] keys,Label[] labels){} @Override public void visitMultiANewArrayInsn(String d,int dims){} @Override public void visitVarInsn(int op,int v){} @Override public void visitTypeInsn(int op,String t){} @Override public void visitFieldInsn(int op,String o,String n,String d){} @Override public void visitMethodInsn(int op,String o,String n,String d,boolean i){} @Override public void visitInvokeDynamicInsn(String n,String d,Handle h,Object... a){} @Override public void visitJumpInsn(int op,Label l){} @Override public void visitLabel(Label l){} @Override public void visitLdcInsn(Object v){} @Override public void visitIincInsn(int v,int i){} @Override public void visitFrame(int t,int nl,Object[] l,int ns,Object[] s){} @Override public void visitLineNumber(int n,Label l){} @Override public void visitLocalVariable(String n,String d,String s,Label st,Label en,int i){} @Override public void visitTryCatchBlock(Label s,Label e,Label h,String t){} @Override public void visitMaxs(int s,int l){}
     @Override public void visitEnd(){
      out.visitCode();
      if(kind==2){out.visitVarInsn(Opcodes.ALOAD,1);out.visitVarInsn(Opcodes.ALOAD,2);out.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"recover","(Lorg/bukkit/entity/Player;"+BOUNDS+")V",false);out.visitInsn(Opcodes.RETURN);out.visitMaxs(2,3);}
      else {out.visitVarInsn(Opcodes.ALOAD,0);out.visitVarInsn(Opcodes.ALOAD,0);out.visitFieldInsn(Opcodes.GETFIELD,RULES,"plugin","L"+PLUGIN+";");
       if(kind==0){out.visitVarInsn(Opcodes.ALOAD,0);out.visitFieldInsn(Opcodes.GETFIELD,RULES,"cache","Ljava/util/Map;");}
       out.visitVarInsn(Opcodes.ALOAD,1);
       out.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,kind==0?"move":"teleport",kind==0?"(L"+RULES+";L"+PLUGIN+";Ljava/util/Map;Lorg/bukkit/event/player/PlayerMoveEvent;)V":"(L"+RULES+";L"+PLUGIN+";Lorg/bukkit/event/player/PlayerTeleportEvent;)V",false);
       out.visitInsn(Opcodes.RETURN);out.visitMaxs(kind==0?4:3,2);}
      out.visitEnd();
     }
    };
   }
  },0);if(found[0]!=1||found[1]!=1||found[2]!=1)throw new IllegalStateException("Missing arena rules");Files.write(path,w.toByteArray());System.out.println("Patched move, teleport, and boundary recovery");
 }
}
