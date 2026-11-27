package ru.rooyzee.elytrixairdrop.patch;
import java.util.*;
public class HologramTextTest {
 public static void main(String[] args){
  String cool=String.join(" ",HologramText.main("fire","Остывание: 1м","COOLING"));
  String open=String.join(" ",HologramText.main("fire","Остыл","OPEN"));
  String looted=String.join(" ",HologramText.main("fire","Лут забран","OPEN"));
  if(!cool.contains("После остывания")||!cool.contains("обсидиан")||!open.contains("Добудьте обсидиан")||!open.contains("сундук внутри")||!open.contains("киркой")||looted.contains("Добудьте"))throw new AssertionError("fire instructions");
  if(MeteorGeometry.hologramAboveCenter()<=MeteorGeometry.RADIUS+1)throw new AssertionError("fire hologram intersects meteor");
  if(!String.join(" ",HologramText.main("peaceful","Ожидание","")).contains("волнами"))throw new AssertionError("peaceful waves");
  String sky=String.join(" ",HologramText.main("sky","Открыт",""));if(!sky.contains("подброс")||sky.contains("разной редкости"))throw new AssertionError("sky mechanics");
  if(!HologramText.main("unknown","","").isEmpty())throw new AssertionError("unknown type");
  System.out.println("Hologram text stages and above-meteor placement OK");
 }
}
