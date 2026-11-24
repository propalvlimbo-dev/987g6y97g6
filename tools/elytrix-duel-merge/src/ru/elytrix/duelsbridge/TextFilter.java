package ru.elytrix.duelsbridge;
import net.kyori.adventure.text.format.TextDecoration;
public final class TextFilter {
 private TextFilter(){}
 public static String clean(String s) {
  if(s==null)return null;
  return s.replaceAll("(?i)[&§]l", "").replaceAll("(?i)</?(?:bold|b)(?::(?:true|false))?>", "");
 }
 public static String displayArena(String name){return name!=null && name.equalsIgnoreCase("arena1") ? "АРЕНА" : name;}
 public static String displayArenaReport(String text){return text==null ? null : text.replaceAll("(?i)(?<![a-z0-9_])arena1(?![a-z0-9_])", "АРЕНА");}
 public static Object displayToken(String key,Object value){return "arena".equalsIgnoreCase(key) && value instanceof String ? displayArena((String)value) : value;}
 public static java.util.Map<String,String> displayValues(java.util.Map<String,String> values){
  if(values==null || !values.containsKey("arena"))return values;
  java.util.Map<String,String> copy=new java.util.HashMap<>(values);copy.put("arena",displayArena(copy.get("arena")));return copy;
 }
 public static TextDecoration[] decorations(TextDecoration[] original){
  if(original==null)return null;
  int size=0;for(TextDecoration d:original)if(d!=TextDecoration.BOLD)size++;
  if(size==original.length)return original;
  TextDecoration[] result=new TextDecoration[size];int i=0;for(TextDecoration d:original)if(d!=TextDecoration.BOLD)result[i++]=d;return result;
 }
}
