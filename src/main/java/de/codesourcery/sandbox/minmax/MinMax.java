package de.codesourcery.sandbox.minmax;


public class MinMax
{
    private static final int NEG_INFINITY= -1000000000;
    private static final int POS_INFINITY=  1000000000;
    
    public static final int TILES_TO_WIN = 4;

    private static final boolean ALPHA_BETA = true;
    
    private static final boolean DEBUG_ONLY_FINISH = false;
    private static final boolean DEBUG = false;

    private static final int MAX_DEPTH = 8;

    public static final class Move 
    {
        private int x;
        private int y;

        protected Move(int x, int y)
        {
            this.x = x;
            this.y = y;
        }

        public void undo(Board b) {
            b.clear( x , y );
        }

        public void apply(Board b,Player player) {
            b.set( x , y , player );
        }

        @Override
        public String toString()
        {
            return "Move[ "+x+" , "+y+"]";
        }
    }

    public Move calcNextMove(Board board,final Player currentPlayer,Player player1,Player player2) 
    {
        final MinMaxImpl impl = new MinMaxImpl( board , player1,player2);
        return impl.findMove( currentPlayer );
    }

    protected class MinMaxImpl {

        private final Board board;
        private final Player player1;
        private final Player player2;
        
        private Player toSolve;
        private Move bestMove;

        public MinMaxImpl(Board board,Player player1,Player player2) {
            this.board = board;
            this.player1 = player1;
            this.player2 = player2;
        }

        private Player oppositePlayer(Player current) 
        {
            return current.equals( player1 ) ? player2 : player1;
        }
        
        public Move findMove(final Player currentPlayer) 
        {
            this.toSolve = currentPlayer;
            max(currentPlayer,0, NEG_INFINITY , POS_INFINITY );
            return bestMove;
        }        
        
        private int max(Player current, int depth,int alpha,int beta) 
        {
            int rating = evaluate();
            
            if (depth > MAX_DEPTH || isTerminalScore(rating) ) {
                if ( DEBUG ) System.out.println("COMPUTER[Depth "+depth+"] *** terminal state (rating: "+scoreToString(rating)+", max_depth: "+(depth > MAX_DEPTH)+" ***");
                return rating;
            }
            
            int maxScore = alpha;
            Move best = null;
            for ( Move move : board.getPossibleMoves( current )) 
            {
                if ( DEBUG ) {
                    System.out.println("COMPUTER[Depth "+depth+"] ================== Exploring move "+move);
                    System.out.println( board.toString( move.x , move.y , "O" , "Score ("+oppositePlayer(current)+"): "+rating) );
                }
                
                move.apply( board ,  current );
                int score = min( oppositePlayer(current) , depth+1 , maxScore , beta );
                move.undo( board );

                if ( DEBUG || (DEBUG_ONLY_FINISH && depth == 0 ) ) 
                {
                    System.out.println("COMPUTER[Depth "+depth+"] ================== Finished exploring move "+move+" (score: "+scoreToString(score)+")");
                }                
                
                if (score > maxScore) 
                {
                    maxScore = score;
                    best = move;
                    if ( ALPHA_BETA && score >= beta ) {
                        if ( DEBUG ) {
                            System.out.println("COMPUTER[Depth "+depth+"] ================== Alpha/Beta ( "+scoreToString(alpha)+" , "+scoreToString(beta)+" )");
                        }
                        break;
                    }
                }
            }
            
            if ( DEBUG ) {
                System.out.println("COMPUTER[Depth "+depth+"] ================== Best move : "+best+" (rating: "+scoreToString(maxScore)+")");
            }              
            
            if ( depth == 0 ) 
            {
                bestMove = best;
            }            
            return maxScore;
        }
        
        private int min(Player current, int depth,int alpha,int beta) 
        {
            int rating = evaluate();
            
            if (depth > MAX_DEPTH || isTerminalScore(rating) ) {
                if ( DEBUG ) System.out.println("HUMAN [Depth "+depth+"] *** terminal state (rating: "+scoreToString(rating)+", max_depth: "+(depth > MAX_DEPTH)+" ***");
                return rating;
            }
            
            Move best = null;
            int minScore = beta;
            for ( Move move : board.getPossibleMoves( current ) ) 
            {
                if ( DEBUG ) {
                    System.out.println("HUMAN [Depth "+depth+"] ================== Exploring move "+move);
                    System.out.println( board.toString( move.x , move.y , "O" , "Score ("+oppositePlayer(current)+"): "+scoreToString(rating)) );
                }
                
                move.apply( board ,  current );           
                int score = max( oppositePlayer( current ) , depth+1 , alpha , minScore );
                move.undo( board );
                
                if (score < minScore) 
                {
                    minScore = score;
                    best = move;
                    if ( ALPHA_BETA && minScore <= alpha ) {
                        if ( DEBUG ) {
                            System.out.println("HUMAN [Depth "+depth+"] ================== Alpha/Beta ( "+scoreToString(alpha)+" , "+scoreToString(beta)+" )");
                        }                        
                        break;
                    }
                }
            }
            
            if ( DEBUG ) {
                System.out.println("HUMAN [Depth "+depth+"] ================== Best move : "+best+" (rating: "+scoreToString(minScore)+")");
            }              
            
            return minScore;
        }
        
        private String scoreToString(int score) {
            switch(score) {
                case Board.WIN_SCORE:
                    return "WIN";
                case Board.LOSS_SCORE:
                    return "LOSS";
                case Board.DRAW_SCORE:
                    return "DRAW";
                default:
                    return Integer.toString(score);
            }
        }
        
        private boolean isTerminalScore(int score) {
            switch( score ) {
                case Board.WIN_SCORE:
                case Board.LOSS_SCORE:
                case Board.DRAW_SCORE:
                    return true;
            }
            return false;
        }        

        /**
         * Evaluate game state with regards to a specific player.
         * 
         * @param b
         * @param currentPlayer
         * @param player1
         * @param player2
         * @return 1 = win, -1 = loss, 0 = otherwise
         */
        private int evaluate() 
        {
            return MinMax.this.evaluate(board,toSolve,player1,player2);
        }        
    }

    public boolean hasWon(Board b,Player currentPlayer) 
    {
        // check rows
        return b.calculateScore( currentPlayer, TILES_TO_WIN ) == Board.WIN_SCORE;
    }
    
    public int evaluate(Board board, final Player currentPlayer,Player player1,Player player2) 
    {
        int score = board.calculateScore( currentPlayer, TILES_TO_WIN );
        if ( score == Board.WIN_SCORE ) {
            return score;
        }
        
        if ( hasWon( board , player1.equals(currentPlayer) ? player2: player1 ) ) {
            return Board.LOSS_SCORE;
        } 
        
        if ( board.isFull() ) {
            return Board.DRAW_SCORE;
        }
        return score;
    }        

}
