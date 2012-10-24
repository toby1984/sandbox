package de.codesourcery.sandbox.pathfinder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.sandbox.pathfinder.QuadTree.IVisitor;
import de.codesourcery.sandbox.pathfinder.QuadTree.QuadNode;

public class RectPolygon
{
    private List<Edge> horizontal = new ArrayList<>();
    private List<Edge> vertical = new ArrayList<>();

    public static class Edge 
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

    public RectPolygon() {
    }
    
    public List<Edge> getAllEdges() {
        List<Edge> result = new ArrayList<>();
        result.addAll( horizontal );
        result.addAll( vertical );
        return result;
    }

    public void add(Rec2 other) 
    {
        // top
        addEdge( new Edge( other.x1 , other.y1 , other.x2 , other.y1 ) );
        // bottom
        addEdge( new Edge( other.x1 , other.y2 , other.x2 , other.y2 ) );

        // left
        addEdge( new Edge( other.x1 , other.y1 , other.x1 , other.y2 ) );
        // right
        addEdge( new Edge( other.x2 , other.y1 , other.x2 , other.y2 ) );          
    }

    private void addEdge(Edge e) {

        List<Edge> list = e.isHorizontal() ? horizontal : vertical;

        Edge current = e;
outer:            
        for ( Edge edge : list ) 
        {
            if ( edge.isAdjacent( current ) ) {
                current = edge.intersect( current );
                if ( current == null ) {
                    return; // edge full merged
                }
                break outer;
            }
        }
        list.add( current );
    }
    
    public static void main(String[] args)
    {

        final int MODEL_WIDTH = 100;
        final int MODEL_HEIGHT = 100;
        
        final QuadTree<Integer> tree = new QuadTree<Integer>(MODEL_WIDTH,MODEL_HEIGHT);
        
        final JPanel panel = new JPanel() 
        {
            @Override
            public void paint(final Graphics g)
            {
                super.paint(g);
                
                final double xInc = getWidth() / MODEL_WIDTH;
                final double yInc = getHeight() / MODEL_HEIGHT;
                
                final IVisitor<Integer> visitor = new IVisitor<Integer>() {

                    @Override
                    public boolean visit(QuadNode<Integer> node, int currentDepth)
                    {
                        if ( node.isLeaf() ) {
                            renderLeaf( node , xInc , yInc , g );
                        }
                        return true;
                    }
                };
                tree.visitPreOrder( visitor );
            }
            
            private void renderLeaf(QuadNode<Integer> node , double xInc, double yInc , Graphics g) {
                g.setColor( Color.RED );
                int x = (int) (node.x1 * xInc);
                int y = (int) (node.y1 * yInc);
                int w = (int) (node.width() * xInc);
                int h = (int) (node.height() * yInc);
                g.drawRect(x , y , w , h );
            }
        };
        
        panel.setPreferredSize(new Dimension(400,200));

        panel.addMouseListener( new MouseAdapter() 
        {
            public void mouseClicked(java.awt.event.MouseEvent e) 
            {
                final double xInc = panel.getWidth() / MODEL_WIDTH;
                final double yInc = panel.getHeight() / MODEL_HEIGHT;
                
                final int modelX = (int) (e.getX() / xInc);
                final int modelY = (int) (e.getY() / yInc);
                        
                tree.store( modelX , modelY , 10 , 10 , 1 );
                panel.repaint();
            };
        } );  

        final JFrame frame = new JFrame("QuadTreeTest");
        
        frame.addKeyListener( new KeyAdapter() {
            
            public void keyTyped(java.awt.event.KeyEvent e) {
                
                if ( e.getKeyChar() == 'm' ) 
                {
                    // gather values
                    final List<Rec2> rectangles = new ArrayList<>();
                    
                    final IVisitor<Integer> visitor = new IVisitor<Integer>() {

                        @Override
                        public boolean visit(QuadNode<Integer> node, int currentDepth)
                        {
                            if ( node.isLeaf() ) {
                                rectangles.add(node);
                            }
                            return true;
                        }
                    };
                    tree.visitPreOrder( visitor );                    
                    
                    // group adjacent rectangles
                    final List<List<Rec2>> map = new ArrayList<>();
                    for ( Rec2 input : rectangles ) 
                    {
                        boolean matched = false;
outer:                        
                        for ( List<Rec2> list : map ) {
                            for ( Rec2 r : list ) {
                                if ( r.isAdjactant( input ) ) {
                                    list.add( input );
                                    break outer;
                                }
                            }
                        }
                        if ( ! matched ) 
                        {
                            List<Rec2> newList = new ArrayList<>();
                            newList.add( input );
                            map.add( newList );
                        }
                    }
                    
                    // merge adjacent rectangles
                    for ( List<Rec2> l : map ) 
                    {
                        final RectPolygon p = new RectPolygon();
                        for ( Rec2 r : l ) {
                            p.add( r );
                        }

                        final double xInc = panel.getWidth() / MODEL_WIDTH;
                        final double yInc = panel.getHeight() / MODEL_HEIGHT;
                        
                        for ( Edge edge : p.getAllEdges() ) 
                        {
                            int x1 = (int) ( edge.x1 * xInc );
                            int x2 = (int) ( edge.x2 * xInc );
                            int y1 = (int) ( edge.y1 * yInc );
                            int y2 = (int) ( edge.y1 * yInc );
                            panel.getGraphics().setColor( Color.BLUE );
                            panel.getGraphics().drawLine( x1 , y1 , x2 , y2 );
                        }
                    }
                    
                }
                
            };
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
