package de.codesourcery.sandbox.circuitsim;

import java.util.ArrayList;
import java.util.List;

public class Port
{
    private final ICircuitComponent owner;
    
    private final List<Port> connectedPorts = new ArrayList<>();
    
    protected Port(ICircuitComponent owner)
    {
        this.owner = owner;
    }

    public List<Port> getConnectedPorts()
    {
        return connectedPorts;
    }

    public ICircuitComponent getOwningComponent() {
        return owner;
    }
}
