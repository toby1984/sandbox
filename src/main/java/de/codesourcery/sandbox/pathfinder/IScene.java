package de.codesourcery.sandbox.pathfinder;

public interface IScene
{
    public static final byte FREE=(byte)0;
    
    public static final byte OCCUPIED=(byte) 1;
    
    public static interface ISceneIterator {
        
        public byte next();
        
        public int x();
        
        public int y();
        
        public boolean hasNext();
    }
    
    public int getHeight();
    
    public int getWidth();
    
    public byte read(int x,int y);
    
    public boolean isFree(int x,int y);
    
    public IScene write(int x,int y,byte status);
    
    public ISceneIterator iterator();
}
