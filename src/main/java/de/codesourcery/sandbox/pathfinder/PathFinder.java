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
    private final List<PathNode> openList=new ArrayList<>();
    private final Map<PathNode,PathNode> openMap=new HashMap<>();
    
    // nodes ruled out
    private final Set<PathNode> closeList = new HashSet<>();
    
    public static final class PathNode 
    {
        public PathNode parent;
        public final int x;
        public final int y;
        
        private int f;
        private int g;
        
        public PathNode(int x,int y) 
        {
            this(x,y,null);
        }
        
        public PathNode(int x,int y,PathNode parent) 
        {
            this.parent=parent;
            this.x = x;
            this.y = y;
        }

        public void f(int value) { this.f = value; }
        public void g(int value) { this.g = value; }
        
        public int f() { return f; }
        public int g() { return g;}
        
        public PathNode parent() { return parent; }
        public void setParent(PathNode parent) { this.parent = parent; }
        public int x() { return x; }
        public int y() { return y; }
        
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
                builder.append( "[ "+pop.x+" , "+pop.y+" ]");
                if ( ! stack.isEmpty() ) {
                    builder.append(" -> ");
                }
            }
            return builder.toString();            
        }
        
        @Override
        public int hashCode()
        {
            return 31 * (31 + x) + y;
        }
        
        @Override
        public boolean equals(Object obj)
        {
        	if ( obj == this ) {
        		return true;
        	}
            if (obj.getClass() == PathNode.class )
            {
                final PathNode other = (PathNode) obj;
                return x == other.x && y == other.y;
            }
            return false;
        }
    }
    
    public static void main(String[] args)
    {
        final Scene scene = new Scene(7,5);
        scene.write( 3 , 1 , IScene.OCCUPIED )
             .write( 3 , 2 , IScene.OCCUPIED )
             .write( 3 , 3 , IScene.OCCUPIED );
                
        final PathNode start = new PathNode(1,2);
        final PathNode end = new PathNode(5,2);
        
        final PathFinder finder = new PathFinder( scene );
        final PathNode result = finder.findPath(start,end);
        System.out.println("Result: "+result);
    }
    
    public PathFinder(IScene scene) {
        this.scene=scene;
        this.sceneWidth = scene.getWidth();
        this.sceneHeight = scene.getHeight();
    }
    
    private void insert(PathNode node) {
    	
    	openMap.put(node,node);
    	
    	int len = openList.size();
    	for ( int i = 0 ; i < len ; i++ ) {
    		if ( node.f() <= openList.get(i).f() ) {
    			openList.add( i , node );
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
        
        openList.clear();
        openMap.clear();
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
            
            final PathNode cheapestPath = openList.remove( 0 );
            
            closeList.add( cheapestPath );            
            
            if ( cheapestPath.equals( target ) ) 
            {
                return cheapestPath;
            }            
            current = cheapestPath;
        }
    }
    
    private void assignCost(PathNode current,PathNode target) {
        final int movementCost = calcMovementCost(current);
        final int estimatedCost = calcEstimatedCost( current , target );
        current.f( movementCost + estimatedCost );
        current.g( movementCost );
    }

    private int calcMovementCost(PathNode current)
    {
        int cost=0;
        PathNode tmp = current;
        
        while( tmp != null && tmp.parent != null ) 
        {
            final boolean movedX = tmp.x != tmp.parent.x; // movement along x-axis
            final boolean movedY = tmp.y != tmp.parent.y; // movement along y-axis
            if ( ( movedX && ! movedY ) || (! movedX && movedY) ) {
                cost+=10; // only moved in either X _OR_ Y direction
            } else if ( movedX && movedY ) {
                cost+=14; // moved in X AND_ Y direction
            }

            tmp=tmp.parent;
        }
        return cost;
    }
    
    private int calcEstimatedCost(PathNode node,PathNode end) {
        
        final int x=node.x;
        final int y=node.y;
        
        final int xDelta = x > end.x ? x - end.x : end.x - x;
        final int yDelta = y > end.y ? y - end.y : end.y - y;
        return (xDelta + yDelta)*10;
    }    
    
    public void findNeighbors(PathNode current,PathNode target) 
    {    
        // visit neighbor nodes
        final int startX = current.x > 0 ? current.x - 1 : current.x;
        final int endX = current.x < (sceneWidth -1 ) ? current.x + 1 : current.x; 
        
        final int startY = current.y > 0 ? current.y - 1 : current.y;
        final int endY = current.y < (sceneHeight - 1 ) ? current.y + 1 : current.y;
        
        for ( int x = startX ; x <= endX; x++ ) 
        {
            for ( int y = startY ; y <= endY ; y++ )
            {
                if ( x != current.x || y != current.y ) 
                {
                    if ( scene.isFree( x , y ) ) 
                    {
                        final PathNode newNode = new PathNode( x , y , current );
                        
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
            }
        }
    }
}
