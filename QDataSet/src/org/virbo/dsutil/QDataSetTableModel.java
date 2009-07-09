/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dsutil;

import java.util.Enumeration;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.das2.datum.Units;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dsops.Ops;
import test.BundleBinsDemo;

/**
 *
 * @author jbf
 */
public class QDataSetTableModel extends AbstractTableModel {

    QDataSet ds;
    QDataSet bundle1;
    QDataSet dep0;
    QDataSet dep1;
    int dep0Offset;
    int colCount;
    Units[] units;
    String[] labels;

    QDataSetTableModel(QDataSet ds) {
        this.ds = ds;
        this.dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        dep0Offset = dep0 == null ? 0 : 1;
        this.bundle1 = (QDataSet) ds.property(QDataSet.BUNDLE_1);
        this.dep1 = (QDataSet) ds.property(QDataSet.DEPEND_1);

        colCount = dep0Offset;
        colCount += ds.length() == 0 ? 0 : ds.length(0);
        units = new Units[colCount];
        labels = new String[colCount];

        int i = 0;
        if (dep0 != null) {
            units[i++] = (Units) dep0.property(QDataSet.UNITS);
            labels[i++] = (String) dep0.property(QDataSet.LABEL);
        }

        if (bundle1 != null) {
            for (int j = 0; j < bundle1.length(); j++) {
                int n = 1;
                for (int k = 0; k < bundle1.length(j); k++) {
                    n *= bundle1.value(j, k);
                }
                for (int k = 0; k < n; k++) {
                    units[i] = (Units) bundle1.property(QDataSet.UNITS, j);
                    labels[i] = (String) bundle1.property(QDataSet.LABEL, j);
                    i++;
                }
            }
        } else if (dep1 != null) {
            Units dep1Units = (Units) dep1.property(QDataSet.UNITS);
            if (dep1Units == null) {
                dep1Units = Units.dimensionless;
            }
            for (int k = 0; k < dep1.length(); k++) {
                units[i] = (Units) ds.property(QDataSet.UNITS);
                labels[i] = dep1Units.createDatum(dep1.value(k)).toString();
                i++;
            }

        }

        for (i = 0; i < units.length; i++) {
            if (units[i] == null) {
                units[i] = Units.dimensionless;
            }
        }
    }

    public int getRowCount() {
        return ds.length();
    }

    public int getColumnCount() {
        return colCount;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex < dep0Offset) {
            return units[columnIndex].createDatum(this.dep0.value(rowIndex));
        } else {
            return units[columnIndex].createDatum(this.ds.value(rowIndex, columnIndex - dep0Offset));
        }
    }

    public TableColumnModel getTableColumnModel() {
        DefaultTableColumnModel result = new DefaultTableColumnModel();

        for (int i = 0; i < colCount; i++) {
            TableColumn c = new TableColumn();
            if (i < dep0Offset) {
                c.setHeaderValue(dep0.property(QDataSet.LABEL));
            } else {
                c.setHeaderValue(labels[i]);
            }
            result.addColumn(new TableColumn());
        }


        return result;
    }

    public static void main(String[] args) {
        QDataSet ds = BundleBinsDemo.demo1();
        QDataSetTableModel m = new QDataSetTableModel(ds);
        JTable t = new JTable();
        t.setColumnModel(m.getTableColumnModel());
        t.setModel(m);

        JFrame frame = new JFrame();
        frame.getContentPane().add(t);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }
}
