package ru.rooyzee.elytrixtrader.patch;
import java.io.File;
import java.util.Arrays;
import ru.rooyzee.elytrixtrader.model.TraderConfig;
/** Ignore other trader files without deleting or editing the user's data. */
public final class TraderScope {
 private TraderScope(){}
 public static File[] files(File[] files){return files==null?null:Arrays.stream(files).filter(f->f!=null && f.getName().equalsIgnoreCase("experienced.yml")).toArray(File[]::new);}
 public static boolean allowed(TraderConfig cfg){return cfg!=null && "experienced".equalsIgnoreCase(cfg.id());}
}
