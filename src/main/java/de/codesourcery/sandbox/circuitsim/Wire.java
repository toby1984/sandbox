package de.codesourcery.sandbox.circuitsim;


public class Wire extends AbstractCircuitComponent
{
    public Wire() {
        super( 2 , Orientation.HORIZONTAL );
    }

    @Override
    public int getWidth()
    {
        return 1;
    }

    @Override
    public int getHeight()
    {
        return 0;
    }
}
