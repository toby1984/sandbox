package de.codesourcery.sandbox.pathfinder;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class BSP<T> {

	private BSPNode<T> root;
	
	public interface IVisitor<T> {
		public boolean visit(BSPNode<T> node,int currentDepth);
	}	
	
	protected static class BSPNode<T> 
	{
		public final int x1;
		public final int y1;
		public final int x2;
		public final int y2;
		
		@SuppressWarnings("rawtypes")
		public final BSPNode[] children = new BSPNode[4];
		
		@Override
		public String toString() {
			return "BSPNode [ x=" + x1 + ", y = "+y1+" , width=" + width()+ ", height=" + height() +"]";
		}

		public BSPNode(int x, int y,int width,int height) {
			this.x1 = x;
			this.y1 = y;
			this.x2 = x1+width;
			this.y2 = y1+height;
		}
		
		public final boolean contains(int x,int y) {
			return x >= x1 && x < x2 &&
				   y >= y1 && y < y2;
		}
		
		public final boolean visitPreOrder(IVisitor<T> v) {
			return visitPreOrder(v,0);
		}
		
		public final boolean visitPreOrder(IVisitor<T> v,int currentDepth) {
			
			if ( ! v.visit( this , currentDepth ) ) {
				return false;
			}
			for ( BSPNode<T> child : children ) 
			{
				if ( child != null && ! child.visitPreOrder( v , currentDepth+1) ) {
					return false;
				}
			}
			return true;
		}
		
		public final int width() {
			return x2-x1;
		}
		
		public final int height() {
			return y2-y1;
		}
		
		public T getValue(int x,int y) 
		{
			if ( this.x1 == x && this.y1 == y && isLeaf() ) {
				return ((BSPLeafNode<T>) this).getValue();
			}
			
			if ( ! contains( x,y ) ) {
				return null;
			}
			
			@SuppressWarnings("unchecked")
			final BSPNode<T> child = children[ getQuadrant(x, y ) ];
			if ( child != null ) {
				return child.getValue(x,y);
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
		
		public List<BSPLeafNode<T>> getValues(int x,int y,int width,int height) 
		{
			final List<BSPLeafNode<T>> result = new ArrayList<>();
			getValues(x,y,width,height, result);
			return result;
		}
		
		private void getValues(int x,int y,int width,int height,List<BSPLeafNode<T>> result) 
		{
			if ( this.intersects( x, y, width,height ) ) 
			{
				if ( isLeaf() ) 
				{
					final BSPLeafNode<T> leaf = (BSPLeafNode<T>) this;
					T v = leaf.getValue();
					if ( v!= null ) {
						result.add( leaf );
					}
				} 
				else 
				{
					for ( BSPNode<T> child : children ) 
					{
						if ( child != null ) {
							child.getValues( x , y , width , height , result );
						}
					}
				}
			}
		}
		
		private int getQuadrant(BSPNode<T> node) 
		{
			return getQuadrant( node.x1 , node.y1 );
		}
		
		private int getQuadrant(int x , int y) 
		{
			final int w = width();
			final int h = height();
			
			final int newWidth = w > 1 ? w / 2 : w;
			final int newHeight = h > 1 ? h / 2 : h;
			
			if ( x >= x1 && x < x1 + newWidth &&
			     y >= y1 && y < y1 + newHeight ) 
			{
			  return 0;
			}
			
			if ( x >= x1+newWidth && x < x1 + w &&
				 y >= y1 && y < y1 + newHeight ) 
		    {
			  return 1;
		    }			
			
			if ( x >= x1 && x < x1 + newWidth &&
				 y >= y1+newHeight && y < y1 + h) 
		    {
				return 2;
			 }			
			return 3;
		}
		
		public void add(BSPLeafNode<T> node) 
		{
			if ( isLeaf() ) 
			{
				if ( x1 == node.x1 && y1 == node.x2 ) 
				{
					((BSPLeafNode<T>) this).setValue( node.getValue() );
					return;
				} 
				throw new IllegalStateException("Unreachable code reached");
			}
			
			final int quadrant = getQuadrant( node );
			
			@SuppressWarnings("unchecked")
			final BSPNode<T> child = children[quadrant];
			if ( child == null ) 
			{
				children[quadrant] = node;
				return;
			}
			
			if ( ! child.isLeaf() ) 
			{
				child.add( node ); // recurse
				return;
			}
			
			// we already got a leaf at the quadrant we want to insert 
			if ( child.x1 == node.x1 && child.y1 == node.y1 ) 
			{
				((BSPLeafNode<T>) child).setValue( node.getValue() );
				return;
			}
			
			// split child
			final int w = width();
			final int h = height();
			
			final int newWidth = w > 1 ? w / 2 : w;
			final int deltaWidth = w - newWidth*2;
			
			final int newHeight = h > 1 ? h / 2 : h;
			final int deltaHeight = w - newHeight*2;
			
			final BSPNode<T> newNode;
			switch( quadrant )
			{
				case 0:
					newNode = new BSPNode<T>(x1,y1,newWidth,newHeight);
					break;
				case 1:
					newNode = new BSPNode<T>(x1+newWidth,y1,newWidth+deltaWidth,newHeight);
					break;
				case 2:
					newNode = new BSPNode<T>(x1,y1+newHeight,newWidth,newHeight+deltaHeight);
					break;
				case 3:
					newNode = new BSPNode<T>(x1+newWidth,y1+newHeight,newWidth+deltaWidth,newHeight+deltaHeight);
					break;
				default:
					throw new IllegalStateException("Unreachable code reached");
			}
			
			children[quadrant] = newNode;
			newNode.add( (BSPLeafNode<T>) child );
			newNode.add( node );
		}
		
		public boolean isLeaf() {
			return false;
		}
	}
	
	protected static final class BSPLeafNode<T> extends BSPNode<T> {

		private T value;
		
		public BSPLeafNode(int x, int y, T value) {
			super(x, y, 1, 1);
			this.value = value;
		}

		public T getValue() {
			return value;
		}
		
		public void setValue(T value) {
			this.value = value;
		}
		
		@Override
		public boolean isLeaf() {
			return true;
		}
		
		@Override
		public String toString() {
			return "BSPNode [ x=" + x1 + ", y = "+y1+" , width=" + width()+ ", height=" + height()+ " , value=" + value + "]";
		} 
	}
	
	public BSP(int width,int height) 
	{
		root = new BSPNode<T>( 0 , 0 , width , height );
	}
	
	public static void main(String[] args) {
		final BSP<Integer> tree = new BSP<Integer>(128,128);
		
		tree.store( 5 , 5 , 42 );
		tree.store( 5 , 7 , 43 );
		tree.store( 70 , 70 , 44 );
		
		System.out.println("At (5,5): "+tree.getValue(5,5));
		System.out.println("At (5,7): "+tree.getValue(5,7));
		
		System.out.println("Values: "+tree.getValues(0,0,100,100) );
		tree.print();
	}
	
	public void store(int x,int y,T value) 
	{
		System.out.println("Storing "+value+" at ("+x+","+y+")");
		root.add( new BSPLeafNode<T>(x,y,value) );
	}
	
	public T getValue(int x,int y) {
		return root.getValue(x,y);
	}
	
	
	public List<BSPLeafNode<T>> getValues(int x,int y,int width,int height) {
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
		visitPreOrder( visitor );
	}
	
	public void visitPreOrder(IVisitor<T> v) {
		root.visitPreOrder( v );
	}
}
