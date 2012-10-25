package de.codesourcery.sandbox.pathfinder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.lang.StringUtils;

public class KDTree<T>
{
    private final KDTreeNode<T> root;
    
    private static final byte X_AXIS = 0;
    private static final byte Y_AXIS = 1;
    
    public interface IVisitor<T> 
    {
        public boolean visit(KDTreeNode<T> node, int depth);
    }
    
    public static class KDTreeNode<T> extends Rec2 {

        private final byte splitAxis;
        private final int splitPoint;
        
        private KDTreeNode<T> leftChild;
        private KDTreeNode<T> rightChild;
        
        protected KDTreeNode(int x1, int y1, int x2, int y2,byte splitAxis)
        {
            super(x1, y1, x2, y2);
            this.splitAxis=splitAxis;
            splitPoint = calcSplitPoint(splitAxis);
        }
        
        private int calcSplitPoint(byte axis) 
        {
            if ( axis == X_AXIS ) {
                return x1+((x2-x1)/2);
            } 
            return y1+((y2-y1)/2);
        }
        
        private boolean isLeftOfSplitAxis(KDTreeNode<T> node) {
            
            if ( splitAxis == X_AXIS ) {
                return node.x1 < splitPoint;
            } 
            return node.y1 < splitPoint;
        }
        
        public void add(KDLeafNode<T> node) 
        {
            if ( isLeftOfSplitAxis( node ) )
            {

                if ( leftChild == null ) 
                {
                    leftChild = node;
                    return;
                } 
                
                if ( ! leftChild.isLeaf() ) {
                    leftChild.add( node );
                    return;
                } 
                
                System.out.println("LEFT");
                // replace leaf node with split one
                final KDTreeNode<T> newNode;
                if ( splitAxis == X_AXIS ) {
                    // this node is split by X-Axis , split along other (=> Y) axis
                    newNode = new KDTreeNode<T>( x1,y1,splitPoint,y2,Y_AXIS);
                } else {
                   // this node is split by Y-axis, split along other (=> X) axis
                    newNode = new KDTreeNode<T>( x1,y1,x2,splitPoint,X_AXIS);                    
                }
                System.out.println("New node: "+newNode);
                newNode.add( (KDLeafNode<T>) leftChild );
                newNode.add( node );
                leftChild = newNode;
                return;
            } 
            
            /*
             * Right half
             */
            
            if ( rightChild == null ) 
            {
                rightChild = node;
                return;
            } 
            
            if ( ! rightChild.isLeaf() ) {
                rightChild.add( node );
                return;
            } 
            
            System.out.println("RIGHT");
            
            // replace leaf node with split one
            final KDTreeNode<T> newNode;
            if ( splitAxis == X_AXIS ) {
                // this node is split by X-Axis , split along other (=> Y) axis
                newNode = new KDTreeNode<T>( splitPoint,y1,x2,y2,Y_AXIS);
            } else {
               // this node is split by Y-axis, split along other (=> X) axis
                newNode = new KDTreeNode<T>( x1,splitPoint,x2,y2,X_AXIS);                    
            }
            System.out.println("New node: "+newNode);
            newNode.add( (KDLeafNode<T>) rightChild );
            newNode.add( node );
            rightChild = newNode;            
        }
        
        public boolean isLeaf() {
            return false;
        }
        
        public void visitPreOrder(IVisitor<T> v) {
            visitPreOrder( v , 0 );
        }
        
        public boolean visitPreOrder(IVisitor<T> v,int depth) {
            if ( ! v.visit( this, depth) ) {
                return false;
            }
            
            if ( leftChild != null ) {
                if ( ! leftChild.visitPreOrder(v, depth+1) ) {
                    return false;
                }
            }
            
            if ( rightChild != null ) {
                if ( ! rightChild.visitPreOrder( v, depth+1 ) ) 
                {
                    return false;
                }
            }            
            return true;
        }
        
        public void print() 
        {
            final IVisitor<T> v = new IVisitor<T>() {

                @Override
                public boolean visit(KDTreeNode<T> node, int depth)
                {
                    System.out.println( StringUtils.repeat(" ", depth*4)+node );
                    return true;
                }
            };
            visitPreOrder(v);
        }


        @Override
        public String toString()
        {
            if ( isLeaf() ) 
            {
                final KDLeafNode<T> o = (KDLeafNode<T>) this;
                return "Leaf [ x = "+x1+" , y = "+y1+" , width = "+width()+" , height = "+height()+" , value = "+o.value+"]";
            }
            final String axis = splitAxis == X_AXIS ? "X" : "Y";
            return "TreeNode [ x = "+x1+" , y = "+y1+" , width = "+width()+" , height = "+height()+" , splitAxis = " + axis + ", splitPoint=" + splitPoint + "]";
        }
       
    }
    
    public static final class KDLeafNode<T> extends KDTreeNode<T> {

        private T value;
        
        protected KDLeafNode(int x1, int y1, int x2, int y2,T value)
        {
            super(x1, y1, x2, y2,X_AXIS);
            this.value = value;
        }
        
        public T getValue()
        {
            return value;
        }
        
        public void setValue(T value)
        {
            this.value = value;
        }
        
        public boolean isLeaf() {
            return true;
        }        
    }    
    
    public KDTree(int width,int height) {
        root = new KDTreeNode<T>(0,0,width,height,X_AXIS);
    }
    
    public void store(int x,int y,int width,int height,T value) {
        root.add( new KDLeafNode<T>(x,y,x+width,y+height,value) );
    }
    
    public void visitPreOrder(IVisitor<T> visitor) {
        root.visitPreOrder(visitor);
    }
    
    public void print() {
        root.print();
    }
    
    // ==============================================================================

    private static final int MODEL_WIDTH=20;
    private static final int MODEL_HEIGHT=20;
    
    private static final KDTree<Integer> tree = new KDTree<Integer>(MODEL_WIDTH,MODEL_HEIGHT);
    
    protected static final class MyPanel extends JPanel 
    {
        private double xInc;
        private double yInc;
        
        @Override
        public void paint(final Graphics g)
        {
            recalc();
            super.paint(g);
            
            final IVisitor<Integer> visitor1 = new IVisitor<Integer>() {

                @Override
                public boolean visit(KDTreeNode<Integer> node, int depth)
                {
                    if ( node.isLeaf() ) {
                        Rec2 rect = modelToView(node);
                        g.setColor(Color.RED);
                        g.fillRect(rect.x1,rect.y1,rect.width() , rect.height() );
                    } 
                    return true;
                }
            };
            
            final IVisitor<Integer> visitor2 = new IVisitor<Integer>() {

                @Override
                public boolean visit(KDTreeNode<Integer> node, int depth)
                {
                    if ( ! node.isLeaf() ) {
                        Rec2 rect = modelToView(node);
                        g.setColor(Color.BLUE);
                        g.drawRect(rect.x1,rect.y1,rect.width() , rect.height() );
                        
                        g.setColor(Color.GREEN);
                        if ( node.splitAxis == X_AXIS ) {
                            Point p1 = modelToView( node.splitPoint, node.y1 );
                            Point p2 = modelToView( node.splitPoint , node.y2 );
                            g.drawLine( p1.x,p1.y,p2.x,p2.y);
                        } else {
                            Point p1 = modelToView( node.x1 , node.splitPoint );
                            Point p2 = modelToView( node.x2 , node.splitPoint );
                            g.drawLine( p1.x,p1.y,p2.x,p2.y);                            
                        }
                    } 
                    return true;
                }
            };
            
            tree.visitPreOrder( visitor1 );            
            tree.visitPreOrder( visitor2 );            
        }
        
        private void recalc() {
            xInc = getWidth() / MODEL_WIDTH;
            yInc = getHeight() / MODEL_HEIGHT;
        }
        
        public Rec2 modelToView(Rec2 r) 
        {
            final Point p1 = modelToView( r.x1 , r.y1 );
            final Point p2 = modelToView( r.x2 , r.y2 );
            
            return new Rec2( p1.x , p1.y , p2.x , p2.y ); 
        }        
        
        public Point viewToModel(int x,int y) {
            return new Point( (int) (x / xInc) , (int) (y / yInc ) );
        }
        
        public Point modelToView(int x,int y) {
            return new Point( (int) (x*xInc) , (int) (y * yInc ) );
        }        
    }
    
    public static void main(String[] args)
    {
        final MyPanel panel = new MyPanel();
        
        panel.setPreferredSize(new Dimension(700,400));

        panel.addMouseListener( new MouseAdapter() 
        {
            public void mouseClicked(java.awt.event.MouseEvent e) 
            {
                final Point p = panel.viewToModel( e.getX() , e.getY() );
                System.out.println("Storing at "+p.x+" / "+p.y);
                tree.store( p.x,p.y, 1, 1, 1 );
                tree.print();
                panel.repaint();
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
}
