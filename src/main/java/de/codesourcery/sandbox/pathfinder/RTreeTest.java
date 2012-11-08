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

import de.codesourcery.sandbox.pathfinder.RTree.RTreeVisitor;
import de.codesourcery.sandbox.pathfinder.RTree.RegularNode;
import de.codesourcery.sandbox.pathfinder.RTree.ValueNode;

public class RTreeTest {

	private static final boolean RENDER_NON_LEAF_NODES = false;
	
	private static final int RANDOM_NODE_COUNT = 1000;
	private static final boolean RANDOM_NODES = false;	

	private static final int MODEL_WIDTH = 600;
	private static final int MODEL_HEIGHT = 400;

	private final RTree<Integer> rtree= new RTree<Integer>(5);

	public static void main(String[] args) 
	{
		new RTreeTest().run();
	}

	public void run() 
	{
		if ( RANDOM_NODES ) {
			Random r = new Random(System.currentTimeMillis());
			for ( int i = 0 ; i < RANDOM_NODE_COUNT ; i++ ) 
			{
				int width  = 2+r.nextInt(30);
				int height = 2+r.nextInt(30);

				int x = 2+r.nextInt(MODEL_WIDTH);
				int y = 2+r.nextInt(MODEL_HEIGHT);
				rtree.add( new Rec2(x,y,x+width,y+height), 1 );
			}

			final int[] leafNodes = {0};
			final int[] nonLeafNodes = {0};

			RTreeVisitor<Integer> visitor = new RTreeVisitor<Integer>() {

				@Override
				public boolean visitNode(RegularNode<Integer> node, int depth) 
				{
					nonLeafNodes[0]++;
					return true;
				}

				@Override
				public boolean visitValue(ValueNode<Integer> node, int depth) {
					leafNodes[0]++;
					return true;
				}
			};

			rtree.visitPreOrder( visitor );

			int totalNodes = leafNodes[0]+nonLeafNodes[0];
			System.out.println("Leaf     nodes: "+leafNodes[0]);
			System.out.println("Non-Leaf nodes: "+nonLeafNodes[0]);
			System.out.println("Total    nodes: "+totalNodes);
			final float fillFactor = leafNodes[0] / (float) totalNodes;
			System.out.println("Fill    factor: "+fillFactor);
		}
		
		final MyPanel panel = new MyPanel();

		panel.setPreferredSize(new Dimension(MODEL_WIDTH,MODEL_HEIGHT));

		panel.addMouseListener( new MouseAdapter() 
		{
			public void mouseClicked(java.awt.event.MouseEvent e) 
			{
				final Point p = panel.viewToModel( e.getX() , e.getY() );

				rtree.add( new Rec2(p.x,p.y,p.x+10,p.y+10) , 1 );
				panel.repaint();
			};
		} );  

		final JFrame frame = new JFrame("R-tree test");

		frame.addKeyListener( new KeyAdapter() {

			public void keyTyped(java.awt.event.KeyEvent e) {

				if ( e.getKeyChar() == ' ' && panel.getCurrentSelection() != null ) 
				{
					final Rec2 selection = panel.getCurrentSelection() ;
					final Rec2 rect = panel.viewToModel( selection);

					long time1 = -System.currentTimeMillis();
					final List<ValueNode<Integer>> nodes = rtree.getValues( rect );
					time1 += System.currentTimeMillis();

					System.out.println("Selection time: "+time1);

					for (ValueNode<Integer> leaf : nodes) {
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

			renderLeafNodes(graphics);			
			if ( RENDER_NON_LEAF_NODES ) {
				renderNonLeafNodes(graphics);
			}
		}

		private void renderLeafNodes(final Graphics2D graphics)
		{

			graphics.setColor( Color.RED ); 

			final RTreeVisitor<Integer> visitor2 = new RTreeVisitor<Integer>() {

				@Override
				public boolean visitNode(RegularNode<Integer> node, int depth) {
					return true;
				}

				@Override
				public boolean visitValue(ValueNode<Integer> node, int depth) 
				{
					renderLeaf(graphics, node);					
					return true;
				}
			};
			rtree.visitPreOrder( visitor2 );
		}

		private final Random rnd = new Random(System.currentTimeMillis());

		private Color randomColor() 
		{
			do {
				int r = rnd.nextInt(256);
				int g = rnd.nextInt(256);
				int b = rnd.nextInt(256);
				double dist = Math.sqrt(r*r+g*g+b*b);
				if ( dist > 36.65 ) {
					return new Color(r,g,b);
				}
			} while ( true );
		}

		private void renderNonLeafNodes(final Graphics2D graphics)
		{
			graphics.setColor( Color.BLUE );
			final RTreeVisitor<Integer> visitor2 = new RTreeVisitor<Integer>() {

				@Override
				public boolean visitNode(RegularNode<Integer> node, int depth) 
				{
					if ( depth > 1 ) {
						renderBox(  node , graphics );
					}
					return true;
				}

				@Override
				public boolean visitValue(ValueNode<Integer> node, int depth) {
					return true;
				}
			};
			
			rtree.visitPreOrder( visitor2 );
			
			graphics.setColor( Color.GREEN );
			final RTreeVisitor<Integer> visitor3 = new RTreeVisitor<Integer>() {

				@Override
				public boolean visitNode(RegularNode<Integer> node, int depth) {
					if ( depth == 1 ) {
						renderBox(  node , graphics );
					}
					return true;
				}

				@Override
				public boolean visitValue(ValueNode<Integer> node, int depth) {
					return true;
				}
			};			
			
			rtree.visitPreOrder( visitor3 );			
		}        

		public void markLeaf(ValueNode<Integer> node) 
		{
			Graphics2D g = (Graphics2D) getGraphics();
			g.setColor(Color.GREEN);

			Point p1 = modelToView( node.x1 , node.y1 );
			Point p2 = modelToView( node.x2 , node.y2 );

			renderFilledCircle(  new Rec2( p1.x , p1.y , p2.x , p2.y ) , g );
		}

		private void renderLeaf(final Graphics2D graphics,ValueNode<Integer> node) 
		{
			renderFilledBox( node , graphics );
		}		

		private void renderBox(RegularNode<Integer> node,Graphics2D graphics) 
		{
			Point p = modelToView( node.x1 , node.y1 );
			double viewWidth = node.width() * xInc;
			double viewHeight = node.height() * yInc;
			renderBox( p , (int) viewWidth , (int) viewHeight , graphics );
		}

		private void renderFilledBox(ValueNode<Integer> node,Graphics2D graphics) 
		{
			Point p = modelToView( node.x1 , node.y1 );
			double viewWidth = node.width() * xInc;
			double viewHeight = node.height() * yInc;
			renderFilledBox( p , (int) viewWidth , (int) viewHeight , graphics );
		}		

		private void renderBox(Point p1 , int width,int height,Graphics2D graphics) {
			graphics.drawRect( p1.x , p1.y , width , height );
		}

		private void renderFilledBox(Point p1 , int width,int height,Graphics2D graphics) {
			graphics.fillRect( p1.x , p1.y , width , height );
		}		

		private void renderCircle(Point p1,int radius,Graphics2D graphics) 
		{
			final int topLeftX = p1.x-(radius/2);
			final int topLeftY = p1.y-(radius/2);
			graphics.drawRect( topLeftX, topLeftY, radius , radius );
		}   

		private void renderFilledCircle(Rec2 rect,Graphics2D graphics) 
		{
			graphics.fillRect( rect.x1 , rect.y1 , rect.width() , rect.height() );
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