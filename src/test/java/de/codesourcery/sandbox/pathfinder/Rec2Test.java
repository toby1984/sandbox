package de.codesourcery.sandbox.pathfinder;

import junit.framework.TestCase;

public class Rec2Test extends TestCase
{

    public void testHorizontallyAdjacent1() {
        
        Rec2 r1 = rect( 0 , 0 , 5, 5 );
        Rec2 r2 = rect( 6 , 0 , 5, 5 );
        
        assertTrue( r1.isAdjactant( r2 ) );
        assertTrue( r2.isAdjactant( r1 ) );
    }
    
    public void testNotHorizontallyAdjacent1() {
        
        Rec2 r1 = rect( 0 , 0 , 5, 5 );
        Rec2 r2 = rect( 7 , 0 , 5, 5 );
        
        assertFalse( r1.isAdjactant( r2 ) );
        assertFalse( r2.isAdjactant( r1 ) );
    }    
    
    public void testVerticalAdjacent1() {
        
        Rec2 r1 = rect( 0 , 0 , 5, 5 );
        Rec2 r2 = rect( 0 , 6 , 5, 5 );
        
        assertTrue( r1.isAdjactant( r2 ) );
        assertTrue( r2.isAdjactant( r1 ) );
    }    
    
    public void testNotVerticalAdjacent1() {
        
        Rec2 r1 = rect( 0 , 0 , 5, 5 );
        Rec2 r2 = rect( 0 , 7 , 5, 5 );
        
        assertFalse( r1.isAdjactant( r2 ) );
        assertFalse( r2.isAdjactant( r1 ) );
    }     
    
    private static Rec2 rect(int x,int y,int w,int h) {
        return new Rec2(x,y,x+w,y+h);
    }
}
