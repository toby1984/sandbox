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

public class SceneRenderer
{
    private final IScene scene;
    private final int sceneWidth;
    private final int sceneHeight;
    
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

    public void renderScene(Dimension canvas, Graphics2D graphics) 
    {
        final int screenWidth = canvas.width;
        final int screenHeight = canvas.height;
        
        final int xInc = screenWidth / sceneWidth;
        final int yInc = screenHeight / sceneHeight;
        
        graphics.setColor(Color.RED);
        
        // draw grid Y
        for ( int x = 0 ; x < sceneWidth ; x+=1) 
        {
            final int currentX=x*xInc ;
            graphics.drawLine( currentX, 0 , currentX, screenHeight );
        }
        
        // draw grid X
        for ( int y = 0 ; y < sceneHeight ; y+=1) 
        {
            final int currentY=y*yInc ;
            graphics.drawLine( 0, currentY , screenWidth , currentY );
        }        
        
        // draw grid
        for ( IScene.ISceneIterator it = scene.iterator() ; it.hasNext() ; ) 
        {
            if ( it.next() == IScene.OCCUPIED ) {
                graphics.fillRect( it.x() * xInc,it.y() * yInc , xInc , yInc );
            }
        }
        
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
                    graphics.fillRect( m.x * xInc, m.y * yInc , xInc , yInc );
                }
            }
        }
    }
    
    public int viewXToModel(int viewX,Dimension canvas) {
        final int xInc = canvas.width / sceneWidth;
        return viewX / xInc;
    }
    
    public int viewYToModel(int viewY,Dimension canvas) {
        final int yInc = canvas.height / sceneHeight;
        return viewY / yInc;
    }    
}
