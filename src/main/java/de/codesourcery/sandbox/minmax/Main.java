package de.codesourcery.sandbox.minmax;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.sandbox.minmax.Board.IBoardVisitor;
import de.codesourcery.sandbox.minmax.MinMax.Move;
import de.codesourcery.sandbox.pathfinder.Vec2;
import de.codesourcery.sandbox.pathfinder.Vec2d;

public class Main
{
    public static void main(String[] args)
    {
        new Main().run();
    }

    private final Board board = new Board(4,4);
    private final Player human = new Player("Human",Color.GREEN);
    private final Player computer = new Player("Computer",Color.BLACK);
    
    private boolean gameOver;
    private Player winner = null;

    protected final class MyPanel extends JPanel {

        private int xOffset;
        private int yOffset;
        private double xInc;
        private double yInc;

        private double radius;

        @Override
        public void paint(Graphics g)
        {
            super.paint(g);
            renderBoard(g);
        }

        private void renderBoard(final Graphics g) {

            xOffset = (int) (getWidth()*0.1);
            yOffset = (int) (getHeight()*0.1);

            xInc = (getWidth()-xOffset) / (double) board.width();
            yInc = (getHeight()-yOffset) / (double) board.height();

            final double diameter = xInc > yInc ? yInc*0.8 : xInc*0.8;
            radius = diameter/2;

            final IBoardVisitor visitor = new IBoardVisitor() {

                @Override
                public void visit(int x, int y, Player p)
                {
                    Vec2d center = modelToView(x,y);
                    g.setColor( Color.BLUE );                    
                    g.drawRect( (int) (center.x-radius) ,(int) (center.y-radius), (int) diameter , (int) diameter );                    
                    if ( p != null ) {
                        g.setColor( p.getColor() );
                        g.fillOval( (int) (center.x-radius) ,(int) (center.y-radius), (int) radius*2 , (int) radius*2 );
                    } else {
                        g.setColor( Color.BLUE );
                        g.drawOval( (int) (center.x-radius) ,(int) (center.y-radius), (int) radius*2 , (int) radius*2 );
                    }
                }
            };
            board.visit( visitor );

            if ( gameOver ) 
            {
                g.setColor(Color.RED);                
                if ( winner != null ) {
                    g.drawString( "Winner: "+winner.name() , 15,15 );
                } else {
                    g.drawString( "Draw !!!!" , 15 , 15 );
                }
            }
        }

        private Vec2d modelToView(int x,int y) 
        {
            double x1 = x*xInc;
            double y1 = y*yInc;

            final double centerX = x1+(xInc/2.0);
            final double centerY = y1+(yInc/2.0);
            return new Vec2d(xOffset+centerX,yOffset+centerY);
        }

        public Vec2 viewToModel(int x1,int y1) 
        {
            int i = (int) ( (x1-xOffset) / xInc);
            int j = (int) ( (y1-yOffset) / yInc);

            System.out.println("Clicked: "+i+","+j);  
            if ( i < 0 || i >= board.width() || j < 0 || j >= board.height() ) {
                return null;
            }

            final Vec2d center = modelToView(i, j);
            double dx = x1 - center.x;
            double dy = y1 - center.y;
            double distance = Math.sqrt(dx*dx+dy*dy);
            System.out.println("Distance: "+distance+" , radius="+radius);
            if ( distance <= radius ) {
                return new Vec2(i,j);
            }
            return null;
        }
    }

    public void run() {

        final MyPanel panel = new MyPanel();

        panel.setPreferredSize(new Dimension(600,400));

        final MouseAdapter mouseListener = new MouseAdapter() 
        {
            public void mouseClicked(java.awt.event.MouseEvent e) 
            {
                Vec2 clicked = panel.viewToModel( e.getX(), e.getY() );
                if ( clicked != null && ! gameOver ) {
                    Player existing = board.read(clicked.x,clicked.y);
                    if ( existing == null ) 
                    {
                        board.set( clicked.x , clicked.y , human );

                        MinMax minMax = new MinMax();

                        int score = minMax.evaluate( board , human , human,computer );
                        System.out.println("Score = "+score);
                        
                        if ( minMax.hasWon(board , human ) ) {
                            gameOver = true;
                            winner = human;
                        } 
                        
                        if ( board.isFull() ) {
                            gameOver = true;
                        }
                        
                        if ( ! gameOver ) 
                        {
                            System.out.println("Calculating...");
                            Move move = minMax.calcNextMove( board , computer , human, computer );
                            System.out.println("Finished , best move: "+move);
                            move.apply( board , computer );
                            
                            score = minMax.evaluate( board , computer , human,computer );
                            System.out.println("Score = "+score);
                            
                            if ( minMax.hasWon(board , computer ) ) {
                                winner = computer;
                                gameOver = true;
                            }
                            if ( board.isFull() ) {
                                gameOver = true;
                            }
                        }
                        panel.repaint();
                    }
                }
            }
        };

        panel.addMouseListener( mouseListener );  

        final JFrame frame = new JFrame("Tic-Tac-Tock");

        frame.addKeyListener( new KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent e) {
                if ( e.getKeyChar() == ' ' ) {
                    board.clear();
                    winner=null;
                    gameOver = false;
                    panel.repaint();
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
