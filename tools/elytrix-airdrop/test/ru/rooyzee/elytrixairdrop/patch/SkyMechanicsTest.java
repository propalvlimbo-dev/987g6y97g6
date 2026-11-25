package ru.rooyzee.elytrixairdrop.patch;
public class SkyMechanicsTest {
 public static void main(String[] args){
  for(int i=0;i<8;i++){
   for(int offset=1;offset<=7;offset++)if(WindMath.nextDirection(i,offset)==i)throw new AssertionError("direction unchanged");
   double x=WindMath.directionX(i),z=WindMath.directionZ(i);
   if(Math.abs(x*x+z*z-1)>1e-9)throw new AssertionError("non-unit direction");
   if(Math.abs(WindMath.impulse(0,0,x,z)-WindMath.IMPULSE)>1e-9)throw new AssertionError("no wind");
   if(WindMath.impulse(-0.3*x,-0.3*z,x,z)!=WindMath.IMPULSE)throw new AssertionError("resistance");
   if(WindMath.impulse(0.3*x,0.3*z,x,z)!=0)throw new AssertionError("external velocity limited");
   double vx=0,vz=0;for(int j=0;j<100;j++){double step=WindMath.impulse(vx,vz,x,z);vx+=x*step;vz+=z*step;}
   if(vx*x+vz*z>WindMath.MAX_ALONG_WIND+1e-9)throw new AssertionError("unbounded wind");
  }
  System.out.println("Wind direction, resistance, impulse and cap OK");
 }
}
