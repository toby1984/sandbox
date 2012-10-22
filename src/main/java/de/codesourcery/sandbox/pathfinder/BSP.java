package de.codesourcery.sandbox.pathfinder;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class BSP<T> {

	private BSPNode<T> root;
	
	public interface IVisitor<T> {
		public boolean visit(BSPNode<T> node,int currentDepth);
	}	
	
	protected static final class BSPNode<T> 
	{
		public final int x1;
		public final int y1;
		public final int x2;
		public final int y2;
		
		public T value;
		
		public  final List<BSPNode<T>> children=new ArrayList<>();
		
		@Override
		public String toString() {
			return "BSPNode [ x=" + x1 + ", y = "+y1+" , width=" + width()+ ", height=" + height()+ " , value=" + value + "]";
		}

		public BSPNode(int x, int y,int width,int height) {
			this(x,y,width , height , null );
		}
		
		public BSPNode(int x, int y,int width,int height,T value) {
			this.x1 = x;
			this.y1 = y;
			this.x2 = x1+width;
			this.y2 = y1+height;
			this.value = value;
		}
		
		public boolean contains(int x,int y) {
			return x >= x1 && x < x2 &&
				   y >= y1 && y < y2;
		}
		
		public BSPNode<T> find(int x,int y) 
		{
			if ( ! contains( x,y ) ) {
				return null;
			}
			for ( BSPNode<T> child : children ) 
			{
				if ( child.contains( x, y ) ) {
					return child.find( x , y );
				}
			}
			return this;
		}
		
		public boolean visit(IVisitor<T> v) {
			return visit(v,0);
		}
		
		public boolean visit(IVisitor<T> v,int currentDepth) {
			
			if ( ! v.visit( this , currentDepth ) ) {
				return false;
			}
			for ( BSPNode<T> child : children ) {
				if ( ! child.visit( v , currentDepth+1) ) {
					return false;
				}
			}
			return true;
		}
		
		public int width() {
			return x2-x1;
		}
		
		public int height() {
			return y2-y1;
		}
		
		public T getValue(int x,int y) 
		{
			if ( this.x1 == x && this.y1 == y && this.width() == 1 && this.height() == 1 ) {
				return this.value;
			}
			
			if ( ! contains( x,y ) ) {
				return null;
			}
			
			for ( BSPNode<T> child : children ) {
				if ( child.contains(x, y ) ) {
					return child.getValue(x,y);
				}
			}
			return null;
		}
		
		public boolean intersects(int x,int y,int width,int height) 
		{
			// fully contained
			final int px1 = x;
			final int px2 = x+width;
			final int py1 = y;
			final int py2 = y+height;
			
			if ( x1 < px1 && x2 > px2 && y1 < py1 && y2 > py2 ) { // this rect fully encloses the other
				return true;
			}
			
			if ( px1 < x1 && px2 > x2 && py1 < y1 && py2 > y2 ) { // other rect fully encloses this one
				return true;
			}			
			
			return ( x1 >= px1 && x1 < px2 && y1 >= py1 && y1 < py2 ) ||
				   ( x2 >= px1 && x2 < px2 && y2 >= py1 && y2 < py2 ) ||
				   ( px1 >= x1 && px1 < x2 && py1 >= y1 && py1 < y2 ) ||
				   ( px2 >= x1 && px2 < x2 && py2 >= y1 && py2 < y2 );				   
		}
		
		public List<BSPNode<T>> getValues(int x,int y,int width,int height) 
		{
			final List<BSPNode<T>> result = new ArrayList<>();
			getValues(x,y,width,height, result);
			return result;
		}
		
		private void getValues(int x,int y,int width,int height,List<BSPNode<T>> result) 
		{
			if ( this.intersects( x, y, width,height ) ) 
			{
				if ( this.width() == 1 && this.height() == 1 ) {
					if ( value != null ) {
						result.add( this );
					}
				} 
				else 
				{
					for ( BSPNode<T> child : children ) 
					{
						child.getValues( x , y , width , height , result );
					}
				}
			}
		}
		
		public void add(BSPNode<T> node) {
			/*
			 * +--+--+
			 * |  |  |
			 * +--+--+
			 * |  |  |
			 * +--+--+
			 * 
			 */
			
			if ( children.isEmpty() ) {
				children.add( node );
				return;
			}
			
			final BSPNode<T> oldValueChild = children.size() == 1 ? children.remove(0) : null;
			
			final int w = width();
			final int h = height();
			
			if ( w < 2 && h < 2) 
			{
				value = node.value;
				return;
			} 
			
			final int newWidth = w > 1 ? w / 2 : w;
			final int deltaWidth = w - newWidth*2;
			
			final int newHeight = h > 1 ? h / 2 : h;
			final int deltaHeight = w - newHeight*2;
			
			final List<BSPNode<T>> nodes = new ArrayList<>();
			
			if ( w > 1 && h > 1 ) 
			{
				// partition both dimensions
				System.out.println("Split both: "+newWidth+"+"+deltaWidth+","+newHeight+"+"+deltaHeight);
				nodes.add( new BSPNode<T>(x1,y1,newWidth,newHeight) );
				nodes.add( new BSPNode<T>(x1+newWidth,y1,newWidth+deltaWidth,newHeight) );
				
				nodes.add( new BSPNode<T>(x1,y1+newHeight,newWidth,newHeight+deltaHeight) );
				nodes.add( new BSPNode<T>(x1+newWidth,y1+newHeight,newWidth+deltaWidth,newHeight+deltaHeight) );
				
			} else if ( w > 1 && h < 2 ) {
				// partition horizontally
				nodes.add( new BSPNode<T>(x1,y1,newWidth,newHeight) );
				nodes.add( new BSPNode<T>(x1+newWidth,y1,newWidth+deltaWidth,newHeight) );				
			} else if ( w < 2 && h > 1 ) {
				// partition vertically
				nodes.add( new BSPNode<T>(x1,y1,newWidth,newHeight) );
				nodes.add( new BSPNode<T>(x1,y1+newHeight,newWidth,newHeight+deltaHeight) );
			}

			// sort children into new nodes
outer:			
			for( BSPNode<T> child : children ) 
			{
				for ( BSPNode<T> newNode : nodes ) {
					if ( newNode.contains( child.x1 , child.y1 ) ) {
						newNode.children.add( child );
						break outer;
					}
				}
				throw new IllegalStateException("Unreachable code reached");
			}
			
			// replace this node with new nodes
			children.clear();
			children.addAll( nodes );
			
			// recurse
			if ( oldValueChild != null ) {
				find( oldValueChild.x1 , oldValueChild.y1 ).add( oldValueChild );
			}
			find( node.x1 , node.y1 ).add( node );
		}
	}
	
	public BSP(int width,int height) 
	{
		root = new BSPNode<T>( 0 , 0 , width , height );
	}
	
	public static void main(String[] args) {
		final BSP<Integer> tree = new BSP<Integer>(128,128);
		
		tree.store( 5 , 5 , 42 );
		tree.store( 5 , 6 , 43 );
		
		System.out.println("At (5,5): "+tree.getValue(5,5));
		System.out.println("At (5,7): "+tree.getValue(5,7));
		
		System.out.println("Values: "+tree.getValues(0,0,100,100) );
		tree.print();
	}
	
	public void store(int x,int y,T value) 
	{
		System.out.println("Storing "+value+" at ("+x+","+y+")");
		root.find(x,y).add( new BSPNode<T>(x,y,1,1,value) );
	}
	
	public T getValue(int x,int y) {
		return root.getValue(x,y);
	}
	
	public List<BSPNode<T>> getValues(int x,int y,int width,int height) {
		return root.getValues( x , y , width , height );
	}
	
	public void print() {
		
		final IVisitor<T> visitor = new IVisitor<T>() {

			@Override
			public boolean visit(BSPNode<T> node, int currentDepth) 
			{
				final int indent = currentDepth*4;
				System.out.println( StringUtils.repeat(" ", indent)+node);
				return true;
			}
			
		};
		root.visit( visitor );
	}
}
