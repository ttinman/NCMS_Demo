package demo;

import com.viettel.gatepro.sgw.client.api.gui.GateProGUI;
import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.*;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.animate.LocationAnimator;
import prefuse.action.animate.QualityControlAnimator;
import prefuse.action.animate.VisibilityAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.FontAction;
import prefuse.action.filter.FisheyeTreeFilter;
import prefuse.action.layout.CollapsedSubtreeLayout;
import prefuse.action.layout.graph.NodeLinkTreeLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.activity.SlowInSlowOutPacer;
import prefuse.controls.*;
import prefuse.data.*;
import prefuse.data.event.TupleSetListener;
import prefuse.data.io.TreeMLReader;
import prefuse.data.search.PrefixSearchTupleSet;
import prefuse.data.tuple.TupleSet;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.GraphicsLib;
import prefuse.util.UpdateListener;
import prefuse.util.display.DisplayLib;
import prefuse.util.ui.JFastLabel;
import prefuse.util.ui.JSearchPanel;
import prefuse.util.ui.UILib;
import prefuse.visual.AggregateItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.BreakIterator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;


/**
 * Demonstration of a node-link tree viewer
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @version 1.0
 */
public class NCMS_Parameter extends Display {

    public static final String TREE_CHI = "datasets/chi-ontology.xml";

    private static final String tree = "tree";
    private static final String treeNodes = "tree.nodes";
    private static final String treeEdges = "tree.edges";

    private LabelRenderer m_nodeRenderer;
    private EdgeRenderer m_edgeRenderer;

    private String m_label = "label";
    private int m_orientation = Constants.ORIENT_LEFT_RIGHT;
    private FlexibleTreeLayout treeLayout;
    private int action = 0;
    private GateProGUI newGui;
    private HashMap<String,Vector<Vector>> sampleData = new HashMap<String, Vector<Vector>>();
    private JPanel tablePanel;
    private CommonTableModel model;

    public NCMS_Parameter(Tree t, String label) {
        super(new Visualization());
        m_label = label;

        m_vis.add(tree, t);

        m_nodeRenderer = new LabelRenderer(m_label, "image");
        m_nodeRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
        m_nodeRenderer.setHorizontalAlignment(Constants.LEFT);
//        m_nodeRenderer.setRoundedCorner(8,8);
        m_nodeRenderer.setHorizontalPadding(0);
        m_nodeRenderer.setVerticalPadding(0);
        m_nodeRenderer.setImagePosition(Constants.TOP);
        m_nodeRenderer.setMaxImageDimensions(24, 24);
        m_edgeRenderer = new EdgeRenderer(Constants.EDGE_TYPE_CURVE);

        DefaultRendererFactory rf = new DefaultRendererFactory(m_nodeRenderer);
        rf.add(new InGroupPredicate(treeEdges), m_edgeRenderer);
        m_vis.setRendererFactory(rf);

        // colors
        ItemAction nodeColor = new NodeColorAction(treeNodes);
        ItemAction textColor = new ColorAction(treeNodes,
                VisualItem.TEXTCOLOR, ColorLib.rgb(0, 0, 0));
        m_vis.putAction("textColor", textColor);

        ItemAction edgeColor = new ColorAction(treeEdges,
                VisualItem.STROKECOLOR, ColorLib.rgb(200, 200, 200));

        // quick repaint
        ActionList repaint = new ActionList();
        repaint.add(nodeColor);
        repaint.add(new RepaintAction());
        m_vis.putAction("repaint", repaint);

        // full paint
        ActionList fullPaint = new ActionList();
        fullPaint.add(nodeColor);
        m_vis.putAction("fullPaint", fullPaint);

        // animate paint change
        ActionList animatePaint = new ActionList(400);
        animatePaint.add(new ColorAnimator(treeNodes));
        animatePaint.add(new RepaintAction());
        m_vis.putAction("animatePaint", animatePaint);

        // create the tree layout action
//        treeLayout = new NodeLinkTreeLayout(tree,
//                m_orientation, 100, 0, 80);
        treeLayout = new FlexibleTreeLayout(tree);
        treeLayout.setLayoutAnchor(new Point2D.Double(25, 300));
        m_vis.putAction("treeLayout", treeLayout);

        CollapsedSubtreeLayout subLayout =
                new CollapsedSubtreeLayout(tree, m_orientation);
        m_vis.putAction("subLayout", subLayout);

        AutoPanAction autoPan = new AutoPanAction();

        // create the filtering and layout
        ActionList filter = new ActionList();
        filter.add(new FisheyeTreeFilter(tree, 2));
        filter.add(new FontAction(treeNodes, FontLib.getFont("Tahoma", 16)));
        filter.add(treeLayout);
        filter.add(subLayout);
        filter.add(textColor);
        filter.add(nodeColor);
        filter.add(edgeColor);
        m_vis.putAction("filter", filter);

        // animated transition
        ActionList animate = new ActionList(1000);
        animate.setPacingFunction(new SlowInSlowOutPacer());
        animate.add(autoPan);
        animate.add(new QualityControlAnimator());
        animate.add(new VisibilityAnimator(tree));
        animate.add(new LocationAnimator(treeNodes));
        animate.add(new ColorAnimator(treeNodes));
        animate.add(new RepaintAction());
        m_vis.putAction("animate", animate);
        m_vis.alwaysRunAfter("filter", "animate");

        // create animator for orientation changes
        ActionList orient = new ActionList(2000);
        orient.setPacingFunction(new SlowInSlowOutPacer());
        orient.add(autoPan);
        orient.add(new QualityControlAnimator());
        orient.add(new LocationAnimator(treeNodes));
        orient.add(new RepaintAction());
        m_vis.putAction("orient", orient);

        // ------------------------------------------------

        // initialize the display
        setSize(700, 600);
        setItemSorter(new TreeDepthItemSorter());
//        addControlListener(new ZoomToFitControl());
        addControlListener(new ZoomControl());
        addControlListener(new WheelZoomControl());
        addControlListener(new PanControl());
        addControlListener(new FocusControl(1, "filter"));

        registerKeyboardAction(
                new OrientAction(Constants.ORIENT_LEFT_RIGHT),
                "left-to-right", KeyStroke.getKeyStroke("ctrl 1"), WHEN_FOCUSED);
        registerKeyboardAction(
                new OrientAction(Constants.ORIENT_TOP_BOTTOM),
                "top-to-bottom", KeyStroke.getKeyStroke("ctrl 2"), WHEN_FOCUSED);
        registerKeyboardAction(
                new OrientAction(Constants.ORIENT_RIGHT_LEFT),
                "right-to-left", KeyStroke.getKeyStroke("ctrl 3"), WHEN_FOCUSED);
        registerKeyboardAction(
                new OrientAction(Constants.ORIENT_BOTTOM_TOP),
                "bottom-to-top", KeyStroke.getKeyStroke("ctrl 4"), WHEN_FOCUSED);

        // ------------------------------------------------

        // filter graph and perform layout
        setOrientation(m_orientation);

        Visualization vis = this.getVisualization();
        Rectangle2D bounds = vis.getBounds(tree);
        GraphicsLib.expand(bounds, 320+ (int) (1 / this.getScale()));
        DisplayLib.fitViewToBounds(this, bounds, 2000);

        m_vis.run("filter");

        TupleSet search = new PrefixSearchTupleSet();
        m_vis.addFocusGroup(Visualization.SEARCH_ITEMS, search);
        search.addTupleSetListener(new TupleSetListener() {
            public void tupleSetChanged(TupleSet t, Tuple[] add, Tuple[] rem) {
                m_vis.cancel("animatePaint");
                m_vis.run("fullPaint");
                m_vis.run("animatePaint");
            }
        });

        UpdateListener lstnr = new UpdateListener() {
            public void update(Object src) {
            }

            public void componentResized(ComponentEvent e) {
                double x = e.getComponent().getWidth();
                double y = e.getComponent().getHeight();
                double min = x < y ? x : y;
                treeLayout.setRescale(x / min, y / min);
                m_vis.run("filter");
//                homeList.clear();
//                TupleSet tur = m_vis.getVisualGroup(Visualization.ALL_ITEMS);
//                Iterator ite = tur.tuples();
//                while(ite.hasNext()){
//                    homeList.add(ite.next());
//                }
            }
        };

        addComponentListener(lstnr);

        PopupMenuController popup = new PopupMenuController(m_vis);
         addControlListener(popup);
//      makes us able to stop TextEditor by special KeyEvents (e.g. Enter)
        getTextEditor().addKeyListener(popup);

        //Init sample data
        initSampleData();
    }

    public void initSampleData(){
       String nodeTitle = "MWPD09";
       Vector parameters = new Vector();
       parameters.add("FORCE FILE CHANGE");
       parameters.add("NO");
       parameters.add("NO");
       parameters.add("DEFAULT");
       parameters.add("STORING");
       parameters.add("YES");
       Vector item = new Vector();
        item.add(parameters);
        sampleData.put(nodeTitle,item);

       nodeTitle = "MWPD08";
       parameters = new Vector();
       parameters.add("AT");
       parameters.add("1");
       parameters.add("1");
       parameters.add("DEFAULT");
       parameters.add("SUB");
       parameters.add("2");
       item = new Vector();
        item.add(parameters);
       sampleData.put(nodeTitle,item);

       nodeTitle = "BCHT71";
       parameters = new Vector();
       parameters.add("GT");
       parameters.add("100");
       parameters.add("100");
       parameters.add("DEFAULT");
       parameters.add("VOICE");
       parameters.add("101");
       item = new Vector();
        item.add(parameters);
        sampleData.put(nodeTitle,item);
    }

    // ------------------------------------------------------------------------

    public void setOrientation(int orientation) {
        NodeLinkTreeLayout rtl
                = (NodeLinkTreeLayout) m_vis.getAction("treeLayout");
        CollapsedSubtreeLayout stl
                = (CollapsedSubtreeLayout) m_vis.getAction("subLayout");
        switch (orientation) {
            case Constants.ORIENT_LEFT_RIGHT:
                m_nodeRenderer.setHorizontalAlignment(Constants.LEFT);
                m_edgeRenderer.setHorizontalAlignment1(Constants.RIGHT);
                m_edgeRenderer.setHorizontalAlignment2(Constants.LEFT);
                m_edgeRenderer.setVerticalAlignment1(Constants.CENTER);
                m_edgeRenderer.setVerticalAlignment2(Constants.CENTER);
                break;
            case Constants.ORIENT_RIGHT_LEFT:
                m_nodeRenderer.setHorizontalAlignment(Constants.RIGHT);
                m_edgeRenderer.setHorizontalAlignment1(Constants.LEFT);
                m_edgeRenderer.setHorizontalAlignment2(Constants.RIGHT);
                m_edgeRenderer.setVerticalAlignment1(Constants.CENTER);
                m_edgeRenderer.setVerticalAlignment2(Constants.CENTER);
                break;
            case Constants.ORIENT_TOP_BOTTOM:
                m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
                m_edgeRenderer.setHorizontalAlignment1(Constants.CENTER);
                m_edgeRenderer.setHorizontalAlignment2(Constants.CENTER);
                m_edgeRenderer.setVerticalAlignment1(Constants.BOTTOM);
                m_edgeRenderer.setVerticalAlignment2(Constants.TOP);
                break;
            case Constants.ORIENT_BOTTOM_TOP:
                m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
                m_edgeRenderer.setHorizontalAlignment1(Constants.CENTER);
                m_edgeRenderer.setHorizontalAlignment2(Constants.CENTER);
                m_edgeRenderer.setVerticalAlignment1(Constants.TOP);
                m_edgeRenderer.setVerticalAlignment2(Constants.BOTTOM);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unrecognized orientation value: " + orientation);
        }
        m_orientation = orientation;
        rtl.setOrientation(orientation);
        stl.setOrientation(orientation);
    }

    public int getOrientation() {
        return m_orientation;
    }

    // ------------------------------------------------------------------------

    public static void main(String argv[]) {
        String infile = TREE_CHI;
        String label = "name";
        if (argv.length > 1) {
            infile = argv[0];
            label = argv[1];
        }

        UILib.setAlloyLookAndFeel();

        JComponent treeview = demo(infile, label);

        JFrame frame = new JFrame("NCMS | Parameter Tracing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(treeview);
        frame.pack();
        frame.setVisible(true);
    }

    public static JComponent demo() {
        return demo(TREE_CHI, "name");
    }

    public static JComponent demo(String datafile, final String label) {
        Color BACKGROUND = Color.WHITE;
        Color FOREGROUND = Color.BLACK;

        Tree t = null;
        try {
            t = (Tree) new TreeMLReader().readGraph(datafile);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        // create a new treemap
        final NCMS_Parameter tview = new NCMS_Parameter(t, label);
        tview.setBackground(BACKGROUND);
        tview.setForeground(FOREGROUND);

        // create a search panel for the tree map
        JSearchPanel search = new JSearchPanel(tview.getVisualization(),
                treeNodes, Visualization.SEARCH_ITEMS, label, true, true);
        search.setShowResultCount(true);
        search.setBorder(BorderFactory.createEmptyBorder(5, 5, 4, 0));
        search.setFont(FontLib.getFont("Tahoma", Font.PLAIN, 11));
        search.setBackground(BACKGROUND);
        search.setForeground(FOREGROUND);

        final JFastLabel title = new JFastLabel("                 ");
        title.setPreferredSize(new Dimension(350, 20));
        title.setVerticalAlignment(SwingConstants.BOTTOM);
        title.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        title.setFont(FontLib.getFont("Tahoma", Font.PLAIN, 16));
        title.setBackground(BACKGROUND);
        title.setForeground(FOREGROUND);



        Box box = new Box(BoxLayout.X_AXIS);
        box.add(Box.createHorizontalStrut(10));
        box.add(title);
        box.add(Box.createHorizontalGlue());
        box.add(search);
        box.add(Box.createHorizontalStrut(3));
        box.setBackground(BACKGROUND);

         tview.setTablePanel( new JPanel(new BorderLayout()));
        Vector column = new Vector();
        column.add("Tên tham số");
        column.add("Giá trị mặc định");
        column.add("Giá trị đề nghị");
        column.add("Group");
        column.add("Subgroup");
        column.add("Giá trị thực tế");

         tview.setModel(new CommonTableModel(column));
        JTable table = new JTable(tview.getModel());
        final JScrollPane scrollPane = new JScrollPane(table);
        tview.getTablePanel().add(scrollPane, BorderLayout.CENTER);

        JPanel dataPanel = new JPanel(new BorderLayout());
        dataPanel.add(tview,BorderLayout.CENTER);
        dataPanel.add(tview.getTablePanel(),BorderLayout.SOUTH);
        tview.getTablePanel().setVisible(false);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);
        panel.setForeground(FOREGROUND);
        panel.add(dataPanel, BorderLayout.CENTER);
        panel.add(box, BorderLayout.SOUTH);

        tview.addControlListener(new ControlAdapter() {
            public void itemEntered(VisualItem item, MouseEvent e) {
                if (item.canGetString(label)){

                    title.setText(item.getString(label));
                }

            }

            public void itemExited(VisualItem item, MouseEvent e) {
                title.setText(null);

            }
        });

        return panel;
    }

    // ------------------------------------------------------------------------


    public class OrientAction extends AbstractAction {
        private int orientation;

        public OrientAction(int orientation) {
            this.orientation = orientation;
        }

        public void actionPerformed(ActionEvent evt) {
            setOrientation(orientation);
            getVisualization().cancel("orient");
            getVisualization().run("treeLayout");
            getVisualization().run("orient");
        }
    }

    public class AutoPanAction extends prefuse.action.Action {
        private Point2D m_start = new Point2D.Double();
        private Point2D m_end = new Point2D.Double();
        private Point2D m_cur = new Point2D.Double();
        private int m_bias = 150;

        public void run(double frac) {
            TupleSet ts = m_vis.getFocusGroup(Visualization.FOCUS_ITEMS);
            if (ts.getTupleCount() == 0)
                return;

            if (frac == 0.0) {
                int xbias = 0, ybias = 0;
                switch (m_orientation) {
                    case Constants.ORIENT_LEFT_RIGHT:
                        xbias = m_bias;
                        break;
                    case Constants.ORIENT_RIGHT_LEFT:
                        xbias = -m_bias;
                        break;
                    case Constants.ORIENT_TOP_BOTTOM:
                        ybias = m_bias;
                        break;
                    case Constants.ORIENT_BOTTOM_TOP:
                        ybias = -m_bias;
                        break;
                }

                VisualItem vi = (VisualItem) ts.tuples().next();
                m_cur.setLocation(getWidth() / 2, getHeight() / 2);
                getAbsoluteCoordinate(m_cur, m_start);
                m_end.setLocation(vi.getX() + xbias, vi.getY() + ybias);
            } else {
                m_cur.setLocation(m_start.getX() + frac * (m_end.getX() - m_start.getX()),
                        m_start.getY() + frac * (m_end.getY() - m_start.getY()));
                panToAbs(m_cur);
            }
        }
    }

    public static class FlexibleTreeLayout extends NodeLinkTreeLayout {
        private double scaleX = 1.0, scaleY = 1.0;

        public FlexibleTreeLayout(String group) {
            super(group);
        }

        protected void setPolarLocation(NodeItem n, NodeItem p, double r, double t) {
            setX(n, p, m_root.getX() + scaleX * r * Math.cos(t));
            setY(n, p, m_root.getY() + scaleY * r * Math.sin(t));
        }

        /**
         * specifies the recale factors
         *
         * @param x
         * @param y
         */
        public void setRescale(double x, double y) {
            scaleX = x;
            scaleY = y;
            System.out.println("Rescale " + x + " " + y);
        }
    }

    public static class NodeColorAction extends ColorAction {

        public NodeColorAction(String group) {
            super(group, VisualItem.FILLCOLOR);
        }

        public int getColor(VisualItem item) {
            if (m_vis.isInGroup(item, Visualization.SEARCH_ITEMS))
                return ColorLib.rgb(255, 190, 190);
            else if (m_vis.isInGroup(item, Visualization.FOCUS_ITEMS))
                return ColorLib.rgb(198, 229, 229);
            else if (item.getDOI() > -1)
                return ColorLib.rgb(164, 193, 193);
//            else if (!item.getString("status").equalsIgnoreCase("matched"))
//                return ColorLib.rgba(243, 117, 33, 50);
            else
                return ColorLib.rgba(255, 255, 255, 0);
        }

    } // end of inner class TreeMapColorAction

   public class PopupMenuController extends ControlAdapter implements
           ActionListener {

    	private Graph g;
		private Display d;
		private Visualization vis;
		private VisualItem clickedItem;

		private JPopupMenu nodePopupMenu;
    	private JPopupMenu popupMenu;

		private Point2D mousePosition = new Point2D.Double();
		private VisualItem nodeVisualDummy;
		public Node nodeSourceDummy;
		public Edge edgeDummy;
		private boolean creatingEdge = false;
		private boolean editing;
        private Point currentPoint;

    	public PopupMenuController(Visualization vis) {
    		this.vis = vis;
//    		this.g = (Graph)m_vis.getGroup(m_group);
    		this.d = vis.getDisplay(0);


    		//create popupMenu for nodes
    		nodePopupMenu = new JPopupMenu();
            JMenuItem viewDetail = new JMenuItem("Chi tiết", 'c');
    		JMenuItem delete = new JMenuItem("Kết nối", 'd');
    		JMenuItem editText = new JMenuItem("Backup cấu hình", 'a');
    		JMenuItem addEdge = new JMenuItem("Restore cấu hình", 'e');
    		JMenuItem addNode = new JMenuItem("Import cấu hình", 'n');

            viewDetail.setActionCommand("node_viewDetail");
    		delete.setActionCommand("node_delete");
    		editText.setActionCommand("node_editText");
    		addEdge.setActionCommand("node_addEdge");
    		addNode.setActionCommand("node_addNode");

            nodePopupMenu.add(viewDetail);
    		nodePopupMenu.addSeparator();
    		nodePopupMenu.add(delete);
    		nodePopupMenu.addSeparator();
    		nodePopupMenu.add(editText);
    		nodePopupMenu.addSeparator();
    		nodePopupMenu.add(addEdge);
    		nodePopupMenu.add(addNode);

    		delete.setMnemonic(KeyEvent.VK_D);
    		editText.setMnemonic(KeyEvent.VK_A);
    		addEdge.setMnemonic(KeyEvent.VK_E);
    		addNode.setMnemonic(KeyEvent.VK_N);

            viewDetail.addActionListener(this);
    		delete.addActionListener(this);
    		editText.addActionListener(this);
    		addEdge.addActionListener(this);
    		addNode.addActionListener(this);


    		//create popupMenu for 'background'
    		popupMenu = new JPopupMenu();
    		addNode = new JMenuItem("add Node", 'n');
    		addNode.setActionCommand("addNode");
    		popupMenu.addSeparator();
    		popupMenu.add(addNode);
    		addNode.setMnemonic(KeyEvent.VK_N);
    		addNode.addActionListener(this);

		}

    	// ---------------------------------------------
    	// --- methods for event processing
    	// ---------------------------------------------

    	@Override
    	public void itemClicked(VisualItem item, MouseEvent e) {
            currentPoint = e.getPoint();
    		if (SwingUtilities.isRightMouseButton(e)) {
    			clickedItem = item;

    			if (item instanceof NodeItem) {
    	    		nodePopupMenu.show(e.getComponent(), e.getX(), e.getY());
    			}

    		} else if (SwingUtilities.isLeftMouseButton(e)) {
    			if (item instanceof NodeItem) {	//a node was clicked
                    clickedItem = (VisualItem)item;
                    switch (action) {
                        case 1:    //	create a node

                            break;

                        case 2:    //	create an edge
                            creatingEdge = true;
                            break;

                        case 3:    //	rename node

                            break;
                        case 4:    //	rename node

                            break;
                        case 5: {


                        }

                        break;
                        default:

                            break;
                    }

    			}
    		}
    	}


		@Override
    	public void mouseClicked(MouseEvent e) {

    		if (SwingUtilities.isRightMouseButton(e)) {
    			clickedItem = null;

    			popupMenu.show(e.getComponent(), e.getX(), e.getY());
    		}

    	}

    	@Override
    	public void keyReleased(KeyEvent e) {
    		// called, when keyReleased events on displays textEditor are fired
    		if (e.getKeyCode() == KeyEvent.VK_F1) {

    		}
    	}

    	@Override
    	public void itemKeyReleased(VisualItem item, KeyEvent e) {
    		keyReleased(e);
    	}


    	/**
    	 * called on popupMenu Action
    	 */
    	public void actionPerformed(ActionEvent e) {
    		if (e.getActionCommand().startsWith("node")) {
    			if (e.getActionCommand().endsWith("delete")) {
                    if(newGui==null){
                        newGui = new GateProGUI("192.168.208.69",2257,"JulianPC","ngantrung");
                    }

                    newGui.setConnectInfo("192.168.4.95:23");
                    newGui.showMe(null);
                    action =1;
    			} else if (e.getActionCommand().endsWith("editText")) {
                     action =2;
    			} else if (e.getActionCommand().endsWith("addEdge")) {

                     action =3;
    			} else if (e.getActionCommand().endsWith("addNode")) {
                     action =4;
    			}  else if (e.getActionCommand().endsWith("viewDetail")) {
                    if (tablePanel.isVisible()) {
                        model.removeDataAll();
                        tablePanel.setVisible(false);
                    } else {
                        if (sampleData.get(clickedItem.getString("name")) == null) return;
                        for (Vector oneRow : sampleData.get(clickedItem.getString("name"))) {
                            model.addData(oneRow);
                        }
                        tablePanel.setVisible(true);
                    }
    			}
    		} else {
    			if (e.getActionCommand().equals("addNode")) {
    				int node = (int) (Math.random()*(g.getNodeCount()-1));
					Node source = g.getNode(node);	//random source

    			} else {

    			}
    		}
    	}




    	@Override
    	public void mouseMoved(MouseEvent e) {
    		//necessary, if you have no dummy and this ControlAdapter is running
    		if (nodeVisualDummy == null) return;
    		// update the coordinates of the dummy-node to the mouselocation so the tempEdge is drawn to the mouselocation too
    		d.getAbsoluteCoordinate(e.getPoint(), mousePosition);
    		nodeVisualDummy.setX(mousePosition.getX());
    		nodeVisualDummy.setY(mousePosition.getY());
    	}

    	/**
    	 * only necessary if edge-creation is used together with aggregates and
    	 * the edge should move on when mousepointer moves within an aggregate
    	 */
    	@Override
    	public void itemMoved(VisualItem item, MouseEvent e) {
    		if (item instanceof AggregateItem)
    			mouseMoved(e);
    	}


    }

    public JPanel getTablePanel() {
        return tablePanel;
    }

    public void setTablePanel(JPanel tablePanel) {
        this.tablePanel = tablePanel;
    }

    public CommonTableModel getModel() {
        return model;
    }

    public void setModel(CommonTableModel model) {
        this.model = model;
    }
} // end of class TreeMap
