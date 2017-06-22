/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.TableUtil;
import org.das2.datum.Units;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.dataset.TableDataSetAdapter;
import org.das2.qds.ops.Ops;
import org.das2.qstream.SimpleStreamFormatter;

/**
 *
 * @author jbf
 */
public class FormatBenchmarks {

    public static void main(String[] args) throws ParseException, org.das2.qstream.StreamException, ParserConfigurationException, IOException {
        int nrec = 200000;
        MutablePropertyDataSet tags = (MutablePropertyDataSet) Ops.timegen("2003-09-09", "1 " + Units.days, nrec);
        tags.putProperty(QDataSet.NAME, "time");

        MutablePropertyDataSet ds = (MutablePropertyDataSet) Ops.randn(nrec, 3);
        ds.putProperty(QDataSet.DEPEND_0, tags);
        ds.putProperty(QDataSet.NAME, "B_GSM");

        MutablePropertyDataSet labels = (MutablePropertyDataSet) Ops.findgen(3);
        labels.putProperty(QDataSet.NAME, "dimLabels");
        ds.putProperty(QDataSet.DEPEND_1, labels);

        for (int j = 1; j < 2; j++) {
            boolean ascii = j == 0;

            SimpleStreamFormatter format = new SimpleStreamFormatter();

            for (int i = 0; i < 5; i++) {
                long t0 = System.currentTimeMillis();

                String filename = ascii ? "benchmark1.qds" : "benchmark1.binary.qds";
                format.format(ds, new FileOutputStream(System.getProperty("user.home") + "/temp/" + filename), ascii);

                System.err.println("Time to write " + nrec + " records: " + (System.currentTimeMillis() - t0));
            }

            TableDataSet tds = TableDataSetAdapter.create(ds);

            for (int i = 0; i < 5; i++) {
                long t0 = System.currentTimeMillis();

                if (ascii) {
                    TableUtil.dumpToAsciiStream(tds, new FileOutputStream(System.getProperty("user.home") + "/temp/benchmark1.d2s"));
                } else {
                    TableUtil.dumpToBinaryStream(tds, new FileOutputStream(System.getProperty("user.home") + "/temp/benchmark1.binary.d2s"));
                }

                System.err.println("Time to write " + nrec + " records: " + (System.currentTimeMillis() - t0));
            }

        }
    }
}