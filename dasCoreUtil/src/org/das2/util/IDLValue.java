/* File: IDLValue.java
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

package org.das2.util;

import java.util.Arrays;

public class IDLValue {
    
    static int SCALAR = 1;
    static int ARRAY = 2;
    
    protected double[] aValue;
    
    protected double sValue;
    
    protected int type;
    
    IDLValue() {
    }
    
    public IDLValue(double a) {
        sValue= a;
        type= SCALAR;
    }
    
    /**
     * create an IDLValue representing this array.  The data is copied
     * as the object is constructed.
     * @param a 
     */
    public IDLValue(double[] a) {
        aValue= Arrays.copyOf(a,a.length);
        type= ARRAY;
    }
    
    public IDLValue IDLmultiply(IDLValue b) {
        IDLValue result= new IDLValue();
        if ((type==SCALAR) && (b.type==SCALAR)) {
            result.type=SCALAR;
            result.sValue=sValue*b.sValue;
        } else if ((type==ARRAY) && (b.type==SCALAR)) {
            result.type=ARRAY;
            result.aValue= new double[aValue.length];
            for (int i=0; i<aValue.length; i++) result.aValue[i]=aValue[i]*b.sValue;
        } else if ((type==SCALAR) && (b.type==ARRAY)) {
            result.type=ARRAY;
            result.aValue= new double[b.aValue.length];
            for (int i=0; i<b.aValue.length; i++) result.aValue[i]=sValue*b.aValue[i];
        } else if ((type==ARRAY) && (b.type==ARRAY)) {
            result.type=ARRAY;
            int length;
            if (aValue.length<b.aValue.length) length=aValue.length; else length=b.aValue.length;
            result.aValue= new double[length];
            for (int i=0; i<length; i++) result.aValue[i]=aValue[i]*b.aValue[i];
        }
        return result;
    }
    
    public IDLValue IDLdivide(IDLValue b) {
        IDLValue result= new IDLValue();
        if ((type==SCALAR) && (b.type==SCALAR)) {
            result.type=SCALAR;
            result.sValue=sValue/b.sValue;
        } else if ((type==ARRAY) && (b.type==SCALAR)) {
            result.type=ARRAY;
            result.aValue= new double[aValue.length];
            for (int i=0; i<aValue.length; i++) result.aValue[i]=aValue[i]/b.sValue;
        } else if ((type==SCALAR) && (b.type==ARRAY)) {
            result.type=ARRAY;
            result.aValue= new double[b.aValue.length];
            for (int i=0; i<b.aValue.length; i++) result.aValue[i]=sValue/b.aValue[i];
        } else if ((type==ARRAY) && (b.type==ARRAY)) {
            result.type=ARRAY;
            int length;
            if (aValue.length<b.aValue.length) length=aValue.length; else length=b.aValue.length;
            result.aValue= new double[length];
            for (int i=0; i<length; i++) result.aValue[i]=aValue[i]/b.aValue[i];
        }
        return result;
    }
    
    public IDLValue IDLadd(IDLValue b) {
        IDLValue result= new IDLValue();
        if ((type==SCALAR) && (b.type==SCALAR)) {
            result.type=SCALAR;
            result.sValue=sValue+b.sValue;
        } else if ((type==ARRAY) && (b.type==SCALAR)) {
            result.type=ARRAY;
            result.aValue= new double[aValue.length];
            for (int i=0; i<aValue.length; i++) result.aValue[i]=aValue[i]+b.sValue;
        } else if ((type==SCALAR) && (b.type==ARRAY)) {
            result.type=ARRAY;
            result.aValue= new double[b.aValue.length];
            for (int i=0; i<b.aValue.length; i++) result.aValue[i]=sValue+b.aValue[i];
        } else if ((type==ARRAY) && (b.type==ARRAY)) {
            result.type=ARRAY;
            int length;
            if (aValue.length<b.aValue.length) length=aValue.length; else length=b.aValue.length;
            result.aValue= new double[length];
            for (int i=0; i<length; i++) result.aValue[i]=aValue[i]+b.aValue[i];
        }
        return result;
    }
    
    public IDLValue IDLsubtract(IDLValue b) {
        IDLValue result= new IDLValue();
        if ((type==SCALAR) && (b.type==SCALAR)) {
            result.type=SCALAR;
            result.sValue=sValue-b.sValue;
        } else if ((type==ARRAY) && (b.type==SCALAR)) {
            result.type=ARRAY;
            result.aValue= new double[aValue.length];
            for (int i=0; i<aValue.length; i++) result.aValue[i]=aValue[i]-b.sValue;
        } else if ((type==SCALAR) && (b.type==ARRAY)) {
            result.type=ARRAY;
            result.aValue= new double[b.aValue.length];
            for (int i=0; i<b.aValue.length; i++) result.aValue[i]=sValue-b.aValue[i];
        } else if ((type==ARRAY) && (b.type==ARRAY)) {
            result.type=ARRAY;
            int length;
            if (aValue.length<b.aValue.length) length=aValue.length; else length=b.aValue.length;
            result.aValue= new double[length];
            for (int i=0; i<length; i++) result.aValue[i]=aValue[i]-b.aValue[i];
        }
        return result;
    }
    
    public IDLValue IDLexponeniate(IDLValue b) {
        IDLValue result= new IDLValue();
        if ((type==SCALAR) && (b.type==SCALAR)) {
            result.type=SCALAR;
            result.sValue=Math.pow(sValue,b.sValue);
        } else if ((type==ARRAY) && (b.type==SCALAR)) {
            result.type=ARRAY;
            result.aValue= new double[aValue.length];
            for (int i=0; i<aValue.length; i++) result.aValue[i]=Math.pow(aValue[i],b.sValue);
        } else if ((type==SCALAR) && (b.type==ARRAY)) {
            result.type=ARRAY;
            result.aValue= new double[b.aValue.length];
            for (int i=0; i<b.aValue.length; i++) result.aValue[i]=Math.pow(sValue, b.aValue[i]);
        } else if ((type==ARRAY) && (b.type==ARRAY)) {
            result.type=ARRAY;
            int length;
            if (aValue.length<b.aValue.length) length=aValue.length; else length=b.aValue.length;
            result.aValue= new double[length];
            for (int i=0; i<length; i++) result.aValue[i]= Math.pow(aValue[i],b.aValue[i]);
        }
        return result;
    }
    
    /**
     * return a copy of the data.
     * @return 
     */
    public double[] toArray() {
        return Arrays.copyOf(aValue,aValue.length);
    }
    
    public double toScalar() {
        return sValue;
    }
    
    @Override
    public String toString() {
        StringBuilder result= new StringBuilder();
        if (type==SCALAR) {
            result.append("").append( sValue);
        } else {
            result.append( "[" );
            int i;
            for (i=0; i<aValue.length-1; i++) {
                result.append(aValue[i]);
                result.append(",");
            }
            result.append(aValue[i]);
            result.append("]");
        }
        return result.toString();
    }
    
    public static IDLValue findgen(int length) {
        IDLValue result= new IDLValue();
        result.type= IDLValue.ARRAY;
        result.aValue= new double[length];
        for (int i=0; i<length; i++) {
            result.aValue[i]=i;
        }
        return result;
    }
    
    public static IDLValue alog10(IDLValue x) {
        if (x.type == IDLValue.SCALAR) {
            return new IDLValue(Math.log10(x.sValue));
        }
        else if (x.type == IDLValue.ARRAY) {
            double[] aValue = new double[x.aValue.length];
            for (int index = 0; index < x.aValue.length; index++) {
                aValue[index] = Math.log10(x.aValue[index]);
            }
            return new IDLValue(aValue);
        }
        else {
            throw new AssertionError("Unrecognized IDLValue type: " + x.type);
        }
    }
    
    public static IDLValue sin(IDLValue x) {
        if (x.type == IDLValue.SCALAR) {
            return new IDLValue(Math.sin(x.sValue));
        }
        else if (x.type == IDLValue.ARRAY) {
            double[] aValue = new double[x.aValue.length];
            for (int index = 0; index < x.aValue.length; index++) {
                aValue[index] = Math.sin(x.aValue[index]);
            }
            return new IDLValue(aValue);
        }
        else {
            throw new AssertionError("Unrecognized IDLValue type: " + x.type);
        }
    }
}

