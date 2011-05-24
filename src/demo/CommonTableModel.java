package demo;

import javax.swing.table.AbstractTableModel;
import java.util.Enumeration;
import java.util.Vector;

/**
 *<p>
 * Title: CommonTableModel
 * </p>
 *<p>
 * Description: Common utility functions which which is used by WiBro EMS
 * </p>
 *<p>
 * Copyright: Copyright (c) 2005
 * </p>
 *<p>
 * Company: LG Electronics Inc.
 * </p>
 * 
 * @author ȫ�¼�
 *@version 1.0
 *@created 2005.06.07
 *@modified 2005.06.07
 *@product WiBro_EMS
 *@sw_block util_library
 *@function_no 207-36
 */
public class CommonTableModel extends AbstractTableModel
{
    protected Vector columnInfo;
    protected Vector dataInfo;
    Vector cellEditableInfo = new Vector();
    final int LIMIT_DATA = 50000;
    private int limitData;
    boolean unlimited = true;

    public CommonTableModel() {
        columnInfo = new Vector();
        dataInfo = new Vector();
        limitData = LIMIT_DATA;
    }

    public CommonTableModel(Vector columnInfo) {
        this.columnInfo = columnInfo;
        dataInfo = new Vector();
        limitData = LIMIT_DATA;
    }

    public CommonTableModel(Vector columnInfo, Vector dataInfo) {
        this.columnInfo = columnInfo;
        this.dataInfo = dataInfo;
    }

    public void setUnlimited(boolean flag) {
        unlimited = flag;
    }

    public void setLimitDataNum(int n) {
        unlimited = false;
        limitData = n;
    }

    public void addData(Vector oneRow) {
        if (dataInfo.size() > limitData) {
            takeOutSomeData();
        }
        dataInfo.addElement(oneRow);
    }

    public void addDataToTop(Vector oneRow) {
        if (dataInfo.size() > limitData) {
            takeOutSomeData();
        }
        dataInfo.add(0, oneRow);
    }

    public void addData(Vector oneRow, int index) {
        if (dataInfo.size() > limitData) {
            takeOutSomeData();
        }
        dataInfo.add(index, oneRow);
    }

    public void changeData(Vector oneRow, int row) {
        dataInfo.setElementAt(oneRow, row);
    }

    public void removeData(int index) {
        dataInfo.removeElementAt(index);
    }

    public void removeDataAll() {
        dataInfo.removeAllElements();
    }

    public void setData(Vector p_dataInfo) {
        // this.dataInfo = p_dataInfo;
        for (int i = 0; i < p_dataInfo.size(); i++) {
            dataInfo.addElement(p_dataInfo.get(i));
        }
        if (dataInfo.size() > limitData) {
            takeOutSomeData();
        }
    }

    private void takeOutSomeData() {
        if (unlimited)
            return;
        int diff = dataInfo.size() - limitData;
        for (int i = 0; i < diff; i++) {
            removeData(dataInfo.size() - 1);
        }
    }

    public void setColumn(Vector columnInfo) {
        this.columnInfo = columnInfo;
    }

    public int getColumnCount() {
        if (columnInfo != null)
            return columnInfo.size();
        else
            return 0;
    }

    public void setValueAt(Object value, int row, int column) {
        Object o = dataInfo.elementAt(row);
        if (o instanceof Vector) {
            Vector oneRow = (Vector) o;
            oneRow.setElementAt(value, column);
        }
    }

    public Vector getRowAt(int row) {
        if (row < getRowCount()) {
            return (Vector) dataInfo.elementAt(row);
        }
        return null;
    }

    public Object getValueAt(int row, int column) {
        if (row < getRowCount()) {
            Object object = dataInfo.elementAt(row);
            if (object instanceof Vector) {
                Vector rowInfo = (Vector) object;
                return rowInfo.elementAt(column);
            }
        }
        return null;
    }

    public int getRowCount() {
        if (dataInfo != null)
            return dataInfo.size();
        else
            return 0;
    }

    public String getColumnName(int column) {
        return columnInfo.elementAt(column).toString();
    }

    public Class getColumnClass(int column) {
        return getValueAt(0, column).getClass();
    }

    public void setCellEditable(int column) {
        cellEditableInfo.addElement(new Integer(column));
    }

    public boolean isCellEditable(int row, int column) {
        Enumeration enumeration = cellEditableInfo.elements();
        while (enumeration.hasMoreElements()) {
            Object o = enumeration.nextElement();
            if (o instanceof Integer) {
                Integer col = (Integer) o;
                if (col.intValue() == column)
                    return true;
            }
        }
        return false;
    }

    public Vector getData() {
        return this.dataInfo;
    }
}
