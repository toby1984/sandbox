package de.codesourcery.sandbox.boids;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.codesourcery.sandbox.pathfinder.Rec2D;
import de.codesourcery.sandbox.pathfinder.Vec2;
import de.codesourcery.sandbox.pathfinder.Vec2d;

public final class World
{
    private final BoidKDTree<Boid> tiles = new BoidKDTree<Boid>();
    private final List<Boid> allBoids = new ArrayList<>();
    
    public World() {
    } 
        
    public void add(Boid boid) {
    
        if (boid == null) {
            throw new IllegalArgumentException("boid must not be NULL.");
        }
        synchronized( allBoids ) {
            allBoids.add( boid );
        }
        final Vec2d loc = boid.getLocation();        
        synchronized( tiles ) 
        {
            tiles.add( loc.x , loc.y , boid );
        }
    }
        
    public interface IBoidVisitor 
    {
        public void visit(Boid boid);
    }
        
    public void visitAllBoids(IBoidVisitor visitor) 
    {
        for ( Boid b : allBoids ) {
            visitor.visit( b );
        }
    }
    
    public void visitBoids(double x , double y , double maxRadius,IBoidVisitor visitor) 
    {
        final List<Boid> closestNeighbours = tiles.findClosestNeighbours( x , y , maxRadius , 10 );
        for ( Boid b : closestNeighbours ) {
            visitor.visit( b );
        }
    }
    
    public List<Boid> getAllBoids()
    {
        return allBoids;
    } 
    
    public int getPopulation() {
        return this.allBoids.size();
    }    
    
}
