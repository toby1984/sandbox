package de.codesourcery.sandbox.pathfinder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.sandbox.pathfinder.QuadTree.IVisitor;
import de.codesourcery.sandbox.pathfinder.QuadTree.QuadLeafNode;
import de.codesourcery.sandbox.pathfinder.QuadTree.QuadNode;

public class QuadTreeTest {

	private static final int MODEL_WIDTH = 400;
	private static final int MODEL_HEIGHT = 600;

	private final QuadTree<Byte> tree= new QuadTree<Byte>(MODEL_WIDTH , MODEL_HEIGHT);

	public static void main(String[] args) 
	{
		new QuadTreeTest().run();
	}

	public void run() 
	{
		final MyPanel panel = new MyPanel();
		
		Random rnd = new Random(System.currentTimeMillis());
		for ( int i = 0 ; i < 1000 ; i++ ) 
		{
			int x = rnd.nextInt(MODEL_WIDTH);
			int y = rnd.nextInt(MODEL_HEIGHT);
			tree.store( x,y,(byte)1);
		}

		panel.setPreferredSize(new Dimension(MODEL_WIDTH,MODEL_HEIGHT));

		panel.addMouseListener( new MouseAdapter() 
		{
			public void mouseClicked(java.awt.event.MouseEvent e) 
			{
				final Point p = panel.viewToModel( e.getX() , e.getY() );
				System.out.println("Store: "+p);
				tree.store( p.x,p.y, (byte) 1 );
				panel.repaint();
			};
		} );  

		final JFrame frame = new JFrame("QuadTreeTest");
		
		frame.addKeyListener( new KeyAdapter() {
			
			public void keyTyped(java.awt.event.KeyEvent e) {
				
				if ( e.getKeyChar() == ' ' && panel.getCurrentSelection() != null ) 
				{
					final Rec2 selection = panel.getCurrentSelection() ;
					final Rec2 rect = panel.viewToModel( selection);
					
					long time1 = -System.currentTimeMillis();
					final List<QuadLeafNode<Byte>> nodes = tree.getValues( rect );
					time1 += System.currentTimeMillis();
					
					System.out.println("Selection time: "+time1);
					
					for (QuadLeafNode<Byte> leaf : nodes) {
						panel.markLeaf( leaf );
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

	private class MyPanel extends JPanel {

		private double xInc;
		private double yInc;

		private boolean isDragging = false;

		private Vec2 start;
		private Vec2 current;

		private final MouseAdapter adapter = new MouseAdapter() {

			public void mousePressed(java.awt.event.MouseEvent e) 
			{
				if ( e.getButton() == MouseEvent.BUTTON3 ) 
				{
					if ( start != null ) {
						drawSelection();
					}
					
					isDragging = true;
					start = new Vec2(e.getX(),e.getY());
					current = start;
					drawSelection();
				}
			}

			public void mouseReleased(MouseEvent e) {

				if ( e.getButton() == MouseEvent.BUTTON3 ) 
				{
					isDragging = false;
					updateSelection(e.getX(),e.getY());
				}				
			};

			private void updateSelection(int x2, int y2) 
			{
				drawSelection();
				current = new Vec2(x2,y2);
				drawSelection();
			}

			public void mouseDragged(java.awt.event.MouseEvent e) {
				if ( isDragging ) {
					updateSelection(e.getX(),e.getY());
				}
			}
		};

		public MyPanel() {
			addMouseListener( adapter );
			addMouseMotionListener( adapter );
		}
		
		public Rec2 getCurrentSelection() {
			if ( start != null ) {
				return new Rec2(start,current);
			}
			return null;
		}
		
		private void drawSelection() 
		{
			drawSelection( (Graphics2D) getGraphics() );
		}

		private void drawSelection(Graphics2D graphics) 
		{
			graphics.setXORMode( Color.RED );

			final Rec2 rec = new Rec2(start,current);

			graphics.drawRect( rec.x1 , 
					rec.y1 , 
					rec.width() , rec.height());
			
			graphics.setPaintMode();
		}
		
		@Override
		public void paint(final Graphics g)
		{
			setBackground(Color.WHITE);
			super.paint(g);
			
			if ( ! isDragging && start != null ) {
				drawSelection((Graphics2D) g);
			}

			final Graphics2D graphics = (Graphics2D) g;

			xInc = getWidth() / (double) MODEL_WIDTH;
			yInc = getHeight() / (double) MODEL_HEIGHT;

			final IVisitor<Byte> visitor = new IVisitor<Byte>() {

				@Override
				public boolean visit(QuadNode<Byte> node, int currentDepth) 
				{
					if ( node.isLeaf() ) 
					{
						// render leaf
						renderLeaf(graphics, node);
					} else { 
						// render non-leaf
						graphics.setColor( Color.BLUE );
						renderBox(  node , graphics );
					}
					return true;
				}
			};

			tree.visitPreOrder( visitor );
		}
		
		public void markLeaf(QuadNode<Byte> node) 
		{
			Graphics2D g = (Graphics2D) getGraphics();
			g.setColor(Color.GREEN);
			renderFilledCircle( modelToView( node.x1 , node.y1 ) , 10 , g );
		}
		
		private void renderLeaf(final Graphics2D graphics,QuadNode<Byte> node) 
		{
			graphics.setColor( Color.RED );
			renderCircle( modelToView( node.x1 , node.y1 ) , 10 , graphics );
		}		
		
		private void renderBox(QuadNode<Byte> node,Graphics2D graphics) 
		{
			Point p = modelToView( node.x1 , node.y1 );
			double viewWidth = node.width() * xInc;
			double viewHeight = node.height() * yInc;
			renderBox( p , (int) viewWidth , (int) viewHeight , graphics );
		}

		private void renderBox(Point p1 , int width,int height,Graphics2D graphics) {
			graphics.drawRect( p1.x , p1.y , width , height );
		}
		
		private void renderCircle(Point p1,int radius,Graphics2D graphics) 
		{
			final int topLeftX = p1.x-(radius/2);
			final int topLeftY = p1.y-(radius/2);
			graphics.drawRect( topLeftX, topLeftY, radius , radius );
		}   
		
		private void renderFilledCircle(Point p1,int radius,Graphics2D graphics) 
		{
			final int topLeftX = p1.x-(radius/2);
			final int topLeftY = p1.y-(radius/2);
			graphics.fillRect( topLeftX, topLeftY, radius , radius );
		}  
		
		public Rec2 viewToModel(Rec2 r) 
		{
			Point p1 = viewToModel( r.x1 , r.y1 );
			Point p2 = viewToModel( r.x2 , r.y2 );
			return new Rec2( p1.x , p1.y , p2.x , p2.y );
		}

		public Point modelToView(int x,int y) 
		{
			double xView = x*xInc;
			double yView = y*yInc;
			return new Point( (int) xView , (int) yView );
		}    

		public Point viewToModel(int x,int y) 
		{
			double xInc = getWidth() / (double) MODEL_WIDTH; // 2000 / 1000 => 0.5
					double yInc = getHeight() / (double) MODEL_HEIGHT;
					double xModel = x / xInc;
					double yModel  = y / yInc;
					return new Point( (int) xModel, (int) yModel);
		}      	
	};
}
