/* File: CommandBlock.java
 * Copyright (C) 2002-2003 University of Iowa
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;

public class CommandBlock {
    
    private static final int NONE = 0;
    
    private static final int SET = 1;
    
    private static final int UPDATE = 2;
    
    private static final int IF = 4;
    
    private static final int ELSEIF = 8;
    
    private static final int INVOKE = 16;
    
    private static final int ALERT = 32;
    
    ArrayList commandList;
    
    CommandBlock() {
        commandList = new ArrayList();
    }
    
    CommandBlock(Element element, FormBase form) {
        
        commandList = new ArrayList();
        NodeList children = element.getChildNodes();
        int childCount = children.getLength();
        
        for (int index = 0; index < childCount; index++) {
            Node node = children.item(index);
            if (node instanceof Element) {
                String tagName = node.getNodeName();
                if (tagName.equals("set")) {
                    addCommand(processSetElement(form, (Element)node));
                }
                else if (tagName.equals("if")) {
                    addCommand(processIfElement(form, (Element)node));
                }
                else if (tagName.equals("elseif")) {
                    addCommand(processElseifElement(form, (Element)node));
                }
                else if (tagName.equals("else")) {
                    addCommand(processElseElement(form, (Element)node));
                }
                else if (tagName.equals("invoke")) {
                    addCommand(processInvokeElement(form, (Element)node));
                }
            }
        }
    }
    
    private Command processSetElement(FormBase form, Element element) {
        String property = element.getAttribute("property");
        String value = element.getAttribute("value");
        
        if (!edu.uiowa.physics.pw.das.NameContext.QUALIFIED_NAME.matcher(property).matches()) {
            throw new IllegalArgumentException("property attribute must be a valid identifier: <set property=\""+property+"\" ...");
        }
        
        return new SetCommand(property, value);
    }
    
    private Command processIfElement(FormBase form, Element element) {
        String test = element.getAttribute("test");
        return new IfCommand(test, element, form);
    }
    
    private Command processElseifElement(FormBase form, Element element) {
        String test = element.getAttribute("test");
        return new ElseIfCommand(test, element, form);
    }
    
    private Command processElseElement(FormBase form, Element element) {
        return new ElseCommand(element, form);
    }
    
    private Command processInvokeElement(FormBase form, Element element) {
        String target = element.getAttribute("method");
        String args = element.getAttribute("args");
        String[] argsArray;
        if (args.trim().length() == 0) argsArray = new String[0];
        else argsArray = args.split(",");
        for (int i = 0; i < argsArray.length; i++) {
            argsArray[i] = argsArray[i].trim();
        }
        return new InvokeCommand(target, argsArray);
    }
    
    public void execute(FormBase form) throws edu.uiowa.physics.pw.das.DasException, DataFormatException, ParsedExpressionException, InvocationTargetException {
        Iterator iterator = commandList.iterator();
        int lastCommand = -1;
        boolean shouldSkip = false;
        Object value;
        while (iterator.hasNext()) {
            Command command = (Command)iterator.next();
            if ((command instanceof ElseIfCommand || command instanceof ElseCommand) && shouldSkip) {
                continue;
            }
            command.execute(form);
            if (command instanceof IfCommand) {
                shouldSkip = ((IfCommand)command).getShouldSkip();
            }
        }
        
    }
    
    public void appendDOMElements(Element element) {
        Document document = element.getOwnerDocument();
        for (Iterator i = commandList.iterator(); i.hasNext();) {
            element.appendChild(((Command)i.next()).getDOMElement(document));
        }
    }
    
    void insertCommand(CommandBlock.Command command, int index) {
        if (command.getParent() != null && command.getParent() != this) {
            command.getParent().commandList.remove(command);
        }
        command.setParent(this);
        if (index == -1) {
            commandList.add(command);
        }
        else {
            commandList.add(index, command);
        }
    }
    
    void addCommand(CommandBlock.Command command) {
        if (command.getParent() != null && command.getParent() != this) {
            command.getParent().commandList.remove(command);
        }
        command.setParent(this);
        commandList.add(command);
    }
    
    void removeCommand(int index) {
        commandList.remove(index);
    }
    
    void removeCommand(CommandBlock.Command command) {
        commandList.remove(command);
    }
    
    int indexOf(CommandBlock.Command command) {
        return commandList.indexOf(command);
    }
    
    static class SetCommand implements Command {
        String id;
        String value;
        CommandBlock parent;
        
        SetCommand(String id, String value) {
            this.id = id;
            this.value = value;
        }
        
        public void execute(FormBase form)throws edu.uiowa.physics.pw.das.DasException, DataFormatException, ParsedExpressionException, InvocationTargetException, edu.uiowa.physics.pw.das.DasNameException {
            form.getDasApplication().getNameContext().set(id, value);
        }
        
        public Element getDOMElement(Document document) {
            Element element = document.createElement("set");
            element.setAttribute("property", id);
            element.setAttribute("value", value);
            return element;
        }
        
        public CommandBlock getParent() {
            return parent;
        }
        
        public void setParent(CommandBlock parent) {
            this.parent = parent;
        }
        
        public String toString() {
            return "SET " + id + " = " + value;
        }
        
    }
    
    static class InvokeCommand implements Command {
        
        String target;
        String[] args;
        CommandBlock parent;
        
        public InvokeCommand(String target, String[] args) {
            this.target = target;
            this.args = args;
        }
        
        public void execute(FormBase form)throws edu.uiowa.physics.pw.das.DasException, DataFormatException, ParsedExpressionException, InvocationTargetException {
            form.invoke(target, args);
        }
        
        public Element getDOMElement(Document document) {
            Element element = document.createElement("invoke");
            element.setAttribute("method", target);
            if (args != null && args.length > 0) {
                String argsString = java.util.Arrays.asList(args).toString();
                argsString = argsString.substring(0, argsString.length() - 1);
                element.setAttribute("args", argsString);
            }
            return element;
        }
        
        public void setParent(CommandBlock parent) {
            this.parent = parent;
        }
        
        public CommandBlock getParent() {
            return parent;
        }
        
        public String toString() {
            return "INVOKE " + target + (args == null ? "[]" : Arrays.asList(args).toString());
        }
        
    }
    
    static class IfCommand extends BlockCommand {
        String test;
        boolean shouldSkip;
        
        public IfCommand(String test) {
            super();
            this.test = test;
        }
        
        public IfCommand(String test, Element element, FormBase form) {
            super(element, form);
            this.test = test;
        }
        
        public boolean getShouldSkip() {
            return shouldSkip;
        }
        
        public void execute(FormBase form)throws edu.uiowa.physics.pw.das.DasException, DataFormatException, ParsedExpressionException, InvocationTargetException {
            Matcher refMatcher = edu.uiowa.physics.pw.das.NameContext.refPattern.matcher(test);
            Object value;
            if (refMatcher.matches()) {
                value = form.getDasApplication().getNameContext().get(refMatcher.group(1));
            }
            else {
                value = form.getDasApplication().getNameContext().parseValue(test, boolean.class);
            }
            Boolean bool;
            if (value instanceof Boolean) {
                bool = (Boolean)value;
            }
            else {
                throw new DataFormatException(value + " is not a boolean");
            }
            if (bool.booleanValue()) {
                super.execute(form);
            }
            shouldSkip = bool.booleanValue();
        }
        
        public Element getDOMElement(Document document) {
            Element element = document.createElement("if");
            element.setAttribute("test", test);
            appendDOMElements(element);
            return element;
        }
        
        public String toString() {
            return "IF " + test;
        }
        
    }
    
    static class ElseIfCommand extends IfCommand {
        ElseIfCommand(String test) {
            super(test);
        }
        
        ElseIfCommand(String test, Element element, FormBase form) {
            super(test, element, form);
        }
        
        public Element getDOMElement(Document document) {
            Element element = document.createElement("elseif");
            element.setAttribute("test", test);
            appendDOMElements(element);
            return element;
        }
        
        public String toString() {
            return "ELSEIF " + test;
        }
    }
    
    static class ElseCommand extends IfCommand {
        ElseCommand() {
            super("true");
        }
        
        ElseCommand(Element element, FormBase form) {
            super("true", element, form);
        }
        
        public Element getDOMElement(Document document) {
            Element element = document.createElement("else");
            appendDOMElements(element);
            return element;
        }
        
        public String toString() {
            return "ELSE";
        }
    }
    
    static interface Command {
        CommandBlock getParent();
        void setParent(CommandBlock parent);
        void execute(FormBase form)throws edu.uiowa.physics.pw.das.DasException, DataFormatException, ParsedExpressionException, InvocationTargetException;
        Element getDOMElement(Document document);
    }
    
    static abstract class BlockCommand extends CommandBlock implements Command {
        CommandBlock parent;
        public BlockCommand() {
            super();
        }
        public BlockCommand(Element element, FormBase form) {
            super(element, form);
        }
        public void setParent(CommandBlock parent) {
            this.parent = parent;
        }
        public CommandBlock getParent() {
            return parent;
        }
    }
    
    static class Identifier {
        
        public String text;
        
        public Identifier(String t) { text = t; }
        
        public String toString() {
            return text;
        }
        
    }
    
}

