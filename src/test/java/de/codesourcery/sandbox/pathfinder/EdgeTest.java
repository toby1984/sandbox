package de.codesourcery.sandbox.pathfinder;

import junit.framework.TestCase;
import de.codesourcery.sandbox.pathfinder.Edge.EdgeOption;
import de.codesourcery.sandbox.pathfinder.Edge.IntersectionResult;
import de.codesourcery.sandbox.pathfinder.Edge.PointOption;

public class EdgeTest extends TestCase
{
    public void testIdenticalLines() 
    {
        Edge e1 = edge(point(0,0),point(10,10));
        Edge e2 = edge(point(0,0),point(10,10));
        
        assertIsEdge( e1.calcIntersection( e2 ) , e1 );
        assertIsEdge( e2.calcIntersection( e1 ) , e1 );        
    }
    
    public void testHorizontalLinePointIntersection() 
    {
        Edge e1 = edge(point(0,0),point(10,0));
        Edge e2 = edge(point(5,0),point(5,0));
        
        assertIsPoint( e1.calcIntersection( e2 ) , 5 , 0 );
        assertIsPoint( e2.calcIntersection( e1 ) , 5 , 0 );        
    }   
    
    public void testHorizontalLinePointIntersectionNoIntersect() 
    {
        Edge e1 = edge(point(0,0),point(10,0));
        Edge e2 = edge(point(11,0),point(11,0));
        
        assertIsEmpty(e1.calcIntersection( e2 ));
        assertIsEmpty(e2.calcIntersection( e1 ));        
    }      
    
    public void testVerticalLinePointIntersection() 
    {
        Edge e1 = edge(point(0,0),point(0,10));
        Edge e2 =  edge(point(0,5),point(0,5));
        
        assertIsPoint( e1.calcIntersection( e2 ) , 0, 5 );
        assertIsPoint( e2.calcIntersection( e1 ) , 0, 5 );        
    }    
    
    public void testVerticalLinePointIntersectionNoIntersect() 
    {
        Edge e1 = edge(point(0,0),point(0,10));
        Edge e2 = edge(point(0,11),point(0,11));
        
        assertIsEmpty( e1.calcIntersection( e2 ) );
        assertIsEmpty( e2.calcIntersection( e1 ) );        
    }  
    
    public void testParallelLineIntersection1() 
    {
        // vertical lines ,  no overlap
        Edge e1 = edge(point(0,0),point(0,10));
        Edge e2 = edge(point(1,0),point(1,10));
        
        assertIsEmpty( e1.calcIntersection( e2 ) );
        assertIsEmpty( e2.calcIntersection( e1 ) );        
    }   
    
    public void testParallelLineIntersection2() 
    {
        // horizontal lines, no overlap
        Edge e1 = edge(point(0,0),point(10,0));
        Edge e2 = edge(point(0,1),point(10,1));
        
        assertIsEmpty( e1.calcIntersection( e2 ) );
        assertIsEmpty( e2.calcIntersection( e1 ) );        
    }     
    
    public void testParallelLineIntersection3() 
    {
        // horizontal lines, no overlap
        Edge e1 = edge(point(1,1),point(3,3));
        Edge e2 = edge(point(1,3),point(5,5));
        
        assertIsEmpty( e1.calcIntersection( e2 ) );
        assertIsEmpty( e2.calcIntersection( e1 ) );        
    }  
    
    public void testParallelLineIntersection4() 
    {
        // vertical lines ,  no overlap
        Edge e1 = edge(point(3,3),point(4,4));
        Edge e2 = edge(point(1,1),point(2,2));
        
        assertIsEmpty( e1.calcIntersection( e2 ) );
        assertIsEmpty( e2.calcIntersection( e1 ) );        
    }    
    
    public void testParallelLineIntersection5() 
    {
        // vertical lines ,  no overlap
        Edge e1 = edge(point(3,3),point(4,4));
        Edge e2 = edge(point(1,1),point(3,3));
        
        assertIsPoint( e1.calcIntersection( e2 ) , 3 , 3 );
        assertIsPoint( e2.calcIntersection( e1 ) , 3 , 3 );        
    }      
    
    public void testHorizontalLineLineIntersection1() 
    {
        Edge e1 = edge(point(0,0),point(10,0));
        Edge e2 = edge(point(5,0),point(7,0));
        
        assertIsEdge( e1.calcIntersection( e2 ) , edge(point(5,0) , point(7,0 ) ));
        assertIsEdge( e2.calcIntersection( e1 ) , edge(point(5,0) , point(7,0 ) ));        
    }
    
    public void testDiagonlaLineIntersection1() 
    {
        Edge e1 = edge(point(1,1),point(4,4));
        Edge e2 = edge(point(2,2),point(3,3));
        
        assertIsEdge( e1.calcIntersection( e2 ) , edge(point(2,2) , point(3,3 ) ));
        assertIsEdge( e2.calcIntersection( e1 ) , edge(point(2,2) , point(3,3 ) ));        
    }    
    
    public void testDiagonlaLineIntersection2() 
    {
        Edge e1 = edge(point(1,1),point(4,4));
        Edge e2 = edge(point(3,3),point(5,5));
        
        assertIsEdge( e1.calcIntersection( e2 ) , edge(point(3,3) , point(4,4 ) ));
        assertIsEdge( e2.calcIntersection( e1 ) , edge(point(3,3) , point(4,4 ) ));        
    }    
    
    public void testHorizVertIntersection() 
    {
        Edge e1 = edge(point(10,0),point(10,10));
        Edge e2 = edge(point(5,5),point(15,5));
        
        assertIsPoint( e1.calcIntersection( e2 ) ,  10 , 5 );
        assertIsPoint( e1.calcIntersection( e2 ) ,  10 , 5 );        
    }    
    
    // ================= HELPER ===============
    
    private static Vec2 point(int x,int y) {
        return new Vec2(x,y);
    }
    
    private static Edge edge( Vec2 v1, Vec2 v2) {
        return new Edge(v1.x,v1.y,v2.x,v2.y);
    }
    
    private static void assertIsPoint(IntersectionResult result,int x,int y) 
    {
        assertTrue("Expected a point but got "+result,result.isPoint() );
        assertEquals( new Vec2(x,y) , new Vec2( ((PointOption) result).x , ((PointOption) result).y ) );
    }
    
    private static void assertIsEmpty(IntersectionResult result) {
        assertTrue("Expected an empty result but got "+result,result.isEmpty() );
    }    
    
    private static void assertIsEdge(IntersectionResult result,Edge expected) {
        assertTrue("Expected an edge but got "+result,result.isEdge() );
        assertEquals(expected , ((EdgeOption) result).toEdge());
    }      
}
