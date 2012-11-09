package de.codesourcery.sandbox.boids;

import de.codesourcery.sandbox.boids.Main.NeighborAggregator;
import de.codesourcery.sandbox.pathfinder.Rec2D;
import de.codesourcery.sandbox.pathfinder.Vec2d;

public final class Boid
{
    private final Vec2d acceleration;
    private final Vec2d location;
    private final Vec2d velocity;    
    
    public Boid(Vec2d position, Vec2d acceleration,Vec2d velocity)
    {
        this.location = position;
        this.acceleration = acceleration;
        this.velocity = velocity;
    }
    
    public Vec2d getVelocity()
    {
        return velocity;
    }
    
    public Vec2d getLocation()
    {
        return location;
    }
    
    public Vec2d getAcceleration()
    {
        return acceleration;
    }

    public Vec2d getNeighbourCenter() 
    {
       return location;
    }
    
    public void visitNeighbors(World world , double neighborRadius,NeighborAggregator visitor) 
    {
        final Vec2d pos = getNeighbourCenter();
        final Rec2D rect = new Rec2D( pos.x - neighborRadius , 
                pos.y - neighborRadius , 
                pos.x + neighborRadius , 
                pos.y + neighborRadius);
        
        world.visitBoids( rect , visitor );
    }
}
