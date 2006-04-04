/**
 * Voronoi diagram
 *
 * Copyright(c) 1998 Ping-Che Chen
 *
 * This is the main program.
 *
 */

package edu.uiowa.physics.pw.das.math.DelaunayTriangulation.Chen;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;

public class Main extends Applet {

  List vertices;
  int status = 1;
  int v = 0;
  Vertex[] diagram;
  String message;

  public void init() {
    vertices = new List();

    addMouseListener(new MyMouseListener());
  }

  public void paint(Graphics g) {
    g.setColor(Color.red);
    g.fillRect(0, 0, 10, 10);
    g.setColor(Color.green);
    g.fillRect(10, 0, 10, 10);
    g.setColor(Color.blue);
    g.fillRect(20, 0, 10, 10);
    g.setColor(Color.white);
    g.fillRect(30, 0, 10, 10);

    g.setColor(Color.black);
    Enumeration i = vertices.elements();
    while(i.hasMoreElements()) {
      Coord p = (Coord)i.nextElement();
      g.fillArc((int)p.x - 1, (int)(-p.y) - 1, 3, 3, 0, 360);
    }
    if(status == 2) {
      for(int j = 0; j < diagram.length; j++) {
	i = diagram[j].edges.elements();
	while(i.hasMoreElements()) {
	  VoronoiSegment s = (VoronoiSegment)i.nextElement();

	  if(s.floating) {
	    continue;
	  }

	  int x1, y1, x2, y2;
	  int rx1, ry1, rx2, ry2;

	  double dx = s.p2.x - s.p1.x;
	  double dy = s.p2.y - s.p1.y;

	  double m = dy / dx;
	  double b = s.p1.y - m * s.p1.x;

	  if(dx > 0) {
	    rx1 = 0;
	    rx2 = getSize().width;
	  }
	  else {
	    rx1 = getSize().width;
	    rx2 = 0;
	  }

	  if(dy > 0) {
	    ry1 = 0;
	    ry2 = getSize().height;
	  }
	  else {
	    ry1 = getSize().height;
	    ry2 = 0;
	  }

	  x1 = (int)s.p1.x;
	  y1 = -(int)s.p1.y;
	  x2 = (int)s.p2.x;
	  y2 = -(int)s.p2.y;

	  if(s.i1) {
	    if(dx == 0) {
	      y1 = ry1;
	    }
	    else {
	      x1 = rx1;
	      y1 = -(int)(m * rx1 + b);
	    }
	  }
	  if(s.i2) {
	    if(dx == 0) {
	      y2 = ry2;
	    }
	    else {
	      x2 = rx2;
	      y2 = -(int)(m * rx2 + b);
	    }
	  }

	  g.setColor(Color.black);
	  g.drawLine(x1, y1, x2, y2);
	}
      }
    }
    else if(status == 3) {
      for(int j = 0; j < diagram.length; j++) {
	i = diagram[j].edges.elements();
	while(i.hasMoreElements()) {
	  VoronoiSegment s = (VoronoiSegment)i.nextElement();
	  if(s.floating)
	    continue;

	  int x1 = (int)s.v1.x;
	  int y1 = -(int)s.v1.y;
	  int x2 = (int)s.v2.x;
	  int y2 = -(int)s.v2.y;

	  g.setColor(Color.black);
	  g.drawLine(x1, y1, x2, y2);
	}
      }
    }
  }

  public void update(Graphics g) {
    Color old = g.getColor();
    g.setColor(Color.lightGray);
    g.fillRect(0, 0, getSize().width, getSize().height);
    g.setColor(old);
    paint(g);
  }

  class MyMouseListener extends MouseAdapter {
    public void mouseClicked(MouseEvent e) {
      if(e.getX() < 30 && e.getX() >= 20 && e.getY() < 10) {
	status = 1;
	vertices.removeAll();
	v = 0;
	repaint();
      }
      else if(status < 2) {
	if(e.getX() < 20 && e.getX() >= 10 && e.getY() < 10) {
	  Random r = new Random();
	  for(int i = 0; i < 10; i++) {
	    int x = Math.abs(r.nextInt()) % getSize().width;
	    int y = Math.abs(r.nextInt()) % getSize().height;
	    Coord p = new Coord();
	    p.x = x;
	    p.y = -y;
	    vertices.insertTail(p);
	  }
	  v += 10;

	  repaint();
	}
	else if(e.getX() < 10 && e.getY() < 10) {
	  status++;

	  Coord[] t = new Coord[v];
	  Enumeration p = vertices.elements();
	  for(int i = 0; i < v; i++) {
	    t[i] = (Coord)p.nextElement();
	  }

	  Voronoi.g = getGraphics();
	  Voronoi.row = 1;
	  diagram = Voronoi.generate(t);

	  repaint();
	}
	else {
	  Coord p = new Coord();
	  p.x = e.getX();
	  p.y = -e.getY();

	  vertices.insertTail(p);
	  v++;

	  repaint();
	}
      }
      else if(e.getX() < 40 && e.getX() >= 30 && e.getY() < 10) {
	if(status == 2)
	  status = 3;
	else
	  status = 2;

	repaint();
      }
    }
  }

}
