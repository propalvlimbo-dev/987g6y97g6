package ru.elytrix.duelsbridge;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

/** Copies an arena template without allowing transient level*.dat saves to abort it. */
public final class SafeWorldCopy {
 private static final Pattern TEMP_LEVEL = Pattern.compile("level[0-9]+\\.dat");
 private SafeWorldCopy() {}
 static boolean transientFile(Path root, Path file) {
   return file.getParent() != null && file.getParent().equals(root)
       && TEMP_LEVEL.matcher(file.getFileName().toString()).matches();
 }
 public static void copy(Path source, Path target) throws IOException {
  Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
   @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes a) throws IOException {
    Files.createDirectories(target.resolve(source.relativize(dir))); return FileVisitResult.CONTINUE;
   }
   @Override public FileVisitResult visitFile(Path file, BasicFileAttributes a) throws IOException {
    if (transientFile(source,file) || file.getFileName().toString().equals("session.lock")) return FileVisitResult.CONTINUE;
    Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
    return FileVisitResult.CONTINUE;
   }
   @Override public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
    if (exc instanceof NoSuchFileException && transientFile(source,file)) return FileVisitResult.CONTINUE;
    throw exc;
   }
  });
 }
}
