package ru.elytrix.duelsbridge;
import java.util.List;import org.bukkit.*;
public class RotatingBoundsTest {
 public static void main(String[] args){
  World world=()->"arena_instance_test";
  Location initial=new Location(world,100,101,16), initial2=new Location(world,100,101,-18);
  Location a=new Location(world,0,101,-18), b=new Location(world,0,101,16);
  ArenaRules.Bounds bounds=new ArenaRules.Bounds(world.getName(),initial,initial2,List.of(a,b),20,7,25);
  if(!bounds.inside(a)||!bounds.inside(b))throw new AssertionError("active pair must fit");
  if(bounds.inside(initial))throw new AssertionError("other arena must not expand active pair boundary");
  System.out.println("Actual transformed rotation bounds test passed");
 }
}
