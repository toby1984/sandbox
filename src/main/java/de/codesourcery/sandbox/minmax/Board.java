package de.codesourcery.sandbox.minmax;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.sandbox.minmax.MinMax.Move;

public class Board 
{
    public static final int LOSS_SCORE = -100000;
    public static final int DRAW_SCORE =   0;
    public static final int WIN_SCORE  =  100000;

    protected interface IBoardVisitor {
        public void visit(int x,int y , Player p);
    }

    private int tileCount = 0;
    private final int width;
    private final int height;

    private final Player[][] data;

    public Board(Board b) 
    {
        this.width = b.width;
        this.height = b.height;
        this.tileCount = b.tileCount;
        this.data = createArray(b.width , b.height);
        for(int i = 0 ; i < width ; i++ ) {
            System.arraycopy( b.data[i] , 0 , this.data[i] , 0 , height );
        }
    }
    
    public String toCode() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Board b = new Board("+width+","+height+");\n");
        visit( new IBoardVisitor() {

            @Override
            public void visit(int x, int y, Player p)
            {
                if ( p != null ) {
                    builder.append("b.set("+x+","+y+","+p.name().toLowerCase()+");\n");
                }
            }} );
        return builder.toString();
    }

    public Board(int width, int height)
    {
        this.width = width;
        this.height = height;

        data = createArray(width,height);
    }
    
    public List<Move> getPossibleMoves(Player currentPlayer) 
    {
        final List<Move> result = new ArrayList<>();
        for ( int x = 0 ; x < width ; x++ ) {
            for ( int y = 0 ; y < height ; y++ ) 
            {
                if ( isValidMove(x, y) ) {
                    result.add( new Move(x,y) );
                }
            }
        }
        return result;
    }    
    
    public boolean isValidMove(int x,int y) 
    {
        for ( int iy = 0 ; iy <= y ; iy++ ) 
        {
            if ( data[x][iy] != null ) {
                return false;
            }
        }
        return y == height - 1 || (data[x][y+1] != null);
    }
    
    public boolean isEmpty() {
        return tileCount == 0;
    }

    public boolean isFull() 
    {
        return tileCount == width*height;
    }

    public String toString()
    {
        return toString(1000,1000,"");
    }

    public static void main(String[] args)
    {

        Board b = new Board(3,3);
        Player human = new Player(1,"human",Color.RED);
        Player computer = new Player(2,"computer",Color.BLACK);

        b.set(0,0,computer);
        b.set(0,1,computer);
        b.set(0,2,human);
        b.set(1,0,human);        
        b.set(1,1,human);
        b.set(1,2,human);
        
        b.set(2,0,computer);
        b.set(2,1,human);
        b.set(2,2,computer);
        
        int score = b.calculateScore( human , 3 );
        if ( score != Board.WIN_SCORE ) {
            throw new RuntimeException("Got score: "+score);
        }        
        
        System.exit(1);

        // Column 3
        b.clear();
        b.set(3,0,human);
        b.set(3,1,human);
        b.set(3,2,human);
        score = b.calculateScore( human , 3 );
        if ( score != Board.WIN_SCORE ) {
            throw new RuntimeException("Got score: "+score);
        }

        // Column 0, bottom
        b.clear();
        b.set(0,3,human);
        b.set(0,2,human);
        b.set(0,1,human);
        score = b.calculateScore( human , 3 );
        if ( score != Board.WIN_SCORE ) {
            throw new RuntimeException("Got score: "+score);
        }

        // column 0 , top
        b.clear();
        b.set(0,0,human);
        b.set(0,1,human);
        b.set(0,2,human);
        score = b.calculateScore( human , 3 );
        if ( score != Board.WIN_SCORE ) {
            throw new RuntimeException("Got score: "+score);
        }        

        /* X
         *  X
         *   X
         */
        b.clear();
        b.set(0,0,human);
        b.set(1,1,human);
        b.set(2,2,human);
        score = b.calculateScore( human , 3 );
        if ( score != Board.WIN_SCORE ) {
            throw new RuntimeException("Got score: "+score);
        }   

        /*
         * check diagonal:
         *       X
         *     X
         *   X
         */
        b.clear();
        b.set(3,3,human);
        b.set(2,2,human);
        b.set(1,1,human);
        score = b.calculateScore( human , 3 );
        if ( score != Board.WIN_SCORE ) {
            throw new RuntimeException("Got score: "+score);
        }        

        System.out.println("OK.");
    }

    public String toString(int expectedX,int expectedY,String value)
    {
        return toString(expectedX,expectedY,value,null);
    }
    
    public String toString(String rightText)
    {
        return toString(-1,-1,null ,rightText );
    }    
    
    public String toString(int expectedX,int expectedY,String value,String rightText)
    {
        
        final int centerY = height / 2;
        
        StringBuilder builder = new StringBuilder();
        for ( int y = 0 ; y < height ; y++ ) 
        {
            builder.append("|");
            for ( int x = 0 ; x < width ; x++ ) 
            {
                Player p = data[x][y];
                final String s;
                if ( x == expectedX && y == expectedY ) {
                    s = value;
                } else {
                    if ( p ==null ) {
                        s = "";
                    } 
                    else 
                    {
                        s = ""+p.name().charAt(0);
                    }
                }
                builder.append( StringUtils.center(s, 3 , " ") );
                if ( (x+1) < width ) {
                    builder.append("+");
                }
            }
            builder.append("|");
            
            if ( y == centerY && rightText != null ) {
                builder.append("    "+rightText);
            }       
            
            if ( (y+1) < height ) 
            {
                builder.append("\n");
                for ( int i = 0 ; i < width ; i++ ) {
                    builder.append("+---");
                }
                builder.append("|");
                builder.append("\n");
            }
        }
        return builder.toString();
    } 

    public int calculateScore(Player p,int requiredToWin) 
    {
        int totalScore = 0;
        final SequenceVisitor visitor = new SequenceVisitor(p , requiredToWin );

        // rows score
        int score = calculateRowsScore( visitor );
        if ( score == WIN_SCORE ) {
            return WIN_SCORE;
        }
        totalScore += score;

        // columns score
        score = calculateColumnsScore( visitor );
        if ( score == WIN_SCORE ) {
            return WIN_SCORE;
        }
        totalScore += score;      

        // diagonals #1 score
        score = calculateDiagonals1Score( visitor );
        if ( score == WIN_SCORE ) {
            return WIN_SCORE;
        }
        totalScore += score;    

        // diagonals #2 score
        score = calculateDiagonals2Score( visitor );
        if ( score == WIN_SCORE ) {
            return WIN_SCORE;
        }
        totalScore += score;         

        return totalScore == 0 ? 1 : totalScore;
    }

    private int calculateRowsScore(SequenceVisitor visitor) 
    {
        int totalScore = 0;
        for ( int y = 0 ; y < height; y++ ) 
        {
            visitor.reset();

            Player previous = null;
            for ( int x = 0 ; x < width ; x++ ) 
            {
                final Player actual = data[x][y];
                if ( ! visitor.visit( actual , previous , x > 0 ) ) {
                    return visitor.getScore();
                }
                previous = actual;
            }
            if ( ! visitor.finish() ) {
                return visitor.getScore();
            }            
            totalScore += visitor.getScore();            
        }
        return totalScore;        
    }

    private int calculateColumnsScore(SequenceVisitor visitor)
    {
        int totalScore = 0;
        for ( int x = 0 ; x < width ; x++ ) 
        {
            visitor.reset();

            Player previous = null;
            for ( int y = 0 ; y < height ; y++ ) 
            {
                final Player actual = data[x][y];
                if ( ! visitor.visit( actual , previous , y > 0 ) ) {
                    return visitor.getScore();
                }
                previous = actual;
            }
            if ( ! visitor.finish() ) {
                return visitor.getScore();
            }
            totalScore += visitor.getScore();            
        }
        return totalScore;
    }    

    private int calculateDiagonals1Score(SequenceVisitor visitor) 
    {
        /*     X
         *   X
         * X
         */
        int totalScore = 0;
        final int yMax = height-1;

        for ( int x = 0 ; x < width ; x++ ) 
        {
            visitor.reset();
            int i = x;
            Player previous = null;
            for ( int y = yMax ; y >= 0 && i < width; y--, i++ ) 
            {
                final Player actual = data[i][y];
                if ( ! visitor.visit( actual , previous , y > yMax ) ) {
                    return visitor.getScore();
                }
                previous = actual;
            }
            if ( ! visitor.finish() ) {
                return visitor.getScore();
            }            
            totalScore += visitor.getScore();            
        }

        for ( int y = yMax-1 ; y >= 0 ; y-- ) 
        {
            visitor.reset();
            int i = y;
            Player previous = null;
            for ( int x = 0 ; x < width && i >= 0 ; x++, i-- ) 
            {
                final Player actual = data[x][i];
                if ( ! visitor.visit( actual , previous , x > 0 ) ) {
                    return visitor.getScore();
                }
                previous = actual;
            }
            if ( ! visitor.finish() ) {
                return visitor.getScore();
            }            
            totalScore += visitor.getScore();            
        }          
        return totalScore;
    }    

    private int calculateDiagonals2Score(SequenceVisitor visitor) 
    {
        /* X   
         *   X 
         *     X
         */
        int totalScore = 0;

        for ( int x = 0 ; x < width ; x++ ) 
        {
            visitor.reset();
            int i = x;
            Player previous = null;
            for ( int y = 0 ; y < height && i < width; y++, i++ ) 
            {
                final Player actual = data[i][y];
                if ( ! visitor.visit( actual , previous , y > 0 ) ) {
                    return visitor.getScore();
                }
                previous = actual;
            }
            if ( ! visitor.finish() ) {
                return visitor.getScore();
            }            
            totalScore += visitor.getScore();            
        }

        for ( int y = 1 ; y < height ; y++ ) 
        {
            visitor.reset();
            int i = y;
            Player previous = null;
            for ( int x = 0 ; x < width && i < height  ; x++, i++ ) 
            {
                final Player actual = data[x][i];
                if ( ! visitor.visit( actual , previous , x > 0 ) ) {
                    return visitor.getScore();
                }
                previous = actual;
            }
            if ( ! visitor.finish() ) {
                return visitor.getScore();
            }            
            totalScore += visitor.getScore();            
        }   
        return totalScore;
    }    

    /**
     * Calculates a player's score for a single row/column/diagonal.
     * 
     * Increments score by:
     * 
     * 1 * seqLen : for each sequence of 1..(requiredToWin-1) tiles
     * 5 * seqLen : for each sequence of 1..(requiredToWin-1) tiles with at least one free slot before or after the sequence
     * 10 * seqLen: for each sequence of 1..(requiredToWin-1) tiles with a free slot before AND after the sequence
     * 
     * 10000 : If a sequence is equal to/longer than 'requiredToWin' tiles, this method immediately returns with a value of 10000.
     */    
    protected static final class SequenceVisitor {

        private final int requiredToWin;
        private final Player currentPlayer;

        // transient
        private int rating = 0;

        private int sequenceLen = 0;
        private boolean inSequence = false;
        private boolean freeAtStart = false;

        public SequenceVisitor(Player currentPlayer,final int requiredToWin) {
            this.requiredToWin = requiredToWin;
            this.currentPlayer = currentPlayer;
        }

        public int getScore() 
        {
            return rating;
        }

        public boolean finish() 
        {
            if ( inSequence ) 
            {
                final boolean result = updateScore( null, true );
                inSequence = false;
                return result;
            } 
            return true;
        }

        public void reset() {
            rating = 0;
            sequenceLen = 0;
            inSequence = false;
            freeAtStart = false;            
        }

        public boolean visit(Player currentTileOwner,Player previous,boolean hasPrevious) 
        {
            if ( currentTileOwner == null || ! currentTileOwner.equals( currentPlayer ) ) 
            {
                if ( inSequence &&  ! updateScore( currentTileOwner, false ) )  
                {
                    inSequence = false;
                    return false;
                }

                sequenceLen = 0;
                freeAtStart = false;
                inSequence = false;                    
            } 
            else 
            {
                if ( ! inSequence && hasPrevious && previous == null ) {
                    freeAtStart = true;
                }
                inSequence = true;
            }  

            if ( inSequence ) {
                sequenceLen++;
            }
            return true;
        }

        private boolean updateScore(Player actual,boolean atEnd)
        {
            final boolean freeAtEnd = ! atEnd && actual == null;

            if ( sequenceLen >= requiredToWin ) 
            {
                rating = WIN_SCORE; // player has won
                return false;
            }
            
            if ( freeAtStart && freeAtEnd ) { // free space at start & end
                if ( sequenceLen == (requiredToWin -1 ) ) {
                    rating += (40 * sequenceLen);
                } else {
                    rating += (20 * sequenceLen);
                }
            } else if ( freeAtStart && ! freeAtEnd ) { // free space at start only
                if ( sequenceLen == (requiredToWin -1 ) ) {
                    rating += (10 * sequenceLen);
                } else {
                    rating += (5 * sequenceLen);
                }                
            } else {
//                rating += sequenceLen; // no free space at start nor end
            }
            return true;
        }
    }

    private static Player[][] createArray(int w,int h) 
    {
        Player[][] result = new Player[w][];
        for ( int i = 0 ; i < w; i++ ) 
        {
            result[i] = new Player[h];
        }
        return result;
    }

    public int width()
    {
        return width;
    }

    public int height()
    {
        return height;
    }

    public void set(int x,int y,Player player) 
    {
        if ( data[x][y] != null ) {
            throw new IllegalStateException("Position "+x+","+y+" is already occupied by player "+player);
        }
        data[x][y] = player;
        tileCount++;
    }

    public void clear(int x, int y)
    {
        if ( data[x][y] != null ) {
            data[x][y]=null;
            tileCount--;
        }
    }    

    public Player read(int x,int y) {
        return data[x][y];
    }

    public void clear() 
    {
        for ( int i = 0 ; i < width; i++ ) 
        {
            data[i] = new Player[height];
        }
        tileCount = 0;
    }

    public void visit(IBoardVisitor v) 
    {
        for ( int x= 0 ; x < width ; x++ ) {
            for ( int y= 0 ; y < height ; y++ ) {
                Player p = data[x][y];
                v.visit( x,  y ,  p );
            }
        }
    }
}