package de.codesourcery.sandbox.genetic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class Main extends JFrame {

	private static final int MODEL_WIDTH=65535;
	private static final int MODEL_HEIGHT=65535;

	public static final int MIN_RADIUS = 1000;	
	public static final int MAX_RADIUS = 10000;
	public static final int CIRCLE_COUNT = 10;

	public static final boolean USE_GRAY_CODE = false;
	public static final int CHROMOSOME_BITS = 16*3;
	public static final double CROSSOVER_RATE = 0.7;
	public static final double MUTATION_RATE = 0.7;
	
	public static final int POPULATION_SIZE=5000;
	public static final int REPAINT_INTERVAL = 10;

	public static final int TOP_TEN=1;
	public static final int ELITIST_COUNT =  0; // (int) (POPULATION_SIZE*0.05);

	public static final int THREAD_COUNT = 4;

	private final Random rnd = new Random(System.nanoTime());
//		private final Random rnd = new XORShiftRandom();

	private final ExecutorService threadPool;

	public class XORShiftRandom extends Random {
		private long seed = System.nanoTime();

		public XORShiftRandom() {
		}
		protected int next(int nbits) {
			long x = this.seed;
			x ^= (x << 21);
			x ^= (x >>> 35);
			x ^= (x << 4);
			this.seed = x;
			x &= ((1L << nbits) -1);
			return (int) x;
		}
	}

	private final List<Circle> circles = new ArrayList<>();

	private final AtomicInteger currentGenerationNumber=new AtomicInteger(1);
	private volatile Population population;

	public Main() 
	{
		final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>( 100 );

		final ThreadFactory factory = new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) 
			{
				final Thread t = new Thread(r);
				t.setDaemon( true );
				return t;
			}
		};
		threadPool = new ThreadPoolExecutor(THREAD_COUNT,THREAD_COUNT, 1 , TimeUnit.MINUTES , 
				queue, factory , new CallerRunsPolicy() ); 
	}

	protected final class Population 
	{
		private final List<Chromosome> chromosomes = new ArrayList<>(POPULATION_SIZE+4);

		public void add(Chromosome c) {
			this.chromosomes.add( c );
		}

		public int size() {
			return chromosomes.size();
		}

		public List<Chromosome> getNFittest(int n) 
		{
			final List<Chromosome> result = new ArrayList<>(n);
			for ( Chromosome c : chromosomes ) 
			{
				final int len = result.size();
				if ( len < n ) {
					result.add( c );
					continue;
				}

				final double f = getFitness(c );

				for ( int i = 0 ; i < len ; i++)
				{
					if ( f > getFitness( result.get(i) ) ) 
					{
						result.add( i , c );
						if ((len+1) > n ) {
							result.remove( result.size() -1 );
						}
						break;
					}
				}
			}
			return result;
		}
	}

	protected double getFitness(Chromosome c) {
		if (c.fitness == -1) {
			c.fitness = fitness( c );
		}
		return c.fitness;
	}

	protected final class Chromosome 
	{
		public long value = 0;
		public double fitness=-1;

		public Chromosome(long value) {
			this.value = value;
		}

		public Chromosome createCopy() {
			final Chromosome  result = new Chromosome(this.value);
			result.fitness = -1;
			return result;
		}

		public Chromosome(Circle circle) {
			this( circle.encode() );
		}

		public Circle getcircle() {
			return Circle.decode( value );
		}
	}

	public void nextGeneration() throws InterruptedException {

		if ( population == null ) {
			population = createRandomPopulation( POPULATION_SIZE );
			return;
		}

		population = darwin();
		currentGenerationNumber.incrementAndGet();
	}

	private Population darwin() throws InterruptedException 
	{
		final Population result = new Population();
		final double totalFitness = getTotalFitnessSum( population );

		 result.chromosomes.addAll( population.getNFittest( ELITIST_COUNT ) );

		final CountDownLatch latch = new CountDownLatch( THREAD_COUNT );
		final int batchSize = (POPULATION_SIZE-ELITIST_COUNT) / THREAD_COUNT;

		for ( int i = 0 ; i < THREAD_COUNT ; i++ ) 
		{
			final Runnable r = new Runnable() 
			{
				public void run() 
				{
					try 
					{
						final List<Chromosome> tmp = new ArrayList<>(batchSize);
						for ( int j = 0 ; j < batchSize ; j += 2 ) 
						{
							Chromosome c1 = pickChromosome(totalFitness).createCopy();
							Chromosome c2 = pickChromosome(totalFitness).createCopy();

							crossOver(c1,c2);

							mutate( c1 );
							mutate( c2 );

							tmp.add( c1 );
							tmp.add( c2 );
						}

						synchronized( result ) {
							result.chromosomes.addAll( tmp );
						}

					} finally {
						latch.countDown();
					}
				}
			};

			threadPool.submit( r );
		}

		latch.await();

		return result;
	}

	private void crossOver(Chromosome c1,Chromosome c2) 
	{
		final boolean doCrossOver = rnd.nextDouble() <= CROSSOVER_RATE; 
		if ( doCrossOver ) 
		{
			final int crossOverPoint = rnd.nextInt( CHROMOSOME_BITS );

			long mask1 = (1 << crossOverPoint) - 1;
			long mask2 = ~mask1;

			c1.value = (c1.value & mask1) | ( c2.value & mask2 );
			c2.value = (c2.value & mask1) | ( c1.value & mask2 );
		}
	}

	private void mutate(Chromosome c) 
	{
		final int len = CHROMOSOME_BITS;
		long value = c.value;
		for ( int i = 0 ; i < len ; i++ ) 
		{
			final boolean doMutate = rnd.nextDouble() <= MUTATION_RATE;
			if ( doMutate ) {
				final int mask = 1 << i;
				if ( ( value & mask ) != 0 ) { // bit set, clear it
					value &= ~mask;
				} else { // bit not set, set it
					value |= mask;
				}
			}
		}
		c.value = value;
	}	

	private Chromosome pickChromosome(double totalFitness) 
	{
		final double index = (rnd.nextInt(1001)/1000.0d)*totalFitness;
		double current = 0.0;
		for ( Chromosome c : population.chromosomes ) 
		{
			current += getFitness(c);

			if ( current >= index ) {
				return c;
			}
		}
		throw new RuntimeException("Internal error,index: "+index+", totalFitness: "+totalFitness);
	}

	public double getTotalFitnessSum(Population population) 
	{
		double result = 0;
		for ( Chromosome c : population.chromosomes ) {
			result += getFitness(c);
		}
		return result;
	}	

	private final Population createRandomPopulation(int size) {

		System.out.print("Creating random population...");
		Population result = new Population();
		for ( int i =0 ; i < size ; i++ ) {
			result.add( new Chromosome( createRandomCircle(1, MAX_RADIUS*3 ,true) ) );
		}
		System.out.println("done!");
		return result;
	}

	protected static final class Circle {

		public final int x;
		public final int y;
		public final int radius;

		public Circle(int x,int y,int radius) {
			this.x = x;
			this.y = y;
			this.radius = radius;
		}

		public boolean overlaps(Circle other) {
			int dx = this.x - other.x;
			int dy = this.y- other.y;
			final double squaredDistance = dx*dx +dy*dy;
			return squaredDistance <= this.radius*this.radius || squaredDistance <= other.radius*other.radius;
		}

		@Override
		public String toString() {
			return "X="+x+" / Y = "+y+" / radius = "+radius;
		}

		public boolean isWithinModelSpace() 
		{
			final int x1 = x-radius;
			final int y1 = y-radius;
			final int x2 = x+radius;
			final int y2 = y+radius;

			return x1 > 0 && x2 >0 && x1 <= MODEL_WIDTH && x2 <= MODEL_WIDTH && y1 <= MODEL_HEIGHT && y2 <= MODEL_HEIGHT && y1 > 0 && y2 > 0;
		}

		public long encode() 
		{
			if ( USE_GRAY_CODE ) {
				long result = sixteenBitsToGrayCode( x & 0xffff );
				result = (result << 16 ) | sixteenBitsToGrayCode(y & 0xffff);
				result = (result << 16 ) | sixteenBitsToGrayCode(radius & 0xffff);				
				return result;
			} 
			long result = x & 0xffff;
			result = (result << 16 ) | (y & 0xffff);
			result = (result << 16 ) | (radius & 0xffff);			
			return result;				
		}

		public static Circle decode(long s) 
		{
			if ( USE_GRAY_CODE ) 
			{
				int r = sixteenBitsFromGrayCode((int) (s & 0xffff));				
				s = s >> 16;
				int y = sixteenBitsFromGrayCode((int) (s & 0xffff));
				s = s >> 16;
				int x = sixteenBitsFromGrayCode((int) (s & 0xffff));
				return new Circle(x,y,r);
			}
			int r = (int) (s & 0xffff);			
			s = s >> 16;
			int y = (int) (s & 0xffff);
			s = s >> 16;
			int x = (int) (s & 0xffff);
			return new Circle(x,y,r);			
		}
	}

	protected final class MyPanel extends JPanel {

		private double xInc;
		private double yInc;

		@Override
		public void paint(Graphics g) 
		{
			super.paint(g);

			xInc = getWidth() / (double) MODEL_WIDTH;
			yInc = getHeight() / (double) MODEL_HEIGHT;

			// draw fittest
			List<Chromosome> fittest = new ArrayList<>();
			double totalFitness = 0;
			Population tmp = population;
			if ( tmp != null ) 
			{
				fittest = tmp.getNFittest(TOP_TEN);
				totalFitness = getTotalFitnessSum( tmp );
			}

			g.setColor(Color.RED);
			for( Chromosome chromosome : fittest ) 
			{
				drawcircle( chromosome.getcircle() , true , g );
			} 
			g.setColor(Color.BLACK);
			g.drawString("Generation: "+currentGenerationNumber , 15 ,15 );
			g.drawString("Total fitness: "+totalFitness, 15 ,25 );

			// draw circles
			g.setColor(Color.BLACK);
			drawcircles(g,false);

			Toolkit.getDefaultToolkit().sync(); // required on Linux, otherwise animation is choppy
		}

		private void drawcircles(Graphics g,boolean fill) 
		{
			for ( Circle circle : circles ) 
			{
				drawcircle( circle , fill , g );
			}
		}

		private void drawcircle(Circle circle, boolean fill , Graphics g) 
		{
			final double xRadius = circle.radius * xInc;
			final double yRadius = circle.radius * yInc;			
			final double minX=circle.x * xInc - xRadius;
			final double minY=circle.y * yInc - yRadius;
			if ( fill ) {
				g.fillOval( round(minX) , round(minY) , round(xRadius) , round(yRadius) );
			} else {
				g.drawOval( round(minX) , round(minY) , round(xRadius) , round(yRadius) );
			}
		}

		private int round(double value) {
			return (int) Math.round(value);
		}
	}

	private boolean overlaps(Circle d) {

		for ( Circle existing : circles ) 
		{
			if ( existing != d && existing.overlaps(d)) {
				return true;
			}
		}
		return false;
	}

	public double fitness(Chromosome c) 
	{
		final Circle d = c.getcircle();
		if ( ! d.isWithinModelSpace() ) 
		{
			return 0.0;
		}

		if ( overlaps( d ) ) {
			return 100;
		}

		return 100+d.radius;
	}

	private Circle createRandomCircle(int minRadius , int maxRadius,boolean checkBounds ) {
		do 
		{
			if ( checkBounds ) {
				int x = maxRadius+rnd.nextInt( MODEL_WIDTH - maxRadius );
				int y = maxRadius+rnd.nextInt( MODEL_HEIGHT - maxRadius  );
				int r = minRadius+rnd.nextInt( maxRadius-minRadius);
				final Circle d = new Circle( x , y , r );
				if ( ! overlaps(d) ) {
					return d;
				}
			} else {
				int x = rnd.nextInt( MODEL_WIDTH );
				int y = rnd.nextInt( MODEL_HEIGHT );
				int r = minRadius+rnd.nextInt( maxRadius-minRadius);
				return new Circle( x , y , r );
			}
		} while ( true );
	}

	public static void main(String[] args) throws Exception 
	{
		new Main().run();
	}	

	public void run() throws InterruptedException, InvocationTargetException {

		// create random circles
		for ( int i = 0 ;i < CIRCLE_COUNT ; i++ ) 
		{
			circles.add(createRandomCircle(MIN_RADIUS,MAX_RADIUS,true));
		}

		final MyPanel panel = new MyPanel();
		panel.setPreferredSize(new Dimension(800,600));

		getContentPane().setLayout( new GridBagLayout() );

		final GridBagConstraints cnstrs = new GridBagConstraints();
		cnstrs.gridheight=GridBagConstraints.REMAINDER;
		cnstrs.gridwidth=GridBagConstraints.REMAINDER;
		cnstrs.weightx = 1.0;
		cnstrs.weighty = 1.0;
		cnstrs.fill = GridBagConstraints.BOTH;

		getContentPane().add( panel , cnstrs );

		final Object LOCK = new Object();
		final AtomicBoolean stop = new AtomicBoolean(true);
		
		addKeyListener( new KeyAdapter() {
			
			public void keyTyped(java.awt.event.KeyEvent e) {
				if ( e.getKeyChar() == ' ' ) {
					synchronized (LOCK) {
						stop.set( ! stop.get() );
						LOCK.notifyAll();
					}
				}
			};
		} );
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible( true );

		while( true ) 
		{
			nextGeneration();

			final boolean render = (currentGenerationNumber.get() % REPAINT_INTERVAL ) == 0;

			if ( render ) {
				SwingUtilities.invokeAndWait( new Runnable() {

					@Override
					public void run() {
						panel.repaint();
					}
				} );
			}
			
			synchronized( LOCK ) {
				if ( stop.get() ) 
				{
					do {
						System.out.println("Stopped.");
						LOCK.wait();
					} while ( stop.get() );
					population = null;
					rnd.setSeed( System.nanoTime() );
					currentGenerationNumber.set( 1 );
					System.out.println("Restarting.");
				}
			}
		}
	}

	public static final int sixteenBitsToGrayCode(int input) {

		int result = nibbleToGrayCode( input & 0b1111 ); // 4 bit
		input = input >> 4;
		result = result <<4;

		result = result | nibbleToGrayCode( input & 0b1111 ); // 8 bit
		input = input >> 4;
		result = result << 4;		

		result = result | nibbleToGrayCode( input & 0b1111 ); // 12 bit
		input = input >> 4;
		result = result << 4;	

		result = result | nibbleToGrayCode( input & 0b1111 ); // 16 bit
		return result;
	}

	public static final int sixteenBitsFromGrayCode(int input) {

		int result = grayCodeToNibble( input & 0b1111 ); // 4 bit
		input = input >> 4;
		result = result << 4;

		result = result | grayCodeToNibble( input & 0b1111 ); // 8 bit
		input = input >> 4;
		result = result << 4;		

		result = result | grayCodeToNibble( input & 0b1111 ); // 12 bit
		input = input >> 4;
		result = result << 4;	

		result = result | grayCodeToNibble( input & 0b1111 ); // 16 bit

		return result;
	}

	protected static final int nibbleToGrayCode(int input) 
	{
		switch( input ) 
		{
			case 0:
				return 0b0000;
			case 1:
				return 0b0001;
			case 2:
				return 0b0011;
			case 3:
				return 0b0010;
			case 4:
				return 0b0110;
			case 5:
				return 0b0111;
			case 6:
				return 0b0101;
			case 7:
				return 0b0100;
			case 8:
				return 0b1100;
			case 9:
				return 0b1101;
			case 10:
				return 0b1111;
			case 11:
				return 0b1110;
			case 12:
				return 0b1010;
			case 13:
				return 0b1011;
			case 14:
				return 0b1001;
			case 15:
				return 0b1000;
			default:
				throw new RuntimeException("Illegal argument: "+input);
		}
	}

	protected static final int grayCodeToNibble(int input) 
	{
		switch(input) 
		{
			case 0b0000:
				return 0;
			case 0b0001:
				return 1;
			case 0b0011:
				return 2;
			case 0b0010:
				return 3;
			case 0b0110:
				return 4;
			case 0b0111:
				return 5;
			case 0b0101:
				return 6;
			case 0b0100:
				return 7;
			case 0b1100:
				return 8;
			case 0b1101:
				return 9;
			case 0b1111:
				return 10;
			case 0b1110:
				return 11;
			case 0b1010:
				return 12;
			case 0b1011:
				return 13;
			case 0b1001:
				return 14;
			case 0b1000:
				return 15;
			default:
				throw new RuntimeException("Illegal argument: "+input);
		}
	}	
}