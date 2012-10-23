package de.codesourcery.sandbox.pathfinder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.sandbox.pathfinder.IScene.ISceneVisitor;


public class Main extends JFrame
{
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    
    private volatile IScene scene;
    private volatile PathFinder finder;
    private volatile SceneRenderer renderer;    
    private final TreeMap<Long,Point> marked = new TreeMap<>();    
    
    public static void main(String[] args) throws IOException
    {
        new Main().run();
    }
    
    protected IScene setup(int width,int height) 
    {
        scene = new Scene(width,height);
        finder = new PathFinder( scene );
        renderer = new SceneRenderer(scene);
        marked.clear();
        return scene;
    }
    
    public void run() throws IOException
    {
        setup(WIDTH,HEIGHT);
        
        final File tmpFile = new File("/tmp/scene.bin");
        final JPanel panel = new JPanel() {
            
            @Override
            public void paint(Graphics g)
            {
                super.paint(g);
                renderer.renderScene(getSize() , (Graphics2D) g );
            }
        };
        
        final MouseAdapter listener = new MouseAdapter() 
        {
            private int lastX = -1;
            private int lastY = -1;            
            private boolean dragging = false;
            
            @Override
            public void mousePressed(MouseEvent e)
            {
                if ( e.getButton() == MouseEvent.BUTTON1 ) {
                    dragging = true;
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e)
            {
                if ( e.getButton() == MouseEvent.BUTTON1 ) {
                    dragging = false;
                }
            }
            
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if ( e.getButton() == MouseEvent.BUTTON3 ) 
                {
                    final int modelX = renderer.viewXToModel( e.getX() , panel.getSize() );
                    final int modelY = renderer.viewYToModel( e.getY() , panel.getSize() );                    
                    if ( marked.size() == 2 ) 
                    {
                        final Long removedId = marked.keySet().iterator().next();
                        marked.remove( removedId );
                        renderer.clearMarker( removedId );
                    }
                    
                    final long markerId = renderer.addMarker( modelX , modelY , Color.GREEN );
                    marked.put( markerId , new Point(modelX,modelY) );
                    panel.repaint();
                }
            }
            
            @Override
            public void mouseDragged(MouseEvent e)
            {
                if ( dragging ) 
                {
                    final int modelX = renderer.viewXToModel( e.getX() , panel.getSize() );
                    final int modelY = renderer.viewYToModel( e.getY() , panel.getSize() );
                    
                    if ( lastX != modelX || lastY != modelY ) 
                    {
                        if ( lastX != -1 ) {
                            markLine(lastX,lastY , modelX , modelY );
                        } else {
                            setDot( modelX,modelY );                            
                        }
                        
                        lastX = modelX;
                        lastY = modelY;
                        
                        panel.repaint();
                    }
                }               
            }
            
            private void setDot(int x,int y) {
                
                scene.write( x,y , IScene.OCCUPIED );
                
                final boolean notAtRightBorder = x+1 < scene.getWidth();
                final boolean notAtBottomBorder = y+1 < scene.getHeight();
                
                if ( notAtRightBorder ) 
                {
                    scene.write( x+1,y , IScene.OCCUPIED );
                }
                
                if (notAtBottomBorder ) 
                {
                    scene.write( x,y+1 , IScene.OCCUPIED );
                }     
                
                if ( notAtRightBorder && notAtBottomBorder ) {
                    scene.write( x+1,y+1 , IScene.OCCUPIED );
                }
            }
            
            private void markLine(int x1,int y1,int x2,int y2) {
                
                int x = x1;
                int y = y1;
                while (true ) 
                {
                    setDot( x,y );
                    if ( x == x2 && y == y2 ) {
                        return;
                    }
                    if ( x < x2 ) {
                        x++;
                    } else if ( x > x2 ) {
                        x--;
                    }
                    if ( y < y2 ) {
                        y++;
                    } else if ( y > y2 ) {
                        y--;
                    }
                } 
            }
            
        };
        
        panel.setPreferredSize(new Dimension(400,200));
        
        final JFrame frame = new JFrame("PathFinder V0.0");
        
        panel.addMouseListener( listener);  
        panel.addMouseMotionListener( listener );        
        
        frame.addKeyListener( new KeyAdapter() {
            
            private Long lastPathMarkerId = null;
            
            public void keyTyped(java.awt.event.KeyEvent e) 
            {
                if ( e.getKeyChar() == 'd' ) 
                {
                    System.out.println("Cleared");
                    marked.clear();
                    renderer.clearAllMarkers();
                    panel.repaint();
                    return;
                }
                
                if ( e.getKeyChar() == 's' ) {
                    System.out.println("Saving as "+tmpFile.getAbsolutePath());
                    try {
                        Scene.save( tmpFile , scene );
                        System.out.println("Saved.");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    return;
                }
                
                if ( e.getKeyChar() == 'l' && tmpFile.exists() ) 
                {
                    System.out.println("Loading from "+tmpFile.getAbsolutePath());
                    try 
                    {
                        final IScene otherScene = Scene.load( tmpFile );
                        setup( otherScene.getWidth() , otherScene.getHeight() );
                        
                        final ISceneVisitor v= new ISceneVisitor() {

							@Override
							public void visit(int x, int y, byte cellStatus) {
								scene.write( x , y , cellStatus );
							}
                        };
                        
                        otherScene.visitOccupiedCells( v );
                        
                        System.out.println("loaded.");
                        panel.repaint();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    return;
                }                
                
                if ( e.getKeyChar() == ' ' && marked.size() == 2 ) 
                {
                    System.out.println("SPACE");
                    
                    final List<PathFinder.PathNode> nodes = new ArrayList<>();
                    for ( Point p : marked.values() ) {
                        nodes.add( new PathFinder.PathNode( p.x , p.y ) );
                    }
                    final PathFinder.PathNode start = nodes.get(0);
                    final PathFinder.PathNode end = nodes.get(1);
  
                    long time = -System.currentTimeMillis();
                    PathFinder.PathNode path = finder.findPath( start , end );
                    time += System.currentTimeMillis();
                    System.out.println("Time: "+time+" ms");
                    
                    if ( lastPathMarkerId != null ) {
                        renderer.clearMarker( lastPathMarkerId );
                    }
                    
                    if ( path != null ) {
                        lastPathMarkerId = renderer.addMarkers( path , Color.BLUE );
                    }
                    panel.repaint();
                }
            };
        } );
        
        frame.getContentPane().setLayout( new GridBagLayout() );
        final GridBagConstraints cnstrs = new GridBagConstraints();
        cnstrs.fill = GridBagConstraints.BOTH;
        cnstrs.gridx = GridBagConstraints.REMAINDER;
        cnstrs.gridy = GridBagConstraints.REMAINDER;
        cnstrs.weightx = 1.0;
        cnstrs.weighty = 1.0;
        frame.getContentPane().add( panel , cnstrs );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.pack();
        frame.setVisible( true );
    }
}
