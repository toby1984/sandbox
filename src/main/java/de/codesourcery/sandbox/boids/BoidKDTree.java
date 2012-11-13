package de.codesourcery.sandbox.boids;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.sandbox.pathfinder.Vec2;
import de.codesourcery.sandbox.pathfinder.Vec2d;


public class BoidKDTree<T>
{
    private TreeNode<T> root;

    private static final int LEFT = 0;
    private static final int RIGHT = 1;    

    protected static enum Axis {
        X,Y;
    }

    public static abstract class TreeNode<T> 
    {
        public abstract void add(double x,double y,int depth , LeafNode<T> value);

        public abstract boolean isLeaf();

        public abstract void visitPreOrder(int x , int y , KDXYTreeVisitor<T> visitor);

        public abstract void visitPreOrder(int depth , KDTreeVisitor<T> visitor);

        public abstract void visitPreOrder(KDLeafVisitor<T> visitor);        

        public abstract void findNearestNeighbors(int depth , NearestNeighborGatherer<T> gatherer);         
    }

    public interface KDTreeVisitor<T> {
        public void visit(int depth  ,TreeNode<T> node);
    }

    public interface KDXYTreeVisitor<T> {
        public void visit(int x,int y,TreeNode<T> node);
    }    

    public interface KDLeafVisitor<T> {
        public void visit(LeafNode<T> node);
    }    

    public static final class NonLeafNode<T> extends TreeNode<T>
    {
        private final double splitValue;

        private TreeNode<T> left;
        private TreeNode<T> right;

        protected NonLeafNode(double splitValue)
        {
            this.splitValue = splitValue;
        }

        public final void visitPreOrder(int x , int y , KDXYTreeVisitor<T> visitor) 
        {
            visitor.visit( x , y , this );
            
            if ( left != null ) {
                left.visitPreOrder( x , y+1 , visitor );
            }
            if ( right != null ) {
                right.visitPreOrder( x+1 , y+1 , visitor );
            }            
        }        

        @Override
        public void visitPreOrder(KDLeafVisitor<T> visitor)
        {
            if ( left != null ) {
                left.visitPreOrder( visitor );
            } 
            if ( right != null ) {
                right.visitPreOrder( visitor );
            }
        }

        public void add(double x,double y,int depth , LeafNode<T> value) 
        {
            if ( (depth % 2 )== 0 ) 
            {
                // current node is split on X axis
                if ( x < splitValue ) 
                {
                    // left subtree
                    if ( left == null ) {
                        left = value;
                    } 
                    else 
                    {
                        if ( left.isLeaf() ) 
                        {
                            // left == leaf node , split at Y-axis and re-insert
                            final LeafNode<T> tmp = (LeafNode<T>) left;
                            if ( tmp.x == x && tmp.y == y ) 
                            {
                                if ( ! tmp.supportsMultipleValues() ) {
                                    left = new MultiValuedLeafNode<>( (SingleValueLeafNode<T>) tmp );
                                }
                                left.add( x , y , depth , value );
                            } 
                            else 
                            {
                                final double splitY = (tmp.y+y)/2.0;
                                final NonLeafNode<T> newNode = new NonLeafNode<>( splitY );                            
                                newNode.add( tmp.x , tmp.y , depth +1 , tmp );
                                newNode.add( x , y , depth +1 , value );
                                left = newNode;
                            }
                        } else {
                            left.add( x ,  y ,  depth + 1 , value );
                        } 
                    }
                }
                else 
                {
                    // right subtree
                    if ( right == null ) {
                        right = value;
                    } else {
                        if ( right.isLeaf() ) 
                        {
                            // right == leaf node , split at Y-axis and re-insert
                            final LeafNode<T> tmp = (LeafNode<T>) right;                            
                            if ( tmp.x == x && tmp.y == y ) 
                            {
                                if ( ! tmp.supportsMultipleValues() ) {
                                    right = new MultiValuedLeafNode<>( (SingleValueLeafNode<T>) tmp );
                                }
                                right.add( x , y , depth , value );
                            } 
                            else 
                            {                            
                                final double splitY = (tmp.y+y)/2.0;
                                final NonLeafNode<T> newNode = new NonLeafNode<>( splitY );
                                newNode.add( tmp.x , tmp.y , depth +1 , tmp );
                                newNode.add( x , y , depth +1 , value );
                                right = newNode;
                            }
                        } else {
                            right.add( x ,  y ,  depth + 1 , value );
                        } 
                    }                    
                }
            } else {
                // current node is split on Y axis
                if ( y < splitValue ) 
                {
                    // left subtree
                    if ( left == null ) {
                        left = value;
                    } 
                    else 
                    {
                        if ( left.isLeaf() ) {
                            // left == leaf node , split at X-axis and re-insert
                            final LeafNode<T> tmp = (LeafNode<T>) left;
                            if ( tmp.x == x && tmp.y == y ) 
                            {
                                if ( ! tmp.supportsMultipleValues() ) {
                                    left = new MultiValuedLeafNode<>( (SingleValueLeafNode<T>) tmp );
                                }
                                left.add( x , y , depth , value );
                            } 
                            else 
                            {                            
                                final double splitX = (tmp.x+x)/2.0;
                                final NonLeafNode<T> newNode = new NonLeafNode<>( splitX );
                                newNode.add( tmp.x , tmp.y , depth +1 , tmp );
                                newNode.add( x , y , depth +1 , value );
                                left = newNode;
                            }
                        } else {
                            left.add( x ,  y ,  depth + 1 , value );
                        } 
                    }
                }
                else 
                {
                    // right subtree
                    if ( right == null ) {
                        right = value;
                    } else {
                        if ( right.isLeaf() ) {
                            // right == leaf node , split at X-axis and re-insert
                            final LeafNode<T> tmp = (LeafNode<T>) right;
                            if ( tmp.x == x && tmp.y == y ) 
                            {
                                if ( ! tmp.supportsMultipleValues() ) {
                                    right = new MultiValuedLeafNode<>( (SingleValueLeafNode<T>) tmp );
                                }
                                right.add( x , y , depth , value );
                            } 
                            else {                            
                                final double splitX = (tmp.x+x)/2.0;
                                final NonLeafNode<T> newNode = new NonLeafNode<>( splitX );                            
                                newNode.add( tmp.x , tmp.y , depth +1 , tmp );
                                newNode.add( x , y , depth +1 , value );
                                right = newNode;
                            }
                        } else {
                            right.add( x ,  y ,  depth + 1 , value );
                        } 
                    }                    
                }

            }
        }

        @Override
        public final boolean isLeaf()
        {
            return false;
        }

        @Override
        public void visitPreOrder(int depth , KDTreeVisitor<T> visitor)
        {
            visitor.visit( depth , this );

            if ( left != null ) {
                left.visitPreOrder( depth + 1 , visitor );
            }
            if ( right != null ) {
                right.visitPreOrder( depth + 1 , visitor );
            }
        }

        @Override
        public String toString()
        {
            return "NODE[ split="+splitValue+" ]";
        }

        @Override
        public void findNearestNeighbors(int depth , NearestNeighborGatherer<T> gatherer)
        {
            final int visitedTree;
            final boolean xAxis = ( depth % 2 ) == 0;
            if ( xAxis ) 
            {
                // split on X-axis
                if ( gatherer.x < splitValue ) 
                {
                    // left subtree                    
                    visitedTree = LEFT;
                    if ( left != null ) {
                        if ( left.isLeaf() ) {
                            gatherer.addCandidate( (LeafNode<T>) left);
                        } else { 
                            left.findNearestNeighbors( depth +1 , gatherer );
                        }
                    }
                } 
                else 
                {
                    // right subtree
                    visitedTree = RIGHT;                    
                    if ( right != null ) 
                    {
                        if ( right.isLeaf() ) {
                            gatherer.addCandidate( (LeafNode<T>) right);
                        } else {
                            right.findNearestNeighbors( depth +1 , gatherer );
                        }
                    }
                }
            } 
            else // split on Y-axis 
            {  
                if ( gatherer.y < splitValue ) 
                {
                    // left subtree
                    visitedTree = LEFT; 
                    if ( left != null ) 
                    {
                        if ( left.isLeaf() ) {
                            gatherer.addCandidate( (LeafNode<T>) left);
                        } else { 
                            left.findNearestNeighbors( depth + 1 , gatherer );
                        }
                    }
                } 
                else 
                {
                    // right subtree
                    visitedTree = RIGHT;  
                    if ( right != null ) {
                        if ( right.isLeaf() ) {
                            gatherer.addCandidate( (LeafNode<T>) right);
                        } else {
                            right.findNearestNeighbors( depth + 1 , gatherer );
                        }
                    }
                }                
            }

            if ( ! gatherer.isFull() ) {
                // check if the other subtree may contain values that are closer 
                // than the point farthest out we've found so far
                final double distance;
                if ( xAxis ) {
                    distance = Math.abs( gatherer.x - splitValue );
                } else {
                    distance = Math.abs( gatherer.y - splitValue );
                }

                final boolean isWithinRadius= distance <= gatherer.radius ;
                if ( right != null && isWithinRadius && visitedTree == LEFT ) {
                    if ( right.isLeaf() ) {
                        gatherer.addCandidate( (LeafNode<T>) right);
                    } else {
                        right.findNearestNeighbors( depth + 1 , gatherer );
                    }
                } 
                else if ( left != null && isWithinRadius && visitedTree == RIGHT  ) 
                {
                    if ( left.isLeaf() ) {
                        gatherer.addCandidate( (LeafNode<T>) left);
                    } else {
                        left.findNearestNeighbors( depth + 1 , gatherer );
                    }
                }
            }
        }
    }    

    public static final class NearestNeighborGatherer<T> {

        public final double x;
        public final double y;
        public final double radius;
        public final double radiusSquared;

        private int valueCount = 0;
        public final int maxNeighborCount;
        public double maxDistanceSquared=0;

        private final PriorityQueue<NodeWithDistance<T>> queue;

        public NearestNeighborGatherer(double x, double y, double radius, int maxNeighborCount)
        {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.radiusSquared = radius*radius;
            this.maxNeighborCount = maxNeighborCount;
            queue = new PriorityQueue<>( maxNeighborCount );
        }

        public List<T> getResults() 
        {
            final List<T> result = new ArrayList<>();
            int i = 0;

            NodeWithDistance<T> current = null;
            while ( i < maxNeighborCount && ( current = queue.poll() ) != null ) 
            {
                current.node.addValues( result );
                i++;
            }
            return result;
        }

        public boolean isFull() {
            return valueCount > 50;
        }

        public void addCandidate(LeafNode<T> node) 
        {
            double dx = (x - node.x)*(x - node.x);
            double dy = (y - node.y)*(y - node.y);

            double distanceSquared = dx+dy;
            if ( distanceSquared < radiusSquared ) { 
                if ( dx > maxDistanceSquared ) {
                    maxDistanceSquared = distanceSquared;
                }
                valueCount += node.getValueCount();
                queue.add( new NodeWithDistance<T>(node,distanceSquared ) );
            }
        }
    }

    protected static final class NodeWithDistance<T> implements Comparable<NodeWithDistance<T>> 
    {
        public final double distanceSquared;
        public final LeafNode<T> node;

        public NodeWithDistance(LeafNode<T> node,double distanceSquared) {
            this.distanceSquared = distanceSquared;
            this.node = node;
        }

        @Override
        public int compareTo(NodeWithDistance<T> o)
        {
            if ( this.distanceSquared < o.distanceSquared ) {
                return -1;
            } 
            if ( this.distanceSquared == o.distanceSquared ) {
                return 0;
            }
            return 1;
        }
    }

    public static abstract class LeafNode<T> extends TreeNode<T>
    {
        public final double x;
        public final double y;

        public LeafNode(double x, double y)
        {
            this.x = x;
            this.y = y;
        }

        public abstract int getValueCount();

        @Override
        public final void visitPreOrder(KDLeafVisitor<T> visitor)
        {
            visitor.visit( this );
        }

        public abstract boolean supportsMultipleValues();

        @Override
        public void add(double x, double y, int depth, LeafNode<T> value)
        {
            throw new RuntimeException("Not supported: add()");
        }

        public abstract void addValues(Collection<T> collection);

        @Override
        public final boolean isLeaf()
        {
            return true;
        }

        @Override
        public final void visitPreOrder(int depth , KDTreeVisitor<T> visitor)
        {
            visitor.visit( depth  , this );
        }

        public final void visitPreOrder(int x , int y , KDXYTreeVisitor<T> visitor) {
            visitor.visit( x , y , this );
        }

        public final void visitClosestNeighbor(double x,double y,int depth, KDTreeVisitor<T> visitor) 
        {
            visitor.visit( depth , this );
        }

        @Override
        public final void findNearestNeighbors(int depth, NearestNeighborGatherer<T> gatherer)
        {
            throw new RuntimeException("This method must never be called.");            
        }
    }

    public static final class SingleValueLeafNode<T> extends LeafNode<T>
    {
        public final T value;

        public SingleValueLeafNode(double x, double y, T value)
        {
            super(x,y);
            this.value = value;
        }

        @Override
        public int getValueCount()
        {
            return 1;
        }

        public boolean supportsMultipleValues() {
            return false;
        }

        @Override
        public void addValues(Collection<T> collection)
        {
            collection.add( value );
        }

        @Override
        public void add(double x, double y, int depth, LeafNode<T> value)
        {
            throw new RuntimeException("Not supported: add() on single-valued leaf node");
        }

        @Override
        public String toString()
        {
            return "LEAF[ "+x+" , "+y +" ] = "+value;
        }
    }    

    public static final class MultiValuedLeafNode<T> extends LeafNode<T>
    {
        public final List<T> values = new ArrayList<>();

        public MultiValuedLeafNode(SingleValueLeafNode<T> node) {
            super( node.x ,node.y );
            values.add( node.value );
        }

        public MultiValuedLeafNode(double x, double y, T value)
        {
            super(x,y);
            this.values.add( value );
        }

        public boolean supportsMultipleValues() {
            return true;
        }        

        @Override
        public void addValues(Collection<T> collection)
        {
            collection.addAll( values );
        }

        @Override
        public void add(double x, double y, int depth, LeafNode<T> node)
        {
            if ( node.supportsMultipleValues() ) {
                values.addAll( ((MultiValuedLeafNode<T>) node).values );                
            } else {
                values.add( ((SingleValueLeafNode<T>) node).value );
            }
        }

        @Override
        public String toString()
        {
            return "LEAF[ "+x+" , "+y +" ] = "+values;
        }

        @Override
        public int getValueCount()
        {
            return values.size();
        }
    }     

    public BoidKDTree() {
    }

    public void visitPreOrder(KDXYTreeVisitor<T> visitor) {

        if ( root != null ) {
            root.visitPreOrder(0,0,visitor);
        }
    }

    @Override
    public String toString() 
    {
        final StringBuilder builder = new StringBuilder();        
        if ( root != null ) 
        {
            final KDTreeVisitor<T> v = new KDTreeVisitor<T>() {

                @Override
                public void visit(int depth  ,TreeNode<T> node)
                {
                    final String indent = StringUtils.repeat( "  ", depth );
                    builder.append(indent+node.toString()).append("\n");
                }

            };
            root.visitPreOrder( 0 , v );
        }
        return builder.toString();
    }

    public void add(double x,double y,T value) 
    {
        if ( root == null ) {
            root = new NonLeafNode<T>(x);
        }
        root.add( x,y , 0 , new SingleValueLeafNode<>( x , y , value) );
    }

    public List<T> findClosestNeighbours(double x,double y,double radius,int maxCount) {

        NearestNeighborGatherer<T> gatherer = new NearestNeighborGatherer<>( x,y,radius,maxCount );
        if ( root != null ) {
            root.findNearestNeighbors( 0 , gatherer );
        }
        return gatherer.getResults();
    }

    public void visitPreOrder(KDLeafVisitor<T> visitor) {
        if ( root != null ) {
            root.visitPreOrder( visitor );
        }
    }

    private static final double MODEL_WIDTH = 400;
    private static final double MODEL_HEIGHT = 400;

    public static void main(String[] args)
    {
        final BoidKDTree<Vec2d> tree = new BoidKDTree<Vec2d>();

        Random rnd = new Random(System.currentTimeMillis());
        for ( int i = 0 ; i < 10 ; i++ ) 
        {
            final double x = rnd.nextDouble()*MODEL_WIDTH;
            final double y = rnd.nextDouble()*MODEL_HEIGHT;
            tree.add( x , y , new Vec2d(x,y) );
        }

        final MyPanel panel = new MyPanel(tree);

        panel.setPreferredSize(new Dimension((int) MODEL_WIDTH*2,(int) MODEL_HEIGHT*2));

        panel.addMouseListener( new MouseAdapter() 
        {
            public void mouseClicked(java.awt.event.MouseEvent e) 
            {
                final Vec2d p = panel.viewToModel( e.getX() , e.getY() );
                panel.mark( p.x , p.y , 25 );
            };
        } );  

        final JFrame frame = new JFrame("KDTreeTest");

        frame.addKeyListener( new KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent e) {};
        } );
        frame.getContentPane().setLayout( new GridBagLayout() );
        final GridBagConstraints cnstrs = new GridBagConstraints();
        cnstrs.fill = GridBagConstraints.BOTH;
        cnstrs.gridx = GridBagConstraints.REMAINDER;
        cnstrs.gridy = GridBagConstraints.REMAINDER;
        cnstrs.weightx = 1.0;
        cnstrs.weighty = 1.0;
        frame.getContentPane().add( panel , cnstrs );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.pack();
        frame.setVisible( true );        
    }

    protected static final class MyPanel extends JPanel {

        private final BoidKDTree<Vec2d> tree;
        private double xInc;
        private double yInc;

        private double markX;
        private double markY;
        private double markRadius;

        public MyPanel(BoidKDTree<Vec2d> tree) {
            this.tree = tree;
        }

        @Override
        public void paint(final Graphics g)
        {
            super.paint(g);

            xInc = getWidth() / MODEL_WIDTH;
            yInc = getHeight() / MODEL_HEIGHT;

//            render((Graphics2D) g);
            renderTree((Graphics2D) g);
        }

        private void renderTree(final Graphics2D g)
        {
            final double xCenter = MODEL_WIDTH / 2.0;
            
            final double columnWidth = MODEL_WIDTH / 40.0;
            final double rowHeight = MODEL_HEIGHT / 40.0;
            
            final int[] leafCount={0};
            final KDXYTreeVisitor<Vec2d> leafVisitor = new KDXYTreeVisitor<Vec2d>() {

                @Override
                public void visit(int x, int y, TreeNode<Vec2d> node)
                {
//                    System.out.println("x="+x+" / y = "+y+" "+( node.isLeaf() ? " <<<<" : "")+" , parent = "+parent);
                    final double modelX = xCenter + ( x * columnWidth );
                    final double modelY = 10.0 + ( y * rowHeight );
                    if ( node.isLeaf() ) {
                        leafCount[0]++;
                        g.setColor(Color.RED);
                    } else {
                        g.setColor(Color.BLACK);
                    }
                    drawCircle( modelX , modelY , 4 , g );
                }
            };
            
            tree.visitPreOrder( leafVisitor );
            
            System.out.println("Leafs: "+leafCount[0]);
        }
        
        private void render(final Graphics2D g)
        {
            final KDLeafVisitor<Vec2d> visitor = new KDLeafVisitor<Vec2d>() {

                @Override
                public void visit(LeafNode<Vec2d> node)
                {
                    drawPoint( new Vec2d( node.x , node.y ) , g );
                }
            };
            
            g.setColor( Color.black );
            tree.visitPreOrder( visitor );

            if ( markX != 0 ) {
                g.setColor(Color.RED);
                drawCircle(markX,markY,markRadius,g);

                long time1 = -System.currentTimeMillis();
                final List<Vec2d> neighbours = tree.findClosestNeighbours( markX,markY,markRadius, 50 );
                time1 += System.currentTimeMillis();
                System.out.println("Time: "+time1);
                double maxDistance = 0;
                Vec2d farest = Vec2d.ORIGIN;
                for ( Vec2d neighbour : neighbours ) {
                    drawPoint( neighbour , g );
                    double distance = neighbour.minus( markX ,  markY ).length();
                    if ( farest == Vec2d.ORIGIN || distance > maxDistance ) {
                        farest = neighbour;
                        maxDistance = distance;
                    } 
                }

                if ( farest != Vec2d.ORIGIN ) {
                    g.setColor(Color.GREEN );
                    drawCircle( markX , markY , maxDistance , g);
                }
            }
        }

        private void drawPoint(Vec2d value,Graphics g)
        {
            final Vec2 p = modelToView( value.x , value.y );
            g.drawRect( p.x , p.y , 1 , 1 );
        }

        private void drawCircle(double x, double y , double radius,Graphics g)
        {
            final Vec2 p = modelToView( x , y );
            final double viewRadius = radius * xInc;
            final double x1 = p.x - viewRadius;
            final double y1 = p.y - viewRadius;
//            System.out.println("Drawing circle at "+x+" , "+y);
            g.drawOval( round(x1) , round(y1) , round(viewRadius*2) , round(viewRadius*2) ); 
        }        

        public Vec2 modelToView(double x,double y) {
            final int xModel = round( x * xInc);
            final int yModel = round( y * yInc);
            return new Vec2( xModel , yModel );
        }

        private static int round(double v) {
            return (int) Math.round( v );
        }

        public Vec2d viewToModel(int x,int y) {
            final double xModel = x / xInc;
            final double yModel = y / yInc;
            return new Vec2d( xModel , yModel );
        }

        public void mark(double x,double y,double radius) 
        {
            this.markX=x;
            this.markY=y;
            this.markRadius=radius;
            repaint();
        }
    }
}