package de.codesourcery.sandbox.pathfinder;

import java.util.List;

public interface IPolygon
{

    public boolean containsPoint(int x,int y);
    
    public List<Edge> getEdges();
    
    public Rec2 getBoundingBox();
    
    public boolean containsLine(int x1,int y1,int x2,int y2);
}
