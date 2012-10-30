package de.codesourcery.sandbox.minmax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MinMax
{
    private static final int NEG_INFINITY = -10000;    
    private static final int POS_INFINITY =  10000;
    
    public static final int TILES_TO_WIN = 3;
    
    private static final int PLAYER_WON = 0xdeadbeef;
    
    private static final int MAX_DEPTH = 5;
    
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
            return "Move[ "+x+" , y "+"]";
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
        
        private Move bestMove;

        public MinMaxImpl(Board board,Player player1,Player player2) {
            this.board = board;
            this.player1 = player1;
            this.player2 = player2;
        }

        public Move findMove(Player currentPlayer) 
        {
            bestMove = null;
            miniMax( currentPlayer , 0 , NEG_INFINITY-1 , POS_INFINITY );
            return bestMove;
        }

        private Player otherPlayer(Player current) 
        {
            return current.equals(player1) ? player2 : player1;
        }
        
        private int miniMax(final Player currentPlayer,int depth, int alpha,int beta) 
        {
            if ( depth > MAX_DEPTH ) {
                return evaluate( currentPlayer );
            }
            
            final List<Move> possibleMoves = findLegalMoves(currentPlayer );

            if ( possibleMoves.isEmpty() )
            {
                return evaluate( currentPlayer );
            }
            
            // sort moves 
//            sortPossibleMoves(currentPlayer, possibleMoves);
            
            if ( depth == 0 ) {
                bestMove = possibleMoves.get(0);
            }
            
            int localAlpha = NEG_INFINITY;
            
            for ( Move next : possibleMoves ) 
            {
                next.apply(board,currentPlayer);
                int rating = -miniMax( otherPlayer( currentPlayer ), depth+1 , -beta, -alpha );
                next.undo( board );
                
               if (rating > localAlpha) 
               {
                   localAlpha = rating;
                   
                   if ( rating > alpha )
                   {
                       alpha = rating;
                   }
                   
                   if ( depth == 0 ) {
                       bestMove = next;
                   }
                   
                   if (alpha >= beta) {
//                     System.out.println( "Pruning for "+currentPlayer+" , score: "+wert+" , alpha = "+alpha+" , beta = "+beta);
//                     System.out.println( board );                       
                     return rating;
                   }
               } 
            }
            return localAlpha;
        }

        private void sortPossibleMoves(final Player currentPlayer, final List<Move> possibleMoves)
        {
            final Comparator<Move> c = new Comparator<Move>() {

                @Override
                public int compare(Move o1, Move o2)
                {
                    o1.apply( board , currentPlayer );
                    int score1 = evaluate( currentPlayer );
                    o1.undo( board );
                    
                    o2.apply( board , currentPlayer );
                    int score2 = evaluate( currentPlayer );
                    o2.undo( board );
                    
                    if ( score1 > score2 ) {
                        return -1;
                    } else if ( score1 < score2) {
                        return 1;
                    }
                    return 0;
                }
            };
            
            Collections.sort(possibleMoves,c);
        }
        
        private List<Move> findLegalMoves(Player currentPlayer) 
        {
            if ( hasWon(board,currentPlayer) || hasWon(board,otherPlayer(currentPlayer) ) ) 
            {
                return Collections.emptyList();
            }
            
            List<Move> result = new ArrayList<>();
            for ( int x = 0 ; x < board.width() ; x++ ) {
                for ( int y = 0 ; y < board.height() ; y++ ) 
                {
                    if ( board.read(x,y) == null ) {
                        result.add( new Move(x,y) );
                    }
                }
            }
            return result;
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
        public int evaluate(Player currentPlayer) 
        {
            return MinMax.this.evaluate(board,currentPlayer,player1,player2);
        }        
        
    }
    
    public boolean hasWon(Board b,Player currentPlayer) 
    {
        // check rows
        if ( b.countMaxSuccessiveTilesInRow( currentPlayer , TILES_TO_WIN ) >= TILES_TO_WIN ) {
            return true;
        }

        // check columns
        if ( b.countMaxSuccessiveTilesInColumn( currentPlayer , TILES_TO_WIN ) >= TILES_TO_WIN ) {
            return true;
        }

        // check diagonals #1
        if ( b.countMaxSuccessiveTilesInDiagonal1( currentPlayer , TILES_TO_WIN ) >= TILES_TO_WIN ) {
            return true;
        }     
        
        // check diagonals #2
        if ( b.countMaxSuccessiveTilesInDiagonal2( currentPlayer , TILES_TO_WIN ) >= TILES_TO_WIN ) 
        {
            return true;
        }     
        return false;
    }

    public int evaluate(Board board, Player currentPlayer,Player player1,Player player2) 
    {
        if ( hasWon( board , currentPlayer.equals(player1) ? player2: player1) ) {
            return NEG_INFINITY;
        }
        
        int sum = sumMaxSuccessiveTilesInARow(board, currentPlayer);
        
        if ( sum == PLAYER_WON ) 
        {
            return POS_INFINITY;
        }

        return sum;
    }        
    
    private int sumMaxSuccessiveTilesInARow(Board board,Player currentPlayer) 
    {
        int sum = 0;
        // check rows
        int count = board.countMaxSuccessiveTilesInRow( currentPlayer , TILES_TO_WIN );
        if ( count >= TILES_TO_WIN ) {
            return PLAYER_WON;
        }
        sum += count;
        
        // check columns
        count = board.countMaxSuccessiveTilesInColumn( currentPlayer , TILES_TO_WIN );
        if ( count >= TILES_TO_WIN ) {
            return PLAYER_WON;
        }            
        sum += count;
        
        // check diagonals #1
        count = board.countMaxSuccessiveTilesInDiagonal1( currentPlayer , TILES_TO_WIN );
        if ( count >= TILES_TO_WIN ) {
            return PLAYER_WON;
        }            
        sum += count;
        
        // check diagonals #2
        count = board.countMaxSuccessiveTilesInDiagonal2( currentPlayer , TILES_TO_WIN );
        if ( count >= TILES_TO_WIN ) {
            return PLAYER_WON;
        }            
        sum += count;
        
        return sum;        
    }     
}
