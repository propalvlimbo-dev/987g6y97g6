package ru.rooyzee.elytrixairdrop.patch;
import java.util.*;
/** The actual pasted schematic columns and the holes they enclose, not its bounding box. */
public final class FireFootprint {
 public static long key(int x,int z){return ((long)x<<32) | (z&0xffffffffL);}
 public static int x(long k){return (int)(k>>32);} public static int z(long k){return (int)k;}
 private final Set<Long> interior=new HashSet<>(),edge=new HashSet<>();
 public FireFootprint(Set<Long> columns){
  if(columns.isEmpty())throw new IllegalArgumentException("empty schematic footprint");
  int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,minZ=Integer.MAX_VALUE,maxZ=Integer.MIN_VALUE;
  for(long k:columns){minX=Math.min(minX,x(k));maxX=Math.max(maxX,x(k));minZ=Math.min(minZ,z(k));maxZ=Math.max(maxZ,z(k));}
  if((long)(maxX-minX+3)*(maxZ-minZ+3)>100000L)throw new IllegalArgumentException("schematic footprint too large");
  minX--;maxX++;minZ--;maxZ++;
  Set<Long> outside=new HashSet<>();ArrayDeque<Long> queue=new ArrayDeque<>();long start=key(minX,minZ);queue.add(start);outside.add(start);
  while(!queue.isEmpty()){
   long k=queue.removeFirst();int x=x(k),z=z(k);
   for(int[] d:DIR){int nx=x+d[0],nz=z+d[1];long n=key(nx,nz);if(nx<minX||nx>maxX||nz<minZ||nz>maxZ||columns.contains(n)||!outside.add(n))continue;queue.add(n);}
  }
  for(int x=minX+1;x<maxX;x++)for(int z=minZ+1;z<maxZ;z++){long k=key(x,z);if(!outside.contains(k))interior.add(k);}
  for(long k:columns)for(int[] d:DIR)if(outside.contains(key(x(k)+d[0],z(k)+d[1]))){edge.add(k);break;}
 }
 private static final int[][] DIR={{1,0},{-1,0},{0,1},{0,-1}};
 public boolean contains(int x,int z){return interior.contains(key(x,z));}
 public Set<Long> edges(){return Collections.unmodifiableSet(edge);}
}
