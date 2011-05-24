package sample;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.controls.*;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.tuple.DefaultTupleSet;
import prefuse.data.tuple.TupleSet;
import prefuse.render.*;
import prefuse.render.Renderer;
import prefuse.util.ColorLib;
import prefuse.util.ui.UILib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 * Demonstration of graph editor functionality.
 * See https://sourceforge.net/forum/forum.php?thread_id=1597565&forum_id=343013
 * for a discussion about the rubberband/drag select/multiple selection of
 * nodes, while the following thread
 * https://sourceforge.net/forum/message.php?msg_id=3758973&forum_id=343013
 * contains the discussion about drawing edges interactively.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author Aaron Barsky
 * @author Bjï¿½rn Kruse
 * @author Juanjo Vega
 */
public class GraphNodesSelection extends Display {

    private static final String graphNodesAndEdges = "graph";
    private static final String graphNodes = "graph.nodes";
    private static final String graphEdges = "graph.edges";
    private static final String RUBBER_BAND = "rubberband";
    private static final String NODES = graphNodes;
    private static final String SELECTED = "sel";
    private LabelRenderer m_nodeRenderer;
    private EdgeRenderer m_edgeRenderer;
    private Graph g;

    public GraphNodesSelection() {
        super(new Visualization());

        initDataGroups();

        // -- set up visualization --
        m_vis.add(graphNodesAndEdges, g);

        // -- set up renderers --
        m_nodeRenderer = new LabelRenderer(VisualItem.LABEL);
        m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
        m_nodeRenderer.setRoundedCorner(8, 8);
        m_edgeRenderer = new EdgeRenderer();

        DefaultRendererFactory rf = new DefaultRendererFactory(m_nodeRenderer);
        rf.add(new InGroupPredicate(graphEdges), m_edgeRenderer);
        m_vis.setRendererFactory(rf);

        // -- set up processing actions --

        // colors
        ColorAction nodeTextColor = new ColorAction(graphNodes, VisualItem.TEXTCOLOR);
        ColorAction nodeFillColor = new ColorAction(graphNodes, VisualItem.FILLCOLOR, ColorLib.rgb(234, 234, 234));
        nodeFillColor.add("_hover", ColorLib.rgb(220, 200, 200));
        nodeFillColor.add(VisualItem.HIGHLIGHT, ColorLib.rgb(220, 220, 0));
        ColorAction nodeStrokeColor = new ColorAction(graphNodes, VisualItem.STROKECOLOR);

        ColorAction edgeLineColor = new ColorAction(graphEdges, VisualItem.STROKECOLOR, ColorLib.rgb(200, 200, 200));
        edgeLineColor.add("_hover", ColorLib.rgb(220, 100, 100));
        ColorAction edgeArrowColor = new ColorAction(graphEdges, VisualItem.FILLCOLOR, ColorLib.rgb(100, 100, 100));
        edgeArrowColor.add("_hover", ColorLib.rgb(220, 100, 100));

        // recolor
        ActionList recolor = new ActionList();
        recolor.add(nodeTextColor);
        recolor.add(nodeFillColor);
        recolor.add(nodeStrokeColor);
        recolor.add(edgeLineColor);
        recolor.add(edgeArrowColor);
        m_vis.putAction("recolor", recolor);


        ForceDirectedLayout fdl = new ForceDirectedLayout(graphNodesAndEdges);
        ActionList layout = new ActionList(ActionList.INFINITY);
        layout.add(fdl);
        layout.add(recolor);
        layout.add(new RepaintAction());
        m_vis.putAction("layout", layout);

        m_vis.putAction("repaint", new RepaintAction());

        // Create the focus group
        TupleSet selectedItems = new DefaultTupleSet();
        m_vis.addFocusGroup(SELECTED, selectedItems);

        // listen for changes
        TupleSet focusGroup = m_vis.getGroup(SELECTED);
        focusGroup.addTupleSetListener(new TupleSetListener() {

            public void tupleSetChanged(TupleSet ts, Tuple[] add, Tuple[] rem) {
                //do whatever you do with newly selected/deselected items
//                    	m_vis.cancel("layout");
                for (int i = 0; i < add.length; i++) {
                    VisualItem item = (VisualItem) add[i];
                    item.setHighlighted(true);

                }
                for (int i = 0; i < rem.length; i++) {
                    VisualItem item = (VisualItem) rem[i];
                    item.setHighlighted(false);
                }
//                        m_vis.run("layout");
            }
        });

        //      Create the rubber band object for rendering on screen
        Table rubberBandTable = new Table();
        rubberBandTable.addColumn(VisualItem.POLYGON, float[].class);
        rubberBandTable.addRow();
        m_vis.add(RUBBER_BAND, rubberBandTable);
        VisualItem rubberBand = (VisualItem) m_vis.getVisualGroup(RUBBER_BAND).tuples().next();
        rubberBand.set(VisualItem.POLYGON, new float[8]);
        rubberBand.setStrokeColor(ColorLib.color(ColorLib.getColor(255, 0, 0)));

        // render the rubber band with the default polygon renderer
        Renderer rubberBandRenderer = new PolygonRenderer(Constants.POLY_TYPE_LINE);
        rf.add(new InGroupPredicate(RUBBER_BAND), rubberBandRenderer);

        // Link the rubber band control to the rubber band display object
        addControlListener(new RubberBandSelect(rubberBand));

        pan(400, 300);
        zoom(new Point2D.Double(400, 300), 1.75);
        addControlListener(new DragControl());
        addControlListener(new ZoomToFitControl(Control.MIDDLE_MOUSE_BUTTON));
        addControlListener(new ZoomControl());
        ToolTipControl ttc = new ToolTipControl("label");
        Control hoverc = new ControlAdapter() {

            public void itemEntered(VisualItem item, MouseEvent evt) {
//                if (item.isInGroup("canUrban")) {
//                    g_total.setText(item.getString("label"));
                    Display d = (Display)evt.getSource();
                    d.setToolTipText(item.getString(VisualItem.LABEL));
//                }
            }

            public void itemExited(VisualItem item, MouseEvent evt) {
//                if (item.isInGroup("canUrban")) {
//                    g_total.setText(g_totalStr);
                     Display d = (Display)evt.getSource();
                    d.setToolTipText(null);
//                }
            }
        };
        addControlListener(ttc);
        addControlListener(hoverc);
        // filter graph and perform layout
        m_vis.run("layout");
    }

    private void initDataGroups() {
        // create sample graph
        g = new Graph(true);
        for (int i = 0; i < 3; ++i) {
            Node n1 = g.addNode();
            Node n2 = g.addNode();
            Node n3 = g.addNode();
            g.addEdge(n1, n2);
            g.addEdge(n1, n3);
        }
        g.addEdge(0, 3);
        g.addEdge(6, 0);

        // add labels for nodes and edges
        g.addColumn(VisualItem.LABEL, String.class);
        for (int i = 0; i < 9; i++) {
            g.getNode(i).setString(VisualItem.LABEL, "Node " + i);

        }
    }

    public static void main(String argv[]) {
        initUI();
    }

    private static void initUI() {
        UILib.setPlatformLookAndFeel();

        //the main panel = the visual editor
        GraphNodesSelection ed = new GraphNodesSelection();

        //add everything together and display the frame
        JPanel main = new JPanel(new BorderLayout());
        main.add(ed, BorderLayout.CENTER);

        JFrame frame = new JFrame("p r e f u s e  |  n o d e s  s e l e c t i o n");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(800, 600));
        frame.setContentPane(main);
        frame.pack();
        frame.setVisible(true);
    }

    public static class PopupMenuController extends ControlAdapter implements ActionListener {

        private Graph g;
        private Display d;
        private Visualization vis;

        public PopupMenuController(Visualization vis) {
            this.vis = vis;
            this.g = (Graph) vis.getSourceData(graphNodesAndEdges);
            this.d = vis.getDisplay(0);
        }

        // ---------------------------------------------
        // --- methods for event processing
        // ---------------------------------------------
        @Override
        public void itemClicked(VisualItem item, MouseEvent e) {
        }

        @Override
        public void itemKeyReleased(VisualItem item, KeyEvent e) {
            keyReleased(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }

        /**
         * only necessary if edge-creation is used together with aggregates and
         * the edge should move on when mousepointer moves within an aggregate
         */
        @Override
        public void itemMoved(VisualItem item, MouseEvent e) {
        }

        public void actionPerformed(ActionEvent e) {
        }
    } // end of inner class PopupMenuController
//  Add a control to set the rubber band bounds and perform the actual selection logic

    public class RubberBandSelect extends ControlAdapter {

        private int downX1,  downY1;
        private VisualItem rubberBand;
        Point2D screenPoint = new Point2D.Float();
        Point2D absPoint = new Point2D.Float();
        Rectangle2D rect = new Rectangle2D.Float();
        Rectangle r = new Rectangle();

        public RubberBandSelect(VisualItem rubberBand) {
            this.rubberBand = rubberBand;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }

            Display d = (Display) e.getComponent();
            Visualization vis = d.getVisualization();
            TupleSet focus = vis.getFocusGroup(SELECTED);
            if (!e.isShiftDown()) {
                focus.clear();
            }

            float[] bandRect = (float[]) rubberBand.get(VisualItem.POLYGON);
            bandRect[0] = bandRect[1] = bandRect[2] = bandRect[3] =
                    bandRect[4] = bandRect[5] = bandRect[6] = bandRect[7] = 0;

            d.setHighQuality(false);
            screenPoint.setLocation(e.getX(), e.getY());
            d.getAbsoluteCoordinate(screenPoint, absPoint);
            downX1 = (int) absPoint.getX();
            downY1 = (int) absPoint.getY();
            rubberBand.setVisible(true);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }

            Display d = (Display) e.getComponent();
            screenPoint.setLocation(e.getX(), e.getY());
            d.getAbsoluteCoordinate(screenPoint, absPoint);
            int x1 = downX1;
            int y1 = downY1;
            int x2 = (int) absPoint.getX();
            int y2 = (int) absPoint.getY();

            float[] bandRect = (float[]) rubberBand.get(VisualItem.POLYGON);
            bandRect[0] = x1;
            bandRect[1] = y1;
            bandRect[2] = x2;
            bandRect[3] = y1;
            bandRect[4] = x2;
            bandRect[5] = y2;
            bandRect[6] = x1;
            bandRect[7] = y2;

            if (x2 < x1) {
                int temp = x2;
                x2 = x1;
                x1 = temp;
            }
            if (y2 < y1) {
                int temp = y2;
                y2 = y1;
                y1 = temp;
            }
            rect.setRect(x1, y1, x2 - x1, y2 - y1);

            Visualization vis = d.getVisualization();
            TupleSet focus = vis.getFocusGroup(SELECTED);

            if (!e.isShiftDown()) {
                focus.clear();
            }

            //allocate the maximum space we could need

            Tuple[] selectedItems = new Tuple[vis.getGroup(NODES).getTupleCount()];
            Iterator it = vis.getGroup(NODES).tuples();

            //in this example I'm only allowing Nodes to be selected
            int i = 0;
            while (it.hasNext()) {
                VisualItem item = (VisualItem) it.next();
                if (item.isVisible() && item.getBounds().intersects(rect)) {
                    selectedItems[i++] = item;
                }
            }

            //Trim the array down to the actual size
            Tuple[] properlySizedSelectedItems = new Tuple[i];
            System.arraycopy(selectedItems, 0, properlySizedSelectedItems, 0, i);
            for (int j = 0; j < properlySizedSelectedItems.length; j++) {
                Tuple tuple = properlySizedSelectedItems[j];
                focus.addTuple(tuple);
            }

            rubberBand.setValidated(false);
            d.repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            rubberBand.setVisible(false);
            Display d = (Display) e.getComponent();

            d.setHighQuality(true);
            d.getVisualization().repaint();
        }
    }
} // end of class GraphEditor