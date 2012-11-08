package de.codesourcery.sandbox.circuitsim;

import java.util.ArrayList;
import java.util.List;

public class Circuit
{
    private List<ICircuitComponent> components=new ArrayList<>();
    
    public void addComponent(ICircuitComponent component) {
        components.add( component );
    }
    
    public List<ICircuitComponent> getComponents()
    {
        return components;
    }
    
    
}
