package de.codesourcery.sandbox.pathfinder;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class QuadTree<T> {

    private QuadNode<T> root;

    public interface IVisitor<T> {
        public boolean visit(QuadNode<T> node,int currentDepth);
    }	

    public static class QuadNode<T> extends Rec2
    {
        protected QuadNode<T> q0= null;
        protected QuadNode<T> q1= null;
        protected QuadNode<T> q2= null;
        protected QuadNode<T> q3= null;

        @Override
        public String toString() {
            return "BSPNode [ x=" + x1 + ", y = "+y1+" , width=" + width()+ ", height=" + height() +"]";
        }

        protected final QuadNode<T> getChild(int quadrant) 
        {
            switch(quadrant) {
                case 0:
                    return q0;
                case 1:
                    return q1;
                case 2:
                    return q2;
                case 3:
                    return q3;
                default:
                    throw new IllegalArgumentException("Unknown quadrant: "+quadrant);
            }
        }

        protected final void setChild(int quadrant,QuadNode<T> child) 
        {
            switch(quadrant) {
                case 0:
                    q0=child;
                    break;
                case 1:
                    q1=child;
                    break;					
                case 2:
                    q2=child;
                    break;					
                case 3:
                    q3=child;
                    break;					
                default:
                    throw new IllegalArgumentException("Unknown quadrant: "+quadrant);
            }			
        }

        public QuadNode(Rec2 r) {
            super(r.x1,r.y1,r.x2,r.y2);
        }

        public QuadNode(int x, int y,int width,int height) {
            super(x,y,x+width,y+height);
        }

        public final boolean visitPreOrder(IVisitor<T> v) {
            return visitPreOrder(v,0);
        }

        public final boolean visitPreOrder(IVisitor<T> v,int currentDepth) {

            if ( ! v.visit( this , currentDepth ) ) {
                return false;
            }

            if ( q0 != null && ! q0.visitPreOrder( v , currentDepth +1 ) ) {
                return false;
            }

            if ( q1 != null && ! q1.visitPreOrder( v , currentDepth +1 ) ) {
                return false;
            }

            if ( q2 != null && ! q2.visitPreOrder( v , currentDepth +1 ) ) {
                return false;
            }		

            if ( q3 != null && ! q3.visitPreOrder( v , currentDepth +1 ) ) {
                return false;
            }			

            return true;
        }

        public final QuadLeafNode<T> getValue(int x,int y) 
        {
            if ( isLeaf() ) {
                return (QuadLeafNode<T>) this;
            }

            switch ( getQuadrant( x, y ) ) 
            {
                case 0:
                    if ( q0 != null ) {
                        return q0.getValue(x, y);
                    }
                    break;
                case 1:
                    if ( q1 != null ) {
                        return q1.getValue(x, y);
                    }
                    break;
                case 2:
                    if ( q2 != null ) {
                        return q2.getValue(x, y);
                    }
                    break;
                case 3:
                    if ( q3 != null ) {
                        return q3.getValue(x, y);
                    }
                    break;
                default:
                    // $$FALL-THROUGH$$
            }
            return null;
        }

        public List<QuadLeafNode<T>> getValues(Vec2 p1,Vec2 p2) 
        {
            final List<QuadLeafNode<T>> result = new ArrayList<>();
            if ( this.intersectsLine( p1,p2 ) ) {
                getValues(p1,p2,result);
            }
            return result;
        }		

        private void getValues(Vec2 p1,Vec2 p2,List<QuadLeafNode<T>> result) {
            if ( isLeaf() ) {
                result.add( (QuadLeafNode<T>) this);
            } 
            else 
            {
                if ( q0 != null && q0.intersectsLine( p1,p2 ) ) {
                    q0.getValues( p1,p2 , result );
                }
                if ( q1 != null && q1.intersectsLine( p1,p2 ) ) {
                    q1.getValues( p1,p2 , result );
                }
                if ( q2 != null && q2.intersectsLine( p1,p2 ) ) {
                    q2.getValues( p1,p2 , result );
                }
                if ( q3 != null && q3.intersectsLine( p1,p2 ) ) {
                    q3.getValues( p1,p2 , result );
                }                
            }
        }

        public List<QuadLeafNode<T>> getValues(Rec2 rect) 
        {
            return getValues( rect.x1 , rect.y1 , rect.width() , rect.height() );
        }

        public boolean containsValues(int x,int y,int width,int height) 
        {
            if ( isLeaf() ) {
                return true;
            }

            if ( ! overlap(x,y,width,height) ) {
                return false;
            }

            if ( q0 != null && q0.containsValues( x,y,width,height ) ) 
            {
                return true;
            }
            if ( q1 != null && q1.containsValues( x,y,width,height ) ) 
            {
                return true;
            }         
            if ( q2 != null && q2.containsValues( x,y,width,height ) ) 
            {
                return true;
            }  
            if ( q3 != null && q3.containsValues( x,y,width,height ) ) 
            {
                return true;
            }              
            return false;
        }

        public List<QuadLeafNode<T>> getValues(int x,int y,int width,int height) 
        {
            final List<QuadLeafNode<T>> result = new ArrayList<>();
            if ( overlap( x, y, width,height ) ) { 
                getValues(x,y,width,height, result);
            }
            return result;
        }

        private void getValues(int x,int y,int width,int height,List<QuadLeafNode<T>> result) 
        {
            if ( isLeaf() ) 
            {
                final QuadLeafNode<T> leaf = (QuadLeafNode<T>) this;
                T v = leaf.getValue();
                if ( v!= null ) {
                    result.add( leaf );
                }
            } 
            else 
            {
                if ( q0 != null && q0.overlap( x ,y, width, height ) ) {
                    q0.getValues( x , y , width , height , result );
                }
                if ( q1 != null && q1.overlap( x ,y, width, height ) ) {
                    q1.getValues( x , y , width , height , result );
                }
                if ( q2 != null && q2.overlap( x ,y, width, height ) ) {
                    q2.getValues( x , y , width , height , result );
                }
                if ( q3 != null && q3.overlap( x ,y, width, height ) ) {
                    q3.getValues( x , y , width , height , result );
                }					
            }
        }

        public void print() {

            final IVisitor<T> visitor = new IVisitor<T>() {

                @Override
                public boolean visit(QuadNode<T> node, int currentDepth) 
                {
                    final int indent = currentDepth*4;
                    System.out.println( StringUtils.repeat(" ", indent)+node);
                    return true;
                }

            };
            visitPreOrder( visitor );
        }	

        private int getQuadrant(QuadNode<T> node) 
        {
            final int centerX = node.width() == 1 ? node.x1 : node.x1 + (node.width()/2);
            final int centerY = node.height() == 1 ? node.y1 : node.y1 + (node.height()/2);
            return getQuadrant( centerX , centerY );

            //            if ( getQuadrantBounds(0).completelyContains( node ) ) {
            //                return 0;
            //            }
            //            
            //            if ( getQuadrantBounds(1).completelyContains( node ) ) {
            //                return 1;
            //            }       
            //            
            //            if ( getQuadrantBounds(2).completelyContains( node ) ) {
            //                return 2;
            //            }            
            //            
            //            if ( getQuadrantBounds(3).completelyContains( node ) ) {
            //                return 3;
            //            }             
            //            
            //            System.err.println( getQuadrantBounds(0)+" contains "+node);
            //            System.err.println( getQuadrantBounds(1)+" contains "+node);
            //            System.err.println( getQuadrantBounds(2)+" contains "+node);
            //            System.err.println( getQuadrantBounds(3)+" contains "+node);
            //            
            //            System.err.println("Not contained.");
            //            return -1;
        }

        private Rec2 getQuadrantBounds(int quadrant) {

            final int w = width();
            final int h = height();

            final int newWidth = w > 1 ? w / 2 : w;
            final int newHeight = h > 1 ? h / 2 : h;

            switch(quadrant) 
            {
                case 0:
                    return new Rec2(x1,y1,x1+newWidth,y1+newHeight);
                case 1:   
                    return new Rec2( x1+newWidth, y1 , x1+w , y1 + newHeight);
                case 2:
                    return new Rec2( x1 , y1 + newHeight , x1 + newWidth , y1 + h );
                case 3:
                    return new Rec2( x1 + newWidth , y1 + newHeight , x1 + w , y1 + h );
                default:
                    throw new IllegalArgumentException("Invalid quadrant: "+quadrant);
            }
        }

        private int getQuadrant(int x , int y) 
        {
            final int w = width();
            final int h = height();

            final int newWidth = w > 1 ? w / 2 : w;
            final int newHeight = h > 1 ? h / 2 : h;

            if ( x >= x1 && x < x1 + newWidth &&
                    y >= y1 && y < y1 + newHeight ) 
            {
                return 0;
            }

            if ( x >= x1+newWidth && x < x1 + w &&
                    y >= y1 && y < y1 + newHeight ) 
            {
                return 1;
            }			

            if ( x >= x1 && x < x1 + newWidth &&
                    y >= y1+newHeight && y < y1 + h) 
            {
                return 2;
            }

            if ( x >= x1+newWidth && x < x1 + w &&
                    y >= y1+newHeight && y < y1 + h) 
            {
                return 3;
            }
            return -1;
        }

        public void add(QuadLeafNode<T> node) 
        {
            if ( isLeaf() ) 
            {
                if ( ! this.matches( node ) )
                {
                    throw new IllegalStateException("Node "+this+" already occupied, can't set "+node);
                }
                ((QuadLeafNode<T>) this).setValue( node.getValue() );
                return;				
            }

            final int quadrant = getQuadrant( node );
            if ( quadrant == -1 ) {
                throw new RuntimeException("Internal error, node "+this+" does not contain "+node);
            }

            final QuadNode<T> child = getChild(quadrant);
            if ( child == null ) 
            {
                setChild(quadrant , node );
                return;
            }

            if ( ! child.isLeaf() ) 
            {
                child.add( node ); // recurse
                return;
            }

            // we already got a leaf at the quadrant we want to insert into
            if ( child.matches( node ) )
            {
                ((QuadLeafNode<T>) child).setValue( node.getValue() );
                return;
            }

            final QuadNode<T> newNode = new QuadNode<T>( getQuadrantBounds( quadrant ) );

            setChild(quadrant,newNode);

            newNode.add( node );
            newNode.add( (QuadLeafNode<T>) child );            
        }

        public boolean isLeaf() {
            return false;
        }
    }

    public static final class QuadLeafNode<T> extends QuadNode<T> {

        private T value;

        public QuadLeafNode(int x, int y, T value) {
            super(x, y, 1, 1 );
            this.value = value;		    
        }

        public QuadLeafNode(Rec2 rect) {
            this( rect.x1 , rect.y1 , rect.width() , rect.height() );
        }

        public QuadLeafNode(Rec2 rect,T value) {
            this( rect.x1 , rect.y1 , rect.width() , rect.height() , value );
        }        

        public QuadLeafNode(int x, int y, int width,int height) {
            super(x, y, width, height );
        }        

        public QuadLeafNode(int x, int y, int width,int height,T value) {
            super(x, y, width, height );
            this.value = value;
        }		

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        public String toString() {
            return "BSPNode [ x=" + x1 + ", y = "+y1+" , width=" + width()+ ", height=" + height()+ " , value=" + value + "]";
        } 
    }

    public QuadTree(int width,int height) 
    {
        root = new QuadNode<T>( 0 , 0 , width , height );
    }

    public static void main(String[] args) 
    {
        final int WIDTH = 10;
        final int HEIGHT =10;

        /*
BSPNode [ x=0, y = 0 , width=100, height=100]
    BSPNode [ x=0, y = 0 , width=50, height=50]
        BSPNode [ x=4, y = 7 , width=20, height=20 , value=1]
        BSPNode [ x=26, y = 13 , width=20, height=20 , value=1]
Rec2 [x1=0, y1=0, x2=25, y2=25] MINUS BSPNode [ x=4, y = 7 , width=20, height=20 , value=1] => [Rec2 [x1=0, y1=0, x2=25, y2=7], Rec2 [x1=0, y1=25, x2=25, y2=27], Rec2 [x1=0, y1=7, x2=4, y2=27], Rec2 [x1=24, y1=7, x2=25, y2=27]]
Rec2 [x1=25, y1=0, x2=50, y2=25] MINUS BSPNode [ x=26, y = 13 , width=20, height=20 , value=1] => [Rec2 [x1=25, y1=0, x2=50, y2=13], Rec2 [x1=25, y1=25, x2=50, y2=33], Rec2 [x1=25, y1=13, x2=26, y2=33], Rec2 [x1=46, y1=13, x2=50, y2=33]]

         */
        final QuadTree<Integer> tree = new QuadTree<Integer>(WIDTH,HEIGHT);

        //		tree.store( 5 , 5 , 42 );
        //		tree.store( 5 , 7 , 43 );
        //		tree.store( 70 , 70 , 44 );
        //		
        //		System.out.println("At (5,5): "+tree.getValue(5,5));
        //		System.out.println("At (5,7): "+tree.getValue(5,7));
        //		
        //		System.out.println("Values: "+tree.getValues(0,0,100,100) );
        //		tree.print();

        //        final List<Point> p = new ArrayList<>();
        //
        //        p.add( new Point(3,1) );
        //        p.add( new Point(4,1) );
        //        p.add( new Point(3,2) );
        //        p.add( new Point(4,2) );
        //        p.add( new Point(3,3) );
        //        p.add( new Point(4,3) );
        //
        //        for ( Point x : p ) {
        //            tree.store( x.x , x.y , 3 );
        //        }
        //
        //        for ( int x = 0 ; x < 7 ; x++ ) {
        //            for ( int y = 0 ; y < 5 ; y++ ) 
        //            {
        //                System.out.println("Value( "+x+","+y+" ) = "+tree.getValues(x, y , 1, 1 ) );
        //            }
        //        }

        //                Random rnd = new Random(System.currentTimeMillis());
        //                long time = -System.currentTimeMillis();
        //                for ( int i = 0 ; i < 1000000 ; i++ ) {
        //                    final int x = rnd.nextInt( WIDTH );
        //                    final int y = rnd.nextInt( HEIGHT );
        //                    final int value = rnd.nextInt(1024);
        //                    tree.store( x , y , value );
        //                    QuadLeafNode<Integer> stored = tree.getValue( x , y );
        //                    if ( stored == null || stored.getValue() == null || stored.getValue().intValue() != value ) {
        //                        throw new RuntimeException("Read at "+x+","+y+" failed, expected "+value+" , got "+stored);
        //                    }
        //                }
        //                time += System.currentTimeMillis();
        //                System.out.println("Insert time: "+time+" ms");
    }

    private int getLeafArea() {

        final int[] result = {0};

        final IVisitor<T> visitor = new IVisitor<T>() {

            @Override
            public boolean visit(QuadNode<T> node, int currentDepth)
            {
                if ( node.isLeaf() ) {
                    result[0]+=node.getArea();
                }
                return true;
            }};

            visitPreOrder( visitor );
            return result[0];
    }

    public void store(int x,int y,int width,int height , T value) 
    {
        try {
            root.add( new QuadLeafNode<T>(x,y,width,height,value) );
        } 
        catch(RuntimeException e) {
            root.print();
            throw e;
        }
    }

    public void store(int x,int y,T value) 
    {
        try {
            root.add( new QuadLeafNode<T>(x,y,value) );
        } 
        catch(RuntimeException e) {
            root.print();
            throw e;
        }
    }

    /**
     * Returns the value closest to a given point.
     * 
     * @param x
     * @param y
     * @return
     */
    public QuadLeafNode<T> getValue(int x,int y) {
        return root.getValue(x,y);
    }

    public List<QuadLeafNode<T>> getValues(Rec2 rect) {
        return root.getValues(rect);
    }

    public List<QuadLeafNode<T>> getValues(Vec2 p1,Vec2 p2) {
        return root.getValues(p1,p2);
    }	

    public boolean containsValues(int x,int y,int width,int height) {
        return root.containsValues(x, y, width,height);
    }

    public List<QuadLeafNode<T>> getValues(int x,int y,int width,int height) {
        return root.getValues( x , y , width , height );
    }

    public void visitPreOrder(IVisitor<T> v) {
        root.visitPreOrder( v );
    }

    public void print() {
        root.print();
    }

}