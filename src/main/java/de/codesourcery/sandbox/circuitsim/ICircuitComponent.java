package de.codesourcery.sandbox.circuitsim;

import java.util.List;

import de.codesourcery.sandbox.pathfinder.Rec2;
import de.codesourcery.sandbox.pathfinder.Vec2;

public interface ICircuitComponent
{
    public enum Orientation {
        HORIZONTAL,
        VERTICAL;
    }
    
    /**
     * 
     * @return location in MODEL coordinates.
     */
    public Vec2 getLocation();
    
    /**
     * 
     * @return bounds in MODEL coordinates.
     */
    public Rec2 getBounds();
    
    public List<Port> getPorts();
    
    public boolean hasOrientation(Orientation orientation);
    
    public Orientation getOrientation();
    
    public void flipOrientation();
    
    /**
     * 
     * @return width in MODEL units.
     */
    public int width();
    
    /**
     * 
     * @return height in MODEL units.
     */
    public int height();
}
