package sample;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataSizeAction;
import prefuse.action.layout.Layout;
import prefuse.action.layout.SpecifiedLayout;
import prefuse.activity.Activity;
import prefuse.controls.*;
import prefuse.data.Graph;
import prefuse.data.io.GraphMLReader;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * This small demo shall demonstrate some simple issues:
 * 1) use of a DataSizeAction to vary item sizes based on a datafield
 * 2) use of SpecifiedLayout to assign items positions based on datafields
 * 3) how to stop a running action upon window deactivation
 * 
 * @author Bj�rn Kruse
 */
public class SpecifiedLayout_DataSizeAction extends JPanel {

    private static final String graph = "graph";
    private static final String nodes = "graph.nodes";
    private static final String edges = "graph.edges";
    private Visualization m_vis;
    
    public SpecifiedLayout_DataSizeAction(Graph g, String label) {
        m_vis = new Visualization();
        
        LabelRenderer tr = new LabelRenderer(label);
        tr.setRoundedCorner(8, 8);
        m_vis.setRendererFactory(new DefaultRendererFactory(tr));

        m_vis.addGraph(graph, g);

        ColorAction fill = new ColorAction(nodes, VisualItem.FILLCOLOR, ColorLib.rgb(200, 200, 255));
        fill.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255, 200, 125));
        
        ActionList coloring = new ActionList();
        coloring.add(fill);
        coloring.add(new ColorAction(nodes, VisualItem.TEXTCOLOR, ColorLib.rgb(0, 0, 0)));
        coloring.add(new ColorAction(edges, VisualItem.FILLCOLOR, ColorLib.gray(200)));
        coloring.add(new ColorAction(edges, VisualItem.STROKECOLOR, ColorLib.gray(200)));
        
        // 1) the DataSizeAction for the edges based on datafield "weight"
        Action dataSizeAction = new DataSizeAction("graph.edges", "weight");
        
        // 2) the SpecifiedLayout, which assigns x/y coordinates based on 
        //    datafields x-coord and y-coord
        Layout specifiedLayout = new SpecifiedLayout("graph.nodes", "x-coord", "y-coord");
        
        Action printing = new Action() {
			public void run(double frac) {
				System.out.println("action is running");
			}
        };
        printing.setStepTime(1000); // seems not to work, don't know why. Bug?
        
        ActionList draw = new ActionList(Activity.INFINITY);
        // commented out because it produces much output to console
        //   remove comments to see 3) in "action"
//        draw.add(printing);  
        draw.add(coloring);  
        draw.add(specifiedLayout);  
        draw.add(dataSizeAction);  
        draw.add(new RepaintAction());
        m_vis.putAction("draw", draw);  
        
        // --------------------------------------------------------------------
        // set up a display to show the visualization
        
        Display display = new Display(m_vis);
        display.setSize(300,300);
        display.pan(50,50);
        display.addControlListener(new PanControl());
        display.addControlListener(new ZoomControl());
        display.addControlListener(new WheelZoomControl());
        display.addControlListener(new ZoomToFitControl());
        display.addControlListener(new NeighborHighlightControl());

        // now we run our action list
        m_vis.run("draw");
        
        add(display);
    }
    
    public static void main(String[] args) {
        Graph g = null;
        try {
        	g = new GraphMLReader().readGraph("datasets/graphml-sample2.xml"); //datafile
        } catch ( Exception e ) {
        	e.printStackTrace();
        	System.exit(1);
        }
        final SpecifiedLayout_DataSizeAction view = new SpecifiedLayout_DataSizeAction(g, "name");
        JFrame frame = new JFrame("prefuse  |  specifiedLayout and DataSizeAction demo");
        frame.setContentPane(view);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // 3) how to stop a running action if prefuse window is deactivated
        frame.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                view.m_vis.run("draw");
            }
            public void windowDeactivated(WindowEvent e) {
                view.m_vis.cancel("draw");
            }
        });
    }
    
}
