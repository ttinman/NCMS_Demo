package demo; /**
 *  NCMS_Topo  version 1.0     
 *
 * Copyright YYYY Viettel Telecom. All rights reserved.
 * VIETTEL PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import prefuse.util.ui.UILib;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;


/**
 * User: qlmvt_Trungnt42
 * Date: 5/19/11
 * Time: 2:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class NCMS_Topo  extends JPanel
{

    private JPanel topPanel = new JPanel(new FlowLayout());

    private JPanel centerPanel = new JPanel(new CardLayout());

    private String[] listTopos = {"Định tuyến GT","Tham số tổng đài"};

    private JComboBox comboBox = new JComboBox(listTopos);

    public NCMS_Topo(){
        initComponents();
        initActions();
    }

    public void initComponents(){
        setLayout(new BorderLayout());
        topPanel.add(comboBox,FlowLayout.LEFT);
        centerPanel.add(createRoutingPanel(),"Định tuyến GT");
        centerPanel.add(createParameterPanel(),"Tham số tổng đài");
        add(topPanel,BorderLayout.NORTH);
        add(centerPanel,BorderLayout.CENTER);
    }

    public void initActions(){
        comboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                CardLayout cl = (CardLayout)(centerPanel.getLayout());
                cl.show(centerPanel, (String)e.getItem());
            }
        });
    }

    public JPanel createRoutingPanel(){
         return NCMS_Routing.demo("datasets/socialnet_tree.xml", "name");
    }

    public JComponent createParameterPanel(){
         return NCMS_Parameter.demo("datasets/chi-ontology.xml", "name");
    }

    public static void main(String[] args){
        UILib.setAlloyLookAndFeel();
        JFrame frame = new JFrame("NCMS | Topology");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

//        Toolkit tk = Toolkit.getDefaultToolkit();
//        int xSize = ((int) tk.getScreenSize().getWidth());
//        int ySize = ((int) tk.getScreenSize().getHeight());
//        frame.setSize(xSize, ySize);
        frame.setUndecorated(true);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setBounds(0, 0, screenSize.width, screenSize.height);
        frame.setContentPane(new NCMS_Topo());

//        frame.pack();
        frame.setVisible(true);
    }
}
