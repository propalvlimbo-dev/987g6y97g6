package ru.rooyzee.elytrixairdrop.patch;
import java.util.*;
/** Modern, concise event text. No resource-pack background or decorative frame. */
public final class HologramText {
 private HologramText(){}
 public static List<String> main(String type,String stage,String firePhase){
  List<String> lines=new ArrayList<>();
  if("fire".equals(type)){
   lines.add("&#F8BEFB&lОГНЕННЫЙ АИРДРОП");
   lines.add("OPEN".equals(firePhase) && !stage.contains("Лут забран")?"&fМетеорит &#F8BEFBостыл!":stage);
   if("WAITING".equals(firePhase))lines.add("&fПодойдите, чтобы &#F8BEFBначать");
   else if("FALLING".equals(firePhase))lines.add("&fОсторожно, &#F8BEFBметеорит падает!");
   else if("COOLING".equals(firePhase)){
    lines.add("&fПосле остывания &#F8BEFBдобывайте обсидиан");
    lines.add("&fСундук внутри · кирка: &#F8BEFBалмазная/незеритовая");
   }else if("OPEN".equals(firePhase) && !stage.contains("Лут забран")){
    lines.add("&fДобудьте обсидиан — &#F8BEFBсундук внутри!");
    lines.add("&fАлмазной или &#F8BEFBнезеритовой киркой");
   }
  }else if("peaceful".equals(type)){
   lines.add("&#F8BEFB&lМИРНЫЙ АИРДРОП");lines.add(stage);
   lines.add("&fЛут появляется &#F8BEFBволнами по 1–2 предмета");
  }else if("sky".equals(type)){
   lines.add("&#F8BEFB&lВОЗДУШНЫЙ АИРДРОП");lines.add(stage);
   lines.add("&fАктивируйте &#F8BEFBшалкеры и забирайте лут");
   lines.add("&fОсторожно: возможен &#F8BEFBподброс вверх");
  }else return Collections.emptyList();
  return lines;
 }
}
