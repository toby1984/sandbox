package de.codesourcery.sandbox.pathfinder;

public final class Vec2
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
    
    public Vec2 sub(Vec2 o) {
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
        Vec2 v1 = new Vec2(1,0);
        System.out.println("Len #1: "+v1.length());
        Vec2 v2 = new Vec2(-1,1);
        System.out.println("Len #2: "+v2.length());
        System.out.println("Dot product: "+v1.dotProduct( v2 ) );
        System.out.println("Angle (rad): "+v1.angleInRad( v2 ) );
        System.out.println("Angle (deg): "+v1.angleInDeg( v2 ) );        
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
