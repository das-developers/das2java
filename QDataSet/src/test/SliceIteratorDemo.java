/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import org.das2.qds.QDataSet;
import org.das2.qds.QubeDataSetIterator;
import org.das2.qds.ops.Ops;

/**
 *
 * @author jbf
 */
public class SliceIteratorDemo {

    public static void main(String[] args) {
        System.err.println("test1: ");
        {
            QDataSet ds = Ops.findgen(10, 5);

            QubeDataSetIterator it = QubeDataSetIterator.sliceIterator(ds, 3);

            while (it.hasNext()) {
                it.next();
                System.err.print(" " + it.getValue(ds));
            }
        }
        System.err.println("");

        System.err.println("test2: ");
        {
            QDataSet ds = Ops.findgen(10, 5, 2);

            QubeDataSetIterator it = QubeDataSetIterator.sliceIterator(ds, 3);

            while (it.hasNext()) {
                it.next();
                System.err.print(" " + it.getValue(ds));
            }
        }
        System.err.println("");
    }
}
