package de.codesourcery.sandbox.pathfinder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import de.codesourcery.sandbox.pathfinder.IScene.ISceneVisitor;

public class SceneRenderer
{
    private final IScene scene;
    private final int sceneWidth;
    private final int sceneHeight;
    
    private boolean renderGrid = false;
    
    // @GuardedBy( "markers" )
    private final Map<Long,List<Marker>> markers = new HashMap<>();
    
    private final AtomicLong markerId = new AtomicLong(0);
    
    public static final class Marker 
    {
        public final long id;
        public final Color color;
        public final int x;
        public final int y;
        
        public Marker(long id,int x, int y,Color color)
        {
            this.id=id;
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }
    
    public void setRenderGrid(boolean renderGrid)
    {
        this.renderGrid = renderGrid;
    }
    
    protected SceneRenderer(IScene scene)
    {
        this.scene = scene;
        sceneWidth = scene.getWidth();
        sceneHeight = scene.getHeight();
    }
    
    public long addMarker(int x, int y,Color color) 
    {
        final long id = markerId.incrementAndGet();
        final Marker marker = new Marker(id , x, y, color);
        
        synchronized( markers ) 
        {
            markers.put( id , Collections.singletonList( marker ) );
        }
        return id;
    }
    
    public void clearMarker(long markerId) 
    {
        synchronized( markers ) 
        {
            markers.remove( markerId );
        }
    }
    
    public void clearAllMarkers()
    {
        synchronized( markers ) 
        {
            markers.clear();
        }
    }    
    
    public long addMarkers(PathFinder.PathNode path,Color color) 
    {
        final long id = markerId.incrementAndGet();
        PathFinder.PathNode current = path;
        final List<Marker> l = new ArrayList<>();
        do {
            l.add( new Marker(id,current.x , current.y , color ) );
            current = current.parent;
        } while ( current != null );
        
        synchronized( markers ) 
        {
            markers.put( id , l );
        }        
        return id;
    }

    public void renderScene(Dimension canvas, final Graphics2D graphics) 
    {
        final int screenWidth = canvas.width;
        final int screenHeight = canvas.height;
        
        final double xInc = screenWidth / (double) sceneWidth;
        final double yInc = screenHeight / (double) sceneHeight;

        final int xMax = (int) Math.round( sceneWidth * xInc );
        final int yMax = (int) Math.round( sceneHeight * yInc );

        graphics.setColor(Color.RED);

        if ( renderGrid ) 
        {
            // draw grid Y
            for ( int x = 0 ; x < sceneWidth ; x+=1) 
            {
                final int currentX=(int) Math.round(x*xInc);
                graphics.drawLine( currentX, 0 , currentX, yMax);
            }
            
            // draw grid X
            for ( int y = 0 ; y < sceneHeight ; y+=1) 
            {
                final int currentY= (int) Math.round(y*yInc);
                graphics.drawLine( 0, currentY , xMax  , currentY );
            }        
        }
        
        // draw grid
        final ISceneVisitor visitor = new ISceneVisitor() {
			
			@Override
			public void visit(int x, int y, byte cellStatus) 
			{
                graphics.fillRect( (int) Math.round(x * xInc),(int) Math.round( y * yInc ), (int) Math.round(xInc), (int) Math.round(yInc) );				
			}
		};
		scene.visitOccupiedCells( visitor );
        
        // draw markers
        synchronized( markers ) 
        {
            for ( List<Marker> list : markers.values() ) 
            {
                Color lastColor = null;
                for ( Marker m : list ) 
                {
                    if ( lastColor == null || ! lastColor.equals(m.color)) {
                        graphics.setColor( m.color );
                        lastColor = m.color;
                    }
                    graphics.fillRect( (int) Math.round(m.x * xInc), (int) Math.round(m.y * yInc) , (int) Math.round(xInc) , (int) Math.round(yInc) );
                }
            }
        }
    }
    
    public int viewXToModel(int viewX,Dimension canvas) {
        final double xInc = canvas.width / (double) sceneWidth;
        return (int) Math.round( viewX / xInc );
    }
    
    public int viewYToModel(int viewY,Dimension canvas) {
        final double yInc = canvas.height / (double) sceneHeight;
        return (int) Math.round( viewY / yInc );
    }    
}
