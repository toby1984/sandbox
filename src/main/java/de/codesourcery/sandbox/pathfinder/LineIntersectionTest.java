package de.codesourcery.sandbox.pathfinder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.sandbox.pathfinder.Edge.EdgeOption;
import de.codesourcery.sandbox.pathfinder.Edge.IntersectionResult;
import de.codesourcery.sandbox.pathfinder.Edge.PointOption;

public class LineIntersectionTest
{
    private final int MODEL_WIDTH=400;
    private final int MODEL_HEIGHT=600;
    
    public static void main(String[] args)
    {
        new LineIntersectionTest().run();
    }
    
    private static class MyPanel extends JPanel {
        
        private final List<Vec2> points = new ArrayList<>();
        
        private IntersectionResult intersectionResult=Edge.EMPTY;
        
        public void addPoint(int x,int y) {
            
            intersectionResult = Edge.EMPTY;
            
            x = ( x / 10 ) * 10;
            y = ( y / 10 ) * 10;
            
            if ( points.size() < 4 ) 
            {
                points.add( new Vec2(x,y) );
            } else {
                points.remove(0);
                points.remove(0);
                points.add( new Vec2(x,y) );
            }

            repaint();
        }        

        @Override
        public void paint(Graphics g)
        {
            super.paint(g);
            
            g.setColor(Color.BLUE);
            
            if ( points.size() >= 2 ) {
                drawLine(points.get(0),points.get(1) ,g );
            } else if ( points.size() == 1 ) {
                drawPoint(points.get(0) ,g );
            }
            
            if ( points.size() >= 4 ) {
                drawLine(points.get(2),points.get(3) ,g );
            } else if ( points.size() >= 3 ) {
                drawPoint( points.get(2) , g );
            }
            
            if ( ! intersectionResult.isEmpty() ) 
            {
                g.setColor(Color.GREEN);
                if ( intersectionResult.isEdge() ) 
                {
                    final EdgeOption edge = intersectionResult.asEdgeOption();
                    g.drawLine(  edge.x1 , edge.y1 ,edge.x2, edge.y2 );
                } else {
                    final PointOption p = intersectionResult.asPointOption();
                    final int r = 8; 
                    g.drawArc( p.x-(r/2) , p.y-(r/2) , r , r , 0 , 360 );
                }
            }
        }
        
        public void intersection()
        {
            if ( points.size() == 4 ) 
            {
                Vec2 p1 = points.get(0);
                Vec2 p2 = points.get(1);
                Vec2 p3 = points.get(2);
                Vec2 p4 = points.get(3);

                Edge e1 = new Edge( p1 , p2 );
                Edge e2 = new Edge( p3, p4 );
                
                this.intersectionResult = e1.calcIntersection(e2 );
                System.out.println( e1+" <-> "+e2+" => "+intersectionResult);
            } else {
                System.out.println("Not enough points selected.");
                this.intersectionResult = Edge.EMPTY;
            }
            repaint();
        }
        
        private void drawLine(Vec2 p1,Vec2 p2,Graphics g) {
            g.drawLine( p1.x,p1.y,p2.x,p2.y );
            
            g.drawString( "( "+p1.x+" , "+p1.y+" ) -> ( "+p2.x+" , "+p2.y+" )" , p1.x, p1.y );            
        }
        
        private void drawPoint(Vec2 p1,Graphics g) {
            final int x = p1.x;
            final int y = p1.y;
            g.drawLine( x-5,y,x+5,y);
            g.drawLine( x,y-5,x,y+5);
            g.drawString( "( "+x+" , "+y+" )" , x, y );
        }        
        
    }
    
    public void run() {

        final MyPanel panel = new MyPanel();
        
//      Random rnd = new Random(System.currentTimeMillis());
//      for ( int i = 0 ; i < 1000 ; i++ ) 
//      {
//          int x = rnd.nextInt(MODEL_WIDTH);
//          int y = rnd.nextInt(MODEL_HEIGHT);
//          regularTree.store( x,y,(byte)1);
//      }

        panel.setPreferredSize(new Dimension(MODEL_WIDTH,MODEL_HEIGHT));

        final MouseAdapter mouseListener = new MouseAdapter() 
        {
            private boolean inPanel = false;
            
            public void mouseClicked(java.awt.event.MouseEvent e) 
            {
                inPanel = true;
                panel.addPoint( e.getX(), e.getY() );
            }
            
            @Override
            public void mouseEntered(MouseEvent e)
            {
                inPanel=true;
            }
            
            @Override
            public void mouseExited(MouseEvent e)
            {
                inPanel=false;
            }
            
            @Override
            public void mouseMoved(MouseEvent e)
            {
                if ( inPanel ) 
                {
                    Graphics g = panel.getGraphics();
                    g.setPaintMode();
                    g.setColor( panel.getForeground() );
                    g.clearRect( 0,0 , 200,20 );
                    g.drawString("                             ",15,15);
                    
                    int x = (e.getX() / 10 )* 10;
                    int y = (e.getY() / 10 )* 10;
                    g.drawString("X = "+x+" / Y = "+y , 15,15 );
                }
            }
        };
        panel.addMouseListener( mouseListener );  
        panel.addMouseMotionListener( mouseListener );

        final JFrame frame = new JFrame("IntersectionTest");
        
        frame.addKeyListener( new KeyAdapter() {
            
            public void keyTyped(java.awt.event.KeyEvent e) {
                
                if ( e.getKeyChar() == ' ' ) 
                {
                    panel.intersection();
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
