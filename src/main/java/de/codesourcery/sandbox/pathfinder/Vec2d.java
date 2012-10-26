package de.codesourcery.sandbox.pathfinder;

public class Vec2d
{
    private static final float RAD_TO_DEG = (float) ( 180.0d / Math.PI );

    public static final Vec2 ORIGIN = new Vec2(0,0);

    public final double x;
    public final double y;

    public Vec2d(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public Vec2d add(Vec2 o) {
        return new Vec2d(this.x+o.x,this.y+o.y);
    }
    
    public Vec2d normalize() 
    {
        double l = length();
        return new Vec2d( x/l , y/l );
    }

    public Vec2d multiply(double factor) {
        return new Vec2d( factor*x , factor*y );
    }

    public Vec2d minus(double x1,double y1) {
        return new Vec2d(this.x-x1,this.y-y1);
    }      

    public Vec2d minus(Vec2 o) {
        return new Vec2d(this.x-o.x,this.y-o.y);
    }    

    public double length() {
        return Math.sqrt( x*x + y*y );
    }

    public double dotProduct(Vec2d b) 
    {
        return this.x*b.x + this.y*b.y;
    }    

    public float angleInRad(Vec2d other) {
        double f = dotProduct(other) / ( this.length() * other.length() );
        System.out.println("f="+f);
        return (float) Math.acos(f);
    }

    public float angleInDeg(Vec2d other) {
        return angleInRad(other)*RAD_TO_DEG;
    }    

    public double cosine() {
        return x / Math.sqrt( x*x + y*y ); 
    }
    
    public double cosineInDeg() {
        return Math.acos( x / Math.sqrt( x*x + y*y ) )*RAD_TO_DEG; 
    }    

    public static void main(String[] args)
    {
        final Vec2  v1 = new Vec2(1,1);
        final Vec2 v2 = new Vec2(1,1);
        System.out.println( "dotProduct = "+v1.dotProduct( v2 ) );    
        System.out.println( "l1         = "+v1.length());
        System.out.println( "l2         = "+v2.length());
        System.out.println( "angle = "+v1.angleInDeg( v2 ) );
    }

    @Override
    public int hashCode()
    {
        return 31 * (31 + Double.valueOf( x ).hashCode() ) + Double.valueOf( y ).hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (! (obj instanceof Vec2d) ) {
            return false;
        }
        final Vec2d other = (Vec2d) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public String toString()
    {
        return "("+x+","+y+")";
    }
}
