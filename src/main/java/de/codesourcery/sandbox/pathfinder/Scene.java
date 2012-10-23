package de.codesourcery.sandbox.pathfinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import de.codesourcery.sandbox.pathfinder.QuadTree.QuadLeafNode;
import de.codesourcery.sandbox.pathfinder.QuadTree.QuadNode;
import de.codesourcery.sandbox.pathfinder.QuadTree.IVisitor;

public final class Scene implements IScene
{
    private final QuadTree<Byte> data;
    private final int width;
    private final int height;
    
    public Scene(int width,int height) 
    {
        if ( width < 1 || height < 1 ) {
            throw new IllegalArgumentException("Invalid width/height: "+width+" x "+height);
        }
        this.width = width;
        this.height = height;
        this.data = new QuadTree<Byte>( width , height );
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
    	final QuadLeafNode<Byte> value = data.getValue( x , y);
    	if ( value == null ) {
    	    return IScene.FREE;
    	}
        return value.getValue().byteValue();
    }

    @Override
    public IScene write(int x, int y, byte status)
    {
    	if ( status == IScene.FREE ) {
    		data.store(x,y, null );
    	} else {
    		data.store(x,y, Byte.valueOf( status) );
    	}
        return this;
    }
    
    @Override
    public void visitOccupiedCells(final ISceneVisitor cellVisitor)
    {
    	
    	final IVisitor<Byte> visitor = new IVisitor<Byte>() {

			@Override
			public boolean visit(QuadNode<Byte> node, int currentDepth) 
			{
				if ( node.isLeaf() ) {
					Byte value = ((QuadLeafNode<Byte>) node).getValue();
					cellVisitor.visit( node.x1 , node.y1 , value == null ? IScene.FREE : Byte.valueOf( value ) );
				}
				return true;
			}
		};
		data.visitPreOrder( visitor );
    }

    @Override
    public boolean isFree(int x, int y)
    {
        return read(x,y) == IScene.FREE;
    }

    public static void save(File file,IScene scene) throws IOException {
        save( new ObjectOutputStream( new FileOutputStream( file ) ) , scene );
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
            if ( value != IScene.FREE ) {
            	result.write( x , y , value );
            }
        }
        in.close();
        return result;
    }

}
