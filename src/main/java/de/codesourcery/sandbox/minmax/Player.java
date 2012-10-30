package de.codesourcery.sandbox.minmax;

import java.awt.Color;

import org.apache.commons.lang.ObjectUtils;

public class Player 
{
    private final String name;
    private final Color color;

    protected Player(String name,Color color)
    {
        this.name = name;
        this.color = color;
    }
    
    @Override
    public String toString()
    {
        return "Player[ "+name+" ]";
    }
    
    public Color getColor()
    {
        return color;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Player) {
            final Player other = (Player) obj;
            return ObjectUtils.equals( name , other.name);
        }
        return false;
    }

    public String name()
    {
        return name;
    }
}