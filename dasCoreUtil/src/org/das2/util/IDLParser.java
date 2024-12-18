/* File: IDLParser.java
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

import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;
/**
 * This was created originally to evaluate expressions found in das2server dsdf files that would
 * describe column locations, which would be expressions like "10^(findgen(3)/3.3)"
 * @author  jbf
 */
public class IDLParser {
    
    public static final int EXPR=1;
    public static final int EXPR_LIST=2;
    public static final int FACTOR=3;
    public static final int TERM=4;
    public static final int NUMBER=5;
    public static final int NOPARSER=6;

    /** Creates a new instance of idlParser */
    public IDLParser() {
    }
    
    public String[] IDLTokenizer(String s) {
        String delimiters=" \t[,]()^*/+-";
        StringTokenizer st = new StringTokenizer(s, delimiters, true);
        int countTokens=st.countTokens();
        String[] tokens=new String[countTokens];
        for (int i=0; i<countTokens; i++)
            tokens[i]= st.nextToken();
        
        int nullcount = 0;
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals(" ") || tokens[i].equals("\t")) {
                tokens[i] = null;
                nullcount++;
            } else if ((tokens[i].endsWith("e") || tokens[i].endsWith("E")) && tokens[i+1].endsWith("-")) {
                tokens[i] = tokens[i] + tokens[i+1] + tokens[i+2];
                tokens[i+1] = null;
                tokens[i+2] = null;
                i+=2;
                nullcount+=2;
            }
        }
        if (nullcount > 0)
        {
            String[] temp = new String[tokens.length-nullcount];
            int tIndex = 0;
            for (int i = 0; i < tokens.length; i++)
            {
                if (tokens[i] == null) continue;
                temp[tIndex] = tokens[i];
                tIndex++;
            }
            tokens = temp;
        }
        return tokens;
    } 
    
    public double parseIDLScalar(String s) {
        String[] tokens= IDLTokenizer(s);
        IDLValue expr= parseIDLArrayTokens(tokens,EXPR);
        if (expr == null) return Double.NaN;
        else return expr.toScalar();
    }
    public double[] parseIDLArray(String s) {
        String[] tokens= IDLTokenizer(s);
        IDLValue expr= parseIDLArrayTokens(tokens,EXPR);
        if (expr == null) return null;
        else return expr.toArray();
    }
    
    private IDLValue parseIDLExprList(String[] tokens) {
        int ileft= 0;
        int nleft= 0;
        int iright=0;
        int itok;
        
        Vector exprs= new Vector();
        
        ileft=1;
        for (itok=1; itok<tokens.length; itok++) {
            if ((tokens[itok].equals(",") && nleft==0) ||
            (tokens[itok].equals("]") && nleft==0)) {
                iright=itok-1;
                String[] expr= new String[iright-ileft+1];
                for (int i=0; i<expr.length; i++ )
                    expr[i]= tokens[i+ileft];
                IDLValue expr1= parseIDLArrayTokens(expr,EXPR);
                exprs.add(expr1);
                ileft=itok+1;
            } else if (tokens[itok].equals("(")) {
                nleft++;
            } else if (tokens[itok].equals(")")) {
                nleft--;
            } else if (tokens[itok].equals("[")) {
                nleft++;
            } else if (tokens[itok].equals("]")) {
                nleft--;
            }
        }
        
        IDLValue result= new IDLValue();
        result.type= IDLValue.ARRAY;
        Vector aValue= new Vector();
        
        for (int iexpr=0; iexpr<exprs.size(); iexpr++) {
            IDLValue expr= (IDLValue) exprs.get(iexpr);
            if (expr.type==IDLValue.SCALAR) {
                aValue.add(expr.sValue);  // ==null?
            } else {
                for (int i=0; i<expr.aValue.length; i++)
                    aValue.add(expr.aValue[i]);
            }
        }
        result.aValue= new double[aValue.size()];
        for (int i=0; i<aValue.size(); i++)
            result.aValue[i]= ((Double) aValue.get(i)).doubleValue();
        
        return result;
    }
    
    public IDLValue parseIDLArrayTokens(String[] tokens, int type) {
        IDLValue expr1=null,result=null;
        int ileft;
        int iright;
        
        if (tokens.length==0) {
            return null;
        }
        
        if (tokens.length==1) {
            expr1= new IDLValue(Double.parseDouble(tokens[0]));
            return expr1;
        }
        
        String [] ops=null;
        int next_parser;
        
        if (type==EXPR) {
            ops= new String[2];
            ops[0]="+";
            ops[1]="-";
            next_parser=TERM;
        } else if (type==TERM) {
            ops= new String[2];
            ops[0]="*";
            ops[1]="/";
            next_parser=FACTOR;
        } else if (type==FACTOR) {
            ops= new String[1];
            ops[0]="^";
            next_parser=NUMBER;
            if (tokens[0].equals("-")) {
                String [] expr= new String[tokens.length-1];
                for (int i=0; i<expr.length; i++) expr[i]=tokens[i+1];
                expr1= parseIDLArrayTokens(expr,FACTOR);
                result= expr1.IDLmultiply(new IDLValue(-1));
                return result;
            }
        } else {  // (type==NUMBER) {
            next_parser=NOPARSER;
            
            if (tokens[0].equals("(") && tokens[tokens.length-1].equals(")")) {
                ileft=1;
                iright=tokens.length-2;
                String [] expr= new String[iright-ileft+1];
                for (int i=0; i<expr.length; i++) expr[i]=tokens[i+ileft];
                expr1= parseIDLArrayTokens(expr,EXPR);
                return expr1;
            } else if (tokens[0].equals("[") && tokens[tokens.length-1].equals("]")) {
                expr1= parseIDLExprList(tokens);
                return expr1;
            } 

            else if (tokens[0].equalsIgnoreCase("findgen") || tokens[0].equalsIgnoreCase("dindgen")) {
                String [] expr= new String[tokens.length-3];
                for (int i=0; i<expr.length; i++) expr[i]=tokens[i+2];
                expr1= parseIDLArrayTokens(expr,EXPR);
                if (expr1.type!=IDLValue.SCALAR) {
                    Logger.getLogger("das2.anon").info("Syntax error in findgen argument");
                    System.exit(-1);
                } else {
                    expr1= IDLValue.findgen((int)expr1.sValue);
                }
            }
            else if (tokens[0].equalsIgnoreCase("alog10")) {
                String [] expr = new String[tokens.length - 3];
                for (int i = 0; i < expr.length; i++) expr[i] = tokens[i+2];
                expr1 = parseIDLArrayTokens(expr, EXPR);
                expr1 = IDLValue.alog10(expr1);
            }
            else if ( tokens[0].equalsIgnoreCase("sin")) {
                String [] expr = new String[tokens.length - 3];
                for (int i = 0; i < expr.length; i++) expr[i] = tokens[i+2];
                expr1 = parseIDLArrayTokens(expr, EXPR);
                expr1 = IDLValue.sin(expr1);
            }
            else {
                
                return null;
            }
            return expr1;
        }
        
        int ileftop=9999;
        String leftop="";
        int nleft_paren=0;
        int nleft_bracket=0;
        
        for (int iop=0; iop<ops.length; iop++) {
            for (int itok=0; itok<tokens.length; itok++) {
                if (tokens[itok].equals("[")) nleft_bracket++;
                if (tokens[itok].equals("(")) nleft_paren++;
                if (tokens[itok].equals("]")) nleft_bracket--;
                if (tokens[itok].equals(")")) nleft_paren--;
                if (tokens[itok].equals(ops[iop]) && itok!=0) {
                    if ((iop<ileftop) && (nleft_paren==0) && (nleft_bracket==0)) {
                        ileftop= itok;
                        leftop= ops[iop];
                    }
                }
            }
        }
        
        if (ileftop==9999) {   // no operator found
            result= parseIDLArrayTokens(tokens,next_parser);
        } else {
            String [] expr= new String[ileftop];
            for (int i=0; i<ileftop; i++) expr[i]=tokens[i];
            expr1= parseIDLArrayTokens(expr,next_parser);
            expr= new String[tokens.length-ileftop-1];
            for (int i=0; i<expr.length; i++) expr[i]=tokens[i+ileftop+1];
            IDLValue expr2= parseIDLArrayTokens(expr,type);
            
            if (expr1 == null || expr2 == null) {
                if (next_parser == NOPARSER) result= null;
                else result= parseIDLArrayTokens(tokens, next_parser);
           
            } else if (leftop.equals("+")) {
                result= expr1.IDLadd(expr2);
            } else if (leftop.equals("-")) {
                result= expr1.IDLsubtract(expr2);
            } else if (leftop.equals("*")) {
                result= expr1.IDLmultiply(expr2);
            } else if (leftop.equals("/")) {
                result= expr1.IDLdivide(expr2);
            } else if (leftop.equals("^")) {
                result= expr1.IDLexponeniate(expr2);
            }
        }
        
        return result;
        
    }
    
}
