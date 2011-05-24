package sample;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.GroupAction;
import prefuse.action.ItemAction;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.animate.PolarLocationAnimator;
import prefuse.action.animate.QualityControlAnimator;
import prefuse.action.animate.VisibilityAnimator;
import prefuse.action.assignment.*;
import prefuse.action.filter.GraphDistanceFilter;
import prefuse.action.layout.CollapsedSubtreeLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.activity.SlowInSlowOutPacer;
import prefuse.controls.*;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.io.GraphMLReader;
import prefuse.data.query.SearchQueryBinding;
import prefuse.data.search.PrefixSearchTupleSet;
import prefuse.data.search.SearchTupleSet;
import prefuse.data.tuple.TupleSet;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.ui.JFastLabel;
import prefuse.util.ui.JSearchPanel;
import prefuse.util.ui.UILib;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Slight modification of the original RadialGraphView from the prefuse examples
 * with control over nodes and edges.
 * It does not have any extravagent new features but a unique combination of
 * most of those on the PAP forum. Some features are adopted from the other 
 * codes posted on the forum and just that collective effect looks really good.
 * 
 * Some errors are left in the hope that someone could better it without eliminating some
 * of the features. It is easy to omit some of the features to make it error free.
 * 
 * Thanks to all those excellent posts and prefuse offcourse. 
 * 
 * @version 1.0
 * @author gigo
 */
public class RadialGraphViewExtendedFeatures extends Display {

   
	/**
	 * This is the latest version :) 
	 */
	private static final long serialVersionUID = 1L;

	public static final String DATA_FILE = "datasets/socialnet_tree.xml";
    private static final String[] neighborGroups = { "sourceNode", "targetNode", "bothNode" };
    private static final String tree = "tree";
    private static final String treeNodes = "tree.nodes";
    private static final String treeEdges = "tree.edges";
    private static final String linear = "linear";
    private LabelRenderer m_nodeRenderer;
    private EdgeRenderer m_edgeRenderer;
    private String m_label = "label";
    private static final String hover = "hover";
    
    // ------------------------------------------------------------------------
    //here's the main function for the class...
    public static void main(String argv[]) {
        String infile = DATA_FILE;
        String label = "name";
        
        if ( argv.length > 1 ) {
            infile = argv[0];
            label = argv[1];
        }
        
        UILib.setPlatformLookAndFeel();
        
        JFrame frame = new JFrame("R A D I A L - G R A P H | T H E - B O L L - C O N C E P T");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(demo(infile, label));
       
        frame.pack();
        frame.setVisible(true);
    }
    
    public static JPanel demo() {
        return demo(DATA_FILE, "name");
    }
//  basic generation of Graph : STEP 1 : According to the manual of prefuse :)
    public static JPanel demo(String infile, final String label) {
        Graph g = null;
        try {
            g = new GraphMLReader().readGraph(infile);//contents of the infile (*.xml) are loaded in the graph.
            System.out.println("is directed returns "+ g.isDirected());
           
        } catch ( Exception e ) {
        	
        	System.err.println("Error loading graph. Exiting...");
            e.printStackTrace();
            System.exit(1);
        }
        return demo(g, label);
    }
    
    public static JPanel demo(Graph g, final String label) {
        // create a new radial tree view
        final  RadialGraphViewExtendedFeatures gview = new  RadialGraphViewExtendedFeatures(g, label);
        Visualization vis = gview.getVisualization();
       
        // create a search panel for the tree map
        SearchQueryBinding sq = new SearchQueryBinding(
             (Table)vis.getGroup(treeNodes), label,
             (SearchTupleSet)vis.getGroup(Visualization.SEARCH_ITEMS));
        JSearchPanel search = sq.createSearchPanel();
        search.setShowResultCount(true);
        search.setBackground(Color.WHITE);
        search.setBorder(BorderFactory.createEmptyBorder(5,5,4,0));
        search.setFont(FontLib.getFont("Calibri", Font.PLAIN, 13));
       
        
        final JFastLabel title = new JFastLabel("                 ");
        title.setBackground(Color.WHITE);
        title.setPreferredSize(new Dimension(350, 20));
        title.setVerticalAlignment(SwingConstants.BOTTOM);
        title.setBorder(BorderFactory.createEmptyBorder(3,0,0,0));
        title.setFont(FontLib.getFont("Calibri", Font.PLAIN, 16));
        
        gview.addControlListener(new ControlAdapter() {
            public void itemEntered(VisualItem item, MouseEvent e) {
                if ( item.canGetString(label) )
                    title.setText(item.getString(label));
            }
            public void itemExited(VisualItem item, MouseEvent e) {
                title.setText(null);
            }
        });
        
    
        Box box = new Box(BoxLayout.X_AXIS);
        
        
        box.setBackground(Color.WHITE);
        box.add(Box.createHorizontalStrut(10));
        box.add(title);
        box.add(Box.createHorizontalGlue());
        box.add(search);
        box.add(Box.createHorizontalStrut(3));
               
    
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(gview, BorderLayout.CENTER);
       
        panel.add(box, BorderLayout.SOUTH);
        panel.setBackground(Color.WHITE);
        
        Color BACKGROUND = Color.DARK_GRAY;
        Color FOREGROUND = Color.WHITE;
        UILib.setColor(panel, BACKGROUND, FOREGROUND);
   //     panel.add(totalGUI, BorderLayout.WEST);
        return panel;
    }
    
//#################################################################################################################################
    public  RadialGraphViewExtendedFeatures(Graph g, String label) {
        super(new Visualization());
        
        m_label = label;
        // -- set up visualization --
        m_vis.add(tree, g);
        m_vis.setInteractive(treeEdges, null, true);
        // renderer for node
        m_nodeRenderer = new LabelRenderer(m_label);
        //m_nodeRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
        m_nodeRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_DRAW_AND_FILL);
        m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
        m_nodeRenderer.setRoundedCorner(10,10);
       
        // renderer for edges
        m_edgeRenderer = new EdgeRenderer();
        // m_edgeRenderer.setArrowHeadSize(5,10);
        m_edgeRenderer.setEdgeType(Constants.EDGE_TYPE_CURVE);
        m_edgeRenderer.setArrowType(Constants.EDGE_ARROW_FORWARD);
             
//        DefaultRendererFactory rf = new DefaultRendererFactory(m_nodeRenderer);
//        rf.add(new InGroupPredicate(treeEdges), m_edgeRenderer);
//        m_vis.setRendererFactory(rf);
        
        DefaultRendererFactory rf = new DefaultRendererFactory();
        rf.setDefaultRenderer(m_nodeRenderer);
        rf.setDefaultEdgeRenderer(m_edgeRenderer);
        m_vis.setRendererFactory(rf);
        // The filter for the hover group

    	GraphDistanceFilter hoverFilter = new GraphDistanceFilter(tree,hover,Integer.MAX_VALUE);
    	hoverFilter.setEnabled(false);

    	// The tuple set listener for the hover group
    	final GroupDistanceListener hoverListener = new GroupDistanceListener(tree,m_vis,hoverFilter,"onlydraw");

    	m_vis.addFocusGroup(hover);
    	TupleSet hoverGroup = m_vis.getFocusGroup(hover);
    	hoverGroup.addTupleSetListener(hoverListener);
               

// -- set up processing actions --
// colors
//      int[] palette_edges = new int[] {
// 	    ColorLib.rgb(255,180,180)
// 	};
//int[] palette_nodes = new int[] {
// 	    ColorLib.rgb(103,194,255), ColorLib.rgb(103,154,255)
// 	};
        
        
        
        
        
        
        int[] palette_edges = new int[] {
         	    ColorLib.rgb(255, 180, 180), ColorLib.rgb(255, 120, 140), ColorLib.rgb(255, 51, 102)
         	};
        
        int[] palette_nodes = new int[] {
         	    ColorLib.gray(150, 255), ColorLib.blue(255)
         	};
        ItemAction nodeColor = new NodeColorAction(treeNodes,palette_nodes);
        ItemAction textColor = new TextColorAction(treeNodes);
        ItemAction nodeShape = new NodeShapeAction(treeEdges);
       // final DataSizeAction nodeShape = new DataSizeAction(treeNodes,"gender");
       // ItemAction nodeSize = new NodeSizeAction(treeNodes);
      
        ItemAction edgeColor = new EdgeColorAction(treeEdges,palette_edges);
        ItemAction arrowColor = new ArrowColorAction(treeEdges,palette_edges);
        ItemAction edgeSize = new EdgeSizeAction(treeEdges);

        
        FontAction fonts = new FontAction(treeNodes, FontLib.getFont("Calibri", 13));
        fonts.add("ingroup('_focus_')", FontLib.getFont("Calibri", 13));
        
        //edgecolor
        ActionList ecolor = new ActionList();
        ecolor.add(arrowColor);
        ecolor.add(edgeColor);        
        ecolor.add(edgeSize);
        m_vis.putAction("ecolor", ecolor);
        
        //nodeShape
        ActionList nodeshape = new ActionList();
        nodeshape.add(nodeShape);
        m_vis.putAction("nodeshape",nodeShape);
        // recolor
        ActionList recolor = new ActionList();
        recolor.add(nodeColor);
        recolor.add(nodeShape);
        recolor.add(textColor);
        recolor.add(ecolor);
        m_vis.putAction("recolor", recolor);
       
        // repaint
        ActionList repaint = new ActionList();
        repaint.add(recolor);
        repaint.add(new RepaintAction());
        m_vis.putAction("repaint", repaint);
        
        // animate paint change
        ActionList animatePaint = new ActionList(400);
        animatePaint.add(new ColorAnimator(treeNodes));
        animatePaint.add(new RepaintAction());
        m_vis.putAction("animatePaint", animatePaint);
        
        // create the tree layout action
        RadialTreeLayout treeLayout = new RadialTreeLayout(tree);
        
       
        //treeLayout.setAngularBounds(-Math.PI/2, Math.PI);
        m_vis.putAction("treeLayout", treeLayout);
      
        CollapsedSubtreeLayout subLayout = new CollapsedSubtreeLayout(tree);
        m_vis.putAction("subLayout", subLayout);
        
        // create the filtering and layout
        ActionList filter = new ActionList();
       
        filter.add(new TreeRootAction(tree));
        filter.add(hoverFilter);
        filter.add(fonts);
        filter.add(treeLayout);
        filter.add(subLayout);
        filter.add(nodeShape);
        filter.add(nodeColor);
        filter.add(textColor);
       
        filter.add(arrowColor);
        filter.add(edgeColor);
        filter.add(edgeSize);
     
       
        m_vis.putAction("filter", filter);
        
        // animated transition
        ActionList animate = new ActionList(1250);
        animate.setPacingFunction(new SlowInSlowOutPacer());
        animate.add(new QualityControlAnimator());
        animate.add(new VisibilityAnimator(tree));
        animate.add(new PolarLocationAnimator(treeNodes, linear));
        animate.add(new ColorAnimator(treeNodes));
        animate.add(new RepaintAction());
        m_vis.putAction("animate", animate);
        m_vis.alwaysRunAfter("filter", "animate");
        
        // ------------------------------------------------
        
        // initialize the display
        setSize(600,600);
        setItemSorter(new TreeDepthItemSorter());
        addControlListener(new DragControl());
        addControlListener(new ZoomToFitControl());
        addControlListener(new ZoomControl());
        addControlListener(new PanControl());
        addControlListener(new FocusControl(1, "filter"));
        addControlListener(new HoverActionControl("repaint"));
        addControlListener(new NeighborHighlightControl(m_vis,neighborGroups));
        addControlListener(new HoverControl(hover));
        
        // ------------------------------------------------
        
        // filter graph and perform layout
        m_vis.run("filter");
        
        // maintain a set of items that should be interpolated linearly
        // this isn't absolutely necessary, but makes the animations nicer
        // the PolarLocationAnimator should read this set and act accordingly
//        m_vis.addFocusGroup(linear, new DefaultTupleSet());
//        m_vis.getGroup(Visualization.FOCUS_ITEMS).addTupleSetListener(
//            new TupleSetListener() {
//                public void tupleSetChanged(TupleSet t, Tuple[] add, Tuple[] rem) {
//                    TupleSet linearInterp = m_vis.getGroup(linear);
//                    if ( add.length < 1 ) return; linearInterp.clear();
//                    for ( Node n = (Node)add[0]; n!=null; n=n.getParent() )
//                        linearInterp.addTuple(n);
//                }
//            }
//        );
        
        SearchTupleSet search = new PrefixSearchTupleSet();
        m_vis.addFocusGroup(Visualization.SEARCH_ITEMS, search);
        search.addTupleSetListener(new TupleSetListener() {
            public void tupleSetChanged(TupleSet t, Tuple[] add, Tuple[] rem) {
                m_vis.cancel("animatePaint");
                m_vis.run("recolor");
                m_vis.run("animatePaint");
                
            }
        });
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Switch the root of the tree by requesting a new spanning tree
     * at the desired root
     */
    public static class TreeRootAction extends GroupAction {
        public TreeRootAction(String graphGroup) {
            super(graphGroup);
        }
        public void run(double frac) {
            TupleSet focus = m_vis.getGroup(Visualization.FOCUS_ITEMS);
            if ( focus==null || focus.getTupleCount() == 0 ) return;
            
            Graph g = (Graph)m_vis.getGroup(m_group);
            Node f = null;
            try{
		            Iterator tuples = focus.tuples();
			            while (tuples.hasNext() && !g.containsTuple(f=(Node)tuples.next()))
			            {
			                f = null;
			            }
		            if ( f == null ) return;
		            g.getSpanningTree(f);
		       }
		    catch(Exception e)
		       {
		           	e.printStackTrace();
		       }
        }
    }
    
    /**
     * Set node fill colors
     */
    public static class NodeColorAction extends DataColorAction {
        public NodeColorAction(String group,int [] palette) {
        	 
        	super(group, "gender", Constants.NOMINAL, VisualItem.FILLCOLOR, palette);
        	add("_hover", ColorLib.rgb(0, 128, 128));
            add("ingroup('_search_')", ColorLib.gray(0, 255));
            add("ingroup('_focus_')", ColorLib.rgb(0, 128, 128));
           
            
            add(new InGroupPredicate(neighborGroups[0]), ColorLib.rgb(135, 206, 250));
        	add(new InGroupPredicate(neighborGroups[1]), ColorLib.rgb(143, 210, 143));
            add(new InGroupPredicate(neighborGroups[2]), ColorLib.rgb(216, 191, 216));
        }
                
    } // end of inner class NodeColorAction
    
    /**
     * Set node text colors
     */
    public static class TextColorAction extends ColorAction {
        public TextColorAction(String group) {
            super(group, VisualItem.TEXTCOLOR, ColorLib.rgb(255, 255, 255));
            add("_hover", ColorLib.rgb(255, 255, 255));
            add("ingroup('_search_')", ColorLib.rgb(255, 255, 255));
            add("ingroup('_focus_')", ColorLib.rgb(255, 255, 255));
            
        	add(new InGroupPredicate(neighborGroups[0]), ColorLib.rgb(0, 0, 100));
        	add(new InGroupPredicate(neighborGroups[1]), ColorLib.rgb(0, 100, 0));
            add(new InGroupPredicate(neighborGroups[2]), ColorLib.rgb(160, 32, 240));
        }
    } // end of inner class TextColorAction
    
    /**
     * Set edge  colors
     */
    public static class EdgeColorAction extends DataColorAction {
        public EdgeColorAction(String group,int [] palette) {
        	super(group,"weight", Constants.EDGE_ARROW_FORWARD, VisualItem.STROKECOLOR, palette);
        	add("_hover", ColorLib.rgb(255, 255, 255));
       }
    } // end of inner EdgeColorAction
    public static class ArrowColorAction extends DataColorAction {
        public ArrowColorAction(String group,int [] palette) {
        	super(group,"weight", Constants.EDGE_ARROW_FORWARD, VisualItem.FILLCOLOR, palette);
        	add("_hover", ColorLib.rgb(255, 255, 255));
            	          
       }
    } // end of inner EdgeColorAction
    /**
     * Set edge size
     */
    public static class EdgeSizeAction extends DataSizeAction {
        public EdgeSizeAction(String group) {
        	super(group,"weight", Constants.NUMERICAL);
        	//add("_hover", 1);
     
        }
    } // end of inner EdgeColorAction

//------------------------------------------------------------------------------------------------------------------------------------
    
    /**
     * Set node Shape
     */
    public static class NodeShapeAction extends ShapeAction {
        public NodeShapeAction(String group) {
        	super(group);
           
            add("ingroup('_search_')", Constants.SHAPE_DIAMOND);
            add("ingroup('_focus_')", Constants.SHAPE_HEXAGON);
            
        	add(new InGroupPredicate(neighborGroups[0]), Constants.SHAPE_TRIANGLE_DOWN);
        	add(new InGroupPredicate(neighborGroups[1]), Constants.SHAPE_TRIANGLE_DOWN);
            add(new InGroupPredicate(neighborGroups[2]), Constants.SHAPE_TRIANGLE_DOWN);
        }
    } // end of inner class TextColorAction
    

    /**
     * Set node Size
     */
    public static class NodeSizeAction extends DataSizeAction {
        public NodeSizeAction(String group) {
            super(group,"gender",2);
           
//          add("ingroup('_search_')", ColorLib.rgb(255,255,255));
//          add("ingroup('_focus_')", ColorLib.rgb(255,255,255));
            
        	add(new InGroupPredicate(neighborGroups[0]),10);
        	add(new InGroupPredicate(neighborGroups[1]),20);
         // add(new InGroupPredicate(neighborGroups[2]), Constants.SHAPE_TRIANGLE_DOWN);
        }
    } // end of inner class TextColorAction
    
    
 //************************************************************************************************************************************   
    // Class NeighborHighlightControl
    
    public class NeighborHighlightControl extends ControlAdapter {
    	
    	private Visualization visu;
    	
    	String sourceGroupName, targetGroupName, bothGroupName;
    	
    	TupleSet sourceTupleSet, targetTupleSet, bothTupleSet;
    	
    	public NeighborHighlightControl(Visualization vis, String[] groupNames) {
    	    visu = vis;
    	    
    	    sourceGroupName = groupNames[0];
    	    targetGroupName = groupNames[1];
    	    bothGroupName   = groupNames[2];
    	    
    	    try {
    		visu.addFocusGroup(sourceGroupName);
    		visu.addFocusGroup(targetGroupName);
    		visu.addFocusGroup(bothGroupName);
    	    } catch (Exception e) {
    		System.out.println("Problems over problems while adding foucs groups to visualization " + e.getMessage());
    	    }
    	    
    	    sourceTupleSet = visu.getFocusGroup(sourceGroupName);
    	    targetTupleSet = visu.getFocusGroup(targetGroupName);
    	    bothTupleSet = visu.getFocusGroup(bothGroupName);
    	}
    	
    	public void itemEntered(VisualItem item, MouseEvent e) {
    	    if (item instanceof NodeItem)
    		setNeighbourHighlight((NodeItem) item);
    	}
    	
    	public void itemExited(VisualItem item, MouseEvent e) {
    	    if (item instanceof NodeItem) {
    		sourceTupleSet.clear();
    		targetTupleSet.clear();
    		bothTupleSet.clear();
    	    }
    	}
    	
    	protected void setNeighbourHighlight(NodeItem centerNode) {
    	    
    	    HashSet source = new HashSet();
    	    HashSet target = new HashSet();
    	    HashSet both = new HashSet();
    	    
    	    Iterator iterInEdges = centerNode.inEdges();
    	    while (iterInEdges.hasNext()) {
    		EdgeItem edge = (EdgeItem) iterInEdges.next();
    		NodeItem sourceNode = edge.getSourceItem();
    		
    		
    		
    		source.add(sourceNode);
    	    }
    	    
    	    Iterator iterOutEdges = centerNode.outEdges();
    	    while (iterOutEdges.hasNext()) {
    		EdgeItem edge = (EdgeItem) iterOutEdges.next();
    		NodeItem targetNode = edge.getTargetItem();
    		
    		if (source.contains(targetNode)) {
    		    both.add(targetNode);
    		} else {
    		    target.add(targetNode);
    		}
    	    }
    	    
    	    source.removeAll(both);
    	    
    	    Iterator iterSource = source.iterator();
    	    while (iterSource.hasNext())
    		sourceTupleSet.addTuple((NodeItem)iterSource.next());
    	    
    	    Iterator iterTarget = target.iterator();
    	    while (iterTarget.hasNext())
    		targetTupleSet.addTuple((NodeItem)iterTarget.next());
    	    
    	    Iterator iterBoth = both.iterator();
    	    while (iterBoth.hasNext())
    		bothTupleSet.addTuple((NodeItem)iterBoth.next());
    	}
    	
     } // end of class  NeighborHighlightControlForDirectedGraphs
    
    public class GroupDistanceListener implements TupleSetListener {

    	String graph;
    	Visualization vis;
    	GraphDistanceFilter filter;
    	String drawAction;

    	ArrayList previousVisibleItems;
    	int distance;

    	boolean lastTimeFiltered = false;

    	public GroupDistanceListener(String graphName,Visualization vis,GraphDistanceFilter filter,String drawAction) {
    	    this(graphName,vis,filter,drawAction,1);
    	}

    	public GroupDistanceListener(String graph,Visualization vis,GraphDistanceFilter filter,String drawAction,int distance) {
    	    this.graph = graph;
    	    this.vis = vis;
    	    this.filter = filter;
    	    this.drawAction = drawAction;
    	    this.distance = distance;
    	    previousVisibleItems = new ArrayList();
    	}

    	public void setDistance(int distance) {
    	    this.distance = distance;
    	    filter.setDistance(distance);
    	}

    	public void tupleSetChanged(TupleSet ts, Tuple[] add, Tuple[] rem)
    	{
    	    if ( ts.getTupleCount() == 0 ) {
    		if (previousVisibleItems != null) {
    		    Iterator iter = previousVisibleItems.iterator(); // reconstructimg the pre filtered state
    		    while (iter.hasNext()) {
    			VisualItem aItem = (VisualItem) iter.next();
    			aItem.setVisible(true);
    		    }
    		}
    		lastTimeFiltered = false;
    		filter.setEnabled(false);

    	    } else {
    		if (!lastTimeFiltered) { // remembering the last unfiltered set of visible items
    		    previousVisibleItems.clear();
    		    Iterator iter  = vis.visibleItems(graph);
    		    while (iter.hasNext()) {
    			VisualItem aItem = (VisualItem) iter.next();
    			previousVisibleItems.add(aItem);
    		    }
    		}
    		lastTimeFiltered = true;
    		filter.setEnabled(true);
    		filter.setDistance(distance);

    	    }
    	    vis.run(drawAction);
    	}
        }  // end of class GroupDistanceListener

    /**
     * A simple control maintaining a focus group which contains
     * only the node under the mouse cursor if any.
     * 
     * @author martin dudek
     *
     */

    public class HoverControl extends ControlAdapter {
	String hoverGroupName;

	public HoverControl(String hoverGroupName) {
	    this.hoverGroupName = hoverGroupName;
	}
	public void itemEntered(VisualItem item, MouseEvent e) {

	    if (item instanceof NodeItem) {
		Visualization vis = item.getVisualization();
		vis.getGroup(this.hoverGroupName).setTuple(item);
		
	    }
	}

	public void itemExited(VisualItem item, MouseEvent e) {

	    if (item instanceof NodeItem) {
		Visualization vis = item.getVisualization();
		vis.getGroup(this.hoverGroupName).removeTuple(item);
		
	    }
	}
    }
   
    private static JPanel createSquareJPanel(Color color, int size) {
        JPanel tempPanel = new JPanel();
        tempPanel.setBackground(color);
        tempPanel.setMinimumSize(new Dimension(size, size));
        tempPanel.setMaximumSize(new Dimension(size, size));
        tempPanel.setPreferredSize(new Dimension(size, size));
        return tempPanel;
    }
    
    
} // end of class  prefuse.demos.RadialGraphViewExtendedFeatures
