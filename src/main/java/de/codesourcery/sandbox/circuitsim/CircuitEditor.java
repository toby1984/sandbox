package de.codesourcery.sandbox.circuitsim;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.sandbox.pathfinder.Rec2;

public class CircuitEditor extends JPanel
{
    public static final int MODEL_WIDTH = 100;
    public static final int MODEL_HEIGHT = 60;

    private double xInc;
    private double yInc;

    private final Circuit circuit = new Circuit();

    public static void main(String[] args)
    {
        new CircuitEditor().run();
    }

    @Override
    public void paint(Graphics g)
    {
        super.paint(g);

        xInc = getWidth() / (double) MODEL_WIDTH;
        yInc = getHeight() / (double) MODEL_HEIGHT;

        drawGrid((Graphics2D) g);
        drawComponents((Graphics2D) g);
    }

    private void drawComponents(Graphics2D g)
    {
        for ( ICircuitComponent c : circuit.getComponents() ) {
            renderComponent(c,g);
        }
    }

    private void renderComponent(ICircuitComponent c, Graphics2D g)
    {
        // draw bounding box
        drawRect( c.getBounds() , g );

        // draw ports
        int index = 0;            
        final List<Port> ports = c.getPorts();
        final Iterator<Port> it = ports.iterator();
        final Rec2 bounds = c.getBounds();

        final int yUpper = bounds.y1;
        final int yLower = bounds.y2;            
        final double inc = c.width() / (double) ports.size();
        for( double x = bounds.x1 ; x < bounds.x2 ; x+= inc , index++ ) 
        {
            if ( (index %2 ) == 0 ) {
                drawPort( x , yUpper , g );
            } else {
                drawPort( x , yLower , g );
            }
        }
    }

    private void drawPort(double modelX,double modelY,Graphics2D g) {

        final Point  p = modelToView( modelX , modelY );
        final int diameter = 6;
        final double diameterX = diameter*xInc;
        //        final double diameterY = diameter*yInc;        
        final int x = round( p.x - diameterX/2.0d );
        final int y = round( p.y - diameterX/2.0d );
        g.drawOval( x,y,round( diameterX ) ,round( diameterX) );
    }

    private static int round(double d) {
        return (int) Math.round(d);
    }

    private void drawRect(Rec2 r , Graphics2D g ) {
        Rec2 viewRect = modelToView( r );
        g.drawRect( viewRect.x1 , viewRect.y1 , viewRect.width() , viewRect.height() ); 
    }

    private void drawGrid(Graphics2D g)
    {
        g.setColor( Color.BLUE );
        for ( int x = 0 ; x < MODEL_WIDTH ; x++ ) 
        {
            for ( int y = 0 ; y < MODEL_HEIGHT ; y++ ) 
            {
                final Point p = modelToView( x, y);
                g.drawLine( p.x , p.y, p.x , p.y );
            }
        }
    }

    private Rec2 modelToView(Rec2 r) {
        final int x1 = (int) Math.round( r.x1 * xInc );
        final int y1 = (int) Math.round( r.y1 * yInc );

        final int x2 = (int) Math.round( r.x2* xInc );
        final int y2 = (int) Math.round( r.y2 * yInc );
        return new Rec2(x1,y1,x2,y2);
    }

    private Point modelToView(double x,double y) {
        final int x1 = (int) Math.round( x * xInc );
        final int y1 = (int) Math.round( y * yInc );
        return new Point(x1,y1);
    }

    private Point viewToModel(int x,int y) 
    {
        final int x1 = (int) Math.round( x / xInc );
        final int y1 = (int) Math.round( y / yInc );
        return new Point(x1,y1);
    }    

    public void run() {

        setPreferredSize(new Dimension(MODEL_WIDTH,MODEL_HEIGHT));

        addMouseListener( new MouseAdapter() 
        {
            public void mouseClicked(java.awt.event.MouseEvent e) 
            {};
        } );  

        final JFrame frame = new JFrame("CircuitEditor");

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
        frame.getContentPane().add( this , cnstrs );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.pack();
        frame.setVisible( true );        
    }
}
