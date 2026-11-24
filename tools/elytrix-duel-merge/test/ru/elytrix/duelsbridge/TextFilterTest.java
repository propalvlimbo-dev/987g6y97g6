package ru.elytrix.duelsbridge;
import net.kyori.adventure.text.format.TextDecoration;
public class TextFilterTest {public static void main(String[] a){
 String t=TextFilter.clean("&#F8BEFB&l┃ §Lпример <bold>текст</bold> <b>ещё</b>");
 if(!t.equals("&#F8BEFB┃ пример текст ещё"))throw new AssertionError(t);
 TextDecoration[] d=TextFilter.decorations(new TextDecoration[]{TextDecoration.BOLD,TextDecoration.ITALIC});
 if(d.length!=1||d[0]!=TextDecoration.ITALIC)throw new AssertionError();
 System.out.println("Text filter tests passed");}}
