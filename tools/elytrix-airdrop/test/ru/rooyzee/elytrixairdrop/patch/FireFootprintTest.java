package ru.rooyzee.elytrixairdrop.patch;
import java.util.*;
public class FireFootprintTest {
 static void ok(boolean x,String m){if(!x)throw new AssertionError(m);}
 public static void main(String[] args){
  Set<Long> square=new HashSet<>();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x==-2||x==2||z==-2||z==2)square.add(FireFootprint.key(x,z));
  FireFootprint a=new FireFootprint(square);ok(a.contains(0,0),"enclosed hole");ok(!a.contains(3,0),"outside");ok(a.edges().size()==16,"outer perimeter");
  Set<Long> l=new HashSet<>();for(int i=0;i<4;i++){l.add(FireFootprint.key(i,0));l.add(FireFootprint.key(0,i));}
  FireFootprint b=new FireFootprint(l);ok(b.contains(0,3),"footprint arm");ok(!b.contains(2,2),"concavity not bounding box");ok(b.edges().size()==7,"L edge");
  try{new FireFootprint(Set.of(FireFootprint.key(0,0),FireFootprint.key(1000,1000)));throw new AssertionError("size guard");}catch(IllegalArgumentException expected){}
  System.out.println("Fire schematic boundary: concave edges, holes and negative coordinates OK");
 }
}
