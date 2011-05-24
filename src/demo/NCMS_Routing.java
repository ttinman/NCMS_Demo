package demo;

import com.viettel.gatepro.sgw.client.api.gui.GateProGUI;
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
import prefuse.action.filter.VisibilityFilter;
import prefuse.action.layout.CollapsedSubtreeLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.activity.SlowInSlowOutPacer;
import prefuse.controls.*;
import prefuse.data.*;
import prefuse.data.event.TupleSetListener;
import prefuse.data.expression.AbstractPredicate;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
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
import prefuse.util.UpdateListener;
import prefuse.util.ui.JFastLabel;
import prefuse.util.ui.JSearchPanel;
import prefuse.util.ui.UILib;
import prefuse.visual.AggregateItem;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

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
public class NCMS_Routing extends Display
{


	/**
	 * This is the latest version :)
	 */
	private static final long serialVersionUID = 1L;

	public static final String DATA_FILE = "datasets/socialnet_tree.xml";
    private static final String[] neighborGroups = { "sourceNode", "targetNode", "bothNode" };
    private static final String tree = "tree";
    private static final String treeNodes = "tree.nodes";
    private static final String treeEdges = "tree.edges";
//    private static final String graphNodesAndEdges = "graph";
    private static final String linear = "linear";
    private LabelRenderer m_nodeRenderer;
    private EdgeRenderer m_edgeRenderer;
    private String m_label = "label";
    private static final String hover = "hover";
    private static final String wrongPath = "wrong";
    private MyInternalFrame tooltipScreen;
    private int action = 0;
    private ArrayList homeList = new ArrayList();
    private GroupDistanceListener hoverListener;
    private GraphDistanceFilter hoverFilter;
    private VisualItem currentItem;
    private JSearchPanel searchPanel;
    private FontAction fonts;
    private ActionList ecolor;
    private FlexibleRadialTreeLayout treeLayout;
    private CollapsedSubtreeLayout subLayout;
    private ItemAction nodeShape;
    private ItemAction nodeColor;
    private ItemAction textColor;
    private ItemAction edgeColor;
    private ItemAction arrowColor;
    private ItemAction edgeSize;
    private VisualItem curFocus;
    private  FocusControl fc;
    private  FocusControl fc1;
    private NeighborHighlightControl nc;
    private GroupDistanceListener gl;
    private HiddenGroupPredicate myPredicate;
    private GateProGUI newGui;
//    private ArrayList homeVisibleItems = new ArrayList();

    public static void main(String argv[]) {
        String infile = DATA_FILE;
        String label = "name";

        if ( argv.length > 1 ) {
            infile = argv[0];
            label = argv[1];
        }

        UILib.setAlloyLookAndFeel();

        JFrame frame = new JFrame("NCMS - Routing Topology");
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

    public static JPanel demo1(String infile, final String label) {
        Graph g = null;
        try {
            g = new GraphMLReader().readGraph(infile);//contents of the infile (*.xml) are loaded in the graph.
            System.out.println("is directed returns "+ g.isDirected());

        } catch ( Exception e ) {

        	System.err.println("Error loading graph. Exiting...");
            e.printStackTrace();
            System.exit(1);
        }
        return createGraphPanel(g, label);
    }

    public static JPanel demo(Graph g, final String label) {

        JPanel gSiteview = createGraphPanel(g,label);
//        JPanel gNodeView = demo1("datasets/socialnet_tree_type.xml", label);
        final JPanel viewCardPanel = new JPanel(new CardLayout());
        viewCardPanel.add(gSiteview,"Site");
//        viewCardPanel.add(gNodeView,"Type");
        //Tìm kiếm định tuyến GT


        JLabel exchangeCode = new JLabel("Mã node: ");
        JTextField exchangeCodeInput = new JTextField();
        JLabel gtCode = new JLabel("Loại GT : ");
        JTextField gtCodeInput = new JTextField();
        Box boxExchangeCode = new Box(BoxLayout.X_AXIS);
        boxExchangeCode.setBackground(Color.WHITE);
        boxExchangeCode.add(Box.createHorizontalStrut(10));
        boxExchangeCode.add(exchangeCode);
        boxExchangeCode.add(Box.createHorizontalGlue());
        boxExchangeCode.add(exchangeCodeInput);
        boxExchangeCode.add(Box.createHorizontalStrut(3));

        Box boxGtCode = new Box(BoxLayout.X_AXIS);
        boxGtCode.setBackground(Color.WHITE);
        boxGtCode.add(Box.createHorizontalStrut(10));
        boxGtCode.add(gtCode);
        boxGtCode.add(Box.createHorizontalGlue());
        boxGtCode.add(gtCodeInput);
        boxGtCode.add(Box.createHorizontalStrut(3));


        final JToggleButton tableViewBtn = new JToggleButton("Table View");
        final JToggleButton topoViewBtn = new JToggleButton("Topo View");
        tableViewBtn.setEnabled(false);
        topoViewBtn.setEnabled(false);
        final JRadioButton siteView = new JRadioButton("Khu vực");
        final JRadioButton exchangeTypeView = new JRadioButton("Loại node");
        siteView.setEnabled(false);
        exchangeTypeView.setEnabled(false);
        siteView.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (siteView.isSelected()) {
                    CardLayout cl = (CardLayout) (viewCardPanel.getLayout());
                    cl.show(viewCardPanel, "Site");
                }
            }
        });
        exchangeTypeView.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (exchangeTypeView.isSelected()) {
                    CardLayout cl = (CardLayout) (viewCardPanel.getLayout());
                    cl.show(viewCardPanel, "Type");
                }
            }
        });
        ButtonGroup group = new ButtonGroup();
        group.add(siteView);
        group.add(exchangeTypeView);
        JButton searchBtn = new JButton("Tìm kiếm");
        searchBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try{
                   tableViewBtn.setEnabled(false);
                   topoViewBtn.setEnabled(false);
                   Thread.sleep(1000);
                   tableViewBtn.setEnabled(true);
                   tableViewBtn.setEnabled(true);
                   topoViewBtn.setEnabled(true);
                }catch (Exception ex){
                    ex.printStackTrace();
                }

            }
        });
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.add(boxExchangeCode,new GridBagConstraints(1,1,2,1,0.5,1.0,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(1,1,1,1),1,1));
        bottomPanel.add(boxGtCode,new GridBagConstraints(3,1,2,1,0.5,1.0,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(1,1,1,1),1,1));
        bottomPanel.add(searchBtn,new GridBagConstraints(5,1,1,1,0.1,1.0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,1,1,1),1,1));
        bottomPanel.add(tableViewBtn,new GridBagConstraints(6,1,1,1,0.1,1.0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,1,1,1),1,1));
        bottomPanel.add(topoViewBtn,new GridBagConstraints(7,1,1,1,0.1,1.0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,1,1,1),1,1));
//        bottomPanel.add(siteView,new GridBagConstraints(8,1,1,1,0.1,1.0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,1,1,1),1,1));
//        bottomPanel.add(exchangeTypeView,new GridBagConstraints(9,1,1,1,0.1,1.0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,1,1,1),1,1));
        //



        //Add Card Layout for switching between Table Layout and Topo Layout
        JPanel tablePanel = new JPanel(new BorderLayout());
        Vector column = new Vector();
        column.add("Mã Node");
        column.add("GT");
        column.add("Hướng chính");
        column.add("Hướng phụ");
        column.add("Ip");
        column.add("Port");
        Iterator ite = g.nodes();
        Vector data = new Vector();
        for(int i =0;i<g.getNodeCount();i++){
           Vector v_data = new Vector();
            Node node = g.getNode(i);
            v_data.add(node.getString(0));
            v_data.add(node.getString(1));
            v_data.add(node.getString(2));
            v_data.add(node.getString(3));
            v_data.add(node.getString(4));
            v_data.add(node.getString(5));
            data.add(v_data);
        }


        CommonTableModel model = new CommonTableModel(column,data);
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        tablePanel.add(scrollPane,BorderLayout.CENTER);
        //

        final JPanel cardPanel = new JPanel(new CardLayout());
        cardPanel.add(new JPanel(),"");
        cardPanel.add(tablePanel,"Table");
        cardPanel.add(viewCardPanel,"Topo");

        tableViewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CardLayout cl = (CardLayout)(cardPanel.getLayout());
                cl.show(cardPanel, "Table");
                siteView.setEnabled(false);
                exchangeTypeView.setEnabled(false);
            }
        });
        topoViewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CardLayout cl = (CardLayout)(cardPanel.getLayout());
                cl.show(cardPanel, "Topo");
                siteView.setEnabled(true);
                exchangeTypeView.setEnabled(true);
            }
        });
        //

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(cardPanel, BorderLayout.CENTER);

        panel.add(bottomPanel, BorderLayout.SOUTH);
        panel.setBackground(Color.WHITE);

        Color BACKGROUND = Color.WHITE;
        Color FOREGROUND = Color.BLACK;
        UILib.setColor(panel, BACKGROUND, FOREGROUND);
   //     panel.add(totalGUI, BorderLayout.WEST);
        return panel;
    }

    public static JPanel createGraphPanel(final Graph g,final String label){
         // create a new radial tree view
        final NCMS_Routing gview = new NCMS_Routing(g, label);
        final Visualization vis = gview.getVisualization();

        // create a search panel for the tree map
        SearchQueryBinding sq = new SearchQueryBinding(
             (Table)vis.getGroup(treeNodes), label,
             (SearchTupleSet)vis.getGroup(Visualization.SEARCH_ITEMS));
        final JSearchPanel search = sq.createSearchPanel();
        search.setShowResultCount(true);
        search.setBackground(Color.WHITE);
        search.setBorder(BorderFactory.createEmptyBorder(5,5,4,0));
        search.setFont(FontLib.getFont("Calibri", Font.PLAIN,13));
        gview.setSearchPanel(search);

        final JFastLabel title = new JFastLabel("                 ");
        title.setBackground(Color.RED);
        title.setPreferredSize(new Dimension(350, 20));
        title.setVerticalAlignment(SwingConstants.BOTTOM);
        title.setBorder(BorderFactory.createEmptyBorder(3,0,0,0));
        title.setFont(FontLib.getFont("Calibri", Font.PLAIN, 16));
//        final JLabel childrenLabel = new JLabel("");
        final JButton childrenLabel = new JButton();
        childrenLabel.setText("<HTML><FONT color=\"#000099\"><U> -> Site</U></FONT></HTML>");
        childrenLabel.setHorizontalAlignment(SwingConstants.LEFT);
        childrenLabel.setBorderPainted(false);
        childrenLabel.setBorder(BorderFactory.createEmptyBorder());
        childrenLabel.setOpaque(false);
        childrenLabel.setBackground(Color.WHITE);
        childrenLabel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String query = "gtCode = 'root'";
                Predicate myPredicate1 = (Predicate)ExpressionParser.parse(query);
                Iterator ite = vis.getVisualGroup(treeNodes).tuples(myPredicate1);
                while(ite.hasNext()){
                    Object obj = ite.next();
                    if(obj instanceof NodeItem){
                        TupleSet ts = vis.getFocusGroup(hover);

                        if(((NodeItem)obj).getString("gtCode").equalsIgnoreCase("root")){
                            ts.setTuple((NodeItem)obj);
                        }

                    }
                }

                gview.getMyPredicate().setFilterInfo("Ha Noi","site",true);

//                gview.getMyPredicate().setAllInfo(true);

                vis.run("filter");
            }
        });

        gview.addControlListener(new ControlAdapter() {
            public void itemEntered(VisualItem item, MouseEvent e) {
                if ( item.canGetString(label) ){
                   if(gview.getCurrentItem()==null){
                       gview.setCurrentItem(item);
                   }
                   title.setText(item.getString(label));
//                   if(item.getString("site").equalsIgnoreCase("hidden")){
//                     childrenLabel.setText("<HTML>-> <FONT color=\"#000099\"><U>"+item.getString("name")+"</U></FONT></HTML>");
//                   }else{
//                     childrenLabel.setText("<HTML>-> <FONT color=\"#000099\"><U>"+item.getString("site")+"</U></FONT></HTML>");
//                   }

                   if(item.getString("gtCode").equalsIgnoreCase("group")){
                      gview.getMyPredicate().setFilterInfo(item.getString("name"),"site",true);
                   }else{
                      gview.getMyPredicate().setSearchOneNodeInfo(item.getString("name"),item.getString("mainPath"),item.getString("slavePath"));
                   }

                }


            }
            public void itemExited(VisualItem item, MouseEvent e) {
                title.setText(null);
//                childrenLabel.setText(item.getString(label));
            }
        });


        Box box = new Box(BoxLayout.X_AXIS);

        box.setBackground(Color.WHITE);
        box.add(Box.createHorizontalStrut(10));
        box.add(title);
        box.add(Box.createHorizontalGlue());
        box.add(search);
        box.add(Box.createHorizontalStrut(3));

        JPanel levelFillPanel = new JPanel(new BorderLayout());
        final JButton homeLabel = new JButton();
        homeLabel.setText("<HTML><FONT color=\"#000099\"><U>Home</U></FONT></HTML>");
        homeLabel.setHorizontalAlignment(SwingConstants.LEFT);
        homeLabel.setBorderPainted(false);
        homeLabel.setBorder(BorderFactory.createEmptyBorder());
        homeLabel.setOpaque(false);
        homeLabel.setBackground(Color.WHITE);
        homeLabel.setForeground(Color.RED);
        homeLabel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String query = "name = 'Home'";
                Predicate myPredicate1 = (Predicate)ExpressionParser.parse(query);
                Iterator ite = vis.getVisualGroup(treeNodes).tuples(myPredicate1);
                while(ite.hasNext()){
                    Object obj = ite.next();
                    if(obj instanceof NodeItem){
                        TupleSet ts = vis.getFocusGroup(hover);

                        if(((NodeItem)obj).getString("name").equalsIgnoreCase("Home")){
                            ts.setTuple((NodeItem)obj);
                        }

                    }
                }

//                gview.getMyPredicate().setFilterInfo("Ha Noi","site",true);

                gview.getMyPredicate().setAllInfo(true);

                vis.run("filter");
            }
        });


        levelFillPanel.add(homeLabel,BorderLayout.WEST);
        levelFillPanel.add(childrenLabel,BorderLayout.CENTER);
        levelFillPanel.setBackground(Color.WHITE);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(levelFillPanel, BorderLayout.NORTH);
        panel.add(gview, BorderLayout.CENTER);
        panel.add(box, BorderLayout.SOUTH);
        panel.setBackground(Color.WHITE);

        Color BACKGROUND = Color.WHITE;
        Color FOREGROUND = Color.BLACK;
        UILib.setColor(panel, BACKGROUND, FOREGROUND);
   //     panel.add(totalGUI, BorderLayout.WEST);
        vis.run("onlydraw");
        return panel;
    }

//    public void createGroupFilter(String label,String name,String filter) {
//        myPredicate = new HiddenGroupPredicate(m_vis, tree, name,filter,true);
//        VisibilityFilter visibilityFilter = new VisibilityFilter(treeNodes, myPredicate);
//
//        ActionList siteFilter = new ActionList();
//        siteFilter.add(visibilityFilter);
//        siteFilter.add(new TreeRootAction(tree));
//        siteFilter.add(hoverFilter);
//        siteFilter.add(fonts);
//        siteFilter.add(treeLayout);
//        siteFilter.add(subLayout);
//        siteFilter.add(nodeShape);
//        siteFilter.add(nodeColor);
//        siteFilter.add(textColor);
//
//        siteFilter.add(arrowColor);
//        siteFilter.add(edgeColor);
//        siteFilter.add(edgeSize);
//        m_vis.putAction(label, siteFilter);
//    }

    public NCMS_Routing(final Graph g, String label) {
        super(new Visualization());
        
        m_label = label;
        // -- set up visualization --
        m_vis.add(tree, g);
        m_vis.setInteractive(treeEdges, null, true);
        // renderer for node
        m_nodeRenderer = new LabelRenderer(m_label,"image");
        //m_nodeRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
        m_nodeRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_DRAW_AND_FILL);
        m_nodeRenderer.setVerticalAlignment(Constants.CENTER);
//        m_nodeRenderer.setRoundedCorner(12,12);
//        m_nodeRenderer.setImageTextPadding(5);
//        m_nodeRenderer.setTextField(null);
        m_nodeRenderer.setHorizontalPadding(0);
        m_nodeRenderer.setVerticalPadding(0);
        m_nodeRenderer.setImagePosition(Constants.TOP);
        m_nodeRenderer.setMaxImageDimensions(40,40);

        // renderer for edges
        m_edgeRenderer = new EdgeRenderer();
        // m_edgeRenderer.setArrowHeadSize(5,10);
        m_edgeRenderer.setEdgeType(Constants.EDGE_TYPE_LINE);
        m_edgeRenderer.setArrowType(Constants.EDGE_ARROW_FORWARD);

//        m_vis.addFocusGroup(wrongPath);

        //
//        String query = "status = 'wrong'";
//                Predicate myPredicate2 = (Predicate)ExpressionParser.parse(query);
//
//        Iterator ite = m_vis.getVisualGroup(treeEdges).tuples();
//        while(ite.hasNext()){
//            VisualItem item = (VisualItem)ite.next();
//            System.out.println();
//        }
        //
//        DefaultRendererFactory rf = new DefaultRendererFactory(m_nodeRenderer);
//        rf.add(new InGroupPredicate(treeEdges), m_edgeRenderer);
//        m_vis.setRendererFactory(rf);
        
        DefaultRendererFactory rf = new DefaultRendererFactory();
        rf.setDefaultRenderer(m_nodeRenderer);
        rf.setDefaultEdgeRenderer(m_edgeRenderer);
        m_vis.setRendererFactory(rf);
        // The filter for the hover group

    	hoverFilter = new GraphDistanceFilter(tree,hover,Integer.MAX_VALUE);
    	hoverFilter.setEnabled(false);

    	// The tuple set listener for the hover group
    	hoverListener = new GroupDistanceListener(tree,m_vis,hoverFilter,"onlydraw");

    	m_vis.addFocusGroup(hover);
    	TupleSet hoverGroup = m_vis.getFocusGroup(hover);
    	hoverGroup.addTupleSetListener(hoverListener);

        m_vis.addFocusGroup(wrongPath);

// -- set up processing actions --
// colors
//      int[] palette_edges = new int[] {
// 	    ColorLib.rgb(255,180,180)
// 	};
//int[] palette_nodes = new int[] {
// 	    ColorLib.rgb(103,194,255), ColorLib.rgb(103,154,255)
// 	};

        
        int[] palette_edges = new int[] {
         	    ColorLib.color(Color.CYAN)
         	};
        
        int[] palette_nodes = new int[] {
         	    ColorLib.gray(89, 55), ColorLib.blue(255)
         	};
        nodeColor = new NodeColorAction(treeNodes,palette_nodes);
        textColor = new TextColorAction(treeNodes);

         nodeShape = new NodeShapeAction(treeEdges);
       // final DataSizeAction nodeShape = new DataSizeAction(treeNodes,"gender");
       // ItemAction nodeSize = new NodeSizeAction(treeNodes);
      
         edgeColor= new EdgeColorAction(treeEdges,palette_edges);
         arrowColor = new ArrowColorAction(treeEdges,palette_edges);
//         edgeSize = new EdgeSizeAction(treeEdges);

        
        fonts = new FontAction(treeNodes, FontLib.getFont("Calibri", 13));
        fonts.add("ingroup('_focus_')", FontLib.getFont("Calibri", 13));
        
        //edgecolor
        ecolor = new ActionList();
        ecolor.add(arrowColor);
        ecolor.add(edgeColor);        
//        ecolor.add(edgeSize);
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
         treeLayout= new FlexibleRadialTreeLayout(tree);
        
       
        //treeLayout.setAngularBounds(-Math.PI/2, Math.PI);
        m_vis.putAction("treeLayout", treeLayout);
      
        subLayout = new CollapsedSubtreeLayout(tree);
        m_vis.putAction("subLayout", subLayout);


        // create the filtering and layout
        ActionList filter = new ActionList();

        myPredicate = new HiddenGroupPredicate(m_vis, treeNodes, "Ha Noi","site",false);
        myPredicate.setAllInfo(true);
        VisibilityFilter hiddenFilter = new VisibilityFilter(treeNodes, myPredicate);

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
//        filter.add(edgeSize);
        filter.add(hiddenFilter);
       
        m_vis.putAction("filter", filter);

//        createGroupFilter("home","Ha Noi","name");

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
        m_vis.alwaysRunAfter("home", "animate");
        m_vis.alwaysRunAfter("animate", "animatePaint");
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
        // ------------------------------------------------
        
        // initialize the display
        setSize(900,950);
        setItemSorter(new TreeDepthItemSorter());
        fc = new FocusControl(1, "filter");
        fc1 = new FocusControl(1,"home");
        nc = new NeighborHighlightControl(m_vis,neighborGroups);
        addControlListener(new DragControl());
//        addControlListener(new ZoomToFitControl());
        addControlListener(new ZoomControl());
        addControlListener(new PanControl());
        addControlListener(fc);
        addControlListener(new HoverActionControl("repaint"));
        addControlListener(nc);
        addControlListener(new HoverControl(hover));
//        addControlListener(fc1);

        PopupMenuController popup = new PopupMenuController(m_vis);
         addControlListener(popup);
//      makes us able to stop TextEditor by special KeyEvents (e.g. Enter)
        getTextEditor().addKeyListener(popup);
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
        
        final SearchTupleSet search = new PrefixSearchTupleSet();
        m_vis.addFocusGroup(Visualization.SEARCH_ITEMS, search);
        search.addTupleSetListener(new TupleSetListener() {
            public void tupleSetChanged(TupleSet t, Tuple[] add, Tuple[] rem) {
                try {
                    m_vis.cancel("animatePaint");
                    m_vis.run("recolor");
                    m_vis.run("animatePaint");
                    Iterator ite = t.tuples();
                    int i =0;
                    ArrayList<NodeItem> nodeItem=new ArrayList<NodeItem>();
                    while (ite.hasNext()) {
                        Object obj = ite.next();
                        if (obj instanceof NodeItem) {
//                        gview.getNc().setNeighbourHighlight((NodeItem)obj);
                            nodeItem.add((NodeItem)obj);
                            i++;

//                        ts.clear();
//                        ts.setTuple((NodeItem)obj);
//                            if (((NodeItem) obj).getString("gtCode").equalsIgnoreCase("root")) {

//                            }
//                        gview.getHoverListener().tupleSetChanged(ts,null,null);
//                        break;
                        }
                    }
                    TupleSet ts = m_vis.getFocusGroup(hover);
                    if(nodeItem.size()==1){

                         ts.setTuple(nodeItem.get(0));
                         myPredicate.setSearchOneNodeInfo(nodeItem.get(0).getString("name"),nodeItem.get(0).getString("mainPath"),nodeItem.get(0).getString("slavePath"));
                         m_vis.run("filter");
                    }else{
                         for(NodeItem nodeItema:nodeItem){
                             ts.addTuple(nodeItema);
                         }
                    }


                } catch (Exception ex) {
                    ex.printStackTrace();
                }


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
            TupleSet focus = m_vis.getGroup(hover);
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
//                    m_vis.run("home");

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
        	 
        	super(group, "image",Constants.NOMINAL, VisualItem.FILLCOLOR, palette);
        	add("_hover", ColorLib.gray(89, 55));
            add("ingroup('_search_')", ColorLib.gray(0,255));
            add("ingroup('_focus_')", ColorLib.gray(89, 55));

//            add("gtCode='group'", ColorLib.rgb(45,168,987));

//            add("site='wrong'", ColorLib.color(Color.RED));
            
            add(new InGroupPredicate(neighborGroups[0]), ColorLib.rgb(135,206,250));
        	add(new InGroupPredicate(neighborGroups[1]), ColorLib.rgb(143,210,143));
            add(new InGroupPredicate(neighborGroups[2]), ColorLib.rgb(216,191,216));
        }
                
    } // end of inner class NodeColorAction
    
    /**
     * Set node text colors
     */
    public static class TextColorAction extends ColorAction {
        public TextColorAction(String group) {
            super(group, VisualItem.TEXTCOLOR, ColorLib.color(Color.BLACK));
            add("_hover", ColorLib.rgb(255,100,255));
            add("ingroup('_search_')", ColorLib.rgb(255,150,255));
            add("ingroup('_focus_')", ColorLib.rgb(255,55,255));
            
        	add(new InGroupPredicate(neighborGroups[0]), ColorLib.rgb(0, 0,100));
        	add(new InGroupPredicate(neighborGroups[1]), ColorLib.rgb(0,100,0));
            add(new InGroupPredicate(neighborGroups[2]), ColorLib.rgb(160,32,240));
        }
    } // end of inner class TextColorAction
    
    /**
     * Set edge  colors
     */
    public static class EdgeColorAction extends DataColorAction {
        public EdgeColorAction(String group,int [] palette) {
        	super(group,"weight",Constants.EDGE_ARROW_FORWARD, VisualItem.STROKECOLOR, palette);
        	add("_hover", ColorLib.color(Color.ORANGE));
            add("weight == 2", ColorLib.color(Color.MAGENTA));
            add("weight == 3", ColorLib.color(Color.GRAY));
//            add("ingroup('_wrong_')", ColorLib.color(Color.MAGENTA));
//            add("site='wrong'", ColorLib.color(Color.RED));
       }
    } // end of inner EdgeColorAction
    public static class ArrowColorAction extends DataColorAction {
        public ArrowColorAction(String group,int [] palette) {
        	super(group,"weight",Constants.EDGE_ARROW_FORWARD, VisualItem.FILLCOLOR, palette);
            add("_hover", ColorLib.color(Color.ORANGE));
            add("weight == 2", ColorLib.color(Color.MAGENTA));
            add("weight == 3", ColorLib.color(Color.GRAY));
//            add("status='wrong'", ColorLib.color(Color.MAGENTA));
       }
    } // end of inner EdgeColorAction
    /**
     * Set edge size
     */
//    public static class EdgeSizeAction extends  DataSizeAction {
//        public EdgeSizeAction(String group) {
//        	super(group,"weight",Constants.NUMERICAL);
//        	//add("_hover", 1);
//
//        }
//    } // end of inner EdgeColorAction

//------------------------------------------------------------------------------------------------------------------------------------
    
    /**
     * Set node Shape
     */
    public static class NodeShapeAction extends ShapeAction {
        public NodeShapeAction(String group) {
        	super(group);
            add("_hover", Constants.SHAPE_DIAMOND);
            add("ingroup('_search_')", Constants.SHAPE_DIAMOND);
            add("ingroup('_focus_')", Constants.SHAPE_HEXAGON);
            
        	add(new InGroupPredicate(neighborGroups[0]),Constants.SHAPE_TRIANGLE_DOWN);
        	add(new InGroupPredicate(neighborGroups[1]), Constants.SHAPE_TRIANGLE_DOWN);
            add(new InGroupPredicate(neighborGroups[2]), Constants.SHAPE_TRIANGLE_DOWN);
        }
    } // end of inner class TextColorAction
    

    /**
     * Set node Size
     */
    public static class NodeSizeAction extends DataSizeAction {
        public NodeSizeAction(String group) {
            super(group,"gtCode",2);
           
//          add("ingroup('_search_')", ColorLib.rgb(255,255,255));
//          add("ingroup('_focus_')", ColorLib.rgb(255,255,255));
            
        	add(new InGroupPredicate(neighborGroups[0]),10);
        	add(new InGroupPredicate(neighborGroups[1]),20);
         // add(new InGroupPredicate(neighborGroups[2]), Constants.SHAPE_TRIANGLE_DOWN);
        }
    } // end of inner class TextColorAction
    
    
 //************************************************************************************************************************************   
    // Class NeighborHighlightControl

    class VisibleGroupPredicate extends AbstractPredicate {

        private Visualization vis;
        private String nodeGroupName;

        private String groupName;

        private String fillter;


        public VisibleGroupPredicate(Visualization vis, String nodeGroupName, String groupName,String filter) {
            this.vis = vis;
            this.nodeGroupName = nodeGroupName;
            this.groupName = groupName;
            this.fillter = filter;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public boolean getBoolean(Tuple t) {
            if (t instanceof Edge) {
                return false;
            }

            NodeItem vi = (NodeItem) vis.getVisualItem(nodeGroupName, t);

            Iterator iter = vi.edges();

            boolean result = false;

            if(vi.getString(fillter).equalsIgnoreCase(groupName)){
                result = true;
//                homeVisibleItems.add(vi);
            }

            while (!result && iter.hasNext()) {
                EdgeItem aEdgeItem = (EdgeItem) iter.next();

                if (aEdgeItem.getSourceItem().getString(fillter).equalsIgnoreCase(groupName)||aEdgeItem.getTargetItem().getString(fillter).equalsIgnoreCase(groupName)) {
                    if(!aEdgeItem.isVisible())
                        aEdgeItem.setVisible(true);
                }else{
                    aEdgeItem.setVisible(false);
                }

            }

            return result;
        }

        public String getFillter() {
            return fillter;
        }

        public void setFillter(String fillter) {
            this.fillter = fillter;
        }
    }

     class HiddenGroupPredicate extends AbstractPredicate {

        private Visualization vis;
        private String nodeGroupName;

        private String value;

        private String field;

        private boolean isVisible;

        private boolean searchOneNode = false;

        private String mainPath;

        private String slavePath;

        private String node;

        private boolean all;

        public HiddenGroupPredicate(Visualization vis, String nodeGroupName, String value,String field,boolean isVisible) {
            this.vis = vis;
            this.nodeGroupName = nodeGroupName;
            this.value = value;
            this.field = field;
            this.isVisible = isVisible;
        }

        public void setFilterInfo(String value,String field,boolean isVisible){
            searchOneNode = false;
            all = false;
            this.value = value;
            this.field = field;
            this.isVisible= isVisible;
        }

         public void setAllInfo(boolean all){
            this.all = all;

        }

        public void setSearchOneNodeInfo(String node,String mainPath,String slavePath){
            searchOneNode = true;
            all = false;
            this.node = node;
            this.mainPath = mainPath;
            this.slavePath = slavePath;
        }

         public boolean getBoolean(Tuple t) {

             if (t instanceof Edge) {
                 return false;
             }



             NodeItem vi = (NodeItem) vis.getVisualItem(nodeGroupName, t);

             Iterator iter = vi.edges();

              boolean result = false;

             if (all) {
                  if(vi.getString("gtCode").equalsIgnoreCase("Home")||vi.getString("gtCode").equalsIgnoreCase("root")||vi.getString("gtCode").equalsIgnoreCase("group")){
                      result = false;
                  }else{
                      result = true;
                  }
                  while (iter.hasNext()) {
                     EdgeItem aEdgeItem = (EdgeItem) iter.next();
                     if (!result) {
                         aEdgeItem.setVisible(false);
                      }
//                     else{
//                        aEdgeItem.setVisible(true);
//                      }




                 }
                 return result;
             }


             if (searchOneNode) {

                 if (vi.getString("name").equalsIgnoreCase(node)) {
                     result = true;

                 }

                 while (iter.hasNext()) {
                     EdgeItem aEdgeItem = (EdgeItem) iter.next();

                     if (aEdgeItem.getSourceItem().getString("name").matches(""+node)) {
                         result = true;
                         aEdgeItem.setVisible(true);
                     }
                      else {
                         aEdgeItem.setVisible(false);
                     }


                 }

             } else {

                 if (vi.getString(field).equalsIgnoreCase(value)) {
                     result = isVisible;
//                homeVisibleItems.add(vi);
                 } else {
                     result = !isVisible;
                 }
//                } catch (Exception exx) {
//                         result = true;
//                }


                 while (!result && iter.hasNext()) {
                     EdgeItem aEdgeItem = (EdgeItem) iter.next();

                     if (isVisible) {
                         if (aEdgeItem.getSourceItem().getString(field).equalsIgnoreCase(value) && aEdgeItem.getTargetItem().getString(field).equalsIgnoreCase(value)) {
                             if (!aEdgeItem.isVisible())
                                 aEdgeItem.setVisible(isVisible);
//                        homeVisibleItems.add(aEdgeItem);


//                    result = true;

                         } else {
                             aEdgeItem.setVisible(!isVisible);
                         }
                     } else {
                         if (aEdgeItem.getSourceItem().getString(field).equalsIgnoreCase(value) || aEdgeItem.getTargetItem().getString(field).equalsIgnoreCase(value)) {

                             aEdgeItem.setVisible(isVisible);
//                        homeVisibleItems.add(aEdgeItem);


//                    result = true;

                         } else {
                             aEdgeItem.setVisible(!isVisible);
                         }
                     }


                 }
             }


             return result;

         }

         public String getValue() {
             return value;
         }

         public void setValue(String value) {
             this.value = value;
         }

         public String getField() {
             return field;
         }

         public void setField(String field) {
             this.field = field;
         }

         public boolean isVisible() {
             return isVisible;
         }

         public void setVisible(boolean visible) {
             isVisible = visible;
         }
     }

    public class NeighborHighlightControl extends ControlAdapter {
    	
    	private Visualization visu;
    	
    	String sourceGroupName, targetGroupName, bothGroupName;
    	
    	TupleSet sourceTupleSet, targetTupleSet, bothTupleSet;

        VisualItem item;
    	
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
    	
    	public void itemEntered(final VisualItem item, final MouseEvent e) {
    	    if (item instanceof NodeItem){

                setNeighbourHighlight((NodeItem) item);
                this.item = item;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (tooltipScreen == null) {
                        tooltipScreen = new MyInternalFrame(null);
                    }
                    tooltipScreen.setLocation(e.getLocationOnScreen());
                    tooltipScreen.showDetail(item.getString("name"), item.getString("gtCode"), item.getString("mainPath"), item.getString("slavePath"),item.getString("ip"),item.getString("port"));
                    tooltipScreen.setVisible(true);
                    }
                });



            }

    	}
    	
    	public void itemExited(VisualItem item, MouseEvent e) {
            if (item instanceof NodeItem) {
                sourceTupleSet.clear();
                targetTupleSet.clear();
                bothTupleSet.clear();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                         if (tooltipScreen != null) {
                    tooltipScreen.setVisible(false);
                }
                    }
                });

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

            if (ts.getTupleCount() == 0) {
                if (previousVisibleItems != null) {

                    Iterator iter = previousVisibleItems.iterator(); // reconstructimg the pre filtered state
                    while (iter.hasNext()) {
                        VisualItem aItem = (VisualItem) iter.next();

//                        aItem.setVisible(true);

                    }
                }
                lastTimeFiltered = false;
                filter.setEnabled(false);

            } else {
                if (!lastTimeFiltered) { // remembering the last unfiltered set of visible items
                    previousVisibleItems.clear();
                    Iterator iter = vis.visibleItems(graph);
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
        VisualItem item;
        public HoverControl(String hoverGroupName) {
            this.hoverGroupName = hoverGroupName;
        }

        public void itemEntered(VisualItem item, MouseEvent e) {

            if (item instanceof NodeItem) {
                Visualization vis = item.getVisualization();
                vis.getGroup(this.hoverGroupName).setTuple(item);
                this.item = item;
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

    public static class FlexibleRadialTreeLayout extends RadialTreeLayout {
        private double scaleX = 1.0, scaleY = 1.0;

        public FlexibleRadialTreeLayout(String group) {
            super(group);
        }

        protected void setPolarLocation(NodeItem n, NodeItem p, double r, double t) {
            setX(n, p, m_origin.getX() + scaleX * r * Math.cos(t));
            setY(n, p, m_origin.getY() + scaleY * r * Math.sin(t));
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

    		JMenuItem delete = new JMenuItem("Kết nối", 'd');
    		JMenuItem editText = new JMenuItem("Backup cấu hình", 'a');
    		JMenuItem addEdge = new JMenuItem("Restore cấu hình", 'e');
    		JMenuItem addNode = new JMenuItem("Import cấu hình", 'n');

    		delete.setActionCommand("node_delete");
    		editText.setActionCommand("node_editText");
    		addEdge.setActionCommand("node_addEdge");
    		addNode.setActionCommand("node_addNode");

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
    				switch (action) {
					case 1:	//	create a node

						break;

					case 2:	//	create an edge
    					creatingEdge = true;
						break;

					case 3:	//	rename node

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
    			} else if (e.getActionCommand().endsWith("editText")) {

    			} else if (e.getActionCommand().endsWith("addEdge")) {


    			} else if (e.getActionCommand().endsWith("addNode")) {

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


    } // end of inne

    public GraphDistanceFilter getHoverFilter() {
        return hoverFilter;
    }

    public void setHoverFilter(GraphDistanceFilter hoverFilter) {
        this.hoverFilter = hoverFilter;
    }

    public GroupDistanceListener getHoverListener() {
        return hoverListener;
    }

    public void setHoverListener(GroupDistanceListener hoverListener) {
        this.hoverListener = hoverListener;
    }

    public ArrayList getHomeList() {
        return homeList;
    }

    public void setHomeList(ArrayList homeList) {
        this.homeList = homeList;
    }

    public VisualItem getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(VisualItem currentItem) {
        this.currentItem = currentItem;
    }

    public JSearchPanel getSearchPanel() {
        return searchPanel;
    }

    public void setSearchPanel(JSearchPanel searchPanel) {
        this.searchPanel = searchPanel;
    }

    public FocusControl getFc() {
        return fc;
    }

    public void setFc(FocusControl fc) {
        this.fc = fc;
    }

    public NeighborHighlightControl getNc() {
        return nc;
    }

    public void setNc(NeighborHighlightControl nc) {
        this.nc = nc;
    }

    public HiddenGroupPredicate getMyPredicate() {
        return myPredicate;
    }

    public void setMyPredicate(HiddenGroupPredicate myPredicate) {
        this.myPredicate = myPredicate;
    }
} // end of class  prefuse.demos.RadialGraphViewExtendedFeatures
