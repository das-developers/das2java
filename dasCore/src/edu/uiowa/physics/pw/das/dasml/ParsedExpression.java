/* File: ParsedExpression.java
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

package edu.uiowa.physics.pw.das.dasml;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class encapsulates a pre-parsed expression.
 *
 * @author Edward West
 */
public class ParsedExpression {
    
    private static final Pattern SIMPLE_NAME_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_-]*");
    
    private static final Pattern INT_PATTERN = Pattern.compile("-?(0|[1-9][0-9]*)");
    
    private static final Pattern FLOAT_PATTERN = Pattern.compile("-?[0-9]*(\\.[0-9]*)?([eE]-?[0-9]+)?");
    
    private static final Pattern PAREN_PATTERN = Pattern.compile("\\(([^\\(\\)])\\)");
    
    private static final Pattern EQUALITY_PATTERN = Pattern.compile("\\b(eq|ne)\\b");
    
    private static final Pattern COMPARISON_PATTERN = Pattern.compile("\\b(lt|le|gt|ge)\\b");
    
    private static final Pattern OR_PATTERN = Pattern.compile("\\bor\\b");
    
    private static final Pattern AND_PATTERN = Pattern.compile("\\band\\b");
    
    private static final Pattern NOT_PATTERN = Pattern.compile("\\Anot\\b");
    
    static final int ID_LOAD = 0;
    static final int ID_ALOAD = 1;
    static final int ID_STORE = 2;
    static final int ID_ASTORE = 3;
    static final int ID_ADD = 4;
    static final int ID_SUBTRACT = 5;
    static final int ID_MULTIPLY = 6;
    static final int ID_DIVIDE = 7;
    static final int ID_NEGATE = 8;
    static final int ID_EQ = 9;
    static final int ID_NE = 10;
    static final int ID_GT = 11;
    static final int ID_LT = 12;
    static final int ID_GE = 13;
    static final int ID_LE = 14;
    static final int ID_OR = 15;
    static final int ID_AND = 16;
    static final int ID_NOT = 17;
    
    private static class Op {
        static final Op LOAD = new Op(ID_LOAD);
        static final Op ALOAD = new Op(ID_ALOAD);
        static final Op STORE = new Op(ID_STORE);
        static final Op ADD = new Op(ID_ADD);
        static final Op SUBTRACT = new Op(ID_SUBTRACT);
        static final Op MULTIPLY = new Op(ID_MULTIPLY);
        static final Op DIVIDE = new Op(ID_DIVIDE);
        static final Op NEGATE = new Op(ID_NEGATE);
        static final Op EQ = new Op(ID_EQ);
        static final Op NE = new Op(ID_NE);
        static final Op GT = new Op(ID_GT);
        static final Op LT = new Op(ID_LT);
        static final Op GE = new Op(ID_GE);
        static final Op LE = new Op(ID_LE);
        static final Op OR = new Op(ID_OR);
        static final Op AND = new Op(ID_AND);
        static final Op NOT = new Op(ID_NOT);
        int id;
        private Op(int id) {this.id = id;}
    }

    private List list;
    
    private String expression;
    
    public ParsedExpression(String expression) throws ParsedExpressionException {
        this.expression = expression;
        list = new LinkedList();
        if (!parseExpression(expression, list)) throw new ParsedExpressionException("Invalid expression");
    }
   
    public String toString() {
        return expression;
    }
    
    private static boolean parseExpression(String s, List queue) {
        return parseOrExpression(s, queue);
    }
    
    private static boolean parseOrExpression(String expression, List queue) {
        if (expression.length() < 1) return false;
        Matcher matcher = OR_PATTERN.matcher(expression);
        while (matcher.find()) {
            String andExpression = expression.substring(matcher.end()).trim();
            String orExpression = expression.substring(0, matcher.start()).trim();
            List leftList = new LinkedList();
            List rightList = new LinkedList();
            if (parseAndExpression(andExpression, rightList)
                && parseOrExpression(orExpression, leftList)) {
                queue.addAll(leftList);
                queue.addAll(rightList);
                queue.add(Op.OR);
                return true;
            }
        }
        return parseAndExpression(expression, queue);
    }
    
    private static boolean parseAndExpression(String expression, List queue) {
        if (expression.length() < 1) return false;
        Matcher matcher = AND_PATTERN.matcher(expression);
        while (matcher.find()) {
            String notExpression = expression.substring(matcher.end()).trim();
            String andExpression = expression.substring(0, matcher.start()).trim();
            List leftList = new LinkedList();
            List rightList = new LinkedList();
            if (parseNotExpression(notExpression, rightList)
                && parseAndExpression(andExpression, leftList)) {
                queue.addAll(leftList);
                queue.addAll(rightList);
                queue.add(Op.AND);
                return true;
            }
        }
        return parseNotExpression(expression, queue);
    }
    
    private static boolean parseNotExpression(String expression, List queue) {
        if (expression.length() < 1) return false;
        Matcher matcher = NOT_PATTERN.matcher(expression);
        if (matcher.find()) {
            String equalityExpression = expression.substring(matcher.end()).trim();
            List list = new LinkedList();
            if (parseEqualityExpression(equalityExpression, list)) {
                queue.addAll(list);
                queue.add(Op.NOT);
                return true;
            }
            else return false;
        }
        return parseEqualityExpression(expression, queue);
    }
    
    private static boolean parseEqualityExpression(String expression, List queue) {
        if (expression.length() < 1) return false;
        Matcher matcher = EQUALITY_PATTERN.matcher(expression);
        while (matcher.find()) {
            String relationalExpression = expression.substring(matcher.end()).trim();
            String equalityExpression = expression.substring(0, matcher.start()).trim();
            List leftList = new LinkedList();
            List rightList = new LinkedList();
            if (parseRelationalExpression(relationalExpression, rightList)
                && parseEqualityExpression(equalityExpression, leftList)) {
                queue.addAll(leftList);
                queue.addAll(rightList);
                String op = matcher.group(1);
                if (op.equals("eq")) {
                    queue.add(Op.EQ);
                }
                else {
                    queue.add(Op.NE);
                }
                return true;
            }
        }
        return parseRelationalExpression(expression, queue);
    }
    
    private static boolean parseRelationalExpression(String expression, List queue) {
        if (expression.length() < 1) return false;
        Matcher matcher = COMPARISON_PATTERN.matcher(expression);
        while (matcher.find()) {
            String additiveExpression = expression.substring(matcher.end()).trim();
            String relationalExpression = expression.substring(0, matcher.start()).trim();
            List leftList = new LinkedList();
            List rightList = new LinkedList();
            if (parseAdditiveExpression(additiveExpression, rightList)
                && parseRelationalExpression(relationalExpression, leftList)) {
                queue.addAll(leftList);
                queue.addAll(rightList);
                String op = matcher.group(1);
                if (op.equals("lt")) {
                    queue.add(Op.LT);
                }
                else if (op.equals("le")) {
                    queue.add(Op.LE);
                }
                else if (op.equals("gt")) {
                    queue.add(Op.GT);
                }
                else {
                    queue.add(Op.GE);
                }
                return true;
            }
        }
        return parseAdditiveExpression(expression, queue);
    }
    
    private static boolean parseAdditiveExpression(String expression, List queue) {
        if (expression.length() < 1) return false;
        int index = Math.max(expression.lastIndexOf('+'),
        expression.lastIndexOf('-'));
        while (index >= 0) {
            String multiplicativeExpression = expression.substring(index + 1).trim();
            String additiveExpression = expression.substring(0, index).trim();
            List leftList = new LinkedList();
            List rightList = new LinkedList();
            if (parseMultiplicativeExpression(multiplicativeExpression, rightList)
                && parseAdditiveExpression(additiveExpression, leftList)) {
                queue.addAll(leftList);
                queue.addAll(rightList);
                queue.add(expression.charAt(index) == '+' ? Op.ADD : Op.SUBTRACT);
                return true;
            }
            index = Math.max(expression.lastIndexOf('+', index-1), expression.lastIndexOf('-', index-1));
        }
        return parseMultiplicativeExpression(expression, queue);
    }
    
    private static boolean parseMultiplicativeExpression(String expression, List queue) {
        if (expression.length() < 1) return false;
        int index = Math.max(expression.lastIndexOf('*'), expression.lastIndexOf('/'));
        while (index >= 0) {
            String unaryExpression = expression.substring(index + 1).trim();
            String multiplicativeExpression = expression.substring(0, index).trim();
            List leftList = new LinkedList();
            List rightList = new LinkedList();
            if (parseUnaryExpression(unaryExpression, rightList)
                && parseMultiplicativeExpression(multiplicativeExpression, leftList)) {
                queue.addAll(leftList);
                queue.addAll(rightList);
                queue.add(expression.charAt(index) == '*' ? Op.MULTIPLY : Op.DIVIDE);
                return true;
            }
        }
        return parseUnaryExpression(expression, queue);
    }
    
    private static boolean parseUnaryExpression(String expression, List queue) {
        return parseNegateExpression(expression, queue);
    }
    
    private static boolean parseNegateExpression(String expression, List queue) {
        if (expression.length() < 1) return false;
        if (expression.charAt(0) == '-') {
            List list = new LinkedList();
            boolean result = parseSimpleExpression(expression.substring(1).trim(), list);
            if (result) {
                queue.addAll(list);
                queue.add(Op.NEGATE);
                return true;
            }
            else return false;
        }
        return parseSimpleExpression(expression, queue);
    }
    
    private static boolean parseSimpleExpression(String expression, List queue) {
        if (expression.length() < 1) return false;
        if (expression.startsWith("(") && expression.endsWith(")")) {
            List list = new LinkedList();
            boolean result = parseExpression(expression.substring(1, expression.length()-1).trim(), list);
            if (result) {
                queue.addAll(list);
                return true;
            }
            else {
                return false;
            }
        }
        if (expression.startsWith("${") && expression.endsWith("}")) {
            List list = new LinkedList();
            boolean result = parseArrayAccess(expression.substring(2, expression.length() - 1).trim(), list);
            if (result) {
                queue.addAll(list);
            }
            return result;
        }
        Matcher matcher = INT_PATTERN.matcher(expression);
        if (matcher.matches()) {
            queue.add(new Integer(expression));
            return true;
        }
        matcher = FLOAT_PATTERN.matcher(expression);
        if (matcher.matches()) {
            queue.add(new Double(expression));
            return true;
        }
        if (expression.equals("true")) {
            queue.add(Boolean.TRUE);
            return true;
        }
        if (expression.equals("false")) {
            queue.add(Boolean.FALSE);
            return true;
        }
        return false;
    }
    
    private static boolean parseArrayAccess(String expression, List queue) {
        expression = expression.trim();
        if (expression.length() < 1) return false;
        if (SIMPLE_NAME_PATTERN.matcher(expression).matches()) {
            queue.add(null);
            queue.add(expression);
            queue.add(Op.LOAD);
            return true;
        }
        else if (expression.charAt(expression.length() - 1) == ']') {
            int index = expression.lastIndexOf('[');
            while (index >= 0) {
                int dotIndex = expression.lastIndexOf('.', index);
                String arrayAccess = expression.substring(0, dotIndex);
                String simpleName = expression.substring(dotIndex + 1, index).trim();
                String indexExpression = expression.substring(index + 1, expression.length() - 2).trim();
                if (!SIMPLE_NAME_PATTERN.matcher(simpleName).matches()) {
                    return false;
                }
                List leftList = new LinkedList();
                List rightList = new LinkedList();
                boolean rightResult = parseExpression(indexExpression, rightList);
                if (rightResult) {
                    boolean leftResult = parseArrayAccess(arrayAccess, leftList);
                    if (leftResult) {
                        queue.addAll(leftList);
                        queue.addAll(rightList);
                        queue.add(Op.ALOAD);
                        return true;
                    }
                }
            }
        }
        else {
            int index = expression.lastIndexOf('.');
            while (index > 0) {
                String arrayAccess = expression.substring(0, index);
                String simpleName = expression.substring(index + 1);
                if (!SIMPLE_NAME_PATTERN.matcher(simpleName).matches()) {
                    return false;
                }
                List leftList = new LinkedList();
                boolean leftResult = parseArrayAccess(arrayAccess, leftList);
                if (leftResult) {
                    queue.addAll(leftList);
                    queue.add(simpleName);
                    queue.add(Op.LOAD);
                    return true;
                }
            }
        }
        return false;
    }
    
    public Object evaluate(edu.uiowa.physics.pw.das.NameContext nc) throws ParsedExpressionException, edu.uiowa.physics.pw.das.DasPropertyException {
        try {
            return evaluate(this.list, nc);
        }
        catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof DataFormatException) {
                ParsedExpressionException pee = new ParsedExpressionException(t.getMessage());
                pee.initCause(t);
                throw pee;
            }
            throw new RuntimeException(ite);
        }
    }
    
    private static Object evaluate(List list, edu.uiowa.physics.pw.das.NameContext nc) throws ParsedExpressionException, InvocationTargetException, edu.uiowa.physics.pw.das.DasPropertyException {
        if (list.size() == 0) {
            throw new RuntimeException("empty expression");
        }
        if (list.size() == 1 && !(list.get(0) instanceof Op)) {
            return list.get(0);
        }
        List stack = new ArrayList(list.size());
        for (Iterator i = list.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof Op) {
                Op op = (Op)o;
                evaluate(stack, op.id, nc);
            }
            else {
                push(stack, o);
            }
        }
        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression: " + list);
        }
        return pop(stack);
    }
    
    private static void evaluate(List stack, int id, edu.uiowa.physics.pw.das.NameContext nc) throws InvocationTargetException, ParsedExpressionException, edu.uiowa.physics.pw.das.DasPropertyException {
        switch(id) {
            case ID_LOAD:
                load(stack, nc);
                break;
            case ID_ALOAD:
                aload(stack, nc);
                break;
            case ID_STORE:
                store(stack, nc);
                break;
            case ID_ASTORE:
                astore(stack, nc);
                break;
            case ID_NEGATE:
                negate(stack);
                break;
            case ID_ADD:
                add(stack);
                break;
            case ID_SUBTRACT:
                subtract(stack);
                break;
            case ID_MULTIPLY:
                multiply(stack);
                break;
            case ID_DIVIDE:
                divide(stack);
                break;
            case ID_EQ:
                eq(stack);
                break;
            case ID_NE:
                ne(stack);
                break;
            case ID_GT:
                gt(stack);
                break;
            case ID_LT:
                lt(stack);
                break;
            case ID_GE:
                ge(stack);
                break;
            case ID_LE:
                le(stack);
                break;
            case ID_OR:
                or(stack);
                break;
            case ID_AND:
                and(stack);
                break;
            default: throw new IllegalStateException();
        }
    }
    
    private static Object pop(List stack) {
        return stack.remove(stack.size() - 1);
    }
    
    private static Boolean popBoolean(List stack) {
        return (Boolean)stack.remove(stack.size() - 1);
    }
    
    private static String popString(List stack) {
        return (String)stack.remove(stack.size() - 1);
    }
    
    private static Integer popInteger(List stack) {
        return (Integer)stack.remove(stack.size() - 1);
    }
    
    private static Double popDouble(List stack) {
        return (Double)stack.remove(stack.size() - 1);
    }
    
    private static Number popNumber(List stack) {
        return (Number)stack.remove(stack.size() - 1);
    }
    
    private static Comparable popComparable(List stack) {
        return (Comparable)stack.remove(stack.size() - 1);
    }
    
    private static void push(List stack, Object o) {
        stack.add(o);
    }
    
    private static Class widest(Class c1, Class c2) {
        if (c1.getSuperclass() != Number.class || c2.getSuperclass() != Number.class) {
            throw new IllegalArgumentException("(" + c1.getName() + ", " + c2.getName());
        }
        else if (c1 == Double.class || c2 == Double.class) {
            return Double.class;
        }
        else if (c1 == Float.class || c2 == Float.class) {
            return Float.class;
        }
        else if (c1 == Long.class || c2 == Long.class) {
            return Long.class;
        }
        else if (c1 == Integer.class || c2 == Integer.class) {
            return Integer.class;
        }
        else if (c1 == Short.class || c2 == Short.class) {
            return Short.class;
        }
        else {
            return Byte.class;
        }
        
    }
    
    private static void load(List stack, edu.uiowa.physics.pw.das.NameContext nc) throws edu.uiowa.physics.pw.das.DasPropertyException, InvocationTargetException, ParsedExpressionException {
        String pname = popString(stack);
        Object source = pop(stack);
        Object value;
        if (source == null) {
            value = nc.get(pname);
        }
        else {
            value = nc.getPropertyValue(source, pname);
        }
        push(stack, value);
    }
    
    private static void aload(List stack, edu.uiowa.physics.pw.das.NameContext nc) throws edu.uiowa.physics.pw.das.DasPropertyException, InvocationTargetException, ParsedExpressionException {
        Integer index = popInteger(stack);
        String pname = popString(stack);
        Object source = pop(stack);
        push(stack, nc.getIndexedPropertyValue(source, pname, index.intValue()));
    }
    
    private static void store(List stack, edu.uiowa.physics.pw.das.NameContext nc) throws edu.uiowa.physics.pw.das.DasPropertyException, InvocationTargetException, ParsedExpressionException {
        Object value = pop(stack);
        String pname = popString(stack);
        Object dest = pop(stack);
        nc.setPropertyValue(dest, pname, value);
    }
    
    private static void astore(List stack, edu.uiowa.physics.pw.das.NameContext nc) throws edu.uiowa.physics.pw.das.DasPropertyException, InvocationTargetException, ParsedExpressionException {
        Object value = pop(stack);
        Integer index = popInteger(stack);
        String pname = popString(stack);
        Object dest = pop(stack);
        nc.setIndexedPropertyValue(dest, pname, index.intValue(), value);
    }
    
    private static void negate(List stack) {
        Double a = popDouble(stack);
        push(stack, new Double(-a.doubleValue()));
    }
    
    private static void add(List stack) {
        Number b = popNumber(stack);
        Number a = popNumber(stack);
        Class widest = widest(a.getClass(), b.getClass());
        Number result;
        if (widest == Double.class) {
            result = new Double(a.doubleValue() + b.doubleValue());
        }
        else if (widest == Float.class) {
            result = new Float(a.floatValue() + b.floatValue());
        }
        else if (widest == Long.class) {
            result = new Long(a.longValue() + b.longValue());
        }
        else if (widest == Integer.class) {
            result = new Integer(a.intValue() + b.intValue());
        }
        else if (widest == Short.class) {
            result = new Short((short)(a.shortValue() + b.shortValue()));
        }
        else {
            result = new Byte((byte)(a.byteValue() + b.byteValue()));
        }
        push(stack, result);
    }
    
    private static void subtract(List stack) {
        Number b = popNumber(stack);
        Number a = popNumber(stack);
        Class widest = widest(a.getClass(), b.getClass());
        Number result;
        if (widest == Double.class) {
            result = new Double(a.doubleValue() - b.doubleValue());
        }
        else if (widest == Float.class) {
            result = new Float(a.floatValue() - b.floatValue());
        }
        else if (widest == Long.class) {
            result = new Long(a.longValue() - b.longValue());
        }
        else if (widest == Integer.class) {
            result = new Integer(a.intValue() - b.intValue());
        }
        else if (widest == Short.class) {
            result = new Short((short)(a.shortValue() - b.shortValue()));
        }
        else {
            result = new Byte((byte)(a.byteValue() - b.byteValue()));
        }
        push(stack, result);
    }
    
    private static void multiply(List stack) {
        Number b = popNumber(stack);
        Number a = popNumber(stack);
        Class widest = widest(a.getClass(), b.getClass());
        Number result;
        if (widest == Double.class) {
            result = new Double(a.doubleValue() * b.doubleValue());
        }
        else if (widest == Float.class) {
            result = new Float(a.floatValue() * b.floatValue());
        }
        else if (widest == Long.class) {
            result = new Long(a.longValue() * b.longValue());
        }
        else if (widest == Integer.class) {
            result = new Integer(a.intValue() * b.intValue());
        }
        else if (widest == Short.class) {
            result = new Short((short)(a.shortValue() * b.shortValue()));
        }
        else {
            result = new Byte((byte)(a.byteValue() * b.byteValue()));
        }
        push(stack, result);
    }
    
    private static void divide(List stack) {
        Number b = popNumber(stack);
        Number a = popNumber(stack);
        Class widest = widest(a.getClass(), b.getClass());
        Number result;
        if (widest == Double.class) {
            result = new Double(a.doubleValue() / b.doubleValue());
        }
        else if (widest == Float.class) {
            result = new Float(a.floatValue() / b.floatValue());
        }
        else if (widest == Long.class) {
            result = new Long(a.longValue() / b.longValue());
        }
        else if (widest == Integer.class) {
            result = new Integer(a.intValue() / b.intValue());
        }
        else if (widest == Short.class) {
            result = new Short((short)(a.shortValue() / b.shortValue()));
        }
        else {
            result = new Byte((byte)(a.byteValue() / b.byteValue()));
        }
        push(stack, result);
    }
    
    private static void eq(List stack) {
        Object b = pop(stack);
        Object a = pop(stack);
        Boolean result = (a == b || (a != null && a.equals(b)) ? Boolean.TRUE :  Boolean.FALSE);
        push(stack, result);
    }
    
    private static void ne(List stack) {
        Object b = pop(stack);
        Object a = pop(stack);
        push(stack, Boolean.valueOf(a == b || (a != null && a.equals(b))));
    }
    
    private static void gt(List stack) {
        Comparable b = popComparable(stack);
        Comparable a = popComparable(stack);
        push(stack, Boolean.valueOf(a.compareTo(b) > 0));
    }
    
    private static void lt(List stack) {
        Comparable b = popComparable(stack);
        Comparable a = popComparable(stack);
        push(stack, Boolean.valueOf(a.compareTo(b) < 0));
    }
    
    private static void ge(List stack) {
        Comparable b = popComparable(stack);
        Comparable a = popComparable(stack);
        push(stack, Boolean.valueOf(a.compareTo(b) >= 0));
    }
    
    private static void le(List stack) {
        Comparable b = popComparable(stack);
        Comparable a = popComparable(stack);
        push(stack, Boolean.valueOf(a.compareTo(b) <= 0));
    }
    
    private static void or(List stack) {
        Boolean b = popBoolean(stack);
        Boolean a = popBoolean(stack);
        push(stack, Boolean.valueOf(a.booleanValue() || b.booleanValue()));
    }
    
    private static void and(List stack) {
        Boolean b = popBoolean(stack);
        Boolean a = popBoolean(stack);
        push(stack, Boolean.valueOf(a.booleanValue() && b.booleanValue()));
    }
    
    private static void not(List stack) {
        Boolean a = popBoolean(stack);
        push(stack, Boolean.valueOf(!a.booleanValue()));
    }
    
}
