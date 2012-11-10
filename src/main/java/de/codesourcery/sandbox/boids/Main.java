package de.codesourcery.sandbox.boids;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.sandbox.boids.World.IBoidVisitor;
import de.codesourcery.sandbox.pathfinder.Vec2d;
import de.codesourcery.sandbox.pathfinder.Vec2dMutable;

public class Main extends JFrame
{
	protected static final boolean DEBUG = false;

	protected static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();

	protected static final AtomicLong TICK_COUNTER = new AtomicLong(0);    
	protected static final AtomicLong FRAME_COUNTER = new AtomicLong(0);

    protected static final boolean DEBUG_PERFORMANCE = true;

	protected static final double MAX_FORCE = 5;
	protected static final double MAX_SPEED = 10;     

	protected static final double COHESION_WEIGHT = 0.33d;
	protected static final double SEPARATION_WEIGHT = 0.4d;
	protected static final double ALIGNMENT_WEIGHT = 0.33d;
	protected static final double BORDER_FORCE_WEIGHT = 0.5d;

	protected static final double  MODEL_MAX = 3000;

	protected static final double SEPARATION_RADIUS = 20;
	protected static final double NEIGHBOUR_RADIUS = 100;
	protected static final double BORDER_RADIUS = MODEL_MAX*0.3;    

    protected static final int POPULATION_SIZE = 11000;

	protected static final Object WORLD_LOCK = new Object();

	private World world;

	private final Random rnd = new Random( 0xdeadbeef );
	private final MyPanel panel = new MyPanel();
	private final ExecutorService threadPool;
	private final AtomicBoolean mayRender = new AtomicBoolean(true);
	private final ScheduledThreadPoolExecutor vsyncThread;

	public Main() 
	{
		System.out.println("Using "+THREAD_COUNT+" CPUs.");
        System.setProperty( "sun.java2d.opengl" , "true" );

		BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>( 100 );

		final ThreadFactory threadFactory = new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r)
			{
				final Thread t= new Thread(r);
				t.setDaemon( true );
				return t;
			}
		};
		threadPool = new ThreadPoolExecutor( THREAD_COUNT , THREAD_COUNT , 1 , TimeUnit.MINUTES , queue,threadFactory, new CallerRunsPolicy() );

		final Runnable r = new Runnable() 
		{
			private int dropCount;
			private int frameCount;
			private int previouslyDroppedFrame=-1;
			@Override
			public void run()
			{
				frameCount++;
				if ( ! mayRender.compareAndSet( false , true ) ) 
				{
					dropCount++;					
					if ( previouslyDroppedFrame != frameCount-1 ) 
					{
						System.out.println("*** Frames dropped: "+dropCount);            		
					}
					previouslyDroppedFrame=frameCount;
				}
			}
		};

		vsyncThread = new ScheduledThreadPoolExecutor(1); 
		vsyncThread.scheduleAtFixedRate( r , 0 , 40 , TimeUnit.MILLISECONDS );
	}

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
			if ( mayRender.compareAndSet(true,false ) ) 
			{            
				synchronized( WORLD_LOCK ) 
				{
					long time = -System.currentTimeMillis();
					world = tick();
					if ( DEBUG_PERFORMANCE ) 
					{
						time += System.currentTimeMillis();
						if ( ( TICK_COUNTER.incrementAndGet() % 10 ) == 0 ) {
							System.out.println("Calculation: "+time);
						}
					}
				}
				panel.repaint();
			}
		}        
	}

	private World tick() throws InterruptedException 
	{
        final World newWorld = new World();

		// fork one thread for each input list
        final CountDownLatch latch = new CountDownLatch( THREAD_COUNT );
        
		for ( final ArrayList<Boid> inputList : getBoidsPerThread() ) 
		{
			threadPool.submit( new Runnable() 
			{
				public void run() 
				{
					try 
					{
						for ( Boid boid : inputList ) 
						{
							final Vec2dMutable newAcceleration = flock(boid); 
							
							final Vec2d newVelocity = boid.getVelocity().plus( newAcceleration ).limit( MAX_SPEED );
							final Vec2d newLocation = boid.getLocation().plus( newVelocity ).wrapIfNecessary( MODEL_MAX );
							
							newWorld.add( new Boid( newLocation , new Vec2d( newAcceleration ) , newVelocity ) );                 	
						}
					} finally {
						latch.countDown();
					}					
				};
			} );
		}

		// wait for threads to finish
		latch.await();
		return newWorld;
	}

	// divide boids into separate lists, each being processed by a different thread      
	private ArrayList<Boid>[] getBoidsPerThread()
	{
		@SuppressWarnings("unchecked")
		final ArrayList<Boid>[] toProcess = new ArrayList[THREAD_COUNT ];

		for ( int i = 0 ;i < THREAD_COUNT ; i++ ) {
			toProcess[i] = new ArrayList<Boid>();
		}

		final int boidsPerThread = world.getPopulation() / THREAD_COUNT;
		int index = 0;
		int i = 0;
		for ( Boid b : world.getAllBoids() ) 
		{
			toProcess[index].add( b );
			i++;
			if ( i > boidsPerThread ) {
				i = 0;
				if ( index < THREAD_COUNT ) {
					index++;
				}
			}
		}
		return toProcess;
	}

	protected Vec2dMutable flock(Boid boid)
	{
		final NeighborAggregator visitor =new NeighborAggregator( boid );
		boid.visitNeighbors(world , NEIGHBOUR_RADIUS , visitor );

		// cohesion
		Vec2dMutable cohesionVec = steerTo( boid , visitor.getAverageLocation() );

		// alignment
		Vec2dMutable alignmentVec = visitor.getAverageVelocity();

		// separation
		Vec2dMutable separationVec = visitor.getAverageSeparationHeading();

		// border force
		final Vec2d pos = boid.getLocation();

		Vec2dMutable borderForce = new Vec2dMutable();
		if ( pos.x < BORDER_RADIUS ) 
		{
			final double delta = (BORDER_RADIUS-pos.x) / BORDER_RADIUS;
			borderForce.x = delta*delta;
		} else if ( pos.x > ( MODEL_MAX - BORDER_RADIUS ) ) 
		{
			final double delta = (BORDER_RADIUS -( MODEL_MAX - pos.x )) / BORDER_RADIUS;
			borderForce.x = -(delta*delta);
		}

		if ( pos.y < BORDER_RADIUS ) 
		{
			final double delta = (BORDER_RADIUS-pos.y) / BORDER_RADIUS;
			borderForce.y = delta*delta;
		} else if ( pos.y > ( MODEL_MAX - BORDER_RADIUS ) ) 
		{
			final double delta = (BORDER_RADIUS -( MODEL_MAX - pos.y )) / BORDER_RADIUS;
			borderForce.y = -(delta*delta);
		}        

		Vec2dMutable mean = new Vec2dMutable();
		
		mean.plus( cohesionVec.normalize().multiply( COHESION_WEIGHT ) );        
		mean.plus( alignmentVec.normalize().multiply( ALIGNMENT_WEIGHT ) );        
		mean.plus( separationVec.normalize().multiply( SEPARATION_WEIGHT ) );
		mean.plus( borderForce.multiply( BORDER_FORCE_WEIGHT ) );

		return mean;
	}

	private Vec2dMutable steerTo(Boid boid , Vec2dMutable target) 
	{
		Vec2dMutable desiredDirection = target.minus( boid.getLocation() );
		final double distance = desiredDirection.length();

		if ( distance > 0 ) 
		{
			desiredDirection.normalize();
			if ( distance < 100 ) 
			{
				desiredDirection.multiply( MAX_SPEED * ( distance/100.0) );
			} else {
				desiredDirection.multiply( MAX_SPEED );
			}

			desiredDirection.minus( boid.getVelocity() );
			desiredDirection.limit( MAX_FORCE );
			return desiredDirection;
		}
		return new Vec2dMutable();
	}

	public static class NeighborAggregator implements IBoidVisitor {

		private final Boid boid;

		private final Vec2dMutable locationSum = new Vec2dMutable();
		private final Vec2dMutable velocitySum = new Vec2dMutable();
		private final Vec2dMutable separationSum = new Vec2dMutable();

		private int neighbourCount=0;
		private int separationNeighbourCount=0;
		
		public NeighborAggregator(Boid b) {
			this.boid = b;
		}

		public int getNeighbourCount()
		{
			return neighbourCount;
		}

		@Override
		public void visit(Boid otherBoid)
		{
			if ( boid == otherBoid ) {
				return;
			}

			final double distance = otherBoid.getLocation().distanceTo( boid.getNeighbourCenter() );

			if ( distance > NEIGHBOUR_RADIUS ) {
				return;
			}

			neighbourCount ++;

			locationSum.plus( otherBoid.getLocation() );
			velocitySum.plus( otherBoid.getVelocity() );

			if ( distance > 0 && distance < SEPARATION_RADIUS ) 
			{
				final Vec2dMutable tmp = new Vec2dMutable( boid.getNeighbourCenter() );
				tmp.minus( otherBoid.getLocation() ).normalize().divide( distance/2 ); 
				separationSum.plus( tmp );
				separationNeighbourCount++;
			}
		}

		// separation
		public Vec2dMutable getAverageSeparationHeading() 
		{
			if ( separationNeighbourCount == 0 ) {
				return new Vec2dMutable(0,0);
			}
			separationSum.divide( separationNeighbourCount );
			return separationSum;
		}        

		public Vec2dMutable getAverageVelocity()  // alignment
		{
			if ( neighbourCount == 0 ) {
				return new Vec2dMutable(0,0);				
			}
			velocitySum.divide( neighbourCount );
			return velocitySum;
		}

		public Vec2dMutable getAverageLocation() // cohesion 
		{
			if ( neighbourCount == 0 ) {
				return new Vec2dMutable(0,0);					
			}
			locationSum.divide( neighbourCount );
			return locationSum;
		}        
	}

	private World createWorld() 
	{
        World world = new World(); // 20x20 tiles

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
			setBackground(Color.WHITE);
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

            final Graphics2D graphics = (Graphics2D) g;
            
            final IBoidVisitor visitor = new IBoidVisitor() {

                private int count = 0;
                @Override
                public void visit(Boid boid)
                {
                    drawBoid( boid ,count == 0 ,graphics );
                    count++;
                }
            };
            
            g.setColor( Color.BLACK );
            synchronized( WORLD_LOCK ) 
            {
                if ( world == null ) {
                    return;
                }
                
				long time = -System.currentTimeMillis();
				world.visitAllBoids( visitor );
				if ( DEBUG_PERFORMANCE ) 
				{
					time += System.currentTimeMillis();
					if ( ( FRAME_COUNTER.incrementAndGet() % 60 ) == 0 ) {
						System.out.println("Rendering: "+time+" ms");
					}
				}
			}
		}

        private void drawBoid(Boid boid, boolean firstBoid , Graphics2D g)
		{
			drawBoid(boid,firstBoid,Color.BLUE,true , g);
		}

        private void drawBoid(final Boid boid, boolean isDebugBoid , Color color , boolean fill , final Graphics2D g)
		{
			if ( DEBUG && isDebugBoid ) 
			{
				// draw neighbor radius
				g.setColor(Color.GREEN );
				drawCircle( boid.getNeighbourCenter() , NEIGHBOUR_RADIUS , g );

				// draw separation radius
				g.setColor(Color.RED);
				drawCircle( boid.getNeighbourCenter() , SEPARATION_RADIUS , g );  

				// mark neighbors
				final NeighborAggregator visitor = new NeighborAggregator(boid) {

					@Override
					public void visit(Boid other)
					{
						if ( other != boid ) {
							super.visit(other);  

							final double distance = other.getLocation().minus( boid.getNeighbourCenter() ).length();

							if ( distance > NEIGHBOUR_RADIUS ) {
								return;
							}                            
							drawBoid( other , false , Color.PINK , true, g );
						}
					}
				};
				boid.visitNeighbors( world , NEIGHBOUR_RADIUS , visitor );

				// cohesion
				Vec2dMutable cohesionVec = steerTo( boid , visitor.getAverageLocation() );

				g.setColor(Color.CYAN);
				drawVec( boid.getLocation() , boid.getLocation().plus( cohesionVec ) , g );

				// alignment
				Vec2dMutable alignmentVec = visitor.getAverageVelocity();
				g.setColor(Color.BLUE);
				drawVec( boid.getLocation() , boid.getLocation().plus( alignmentVec ) , g );

				// separation
				Vec2dMutable separationVec = visitor.getAverageSeparationHeading();
				g.setColor(Color.MAGENTA);
				drawVec( boid.getLocation() , boid.getLocation().plus( separationVec ) , g );                
			}

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
			
			final Vec2d heading = boid.getLocation().plus( headingNormalized.multiply( lengthHeading ) );
			final Vec2d p1 = center.plus( rotated.multiply(length) );
			final Vec2d p2 = center.plus( rotated.multiply( -length ) );

            g.setColor( color );
			drawPoly( fill , g , p1,heading,p2);
		}     

		private void drawVec(Vec2d src, Vec2d dst, Graphics g) {

			Vec2d direction = dst.minus( src ).normalize();

			Vec2d arrowEnd = src.plus( direction.multiply( 55 ) );
			Vec2d arrowStart = src.plus( direction.multiply( 45 ) );
			drawLine( src , arrowEnd , g );

			// draw arrow head
			Vec2d headBase = direction.multiply(10);
			Vec2d p1 = arrowStart.plus( headBase.rotate90DegreesCCW() );
			Vec2d p2 = arrowStart.minus( headBase.rotate90DegreesCCW() );
			drawPoly( true , g , p1 , arrowEnd , p2 );
		}

        private void drawPoly(boolean fill , Graphics g, Vec2d p1,Vec2d p2,Vec2d p3) 
		{
            final int x[] = new int[3];
            final int y[] = new int[3];

            x[0] = (int) Math.round(p1.x * xInc);
            y[0] = (int) Math.round(p1.y * yInc);

            x[1] = (int) Math.round(p2.x * xInc);
            y[1] = (int) Math.round(p2.y * yInc);
            
            x[2] = (int) Math.round(p3.x * xInc);
            y[2] = (int) Math.round(p3.y * yInc);            

			if ( fill ) {
                g.fillPolygon( x , y , 3 );
            } else {
                g.drawPolygon( x , y , 3 );
            }
        }         

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