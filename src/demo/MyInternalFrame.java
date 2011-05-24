/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package demo;

import javax.swing.*;
import java.awt.*;

/* Used by InternalFrameDemo.java. */
public class MyInternalFrame extends JWindow {

    private String exchangeCode;
    private JLabel exchangeCodeLbl;
    private String gtCode;
    private JLabel gtCodeLbl;
    private String mainPath;
    private JLabel mainPathLbl;
    private String slavePath;
    private JLabel slavePathLbl;
    private JLabel ipLbl;
    private JLabel portLbl;

    public MyInternalFrame(Frame frame) {
        super(frame);
        this.setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        exchangeCodeLbl = new JLabel();
        gtCodeLbl = new JLabel();
        mainPathLbl = new JLabel();
        slavePathLbl = new JLabel();
        ipLbl = new JLabel();
        portLbl = new JLabel();
        mainPanel.add(exchangeCodeLbl);
        mainPanel.add(gtCodeLbl);
        mainPanel.add(mainPathLbl);
        mainPanel.add(slavePathLbl);
        mainPanel.add(ipLbl);
        mainPanel.add(portLbl);
        this.add(mainPanel, BorderLayout.CENTER);
        this.setPreferredSize(new Dimension(200, 110));
        this.setSize(this.getPreferredSize());
    }

    public void showDetail(String exchangeCode,String gtCode,String mainPath,String slavePath,String ip,String port){
        exchangeCodeLbl.setText("Mã Node : " +exchangeCode);
        gtCodeLbl.setText("GT : " +gtCode);
        mainPathLbl.setText("Hướng chính : " +mainPath);
        slavePathLbl.setText("Hướng phụ : " +slavePath);
        ipLbl.setText("Ip : " +ip);
        portLbl.setText("Port : " +port);
    }
}
