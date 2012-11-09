package de.codesourcery.sandbox.boids;

import de.codesourcery.sandbox.pathfinder.Vec2d;

public class Boid
{
    private Vec2d acceleration;
    private Vec2d location;
    private Vec2d velocity;    
    
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
    
    public void setVelocity(Vec2d velocity)
    {
        this.velocity = velocity;
    }

    public Vec2d getLocation()
    {
        return location;
    }
    
    public void setLocation(Vec2d position)
    {
        this.location = position;
    }

    public Vec2d getAcceleration()
    {
        return acceleration;
    }

    public void setAcceleration(Vec2d acceleration)
    {
        this.acceleration = acceleration;
    }
    
    public Vec2d getNeighbourRadiusCenter() {
        return location;
    }
}
