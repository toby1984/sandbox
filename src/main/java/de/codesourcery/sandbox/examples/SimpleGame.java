package de.codesourcery.sandbox.examples;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import de.codesourcery.sandbox.pathfinder.Rec2d;

/**
 * Crude version of a once famous game.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class SimpleGame {

	private final Random r = new Random(System.currentTimeMillis());

	private static final double MODEL_WIDTH = 4000;
	private static final double MODEL_HEIGHT = 3000;

	// relative UI sizes
	private static final double BALL_DIAMETER = MODEL_WIDTH * 0.05d;
	private static final double BALL_RADIUS = BALL_DIAMETER / 2.0d;

	private static final double BAT_HEIGHT = MODEL_HEIGHT * 0.15d;
	private static final double BAT_WIDTH = MODEL_WIDTH * 0.03d;
	private static final double BAT_MOVE = BAT_HEIGHT*0.1d;

	private static final double BALL_SPEED = MODEL_WIDTH*0.004;

	private final GamePanel panel = new GamePanel();	
	private final MyKeyListener keylistener = new MyKeyListener();
	
	// flag to avoid repeatedly triggering bat<->ball collisions
	// while the ball overlaps with the bat
	private boolean inCollision = false;	
	
	private final Ball ball = new Ball();
	
	private Player leftPlayer;
	private Player rightPlayer;

	public static void main(String[] args) throws InterruptedException {
		new SimpleGame().run();
	}

	private void run() throws InterruptedException {

		leftPlayer = new Player("Player 1" , createBat( MODEL_WIDTH*0.1 ) );
		rightPlayer = new Player("Player 2" , createBat( MODEL_WIDTH*0.9 ) );

		final JFrame frame = new JFrame("Pong!");

		frame.addKeyListener( keylistener );

		frame.getContentPane().setLayout( new GridBagLayout() );
		final GridBagConstraints cnstrs = new GridBagConstraints();
		cnstrs.fill = GridBagConstraints.BOTH;
		cnstrs.gridx = GridBagConstraints.REMAINDER;
		cnstrs.gridy = GridBagConstraints.REMAINDER;
		cnstrs.weightx = 1.0;
		cnstrs.weighty = 1.0;

		frame.getContentPane().add( panel , cnstrs );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		frame.setPreferredSize(new Dimension(600,300));        
		frame.pack();
		frame.setVisible( true );
		
		new MyTimerThread().start();
	}
	
	public class MyTimerThread extends Thread {
		
		public MyTimerThread() {
			setDaemon(true);
		}
		
		public void run() 
		{
			while(true) 
			{
				gameLoop();
				try 
				{
					java.lang.Thread.sleep(30);
				} catch(Exception e) {
					e.printStackTrace();
					// ok
				}
			}
		}
	}

	private void gameLoop() 
	{
		keylistener.processUserInput();
		handleCollisions();
		ball.move();
		try 
		{
			SwingUtilities.invokeAndWait( new Runnable() {
				public void run() {
					panel.repaint();
				}
			} );
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		}
	}	
	
	protected class Player 
	{
		private final String name;
		
		private volatile int score = 0;

		private final Bat bat;

		private Player(String name , Bat bat) {
			this.name = name;
			this.bat = bat;
		}

		public void incScore() {
			score++;
		}

		public void reset() {
			this.score = 0;
		}
	}

	protected class Bat {

		private volatile double x; // left upper corner X (MODEL coordinates)
		private volatile double y; // left upper corner Y (MODEL coordinates)

		private final double width; // bat width (MODEL coordinates)
		private final double height; // bat height (MODEL coordinates)

		public Bat(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
		
		public double centerX() {
			return x + (width/2.0);
		}
		
		public Rec2d getBoundingBox() 
		{
			return new Rec2d( x , y , (x+width), (y+height) );
		}

		public void moveUp() {
			double newY = y - BAT_MOVE;
			if ( newY  > 0 ) {
				y = newY;
			}
		}

		public void moveDown() {
			double newY = y + BAT_MOVE;
			if ( ( newY+height) <=MODEL_HEIGHT ) {
				y = newY;
			}			
		}		
	}

	protected class Ball 
	{
		private volatile double x;
		private volatile double y;

		private volatile double xMovement;
		private volatile double yMovement;

		private final double diameter=BALL_DIAMETER;

		public Ball() 
		{
			placeRandomly();
		}

		public void placeRandomly() 
		{
			inCollision = false;
			
			this.x = MODEL_WIDTH / 2.0;
			this.y = MODEL_HEIGHT / 2.0;

			this.xMovement = r.nextDouble() >= 0.5d ? BALL_SPEED : -BALL_SPEED; 
			this.yMovement = r.nextDouble() >= 0.5d ? -BALL_SPEED : BALL_SPEED;
		}

		public void move() 
		{
			x+=xMovement;
			y+=yMovement;
		}

		public void bounceVertical() {
			yMovement = -yMovement;
		}

		public void bounceHorizontal() {
			xMovement = -xMovement;
		}		
		
		public Rec2d getBoundingBox() 
		{
			final double x1 = x - BALL_RADIUS;
			final double x2 = x + BALL_RADIUS;

			final double y1 = y - BALL_RADIUS;
			final double y2 = y + BALL_RADIUS;
			
			return new Rec2d( x1 , y1 , x2 , y2 );
		}
	}

	protected class GamePanel extends JPanel 
	{
		private double xInc;
		private double yInc;

		@Override
		public void paint(Graphics g) 
		{
			super.paint(g);
			
			xInc = (getWidth()*0.95d) / MODEL_WIDTH;
			yInc = (getHeight()*0.95d) / MODEL_HEIGHT;			

			renderBounds( g );
			renderBall( g );
			renderBats( g );
			renderScores( g );	
			
			Toolkit.getDefaultToolkit().sync(); // required on Linux, otherwise animation is choppy
		}

		private void renderScores(Graphics g) 
		{
			g.setColor(Color.BLACK );
			final String score = leftPlayer.name+": "+leftPlayer.score+" - "+rightPlayer.name+": "+rightPlayer.score; 
			g.drawString( score , 10 , 15 );
		}

		private void renderBats(Graphics g) 
		{
			g.setColor(Color.GREEN);
			renderBat( leftPlayer.bat , g );
			renderBat( rightPlayer.bat , g );
		}

		private void renderBat(Bat bat, Graphics g) {

			Point center = modelToView( bat.x , bat.y );
			Point extend = modelToView( bat.width , bat.height );
			g.fillRect( center.x ,center.y, extend.x , extend.y );
		}

		private void renderBall(Graphics g) 
		{
			g.setColor(Color.RED);
			Point center = modelToView( ball.x , ball.y );
			Point diameters = modelToView(ball.diameter,ball.diameter);
			final double radius = diameters.x /2.0d;

			g.fillOval( (int) Math.round( center.x - radius ), 
					(int) Math.round( center.y - radius ) , diameters.x , diameters.x ); 
		}

		private void renderBounds(Graphics g) 
		{
			g.setColor(Color.BLUE);
			drawLine( 0 , 0, MODEL_WIDTH , 0 , g );
			drawLine( 0 , 0, 0 , MODEL_HEIGHT , g );
			drawLine( MODEL_WIDTH , 0, MODEL_WIDTH , MODEL_HEIGHT , g );
			drawLine( 0 , MODEL_HEIGHT, MODEL_WIDTH , MODEL_HEIGHT , g );
		}

		private void drawLine(double x1,double y1,double x2,double y2,Graphics g) {

			final Point p1 = modelToView(x1,y1);
			final Point p2 = modelToView(x2,y2);

			g.drawLine(p1.x,p1.y,p2.x,p2.y);
		}

		private Point modelToView(double x,double y) {
			int xr = (int) Math.round( x * xInc );
			int yr = (int) Math.round( y * yInc );
			return new Point(xr,yr);
		}
	}

	private void restartGame() 
	{
		System.out.println("Game reset.");
		
		leftPlayer.reset();
		rightPlayer.reset();

		ball.placeRandomly();		
	}

	private void handleCollisions() 
	{
		final Rec2d bb = ball.getBoundingBox();
		if ( bb.overlap( leftPlayer.bat.getBoundingBox() ) && bb.x1 > leftPlayer.bat.centerX() ) 
		{
			if ( ! inCollision ) {
				ball.bounceHorizontal();
				inCollision = true;
			}
			return;
		} 
		else if ( bb.overlap( rightPlayer.bat.getBoundingBox() ) && bb.x2 < rightPlayer.bat.centerX()) 
		{
			if ( ! inCollision ) {
				ball.bounceHorizontal();
				inCollision = true;
			}			
			return;
		}
		
		inCollision = false;
		
		if ( ball.x < BALL_RADIUS ) { // collision left border
			rightPlayer.incScore();
			ball.placeRandomly();
			return;
		}
		
		if ( ball.x > (MODEL_WIDTH-BALL_RADIUS) ) { // collision right border
			leftPlayer.incScore();
			ball.placeRandomly();
			return;
		}		
		
		if ( ball.y < BALL_RADIUS) { // collision top border
			ball.bounceVertical();
			return;
		}

		
		if ( ball.y > ( MODEL_HEIGHT - BALL_RADIUS) ) { // collision bottom border
			ball.bounceVertical();
			return;
		}		
	}

	private Bat createBat(double xposition)
	{
		int height = (int) BAT_HEIGHT;
		int width = (int) BAT_WIDTH;

		final int center = (int) (MODEL_HEIGHT/2.0);
		return new Bat( (int) xposition , center , width , height );
	}

	public static enum Direction 
	{
		NONE {
			@Override
			public void moveBat(Player bat) {}
		},
		UP{

			@Override
			public void moveBat(Player player) {
				player.bat.moveUp();
			}
			
		},DOWN {

			@Override
			public void moveBat(Player player) {
				player.bat.moveDown();
			}
		};
		
		public abstract void moveBat(Player player);
	}
	private class MyKeyListener extends KeyAdapter {

		private Direction player1Dir = Direction.NONE;
		private Direction player2Dir = Direction.NONE;

		public void keyTyped(java.awt.event.KeyEvent e) {

			if ( e.getKeyChar() == ' ' ) {
				restartGame();
			}
		}

		public void keyReleased(KeyEvent e) 
		{
			if ( e.getKeyCode() == KeyEvent.VK_Q || e.getKeyCode() == KeyEvent.VK_A) 
			{
				player1Dir = Direction.NONE;
			} 
			else if ( e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) 
			{
				player2Dir = Direction.NONE;				
			} 
		}
		
		public void processUserInput() 
		{
			player1Dir.moveBat( leftPlayer );
			player2Dir.moveBat( rightPlayer );
		}

		public void keyPressed(java.awt.event.KeyEvent e) {

			if ( e.getKeyCode() == KeyEvent.VK_Q) 
			{
				player1Dir = Direction.UP;
			} 
			else if ( e.getKeyCode() == KeyEvent.VK_A) 
			{
				player1Dir = Direction.DOWN;
			} 
			else if ( e.getKeyCode() == KeyEvent.VK_UP) 
			{
				player2Dir = Direction.UP;
			} 
			else if ( e.getKeyCode() == KeyEvent.VK_DOWN) 
			{
				player2Dir = Direction.DOWN;				
			}            	
		}
	}
}