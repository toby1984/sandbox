package de.codesourcery.sandbox.pathfinder;


public final class Scene extends AbstractScene
{
    private final byte[] data;
    
    public Scene(int width,int height) 
    {
        super(width,height);
        this.data = new byte[width*height];
    }
    
    @Override
    public byte read(int x, int y)
    {
        return data[y*width+x];
    }

    @Override
    public IScene write(int x, int y, byte status)
    {
        data[y*width+x]=status;
        return this;
    }
    
    @Override
    public void visitOccupiedCells(final ISceneVisitor cellVisitor)
    {
        for ( int x = 0 ; x < width ; x++ ) {
            for ( int y = 0 ; y < height; y++ ) 
            {
                final byte val = data[ y*width + x ];
                if ( val != IScene.FREE ) {
                    cellVisitor.visit(x, y, val );
                }
            }            
        }
        
//    	final IVisitor<Byte> visitor = new IVisitor<Byte>() {
//
//			@Override
//			public boolean visit(QuadNode<Byte> node, int currentDepth) 
//			{
//				if ( node.isLeaf() ) {
//					Byte value = ((QuadLeafNode<Byte>) node).getValue();
//					cellVisitor.visit( node.x1 , node.y1 , value == null ? IScene.FREE : Byte.valueOf( value ) );
//				}
//				return true;
//			}
//		};
//		data.visitPreOrder( visitor );
    }

    @Override
    public boolean isFree(int x, int y)
    {
        return data[y*width+x] == IScene.FREE;
    }
}
