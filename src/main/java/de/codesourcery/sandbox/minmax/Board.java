package de.codesourcery.sandbox.minmax;

import java.awt.Color;

import org.apache.commons.lang.StringUtils;

public class Board 
{

    protected interface IBoardVisitor {
        public void visit(int x,int y , Player p);
    }
    
    private final int width;
    private final int height;
    
    private final Player[][] data;

    public Board(Board b) 
    {
        this.width = b.width;
        this.height = b.height;
        this.data = createArray(b.width , b.height);
        for(int i = 0 ; i < width ; i++ ) {
            System.arraycopy( b.data[i] , 0 , this.data[i] , 0 , height );
        }
    }
    
    public Board(int width, int height)
    {
        this.width = width;
        this.height = height;
        
        data = createArray(width,height);
    }
    
    public boolean isFull() 
    {
        int count = 0;
        for ( int x = 0 ; x < width ; x++ ) {
            for ( int y = 0 ; y < height ; y++ ) {
                if ( data[x][y] != null ) {
                    count++;
                }
            }
        }
        return count == width*height;
    }

    public String toString()
    {
        return toString(1000,1000,"");
    }
    
    public static void main(String[] args)
    {
        
        Board b = new Board(4,4);
        Player human = new Player("human",Color.RED);
        Player computer = new Player("computer",Color.BLACK);
        
        b.set(0,0,human);
        b.set(2,0,computer);
        b.set(0,1,human);
        b.set(1,1,computer);
        b.set(0,2,computer);
        b.set(1,3,human);
        
        System.out.println( b );
        
        System.out.println("Computer won: "+new MinMax().hasWon( b , computer ) );
        System.out.println("Human    won: "+new MinMax().hasWon( b , human ) );
    }
    
    public String toString(int expectedX,int expectedY,String value)
    {
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
            if ( (y+1) < height ) 
            {
                builder.append("\n+---+---+---+");
                builder.append("\n");
            }
        }
        return builder.toString();
    } 
    
    
    /**
     * Diagonal:
     * 
     * <pre>
     *    X
     *   X 
     * X
     * </pre>
     * @param p
     * @return
     */
    public int countMaxSuccessiveTilesInDiagonal1(Player p,int requiredToWin) 
    {
        int maxValue = 0;
        for ( int y = height - 1 ; y >= 0 ; y-- )
        {        
            int i = y;
            int count = 0;
            for ( int x = 0 ; x < width && i >= 0 ; x++,i-- ) 
            {
                Player actual = data[x][i];
                if ( actual != null && actual.equals( p ) ) 
                {
                    count++;
                    if ( count == requiredToWin ) {
                        return count;
                    }
                    if ( count > maxValue ) {
                        maxValue = count;
                    }
                } else {
                    count = 0;
                }                
            }
        }
        
        for ( int x = 0 ; x < width ; x++ )
        {        
            int i = x;
            int count = 0;
            for ( int y = height-1 ; y >= 0 && i < width ; i++,y-- ) 
            {
                Player actual = data[i][y];
                if ( actual != null && actual.equals( p ) ) {
                    count++;
                    if ( count == requiredToWin ) {
                        return count;
                    }
                    if ( count > maxValue ) {
                        maxValue = count;
                    }
                } else {
                    count = 0;
                }                
            }
        }        
        
        return maxValue;
    }
    
    /**
     * Diagonal 2:
     * 
     * <pre>
     * X
     *   X
     *     X
     * </pre>
     * @param p
     * @return
     */
    public int countMaxSuccessiveTilesInDiagonal2(Player p,int requiredToWin) {

        int maxValue = 0;
        
        for ( int x = 0 ; x < width ; x++ )
        {        
            int i = x;
            int count = 0;
            for ( int y = 0 ; y < height && i < width ; i++,y++ ) 
            {
                Player actual = data[i][y];
                if ( actual != null && actual.equals( p ) ) {
                    count++;
                    if ( count == requiredToWin ) {
                        return count;
                    }
                    if ( count > maxValue ) {
                        maxValue = count;
                    }
                } else {
                    count = 0;
                }                
            }
        }   
        
        for ( int y = 0 ; y < height ; y++ )
        {        
            int i = y;
            int count = 0;
            for ( int x = 0 ; x < width && i < height ; x++,i++ ) 
            {
                Player actual = data[x][i];
                if ( actual != null && actual.equals( p ) ) 
                {
                    count++;
                    if ( count == requiredToWin ) {
                        return count;
                    }
                    if ( count > maxValue ) {
                        maxValue = count;
                    }
                } else {
                    count = 0;
                }                
            }
        }
        
        return maxValue;
    }    
    
    public int countMaxSuccessiveTilesInRow(Player p,int requiredToWin) 
    {
        int maxValue = 0;
        for ( int y = 0 ; y < height; y++ ) 
        {
            int count = 0;
            for ( int x = 0 ; x < width ; x++ ) {
                Player actual = data[x][y];
                if ( actual != null && actual.equals( p ) ) {
                    count++;
                    if ( count == requiredToWin ) {
                        return count;
                    }
                    if ( count > maxValue ) {
                        maxValue = count;
                    }                    
                } else {
                    count = 0;
                }
            }
        }
        return maxValue;
    }
    
    public int countMaxSuccessiveTilesInColumn(Player p,int requiredToWin) 
    {
        int maxValue = 0;
        for ( int x = 0 ; x < width ; x++ )         
        {
            int count = 0;
            for ( int y = 0 ; y < height; y++ ) {
                Player actual = data[x][y];
                if ( actual != null && actual.equals( p ) ) {
                    count++;
                    if ( count == requiredToWin ) {
                        return count;
                    }
                    if ( count > maxValue ) {
                        maxValue = count;
                    }                    
                } else {
                    count = 0;
                }
            }
        }
        return maxValue;
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

    public void set(int x,int y,Player player) {
        data[x][y] = player;
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

    public void clear(int x, int y)
    {
        data[x][y]=null;
    }
}