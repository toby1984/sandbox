package de.codesourcery.sandbox.pathfinder;

import de.codesourcery.sandbox.pathfinder.Rec2.Interval;
import de.codesourcery.sandbox.pathfinder.Rec2.Interval.IntervalWithType;


public class Edge 
{
    public int x1;
    public int y1;

    public int x2;
    public int y2;        

    public Edge(Vec2 p1,Vec2 p2)
    {
        this( p1.x ,p1.y , p2.x ,p2.y);
    }
    
    public Edge(int x1, int y1, int x2, int y2)
    {
        if ( x1 <= x2 ) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        } else {
            this.x1 = x2;
            this.y1 = y2;
            this.x2 = x1;
            this.y2 = y1;            
        }
    }
    
    @Override
    public String toString()
    {
        return "("+x1+","+y1+") -> ("+x2+","+y2+")";
    }

    public boolean isVertical() {
        return x1 == x2;
    }

    public boolean isHorizontal() {
        return y1 == y2;
    }   
    
    public Rec2 getBoundingBox() 
    {
        final int minX;
        final int minY;
        final int maxX;
        final int maxY;
        
        if ( x1 <= x2 ) {
            minX = x1;
            maxX = x2;
        } else {
            minX = x2;
            maxX = x1;            
        }
        
        if ( y1 <= y2 ) {
            minY = y1;
            maxY = y2;
        } else {
            minY = y2;
            maxY = y1;            
        }        
        // not that rectangles treat (x2,y2) as not being part of the rectangle 
        // so we need to increment these values by 1  
        return new Rec2(minX,minY,maxX+1,maxY+1);
    }
    
    public static abstract class IntersectionResult 
    {
        public boolean isEdge() {
            return false;
        }
        public boolean isPoint() {
            return false;
        }
        public boolean isEmpty() {
            return false;
        }
        
        public PointOption asPointOption() {
            throw new RuntimeException( this+" is no PointOption");
        }
        
        public EdgeOption asEdgeOption() {
            throw new RuntimeException( this+" is no EdgeOption");
        }        
    }
    
    public static final EmptyOption EMPTY = new EmptyOption();
    
    public static enum OverlapType {
        START,
        END,
        MIDDLE,
        FULLY_CONTAINED;
    }
    
    public static final class EdgeOption extends IntersectionResult 
    {
        
        public final OverlapType type;
        public final int x1;
        public final int y1;
        public final int x2;
        public final int y2;
        
        public EdgeOption(int x1,int y1,int x2,int y2,OverlapType type) {
            this.x1 = x1;
            this.y1= y1;
            this.x2= x2;
            this.y2=y2;
            this.type = type;
        }
        
        public boolean hasType(OverlapType t) {
            return t.equals( this.type );
        }
        
        public OverlapType getOverlapType()
        {
            return type;
        }
        
        public boolean isEdge() {
            return true;
        }        
        
        public Edge toEdge() {
            return new Edge(x1,y1,x2,y2);
        }
        
        @Override
        public String toString()
        {
            return "Segment intersection( "+toEdge()+")";
        }        
        
        public EdgeOption asEdgeOption() {
            return this;
        }          
    }
    
    public static final class PointOption extends IntersectionResult {
        public final int x;
        public final int y;
        public PointOption(int x,int y) {
            this.x = x;
            this.y = y;
        }
        public boolean isPoint() {
            return true;
        }      
        
        @Override
        public String toString()
        {
            return "Point intersection("+x+","+y+")";
        }        
        
        public PointOption asPointOption() {
            return this;
        }        
    }    
    
    public static final class EmptyOption extends IntersectionResult {
        public boolean isEmpty() {
            return true;
        }        
        @Override
        public String toString()
        {
            return "<no intersection>";
        }
    }    
    
    public Vec2d clockwiseNormal() {
        // ( y2-y1 , x1-x2 )
        return new Vec2d(y2-y1,x1-x2).normalize();
    }
    
    public Vec2d counterClockwiseNormal() {
        return new Vec2d( y1-y2 , x2-x1 ).normalize();
    }    
    
    /**
     * Check whether a given point is on this edge.
     * 
     * @param x
     * @param y
     * @return
     */
    public boolean containsPoint(double x,double y) {

        final int minY = y1 <= y2 ? y1 : y2;
        final int maxY = y1 > y2 ? y1 : y2;
        
        if ( x >= x1 && x <= x2 && y >= minY && y <= maxY ) 
        {
            if ( x1 == x2 ) { // this is a vertical line
                return x == this.x1 && y >= minY && y <= maxY;
            }
            if ( y1 == y2 ) { // this is a horizontal line 
                return y == this.y1 && x >= this.x1 && x <= this.x2;
            }
            
            // verify that point satisfies this line's equation: y = mx+b
            final double m = (y2-y1)/(double) (x2-x1);
            final double b = y1-m*x1;
            
            final double expected = (m * x +b);
            final double delta = y - expected;
            return Math.abs( delta ) < 0.0001; // point is on this line
        }
        return false;
    }
    
    /**
     * Calculates the point (if any) where two edges intersect.
     * 
     * @param other
     * @return
     */
    public IntersectionResult calcIntersection(Edge other) {
        
        if ( this == other || this.equals( other ) )
        {
            // trivial case: lines are identical
            return new EdgeOption( this.x1 ,this.y1 , this.x2,this.y2 , OverlapType.FULLY_CONTAINED );
        }
        
        final boolean thisIsAPoint = isPoint();
        final boolean otherIsAPoint = other.isPoint();
        
        if ( thisIsAPoint || otherIsAPoint ) 
        {
            // check point <-> line intersection
            if ( thisIsAPoint && otherIsAPoint ) 
            {
                if ( this.x1 == other.x1 && this.y1 == other.y1 ) {
                    return new PointOption( this.x1, this.y1 );
                }
            }
            if ( thisIsAPoint && other.containsPoint( this.x1  , this.y1 ) ) 
            {
                return new PointOption(this.x1,this.y1);
            }
            if ( otherIsAPoint && containsPoint( other.x1  , other.y1 ) ) 
            {
                return new PointOption(other.x1,other.y1);
            }            
            return EMPTY;
        } 
        
        /* general line equation: y = mx + b
         *
         * m = (y2-y1)/(x2-x1)
         * b = y - m*x
         *
         * <=>
         * 
         * A*x1+B*y1 = C
         * 
         * A = y2-y1
         * B = x1-x2
         */
        
        int a1 = y2-y1;
        int b1 = x1-x2;
        
        int a2 = other.y2-other.y1;
        int b2 = other.x1-other.x2;
        
        final int det = a1*b2 - a2*b1;
        
        if ( det == 0 ) // => line segments are parallel
        { 
            if ( isVertical() ) { // both are vertical line segments
                if ( this.x1 != other.x1 ) {
                    return EMPTY; // cannot possibly overlap
                }
                // project on Y-axis
                IntervalWithType intersection = Interval.intersectionEndInclusive( this.y1 , this.y2 , other.y1 , other.y2 );
                if ( intersection == null ) {
                    return EMPTY;
                }
                return new EdgeOption( this.x1 , intersection.start , this.x1 , intersection.end , intersection.getOverlapType() );
            } 
            
            if ( isHorizontal() ) // both are horizontal line segments
            {
                if ( this.y1 != other.y1 ) {
                    return EMPTY;
                }
                IntervalWithType intersection = Interval.intersectionEndInclusive( this.x1 , this.x2 , other.x1 , other.x2 );
                if ( intersection == null ) {
                    return EMPTY;
                }
                return new EdgeOption( intersection.start , this.y1 , intersection.end , this.y1 , intersection.getOverlapType() );
            }            
            
            // arbitrary parallel line segments
            int m1 = (y2-y1) / (x2-x1 );
            int b21 = y1 - m1 * x1;
            
            int m2 = (other.y2-other.y1) / (other.x2-other.x1 );
            int b22 = other.y1 - m2 * other.x1;
            
            if ( b21 != b22 ) {
                return EMPTY; // no overlap
            }
            
            final IntervalWithType xInterval = Interval.intersectionEndInclusive( this.x1 , this.x2 , other.x1 , other.x2 );
            final IntervalWithType yInterval = Interval.intersectionEndInclusive( this.y1 , this.y2 , other.y1 , other.y2 );            
            
            if ( xInterval == null || yInterval == null ) {
                return EMPTY;
            }
            if ( xInterval.start == xInterval.end &&  yInterval.start ==  yInterval.end ) {
                return new PointOption( xInterval.start , yInterval.start );
            }

            if ( xInterval.getOverlapType() != yInterval.getOverlapType() ) {
                throw new RuntimeException("Internal error, unhandled case");
            }
            return new EdgeOption( xInterval.start , yInterval.start , xInterval.end , yInterval.end , xInterval.getOverlapType() );
        }
        
        double c1 = a1*x1+b1*y1;
        double c2 = a2*other.x1+b2*other.y1;
        
        double x = (b2*c1 - b1*c2)/det;
        double y = (a1*c2 - a2*c1)/det ;   
        
        // make sure the intersection point is actually on both segments
        if ( containsPoint( x,y ) && other.containsPoint( x ,  y ) ) {
            return new PointOption( (int) x , (int) y );
        }
        return EMPTY;
    }
    
    public boolean isPoint() {
        return this.x1==this.x2 && this.y1 == this.y2; 
    }
    
    public double length() {
        return toVec2().length();
    }
    
    public Vec2 toVec2() {
        return new Vec2(this.x2,this.y2).minus(this.x1,this.y1);
    }
    
    public double angleInDeg(Edge other) {
        return toVec2().angleInDeg( other.toVec2() );
    }
    
    public boolean isAdjacent(Edge other) 
    {
        if ( isHorizontal() != other.isHorizontal() ) {
            return false;
        }

        if ( isHorizontal() ) 
        {
            if ( ! other.isHorizontal() ) {
                return false;
            }
            if ( other.y1 == y1+1 || other.y1 == y1-1 ) {
                return (other.x1 >= x1 && other.x1 <= x1) ||
                        (other.x2 >= x1 && other.x2 <= x1);
            }
            return false;
        }

        // vertical adjacent?
        if ( ! other.isVertical() ) {
            return false;
        }
        if ( other.x1 == x1+1 || other.x1 == x1-1 ) {
            return (other.y1 >= y1 && other.y1 <= y1) ||
                    (other.y2 >= y1 && other.y2 <= y1);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return 31 * (31 * (31 * (31 + x1) + x2) + y1) + y2;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        
        if ( obj == null ) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        Edge other = (Edge) obj;
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