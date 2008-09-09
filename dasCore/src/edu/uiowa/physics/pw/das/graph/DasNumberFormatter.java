/* File: DasNumberFormatter.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/**
 *
 * @author  jessica
 */
package edu.uiowa.physics.pw.das.graph;

import java.text.DecimalFormat;

public class DasNumberFormatter {
    
    /** Creates a new instance of DecFormat */
    public DasNumberFormatter() {
    }
    
    /**
     * @param args the command line arguments
     */
    
    
    public static void main(String[] args) {
        DecimalFormat dFormatter = new DecimalFormat();
        DecimalFormat dF = new DecimalFormat();
        String result;
        double [] b = {123.123, 12.1234, 1.2345, 1234.12, 123.1};
        
        dFormatter = createDF(b);
        for(int i=0; i<b.length; i++){
            result = dFormatter.format(b[i]);
            org.das2.util.DasDie.println(result);
        }//for(int i=0; i<b.length; i++)
        
        double [] c = {0.0, 1.0, 20.0, 100.0, 1000.0, .0};
        dFormatter = createDF(c);
        for(int i=0; i<c.length; i++){
            result = dFormatter.format(c[i]);
            org.das2.util.DasDie.println(result);
        }//for(int i=0; i<c.length; i++)
        
        //To show all digits, set the significant digit count to zero. This does not 
        //affect parsing. 
        double [] d = {16578.786750, 20568.56580, 567581.67896, 69698.45875};
        dFormatter = createDF(d);
        int maxIntegerDigit = dFormatter.getMaximumIntegerDigits();
        
        for(int i=0; i<d.length; i++){
            result = dFormatter.format(d[i]);
            org.das2.util.DasDie.println(result);
        }//for(int i=0; i<d.length; i++)
       /*
        myPattern = "##0.##E0";
        *double [] d = {16578.786750, 20568.56580, 567581.67896, 69698.45875};
        16.579E3
        20.569E3
        567.58E3
        69.698E3
        */
        
        
    }//public static void main(String[] args)
    
    static DecimalFormat createDF(double [] a) {
        DecimalFormat decFormat = null;
        int len_a=a.length;
        int max_fracVal=0;
        int max_intVal=0;
        int max_Num=0;
        int testNum=0;
        int index;
        int len_fracDigits=0;
        int len_intDigits=0;
        String fracDigits, intDigits;
        String myPattern = null;
        //fracDigits is the String of fraction digits of given #
        //intDigits is a String of the integer digits of given #
        //Sting myPattern is pattern used to create DecimalFormat object to return
        String [] value = new String[a.length];
        String last = "";
        //used Strings since doubles not properly represented
        for (int i=0; i<len_a; i++) {
            value[i] = String.valueOf(a[i]);   //convert a[i] to String
            index = value[i].indexOf("."); //get index of "."
            fracDigits = value[i].substring(index +1); //fracDigits is String of fraction digits
            int indexE = fracDigits.indexOf("E");
            if (indexE!=-1) fracDigits=fracDigits.substring(0,indexE);
            
            len_fracDigits = fracDigits.length();   //get the length of String of fraction digits
            testNum = Integer.parseInt(fracDigits);
            //if testNum is larger than any prev #, then max_Num is assigned value of testNum
            if (testNum > max_Num)
                max_Num = testNum;
            
            
            //if length of current fracDigit is larger than max, set max to current length
            if(len_fracDigits > max_fracVal)
                max_fracVal=len_fracDigits;
            
            intDigits = value[i].substring(0, index);   //String of beginning integer digits of #
            len_intDigits = intDigits.length();   //get the length of String of integer digits
            
            //if length of current intDigit is larger than max, set max to current length
            if(len_intDigits > max_intVal)
                max_intVal=len_intDigits;
        }//for (int i=0; i<len_a; i++)
        
        //if length of the fractional part of number is less than 5
        if(max_fracVal < 5){
            while(max_fracVal > last.length()){
                last = last + 0;
            }//while
            myPattern = "###." + last;
            decFormat = new DecimalFormat(myPattern);
        }//if (max_fracVal < 5)
        
        //if the largest fractional digit in series is 0, then return pattern ###,
        //ex. 1.0 will be 1, 10.0 will be 10, it will drop unneeded decimal point and trailing 0
        if(max_Num == 0){
            myPattern = "###";
            decFormat = new DecimalFormat(myPattern);
        }//(max_Num == 0)
        
        //scientific notation
        int newMax = len_intDigits + len_fracDigits;

	if(max_intVal > 5){
           String subPattern = "";
            
            for(int k=0; k<=newMax; k++){
                subPattern = subPattern + '#';
                if(newMax==3)
                    subPattern = subPattern + '.';
            }//for(int k=0; k<==nexMax; k++)
           
            //myPattern = subPattern;
            myPattern = "###.####E0";
            decFormat = new DecimalFormat(myPattern);
            decFormat.setMaximumIntegerDigits(2);
	    decFormat.setMinimumIntegerDigits(2);
        }//if(max_intVal > 5)
        
        return decFormat;
    }//static DecimalFormat createDF (double [] a)
    
    
    
    static final double LOG_10 = Math.log(10);
    
    static DecimalFormat makeNewDF(double [] a){
        DecimalFormat df = new DecimalFormat();
        String pattern;
        int len_a = a.length;
        int intDigit, fracDigit, first;
        double intBase, fracBase, digit, second;
        
        for(int k=0; k<len_a; k++){
            first = (int)a[k];
            second = a[k] - first;
        }//for
        return df;
    }
    
    //gives the power of number to base 10
    static double baseTen(double a){
        double base;
        return base = Math.log(a)/LOG_10;
    }//static baseLogTen
    
    
    
}
