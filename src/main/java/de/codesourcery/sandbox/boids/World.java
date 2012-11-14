package de.codesourcery.sandbox.boids;

import java.util.ArrayList;
import java.util.List;

import de.codesourcery.sandbox.pathfinder.Vec2d;

public final class World
{
    private final BoidKDTree<Boid> tree = new BoidKDTree<Boid>();
    private final List<Boid> allBoids = new ArrayList<>();
    
    public World() {
    } 
    
    public void add(Boid boid) 
    {
        synchronized( allBoids ) {
        	allBoids.add( boid );
        }
        
        final Vec2d loc = boid.getLocation();        
        tree.add( loc.x , loc.y , boid );
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
        final List<Boid> closestNeighbours = tree.findClosestNeighbours( x , y , maxRadius , 10 );
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
