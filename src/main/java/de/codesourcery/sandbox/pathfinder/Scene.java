package de.codesourcery.sandbox.pathfinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.NoSuchElementException;

public final class Scene implements IScene
{
    private final byte[] data;
    private final int width;
    private final int height;
    
    public Scene(int width,int height) 
    {
        if ( width < 1 || height < 1 ) {
            throw new IllegalArgumentException("Invalid width/height: "+width+" x "+height);
        }
        this.width = width;
        this.height = height;
        this.data = new byte[ width * height ];
    }
    
    @Override
    public int getHeight()
    {
        return height;
    }

    @Override
    public int getWidth()
    {
        return width;
    }

    @Override
    public byte read(int x, int y)
    {
        return data[y*height+x];        
    }

    @Override
    public IScene write(int x, int y, byte status)
    {
        data[y*height+x] = status;
        return this;
    }

    @Override
    public ISceneIterator iterator()
    {
        return new ISceneIterator() {

            private int x = 0;
            private int y = 0;
            
            private int currentX = -1;
            private int currentY = -1;
            
            @Override
            public byte next()
            {
                if ( y >= height ) {
                    throw new NoSuchElementException();
                }
                final byte result = read(x,y);
                
                currentX = x;
                currentY = y;
                
                x++;
                if ( x == width ) {
                    x = 0;
                    y++;
                }
                return result;
            }

            @Override
            public int x()
            {
                if ( currentX == -1 ) {
                    throw new IllegalStateException("You need to call next() first");
                }
                return currentX;
            }

            @Override
            public int y()
            {
                if ( currentY == -1 ) {
                    throw new IllegalStateException("You need to call next() first");
                }                
                return currentY;
            }

            @Override
            public boolean hasNext()
            {
                return y < height;
            }
        };
    }

    @Override
    public boolean isFree(int x, int y)
    {
        return read(x,y) == IScene.FREE;
    }

    public static void save(File file,IScene scene) throws IOException {
        save( new ObjectOutputStream( new FileOutputStream( file ) ) , scene );
    }
    
    public static void save(ObjectOutputStream out,IScene scene) throws IOException
    {
        out.writeInt( scene.getWidth() );
        out.writeInt( scene.getHeight() );
        
        ISceneIterator iterator = scene.iterator();
        while( iterator.hasNext() ) {
            byte value = iterator.next();
            int x = iterator.x();
            int y = iterator.y();
            out.writeInt( x );
            out.writeInt( y );
            out.writeByte( value );
        }
        out.close();
    }

    public static IScene load(File file) throws IOException {
        return load( new ObjectInputStream( new FileInputStream( file ) ) );
    }
    
    public static IScene load(ObjectInputStream in) throws IOException
    {
        final int width = in.readInt();
        final int height = in.readInt();
        final Scene result = new Scene(width,height);
        
        while ( in.available() > 0 ) {

            int x = in.readInt();
            int y = in.readInt();
            byte value = in.readByte();
            result.write( x , y , value );
        }
        in.close();
        return result;
    }

}
