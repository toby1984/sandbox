package de.codesourcery.sandbox.pathfinder;

import de.codesourcery.sandbox.pathfinder.QuadTree.IVisitor;
import de.codesourcery.sandbox.pathfinder.QuadTree.QuadLeafNode;
import de.codesourcery.sandbox.pathfinder.QuadTree.QuadNode;

public final class QuadTreeScene extends AbstractScene
{
    private final QuadTree<Byte> data;
    
    public QuadTreeScene(int width,int height) 
    {
        super(width,height);
        this.data = new QuadTree<Byte>(width,height);
    }
    
    @Override
    public byte read(int x, int y)
    {
        final QuadLeafNode<Byte> value = data.getValue(x, y);
        if ( value == null ) {
            return IScene.FREE;
        }
        Byte val = value.getValue();
        return val == null ? IScene.FREE : val;
    }

    @Override
    public IScene write(int x, int y, byte status)
    {
        if ( status != IScene.FREE ) {
            data.store(x,y,status);
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
        return ! data.containsValues( x , y , 1 , 1 );
    }
}
