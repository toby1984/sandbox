package de.codesourcery.sandbox.pathfinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.sandbox.pathfinder.Edge.EdgeOption;
import de.codesourcery.sandbox.pathfinder.Edge.IntersectionResult;
import de.codesourcery.sandbox.pathfinder.Edge.PointOption;

public class Polygon implements IPolygon
{
    private final List<Edge> edges = new ArrayList<Edge>();

    private Rec2 boundingBox;
    
    public Polygon() {
    }
    
    @Override
    public String toString()
    {
        return "Polygon[ "+StringUtils.join( this.edges , " | ")+" ]";
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if ( obj instanceof IPolygon) 
        {
            List<Edge> otherEdges = ((IPolygon) obj).getEdges();
            
            if ( this.getEdges().size() != otherEdges.size() ) {
                System.out.println("Edge count mismatch");
                return false;
            }
outer:            
            for ( Edge thisEdge : getEdges() ) 
            {

                for ( Edge otherEdge : otherEdges ) {
                    if ( thisEdge.equals( otherEdge ) ) {
                        continue outer;
                    }
                }
                return false;
            }
            return true;
        }
        return false;
    }
    
    public Polygon(List<Edge> edges) 
    {
        this.edges.addAll( edges );
        recalcBoundingBox();
    }    
    
    public void addEdges(List<Edge> list) {
        this.edges.addAll( list );
        recalcBoundingBox();
    }
    public void addEdge(Edge e) {
        edges.add( e );
        recalcBoundingBox();
    }
    
    private void recalcBoundingBox() {

        Iterator<Edge> it = edges.iterator();
        Edge e = it.next();
        
        int minX= e.x1 < e.x2 ? e.x1 : e.x2;
        int maxX= e.x1 > e.x2 ? e.x1 : e.x2;
                
        int minY= e.y1 < e.y2 ? e.y1 : e.y2;
        int maxY= e.y1 > e.y2 ? e.y1 : e.y2;
        
        while( it.hasNext() ) 
        {
            e = it.next();
            
            if ( e.x1 < minX ) {
                minX = e.x1;
            }
            if ( e.x2 < minX ) {
                minX = e.x2;
            }    
            if ( e.y1 < minY ) {
                minY = e.y1;
            }
            if ( e.y2 < minY ) {
                minY = e.y2;
            }   

            if ( e.x1 > maxX ) {
                maxX = e.x1;
            }
            if ( e.x2 > maxX ) {
                maxX = e.x2;
            }    
            if ( e.y1 > maxY ) {
                maxY = e.y1;
            }
            if ( e.y2 > maxY ) {
                maxY = e.y2;
            }             
        }
        
        // rectangle excludes it's boundary x2/y2 , thus I add +1
        boundingBox = new Rec2(minX,minY,maxX+1,maxY+1);
    }
    
    @Override
    public boolean containsPoint(int x, int y)
    {
        if ( boundingBox == null || ! boundingBox.contains( x,  y ) ) 
        {
            return false;
        }
        
        // cast a ray starting at (x,y) and count the number of intersections with lines of this polygon
        int intersectionCount = 0;
        
        Edge ray;
        if ( x <= boundingBox.x2 ) {
            ray = new Edge(x,y,boundingBox.x2+1 , y );
        } else {
            ray = new Edge(boundingBox.x1-1,y, x , y );            
        }
        for ( Edge e : edges ) 
        {
            final IntersectionResult intersection = e.calcIntersection( ray );
            if ( ! intersection.isEmpty() ) {
                intersectionCount++;
            }
        }
        // even number => outside
        // odd number => inside
        return (intersectionCount % 2 ) != 0; 
    }
    
    public void merge(IPolygon polygon) 
    {
        if ( ! getBoundingBox().overlap( polygon.getBoundingBox() ) ) {
            throw new IllegalArgumentException( this+" does not overlap "+polygon);
        }
        
        final Set<Edge> toRemove = new HashSet<>();
        final List<Edge> toAdd = new ArrayList<>();
        
        for ( Edge otherEdge : polygon.getEdges() ) 
        {
            int intersectCount = 0;
            for ( Edge thisEdge : edges ) 
            {
                final IntersectionResult result = thisEdge.calcIntersection( otherEdge );
                System.out.println( otherEdge +" <-> "+thisEdge+" => "+result);
                if ( result.isEmpty() ) 
                {
                    continue;
                } 
                else if ( result.isPoint() ) 
                {
                    final PointOption point = result.asPointOption();
                    if ( ! containsPoint( otherEdge.x1 , otherEdge.y1 ) ) {
                        toAdd.add( new Edge( otherEdge.x1 , otherEdge.y1 , point.x ,point.y ) );
                    } else {
                        toAdd.add( new Edge( otherEdge.x2 , otherEdge.y2 , point.x ,point.y ) );
                    }
                    if ( ! polygon.containsPoint( thisEdge.x2 , thisEdge.y2 ) ) {
                        toRemove.add( thisEdge );
                        toAdd.add( new Edge( point.x , point.y , thisEdge.x2 , thisEdge.y2 ) );
                    } else  if ( ! polygon.containsPoint( thisEdge.x1 , thisEdge.y1 ) ) {
                        toRemove.add( thisEdge );
                        toAdd.add( new Edge( point.x , point.y , thisEdge.x1 , thisEdge.y1 ) );
                    }
                    intersectCount++;
                } 
                else 
                {
                    intersectCount++;
                    
                    // line intersection , see which part of the other edge was actually overlapping with this edge
                    final EdgeOption resultEdge = result.asEdgeOption();
                    
                    switch( resultEdge.getOverlapType() ) 
                    {
                        case FULLY_CONTAINED:
                            // nothing to do here
                            break;
                        case START:
                            toAdd.add( new Edge( resultEdge.x2 , resultEdge.y2 , otherEdge.x2 , otherEdge.y2 ) );
                            break;
                        case MIDDLE:
                            toAdd.add( new Edge( otherEdge.x1 , otherEdge.y1 , resultEdge.x1 , resultEdge.y1 ) );
                            toAdd.add( new Edge( resultEdge.x2 , resultEdge.y2, otherEdge.x2 , otherEdge.y2 ) );
                            break;
                        case END:
                            toAdd.add( new Edge( otherEdge.x1 , otherEdge.y1 , resultEdge.x1 , resultEdge.y1 ) );
                            break;
                        default:
                            throw new RuntimeException("Unhandled switch/case: "+resultEdge.getOverlapType());
                    }
                    
                }
            }
            
            if ( intersectCount == 0 ) {
                toAdd.add( otherEdge );
            }
        }
        
        this.edges.removeAll( toRemove );
        this.edges.addAll( toAdd );
        recalcBoundingBox();
    }
        
    @Override
    public List<Edge> getEdges()
    {
        return edges;
    }

    @Override
    public Rec2 getBoundingBox()
    {
        return boundingBox;
    }

    @Override
    public boolean containsLine(int x1, int y1, int x2, int y2)
    {
        return containsPoint(x1,y1) && containsPoint(x2,y2);
    }
}