package de.codesourcery.sandbox.pathfinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.codesourcery.sandbox.pathfinder.Edge.OverlapType;

public class Rec2 implements IPolygon
{
    public int x1;
    public int y1;
    public int x2;
    public int y2;

    protected Rec2(Vec2 p1,Vec2 p2)
    {
    	x1 = p1.x < p2.x ? p1.x : p2.x;
    	x2 = p1.x > p2.x ? p1.x : p2.x;
    	y1 = p1.y < p2.y ? p1.y : p2.y;
    	y2 = p1.y > p2.y ? p1.y : p2.y;
    }
    
    public Rec2(Rec2 r) {
        this.x1 = r.x1;
        this.x2 = r.x2;
        this.y1 = r.y1;
        this.y2 = r.y2;
    }
    
    public Rec2(int x1, int y1, int x2, int y2)
    {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    public Rec2 union(Rec2 other) 
    {
        final int minX = x1 < other.x1 ? x1 : other.x1;
        final int minY = y1 < other.y1 ? y1 : other.y1;
        
        final int maxX = x2 > other.x2 ? x2 : other.x2;
        final int maxY = y2 > other.y2 ? y2 : other.y2;            
        return new Rec2(minX,minY,maxX,maxY);
    }
    
    public void set(Rec2 r) {
        this.x1 = r.x1;
        this.y1 = r.y1;
        this.x2 = r.x2;
        this.y2 = r.y2;
    }
    
    public static class Interval 
    { 
        public final int start;
        public final int end;
        
        protected Interval(int start, int end)
        {
            this.start = start <= end ? start : end;
            this.end =  end >= start ? end : start;
        }
        
        public Interval intersection(Interval other) {
            
            // sort ascending by start
            final Interval i1 = this.start <= other.start ? this : other;
            final Interval i2 = i1 == this ? other : this;
            
            if ( i2.start >= i1.end ) { 
                return null;
            }
            if ( i2.end <= i1.end ) {
                return new Interval( i2.start , i2.end );
            }
            return new Interval( i2.start , i1.end );
        }
        
        public Interval intersectionEndInclusive(Interval other) {
            
            // sort ascending by start
            final Interval i1 = this.start <= other.start ? this : other;
            final Interval i2 = i1 == this ? other : this;
            
            if ( i2.start > i1.end ) { 
                return null;
            }
            if ( i2.end < i1.end ) {
                return new Interval( i2.start , i2.end );
            }
            return new Interval( i2.start , i1.end );
        }      
        
        public static final class IntervalWithType extends Interval {

            private final OverlapType type;
            
            protected IntervalWithType(int start, int end,OverlapType type)
            {
                super(start, end);
                this.type = type;
            }
            
            public OverlapType getOverlapType()
            {
                return type;
            }
            
            public boolean hasType(OverlapType type) {
                return type.equals(this.type);
            }
        }
        
        /**
         * 
         * The returned <code>OverlapType</code> indicates which part of interval2 (min2,max2)
         * overlapped with interval1 (min1,max1).
         * 
         * @param min1
         * @param max1
         * @param min2
         * @param max2
         * @return
         */
        public static IntervalWithType intersectionEndInclusive(int min1,int max1, int min2,int max2) {
            
            // sort ascending by start
            if ( min1 <= min2 ) {
                
                /*
                 * i1.start = min1
                 * i1.end = max1
                 * 
                 * i2.start = min2
                 * i2.end = max2;
                 * 
                 * final Interval i1 = this.start <= other.start ? this : other;
                 * final Interval i2 = i1 == this ? other : this;
                 * 
                 * if ( i2.start > i1.end ) { 
                 *     return null;
                 * }
                 * if ( i2.end < i1.end ) {
                 *     return new Interval( i2.start , i2.end );
                 * }
                 * return new Interval( i2.start , i1.end );                 
                 */
                
                if ( min2 > max1 ) { 
                    return null;
                }
                
                /*      min1 max1
                 *      |-----|
                 *       |---|
                 *      min2 max2     
                 */
                if ( max2 < max1 ) {
                    return new IntervalWithType( min2 , max2,OverlapType.FULLY_CONTAINED);
                }
                /*  max2 >= max1
                 * 
                 *      min1 max1
                 *      |-----|
                 *          |---|  
                 *        min2  max2   
                 */
                
                return new IntervalWithType( min2 , max1 , OverlapType.START );                
            } 
            
            /*
             * i1.start = min2
             * i1.end = max2
             * 
             * i2.start = min1
             * i2.end = max1;
             */
            
            if ( min1 > max2 ) { 
                return null;
            }
            
            /*  min1 > min2
             * 
             *     min1 max1
             *      |---|
             *  |-------|
             *  min2 max2
             */            
            if ( max1 < max2 ) {
                return new IntervalWithType( min1 , max1 , OverlapType.MIDDLE);
            }
            
            /*  max1 >= max2
             * 
             *     min1 max1
             *      |------|
             *  |-------|
             *  min2 max2
             */              
            return new IntervalWithType( min1 , max2 , OverlapType.END ); 
            
        }
        
        public boolean isAdjacent(Interval other) {
            return this.end == other.start || other.start==this.end;
        }

        @Override
        public int hashCode()
        {
            return 31 * (31  + end) + start;
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof Interval) ) {
                return false;
            }
            Interval other = (Interval) obj;
            return end == other.end && start == other.start;
        }

        @Override
        public String toString()
        {
            return "Interval ( " + start + ", " + end +"]";
        }
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
        if ( obj != null && obj.getClass() == Rec2.class )
        {
            return hasSameCoordinates((Rec2) obj);
        }
        return false;
    }
    
    public final boolean hasSameCoordinates(Rec2 other) 
    {
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
		return "Rec2 ["+getCoordinateString()+"]";
	}
	
    public String getCoordinateString() {
        return "x1=" + x1 + ", y1=" + y1 + ", x2=" + x2 + ", y2=" + y2;
    }	

    @Override
    public boolean containsPoint(int x, int y)
    {
        return x >= x1 && x <= x2 && y >= y1 && y <= y2;
    }

    @Override
    public List<Edge> getEdges()
    {
        return Arrays.asList( new Edge(x1,y1,x2,y1), // top
                               new Edge(x2,y1,x2,y2), // right
                               new Edge(x1,y2,x2,y2), // bottom 
                               new Edge(x1,y1,x1,y2) ); // left
    }

    @Override
    public Rec2 getBoundingBox()
    {
        return new Rec2(this.x1 , this.y1 , x2+1,y2+1 );
    }

    public IPolygon toPolygon() {
        return new Polygon( getEdges() );
    }
    
    @Override
    public boolean containsLine(int x1, int y1, int x2, int y2)
    {
        return contains( x1 , y1 ) && contains( x2, y2 );
    }    
}