/*
 * The Triangulator - a java applet which animates some brute force
 * Delaunay triangulation algorithms of varying computational 
 * complexity.  
 * 
 * Author: Geoff Leach.  gl@cs.rmit.edu.au
 * Date: 29/3/96
 *
 * License to copy and use this software is granted provided that
 * appropriate credit is given to both RMIT and the author.
 *
 * The point of the applet is educational: 
 *
 * (a) To illustrate the importance of computational complexity.
 * The three Delaunay triangulation algorithms implemented have
 * computational complexities of O(n^2), O(n^3) and O(n^4).  The
 * user can see for themselves the improvement in speed going 
 * from O(n^4) to O(n^2).
 * 
 * (b) To illustrate what Delaunay triangulations are.  The Delaunay
 * triangulation, and the related Voronoi diagram, is a 
 * particularly useful data structure for a number of problems.
 * Without going into the applications the applet attempts to 
 * show what Delaunay triangulation algorithms are.
 *
 * (c) To provide a useful context for me to initially learn Java.
 *
 * The code still needs polishing ... 
 * 
 */

package edu.uiowa.physics.pw.das.math;

import java.applet.*;
import java.awt.*;
import java.io.PrintStream;

/* 
 * Sometimes we just have to die. 
 */
class Panic {
    static public void panic(String m) {
	System.err.println(m);
	System.exit(1);
    }
}

/* 
 * Wrapper class for basic int type for pass by reference.
 */
class Int {
    int i;

    public void Int() {
	i = 0;
    }

    public void Int(int i) {
	this.i = i;
    }

    public void setValue(int i) {
	this.i = i;
    }

    public int getValue() {
	return i;
    }
}

/*
 * Point class.  RealPoint to avoid clash with java.awt.Point.
 */
class RealPoint {
    float x, y;

    RealPoint() { x = y = 0.0f; }
    RealPoint(float x, float y) { this.x = x; this.y = y; }
    RealPoint(RealPoint p) { x = p.x; y = p.y; }
    public float x() { return this.x; }
    public float y() { return this.y; }
    public void set(float x, float y) { this.x = x; this.y = y; }

    public float distance(RealPoint p) {
	float dx, dy;

	dx = p.x - x;
	dy = p.y - y;
	return (float)Math.sqrt((double)(dx * dx + dy * dy));
    }

    public float distanceSq(RealPoint p) { 
	float dx, dy;

	dx = p.x - x;
	dy = p.y - y;
	return (float)(dx * dx + dy * dy);
    }
}

/* 
 * Edge class. Edges have two vertices, s and t, and two faces,
 * l (left) and r (right). The triangulation representation and
 * the Delaunay triangulation algorithms require edges.
 */
class Edge {
    int s, t;
    int l, r;

    Edge() { s = t = 0; }
    Edge(int s, int t) { this.s =s; this.t = t; }
    int s() { return this.s; }
    int t() { return this.t; }
    int l() { return this.l; }
    int r() { return this.r; }
}


/*
 * Vector class.  A few elementary vector operations.
 */
class Vector {
    float u, v;

    Vector() { u = v = 0.0f; }
    Vector(RealPoint p1, RealPoint p2) { 
	u = p2.x() - p1.x(); 
	v = p2.y() - p1.y(); 
    }
    Vector(float u, float v) { this.u = u; this.v = v; }

    float dotProduct(Vector v) { return u * v.u + this.v * v.v; }

    static float dotProduct(RealPoint p1, RealPoint p2, RealPoint p3) { 
	float u1, v1, u2, v2;

	u1 =  p2.x() - p1.x();
	v1 =  p2.y() - p1.y();
	u2 =  p3.x() - p1.x();
	v2 =  p3.y() - p1.y();

	return u1 * u2 + v1 * v2;
    }

    float crossProduct(Vector v) { return u * v.v - this.v * v.u; }

    static float crossProduct(RealPoint p1, RealPoint p2, RealPoint p3) { 
	float u1, v1, u2, v2;

	u1 =  p2.x() - p1.x();
	v1 =  p2.y() - p1.y();
	u2 =  p3.x() - p1.x();
	v2 =  p3.y() - p1.y();

	return u1 * v2 - v1 * u2;
    }

    void setRealPoints(RealPoint p1, RealPoint p2) { 
	u = p2.x() - p1.x(); 
	v = p2.y() - p1.y(); 
    }
}

/*
 * Circle class. Circles are fundamental to computation of Delaunay 
 * triangulations.  In particular, an operation which computes a 
 * circle defined by three points is required.
 */
class Circle {
    RealPoint c;
    float r;

    Circle() { c = new RealPoint(); r = 0.0f; }
    Circle(RealPoint c, float r) { this.c = c; this.r = r; }
    public RealPoint center() { return c; }
    public float radius() { return r; }
    public void set(RealPoint c, float r) { this.c = c; this.r = r; }

    /* 
     * Tests if a point lies inside the circle instance.
     */
    public boolean inside(RealPoint p) {
	if (c.distanceSq(p) < r * r)
	    return true;
	else
	    return false;
    }

    /* 
     * Compute the circle defined by three points (circumcircle). 
     */
    public void circumCircle(RealPoint p1, RealPoint p2, RealPoint p3) {
	float cp;

	cp = Vector.crossProduct(p1, p2, p3);
	if (cp != 0.0)
	    {
		float p1Sq, p2Sq, p3Sq;
		float num, den;
		float cx, cy;

		p1Sq = p1.x() * p1.x() + p1.y() * p1.y();
		p2Sq = p2.x() * p2.x() + p2.y() * p2.y();
		p3Sq = p3.x() * p3.x() + p3.y() * p3.y();
		num = p1Sq*(p2.y() - p3.y()) + p2Sq*(p3.y() - p1.y()) + p3Sq*(p1.y() - p2.y());
		cx = num / (2.0f * cp);
		num = p1Sq*(p3.x() - p2.x()) + p2Sq*(p1.x() - p3.x()) + p3Sq*(p2.x() - p1.x());
		cy = num / (2.0f * cp);

		c.set(cx, cy);
	    }
    
	// Radius 
	r = c.distance(p1);
    }
}

/*
 * Triangulation class.  A triangulation is represented as a set of
 * points and the edges which form the triangulation.
 */
class Triangulation {
    static final int Undefined = -1;
    static final int Universe = 0;
    int nPoints;
    RealPoint point[];
    int nEdges;
    int maxEdges;
    Edge edge[];

    Triangulation(int nPoints) {

	// Allocate points. 
	this.nPoints = nPoints;
	this.point = new RealPoint[nPoints];
	for (int i = 0; i < nPoints; i++)
	    point[i] = new RealPoint();

	// Allocate edges. 
	maxEdges = 3 * nPoints - 6;	// Max number of edges.
	edge = new Edge[maxEdges];
	for (int i = 0; i < maxEdges; i++)
	    edge[i] = new Edge();
	nEdges = 0;
    }

    /*
     * Sets the number of points in the triangulation.  Reuses already
     * allocated points and edges.
     */
    public void setNPoints(int nPoints) {
	// Fix edge array. 
	Edge tmpEdge[] = edge;
	int tmpMaxEdges = maxEdges;
	maxEdges = 3 * nPoints - 6;	// Max number of edges.
	edge = new Edge[maxEdges];

	// Which is smaller? 
	int minMaxEdges;
	if (tmpMaxEdges < maxEdges) 
	    minMaxEdges = tmpMaxEdges;
	else
	    minMaxEdges = maxEdges;

	// Reuse allocated edges. 
	for (int i = 0; i < minMaxEdges; i++)
	    this.edge[i] = tmpEdge[i];

	// Get new edges. 
	for (int i = minMaxEdges; i < maxEdges; i++)
	    this.edge[i] = new Edge();

	// Fix point array. 
	RealPoint tmpPoint[] = point;
	point = new RealPoint[nPoints];

	// Which is smaller? 
	int minPoints;
	if (nPoints < this.nPoints) 
	    minPoints = nPoints;
	else
	    minPoints = this.nPoints;

	// Reuse allocated points. 
	for (int i = 0; i < minPoints; i++)
	    this.point[i] = tmpPoint[i];

	// Get new points. 
	for (int i = minPoints; i < nPoints; i++)
	    this.point[i] = new RealPoint();

	this.nPoints = nPoints;
    }

    /* 
     * Generates a set of random points to triangulate. 
     */
    public void randomPoints(RealWindow w) {
	for (int i = 0; i < nPoints; i++)
	    {
		point[i].x = (float)Math.random() * w.xMax();
		point[i].y = (float)Math.random() * w.yMax();
	    }
	nEdges = 0;
    }

    /* 
     * Copies a set of points.
     */
    public void copyPoints(Triangulation t) {
	int n;

	if (t.nPoints < nPoints)
	    n = t.nPoints;
	else 
	    n = nPoints;

	for (int i = 0; i < n; i++) {
	    point[i].x = t.point[i].x;
	    point[i].y = t.point[i].y;
	}

	nEdges = 0;
    }

    void addTriangle(int s, int t, int u) {
	addEdge(s, t);
	addEdge(t, u);
	addEdge(u, s);
    }

    public int addEdge(int s, int t) {
	return addEdge(s, t, Undefined, Undefined);
    }

    /* 
     * Adds an edge to the triangulation. Store edges with lowest
     * vertex first (easier to debug and makes no other difference).
     */
    public int addEdge(int s, int t, int l, int r) {
	int e;

	// Add edge if not already in the triangulation. 
	e = findEdge(s, t);
	if (e == Undefined) 
	    if (s < t)
		{
		    edge[nEdges].s = s;
		    edge[nEdges].t = t;
		    edge[nEdges].l = l;
		    edge[nEdges].r = r;
		    return nEdges++;
		} 
	    else
		{
		    edge[nEdges].s = t;
		    edge[nEdges].t = s;
		    edge[nEdges].l = r;
		    edge[nEdges].r = l;
		    return nEdges++;
		}
	else
	    return Undefined;
    }

    public int findEdge(int s, int t) {
	boolean edgeExists = false;
	int i;

	for (i = 0; i < nEdges; i++)
	    if (edge[i].s == s && edge[i].t == t || 
		edge[i].s == t && edge[i].t == s) {
		edgeExists = true;
		break;
	    }

	if (edgeExists)
	    return i;
	else
	    return Undefined;
    }

    /* 
     * Update the left face of an edge. 
     */
    public void updateLeftFace(int eI, int s, int t, int f) {
	if (!((edge[eI].s == s && edge[eI].t == t) ||
	      (edge[eI].s == t && edge[eI].t == s)))
	    Panic.panic("updateLeftFace: adj. matrix and edge table mismatch");
	if (edge[eI].s == s && edge[eI].l == Triangulation.Undefined)
	    edge[eI].l = f;
	else if (edge[eI].t == s && edge[eI].r == Triangulation.Undefined)
	    edge[eI].r = f;
	else
	    Panic.panic("updateLeftFace: attempt to overwrite edge info");
    }

    public void draw(RealWindowGraphics rWG, Color pC, Color eC) {
	drawPoints(rWG, pC);
	drawEdges(rWG, eC);
    }

    public void drawPoints(RealWindowGraphics rWG, Color c) {
	for (int i = 0; i < nPoints; i++)
	    rWG.drawPoint(point[i], c);
    }

    public void drawEdges(RealWindowGraphics rWG, Color c) {
	for (int i = 0; i < nEdges; i++)
	    drawEdge(rWG, edge[i], c);
    }

    public void drawEdge(RealWindowGraphics rWG, Edge e, Color c) {
	rWG.drawLine(point[e.s], point[e.t], c);
    }

    public void print(PrintStream p) {
	printPoints(p);
	printEdges(p);
    }

    public void printPoints(PrintStream p) {
	for (int i = 0; i < nPoints; i++)
	    p.println(String.valueOf(point[i].x) + " " + String.valueOf(point[i].y));
    }

    public void printEdges(PrintStream p) {
	for (int i = 0; i < nEdges; i++)
	    p.println(String.valueOf(edge[i].s) + " " + String.valueOf(edge[i].t));
    }
}

/* 
 * Rectangle class.  Need rectangles for window to viewport mapping.
 */
class RealRectangle {
    RealPoint ll;
    RealPoint ur;

    RealRectangle() { }

    RealRectangle (RealRectangle r) {
	this.ll = new RealPoint(r.ll);
	this.ur = new RealPoint(r.ur);
    }

    RealRectangle (RealPoint ll, RealPoint ur) {
	this.ll = new RealPoint(ll);
	this.ur = new RealPoint(ur);
    }

    RealRectangle(float xMin, float yMin, float xMax, float yMax) {
	this.ll = new RealPoint(xMin, yMin);
	this.ur = new RealPoint(xMax, yMax);
    }

    public float width() { return ur.x() - ll.x(); }
    public float height() { return ur.y() - ll.y(); }

    public RealPoint ll() { return ll; }
    public RealPoint ur() { return ur; }

    public float xMin() { return ll.x; }
    public float yMin() { return ll.y; }

    public float xMax() { return ur.x; }
    public float yMax() { return ur.y; }
}

/* 
 * A window is essentially a rectangle. 
 */
class RealWindow extends RealRectangle {
    RealWindow() {}
    RealWindow(float xMin, float yMin, float xMax, float yMax) {
	super(xMin, yMin, xMax, yMax); 
    }
    RealWindow(RealWindow w) { super(w.ll(), w.ur()); }
}

/*
 * RealWindowGraphics class. Has a window, a viewport and a 
 * graphics context into which to draw.  The graphics context
 * is only set after calls to repaint result in calls to update.
 * Contains drawing operations, drawTriangle for example, needed 
 * elsewhere.
 */
class RealWindowGraphics {
    RealWindow w = null;	// window
    Dimension v = null;	// viewport
    Graphics g = null;
    float scale = 1.0f;

    static final float realPointRadius = 0.04f;
    static final int pixelPointRadius = 4;
    static final int halfPixelPointRadius = 2;

    RealWindowGraphics(RealWindow w) {
	this.w = new RealWindow(w);
    }
    
    RealWindowGraphics(RealWindow w, Dimension d, Graphics g) {
	this.w = new RealWindow(w);
	this.v = new Dimension(d.width, d.height);
	this.g = g;
	calculateScale();
    }

    public void setWindow(RealWindow w) {
	this.w = new RealWindow(w);
	calculateScale();
    }

    public void setViewport(Dimension d) {
	this.v = new Dimension(d.width, d.height);
	calculateScale();
    }

    public void setGraphics(Graphics g) {
	this.g = g;
    }

    public Graphics getGraphics(Graphics g) {
	return g;
    }

    public void calculateScale() {
	float sx, sy;

	sx = v.width / w.width();
	sy = v.height / w.height();

	if (sx < sy)
	    scale = sx;
	else
	    scale = sy;
    }

    public void drawTriangle(RealPoint p1, 
			     RealPoint p2, 
			     RealPoint p3, 
			     Color c) { 
	drawLine(p1, p2, c);
	drawLine(p2, p3, c);
	drawLine(p3, p1, c);
    }

    public void drawLine(RealPoint p1, RealPoint p2, Color c) { 
	int x1, y1, x2, y2;

	g.setColor(c);
	x1 = (int)(p1.x() * scale);
	y1 = (int)(p1.y() * scale);
	x2 = (int)(p2.x() * scale);
	y2 = (int)(p2.y() * scale);
	g.drawLine(x1, y1, x2, y2);
    }

    public void drawPoint(RealPoint p, Color c) {
	g.setColor(c);

	g.fillOval((int)(scale * p.x()) - halfPixelPointRadius, 
		   (int)(scale * p.y()) - halfPixelPointRadius, 
		   pixelPointRadius, pixelPointRadius);
    }

    public void drawCircle(Circle circle, Color c) {
	drawCircle(circle.center().x(), circle.center().y(), circle.radius(), c);
    }

    public void drawCircle(RealPoint p, float r, Color c) {
	drawCircle(p.x(), p.y(), r, c);
    }

    public void drawCircle(float x, float y, float r, Color c) {
	g.setColor(c);

	g.drawOval((int)(scale * (x - r)), (int)(scale * (y - r)), 
		   (int)(2.0f * r * scale), (int)(2.0f * r * scale));
    }

    public void fillCircle(float x, float y, float r, Color c) {
	g.setColor(c);

	g.fillOval((int)(scale * (x - r)), (int)(scale * (y - r)), 
		   (int)(2.0f * r * scale), (int)(2.0f * r * scale));
    }
}

/*
 * AlgorithmUIHeading class. Provides a heading for part of the user
 * interface. 
 */
class AlgorithmUIHeading extends Panel {

    public AlgorithmUIHeading() {
	// Headings. 
	setLayout(new GridLayout(0,7));
	add(new Label("Algorithm", Label.LEFT));
	add(new Label("Run", Label.LEFT));
	add(new Label("Points", Label.LEFT));
	add(new Label("Triangles", Label.LEFT));
	add(new Label("Circles", Label.LEFT));
	add(new Label("Points", Label.LEFT));
	add(new Label("Pause (mS)", Label.LEFT));
    }
}

/* 
 * TriangulationCanvas class. Each of the triangulation algorithms
 * needs a canvas to draw into.
 */
@SuppressWarnings("deprecation")
class TriangulationCanvas extends Canvas {
    Triangulation t;
    RealWindowGraphics rWG;	// Does the actual drawing. 
    boolean needToClear = false;
    boolean newPoints = false;
    TriangulationAlgorithm alg;	// The algorithm which uses this canvas. 

    TriangulationCanvas(Triangulation t, 
			RealWindow w, 
			TriangulationAlgorithm alg) {
	this.t = t;
	rWG = new RealWindowGraphics(w);
	this.alg = alg;
    }

    public Insets insets() {
	return new Insets(2,10,2,15);
    }

    public void paint(Graphics g) {
	if (needToClear) {
	    g.clearRect(0, 0, size().width, size().height);
     
	    needToClear = false;
	}
	g.drawRect(0, 0, size().width-1, size().height-1);
	rWG.setGraphics(g);
	rWG.setViewport(size());
	alg.draw(rWG, t);
    }

    public void update(Graphics g) {
	paint(g);
    }
}

/* 
 * AlgorithmUI class.  Each algorithm has a set of user interface
 * controls.  This class provides them.
 */
@SuppressWarnings("deprecation")
class AlgorithmUI extends Panel {
    TextField nPointsTextField;
    Checkbox animateCheckBox[];
    Checkbox runCheckBox;
    TextField pauseTextField;
    TriangulationAlgorithm algorithm;  // Algorithm which uses this UI. 

    public AlgorithmUI(TriangulationAlgorithm algorithm,
		       String label, int nPoints, int pause) {

	this.algorithm = algorithm;

	// One set of controls per algorithm. 
	setLayout(new GridLayout(0,7));
	add(new Label(label, Label.LEFT));
	add(runCheckBox = new Checkbox(null, null, true));
	add(nPointsTextField = new TextField(String.valueOf(nPoints), 5));
	animateCheckBox = new Checkbox[AnimateControl.nEntities];
	animateCheckBox[AnimateControl.triangles] = new Checkbox(null, null, true);
	add(animateCheckBox[AnimateControl.triangles]);
	animateCheckBox[AnimateControl.circles] = new Checkbox(null, null, true);
	add(animateCheckBox[AnimateControl.circles]);
	animateCheckBox[AnimateControl.points] = new Checkbox(null, null, true);
	add(animateCheckBox[AnimateControl.points]);
	pauseTextField = new TextField(String.valueOf(pause), 5);
	add(pauseTextField);
    }

    public void setAlgorithm(TriangulationAlgorithm algorithm) {
	this.algorithm = algorithm;
    }

    // Gets the current value in a text field.  
    int getValue(TextField tF) {
	int i;
	try {
	    i = Integer.valueOf(tF.getText()).intValue(); 
	} catch (java.lang.NumberFormatException e) {
	    i = 0;
	}
	return i;
    }

    public boolean handleEvent(Event evt) {
	if (evt.id == Event.ACTION_EVENT) {
	    if (evt.target == runCheckBox) {
		algorithm.control().setRun(((Boolean)evt.arg).booleanValue());
		return true;
	    } else if (evt.target == animateCheckBox[AnimateControl.triangles]) {
		algorithm.control().setAnimate(AnimateControl.triangles, 
					       ((Boolean)evt.arg).booleanValue());
		return true;
	    } else if (evt.target == animateCheckBox[AnimateControl.circles]) {
		algorithm.control().setAnimate(AnimateControl.circles, 
					       ((Boolean)evt.arg).booleanValue());
		return true;
	    } else if (evt.target == animateCheckBox[AnimateControl.points]) {
		algorithm.control().setAnimate(AnimateControl.points, 
					       ((Boolean)evt.arg).booleanValue());
		return true;
	    } else if (evt.target == pauseTextField) {
		algorithm.control().setPause(getValue(pauseTextField));
		return true;
	    } else if (evt.target == nPointsTextField) {
		algorithm.control().nPoints = getValue(nPointsTextField);
		return true;
	    }
	}

	return false;
    }
}

/*
 * AnimateControl class.  Each algorithm animation has various entities
 * which can be displayed.  This class provides the state which controls
 * what is being displayed.  It is manipulated by AlgorithmUI and 
 * accessed by the animation routines in each algorithm.
 */
class AnimateControl {
    TriangulationAlgorithm triAlg;
    static final int automatic = 0;
    static final int manual = 1;
    int animateMode = automatic;
    int pause = 10;
    static final int algorithm = 0;
    static final int triangles = 1;
    static final int points = 2;
    static final int circles = 3;
    static final int nEntities = 4;
    boolean run;
    boolean animate[];
    int nPoints;

    AnimateControl(TriangulationAlgorithm algorithm) {
	triAlg = algorithm;
	animate = new boolean[nEntities];
	for (int i = 0; i < nEntities; i++)
	    animate[i] = true;
	run = true;
    }

    AnimateControl(TriangulationAlgorithm algorithm, int nPoints) {
	this(algorithm);
	this.nPoints = nPoints;
    }

    public void setAnimate(int entity, boolean v) {
	animate[entity] = v;
	if (!v)
	    triAlg.canvas().needToClear = true;
    }

    public boolean animate(int entity) {
	return animate[entity];
    }

    public int mode() {
	return animateMode;
    }

    public void setManualAnimateMode() {
	animateMode = manual;
    }

    public void setAutomaticAnimateMode() {
	animateMode = automatic;
    }

    public int getPause() {
	return pause;
    }

    public void setPause(int p) {
	pause = p;
    }

    public int getNPoints() {
	return nPoints;
    }

    public void setNPoints(int n) {
	nPoints = n;
    }

    public void setRun(boolean v) {
	run = v;
    }

    public boolean getRun() {
	return run;
    }
}

/*
 * TriangulationAlgorithm class.  Absract.  Superclass for
 * actual algorithms.  Has several abstract function members -
 * including the triangulation member which actually computes
 * the triangulation.
 */
abstract class TriangulationAlgorithm {
    String algName;
    TriangulationCanvas triCanvas;
    AnimateControl aniControl;
    AlgorithmUI algorithmUI;
    RealWindow w;
    RealWindowGraphics rWG;

    // Variables and constants for animation state. 
    final int nStates = 5;
    boolean state[] = new boolean[nStates];
    static final int triangulationState = 0;
    static final int pointState = 1;
    static final int triangleState = 2;
    static final int insideState = 4;
    static final int edgeState = 5;

    public TriangulationAlgorithm(Triangulation t, RealWindow w, 
				  String name, int nPoints) {
	algName = name;
	aniControl = new AnimateControl(this, nPoints);
	algorithmUI = new AlgorithmUI(this, name, nPoints, aniControl.getPause());
	triCanvas = new TriangulationCanvas(t, w, this);

	for (int s = 0; s < nStates; s++)
	    state[s] = false;
	triCanvas.needToClear = true;
    }

    public void setCanvas(TriangulationCanvas tc) {
	triCanvas = tc;
    }

    public AnimateControl control() {
	return aniControl;
    }

    public AlgorithmUI algorithmUI() {
	return algorithmUI;
    }

    public TriangulationCanvas canvas() {
	return triCanvas;
    }

    public void setAlgorithmState(int stateVar, boolean value) {
	state[stateVar] = value;
    }

    public void pause() {
	if (aniControl.mode() == AnimateControl.automatic)
	    try {
		wait(aniControl.getPause());
	    } catch (InterruptedException e){}
	else
	    try {wait();} catch (InterruptedException e){}
    }

    public void animate(int state) {
	if ((aniControl.animate(AnimateControl.triangles) ||
	     aniControl.animate(AnimateControl.circles)) && 
	    state == triangulationState)
	    triCanvas.needToClear = true;

	setAlgorithmState(state, true);

	triCanvas.repaint();
    
	pause();
    
	setAlgorithmState(state, false);
    }

    public void reset() {
	for (int s = 0; s < nStates; s++)
	    state[s] = false;
	triCanvas.needToClear = true;
    }

    public synchronized void nextStep() { notify(); }
    abstract public void triangulate(Triangulation t);
    abstract public void draw(RealWindowGraphics rWG, Triangulation t);
}

/* 
 * QuarticAlgorithm class.  O(n^4) algorithm. The most brute-force 
 * of the algorithms.
 */
class QuarticAlgorithm extends TriangulationAlgorithm {
    int i, j, k, l;
    Circle c = new Circle();
    final static String algName = "O(n^4)";

    public QuarticAlgorithm(Triangulation t, RealWindow w, int nPoints) {
	super(t, w, algName, nPoints);
    }

    public void reset() {
	i = j = k = l = 0;
	super.reset();
    }

    public void draw(RealWindowGraphics rWG, Triangulation t) {
	if (state[triangleState]) {
	    if (aniControl.animate(AnimateControl.triangles))
		rWG.drawTriangle(t.point[i], t.point[j], t.point[k], Color.green);
	    if (aniControl.animate(AnimateControl.circles))
		rWG.drawCircle(c, Color.green);
	} else if (state[pointState]) {
	    if (aniControl.animate(AnimateControl.points))
		rWG.drawPoint(t.point[l], Color.orange);
	} else if (state[insideState]) {
	    if (aniControl.animate(AnimateControl.triangles))
		rWG.drawTriangle(t.point[i], t.point[j], t.point[k], Color.red);
	    if (aniControl.animate(AnimateControl.circles))
		rWG.drawCircle(c, Color.red);
	    if (aniControl.animate(AnimateControl.points))
		rWG.drawPoint(t.point[l], Color.red);
	} else if (state[triangulationState]) {
	    t.draw(rWG, Color.black, Color.black);
	} else {
	    t.draw(rWG, Color.black, Color.black);
	}
    }

    public synchronized void triangulate(Triangulation t) {
	boolean pointFree;
	int n = t.nPoints;
	RealPoint p[] = t.point;

	for (i = 0; i < n-2; i++)
	    for (j = i + 1; j < n-1; j++)
		if (j != i)
		    for (k = j + 1; k < n; k++)
			if (k != i && k != j)
			    { 
				c.circumCircle(p[i], p[j], p[k]);
				animate(triangleState);
				pointFree = true;
				for (l = 0; l < n; l++)
				    if (l != i && l != j && l != k) {
					animate(pointState);
					if (c.inside(p[l])) {
					    animate(insideState);
					    pointFree = false;
					    break;
					}
				    }

				if (pointFree)
				    t.addTriangle(i, j, k);
		
				animate(triangulationState);
			    }
    }
}

/* 
 * CubicAlgorithm class.  O(n^3) algorithm. 
 */
class CubicAlgorithm extends TriangulationAlgorithm {
    int s, t, u, i;
    Circle bC = new Circle();
    final static String algName = "O(n^3)";
    int nFaces;

    public CubicAlgorithm(Triangulation t, RealWindow w, int nPoints) {
	super(t, w, algName, nPoints);
    }

    public void reset() {
	nFaces = 0;
	triCanvas.needToClear = true;
	super.reset();
    }

    public void draw(RealWindowGraphics rWG, Triangulation tri) {
	if (state[triangleState]) {
	    if (aniControl.animate(AnimateControl.triangles)) {
		rWG.drawTriangle(tri.point[s], tri.point[t], tri.point[u], 
				 Color.green);
		rWG.drawLine(tri.point[s], tri.point[t], Color.blue);
	    }
	    if (aniControl.animate(AnimateControl.circles))
		rWG.drawCircle(bC, Color.green);
	} else if (state[pointState]) {
	    if (aniControl.animate(AnimateControl.points))
		rWG.drawPoint(tri.point[i], Color.orange);
	} else if (state[insideState]) {
	    if (aniControl.animate(AnimateControl.triangles)) {
		rWG.drawTriangle(tri.point[s], tri.point[t], tri.point[u], Color.red);
		rWG.drawLine(tri.point[s], tri.point[t], Color.blue);
	    }
	    if (aniControl.animate(AnimateControl.circles))
		rWG.drawCircle(bC, Color.red);
	    if (aniControl.animate(AnimateControl.points))
		rWG.drawPoint(tri.point[s], Color.red);
	} else if (state[triangulationState]) {
	    tri.draw(rWG, Color.black, Color.black);
	} else {
	    tri.draw(rWG, Color.black, Color.black);
	}
    }

    public synchronized void triangulate(Triangulation tri) {
	int seedEdge, currentEdge;
	int nFaces;
	Int s, t;

	// Initialise. 
	nFaces = 0;
	s = new Int();
	t = new Int();
  
	// Find closest neighbours and add edge to triangulation. 
	findClosestNeighbours(tri.point, tri.nPoints, s, t);

	// Create seed edge and add it to the triangulation. 
	seedEdge = tri.addEdge(s.getValue(), t.getValue(), 
			       Triangulation.Undefined, 
			       Triangulation.Undefined);

	currentEdge = 0;
	while (currentEdge < tri.nEdges)
	    {
		if (tri.edge[currentEdge].l == Triangulation.Undefined) {
		    completeFacet(currentEdge, tri, nFaces);
		    animate(triangulationState);
		}
		if (tri.edge[currentEdge].r == Triangulation.Undefined) {
		    completeFacet(currentEdge, tri, nFaces);
		    animate(triangulationState);
		}
		currentEdge++;
	    }
    }

    // Find the two closest points.  
    public void findClosestNeighbours(RealPoint p[], int nPoints,
				      Int u, Int v) {
	int i, j;
	float d, min;
	int s, t;

	s = t = 0;
	min = Float.MAX_VALUE;
	for (i = 0; i < nPoints-1; i++)
	    for (j = i+1; j < nPoints; j++)
		{
		    d = p[i].distanceSq(p[j]);
		    if (d < min)
			{
			    s = i;
			    t = j;
			    min = d;
			}
		}
	u.setValue(s);
	v.setValue(t);
    }

    /* 
     * Complete a facet by looking for the circle free point to the left
     * of the edge "e_i".  Add the facet to the triangulation.
     *
     * This function is a bit long and may be better split.
     */
    public void completeFacet(int eI, Triangulation tri, int nFaces) {
	float cP;
	boolean pointFree;
	Edge e[] = tri.edge;
	RealPoint p[] = tri.point;

	// Cache s and t. 
	if (e[eI].l == Triangulation.Undefined)
	    {
		s = e[eI].s;
		t = e[eI].t;
	    }
	else if (e[eI].r == Triangulation.Undefined)
	    {
		s = e[eI].t;
		t = e[eI].s;
	    }
	else
	    // Edge already completed. 
	    return;

	// Find point free circumcircle to the left. 
	for (u = 0; u < tri.nPoints; u++)
	    if (u != s && u != t) {
		if (Vector.crossProduct(p[s], p[t], p[u]) > 0.0) {
		    bC.circumCircle(p[s], p[t], p[u]);
		    animate(triangleState);
		    pointFree = true;
		    for (i = 0; i < tri.nPoints; i++)
			if (i != s && i != t && i != u) {
			    animate(pointState);
			    cP = Vector.crossProduct(p[s], p[t], p[i]);
			    if (cP > 0.0)
				if (bC.inside(p[i])) {
				    animate(insideState);
				    pointFree = false;
				    break;
				}
			}
		    animate(triangulationState);
		    if (pointFree)
			break;
		}
	    }

	// Add new triangle or update edge info if s-t is on hull. 
	if (u < tri.nPoints) {
	    int bP = u;

	    // Update face information of edge being completed. 
	    tri.updateLeftFace(eI, s, t, nFaces);
	    nFaces++;

	    // Add new edge or update face info of old edge. 
	    eI = tri.findEdge(bP, s);
	    if (eI == Triangulation.Undefined)
		// New edge. 
		eI = tri.addEdge(bP, s, nFaces, Triangulation.Undefined);
	    else
		// Old edge. 
		tri.updateLeftFace(eI, bP, s, nFaces);

	    // Add new edge or update face info of old edge. 
	    eI = tri.findEdge(t, bP);
	    if (eI == Triangulation.Undefined)
		// New edge.  
		eI = tri.addEdge(t, bP, nFaces, Triangulation.Undefined);
	    else
		// Old edge.  
		tri.updateLeftFace(eI, t, bP, nFaces); 
	} else
	    tri.updateLeftFace(eI, s, t, Triangulation.Universe); 
    }
}

/* 
 * QuadraticAlgorithm class.  O(n^2) algorithm. 
 */
class QuadraticAlgorithm extends TriangulationAlgorithm {
    int s, t, u, bP;
    Circle bC = new Circle();
    final static String algName = "O(n^2)";
    int nFaces;

    public QuadraticAlgorithm(Triangulation t, RealWindow w, int nPoints) {
	super(t, w, algName, nPoints);
    }

    public void reset() {
	nFaces = 0;
	triCanvas.needToClear = true;
	super.reset();
    }

    public void draw(RealWindowGraphics rWG, Triangulation tri) {
	if (state[triangleState]) {
	    if (aniControl.animate(AnimateControl.triangles)) {
		rWG.drawTriangle(tri.point[s], tri.point[t], tri.point[bP], 
				 Color.green);
		rWG.drawLine(tri.point[s], tri.point[t], Color.blue);
	    }
	    if (aniControl.animate(AnimateControl.circles))
		rWG.drawCircle(bC, Color.green);
	} else if (state[pointState]) {
	    if (aniControl.animate(AnimateControl.points))
		rWG.drawPoint(tri.point[u], Color.orange);
	} else if (state[insideState]) {
	    if (aniControl.animate(AnimateControl.triangles)) {
		rWG.drawTriangle(tri.point[s], tri.point[t], tri.point[bP], Color.red);
		rWG.drawLine(tri.point[s], tri.point[t], Color.blue);
	    }
	    if (aniControl.animate(AnimateControl.circles))
		rWG.drawCircle(bC, Color.red);
	    if (aniControl.animate(AnimateControl.points))
		rWG.drawPoint(tri.point[s], Color.red);
	} else if (state[triangulationState]) {
	    tri.draw(rWG, Color.black, Color.black);
	} else {
	    tri.draw(rWG, Color.black, Color.black);
	}
    }

    public synchronized void triangulate(Triangulation tri) {
	int seedEdge, currentEdge;
	int nFaces;
	Int s, t;

	// Initialise. 
	nFaces = 0;
	s = new Int();
	t = new Int();
  
	// Find closest neighbours and add edge to triangulation. 
	findClosestNeighbours(tri.point, tri.nPoints, s, t);

	// Create seed edge and add it to the triangulation. 
	seedEdge = tri.addEdge(s.getValue(), t.getValue(), 
			       Triangulation.Undefined, 
			       Triangulation.Undefined);

	currentEdge = 0;
	while (currentEdge < tri.nEdges) {
	    if (tri.edge[currentEdge].l == Triangulation.Undefined) {
		completeFacet(currentEdge, tri, nFaces);
		animate(triangulationState);
	    }
	    if (tri.edge[currentEdge].r == Triangulation.Undefined) {
		completeFacet(currentEdge, tri, nFaces);
		animate(triangulationState);
	    }
	    currentEdge++;
	}
    }

    // Find the two closest points.  
    public void findClosestNeighbours(RealPoint p[], int nPoints,
				      Int u, Int v) {
	int i, j;
	float d, min;
	int s, t;

	s = t = 0;
	min = Float.MAX_VALUE;
	for (i = 0; i < nPoints-1; i++)
	    for (j = i+1; j < nPoints; j++)
		{
		    d = p[i].distanceSq(p[j]);
		    if (d < min)
			{
			    s = i;
			    t = j;
			    min = d;
			}
		}
	u.setValue(s);
	v.setValue(t);
    }

    /* 
     * Complete a facet by looking for the circle free point to the left
     * of the edge "e_i".  Add the facet to the triangulation.
     *
     * This function is a bit long and may be better split.
     */
    public void completeFacet(int eI, Triangulation tri, int nFaces) {
	float cP;
	int i;
	Edge e[] = tri.edge;
	RealPoint p[] = tri.point;

	// Cache s and t. 
	if (e[eI].l == Triangulation.Undefined)
	    {
		s = e[eI].s;
		t = e[eI].t;
	    }
	else if (e[eI].r == Triangulation.Undefined)
	    {
		s = e[eI].t;
		t = e[eI].s;
	    }
	else
	    // Edge already completed. 
	    return;
    

	// Find a point on left of edge. 
	for (u = 0; u < tri.nPoints; u++)
	    {
		if (u == s || u == t)
		    continue;
		if (Vector.crossProduct(p[s], p[t], p[u]) > 0.0)
		    break;
	    }

	// Find best point on left of edge. 
	bP = u;
	if (bP < tri.nPoints)
	    {
		bC.circumCircle(p[s], p[t], p[bP]);

		animate(triangleState);

		for (u = bP+1; u < tri.nPoints; u++)
		    {
			if (u == s || u == t)
			    continue;

			animate(pointState);

			cP = Vector.crossProduct(p[s], p[t], p[u]);

			if (cP > 0.0)
			    if (bC.inside(p[u]))
				{
				    animate(insideState);
				    bP = u;
				    bC.circumCircle(p[s], p[t], p[u]);
				    animate(triangleState);
				}
		    }
	    }

	// Add new triangle or update edge info if s-t is on hull. 
	if (bP < tri.nPoints)
	    {
		// Update face information of edge being completed. 
		tri.updateLeftFace(eI, s, t, nFaces);
		nFaces++;

		// Add new edge or update face info of old edge. 
		eI = tri.findEdge(bP, s);
		if (eI == Triangulation.Undefined)
		    // New edge. 
		    eI = tri.addEdge(bP, s, nFaces, Triangulation.Undefined);
		else
		    // Old edge. 
		    tri.updateLeftFace(eI, bP, s, nFaces);

		// Add new edge or update face info of old edge. 
		eI = tri.findEdge(t, bP);
		if (eI == Triangulation.Undefined)
		    // New edge.  
		    eI = tri.addEdge(t, bP, nFaces, Triangulation.Undefined);
		else
		    // Old edge.  
		    tri.updateLeftFace(eI, t, bP, nFaces); 
	    } else
		tri.updateLeftFace(eI, s, t, Triangulation.Universe); 
    }
}

/*
 * AppletUI class. Provides most of the user interface for the applet.
 */
class AppletUI extends Panel {
    AlgorithmUI AlgorithmUI[];

    public AppletUI(TriangulationAlgorithm algorithm[]) {
	Label l;
	Panel p;

	setLayout(new BorderLayout());

	// Per algorithm controls. 
	p = new Panel();
	p.setLayout(new GridLayout(0,1));

	// Headings for algorithm controls. 
	p.add(new AlgorithmUIHeading());

	// One set of controls per algorithm.
	for (int i = 0; i < algorithm.length; i++)
	    p.add(algorithm[i].algorithmUI());

	// Add panel to controls. 
	add("Center", p);

	// Applet controls. 
	p = new Panel();
	p.setLayout(new GridLayout(0,1));
	p.add(new Button("Start"));
	p.add(new Button("Stop"));
	p.add(new Button("New"));
	p.add(new Label("Step Mode", Label.CENTER));
	Choice c = new Choice();
	c.addItem("Auto");
	c.addItem("Manual");
	p.add(c);

	// Add panel to controls. 
	add("East", p);
    }
}

/* 
 * TriangulationApplet class.  "Main Class"
 */
@SuppressWarnings("deprecation")
public class Triangulator extends Applet implements Runnable {
    Thread triangulateThread[];
    int nPoints = 10;
    Triangulation triangulation[];
    TriangulationAlgorithm algorithm[];
    RealWindow w;
    RealWindowGraphics rWG;
    AppletUI appUI;
    public static final int On2 = 0;
    public static final int On3 = 1;
    public static final int On4 = 2;
    Panel canvases;
    static final int nAlgorithms = 3;

    public void init() {

	setBackground(Color.lightGray);
	resize(600,350);

	// Create a rectangle in the real plane for points. 
	w = new RealWindow(0.0f, 0.0f, 1.0f, 1.0f);

	// Create array of triangulations, including random points. 
	triangulation = new Triangulation[nAlgorithms];
	triangulation[0] = new Triangulation(nPoints);
	triangulation[0].randomPoints(w);
	for (int i = 1; i < nAlgorithms; i++) {
	    triangulation[i] = new Triangulation(nPoints);
	    triangulation[i].copyPoints(triangulation[0]);
	}

	// Create an array of triangulation algorithms. 
	algorithm = new TriangulationAlgorithm[nAlgorithms];
	algorithm[0] = new QuadraticAlgorithm(triangulation[0], w, nPoints);
	algorithm[1] = new CubicAlgorithm(triangulation[1], w, nPoints);
	algorithm[2] = new QuarticAlgorithm(triangulation[2], w, nPoints);

	// Array of thread references (one for each algorithm). 
	triangulateThread = new Thread[nAlgorithms];

	// Create user interface. 
	Panel heading = new Panel();
	heading.setLayout(new BorderLayout());
	heading.add("Center", new Label("The Triangulator", Label.CENTER));
	Panel algHeadings = new Panel();
	algHeadings.setLayout(new GridLayout(0, nAlgorithms));
	for (int i = 0; i < nAlgorithms; i++)
	    algHeadings.add(new Label(algorithm[i].algName, Label.CENTER));
	heading.add("South", algHeadings);
	canvases = new Panel();
	canvases.setLayout(new GridLayout(0, nAlgorithms));
	for (int i = 0; i < nAlgorithms; i++)
	    canvases.add(algorithm[i].canvas());
	setLayout(new BorderLayout());
	add("North", heading);
	add("Center", canvases);
	appUI = new AppletUI(algorithm);
	add("South", appUI);
    }

    /*
     * Called for each algorithm thread when started.
     */
    public void run() {
	int algNo;
	String threadName;

	// Work out which algorithm to run from the thread name. 
	threadName = Thread.currentThread().getName();
	algNo = Integer.parseInt(threadName.substring(threadName.length()-1));
	algorithm[algNo].triangulate(triangulation[algNo]);
    }

    public Insets insets() {
	// Right offset is more than left, due to Choice bug.
	return new Insets(5,10,5,15);
    }
    
    /* 
     * Actually start the triangulation algorithms running.
     */
    private synchronized void startTriangulate() {
	for (int i = 0; i < triangulateThread.length; i++)
	    if (triangulateThread[i] != null && triangulateThread[i].isAlive()) {
		stop();
	    }
	for (int i = 0; i < triangulateThread.length; i++) {
	    if (algorithm[i].control().getRun()) {
		triangulateThread[i] = new Thread(this, "Triangulation-" + String.valueOf(i));
		triangulateThread[i].setPriority(Thread.MIN_PRIORITY);
		triangulateThread[i].start();
	    }
	}
    }

    /*
     * Generate new points for the algorithms.
     */
    private synchronized void newPoints() {
	int max, alg;

	stop();

	// Find algorithm with max points. 
	max = 0;
	alg = -1;
	for (int i = 0; i < nAlgorithms; i++)
	    if (algorithm[i].control().getRun() && 
		algorithm[i].control().getNPoints() > max) {
		max = algorithm[i].control().getNPoints();
		alg = i;
	    }

	// Generate maximum number of points. 
	if (alg != -1) {
	    triangulation[alg].setNPoints(algorithm[alg].control().getNPoints());
	    triangulation[alg].randomPoints(w);
	}
    
	/* Now copy points into other algorithms.  This has the effect
	 * that algorithms with the same number of points wind up with
	 * the same points.
	 */
	for (int i = 0; i < nAlgorithms; i++)
	    if (algorithm[i].control().getRun() && i != alg) {
		triangulation[i].setNPoints(algorithm[i].control().getNPoints());
		triangulation[i].copyPoints(triangulation[alg]);
	    }

	for (int i = 0; i < nAlgorithms; i++)
	    if (algorithm[i].control().getRun()) {
		algorithm[i].reset();
		algorithm[i].canvas().repaint();
	    }
    }

    /*
     * Stop the applet. Kill the triangulation algorithm if
     * still triangulating.
     */

    public synchronized void stop() {
	for (int i = 0; i < triangulateThread.length; i++) {
	    if (triangulateThread[i] != null) {
		try { 
		    triangulateThread[i].stop(); 
		} catch (IllegalThreadStateException e) {}
		triangulateThread[i] = null;
	    }
	}
    }

    /* 
     * Gets the current value in a text field.  
     */
    int getValue(TextField tF) {
	int i;
	try {
	    i = Integer.valueOf(tF.getText()).intValue(); 
	} catch (java.lang.NumberFormatException e) {
	    i = 0;
	}
	return i;
    }

    /*
     * Handle main level events. 
     */
    public boolean handleEvent(Event evt) {
	if (evt.id == Event.ACTION_EVENT) {
	    if ("Start".equals(evt.arg)) {
		startTriangulate();
		return true;
	    } else if ("Stop".equals(evt.arg)) {
		stop();
		return true;
	    } else if ("New".equals(evt.arg)) {
		newPoints();
	    } else if ("Manual".equals(evt.arg)) {
		for (int i = 0; i < nAlgorithms; i++)
		    algorithm[i].control().setManualAnimateMode();
		return true;
	    } else if ("Auto".equals(evt.arg)) {
		for (int i = 0; i < nAlgorithms; i++)
		    algorithm[i].control().setAutomaticAnimateMode();
		return true;
	    }
	} else if (evt.id == Event.MOUSE_DOWN) {
	    // These events only occur in the canvases. 
	    for (int i = 0; i < nAlgorithms; i++)
		if (algorithm[i].control().mode() == AnimateControl.manual)
		    algorithm[i].nextStep();
	    return true;
	} else if (evt.id == Event.MOUSE_MOVE) {
	    return true;
	}

	return false;
    }
}
