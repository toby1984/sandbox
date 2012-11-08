package de.codesourcery.sandbox.pathfinder;

import junit.framework.TestCase;

public class PolygonTest extends TestCase
{

    public void testMerge() {
        
        Rec2 r1 = new Rec2(0,0,10,10);
        Rec2 r2 = new Rec2( 2 ,2 ,4 ,4 );
        
        Polygon p1 = new Polygon( r1.getEdges() );
        p1.merge( new Polygon( r2.getEdges() ) );
        
        System.out.println("Result: "+p1);        
        assertEquals( r1.toPolygon() , p1 );
    }
    
    public void testMerge2() {
        
        Rec2 r1 = new Rec2(0,0,10,10);
        Rec2 r2 = new Rec2( 0,0,10,10 );
        
        Polygon p1 = new Polygon( r1.getEdges() );
        p1.merge( new Polygon( r2.getEdges() ) );
        
        System.out.println("Result: "+p1);        
        assertEquals( r1.toPolygon() , p1 );
    }    
    
    public void testMerge3() {
        
        Rec2 r1 = new Rec2(1,2,5,3);
        Rec2 r2 = new Rec2(3,1,7,4);
        
        Polygon p1 = new Polygon( r1.getEdges() );
        p1.merge( new Polygon( r2.getEdges() ) );
        
        System.out.println("Result: "+p1);
        assertEquals( r1.toPolygon() , p1 );        
    }    
    
}
