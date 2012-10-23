package de.codesourcery.sandbox.pathfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


public final class PathFinder
{
    private final IScene scene;

    private final int sceneWidth;
    private final int sceneHeight;

    // nodes to check
    private final Map<PathNode,PathNode> openMap = new HashMap<>(2000);
    private final List<PathNode> openList = new ArrayList<>(2000);

    // nodes ruled out
    private final Set<PathNode> closeList = new HashSet<>();

    public static final class PathNode extends Rec2
    {
        public final PathNode parent;

        private int f;
        private int g;
        
        public PathNode(int x,int y) 
        {
            this(x,y,null);
        }

        public PathNode(int x,int y,PathNode parent) 
        {
            super(x,y,x+1,y+1);
            this.parent=parent;
        }

        public void f(int value) { this.f = value; }
        public void g(int value) { this.g = value; }

        public int f() { return f; }
        public int g() { return g;}

        public PathNode parent() { return parent; }

        public int x() 
        { 
            return x1;
        }
        
        public int y() 
        {
            return y1;
        }          
        
        public int getNodeCount() {
            int result =1;
            PathNode current = this.parent;
            while( current != null ) {
                result++;
                current = current.parent;
            }
            return result;
        }
        
        @Override
        public String toString()
        {
            final Stack<PathNode> stack = new Stack<>();

            PathNode current = this;
            do {
                stack.push( current );
                current = current.parent;
            } while( current != null );

            final StringBuilder builder = new StringBuilder();
            while ( ! stack.isEmpty() ) {
                final PathNode pop = stack.pop();
                builder.append( "[ "+pop.x()+" , "+pop.y()+" ("+pop.width()+" x "+pop.height()+") ]");
                if ( ! stack.isEmpty() ) {
                    builder.append(" -> ");
                }
            }
            return builder.toString();            
        }

    }

    public PathFinder(IScene scene) {
        this.scene=scene;
        this.sceneWidth = scene.getWidth();
        this.sceneHeight = scene.getHeight();
    }

    private void insert(PathNode node) 
    {
        openMap.put(node,node);
        
        final int f = node.f;
        
        for ( int i = 0 ; i < openList.size() ; i++ ) 
        {
            if ( f <= openList.get(i).f ) {
                openList.add(i,node);
                return;
            }
        }
        openList.add( node );
    }

    public PathNode findPath(PathNode start,PathNode target) 
    {
        if ( start.equals( target ) ) { // trivial case
            return start;
        }

        openMap.clear();
        openList.clear();
        closeList.clear();

        assignCost( start, target);
        
        closeList.add( start );

        PathNode current = start;
        while ( true ) 
        {
            findNeighbors( current , target );

            if ( openList.isEmpty() ) {
                return null;
            }

            final PathNode cheapestPath = openList.remove(0);
            openMap.remove( cheapestPath );
            
            closeList.add( cheapestPath );            

            if ( cheapestPath.equals( target ) ) 
            {
                return cheapestPath;
            }            
            current = cheapestPath;
        }
    }

    private void assignCost(PathNode current,PathNode target) 
    {
        final int movementCost = calcMovementCost(current);
        final int estimatedCost = calcEstimatedCost( current , target );
        current.f( movementCost + estimatedCost );
        current.g( movementCost );
        
//        System.out.println("Cost: ("+current+") = "+current.g+" (g) "+( current.f() - current.g() )+" (h) = "+current.f());
    }

    private int calcMovementCost(PathNode current)
    {
        int cost=0;
        PathNode tmp = current;

        while( tmp != null && tmp.parent != null ) 
        {
            // 9-3 = 6
            // 3 - 9 = -6
            final int deltaX = Math.abs( tmp.x() - tmp.parent.x()); // movement along x-axis
            final int deltaY = Math.abs( tmp.y() - tmp.parent.y()); // movement along y-axis
            
            cost += ( 10 * (deltaX + deltaY ) );
            tmp=tmp.parent;
        }
        return cost;
    }

    private int calcEstimatedCost(PathNode node,PathNode end) {

        final int x=node.x();
        final int y=node.y();

        final int xDelta = x > end.x() ? x - end.x() : end.x() - x;
        final int yDelta = y > end.y() ? y - end.y() : end.y() - y;
        return (xDelta + yDelta)*10;
    }    

    private static final Vec2 NORTH = new Vec2(0,-1);
    private static final Vec2 EAST  = new Vec2(1,0);
    private static final Vec2 SOUTH = new Vec2(0,1);
    
    private static final Vec2 NORTH_EAST = new Vec2(1,-1);    
    private static final Vec2 NORTH_WEST = new Vec2(-1,-1);     
    private static final Vec2 SOUTH_EAST = new Vec2(1,1);
    private static final Vec2 SOUTH_WEST = new Vec2(-1,1);     
    
    private static final Vec2 WEST = new Vec2(-1,0);

    public void findNeighbors(PathNode parent,PathNode target) 
    {    
        if ( parent.x() + 1 < sceneWidth ) {
            maybeAddNeighbor(parent, target, EAST );
        }

        if ( parent.x() - 1 >= 0 ) {
            maybeAddNeighbor(parent, target, WEST );
        }        

        if ( parent.y() + 1 < sceneHeight) {
            maybeAddNeighbor(parent, target, SOUTH );
        }        

        if ( parent.y() - 1 >= 0 ) {
            maybeAddNeighbor(parent, target, NORTH );
        }         
    }

    private void maybeAddNeighbor(PathNode parent, PathNode target, Vec2 movement)
    {
        final int newX = parent.x() + movement.x;
        final int newY = parent.y() + movement.y;
        
        if ( scene.isFree( newX, newY ) ) 
        {
            maybeAddNeighbor(parent,target, newX , newY );
        } 
        return;
    }

    private void maybeAddNeighbor(PathNode parent, PathNode target, int x, int y)
    {
        final PathNode newNode = new PathNode( x , y , parent );

        if ( ! closeList.contains(newNode) ) 
        {
            final PathNode existing = openMap.get(newNode);

            assignCost( newNode , target);

            if ( existing == null || newNode.g < existing.g ) // prefer shorter path
            {
                insert( newNode );
            } 
        }
    }
}