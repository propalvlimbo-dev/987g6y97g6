package ru.elytrix.duelsbridge;
import net.kyori.adventure.text.format.TextDecoration;
public final class TextFilter {
 private TextFilter(){}
 public static String clean(String s) {
  if(s==null)return null;
  return s.replaceAll("(?i)[&§]l", "").replaceAll("(?i)</?(?:bold|b)(?::(?:true|false))?>", "");
 }
 public static TextDecoration[] decorations(TextDecoration[] original){
  if(original==null)return null;
  int size=0;for(TextDecoration d:original)if(d!=TextDecoration.BOLD)size++;
  if(size==original.length)return original;
  TextDecoration[] result=new TextDecoration[size];int i=0;for(TextDecoration d:original)if(d!=TextDecoration.BOLD)result[i++]=d;return result;
 }
}
