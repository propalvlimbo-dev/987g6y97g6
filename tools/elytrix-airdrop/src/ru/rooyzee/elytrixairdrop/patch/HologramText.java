package ru.rooyzee.elytrixairdrop.patch;
import java.util.*;
/** Modern, concise event text. No resource-pack background or decorative frame. */
public final class HologramText {
 private HologramText(){}
 public static List<String> main(String type,String stage,String firePhase){
  List<String> lines=new ArrayList<>();
  lines.add("&#F8BEFBᴇʟʏᴛʀɪx &8» &fАИРДРОП");
  if("fire".equals(type)){
   lines.add("&cОГНЕННЫЙ МЕТЕОРИТ");lines.add(stage);
   if("WAITING".equals(firePhase))lines.add("&7Подойдите, чтобы метеорит упал");
   else if("FALLING".equals(firePhase))lines.add("&7Осторожно: метеорит падает!");
   else if("COOLING".equals(firePhase)){
    lines.add("&fПосле остывания добывайте обсидиан");
    lines.add("&7Сундук внутри · нужна алмазная кирка");
   }else if("OPEN".equals(firePhase) && !stage.contains("Лут забран")){
    lines.add("&eДобудьте обсидиан — сундук внутри!");
    lines.add("&7Алмазной или незеритовой киркой");
   }
  }else if("peaceful".equals(type)){
   lines.add("&#70B9FFМИРНЫЙ АИРДРОП");lines.add(stage);
   lines.add("&7Лут появляется волнами по 1–2 предмета");
  }else if("sky".equals(type)){
   lines.add("&#70D9FFВОЗДУШНЫЙ АИРДРОП");lines.add(stage);
   lines.add("&7Активируйте шалкеры и забирайте лут");
   lines.add("&7Осторожно: возможен подброс вверх");
  }else return Collections.emptyList();
  return lines;
 }
}
