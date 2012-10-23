package de.codesourcery.sandbox.pathfinder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public abstract class AbstractScene implements IScene
{
    protected final int width;
    protected final int height;
    
    public static IScene createInstance(int width,int height) {
        return new Scene(width,height);
    }
    
    protected AbstractScene(int width,int height) 
    {
        if ( width < 1 || height < 1 ) {
            throw new IllegalArgumentException("Invalid width/height: "+width+" x "+height);
        }
        this.width = width;
        this.height = height;
    }
    
    @Override
    public final int getHeight()
    {
        return height;
    }

    @Override
    public final int getWidth()
    {
        return width;
    }

    public static void save(final ObjectOutputStream out,IScene scene) throws IOException
    {
        out.writeInt( scene.getWidth() );
        out.writeInt( scene.getHeight() );
        
        final ISceneVisitor visitor = new ISceneVisitor() {
            
            @Override
            public void visit(int x, int y, byte value) 
            {
                try {
                    out.writeInt( x );
                    out.writeInt( y );
                    out.writeByte( value );                     
                } 
                catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        };
        scene.visitOccupiedCells( visitor );
    }

    public static IScene load(ObjectInputStream in) throws IOException
    {
        final int width = in.readInt();
        final int height = in.readInt();
        final IScene result = createInstance(width,height);
        
        while ( in.available() > 0 ) {

            int x = in.readInt();
            int y = in.readInt();
            byte value = in.readByte();
            if ( value != IScene.FREE ) {
                result.write( x , y , value );
            }
        }
        return result;
    }

}
