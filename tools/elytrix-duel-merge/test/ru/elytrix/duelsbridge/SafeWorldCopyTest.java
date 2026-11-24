package ru.elytrix.duelsbridge;
import java.nio.file.*;import java.util.concurrent.atomic.AtomicBoolean;
public class SafeWorldCopyTest {
 static void ck(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args) throws Exception {
  Path root=Files.createTempDirectory("arena-copy"), from=Files.createDirectory(root.resolve("duel"));
  Files.writeString(from.resolve("level.dat"),"WORLD"); Files.writeString(from.resolve("level.dat_old"),"OLD");
  Files.writeString(from.resolve("level123456789.dat"),"TEMP"); Files.writeString(from.resolve("session.lock"),"LOCK");
  Path region=Files.createDirectory(from.resolve("region"));Files.writeString(region.resolve("r.0.0.mca"),"REGION");
  Files.writeString(region.resolve("level123.dat"),"NESTED");
  AtomicBoolean running=new AtomicBoolean(true);
  Thread churn=new Thread(()->{while(running.get()){try{Path f=from.resolve("level987654321.dat");Files.writeString(f,"tmp");Files.deleteIfExists(f);}catch(Exception e){throw new RuntimeException(e);}}});
  churn.start();try{for(int i=0;i<30;i++)SafeWorldCopy.copy(from,root.resolve("copy"+i));}finally{running.set(false);churn.join();}
  Path out=root.resolve("copy0");ck(Files.readString(out.resolve("level.dat")).equals("WORLD"),"level.dat preserved");
  ck(Files.readString(out.resolve("level.dat_old")).equals("OLD"),"backup level preserved");
  ck(Files.readString(out.resolve("region/r.0.0.mca")).equals("REGION"),"region preserved");
  ck(Files.exists(out.resolve("region/level123.dat")),"nested similarly named file preserved");
  ck(!Files.exists(out.resolve("session.lock")) && !Files.exists(out.resolve("level123456789.dat")),"temp files ignored");
  boolean failed=false;try{SafeWorldCopy.copy(root.resolve("missing"),root.resolve("out"));}catch(NoSuchFileException e){failed=true;}
  ck(failed,"missing world fails rather than silently succeeding");
  System.out.println("Safe world copy tests passed");
 }
}
