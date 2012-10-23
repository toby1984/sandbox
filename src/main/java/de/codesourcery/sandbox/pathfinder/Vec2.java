package de.codesourcery.sandbox.pathfinder;

public class Vec2
{
    private static final float RAD_TO_DEG = (float) ( 180.0d / Math.PI );
    
    public static final Vec2 ORIGIN = new Vec2(0,0);
    
    public final int x;
    public final int y;
    
    public Vec2(int x, int y)
    {
        this.x = x;
        this.y = y;
    }
    
    public Vec2 add(Vec2 o) {
        return new Vec2(this.x+o.x,this.y+o.y);
    }
    
    public Vec2 multiply(float factor) {
        return new Vec2( Math.round( factor*x ) , Math.round( factor*y ) );
    }
    
    public Vec2 minus(Vec2 o) {
        return new Vec2(this.x-o.x,this.y-o.y);
    }    
    
    public float length() {
        return (float) Math.sqrt( x*x + y*y );
    }
    
    public int dotProduct(Vec2 b) 
    {
        return this.x*b.x + this.y*b.y;
    }    
    
    public float angleInRad(Vec2 other) {
        float f = dotProduct(other) / ( this.length() * other.length() );
        return (float) Math.acos(f);
    }
    
    public float angleInDeg(Vec2 other) {
        return angleInRad(other)*RAD_TO_DEG;
    }    
    
    public static void main(String[] args)
    {
        
        final Vec2 target=new Vec2(10,10);
        
        final Vec2 current=new Vec2(5,5);
        
        final Vec2 movement=new Vec2(-1,0);        
        
        Vec2 delta = target.minus( current );
        final float angle = movement.angleInDeg( delta );
        System.out.println("Angle (deg): "+angle);        
    }

    @Override
    public int hashCode()
    {
        return 31 * (31 + x) + y;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (! (obj instanceof Vec2) ) {
            return false;
        }
        final Vec2 other = (Vec2) obj;
        return x == other.x && y == other.y;
    }
    
    @Override
    public String toString()
    {
        return "("+x+","+y+")";
    }
}
