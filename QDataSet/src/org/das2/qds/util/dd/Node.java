package org.das2.qds.util.dd;

import java.util.*;

enum Id { ASSIGN, PLUS, IDENT, DOUBLE}

public class Node {
    
    private Id id;

    private List<Node> children = new ArrayList<Node>();

    public Node (Id _id) {
	id = _id;
    }

    public void add(Node child) {
	children.add(child);
    }

    public int numberOfChildren() {
	return children.size();
    }

    public Node getChild(int i) {
	return children.get(i);
    }

    public Node getChild(int i, int n) {
	if (n!=children.size()) 
	    throw new IllegalArgumentException ();
	return children.get(i);
    }

    public Id getId() {
	return id;
    }


    public void printHead(String prefix, String suffix) {
	System.out.print(prefix+id);
	System.out.println(" "+suffix);
    }

    public void printChildren(String prefix) {
	if (children != null) {
	    for (Node n : children) {
		if (n != null) {
		    n.print(prefix + " ");
		}
	    }
	}
    }

    public void print(String prefix, String suffix) {
	printHead(prefix, suffix);
	printChildren(prefix);
    }


    public void print(String prefix) {
	print(prefix, "");
    }
}


class IdentifierNode extends Node {
    private String name;

    public IdentifierNode(String _name) {
	super(Id.IDENT);
	name = _name;
    }

    public String getName() {
	return name;
    }
    public void print(String prefix) {
	print(prefix, name);
    }
}

class DoubleNode extends Node {
    private double value;

    public DoubleNode(double _value) {
	super(Id.DOUBLE);
	value = _value;
    }

    public double getValue() {
	return value;
    }

    public void print(String prefix) {
	print(prefix, value+"");
    }
}

