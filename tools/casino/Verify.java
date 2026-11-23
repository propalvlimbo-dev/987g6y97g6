import java.nio.file.*;
import java.util.zip.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.tree.analysis.*;
public class Verify {
 public static void main(String[] args)throws Exception {
  int n=0;try(ZipFile jar=new ZipFile(args[0])){var entries=jar.entries();while(entries.hasMoreElements()){
   var e=entries.nextElement();if(!e.getName().endsWith(".class"))continue;
   ClassNode c=new ClassNode();new ClassReader(jar.getInputStream(e).readAllBytes()).accept(c,ClassReader.EXPAND_FRAMES);
   for(MethodNode m:c.methods)if((m.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0){
    try{new Analyzer<>(new BasicVerifier()).analyze(c.name,m);}catch(AnalyzerException ex){throw new IllegalStateException(c.name+"."+m.name+m.desc,ex);} n++;
   }
  }}System.out.println("Verified operand types in "+n+" methods");
 }
}
