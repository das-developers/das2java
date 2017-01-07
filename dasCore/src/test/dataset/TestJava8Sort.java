/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.dataset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author jbf
 */
public class TestJava8Sort {
    public static void main(String[]args) {
        List<Integer> sortMe= Arrays.asList( new Integer[] { 1,2,3,2 } );

        {
            List<Integer> sorted= new ArrayList(sortMe); // copy
            Collections.sort(sorted);
            for ( Integer i : sorted ) {
                System.err.println(i);
            }
        }
        
//        // uncomment the following Java 8 code to compare results.
//        {
//            List<Integer> sorted= new ArrayList(sortMe); // copy
//            sorted.sort( (x,y) -> x.compareTo(y) );
//            for ( Integer i : sorted ) {
//                System.err.println(i);
//            }
//        }
    }
}
