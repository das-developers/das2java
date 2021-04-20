package ProGAL.geom2d.delaunay;

import ProGAL.geom2d.Point;

public class Vertex extends ProGAL.geom2d.Point{
	private static final long serialVersionUID = 1L;
	int id;
	static int idCounter = 0;
	
	Triangle first;
	Triangle last;
	
        Vertex(Point p ){
            this( p.x(), p.y() );
        }
        
	public Vertex( double x, double y ){
		super( x,y );
		this.id = idCounter++;
	}
	
	public boolean onBoundary(){
		return first!=last || first.neighbors[ (first.indexOf(this)+2)%3 ] == null;
	}
	
	public String toString(){
		return String.format("V(%.1f,%.1f)",x(),y());
	}
}