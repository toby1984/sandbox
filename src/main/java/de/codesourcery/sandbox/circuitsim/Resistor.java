package de.codesourcery.sandbox.circuitsim;

public class Resistor extends AbstractCircuitComponent
{
    public Resistor() {
        super(2,Orientation.HORIZONTAL);
    }
    
    @Override
    protected int getHeight()
    {
        return 1;
    }

    @Override
    protected int getWidth()
    {
        return 1;
    }

}
