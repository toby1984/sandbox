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
    
    
}