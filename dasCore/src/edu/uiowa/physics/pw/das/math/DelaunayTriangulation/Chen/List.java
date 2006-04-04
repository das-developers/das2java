/**
 * Voronoi diagram
 *
 * Copyright(c) 1998 Ping-Che Chen
 *
 * Linked List
 *
 */

package edu.uiowa.physics.pw.das.math.DelaunayTriangulation.Chen;

import java.lang.*;
import java.util.*;

public class List {
  
  static class Node {
    Object p;
    Node next;
    Node prev;
  }

  public static class ForwardEnumeration implements Enumeration {
    private Node current;

    ForwardEnumeration(Node n) {
      current = n;
    }

    public boolean hasMoreElements() {
      return current != null;
    }

    public Object nextElement() {
      if(current != null) {
	Object p = current.p;
	current = current.next;
	return p;
      }
      else {
	throw new NoSuchElementException();
      }
    }
  }

  public static class BackwardEnumeration implements Enumeration {
    private Node current;

    BackwardEnumeration(Node n) {
      current = n;
    }

    public boolean hasMoreElements() {
      return current != null;
    }
    
    public Object nextElement() {
      if(current != null) {
	Object p = current.p;
	current = current.prev;
	return p;
      }
      else {
	throw new NoSuchElementException();
      }
    }
  }

  private Node head, tail;

  public List() {
    head = tail = null;
  }

  public void insertHead(Object p) {
    Node n = new Node();
    n.p = p;
    n.next = head;
    n.prev = null;
    if(head != null)
      head.prev = n;
    head = n;
    if(tail == null) {
      tail = n;
    }
  }

  public void insertTail(Object p) {
    Node n = new Node();
    n.p = p;
    n.next = null;
    n.prev = tail;
    if(tail != null)
      tail.next = n;
    tail = n;
    if(head == null) {
      head = n;
    }
  }

  public boolean isEmpty() {
    return head == null;
  }

  public Object firstElement() throws NoSuchElementException {
    if(head == null)
      throw new NoSuchElementException();
    return head.p;
  }

  public Object lastElement() throws NoSuchElementException {
    if(tail == null)
      throw new NoSuchElementException();
    return tail.p;
  }

  public boolean contains(Object p) {
    Node n = head;
    while(n != null) {
      if(n.p == p)
	return true;
      else
	n = n.next;
    }

    return false;
  }

  public boolean remove(Object p) {
    Node n = head;
    while(n != null) {
      if(n.p == p) {
	if(n.prev != null) {
	  n.prev.next = n.next;
	}
	if(n.next != null) {
	  n.next.prev = n.prev;
	}
	return true;
      }
      else {
	n = n.next;
      }
    }

    return false;
  }

  public void removeAll() {
    head = tail = null;
  }

  public Enumeration elements() {
    return new ForwardEnumeration(head);
  }

  public Enumeration elementsR() {
    return new BackwardEnumeration(tail);
  }
}









