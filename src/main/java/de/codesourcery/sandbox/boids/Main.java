package de.codesourcery.sandbox.boids;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.sandbox.boids.World.IBoidVisitor;
import de.codesourcery.sandbox.pathfinder.Rec2D;
import de.codesourcery.sandbox.pathfinder.Vec2d;

public class Main extends JFrame
{
    public static final boolean DEBUG = true;
    
    public static final double SEPARATION_RADIUS = 40;
    public static final double NEIGHTBOUR_RADIUS = 60;
    
    public static final double MAX_FORCE = 2;
    
    public static final double COHESION_WEIGHT = 0.05d;
    public static final double SEPARATION_WEIGHT = 2d;
    public static final double ALIGNMENT_WEIGHT = 0.08d;
    
    public static final double  MODEL_MAX = 1000;
    public static final int  TILE_COUNT = 50;
    public static final int POPULATION_SIZE = 150;
    
    public static final double MAX_SPEED = 3;    
    
    public static final int TICK_DELAY = 10;
    
    private static final Object WORLD_LOCK = new Object();
    private World world;
    
    private final Random rnd = new Random(System.currentTimeMillis() );
    
    private final MyPanel panel = new MyPanel();
    
    
    public static void main(String[] args) throws Exception
    {
        new Main().run();
    }
    
    public void run() throws Exception
    {
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        
        panel.setPreferredSize(new Dimension(800,600));
        
        getContentPane().setLayout( new GridBagLayout() );
        GridBagConstraints cnstrs = new GridBagConstraints();
        cnstrs.weightx=1.0;
        cnstrs.weighty=1.0;
        cnstrs.gridheight=GridBagConstraints.REMAINDER;
        cnstrs.gridwidth=GridBagConstraints.REMAINDER;
        cnstrs.fill = GridBagConstraints.BOTH;
        getContentPane().add( panel , cnstrs );
        pack();
        
        setVisible(true);
        mainLoop();
    }
    
    private void mainLoop() throws Exception 
    {
        synchronized( WORLD_LOCK ) {
            world = createWorld();
        }
        
        // main loop
        while( true ) 
        {
            synchronized( WORLD_LOCK ) {
                world = tick();
            }
            panel.repaint();
            if ( TICK_DELAY > 0 ) {
                Thread.sleep(TICK_DELAY);
            }
        }        
    }
    
    private World tick() 
    {
        final World newWorld = new World(MODEL_MAX,TILE_COUNT);
        
        Vec2d mousePosition = panel.getMouseLocation();
        final Boid mouseBoid;
        if ( mousePosition != null ) {
            mouseBoid = new Boid( mousePosition , Vec2d.ORIGIN , Vec2d.ORIGIN );
            world.add( mouseBoid );
        } else {
            mouseBoid=null;
        }
        
        final IBoidVisitor visitor2 = new IBoidVisitor() {
            
            @Override
            public void visit(Boid boid)
            {
                if ( boid == mouseBoid ) {
                    return;
                }
                
                final Vec2d pos0 = boid.getLocation();
                final Vec2d v0 = boid.getVelocity();
                
                Vec2d newAcceleration = flock(boid); 
                Vec2d newVelocity = v0.add( newAcceleration ).limit( MAX_SPEED );
                Vec2d newLocation = pos0.add( newVelocity ).wrapIfNecessary( MODEL_MAX );

                newWorld.add( new Boid( newLocation , newAcceleration , newVelocity ) );
            }
        };
        
        world.visitAllBoids(visitor2);
        return newWorld;
    }
    
    protected Vec2d flock(Boid boid)
    {
        final Vec2d pos = boid.getNeighbourRadiusCenter();
        final Rec2D rect = new Rec2D( pos.x - NEIGHTBOUR_RADIUS , 
                pos.y - NEIGHTBOUR_RADIUS , 
                pos.x + NEIGHTBOUR_RADIUS , 
                pos.y + NEIGHTBOUR_RADIUS);
        
        final NeighborAggregator visitor = new NeighborAggregator(boid);
        world.visitBoids( rect , visitor );
        
        Vec2d mean = Vec2d.ORIGIN;
        
        // cohesion
        mean = mean.add( steerTo( boid , visitor.getAverageLocation() ).multiply( COHESION_WEIGHT ) );
        
        // alignment
        mean = mean.add( visitor.getAverageVelocity().multiply( ALIGNMENT_WEIGHT ) );
        
        // separation
        mean = mean.add( visitor.getAverageSeparationHeading().multiply( SEPARATION_WEIGHT ) );
        
        return mean;
    }
    
    private Vec2d steerTo(Boid boid , Vec2d target) 
    {
        Vec2d desiredDirection = target.minus( boid.getLocation() );
        final double distance = desiredDirection.length();
        
        if ( distance > 0 ) 
        {
            desiredDirection = desiredDirection.normalize();
            if ( distance < 100 ) 
            {
                desiredDirection = desiredDirection.multiply( MAX_SPEED * ( distance/100.0) );
            } else {
                desiredDirection = desiredDirection.multiply( MAX_SPEED );
            }
            
            Vec2d steer = desiredDirection.minus( boid.getVelocity() );
            steer = steer.limit( MAX_FORCE );
            return steer;
        }
        return Vec2d.ORIGIN;
        
        /*
  steer_to: (target) ->
    desired = Vector.subtract(target, @location) # A vector pointing from the location to the target
    d = desired.magnitude()  # Distance from the target is the magnitude of the vector

    # If the distance is greater than 0, calc steering (otherwise return zero vector)
    if d > 0
      desired.normalize()

      # Two options for desired vector magnitude (1 -- based on distance, 2 -- maxspeed)
      if d < 100.0
        desired.multiply(MAX_SPEED*(d/100.0)) # This damping is somewhat arbitrary
      else
        desired.multiply(MAX_SPEED)

      # Steering = Desired minus Velocity
      steer = desired.subtract(@velocity)
      steer.limit(MAX_FORCE)  # Limit to maximum steering force
    else
      steer = new Vector(0,0)

    return steer         
         */
    }
    
    private static class NeighborAggregator implements IBoidVisitor {
        
        private final Boid boid;
        private int neighbourCount=0;
        private int separationNeighbourCount=0;
        
        private Vec2d locationSum = new Vec2d(0,0);
        private Vec2d velocitySum = new Vec2d(0,0);      
        private Vec2d separationSum = new Vec2d(0,0);           
        
        public NeighborAggregator(Boid b) {
            this.boid = b;
        }

        @Override
        public void visit(Boid otherBoid)
        {
            if ( boid == otherBoid ) {
                return;
            }
            
            final double distance = otherBoid.getLocation().minus( boid.getLocation() ).length();
            
            if ( distance > NEIGHTBOUR_RADIUS ) {
                return;
            }
            
            neighbourCount ++;
            
            locationSum = locationSum.add( otherBoid.getLocation() );
            velocitySum = velocitySum.add( otherBoid.getVelocity() );

            if ( distance != 0 && distance < SEPARATION_RADIUS ) {
                separationSum = separationSum.add( boid.getLocation().minus( otherBoid.getLocation() ).normalize().divide( distance ) );
                separationNeighbourCount++;
            }
        }
        
        // separation
        public Vec2d getAverageSeparationHeading() 
        {
            if ( separationNeighbourCount == 0 ) {
                return Vec2d.ORIGIN;                
            }
            return separationSum.divide( separationNeighbourCount );
        }        
        
        public Vec2d getAverageVelocity()  // alignment
        {
            if ( neighbourCount == 0 ) {
                return Vec2d.ORIGIN;                
            }
            return velocitySum.divide( neighbourCount );
        }
        
        public Vec2d getAverageLocation() // cohesion 
        {
            if ( neighbourCount == 0 ) {
                return Vec2d.ORIGIN;
            }
            return locationSum.divide( neighbourCount );
        }        
    }

    private World createWorld() 
    {
        World world = new World(MODEL_MAX , TILE_COUNT ); // 20x20 tiles
        
        for ( int i = 0 ; i < POPULATION_SIZE ; i++ ) 
        {
            final Boid boid = new Boid(createRandomPosition() , createRandomAcceleration(), createRandomVelocity());
            world.add( boid );
        }
        return world;
    }
    
    private Vec2d createRandomPosition() 
    {
        if ( 1 != 2 ) {
            return new Vec2d(MODEL_MAX/2,MODEL_MAX/2);
        }
        final double x = rnd.nextDouble()* MODEL_MAX;
        final double y = rnd.nextDouble()* MODEL_MAX;
        return new Vec2d(x,y);
    }
    
    private Vec2d createRandomAcceleration() {
        
        final double x = (rnd.nextDouble()-0.5)*MAX_FORCE;
        final double y = (rnd.nextDouble()-0.5)*MAX_FORCE;
        return new Vec2d(x,y);
    }
    
    private Vec2d createRandomVelocity() {
        final double x = (rnd.nextDouble()-0.5)*MAX_SPEED;
        final double y = (rnd.nextDouble()-0.5)*MAX_SPEED;
        return new Vec2d(x,y);
    }    
    
    protected final class MyPanel extends JPanel {
        
        private double xInc=1.0;
        private double yInc=1.0;
        
        private volatile Vec2d mousePosition;
        
        public MyPanel() 
        {
            addMouseMotionListener( new MouseMotionListener() {
                
                @Override
                public void mouseMoved(MouseEvent e)
                {
                    mousePosition = viewToModel( e.getX(), e.getY() );
                }
                
                @Override
                public void mouseDragged(MouseEvent e)
                {
                }
            });
        }
        
        public Vec2d getMouseLocation() {
            return mousePosition;
        }
        
        @Override
        public void paint(final Graphics g)
        {
            super.paint(g);
            
            xInc = getWidth() / MODEL_MAX;
            yInc = getHeight() / MODEL_MAX;
            
            synchronized( WORLD_LOCK ) 
            {
                if ( world == null ) {
                    return;
                }
                
                final IBoidVisitor visitor = new IBoidVisitor() {
                    
                    private int count = 0;
                    @Override
                    public void visit(Boid boid)
                    {
                        drawBoid( boid ,count == 0 ,g );
                        count++;
                    }
                };
                
                g.setColor( Color.BLACK );
                world.visitAllBoids( visitor );
            }
        }
        
        private void drawBoid(Boid boid, boolean firstBoid , Graphics g)
        {
            drawBoid(boid,firstBoid,Color.BLACK,false , g);
        }
        
        private void drawBoid(final Boid boid, boolean firstBoid , Color color , boolean fill , final Graphics g)
        {
            final double length=10;
            final double lengthHeading=length*3;
            
            // create vector perpendicular to heading
            Vec2d headingNormalized = boid.getVelocity().normalize();
            final Vec2d rotated = headingNormalized.rotate90DegreesCW();

            /*      heading
             *        /\
             *        / \
             *       /   \
             *      /     \
             *     /       \
             *    /         \
             * p1 +----+----+ p2
             *        center
             */
            
            final Vec2d center = boid.getLocation();
            final Vec2d heading = boid.getLocation().add( headingNormalized.multiply( lengthHeading ) );
            final Vec2d p1 = center.add( rotated.multiply(length) );
            final Vec2d p2 = center.add( rotated.multiply( -length ) );
            
            if ( DEBUG && firstBoid ) 
            {
                // draw neighbor radius
                g.setColor(Color.GREEN );
                drawCircle( boid.getNeighbourRadiusCenter() , NEIGHTBOUR_RADIUS , g );
                
                // draw separation radius
                g.setColor(Color.RED);
                drawCircle( boid.getNeighbourRadiusCenter() , SEPARATION_RADIUS , g );  

                // mark neighbors
                final IBoidVisitor visitor = new IBoidVisitor() {
                    
                    @Override
                    public void visit(Boid other)
                    {
                        if ( other != boid ) {
                            drawBoid( other , false , Color.PINK , true, g );
                        }
                    }
                };
                world.visitBoids( boid.getNeighbourRadiusCenter() , NEIGHTBOUR_RADIUS , visitor );
            }
            
            g.setColor( color );   
            drawPoly( fill , g , p1,heading,p2);
        }     
        
        private void drawPoly(boolean fill , Graphics g, Vec2d... points) 
        {
            final int x[] = new int[points.length];
            final int y[] = new int[points.length];
            
            for ( int i = 0 ; i < points.length ; i++ ) 
            {
                x[i] = (int) Math.round(points[i].x * xInc);
                y[i] = (int) Math.round(points[i].y * yInc);
                
            }
            
            if ( fill ) {
                g.fillPolygon( x , y , points.length );
            } else {
                g.drawPolygon( x , y , points.length );
            }
        }         
        
        @SuppressWarnings("unused")
        private void drawLine(Vec2d p1,Vec2d p2,Graphics g) {
            
            final int x1 = (int) Math.round(p1.x * xInc);
            final int y1 = (int) Math.round(p1.y * yInc);
            
            final int x2 = (int) Math.round(p2.x * xInc);
            final int y2 = (int) Math.round(p2.y * yInc);
            g.drawLine( x1,y1,x2,y2);
        }        
        
        private Vec2d viewToModel(int x,int y) {
            return new Vec2d( x / xInc , y / yInc );
        }
        
        private void drawCircle(Vec2d center, double boidNeightbourRadius, Graphics g)
        {
            final double x1 = (center.x - boidNeightbourRadius)*xInc;
            final double y1 = (center.y - boidNeightbourRadius)*yInc;
            
            final double x2 = (center.x + boidNeightbourRadius)*xInc;
            final double y2 = (center.y + boidNeightbourRadius)*yInc;            
            
            g.fillOval( round(x1) , round(y1) , round(x2-x1) , round(y2-y1) ); 
        }

    }
    
    private static final int round(double d) {
        return (int) Math.round(d);
    }
}
