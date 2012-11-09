package de.codesourcery.sandbox.boids;

import java.util.ArrayList;
import java.util.List;

import de.codesourcery.sandbox.pathfinder.Rec2D;
import de.codesourcery.sandbox.pathfinder.Vec2;
import de.codesourcery.sandbox.pathfinder.Vec2d;

public final class World
{
    private final int tileCount;
    private final double xInc;
    private final double yInc;
    
    private final Tile[][] tiles;
    
    private final List<Boid> allBoids = new ArrayList<>();
    
    protected static final class Tile extends Rec2D {

        private final List<Boid> boids;
        
        public Tile(double x1, double y1, double x2, double y2)
        {
            super(x1, y1, x2, y2);
            boids = new ArrayList<>();
        }
    }
    
    public World(double maxCoordinates, int tileCount) {
        
        this.tileCount = tileCount;
        
        xInc = maxCoordinates / tileCount;
        yInc = maxCoordinates / tileCount;
        
        double modelX = 0;
        double modelY = 0;
        
        tiles = new Tile[ tileCount ][];
        for ( int i = 0 ; i < tileCount ; i++ ) {
            tiles[i] = new Tile[ tileCount ];
        }
        
        for ( int x = 0 ; x < tileCount ; x++ , modelX += xInc ) 
        {
            for ( int y = 0 ; y < tileCount ; y++ , modelY += yInc) {
                tiles[x][y] = new Tile( modelX , modelY , modelX + xInc , modelY + yInc );
            }
        }
    } 
        
    public void add(Boid boid) {
    
        if (boid == null) {
            throw new IllegalArgumentException("boid must not be NULL.");
        }
        final Vec2 coords = modelToTile( boid.getLocation() );
        synchronized( allBoids ) {
            allBoids.add( boid );
        }
        synchronized(tiles[coords.x][coords.y]) {
            tiles[coords.x][coords.y].boids.add( boid );
        }
    }
        
    public interface IBoidVisitor 
    {
        public void visit(Boid boid);
    }
        
    public void visitAllBoids(IBoidVisitor visitor) {
        for ( Boid b : allBoids ) {
            visitor.visit( b );
        }
    }
    
    public void visitBoids(Rec2D modelRect,IBoidVisitor visitor) {
        
        Vec2 p1 = modelToTile( modelRect.x1 , modelRect.y1 );
        Vec2 p2 = modelToTile( modelRect.x2 , modelRect.y2 );
        
        final int xMin;
        final int xMax;
        if ( p1.x < p2.x ) {
            xMin = p1.x;
            xMax = p2.x;
        } else {
            xMin = p2.x;
            xMax = p1.x;            
        }
        
        final int yMin;
        final int yMax;
        if ( p1.y < p2.y ) {
            yMin = p1.y;
            yMax = p2.y;
        } else {
            yMin = p2.y;
            yMax = p1.y;            
        }        
        
        for ( int x = xMin ; x <= xMax ; x++ ) 
        {
            final int tileX;
            if ( x < 0 ) {
                tileX = tileCount+x;
            } else if ( x >= tileCount ) {
                tileX = x-tileCount;
            } else {
                tileX = x;
            }            
            for ( int y = yMin ; y <= yMax ; y++ ) 
            {
                final int tileY;
                if ( y < 0 ) {
                    tileY = tileCount+y;
                } else if ( y >= tileCount ) {
                    tileY = y-tileCount;
                } else {
                    tileY = y;
                }                
                for ( Boid b : tiles[tileX][tileY].boids ) {
                    visitor.visit( b );
                }
            }
        }
    }
    
    public List<Boid> getAllBoids()
    {
        return allBoids;
    } 
    
    public int getPopulation() {
        return this.allBoids.size();
    }    
    
    public Vec2 modelToTile(Vec2d modelCoords) {
        final int x = (int) (modelCoords.x / xInc);
        final int y = (int) (modelCoords.y / yInc);
        return new Vec2(x,y);
    }
    
    public Vec2 modelToTile( double modelX,double modelY) {
        final int x = (int) (modelX / xInc);
        final int y = (int) (modelY / yInc);
        return new Vec2(x,y);
    }    
}
