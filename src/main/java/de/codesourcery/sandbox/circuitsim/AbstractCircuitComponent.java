package de.codesourcery.sandbox.circuitsim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.codesourcery.sandbox.pathfinder.Rec2;
import de.codesourcery.sandbox.pathfinder.Vec2;

public abstract class AbstractCircuitComponent implements ICircuitComponent
{
    private Orientation orientation;
    private final List<Port> ports;
    private Vec2 location;
    
    public AbstractCircuitComponent(int portCount,Orientation orientation) 
    {
        if (orientation == null) {
            throw new IllegalArgumentException("orientation must not be NULL.");
        }
        if ( portCount < 2 ) {
            throw new IllegalArgumentException("Invalid port count: "+portCount);
        }
        
        this.orientation = orientation;
        final List<Port> tmp = new ArrayList<>();
        for( int i = 0 ; i < portCount;i++) {
            tmp.add( new Port( this ) );
        }
        ports = Collections.unmodifiableList(tmp);
    }
    
    @Override
    public boolean hasOrientation(Orientation orientation)
    {
        return orientation.equals(this.orientation);
    }
    
    @Override
    public int width()
    {
        if ( hasOrientation(Orientation.HORIZONTAL) ) {
            return getWidth();
        }
        return getHeight();
    }
    
    @Override
    public int height()
    {
        if ( hasOrientation(Orientation.HORIZONTAL) ) {
            return getHeight();
        }        
        return getWidth();
    }
    
    protected abstract int getHeight();
    protected abstract int getWidth();
    
    @Override
    public Orientation getOrientation()
    {
        return orientation;
    }
    
    @Override
    public void flipOrientation()
    {
        this.orientation = orientation == Orientation.HORIZONTAL ? Orientation.VERTICAL : Orientation.HORIZONTAL;
    }
    
    @Override
    public final List<Port> getPorts()
    {
        return ports;
    }
    
    public void setLocation(Vec2 location)
    {
        if (location == null) {
            throw new IllegalArgumentException("location must not be NULL.");
        }
        this.location = location;
    }    

    @Override
    public Vec2 getLocation()
    {
        return location;
    }
    
    @Override
    public Rec2 getBounds()
    {
        return new Rec2( location.x , location.y , location.x + width() , location.y + height() );
    }
}
