package de.codesourcery.sandbox.pathfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.codesourcery.sandbox.pathfinder.Edge.OverlapType;

public class Rec2D 
{
    public double x1;
    public double y1;
    public double x2;
    public double y2;

    protected Rec2D(Vec2 p1,Vec2 p2)
    {
        x1 = p1.x < p2.x ? p1.x : p2.x;
        x2 = p1.x > p2.x ? p1.x : p2.x;
        y1 = p1.y < p2.y ? p1.y : p2.y;
        y2 = p1.y > p2.y ? p1.y : p2.y;
    }
    
    public Rec2D(Rec2 r) {
        this.x1 = r.x1;
        this.x2 = r.x2;
        this.y1 = r.y1;
        this.y2 = r.y2;
    }
    
    public Rec2D(double x1, double y1, double x2, double y2)
    {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    public Rec2D union(Rec2D other) 
    {
        final double minX = x1 < other.x1 ? x1 : other.x1;
        final double minY = y1 < other.y1 ? y1 : other.y1;
        
        final double maxX = x2 > other.x2 ? x2 : other.x2;
        final double maxY = y2 > other.y2 ? y2 : other.y2;            
        return new Rec2D(minX,minY,maxX,maxY);
    }
    
    public void set(Rec2 r) {
        this.x1 = r.x1;
        this.y1 = r.y1;
        this.x2 = r.x2;
        this.y2 = r.y2;
    }
    
    public static class Interval 
    { 
        public final double start;
        public final double end;
        
        protected Interval(double start, double end)
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
            
            protected IntervalWithType(double start, double end,OverlapType type)
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
        public static IntervalWithType intersectionEndInclusive(double min1,double max1, double min2,double max2) {
            
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
        public String toString()
        {
            return "Interval ( " + start + ", " + end +"]";
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(end);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(start);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Interval other = (Interval) obj;
            if (Double.doubleToLongBits(end) != Double.doubleToLongBits(other.end)) {
                return false;
            }
            if (Double.doubleToLongBits(start) != Double.doubleToLongBits(other.start)) {
                return false;
            }
            return true;
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
    
    public List<Rec2D> minus(Rec2D other) 
    {
        if ( ! overlap( other ) ) {
            return Collections.emptyList();
        }
        
        if ( ! completelyContains( other ) ) {
            throw new IllegalArgumentException("Not completely contained: "+other+" IN "+this);
        }
        
        final Rec2D r1 = createRectIfNotEmpty( x1 , y1 , x2 , other.y1 );
        final Rec2D r2 = createRectIfNotEmpty( x1 , other.y2 , x2 , y2 );
        final Rec2D r3 = createRectIfNotEmpty( x1 , other.y1 , other.x1 , other.y2 );
        final Rec2D r4 = createRectIfNotEmpty( other.x2 , other.y1 , x2 , other.y2 );
        
        final List<Rec2D> result = new ArrayList<>();
        if (r1 != null ) { result.add( r1 ); }
        if (r2 != null ) { result.add( r2 ); }
        if (r3 != null ) { result.add( r3 ); }
        if (r4 != null ) { result.add( r4 ); }
        return result;
    }
    
    private static Rec2D createRectIfNotEmpty(double x1,double y1,double x2,double y2) {
        if ( x1 == x2 || y1 == y2 ) {
            return null;
        }
        
        final double minX = x1 < x2 ? x1 : x2;
        final double minY = y1 < y2 ? y1 : y2;
        
        final double maxX = x1 > x2 ? x1 : x2;
        final double maxY = y1 > y2 ? y1 : y2;        
        
        return new Rec2D(minX,minY,maxX,maxY);
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

    public boolean completelyContains(Rec2D other) {
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
    
    public final boolean overlap(Rec2D r2) 
    {
        return ! (x2 <= r2.x1 || y2 <= r2.y1 || x1 >= r2.x2 || y1 >= r2.y2 );
    }
    
    public final boolean intersectsLine(Vec2 p1,Vec2 p2)
    {
        // Find min and max X for the segment
        double minX = p1.x;
        double maxX = p2.x;

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

    public boolean containsPoint(double x, double y)
    {
        return x >= x1 && x <= x2 && y >= y1 && y <= y2;
    }

    public Rec2D getBoundingBox()
    {
        return new Rec2D(this.x1 , this.y1 , x2+1,y2+1 );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(x1);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(x2);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y1);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y2);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if ( ! (obj instanceof Rec2D ) ) {
            return false;
        }
        
        Rec2D other = (Rec2D) obj;
        if (Double.doubleToLongBits(x1) != Double.doubleToLongBits(other.x1)) {
            return false;
        }
        if (Double.doubleToLongBits(x2) != Double.doubleToLongBits(other.x2)) {
            return false;
        }
        if (Double.doubleToLongBits(y1) != Double.doubleToLongBits(other.y1)) {
            return false;
        }
        if (Double.doubleToLongBits(y2) != Double.doubleToLongBits(other.y2)) {
            return false;
        }
        return true;
    }

}