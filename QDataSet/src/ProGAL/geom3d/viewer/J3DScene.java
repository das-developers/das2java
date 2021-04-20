package ProGAL.geom3d.viewer;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import javax.swing.*;
//
//import javax.media.j3d.*;
//import javax.vecmath.Color3f;
//import javax.vecmath.Matrix3f;
//import javax.vecmath.Vector3d;
//import javax.vecmath.Vector3f;
//
//import com.sun.j3d.utils.geometry.Primitive;
//import com.sun.j3d.utils.geometry.Text2D;
//import com.sun.j3d.utils.picking.PickCanvas;
//import com.sun.j3d.utils.picking.PickResult;
//import com.sun.j3d.utils.picking.PickTool;
//import com.sun.j3d.utils.universe.SimpleUniverse;

import ProGAL.math.Matrix;
import ProGAL.geom3d.*;
import ProGAL.geom3d.surface.ParametricParaboloid;
import ProGAL.geom3d.surface.ParametricSurface;
import ProGAL.geom3d.volumes.LSS;
import ProGAL.geom3d.volumes.Lens;
import ProGAL.geom3d.volumes.OBB;
import ProGAL.geom3d.volumes.RSS;
import ProGAL.geom3d.volumes.Sphere;

/** A graphics class for viewing scenes using Java3D. 
 * All the <code>Shape</code>-subclasses specified in the <code>edu.geom3D</code> 
 * package can be added to a <code>J3DScene</code> object and are automatically 
 * painted on a <code>Canvas3D</code> object. For 
 * instance the following code creates a scene with a cylinder and a red 
 * transparent box and adds the canvas to a frame. 
 * <pre>
 * J3DScene scene = new J3DScene();
 * scene.addShape(  new Cylinder(new Vector(1,0,0), new Vector(0.5,0.5, 0.3), 0.1f) );
 * 
 * Vector boxCorner = new Vector(-1,0,0);
 * Vector[] boxBases = {new Vector(1,0,0), new Vector(0,1,0), new Vector(0,0,1)};
 * float[] boxExtents = {0.8f, 1, 2};
 * Box box = new Box( boxCorner, boxBases, boxExtents );
 * scene.addShape( box, new Color(200,0,0,100) );
 * 
 * Canvas3D canvas = scene.getCanvas();
 * 
 * JFrame frame = new JFrame();
 * frame.setSize(400,400);
 * frame.getContentPane().add( canvas );
 * frame.setVisible(true);
 * </pre>
 * Text can be added to the scene as well and will always face the camera. 
 * 
 * The <code>repaint()</code> method must be called every time the position of 
 * shapes has changed and the canvas should be updated. The pointers 
 * to added shapes are stored, so subsequent changes in the <code>box</code> 
 * object in the above code will be visible on the canvas when <code>repaint()</code> 
 * is called. The following example shows how to animate a sphere rotating around origo.
 * <pre>
 *  J3DScene scene = new J3DScene();
 *  Sphere sphere = new Sphere( new Vector(1,0,0), 0.1f); 
 *  scene.addShape(sphere);
 *  float t = 0;
 *  while(true){
 * 		t+=0.01f;
 * 		sphere.center = new Vector(Math.cos(t), Math.sin(t), 0);
 * 		scene.repaint();
 * 		try{ Thread.sleep(30); }catch(InterruptedException exc){}
 *  }
 * </pre>
 * 
 * A static method is supplied for conveniently creating a frame containing a scene-viewer. 
 * The following example shows how to quickly create a <code>J3DScene</code> object 
 * that is shown in a frame and ready for use:
 * <pre>
 * J3DScene scene = J3DScene.createJ3DSceneInFrame();
 * scene.setAxisEnabled(true);
 * scene.addShape(  new Cylinder(new Vector(1,0,0), new Vector0,1,0), 0.1f) );
 * </pre>
 * @author R. Fonseca
 */
public class J3DScene {
	public JFrame frame;
//	Canvas3D canvas;
//	private BranchGroup sceneRoot, scene;
//	//	private CamBehavior camBehavior;
//	private RebuildBehavior rebuildBehavior;
//	private PickCanvas pickCanvas;
//	private Timer repaintTimer;
//
//	private final BoundingSphere bounds = new BoundingSphere(new javax.vecmath.Point3d(0,0,0), 5000);
//	private Background background;
//	private final LinearFog fog = new LinearFog(); 
//
//	private final Map<Shape,BranchGroup> shapeTransforms = new HashMap<Shape,BranchGroup>();
	final Map<Shape,Color> primitives = new HashMap<Shape,Color>();
//	private final Map<Node, Shape> pickMap = new HashMap<Node,Shape>();
//	private final List<ClickListener> clickListeners = new LinkedList<ClickListener>();
//	private final List<Shape> axisElements = new ArrayList<Shape>();
//	private Camera camera;
//	//	OrbitBehavior orbitBehavior;
//	//	private final Point sceneCenter = new Point(0,0,0);


	/** Set color of background. */
	public void setBackgroundColor(Color c){
//		background.setColor(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f);
//		fog.setColor(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f);
	}

	/** Removes one volume from the scene. */
	public void removeShape(Shape v){
//		primitives.remove(v);
//		BranchGroup bg = shapeTransforms.remove(v);
//		if(bg!=null){
//			bg.detach();
//			scene.removeChild(bg);
//		}
//
//		for(Entry<Node,Shape> entry: new LinkedList<Entry<Node,Shape>>(pickMap.entrySet())){
//			if(entry.getValue()==v){ 
//				pickMap.remove(entry.getKey());
//			}
//		}
//		//		if(camera.getControlPanel()!=null && camera.getControlPanel().isVisible()) 
//		//			camera.collectShapes();
	}

	/** Remove all volumes from the scene. */
	public void removeAllShapes(){
//		while(!primitives.isEmpty())
//			removeShape(primitives.entrySet().iterator().next().getKey());
//		primitives.clear();
//		shapeTransforms.clear();
//		pickMap.clear();
	}


	/** Add a volume object. The standard color gray will be used */
	public void addShape(Shape v){	addShape(v,Color.gray);	}

	/** Add a volume object with a specified color */
	public void addShape(Shape v, Color c){	
		addShape(v,c,12);
	}

	/** Add a volume object with a specified color and detail-level */
	public void addShape(Shape v, Color c, int divisions){	
//		primitives.put(v, c);
//		Node p = genPrimitive(v, c, divisions);
//		if(p!=null){
//			scene.addChild(p);
//
//			//			if(camera!=null && camera.getControlPanel()!=null && camera.getControlPanel().isVisible()) 
//			//				camera.collectShapes();
//		}
	}

	/** Add a text-object at the specified position. */
	public TextShape addText(String t, Point pos){ 
		TextShape text = new TextShape(t,pos);
		addShape(text, Color.GRAY); 
		return text;
	}
	public void addText(String t, Point pos, double height){ 
		addShape(new TextShape(t,pos,height), Color.GRAY); 
	}
	public TextShape addText(String t, Point pos, double height, Color c){
		TextShape text = new TextShape(t,pos,height);
		addShape(text, c);
		return text;
	}


	public void addSurface(ParametricSurface surface){
		addSurface(surface, Color.GRAY, -10, 10, 10, -10, 10, 10);
	}
	public void addSurface(ParametricSurface surface, Color col){
		addSurface(surface, col, -10, 10, 10, -10, 10, 10);
	}
	public void addSurface(ParametricSurface surface, Color col, double uMin, double uMax, int uDivs, double vMin, double vMax, int vDivs){
		primitives.put(surface, col);
//		Node p = genSurface(surface,uMin, uMax, uDivs, vMin, vMax, vDivs, col);
//		if(p!=null)	scene.addChild(p);
	}

	/** Sets the location that the camera looks at to the center of all the shapes added 
	 * to the scene.  */
	public void centerCamera(){
		Vector newCenter = new Vector(0,0,0);

		if(!primitives.isEmpty()){
			for(Entry<Shape, Color> entry: primitives.entrySet()){
//				System.out.println(entry.getKey()+" .. "+entry.getKey().getCenter().toVector());
				newCenter.addThis(entry.getKey().getCenter().toVector());
			}
			newCenter.multiplyThis(1f/primitives.entrySet().size());
		}
		//		centerCamera(newCenter.toPoint());

		//		Transform3D transform = new Transform3D();
		//		transform.setTranslation(new Vector3f(-(float)newCenter.x(), -(float)newCenter.y(), -(float)newCenter.z()));
		//		TransformGroup tg = ((TransformGroup)((TransformGroup)sceneRoot.getChild(0)).getChild(0));
		//		tg.setTransform(transform);
		//		sceneCenter = newCenter.toPoint();


		//camera.setLookingAt(newCenter.toPoint());
	}

	public void centerCamera(Point newCenter){
//		Point lookingAt = camera.getLookingAt();
//		Line l = new Line(lookingAt.clone(), camera.getLookingAt().vectorTo(newCenter));
//		for(double t=0;t<=1;t+=(Math.sin(t*Math.PI)/10+0.01)){
//			lookingAt.set(l.getPoint(t));
//			camera.updateView();
//			//			float x = (float)( sceneCenter.x()*(1-t) + newCenter.x()*t );
//			//			float y = (float)( sceneCenter.y()*(1-t) + newCenter.y()*t );
//			//			float z = (float)( sceneCenter.z()*(1-t) + newCenter.z()*t );
//			//			Transform3D transform = new Transform3D();
//			//			transform.setTranslation(new Vector3f(-x,-y,-z));
//			//			TransformGroup tg = ((TransformGroup)((TransformGroup)sceneRoot.getChild(0)).getChild(0));
//			//			tg.setTransform(transform);
//			try{Thread.sleep(50);}catch(InterruptedException exc){}
//		}
//		//		Transform3D transform = new Transform3D();
//		//		transform.setTranslation(new Vector3f(-(float)newCenter.x(), -(float)newCenter.y(), -(float)newCenter.z()));
//		//		TransformGroup tg = ((TransformGroup)((TransformGroup)sceneRoot.getChild(0)).getChild(0));
//		//		tg.setTransform(transform);
//		//		sceneCenter.set(newCenter);
	}

	/** Zooms such that the maximal distance between two objects is within the view */
	public void autoZoom(){
//		if(primitives.isEmpty()) return;
//		//View axis
//		Line l = new Line(camera.getEye(), camera.getEye().vectorTo(camera.getLookingAt()).normalizeThis());
//		double tanAlpha = Math.tan(0.8*camera.getViewAngle()/2);
//		try{
//			double minT = Double.POSITIVE_INFINITY;
//			for(Entry<Shape, Color> entry: primitives.entrySet()){
//				Point p = entry.getKey().getCenter();
//				double tProj = l.orthogonalProjectionParameter(p);
//				double h = p.distance(l.getPoint(tProj));
//				double t = -h/tanAlpha+tProj;
//				if(t<minT) minT = t;
//			}
//			camera.setLocation(l.getPoint(minT));
//		}catch(ConcurrentModificationException exc){
//			try{ Thread.sleep(300); }catch(InterruptedException exc2){}
//			autoZoom();
//			return;
//		}
//
//		//				double maxDist = 0;
//		//				try{
//		//					for(Entry<Shape, Color> entry: primitives.entrySet()){
//		//						for(Entry<Shape, Color> entry2: primitives.entrySet()){
//		//							double d = entry.getKey().getCenter().distance(entry2.getKey().getCenter());
//		//							if(d>maxDist) maxDist=d;
//		//						}
//		//					}
//		//				}catch(ConcurrentModificationException exc){
//		//					try{ Thread.sleep(300); }catch(InterruptedException exc2){}
//		//					autoZoom();
//		//					return;
//		//				}
//		//				if(maxDist>0){
//		//					this.camBehavior.setScale(4/(maxDist+10));
//		//					this.repaint();
//		//				}
	}

	private boolean parallelProjection = false;

	/** Enables and disables parallel projection (as opposed to perspective projection). */
	public void setParallelProjection(boolean enabled) {
//		if(enabled && !parallelProjection){
//			canvas.getView().setProjectionPolicy(View.PARALLEL_PROJECTION);
//		}
//		if(!enabled && parallelProjection){
//			canvas.getView().setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
//		}
//		parallelProjection = enabled;
	}


	public void setAntialiasing(boolean enabled){
		//canvas.getView().setSceneAntialiasingEnable(enabled);
	}

//	private class RotThread extends Thread {
//		boolean stop = false;
//		public void run() {
//			stop = false;
//			while(!stop){
//				//				camBehavior.rotate(0.01f);
//				double angle = 0.01;
//				Point eye = camera.getEye();
//				Point center = camera.getLookingAt();
//				Vector up = camera.getUp();
//				Vector x = eye.vectorTo(center);
//				Vector y = x.cross(up);
//				eye = eye.addThis(x.multiplyThis(1-Math.cos(angle))).addThis(y.multiplyThis(Math.sin(angle)));
//				camera.updateView();
//				try {Thread.sleep(40);} catch (InterruptedException e) {	}
//			}
//		}
//	}
//
//	private RotThread rotThread;

	/** Toggles rotation */
	public void toggleRotation(){
//		//Thread t = new RotThread();
//		//t.start();
//		if(rotThread!=null && rotThread.isAlive()){
//			rotThread.stop = true;
//		}else{
//			rotThread = new RotThread();
//			rotThread.start();
//		}
	}

//	/**
//	 * Add a click-listener that gets called every time an object or the background is clicked
//	 * @param cl
//	 */
//	public void addClickListener(ClickListener cl){
//		clickListeners.add(cl);
//	}
//	public List<ClickListener> getClickListeners(){
//		return clickListeners;
//	}


	private boolean axisEnabled = false;
	/** Enables or disables xyz-axis from the origo */
	public void setAxisEnabled(boolean axisEnabled){
//		if(axisEnabled && axisElements.isEmpty()){
//			float rad = 0.02f;
//			axisElements.add(new ProGAL.geom3d.volumes.Cylinder(new Point(0,0,0),new Point(1-2*rad,0,0), rad));
//			axisElements.add(new ProGAL.geom3d.volumes.Cylinder(new Point(0,0,0),new Point(0,1-2*rad,0), rad));
//			axisElements.add(new ProGAL.geom3d.volumes.Cylinder(new Point(0,0,0),new Point(0,0,1-2*rad), rad));
//			axisElements.add(new ProGAL.geom3d.volumes.Cone(new Point(1,0,0), new Point(1-2*rad,0,0), 2*rad));
//			axisElements.add(new ProGAL.geom3d.volumes.Cone(new Point(0,0,1), new Point(0,0,1-2*rad), 2*rad));
//			axisElements.add(new ProGAL.geom3d.volumes.Cone(new Point(0,1,0), new Point(0,1-2*rad,0), 2*rad));
////			axisElements.add(new ProGAL.geom3d.volumes.Cone(new Point(1-2*rad,0,0),new Point(1,0,0), 2*rad));
////			axisElements.add(new ProGAL.geom3d.volumes.Cone(new Point(0,0,1-2*rad),new Point(0,0,1), 2*rad));
////			axisElements.add(new ProGAL.geom3d.volumes.Cone(new Point(0,1-2*rad,0),new Point(0,1,0), 2*rad));
//
//			axisElements.add(new TextShape("x", new Point(1,0,0), 0.3));
//			axisElements.add(new TextShape("y", new Point(0,1,0), 0.3));
//			axisElements.add(new TextShape("z", new Point(0,0,1), 0.3));
//		}
//		if(axisEnabled && !this.axisEnabled){
//			for(Shape s: axisElements) addShape(s, Color.GRAY);
//		}
//		if(!axisEnabled && this.axisEnabled){
//			for(Shape s: axisElements) removeShape(s);
//		}
//		this.axisEnabled = axisEnabled;
	}
//
//
//	private void initialBuild(){
//		sceneRoot = new BranchGroup();
//		sceneRoot.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
//		TransformGroup tgroup = new TransformGroup();
//		sceneRoot.addChild(tgroup);
//		tgroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
//		tgroup.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
//		tgroup.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
//		tgroup.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
//
//		//		camBehavior = new CamBehavior(tgroup);
//		//		camBehavior.setSchedulingBounds(bounds);
//		//		sceneRoot.addChild(camBehavior);
//
//		//rebuildBehavior = new RebuildBehavior(tgroup);
//		rebuildBehavior = new RebuildBehavior();
//		rebuildBehavior.setSchedulingBounds(bounds);
//		sceneRoot.addChild(rebuildBehavior);
//
//
//		//BranchGroup scene = buildScene();
//		//BranchGroup scene = new BranchGroup();
//
//		Transform3D transform = new Transform3D();
//		transform.setTranslation(toJ3DVec(new Vector(0,0,0)));
//		TransformGroup tg = new TransformGroup(transform);
//		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
//
//		scene = new BranchGroup();
//		//scene.setCapability(BranchGroup.ALLOW_DETACH);
//		scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
//		scene.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
//
//		for(Entry<Shape, Color> entry: primitives.entrySet())
//			scene.addChild(genPrimitive(entry.getKey(), entry.getValue(), 32));
//		//for(TextPrimitive tp: texts) movedScene.addChild(genTextPrimitive(tp));
//		genLights(scene);
//		scene.addChild(genBackground());
//		//if(paintAxis) scene.addChild(genAxis());
//		tg.addChild(scene);
//		//scene.addChild(tg);
//		tgroup.addChild(tg);
//
//		fog.setColor(new Color3f(Color.WHITE));
//		fog.setFrontDistance(9);
//		fog.setBackDistance(10);
//		//	    fog.setCapability(Fog.ALLOW_COLOR_WRITE);
//		fog.setCapability(LinearFog.ALLOW_DISTANCE_WRITE);
//		fog.setCapability(LinearFog.ALLOW_DISTANCE_READ);
//		fog.setCapability(LinearFog.ALLOW_COLOR_WRITE);
//		fog.setInfluencingBounds(bounds);
//		scene.addChild(fog);
//
//		//tgroup.addChild(scene);
//		scene.compile();
//		sceneRoot.compile();
//
//
//	}

	public J3DScene(){
		//this.initialBuild();
	}

	//public Camera getCamera(){
	//	return camera;
	//}


	//	private class HudCanvas3D extends Canvas3D implements MouseListener{
	//		private static final long serialVersionUID = 1L;
	//
	//		public HudCanvas3D(GraphicsConfiguration arg0) {
	//			super(arg0);
	//			this.addMouseListener(this);
	//		}
	//
	//		public void postRender(){
	//			super.postRender();
	//			J3DGraphics2D g = super.getGraphics2D();
	//			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	//			int w = 30;
	//			int h = 30;
	//			int x = getWidth()-w-2;
	//			int y = getHeight()-h-2;
	//
	//			//Fill circle
	//			g.setColor(new Color(200,200,200));
	//			g.fillOval(x, y, w, h);
	//			x+=2;y+=2;w-=4;h-=4;
	//			g.setColor(new Color(100,100,200));
	//			g.fillOval(x, y, w, h);
	//
	//			//Highlights
	//			for(int i=1;i<10;i++){
	//				g.setColor(new Color(200-10*i,200-10*i,250-5*i));
	//				g.fillOval((int)(x+5), y+i, (int)(w-10), h/2-i);
	//			}
	//			for(int i=1;i<6;i++){
	//				g.setColor(new Color(200-20*i,200-20*i,250-10*i));
	//				g.fillOval((int)(x+5), y-i+h/2, (int)(w-10), h/2-i);
	//			}
	//
	//			//Camera
	//			g.setColor(Color.WHITE);
	//			g.fillRoundRect((int)(x+w*0.16), (int)(y+h*0.47), (int)(w*0.5), (int)(h*0.3), (int)(w*0.15), (int)(w*0.15));
	//			g.fillOval((int)(x+w*0.13), (int)(y+h*0.27), (int)(h*0.25), (int)(h*0.25));
	//			g.fillOval((int)(x+w*0.4), (int)(y+h*0.22), (int)(h*0.3), (int)(h*0.3));
	//			g.fillPolygon(
	//					new int[]{(int)(x+w*0.69), (int)(x+w*0.69), (int)(x+w*0.84), (int)(x+w*0.84)}, 
	//					new int[]{(int)(y+h*0.68), (int)(y+h*0.50), (int)(y+h*0.45), (int)(y+h*0.71)}, 4 );
	//
	//			g.flush(false);
	//		}
	//
	//		public void mouseClicked(MouseEvent arg0) {	}
	//		public void mouseEntered(MouseEvent arg0) {}
	//		public void mouseExited(MouseEvent arg0) {	}
	//		public void mousePressed(MouseEvent arg0) {	}
	//		public void mouseReleased(MouseEvent arg0) {
	//			java.awt.Point p = arg0.getPoint();
	//
	//			int w = 30;
	//			int h = 30;
	//			int x = getWidth()-w-2;
	//			int y = getHeight()-h-2;
	//			java.awt.Point buttonCenter = new java.awt.Point(x+w/2, y+h/2);
	//			if(p.distance(buttonCenter)<15){
	//				JFrame ctrlPanel = camera.getControlPanel();
	//				if(ctrlPanel==null) camera.createControlPanel();
	//				ctrlPanel = camera.getControlPanel();
	//				ctrlPanel.setVisible(true);
	//				java.awt.Point loc = arg0.getLocationOnScreen();
	//				loc.x-=ctrlPanel.getWidth();
	//				loc.y-=ctrlPanel.getHeight();
	//				ctrlPanel.setLocation(loc);
	//				camera.collectShapes();
	//			}
	//		}
	//
	//	}

	/** Get the canvas that displays the scene. If this method is called 
	 * several times the same <code>Canvas3D</code> object will be returned 
	 * every time.*/
//	public Canvas3D getCanvas(){
//            return null;
//		if(canvas==null){
//			//initialBuild();
//
//			//			GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
//			GraphicsConfigTemplate3D template = new GraphicsConfigTemplate3D();
//			template.setSceneAntialiasing(GraphicsConfigTemplate3D.PREFERRED);
//			template.setRedSize(6);
//			template.setGreenSize(6);
//			template.setBlueSize(6);//Fixes the weird ugly rastering on mac 
//			GraphicsConfiguration config =
//					GraphicsEnvironment.getLocalGraphicsEnvironment().
//					getDefaultScreenDevice().getBestConfiguration(template);
//
//			//			canvas = new HudCanvas3D(config);
//			canvas = new Canvas3D(config);
//
//			SimpleUniverse universe = new SimpleUniverse(canvas);
//			//universe.getViewer().getView().setProjectionPolicy(View.PARALLEL_PROJECTION);
//			universe.addBranchGraph(sceneRoot);
//			universe.getViewingPlatform().setNominalViewingTransform();
//			universe.getViewer().getView().setLocalEyeLightingEnable(true);
//
//			camera = new Camera(this, universe.getViewingPlatform(), fog);
//			//						universe.getViewer().getView().setSceneAntialiasingEnable(true);
//
//			//			CamListener cl = new CamListener();
//			//			canvas.addMouseListener(cl);
//			//			canvas.addMouseMotionListener(cl);
//			//			canvas.addKeyListener(cl);
//			//			canvas.addMouseWheelListener(cl);
//
//			//			orbitBehavior = new OrbitBehavior(canvas,
//			//					OrbitBehavior.PROPORTIONAL_ZOOM | OrbitBehavior.REVERSE_ROTATE
//			//					| OrbitBehavior.REVERSE_TRANSLATE );
//			//			orbitBehavior.setSchedulingBounds(bounds);    
//			//			universe.getViewingPlatform().setViewPlatformBehavior(orbitBehavior);
//
//			pickCanvas = new PickCanvas(canvas, sceneRoot);
//			pickCanvas.setMode(PickCanvas.GEOMETRY);
//			addClickListener(new ClickListener(){
//				public void shapeClicked(Shape shape, MouseEvent e) {
//					if(e.getClickCount()==2 && shape!=null){
//						centerCamera(shape.getCenter());
//						//						camera.setLookingAt(shape.getCenter());
//					}
//				}});
//
//			canvas.addMouseListener(new PickListener());
//
//			canvas.getView().setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);
//
//		}
//
//		return canvas;
//	}

	/** Repaint the canvas. If the scene has been changed in any way the 
	 * scene displayer will update the view when <code>repaint()</code> is called 
	 * and no sooner. If the scene is repeatedly changed, and repaint repeatedly 
	 * called the viewer will show an animation. */
	public void repaint(){
//		rebuildBehavior.rebuild();

		//		if(camera.getControlPanel()!=null && camera.getControlPanel().isVisible()) 
		//			camera.collectShapes();
	}

	/** Repaint the canvas repeatedly every <code>millisecondDelay</code> milliseconds. */
	public void repaintRepeatedly(long millisecondDelay){
//		if(repaintTimer!=null){
//			repaintTimer.cancel();
//		}else{
//			repaintTimer = new Timer();
//		}
//		class RepaintTask extends TimerTask{
//			public void run() {
//				repaint();
//			}
//		}
//		repaintTimer.schedule(new RepaintTask(), 1, millisecondDelay);
	}



	//		private class CamListener extends MouseAdapter implements MouseMotionListener, MouseWheelListener, KeyListener{
	//			private boolean shiftPressed = false;
	//			private java.awt.Point lastPoint = null;
	//			private long lastTime = System.currentTimeMillis();
	//			public void mousePressed(MouseEvent e) {	lastPoint = e.getPoint();		}
	//			public void mouseReleased(MouseEvent e) {	lastPoint = null; }
	//			public void mouseClicked(MouseEvent e){
	//				rebuildBehavior.rebuild();
	//			}
	//	
	//			public void mouseDragged(MouseEvent e) {
	//				if(lastPoint==null) {
	//					lastPoint = e.getPoint();
	//					lastTime = System.currentTimeMillis();
	//					return;
	//				}
	//				java.awt.Point point = e.getPoint();
	//				float dX = point.x-lastPoint.x;
	//				float dY = point.y-lastPoint.y;
	//				float damper = Math.max(10, (float)(System.currentTimeMillis()-lastTime))*10f;
	//	
	//				if(shiftPressed){
	//					Vector delta = new Vector(dX, -dY, 0).multiplyThis(1/damper);
	//					camBehavior.translate(delta);
	//				}else{
	//					
	//					camBehavior.rotate(dX*(float)Math.PI/damper);
	//				}
	//				lastPoint = point;
	//				lastTime = System.currentTimeMillis();
	//	
	//			}
	//	
	//			public void mouseWheelMoved(MouseWheelEvent e){
	//				float damper = Math.max(10, (float)(System.currentTimeMillis()-lastTime))*10f;
	//				camBehavior.scale(e.getWheelRotation()/damper);
	//				
	////				orbitBehavior.setZoomFactor(orbitBehavior.getZoomFactor()+e.getWheelRotation());
	////				System.out.println(orbitBehavior.getZoomFactor());
	//				lastTime = System.currentTimeMillis();
	//			}
	//	
	//			public void mouseMoved(MouseEvent e) {}
	//			public void keyPressed(KeyEvent e) {
	//				if( e.getKeyCode()==KeyEvent.VK_SHIFT )	shiftPressed = true;
	//	
	//				if(e.getKeyCode()==KeyEvent.VK_DOWN && shiftPressed){
	//					float damper = Math.max(10, (float)(System.currentTimeMillis()-lastTime));
	//					camBehavior.scale(10f/damper);
	//					lastTime = System.currentTimeMillis();
	//				}
	//				if(e.getKeyCode()==KeyEvent.VK_UP && shiftPressed){
	//					float damper = Math.max(10, (float)(System.currentTimeMillis()-lastTime));
	//					camBehavior.scale(-10f/damper);
	//					lastTime = System.currentTimeMillis();
	//				}
	//				if(e.getKeyCode()==KeyEvent.VK_LEFT && shiftPressed){
	//					camBehavior.rotate(0.1f);
	//				}
	//				if(e.getKeyCode()==KeyEvent.VK_RIGHT && shiftPressed){
	//					camBehavior.rotate(-0.1f);
	//				}
	//	
	//				if(e.getKeyCode()==KeyEvent.VK_UP && !shiftPressed){
	//					camBehavior.translate(new Vector(0,-0.1,0));
	//				}
	//				if(e.getKeyCode()==KeyEvent.VK_DOWN && !shiftPressed){
	//					camBehavior.translate(new Vector(0,0.1,0));
	//				}
	//				if(e.getKeyCode()==KeyEvent.VK_LEFT && !shiftPressed){
	//					camBehavior.translate(new Vector(0.1,0,0));
	//				}
	//				if(e.getKeyCode()==KeyEvent.VK_RIGHT && !shiftPressed){
	//					camBehavior.translate(new Vector(-0.1,0,0));
	//				}
	//				if(e.getKeyCode()==KeyEvent.VK_S){
	//					J3DImageFileWriter.writeJPEGFile("J3DScene.jpg", canvas);
	//					System.out.println("Stored view to J3DScene.jpg");
	//				}
	//				if(e.getKeyCode()==KeyEvent.VK_E){
	//					J3DImageFileWriter.writeEPSFile("J3DScene.eps", canvas);
	//					System.out.println("Stored view to J3DScene.eps");
	//				}
	//				if(e.getKeyCode()==KeyEvent.VK_C){
	//					J3DScene.this.centerCamera();
	//				}
	//				if(e.getKeyCode()==KeyEvent.VK_Z){
	//					J3DScene.this.autoZoom();
	//				}
	//				if(e.getKeyCode()==KeyEvent.VK_R){
	//					J3DScene.this.toggleRotation();
	//				}
	//				if(e.getKeyCode()==KeyEvent.VK_P){
	//					J3DScene.this.setParallelProjection(!parallelProjection);
	//				}
	//				if(e.getKeyCode()==KeyEvent.VK_A){
	//					J3DScene.this.setAxisEnabled(!axisEnabled);
	//				}
	//			}
	//			public void keyReleased(KeyEvent e) {
	//				if( e.getKeyCode()==KeyEvent.VK_SHIFT ) shiftPressed = false;
	//			}
	//			public void keyTyped(KeyEvent e) {}
	//	
	//		}

	//	private static class CamBehavior extends Behavior {
	//
	//		private TransformGroup transformGroup;
	//		private Transform3D trans = new Transform3D();
	//		private WakeupCriterion criterion;
	//		private double yAngle = 0.0f;
	//		private Vector3f translation = new Vector3f(0,0,0);
	//		private double scale = 1f;
	//		
	////		private Point3d eye, center;
	////		private Vector3d up;
	//
	//
	//
	//		private final int ROTATE = 1;
	//
	//		// create a new RotateBehavior
	//		CamBehavior(TransformGroup tg) {	
	//			transformGroup = tg;
	////			eye = new Point3d(0,0,1);
	////			center = new Point3d(0,0,0);
	////			up = new Vector3d(0,1,0);
	//		}
	//
	//		// initialize behavior to wakeup on a behavior post with id = ROTATE
	//		public void initialize() {
	//			criterion = new WakeupOnBehaviorPost(this, ROTATE);
	//			wakeupOn(criterion);
	//		}
	//
	//		// processStimulus to rotate the cube
	//		@SuppressWarnings("rawtypes")
	//		public void processStimulus(Enumeration criteria) {
	//			trans.rotY(yAngle);
	//			trans.setTranslation(translation);
	//			trans.setScale(scale);
	////			trans.lookAt(eye, center, up);
	//			transformGroup.setTransform(trans);
	//			wakeupOn(criterion);
	//			//System.out.println("Scale "+scale);
	//		}
	//
	//		// when the mouse is clicked, postId for the behavior
	//		void rotate(float dY) {
	//			yAngle+=dY;
	//			postId(ROTATE);
	//		}
	//		void translate(Vector delta){
	//			translation.add(new Vector3f((float)delta.x(), (float)delta.y(), (float)delta.z()));
	//			postId(ROTATE);
	//		}
	//		void scale(double s){
	//			scale-=s;
	//			if(scale<=0.001) scale=0.001f;
	//			postId(ROTATE);
	//		}
	//		void setScale(double s){
	//			scale=s;
	//			if(scale<=0.001) scale=0.001f;
	//			postId(ROTATE);
	//		}
	//	}


	/** 
	 * Create a frame containing a canvas, display it and return the  J3DScene object shown in the frame. 
	 * The frame can be retrieved using the <code>J3DScene.frame</code> field.  
	 */
	public static J3DScene createJ3DSceneInFrame() {
            return null;
	}


}
