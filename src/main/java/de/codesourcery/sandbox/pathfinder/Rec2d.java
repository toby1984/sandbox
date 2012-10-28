package de.codesourcery.sandbox.pathfinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.codesourcery.sandbox.pathfinder.Edge.OverlapType;

public class Rec2d 
{
    public double x1;
    public double y1;
    public double x2;
    public double y2;

    protected Rec2d(Vec2 p1,Vec2 p2)
    {
    	x1 = p1.x < p2.x ? p1.x : p2.x;
    	x2 = p1.x > p2.x ? p1.x : p2.x;
    	y1 = p1.y < p2.y ? p1.y : p2.y;
    	y2 = p1.y > p2.y ? p1.y : p2.y;
    }
    
    public Rec2d(double x1, double y1, double x2, double y2)
    {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    public Rec2d union(Rec2d other) 
    {
        final double minX = x1 < other.x1 ? x1 : other.x1;
        final double minY = y1 < other.y1 ? y1 : other.y1;
        
        final double maxX = x2 > other.x2 ? x2 : other.x2;
        final double maxY = y2 > other.y2 ? y2 : other.y2;            
        return new Rec2d(minX,minY,maxX,maxY);
    }
    
    /*
     *     |-----| case a
     * |------|
     * 
     * |-----|
     *    |------| case b
     *
     *    |-----|
     * |----------| case c
     * 
     * |----------| case d
     *    |-----|     
     */
    public List<Rec2> isAdjacent(Rec2 other) {
        
        /* +------+
         * |   1  |
         * +--+---+---+  case 1
         *    |   2   |
         *    +-------+
         * 
         *      +------+
         *      |   2  |
         * +----+--+---+ case 2
         * |   1   |
         * +-------+
         */        
        
        Edge edge;
        if ( this.y2 == other.y1 ) { // case #1
            
        } else if ( this.y1 == other.y2 ) { // case #2
            
        }
        
        /* +--+
         * |  |
         * | 1+--+
         * |  |  | case 3
         * +--+ 2|
         *    |  |
         *    +--+
         *
         *    +--+
         *    |  |
         *    | 2| case 4
         * +--+  |
         * |  |  |
         * | 1+--+
         * |  |    
         * +--+ 
         */         
        
        if ( this.x2 == other.x1 ) { // case #3
            
        } else if ( other.x1 == this.x2 ) { // case #4
            
        }
        
        return null;
    }
    
    public List<Rec2d> minus(Rec2d other) 
    {
        if ( ! overlap( other ) ) {
            return Collections.emptyList();
        }
        
        if ( ! completelyContains( other ) ) {
            throw new IllegalArgumentException("Not completely contained: "+other+" IN "+this);
        }
        
        final Rec2d r1 = createRectIfNotEmpty( x1 , y1 , x2 , other.y1 );
        final Rec2d r2 = createRectIfNotEmpty( x1 , other.y2 , x2 , y2 );
        final Rec2d r3 = createRectIfNotEmpty( x1 , other.y1 , other.x1 , other.y2 );
        final Rec2d r4 = createRectIfNotEmpty( other.x2 , other.y1 , x2 , other.y2 );
        
        final List<Rec2d> result = new ArrayList<>();
        if (r1 != null ) { result.add( r1 ); }
        if (r2 != null ) { result.add( r2 ); }
        if (r3 != null ) { result.add( r3 ); }
        if (r4 != null ) { result.add( r4 ); }
        return result;
    }
    
    private static Rec2d createRectIfNotEmpty(double x1,double y1,double x2,double y2) {
        if ( x1 == x2 || y1 == y2 ) {
            return null;
        }
        
        final double minX = x1 < x2 ? x1 : x2;
        final double minY = y1 < y2 ? y1 : y2;
        
        final double maxX = x1 > x2 ? x1 : x2;
        final double maxY = y1 > y2 ? y1 : y2;        
        
        return new Rec2d(minX,minY,maxX,maxY);
    }
    
    public boolean isEmpty() {
        return x1 == x2 || y1 == y2;
    }
    
    public final boolean matches(Rec2 other) {
        return this.x1 == other.x1 && this.x2 == other.x2 && this.y1 == other.y1 && this.y2 == other.y2;
    }
    
    public final boolean contains(double x,double y) {
        return x >= x1 && x < x2 &&
               y >= y1 && y < y2;
    }    

    public boolean completelyContains(Rec2d other) {
        return x1 <= other.x1 && y1 <= other.y1 && x2 >= other.x2 && y2 >= other.y2;
    }
    
    public final boolean contains(Vec2 v) {
        return v.x >= x1 && v.x < x2 && v.y >= y1 && v.y < y2;
    }
    
    public final double width() {
        return x2-x1;
    }
    
    public final double height() {
        return y2-y1;
    }  
    
    public double getArea() {
        return width()*height();
    }
    
    public final boolean overlap(double rx1,double ry1,double width , double height) 
    {
        final double rx2 = rx1 + width;
        final double ry2 = ry1 + height;
        
        return ! (x2 <= rx1 || y2 <= ry1 || x1 >= rx2 || y1 >= ry2 );
    }    
    
    public final boolean overlap(Rec2d r2) 
    {
        return ! (x2 <= r2.x1 || y2 <= r2.y1 || x1 >= r2.x2 || y1 >= r2.y2 );
    }
    
    public final boolean doubleersectsLine(Vec2 p1,Vec2 p2)
    {
        // Find min and max X for the segment
        double minX = p1.x;
        double maxX = p2.x;

        if(p1.x > p2.x)
        {
            minX = p2.x;
            maxX = p1.x;
        }

        // Find the doubleersection of the segment's and rectangle's x-projections
        if(maxX > x2)
        {
            maxX = x2;
        }

        if(minX < x1)
        {
            minX = x1;
        }

        if(minX > maxX) // If their projections do not doubleersect return false
        {
            return false;
        }

        // Find corresponding min and max Y for min and max X we found before
        double minY = p1.y;
        double maxY = p2.y;

        double dx = p2.x - p1.x;

        if( dx > 0 )
        {
            double a = (p2.y - p1.y) / dx;
            double b = p1.y - a * p1.x;
            minY = a * minX + b;
            maxY = a * maxX + b;
        }

        if(minY > maxY)
        {
            double tmp = maxY;
            maxY = minY;
            minY = tmp;
        }

        // Find the doubleersection of the segment's and rectangle's y-projections
        if(maxY > y2)
        {
            maxY = y2;
        }

        if(minY < y1)
        {
            minY = y1;
        }

        if(minY > maxY) // If Y-projections do not doubleersect return false
        {
            return false;
        }
        return true;
    }

    @Override
    public final int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + hashCode(x1);
        result = prime * result + hashCode(x2);
        result = prime * result + hashCode(y1);
        result = prime * result + hashCode(y2);
        return result;
    }
    
    private final int hashCode(double v) 
    {
    	long d = Double.doubleToLongBits( v );
    	return  (int)(d^(d>>>32));
    }

    @Override
    public final boolean equals(Object obj)
    {
        if (!(obj instanceof Rec2) ) 
        {
            return false;
        }
        
        final Rec2 other = (Rec2) obj;
        if (x1 != other.x1) {
            return false;
        }
        if (x2 != other.x2) {
            return false;
        }
        if (y1 != other.y1) {
            return false;
        }
        if (y2 != other.y2) {
            return false;
        }
        return true;
    }

	@Override
	public String toString() {
		return "Rec2 [x1=" + x1 + ", y1=" + y1 + ", x2=" + x2 + ", y2=" + y2
				+ "]";
	}

    public boolean containsLine(double x1, double y1, double x2, double y2)
    {
        return contains( x1 , y1 ) && contains( x2, y2 );
    }    
}