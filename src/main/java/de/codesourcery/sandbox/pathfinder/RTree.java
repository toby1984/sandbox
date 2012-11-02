package de.codesourcery.sandbox.pathfinder;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class RTree<T>
{
    private static final int MAX_FILL_FACTOR = 2;
    
    private RegularNode<T> root;
    
    /**
     * Regular tree node that either has other regular tree nodes or leaf nodes 
     * as children.
     * 
     * @author tobias.gierke@voipfuture.com
     */
    protected static class RegularNode<T> extends Rec2 {

        private RegularNode<T> parent;
        
        private final List<RegularNode<T>> children = new ArrayList<>();
        
        public RegularNode(Rec2 r) {
            super(r);
        }
        
        public final RegularNode<T> getParent()
        {
            return parent;
        }
        
        public final void setParent(RegularNode<T> parent)
        {
            this.parent = parent;
        }
        
        public boolean visitPreOrder(RTreeVisitor<T> visitor,int depth ) 
        {
            if ( ! visitor.visitNode( this ,  depth ) ) {
                return false;
            }
            
            for ( RegularNode<T> node : children ) 
            {
                if ( ! node.visitPreOrder( visitor , depth + 1 ) ) 
                {
                    return false;
                }
            }
            return true;
        }
        
        protected void addChild(RegularNode<T> child,boolean updateBB) 
        {
            children.add( child );
            child.setParent( this );
            if ( updateBB ) {
                recalcBoundingBox();
            }
        }
        
        protected void recalcBoundingBox() {
            
            Rec2 bb = null;
            for ( RegularNode<T> child : children ) {
                if ( bb == null ) {
                    bb = child;
                } else {
                    bb = bb.union( child );
                }
            }
            setBoundingBox( bb );
        }        
        
        private void replaceChild(RegularNode<T> child,RegularNode<T> newNode) {
            
            final int len = children.size();
            
            for ( int i = 0 ; i < len ; i++ ) 
            {
                if ( children.get(i) == child ) {
                    children.set( i , newNode );
                    newNode.setParent( this );
                    recalcBoundingBox();
                    return;
                }
            }
            throw new IllegalArgumentException("Unknown child: "+child);
        }        
        
        public void add(RTree<T> tree,Rec2 bb,T value) 
        {
            // find child node to add to (the one with the least enlargement)
            int smallestArea=0;
            RegularNode<T> best=null;
            for ( RegularNode<T> child : children ) 
            {
                final int area = child.union( bb ).getArea();                
                if ( best == null || area < smallestArea ) 
                {
                    smallestArea = area;
                    best = child;
                } 
            }
            
            if ( best != null ) {
                best.add( tree , bb , value );
                return;
            }
            
            if ( children.size() < MAX_FILL_FACTOR ) {
                final Leaf<T> l = new Leaf<T>(bb);
                addChild( l , true );
                l.add( tree , bb , value );
                return;
            }
            splitNode(tree).add( tree , bb, value );
        }
        
        public void add(RTree<T> tree,RegularNode<T> node) 
        {
            // find child node to add to (the one with the least enlargement)
            int smallestArea=0;
            RegularNode<T> best=null;
            for ( RegularNode<T> child : children ) 
            {
                final int area = child.union( node ).getArea();                
                if ( best == null || area < smallestArea ) 
                {
                    smallestArea = area;
                    best = child;
                } 
            }
            
            if ( best != null ) {
                best.add( tree , node );
                return;
            }
            
            if ( children.size() < MAX_FILL_FACTOR ) 
            {
                addChild( node , true );
                return;
            }
            
            splitNode( tree ).add( tree , node );
        }        

        private final RegularNode<T> splitNode(RTree<T> tree) 
        {
            // find nodes with the worst fit
            RegularNode<T> node1 = null;
            RegularNode<T> node2 = null;
            Rec2 largestBB = null;
            int largestArea = 0;
            for ( RegularNode<T> child1 : children ) 
            {
                for ( RegularNode<T> child2 : children ) 
                {
                    if ( child1 != child2 ) 
                    {
                        Rec2 bb = child1.union( child2 );
                        if ( largestArea == 0 ) 
                        {
                            node1 = child1;
                            node2 = child2;
                            largestBB = bb;
                            largestArea = bb.getArea();
                        } 
                        else 
                        {
                            final int area = bb.getArea();
                            if ( area > largestArea ) 
                            {
                                node1 = child1;
                                node2 = child2;    
                                largestBB = bb;
                                largestArea = bb.getArea();                                
                            }
                        }
                    }
                }
            }
            
            return addAfterSplit(tree, node1, node2, largestBB);
        }

        private RegularNode<T> addAfterSplit(RTree<T> tree, RegularNode<T> node1, RegularNode<T> node2, Rec2 largestBB)
        {
            final RegularNode<T> newNode = new RegularNode<T>( largestBB );
            
            newNode.addChild( node1 , false );
            newNode.addChild( node2 , false );
            
            newNode.set( largestBB );
            
            if ( parent != null ) {
                parent.replaceChild( this , newNode );
            } else {
                tree.root = newNode;
            }            
            
            // re-add remaining children
            for ( RegularNode<T> child : children ) 
            {
                if ( child != node1 && child != node2 ) 
                {
                    // pick leaf depending on smallest enlargement
                    Rec2 union1= node1.union( child );
                    Rec2 union2= node2.union( child );
                    if ( union1.getArea() <= union2.getArea() ) {
                        node1.add( tree , child );
                    } else {
                        node2.add( tree , child );
                    }
                }
            }
            return newNode;
        }
        
        protected final void setBoundingBox(Rec2 bb) 
        {
            if ( ! bb.hasSameCoordinates( this ) ) {
                set( bb );
                if ( parent != null ) {
                    parent.recalcBoundingBox();
                }
            }            
        }        
        
        @Override
        public String toString()
        {
            return "RegularNode[ "+getCoordinateString()+" ]";
        }         
    }
    
    protected static final class Leaf<T> extends RegularNode<T>
    {
        private final List<ValueNode<T>> values = new ArrayList<>();
        
        public Leaf(Rec2 bb)
        {
            super(bb);
        }
        
        public boolean visitPreOrder(RTreeVisitor<T> visitor,int depth ) 
        {
            if ( ! visitor.visitNode( this ,  depth ) ) {
                return false;
            }
            
            for ( ValueNode<T> node : values ) {
                if ( ! visitor.visitValue( node , depth +1 ) ) {
                    return false;
                }
            }
            return true;
        }        
        
        protected void addChild(RegularNode<T> child,boolean updateBB) {
            throw new UnsupportedOperationException("addChild() not supported on leaf nodes");
        }
        
        protected void addChild(ValueNode<T> child,boolean updateBB) 
        {
            values.add( child );
            if ( updateBB ) {
                recalcBoundingBox();
            }
        }        
        
        protected void recalcBoundingBox() 
        {
            Rec2 bb = null;
            for ( ValueNode<T> child : values ) {
                if ( bb == null ) {
                    bb = child;
                } else {
                    bb = bb.union( child );
                }
            }
            setBoundingBox( bb );
        }        
        
        public void add(RTree<T> tree,Rec2 bb,T value) 
        {
            if ( values.size() < MAX_FILL_FACTOR ) 
            {
                    final ValueNode<T> l = new ValueNode<T>( bb , value );
                    values.add( l );
                    setBoundingBox( union( l ) );
                    return;
            }
            
            // node is full, split and re-try insert
            splitNode( tree ).add( tree , bb , value );
        }
        
        private RegularNode<T> splitNode(RTree<T> tree) 
        {
            // find node's with the worst fit
            ValueNode<T> node1 = null;
            ValueNode<T> node2 = null;
            Rec2 largestBB = null;
            int largestArea = 0;
            for ( ValueNode<T> child1 : values ) 
            {
                for ( ValueNode<T> child2 : values ) 
                {
                    if ( child1 != child2 ) 
                    {
                        Rec2 bb = child1.union( child2 );
                        if ( largestArea == 0 ) 
                        {
                            node1 = child1;
                            node2 = child2;
                            largestBB = bb;
                            largestArea = bb.getArea();
                        } 
                        else 
                        {
                            final int area = bb.getArea();
                            if ( area > largestArea ) 
                            {
                                node1 = child1;
                                node2 = child2;    
                                largestBB = bb;
                                largestArea = bb.getArea();                                
                            }
                        }
                    }
                }
            }
            
            return addAfterSplit(tree, node1, node2, largestBB);            
        }
        
        private void addChild(ValueNode<T> node1) {
            values.add( node1 );
            recalcBoundingBox();
        }
        
        private RegularNode<T> addAfterSplit(RTree<T> tree, ValueNode<T> node1, ValueNode<T> node2, Rec2 largestBB)
        {
            final RegularNode<T> newNode = new RegularNode<T>( largestBB );
            
            final Leaf<T> leaf1 = new Leaf<T>(node1);
            leaf1.addChild( node1 );
            
            final Leaf<T> leaf2 = new Leaf<T>(node2);
            leaf2.addChild( node2 );
            
            newNode.addChild(leaf1,false);
            newNode.addChild(leaf2,false);
            newNode.set( largestBB ); 
            
            if ( getParent() != null ) {
                getParent().replaceChild( this , newNode );
            } else {
                tree.root = newNode;
            }            
            
            // re-add remaining children
            for ( ValueNode<T> child : values ) 
            {
                if ( child != node1 && child != node2 ) 
                {
                    // pick leaf depending on smallest enlargement
                    Rec2 union1= leaf1.union( child );
                    Rec2 union2= leaf2.union( child );
                    if ( union1.getArea() <= union2.getArea() ) {
                        leaf1.add( tree , child , child.value );
                    } else {
                        leaf2.add( tree , child , child.value );
                    }
                }
            }
            
            return newNode;
        }        
        
        public boolean isLeaf() {
            return true;
        }       
        
        @Override
        public String toString()
        {
            return "LeafNode[ "+getCoordinateString()+" ]";
        }        
    }
    
    protected static final class ValueNode<T> extends Rec2 
    {
        private final T value;
        
        public ValueNode(Rec2 bb,T value) 
        {
            super(bb);
            this.value = value;
        }
        
        public T getValue()
        {
            return value;
        }
        
        @Override
        public String toString()
        {
            return "ValueNode[ "+getCoordinateString()+" ] : "+value;
        }
    }
    
    public interface RTreeVisitor<T> 
    {
        public boolean visitNode(RegularNode<T> node,int depth);
        public boolean visitValue(ValueNode<T> node,int depth);        
    }
    
    public void visitPreOrder(RTreeVisitor<T> visitor) {
        if ( root != null ) {
            root.visitPreOrder( visitor , 0 );
        }
    }

    public String toString() 
    {
        final StringBuilder builder = new StringBuilder();
        final RTreeVisitor<T> visitor = new RTreeVisitor<T>() {

            @Override
            public boolean visitNode(RegularNode<T> node, int depth)
            {
                builder.append( indent(depth ) ).append( node.toString() ).append("\n");
                return true;
            }
            
            private String indent(int depth) {
                return StringUtils.repeat( " " , depth * 2 );
            }

            @Override
            public boolean visitValue(ValueNode<T> node, int depth)
            {
                builder.append( indent(depth ) ).append( node.toString() ).append("\n");             
                return true;
            }
        };
        visitPreOrder( visitor );
        return builder.toString();
    }
    
    public void add(Rec2 bb,T object) {
        if ( root == null ) 
        {
            root = new RegularNode<>(bb);
        }
        root.add( this , bb , object );
    }
    
    public static void main(String[] args)
    {
        final RTree<Integer> rtree = new RTree<Integer>();
        rtree.add( new Rec2( 0 , 0 , 100 ,100 ) , new Integer(1) );
        rtree.add( new Rec2( 10 , 10 , 20 ,20 ) , new Integer(2) );     
        rtree.add( new Rec2( 110 , 110 , 150 ,150 ) , new Integer(3) );         
        System.out.println( rtree );
    }
}
