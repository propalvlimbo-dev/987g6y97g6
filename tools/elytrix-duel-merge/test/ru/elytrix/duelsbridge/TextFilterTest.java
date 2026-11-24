package ru.elytrix.duelsbridge;
import net.kyori.adventure.text.format.TextDecoration;
public class TextFilterTest {public static void main(String[] a){
 String t=TextFilter.clean("&#F8BEFB&l┃ §Lпример <bold>текст</bold> <b>ещё</b>");
 if(!t.equals("&#F8BEFB┃ пример текст ещё"))throw new AssertionError(t);
 TextDecoration[] d=TextFilter.decorations(new TextDecoration[]{TextDecoration.BOLD,TextDecoration.ITALIC});
 if(d.length!=1||d[0]!=TextDecoration.ITALIC)throw new AssertionError();
 if(!"АРЕНА".equals(TextFilter.displayArena("arena1")))throw new AssertionError("arena label");
 if(!"arena2".equals(TextFilter.displayArena("arena2")))throw new AssertionError("other arena");
 if(!"arena1".equals(TextFilter.displayToken("player","arena1")))throw new AssertionError("player name changed");
 if(!"АРЕНА".equals(TextFilter.displayToken("arena","arena1")))throw new AssertionError("arena token");
 java.util.Map<String,String> input=new java.util.HashMap<>();input.put("arena","arena1");input.put("player","arena1");
 java.util.Map<String,String> output=TextFilter.displayValues(input);
 if(!"АРЕНА".equals(output.get("arena")) || !"arena1".equals(output.get("player")) || !"arena1".equals(input.get("arena")))throw new AssertionError("presentation values");
 System.out.println("Text filter tests passed");}}
