package de.codesourcery.sandbox.minmax;

import java.awt.Color;

public class Player 
{
    private final byte id;
    private final String name;
    private final Color color;

    protected Player(int id,String name,Color color)
    {
        this.id = (byte) id;
        this.name = name.toUpperCase();
        this.color = color;
    }
    
    public byte getId()
    {
        return id;
    }
    
    @Override
    public String toString()
    {
        return name;
    }
    
    public Color getColor()
    {
        return color;
    }

    @Override
    public int hashCode()
    {
        return id;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Player) {
            final Player other = (Player) obj;
            return this.id == other.id;
        }
        return false;
    }

    public String name()
    {
        return name;
    }
}