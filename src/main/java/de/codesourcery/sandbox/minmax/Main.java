package de.codesourcery.sandbox.minmax;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.sandbox.minmax.Board.IBoardVisitor;
import de.codesourcery.sandbox.minmax.MinMax.Move;
import de.codesourcery.sandbox.pathfinder.Vec2;
import de.codesourcery.sandbox.pathfinder.Vec2d;

public class Main
{
    private static final boolean DEBUG_BOARD = false;
    
    public static void main(String[] args) throws InterruptedException
    {
        new Main().run();
    }

    private final Random rnd = new Random(System.currentTimeMillis());
    
    private final MyPanel panel = new MyPanel();

    private final Player HUMAN = new Player(1,"Human",Color.GREEN);
    
    private final Player COMPUTER1 = new Player(2,"Computer 1",Color.BLACK);
    private final Player COMPUTER2 = new Player(3,"Computer 2",Color.RED);
    
    private final Player player1 = HUMAN;
    private final Player player2 = COMPUTER2;
    
    // game state
    private final Object BOARD_LOCK = new Object();
    private final Board board = new Board(7,6);    
    
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
            synchronized(BOARD_LOCK) {
                renderBoard(g);
            }
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
                                
                if ( winner != null ) {
                    g.setColor(winner.getColor());
                    g.drawString( "Winner: "+winner.name() , 15,15 );
                } else {
                    g.setColor(Color.RED);
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

            if ( i < 0 || i >= board.width() || j < 0 || j >= board.height() ) {
                return null;
            }

            final Vec2d center = modelToView(i, j);
            double dx = x1 - center.x;
            double dy = y1 - center.y;
            double distance = Math.sqrt(dx*dx+dy*dy);
            if ( distance <= radius ) {
                return new Vec2(i,j);
            }
            return null;
        }
    }

    
    public void autoPlay(Player currentPlayer) {
        
        if ( gameOver ) {
            return;
        }
        
        System.out.println("Thinking: "+currentPlayer);        
        
        synchronized( BOARD_LOCK ) 
        {
            final Move move;
            if ( board.isEmpty() ) {
                final List<Move> possible = board.getPossibleMoves( currentPlayer );
                move = possible.get( rnd.nextInt(possible.size()) );
            } else {
                move = new MinMax().calcNextMove( board , currentPlayer , player1, player2 );
            }
            move.apply( board , currentPlayer );
    
            checkGameOver();
        }
        panel.repaint();        
    }
    
    private void checkGameOver() 
    {
        if ( ! gameOver )
        {
            if ( new MinMax().hasWon( board , player1 ) ) 
            {
                gameOver = true;
                winner = player1;
                return;
            } 
            
            if ( new MinMax().hasWon( board , player2 ) ) 
            {
                gameOver = true;
                winner = player2;
                return;
            }                
            
            if ( board.isFull() ) {
                gameOver = true;
            }
            
            if ( gameOver ) {
                System.out.println("*** Game over. ***");
            }
        }
    }
    
    public void run() throws InterruptedException {

        panel.setPreferredSize(new Dimension(600,400));

        final MouseAdapter mouseListener = new MouseAdapter() 
        {
            public void mouseClicked(java.awt.event.MouseEvent e) 
            {
                Vec2 clicked = panel.viewToModel( e.getX(), e.getY() );
                if ( clicked != null && ! gameOver ) 
                {
                    if ( board.isValidMove( clicked.x , clicked.y ) )
                    {
                        board.set( clicked.x , clicked.y , player1 );

                        checkGameOver();
                        
                        if ( ! gameOver && ! DEBUG_BOARD ) 
                        {
                            System.out.println("Calculating...");
                            
                            Move move = new MinMax().calcNextMove( board , player2 , player1, player2 );
                            move.apply( board , player2 );

                            checkGameOver();
                        }
                        
                        panel.repaint();
                    }
                }
            }
        };

        if ( player1.equals( HUMAN ) || player2.equals( HUMAN ) ) {
            panel.addMouseListener( mouseListener );
        } 

        final JFrame frame = new JFrame("Tic-Tac-Tock");

        frame.addKeyListener( new KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent e) 
            {
                if ( gameOver && e.getKeyChar() == ' ' ) {
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
        
        if ( ! player1.equals(HUMAN) && ! player2.equals(HUMAN) ) {
            while ( true ) 
            {
                while ( ! gameOver ) 
                {
                    autoPlay( player1 );
                    autoPlay( player2 );
                }
                System.out.println("*** Game over. ***");
                Thread.sleep(1000);
            }
        }
    }
}