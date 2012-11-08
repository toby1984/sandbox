package de.codesourcery.sandbox.pathfinder;

import junit.framework.TestCase;
import de.codesourcery.sandbox.pathfinder.Rec2.Interval;

public class Rec2Test extends TestCase
{
    public void testInterval()
    {
        // disjunct
        Interval i1 = interval( 1 , 5 );
        Interval i2 = interval( 6 , 7 );
        assertNull( i1.intersection( i2 ) );
        assertNull( i2.intersection( i1 ) );
        
        // full overlap
        Interval i3 = interval( 1 , 5 );
        Interval i4 = interval( 1 , 5 );
        
        assertEquals( interval(1,5) , i3.intersection( i4 ) );
        assertEquals( interval(1,5) , i4.intersection( i3 ) );   
        
        // fully contained
        Interval i5 = interval( 1 , 5 );
        Interval i6 = interval( 2 , 4 );
        
        assertEquals( interval(2,4) , i5.intersection( i6 ) );
        assertEquals( interval(2,4) , i6.intersection( i5 ) );   
        
        // overlap
        Interval i7 = interval( 1 , 5 );
        Interval i8 = interval( 4 , 6 );
        
        assertEquals( interval(4,5) , i7.intersection( i8 ) );
        assertEquals( interval(4,5) , i8.intersection( i7 ) );           
    }
    
    private static Interval interval(int a,int b) {
        return new Interval(a,b);
    }    
}
