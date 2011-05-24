package sample;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.filter.VisibilityFilter;
import prefuse.action.layout.Layout;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.activity.Activity;
import prefuse.controls.ControlAdapter;
import prefuse.controls.PanControl;
import prefuse.controls.ZoomControl;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Tuple;
import prefuse.data.expression.AbstractPredicate;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.PolygonRenderer;
import prefuse.render.Renderer;
import prefuse.render.ShapeRenderer;
import prefuse.util.ColorLib;
import prefuse.util.GraphicsLib;
import prefuse.util.ui.JValueSlider;
import prefuse.visual.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;


/**
 * This demo adds a visible degree filter to the AggregateDemo.
 * This filter is implemented through the use of a predicate, which
 * evaluates the number of visible edges connected with a node
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author <a href="http://goosebumps4all.net">martin dudek</a>
 */

public class AggregateDemoVisibleDegreeFilter extends JPanel {

    public static final String GRAPH = "graph";
    public static final String NODES = "graph.nodes";
    public static final String EDGES = "graph.edges";
    public static final String AGGR = "aggregates";
    
    private Visualization m_vis;

    public AggregateDemoVisibleDegreeFilter() {
	// initialize display and data
	 m_vis = new Visualization();
	 
	
	initDataGroups();

	// set up the renderers
	// draw the nodes as basic shapes
	Renderer nodeR = new ShapeRenderer(20);
	// draw aggregates as polygons with curved edges
	Renderer polyR = new PolygonRenderer(Constants.POLY_TYPE_CURVE);
	((PolygonRenderer)polyR).setCurveSlack(0.15f);

	DefaultRendererFactory drf = new DefaultRendererFactory();
	drf.setDefaultRenderer(nodeR);
	drf.add("ingroup('aggregates')", polyR);
	m_vis.setRendererFactory(drf);

	// set up the visual operators
	// first set up all the color actions
	ColorAction nStroke = new ColorAction(NODES, VisualItem.STROKECOLOR);
	nStroke.setDefaultColor(ColorLib.gray(100));
	nStroke.add("_hover", ColorLib.gray(50));

	ColorAction nFill = new ColorAction(NODES, VisualItem.FILLCOLOR);
	nFill.setDefaultColor(ColorLib.gray(255));
	nFill.add("_hover", ColorLib.gray(200));

	ColorAction nEdges = new ColorAction(EDGES, VisualItem.STROKECOLOR);
	nEdges.setDefaultColor(ColorLib.gray(100));

	ColorAction aStroke = new ColorAction(AGGR, VisualItem.STROKECOLOR);
	aStroke.setDefaultColor(ColorLib.gray(200));
	aStroke.add("_hover", ColorLib.rgb(255, 100, 100));

	int[] palette = new int[] {
		ColorLib.rgba(255, 200, 200, 150),
		ColorLib.rgba(200, 255, 200, 150),
		ColorLib.rgba(200, 200, 255, 150)
	};
	ColorAction aFill = new DataColorAction(AGGR, "id",
		Constants.NOMINAL, VisualItem.FILLCOLOR, palette);

	//MAD - the new predicate
	final VisibleDegreePredicate vdp = new VisibleDegreePredicate(m_vis,NODES,0); 
	
	VisibilityFilter filter = new VisibilityFilter(NODES,vdp);

	// bundle the color actions
	ActionList colors = new ActionList();
	colors.add(nStroke);
	colors.add(nFill);
	colors.add(nEdges);
	colors.add(aStroke);
	colors.add(aFill);
	colors.add(filter);

	// now create the main layout routine
	ActionList layout = new ActionList(Activity.INFINITY);
	layout.add(colors);
	layout.add(new ForceDirectedLayout(GRAPH, true));
	layout.add(new AggregateLayout(AGGR));
	layout.add(new RepaintAction());
	m_vis.putAction("layout", layout);
	
	 Display display = new Display(m_vis);

	// set up the display
	 display.setSize(500,500);
	 display.pan(250, 250);
	 display.setHighQuality(true);
	 display.addControlListener(new AggregateDragControl());
	 display.addControlListener(new ZoomControl());
	 display.addControlListener(new PanControl());

	// set things running
	m_vis.run("layout");
	
	
	final JValueSlider vslider = new JValueSlider("Visibile degree", 0, 4, 0);
	vslider.addChangeListener(new ChangeListener() {
	    public void stateChanged(ChangeEvent e) {
		 
		vdp.setMinDegree(vslider.getValue().intValue());
		m_vis.run("colors");
	    }
	});
	
	vslider.setBackground(Color.WHITE);
	vslider.setPreferredSize(new Dimension(400,30));
	vslider.setMaximumSize(new Dimension(400,30));

	
	Box both = new Box(BoxLayout.Y_AXIS);
	
	both.add(display);
	both.add(vslider);
	
	add(both);

    }

    private void initDataGroups() {
	// create sample graph
	// 9 nodes broken up into 3 interconnected cliques
	Graph g = new Graph();
	for ( int i=0; i<3; ++i ) {
	    Node n1 = g.addNode();
	    Node n2 = g.addNode();
	    Node n3 = g.addNode();
	    g.addEdge(n1, n2);
	    g.addEdge(n1, n3);
	    g.addEdge(n2, n3);
	}
	g.addEdge(0, 3);
	g.addEdge(3, 6);
	g.addEdge(6, 0);

	// add visual data groups
	VisualGraph vg = m_vis.addGraph(GRAPH, g);
	m_vis.setInteractive(EDGES, null, false);
	m_vis.setValue(NODES, null, VisualItem.SHAPE,
		new Integer(Constants.SHAPE_ELLIPSE));

	AggregateTable at = m_vis.addAggregates(AGGR);
	at.addColumn(VisualItem.POLYGON, float[].class);
	at.addColumn("id", int.class);

	// add nodes to aggregates
	// create an aggregate for each 3-clique of nodes
	Iterator nodes = vg.nodes();
	for ( int i=0; i<3; ++i ) {
	    AggregateItem aitem = (AggregateItem)at.addItem();
	    aitem.setInt("id", i);
	    for ( int j=0; j<3; ++j ) {
		aitem.addItem((VisualItem)nodes.next());
	    }
	}

	Iterator edges = vg.edges();
	int n=0;
	while (edges.hasNext()) {
	    EdgeItem ei = (EdgeItem) edges.next();
	    if((n++)%2 == 0) {
		ei.setVisible(false);
	    }
	}
    }

    public static void main(String[] argv) {
	JFrame frame = demo();
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.setVisible(true);
    }

    public static JFrame demo() {
	AggregateDemoVisibleDegreeFilter ad = new AggregateDemoVisibleDegreeFilter();

	
	

	
	
	JFrame frame = new JFrame("p r e f u s e  |  visible degree predicate");
	frame.getContentPane().add(ad);
	frame.pack();
	return frame;
    }

    class VisibleDegreePredicate extends AbstractPredicate {

	private Visualization vis;
	private String nodeGroupName;
	
	private int minDegree;
	

	public VisibleDegreePredicate(Visualization vis, String nodeGroupName) {
	    this(vis,nodeGroupName,1);
	}

	public VisibleDegreePredicate(Visualization vis, String nodeGroupName,int minDegree) {
	    this.vis = vis;
	    this.nodeGroupName = nodeGroupName;
	    this.minDegree = minDegree;	
	}
	
	public int getMinDegree() { 
	    return minDegree; 
	} 
	    
	public void setMinDegree(int n) { 
	    minDegree=n; 
	} 

	public boolean getBoolean(Tuple t) {
	    if (t instanceof Edge) {
		return false;
	    }

	    NodeItem vi = (NodeItem) vis.getVisualItem(nodeGroupName, t);

	    Iterator iter = vi.edges();

	    boolean result = false;
	    int n=0;
	    while (!result && iter.hasNext()) {
		EdgeItem aEdgeItem = (EdgeItem) iter.next();
		if (aEdgeItem.isVisible()) {
		    n++;
		}
		if (n>= minDegree) {
		    result = true;
		}

	    }

	    return result;
	}


    }

} // end of class AggregateDemo


/**
 * Layout algorithm that computes a convex hull surrounding
 * aggregate items and saves it in the "_polygon" field.
 */
class AggregateLayout extends Layout {

    private int m_margin = 5; // convex hull pixel margin
    private double[] m_pts;   // buffer for computing convex hulls

    public AggregateLayout(String aggrGroup) {
	super(aggrGroup);
    }


    public void run(double frac) {

	AggregateTable aggr = (AggregateTable)m_vis.getGroup(m_group);
	// do we have any  to process?
	int num = aggr.getTupleCount();
	if ( num == 0 ) return;

	// update buffers
	int maxsz = 0;
	for ( Iterator aggrs = aggr.tuples(); aggrs.hasNext();  )
	    maxsz = Math.max(maxsz, 4*2*
		    ((AggregateItem)aggrs.next()).getAggregateSize());
	if ( m_pts == null || maxsz > m_pts.length ) {
	    m_pts = new double[maxsz];
	}

	// compute and assign convex hull for each aggregate
	Iterator aggrs = m_vis.visibleItems(m_group);
	while ( aggrs.hasNext() ) {
	    AggregateItem aitem = (AggregateItem)aggrs.next();

	    int idx = 0;
	    if ( aitem.getAggregateSize() == 0 ) continue;
	    VisualItem item = null;
	    Iterator iter = aitem.items();
	    while ( iter.hasNext() ) {
		item = (VisualItem)iter.next();
		if ( item.isVisible() ) {
		    addPoint(m_pts, idx, item, m_margin);
		    idx += 2*4;
		}
	    }
	    // if no aggregates are visible, do nothing
	    if ( idx == 0 ) continue;

	    // compute convex hull
	    double[] nhull = GraphicsLib.convexHull(m_pts, idx);

	    // prepare viz attribute array
	    float[]  fhull = (float[])aitem.get(VisualItem.POLYGON);
	    if ( fhull == null || fhull.length < nhull.length )
		fhull = new float[nhull.length];
	    else if ( fhull.length > nhull.length )
		fhull[nhull.length] = Float.NaN;

	    // copy hull values
	    for ( int j=0; j<nhull.length; j++ )
		fhull[j] = (float)nhull[j];
	    aitem.set(VisualItem.POLYGON, fhull);
	    aitem.setValidated(false); // force invalidation
	}
    }

    private static void addPoint(double[] pts, int idx, 
	    VisualItem item, int growth)
    {
	Rectangle2D b = item.getBounds();
	double minX = (b.getMinX())-growth, minY = (b.getMinY())-growth;
	double maxX = (b.getMaxX())+growth, maxY = (b.getMaxY())+growth;
	pts[idx]   = minX; pts[idx+1] = minY;
	pts[idx+2] = minX; pts[idx+3] = maxY;
	pts[idx+4] = maxX; pts[idx+5] = minY;
	pts[idx+6] = maxX; pts[idx+7] = maxY;
    }

} // end of class AggregateLayout


/**
 * Interactive drag control that is "aggregate-aware"
 */
class AggregateDragControl extends ControlAdapter {

    private VisualItem activeItem;
    protected Point2D down = new Point2D.Double();
    protected Point2D temp = new Point2D.Double();
    protected boolean dragged;

    /**
     * Creates a new drag control that issues repaint requests as an item
     * is dragged.
     */
    public AggregateDragControl() {
    }

    /**
     * @see prefuse.controls.Control#itemEntered(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemEntered(VisualItem item, MouseEvent e) {
	Display d = (Display)e.getSource();
	d.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	activeItem = item;
	if ( !(item instanceof AggregateItem) )
	    setFixed(item, true);
    }

    /**
     * @see prefuse.controls.Control#itemExited(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemExited(VisualItem item, MouseEvent e) {
	if ( activeItem == item ) {
	    activeItem = null;
	    setFixed(item, false);
	}
	Display d = (Display)e.getSource();
	d.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * @see prefuse.controls.Control#itemPressed(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemPressed(VisualItem item, MouseEvent e) {
	if (!SwingUtilities.isLeftMouseButton(e)) return;
	dragged = false;
	Display d = (Display)e.getComponent();
	d.getAbsoluteCoordinate(e.getPoint(), down);
	if ( item instanceof AggregateItem)
	    setFixed(item, true);
    }

    /**
     * @see prefuse.controls.Control#itemReleased(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemReleased(VisualItem item, MouseEvent e) {
	if (!SwingUtilities.isLeftMouseButton(e)) return;
	if ( dragged ) {
	    activeItem = null;
	    setFixed(item, false);
	    dragged = false;
	}            
    }

    /**
     * @see prefuse.controls.Control#itemDragged(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemDragged(VisualItem item, MouseEvent e) {
	if (!SwingUtilities.isLeftMouseButton(e)) return;
	dragged = true;
	Display d = (Display)e.getComponent();
	d.getAbsoluteCoordinate(e.getPoint(), temp);
	double dx = temp.getX()-down.getX();
	double dy = temp.getY()-down.getY();

	move(item, dx, dy);

	down.setLocation(temp);
    }

    protected static void setFixed(VisualItem item, boolean fixed) {
	if ( item instanceof AggregateItem) {
	    Iterator items = ((AggregateItem)item).items();
	    while ( items.hasNext() ) {
		setFixed((VisualItem)items.next(), fixed);
	    }
	} else {
	    item.setFixed(fixed);
	}
    }

    protected static void move(VisualItem item, double dx, double dy) {
	if ( item instanceof AggregateItem) {
	    Iterator items = ((AggregateItem)item).items();
	    while ( items.hasNext() ) {
		move((VisualItem)items.next(), dx, dy);
	    }
	} else {
	    double x = item.getX();
	    double y = item.getY();
	    item.setStartX(x);  item.setStartY(y);
	    item.setX(x+dx);    item.setY(y+dy);
	    item.setEndX(x+dx); item.setEndY(y+dy);
	}
    }



} // end of class AggregateDragControl

/**
 * This predicate evaluates the number of visible edges connected with a node  
 * 
 * @author goose
 *
 */

 class VisibleDegreePredicate extends AbstractPredicate {

	private Visualization vis;
	private String nodeGroupName;
	private int minDegree;
	
	public VisibleDegreePredicate(Visualization vis, String nodeGroupName) {
	    this(vis,nodeGroupName,1);
	}

	public VisibleDegreePredicate(Visualization vis, String nodeGroupName,int minDegree) {
	    this.vis = vis;
	    this.nodeGroupName = nodeGroupName;
	    this.minDegree = minDegree;	
	}
	
	public int getMinDegree() {
	    return minDegree;
	}
	public void setMinDegree(int n) {
	    minDegree=n;
	}

	public boolean getBoolean(Tuple t) {
	    if (t instanceof Edge) {
		return false;
	    }

	    NodeItem vi = (NodeItem) vis.getVisualItem(nodeGroupName, t);

	    Iterator iter = vi.edges();

	    boolean result = false;
	    int n=0;
	    while (!result && iter.hasNext()) {
		EdgeItem aEdgeItem = (EdgeItem) iter.next();
		if (aEdgeItem.isVisible()) {
		    n++;
		}
		if (n>= minDegree) {
		    result = true;
		}
	    }
	    return result;
	}
}