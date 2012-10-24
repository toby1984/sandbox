package de.codesourcery.sandbox.pathfinder;

public class Edge 
{
    public int x1;
    public int y1;

    public int x2;
    public int y2;        

    protected Edge(int x1, int y1, int x2, int y2)
    {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public boolean isVertical() {
        return x1 == x2;
    }

    public boolean isHorizontal() {
        return y1 == y2;
    }   
    
    public boolean overlaps(Edge other) {
        if ( isHorizontal() != other.isHorizontal() ) {
            throw new IllegalArgumentException();
        }
        
        if ( isHorizontal() ) {
            
            
            /*     +-------+
             *     |   t   |
             * +-----------+----+
             * |                |
             * +----------------+
             */
            if ( this.x1 <= other.x1 && this.x2 >= other.x2 ||
                 other.x1 <= this.x1 && other.x2 >= this.x2 ) 
            {
                return true;
            }
            
            /*     +-------+
             *     |   t   |
             * +------+----+
             * |      |
             * +------+
             */            
            if ( other.x1 >= this.x1 ||  this.x1 >= other.x1 ) 
            {
                final int len = other.x2 - this.x2;
                Edge remainder = len <= 0 ? null : new Edge( this.x2 , other.y1 , other.x2 , other.y2 );
                // shorten this edge
                this.x2 = other.x1;
                return remainder;
            } else {

            /* +-------+
             * |   t   |
             * +--+----+---+
             *    |        |
             *    +--------+
             */ 
            }
            
            
        } else {
            
        }
    }

    public Edge intersect(Edge other) 
    {
        if ( isHorizontal() != other.isHorizontal() ) {
            throw new IllegalArgumentException();
        }

        if ( isHorizontal() ) 
        {
            if ( other.x1 >= x1 ) 
            {
                /*     +-------+
                 *     |   t   |
                 * +------+----+
                 * |      |
                 * +------+
                 */
                final int len = other.x2 - this.x2;
                Edge remainder = len <= 0 ? null : new Edge( this.x2 , other.y1 , other.x2 , other.y2 );
                // shorten this edge
                this.x2 = other.x1;
                return remainder;
            }

            /* +-------+
             * |   t   |
             * +--+----+---+
             *    |        |
             *    +--------+
             */                
            final int len = this.x1 - other.x1; 
            Edge remainder = len <= 0 ? null : new Edge( other.x1 , other.y1 , this.x1 , other.y2 );
            
            // shorten this edge
            this.x1 = other.x2;
            return remainder;
        }

        // vertical intersect
        if ( other.y1 >= y1 ) 
        {
            /* +--+
             * |  |
             * | t+--+
             * |  |  |
             * +--+  |
             *    |  |
             *    +--+
             */
            final int len = other.y2  - this.y2;
            Edge remainder = len <= 0 ? null : new Edge( other.x1 , this.y2 , other.x2 , other.y2 );
            // shorten this edge
            this.y2 = other.y1;
            return remainder;
        } 

        /* 
         *    +--+
         *    |  |
         * +--+  |
         * |  |  |
         * | t+--+
         * |  |
         * +--+  
         */
        final int len = this.y1 - other.y1;
        Edge remainder = len <= 0 ? null : new Edge( other.x1 , other.y1 , other.x2 , this.y1 );
        // shorten this edge
        this.y1 = other.y2;            
        return remainder;            
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
}