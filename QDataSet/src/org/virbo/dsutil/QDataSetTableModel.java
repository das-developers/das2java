
package org.virbo.dsutil;

import java.util.UnknownFormatConversionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.FormatStringFormatter;
import org.das2.util.LoggerManager;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import test.BundleBinsDemo;

/**
 * TableModel that shows a QDataSet in a JTable.
 * @author jbf
 */
public class QDataSetTableModel extends AbstractTableModel {

    private static final Logger logger= LoggerManager.getLogger("qdataset.dsutil");
            
    QDataSet ds;
    QDataSet wds;      // weights for ds;
    QDataSet bundle1;
    QDataSet dep0;     // rank 1 or rank 2 bins dataset.
    QDataSet dep1;     // rank 1 or rank 2 bins dataset.
    int dep0Offset;
    int colCount;
    Units[] units;
    String[] labels;
    DatumFormatter[] df;

    /**
     * creates a QDataSetTableModel
     * @param ds dataset to adapt to a model/
     */
    public QDataSetTableModel( QDataSet ds ) {
        this.ds = ds;
        this.wds= DataSetUtil.weightsDataSet(ds);
        this.dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        dep0Offset = dep0 == null ? 0 : 1;
        this.bundle1 = (QDataSet) ds.property(QDataSet.BUNDLE_1);
        this.dep1 = (QDataSet) ds.property(QDataSet.DEPEND_1);
        if ( dep1!=null && dep1.rank()>1 && !SemanticOps.isBins(dep1) ) {
            System.err.println("dep1 is sliced at 0");
        }

        colCount = dep0Offset;
        if ( ds.rank()==1 ) {
            colCount+= 1;
        } else {
            colCount += ds.length(0);
        }
        
        units = new Units[colCount];
        labels = new String[colCount];
        df= new DatumFormatter[colCount];

        int i = 0;
        if (dep0 != null) {
            units[i] = SemanticOps.getUnits(dep0);
            df[i]= units[i].getDatumFormatterFactory().defaultFormatter();
            labels[i] = (String) dep0.property(QDataSet.LABEL);
            i++;
        }

        if (bundle1 != null) {
            for (int j = 0; j < bundle1.length(); j++) {
                units[i] = (Units) bundle1.property(QDataSet.UNITS, j);
                if ( units[i]==null ) units[i]= Units.dimensionless;
                String format= (String)bundle1.property(QDataSet.FORMAT, j);
                if ( format==null ) {
                    df[i]= units[i].getDatumFormatterFactory().defaultFormatter();
                } else {
                    df[i]= getDataFormatter( format, units[i] );
                }
                labels[i] = (String) bundle1.property(QDataSet.LABEL, j);
                if ( labels[i]==null ) labels[i]= (String) bundle1.property(QDataSet.NAME, j);
                i++;
            }
        } else if (dep1 != null) {
            Units dep1Units = SemanticOps.getUnits(dep1);
            if (dep1Units == null) {
                dep1Units = Units.dimensionless;
            }
            int dep1len= ( dep1.rank()==1 || SemanticOps.isBins(dep1) ) ? dep1.length() : dep1.length(0);
            for (int k = 0; k < dep1len; k++) {
                units[i] = SemanticOps.getUnits(ds);
                df[i]= units[i].getDatumFormatterFactory().defaultFormatter();
                if ( dep1.rank()==1 ) {
                    labels[i] = dep1Units.createDatum(dep1.value(k)).toString();
                } else {
                    if ( SemanticOps.isBins(dep1) ) {
                        DatumRange dr= DataSetUtil.asDatumRange( this.dep0.slice(k) );
                        labels[i] = dr.toString();                     
                    } else {
                        labels[i] = dep1Units.createDatum(dep1.value(0,k)).toString() + "*";
                    }
                }
                i++;
            }
        }
        if ( this.ds.rank()==1 ) {
            labels[i]= (String) this.ds.property(QDataSet.LABEL);
            if ( labels[i]==null ) labels[i]= "data";
            units[i]= SemanticOps.getUnits(ds);
            df[i]= units[i].getDatumFormatterFactory().defaultFormatter();
            if ( !identifiesUnits( labels[i], units[i]) ) {
                labels[i]+= " (" +units[i] +")";
            }
            i++;
        }
        for (i = 0; i < units.length; i++) {
            if (units[i] == null) {
                units[i] = Units.dimensionless;
                df[i]= units[i].getDatumFormatterFactory().defaultFormatter();
            }
            if ( labels[i]==null ) {
                labels[i]= "col "+i;
            }
        }
    }
    
    /**
     * copied from AsciiTableDataSourceFormat.  See String.format.
     * @param df the string, such as "%f9.2"
     * @param u the units, which will provide the formatter when the string doesn't work.
     * @return a DatumFormatter for the column.
     * @see String#format(java.lang.String, java.lang.Object...) 
     */
    private DatumFormatter getDataFormatter( String df, Units u ) {
        try {
            if ( df.trim().isEmpty() ) {
                return u.getDatumFormatterFactory().defaultFormatter();
            }
            if ( !df.contains("%") ) df= "%"+df;
            //TODO: would be nice if we could verify formatter.  I had %f5.2 instead of %5.2f and it wasn't telling me.
            return new FormatStringFormatter( df, false );
        } catch ( UnknownFormatConversionException ex ) {
            logger.log(Level.FINER,null,ex);
            return u.getDatumFormatterFactory().defaultFormatter();            
        } catch ( RuntimeException ex ) {
            logger.log(Level.FINER,null,ex);
            return u.getDatumFormatterFactory().defaultFormatter();
        }
    }

    private boolean identifiesUnits( String s, Units u ) {
        return ( s.contains( String.valueOf(u) ) );
    }

    @Override
    public int getRowCount() {
        return ds.length();
    }

    @Override
    public int getColumnCount() {
        return colCount;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex < dep0Offset) {
            if ( this.dep0.rank()==2 ) {
                DatumRange dr= DataSetUtil.asDatumRange( this.dep0.slice(rowIndex) );
                return dr.toString();
            } else {
                Datum d= units[columnIndex].createDatum(this.dep0.value(rowIndex));
                return df[columnIndex].format( d,units[columnIndex] );
            }
        } else {
            if ( this.ds.rank()==1 ) {
                if ( wds.value(rowIndex)==0 ) {
                    return "fill ("+this.ds.value(rowIndex)+")";
                } else {
                    Datum d= units[columnIndex].createDatum(this.ds.value(rowIndex));
                    return df[columnIndex].format(d,units[columnIndex]);
                }
            } else if (this.ds.rank()==2 ) {
                if ( wds.value(rowIndex, columnIndex - dep0Offset)==0 ) {
                    return "fill ("+this.ds.value(rowIndex, columnIndex - dep0Offset)+")";
                } else {
                    double d= this.ds.value(rowIndex, columnIndex - dep0Offset);
                    if ( d>-1e31 ) {
                        Datum datum= units[columnIndex].createDatum(d);
                        return df[columnIndex].format(datum,units[columnIndex]);
                    } else {
                        return "fill ("+d+")";
                    }
                }
                
            } else {
                return "?????";
            }
        }
    }

    /**
     * this currently isn't used because there's a bug.
     * @return the table model.
     */
    public TableColumnModel getTableColumnModel() {
        DefaultTableColumnModel result = new DefaultTableColumnModel();

        QDataSet bds= (QDataSet) ds.property(QDataSet.BUNDLE_1);
        if ( bds!=null ) bds= DataSetOps.flattenBundleDescriptor(bds);
        
        for (int i = 0; i < colCount; i++) {
            TableColumn c = new TableColumn(i);
            Units u;
            if (i < dep0Offset) {
                c.setHeaderValue(dep0.property(QDataSet.LABEL));
                u= (Units) dep0.property(QDataSet.UNITS);
            } else {
                c.setHeaderValue(labels[i]);
                if ( bds==null ) {
                    u= (Units) ds.property(QDataSet.UNITS);
                } else {
                    u= (Units) bds.property(QDataSet.UNITS,i-dep0Offset);
                }
            }

            c.setPreferredWidth( ( u!=null && UnitsUtil.isTimeLocation(u) ) ? 150 : 80 );
            c.setMinWidth(  ( u!=null && UnitsUtil.isTimeLocation(u) ) ? 130 : 80 );
            result.addColumn( c );

        }

        return result;
    }

    @Override
    public String getColumnName( int i ) {
        if (i < dep0Offset) {
            return (String)dep0.property(QDataSet.LABEL);
        } else {
            return labels[i];
        }
    }

    public static void main(String[] args) {
        QDataSet ds = BundleBinsDemo.demo1();
        QDataSetTableModel m = new QDataSetTableModel(ds);
        JTable t = new JTable();
        t.setModel(m);
        t.setColumnModel(m.getTableColumnModel());

        JFrame frame = new JFrame();
        frame.getContentPane().add(t);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }
}
