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

import javax.swing.JPanel;

import de.codesourcery.sandbox.pathfinder.IScene.ISceneVisitor;

public class SceneRenderer
{
    private final IScene scene;
    private final int sceneWidth;
    private final int sceneHeight;
    
    private final JPanel panel;
    
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
    
    public SceneRenderer(IScene scene,JPanel panel)
    {
        this.scene = scene;
        this.panel =  panel;
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
        do 
        {
//            for ( int x = current.x1 ; x < current.x2 ; x++ ) 
//            {
//                for ( int y = current.y1 ; y < current.y2 ; y++ ) {
//                    l.add( new Marker(id,x,y , color ) );
//                }
//            }
            l.add( new Marker(id,current.x(),current.y() , color ) );
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

        final int xMax = (int) Math.floor( sceneWidth * xInc );
        final int yMax = (int) Math.floor( sceneHeight * yInc );

        graphics.setColor(Color.RED);

        if ( renderGrid ) 
        {
            // draw grid Y
            for ( int x = 0 ; x < sceneWidth ; x+=1) 
            {
                final int currentX=(int) Math.floor(x*xInc);
                graphics.drawLine( currentX, 0 , currentX, yMax);
            }
            
            // draw grid X
            for ( int y = 0 ; y < sceneHeight ; y+=1) 
            {
                final int currentY= (int) Math.floor(y*yInc);
                graphics.drawLine( 0, currentY , xMax  , currentY );
            }        
        }
        
        // draw grid
        final ISceneVisitor visitor = new ISceneVisitor() {
			
			@Override
			public void visit(int x, int y, byte cellStatus) 
			{
			    fillRect( x , y , xInc , yInc , graphics );
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
                    fillRect( m.x , m.y , xInc , yInc , graphics );
                }
            }
        }
    }
    
    private void fillRect(int x,int y , double xInc, double yInc , Graphics2D graphics) 
    {
        final double x1 = Math.floor( x * xInc );
        final double y1 = Math.floor( y * yInc);
        final double x2 = Math.floor((x+1) * xInc);
        final double y2 = Math.floor((y+1) * yInc);
        final int w = (int) Math.floor( x2 - x1 );
        final int h = (int) Math.floor( y2 - y1 );
        
        graphics.fillRect( (int) x1 , (int) y1 , w , h );           
    }
    
    public int viewXToModel(int viewX,Dimension canvas) {
        final double xInc = canvas.width / (double) sceneWidth;
        return (int) Math.floor( viewX / xInc );
    }
    
    public void drawRect(Color color,Rec2 rect) {
        
        final double xInc = panel.getWidth() / (double) sceneWidth;
        final double yInc = panel.getHeight() / (double) sceneHeight;
        
        final int x1 = rect.x1;
        final int y1 = rect.y1;
        
        final int x2 = rect.x2;
        final int y2= rect.y2;
        
        final double px1 = Math.floor( x1 * xInc );
        final double py1 = Math.floor( y1 * yInc);
        
        final double px2 = Math.floor( x2 * xInc);
        final double py2 = Math.floor( y2 * yInc);
        
        final int w = (int) Math.floor( px2 - px1 );
        final int h = (int) Math.floor( py2 - py1 );
        
        final Graphics2D graphics = (Graphics2D) panel.getGraphics();
        graphics.setColor(color);
        graphics.drawRect( (int) px1 , (int) py1 , w , h ); 
    }
    
    public int viewYToModel(int viewY,Dimension canvas) {
        final double yInc = canvas.height / (double) sceneHeight;
        return (int) Math.floor( viewY / yInc );
    }    
}
