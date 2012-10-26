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

public class MergeTest
{
    private final int MODEL_WIDTH=400;
    private final int MODEL_HEIGHT=600;
    
    public static void main(String[] args)
    {
        new MergeTest().run();
    }
    
    private static class MyPanel extends JPanel {
        
        private final List<IPolygon> polygons = new ArrayList<>();
        
        private IPolygon mergeResult=null;
        
        public void addPoint(int x,int y) {
            
            mergeResult = null;
            
            x = ( x / 10 ) * 10;
            y = ( y / 10 ) * 10;
            
            if ( polygons.size() >= 2 ) 
            {
                polygons.clear();
            }
            
            final Rec2 r = new Rec2(x,y,x+40,y+30);
            polygons.add( r.toPolygon() );            

            repaint();
        }        

        @Override
        public void paint(Graphics g)
        {
            super.paint(g);
            
            if ( polygons.size() >= 1 ) {
                g.setColor(Color.BLUE);
                drawPolygon(polygons.get(0),g);
            }
            
            if ( polygons.size() >= 2 ) {
                g.setColor(Color.RED);
                drawPolygon(polygons.get(1),g);
            }            
            
            if ( mergeResult != null )
            {
                g.setColor(Color.GREEN);
                drawPolygon( mergeResult , g );
            }
        }
        
        public void merge()
        {
            if ( polygons.size() == 2 ) 
            {
                Polygon newPoly = new Polygon( polygons.get(0).getEdges() );
                
                try {
                newPoly.merge( polygons.get(1) );
                mergeResult = newPoly;
                System.out.println( polygons.get(0)+" <-> "+polygons.get(1)+" => "+mergeResult);
                } catch(IllegalArgumentException e) {
                    System.out.println("Polygons do not overlap.");
                }
            } else {
                System.out.println("Not enough polygons selected.");
                this.mergeResult = null;
            }
            repaint();
        }
        
        private void drawPolygon(IPolygon p,Graphics g) {
            
            for ( Edge e : p.getEdges() ) {
                drawLine( new Vec2( e.x1 , e.y1 ) , new Vec2( e.x2, e.y2 ) , g );
            }
        }
        
        private void drawLine(Vec2 p1,Vec2 p2,Graphics g) 
        {
            g.drawLine( p1.x,p1.y,p2.x,p2.y );
            
//            g.drawString( "( "+p1.x+" , "+p1.y+" ) -> ( "+p2.x+" , "+p2.y+" )" , p1.x, p1.y );            
        }
        
        private void drawPoint(Vec2 p1,Graphics g) {
            final int x = p1.x;
            final int y = p1.y;
            g.drawLine( x-5,y,x+5,y);
            g.drawLine( x,y-5,x,y+5);
//            g.drawString( "( "+x+" , "+y+" )" , x, y );
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
                    panel.merge();
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
