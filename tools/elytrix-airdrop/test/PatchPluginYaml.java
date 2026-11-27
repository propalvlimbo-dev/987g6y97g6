import java.nio.charset.StandardCharsets;
import java.nio.file.*;
/** Amend the extracted plugin descriptor, preserving original dependencies and commands. */
public class PatchPluginYaml {
 public static void main(String[] args)throws Exception{
  Path path=Paths.get(args[0]);String s=Files.readString(path,StandardCharsets.UTF_8).replace("\r\n","\n");
  String old="softdepend: [PlaceholderAPI, FastAsyncWorldEdit]";
  if(!s.contains(old)||!s.contains("\npermissions:")||!s.contains("api-version: 1.16"))throw new IllegalStateException("Unexpected plugin descriptor");
  s=s.replace("api-version: 1.16","api-version: '1.21'").replace(old,"softdepend: [PlaceholderAPI, FastAsyncWorldEdit, ElytrixItem]").replace("\npermissions:","\n  airdroplootpreset:\n    description: Install the shared curated loot preset\n    usage: /airdroplootpreset confirm\n    permission: elytrixairdrop.admin\npermissions:");
  Files.writeString(path,s,StandardCharsets.UTF_8);
 }
}
