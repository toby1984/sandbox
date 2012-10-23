package de.codesourcery.sandbox.pathfinder;

public class Rec2
{
    public final int x1;
    public final int y1;
    public final int x2;
    public final int y2;

    protected Rec2(int x1, int y1, int x2, int y2)
    {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    public final boolean matches(Rec2 other) {
        return this.x1 == other.x1 && this.x2 == other.x2 && this.y1 == other.y1 && this.y2 == other.y2;
    }
    
    public final boolean contains(int x,int y) {
        return x >= x1 && x < x2 &&
               y >= y1 && y < y2;
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
    
    public final boolean intersects(Rec2 rec) 
    {
        return intersects( rec.x1 , rec.x2 , rec.width() , rec.height() );
    }

    public final boolean intersects(int x,int y,int width,int height) 
    {
        // fully contained
        final int px1 = x;
        final int px2 = x+width;
        final int py1 = y;
        final int py2 = y+height;
        
        if ( x1 < px1 && x2 > px2 && y1 < py1 && y2 > py2 ) { // this rect fully encloses the other
            return true;
        }
        
        if ( px1 < x1 && px2 > x2 && py1 < y1 && py2 > y2 ) { // other rect fully encloses this one
            return true;
        }           
        
        return ( x1 >= px1 && x1 < px2 && y1 >= py1 && y1 < py2 ) ||
               ( x2 >= px1 && x2 < px2 && y2 >= py1 && y2 < py2 ) ||
               ( px1 >= x1 && px1 < x2 && py1 >= y1 && py1 < y2 ) ||
               ( px2 >= x1 && px2 < x2 && py2 >= y1 && py2 < y2 );                 
    }
    
    public final boolean intersects(Vec2 p1,Vec2 p2)
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
}
