public class Verify {
 public static void main(String[] args)throws Exception {
  for(String s:args){Class<?> c=Class.forName(s,false,Verify.class.getClassLoader()); c.getDeclaredMethods(); c.getDeclaredConstructors(); System.out.println("Verified "+s);}
 }
}
