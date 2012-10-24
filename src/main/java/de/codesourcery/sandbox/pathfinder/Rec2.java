package de.codesourcery.sandbox.pathfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Rec2
{
    public final int x1;
    public final int y1;
    public final int x2;
    public final int y2;

    protected Rec2(Vec2 p1,Vec2 p2)
    {
    	x1 = p1.x < p2.x ? p1.x : p2.x;
    	x2 = p1.x > p2.x ? p1.x : p2.x;
    	y1 = p1.y < p2.y ? p1.y : p2.y;
    	y2 = p1.y > p2.y ? p1.y : p2.y;
    }
    
    protected Rec2(int x1, int y1, int x2, int y2)
    {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    public boolean isAdjactant(Rec2 other) 
    {
        if (other.y2 + 1 == y1) { // 1
            
            return this.x1 >= other.x1 && this.x1 < other.x2 ||
                    this.x2 >= other.x1 && this.x2 < other.x2 ||
                    other.x1 >= this.x1 && other.x1 < this.x2 ||
                    other.x2 >= this.x1 && other.x2 < this.x2 ||
        }        
        
        if (other.x1 -1 == x2) { // 2
            
        }

        if (other.y1 - 1 == y2) { // 3
            
        }   
        if (other.x2 +1 == x1) { // 4
            
        }        
        return  false;
    }
    
    public Rec2 plus(Rec2 other) 
    {
        final int minX = x1 < other.x1 ? x1 : other.x1;
        final int minY = y1 < other.y1 ? y1 : other.y1;
        
        final int maxX = x2 > other.x2 ? x2 : other.x2;
        final int maxY = y2 > other.y2 ? y2 : other.y2;            
        return new Rec2(minX,minY,maxX,maxY);
    }
    
    public List<Rec2> minus(Rec2 other) 
    {
        if ( ! overlap( other ) ) {
            return Collections.emptyList();
        }
        
        if ( ! completelyContains( other ) ) {
            throw new IllegalArgumentException("Not completely contained: "+other+" IN "+this);
        }
        
        final Rec2 r1 = createRectIfNotEmpty( x1 , y1 , x2 , other.y1 );
        final Rec2 r2 = createRectIfNotEmpty( x1 , other.y2 , x2 , y2 );
        final Rec2 r3 = createRectIfNotEmpty( x1 , other.y1 , other.x1 , other.y2 );
        final Rec2 r4 = createRectIfNotEmpty( other.x2 , other.y1 , x2 , other.y2 );
        
        final List<Rec2> result = new ArrayList<>();
        if (r1 != null ) { result.add( r1 ); }
        if (r2 != null ) { result.add( r2 ); }
        if (r3 != null ) { result.add( r3 ); }
        if (r4 != null ) { result.add( r4 ); }
        return result;
    }
    
    private static Rec2 createRectIfNotEmpty(int x1,int y1,int x2,int y2) {
        if ( x1 == x2 || y1 == y2 ) {
            return null;
        }
        
        final int minX = x1 < x2 ? x1 : x2;
        final int minY = y1 < y2 ? y1 : y2;
        
        final int maxX = x1 > x2 ? x1 : x2;
        final int maxY = y1 > y2 ? y1 : y2;        
        
        return new Rec2(minX,minY,maxX,maxY);
    }
    
    public boolean isEmpty() {
        return x1 == x2 || y1 == y2;
    }
    
    public final boolean matches(Rec2 other) {
        return this.x1 == other.x1 && this.x2 == other.x2 && this.y1 == other.y1 && this.y2 == other.y2;
    }
    
    public final boolean contains(int x,int y) {
        return x >= x1 && x < x2 &&
               y >= y1 && y < y2;
    }    

    public boolean completelyContains(Rec2 other) {
        return x1 <= other.x1 && y1 <= other.y1 && x2 >= other.x2 && y2 >= other.y2;
    }
    
    public final boolean contains(Vec2 v) {
        return v.x >= x1 && v.x < x2 && v.y >= y1 && v.y < y2;
    }
    
    public final int width() {
        return x2-x1;
    }
    
    public final int height() {
        return y2-y1;
    }  
    
    public int getArea() {
        return width()*height();
    }
    
    public final boolean overlap(int rx1,int ry1,int width , int height) 
    {
        final int rx2 = rx1 + width;
        final int ry2 = ry1 + height;
        
        return ! (x2 <= rx1 || y2 <= ry1 || x1 >= rx2 || y1 >= ry2 );
    }    
    
    public final boolean overlap(Rec2 r2) 
    {
        return ! (x2 <= r2.x1 || y2 <= r2.y1 || x1 >= r2.x2 || y1 >= r2.y2 );
    }
    
    public final boolean intersectsLine(Vec2 p1,Vec2 p2)
    {
        // Find min and max X for the segment
        int minX = p1.x;
        int maxX = p2.x;

        if(p1.x > p2.x)
        {
            minX = p2.x;
            maxX = p1.x;
        }

        // Find the intersection of the segment's and rectangle's x-projections
        if(maxX > x2)
        {
            maxX = x2;
        }

        if(minX < x1)
        {
            minX = x1;
        }

        if(minX > maxX) // If their projections do not intersect return false
        {
            return false;
        }

        // Find corresponding min and max Y for min and max X we found before
        int minY = p1.y;
        int maxY = p2.y;

        int dx = p2.x - p1.x;

        if( dx > 0 )
        {
            int a = (p2.y - p1.y) / dx;
            int b = p1.y - a * p1.x;
            minY = a * minX + b;
            maxY = a * maxX + b;
        }

        if(minY > maxY)
        {
            int tmp = maxY;
            maxY = minY;
            minY = tmp;
        }

        // Find the intersection of the segment's and rectangle's y-projections
        if(maxY > y2)
        {
            maxY = y2;
        }

        if(minY < y1)
        {
            minY = y1;
        }

        if(minY > maxY) // If Y-projections do not intersect return false
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
        result = prime * result + x1;
        result = prime * result + x2;
        result = prime * result + y1;
        result = prime * result + y2;
        return result;
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
}