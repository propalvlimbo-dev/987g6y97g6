package ru.elytrix.duelsbridge;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/** One-time copy, never delete or overwrite the existing legacy directory. */
public final class LegacyDataMigration {
 private LegacyDataMigration() {}
 public static void migrate(ElytrixDuelsBridge plugin) {
  Path target=plugin.getDataFolder().toPath(), parent=target.getParent(), old=parent.resolve("EpicDuels");
  if(!Files.exists(old)) return;
  if(Files.exists(target)) {
   if(!Files.exists(target.resolve("arenas.yml")) && Files.exists(old.resolve("arenas.yml")))
    throw new IllegalStateException("ElytrixDuel folder already exists but contains no arenas.yml; refusing to start until legacy data is migrated or reviewed.");
   return;
  }
  Path tmp=null;
  try {
   tmp=Files.createTempDirectory(parent,".elytrixduel-migration-");
   Path work=tmp;
   Files.walkFileTree(old,new SimpleFileVisitor<Path>() {
    @Override public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes a) throws IOException {
     Files.createDirectories(work.resolve(old.relativize(dir)));return FileVisitResult.CONTINUE;
    }
    @Override public FileVisitResult visitFile(Path file,BasicFileAttributes a) throws IOException {
     if(a.isSymbolicLink())throw new IOException("Refusing to migrate symbolic link: "+file);
     Files.copy(file,work.resolve(old.relativize(file)),StandardCopyOption.COPY_ATTRIBUTES);return FileVisitResult.CONTINUE;
    }
   });
   Files.move(tmp,target); tmp=null;
   plugin.getLogger().info("Copied EpicDuels data to ElytrixDuel; original data preserved.");
  } catch(IOException e) {throw new IllegalStateException("Could not copy old EpicDuels data; refusing to start with empty data",e);}
  finally {
   if(tmp!=null) try {Files.walkFileTree(tmp,new SimpleFileVisitor<Path>() {
    @Override public FileVisitResult visitFile(Path f,BasicFileAttributes a)throws IOException{Files.delete(f);return FileVisitResult.CONTINUE;}
    @Override public FileVisitResult postVisitDirectory(Path d,IOException e)throws IOException{if(e!=null)throw e;Files.delete(d);return FileVisitResult.CONTINUE;}
   });} catch(IOException e){plugin.getLogger().warning("Could not remove incomplete migration folder: "+tmp);}
  }
 }
}
