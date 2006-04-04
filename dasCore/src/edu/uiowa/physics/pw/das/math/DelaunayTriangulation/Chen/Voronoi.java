/**
 * Voronoi diagram
 *
 * Copyright(c) 1998 Ping-Che Chen
 *
 * This programs contains the main part of the Voronoi diagram
 * generators.
 */

package edu.uiowa.physics.pw.das.math.DelaunayTriangulation.Chen;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.awt.*;

public class Voronoi {

  private static class CompoundLine {
    public Coord point;
    public Segment line;
  }

  public static Graphics g;
  public static int row;

  /** generate the Voronoi diagram */
  public static Vertex[] generate(final Coord[] points) {
    Vertex[] v = new Vertex[points.length];

    for(int i = 0; i < points.length; i++) {
      v[i] = new Vertex();
      v[i].x = points[i].x;
      v[i].y = points[i].y;
    }

    sort(v, 0, v.length);
    for(int i = 0; i < points.length; i++) {
      v[i].number = i;
    }

    internal(v, 0, v.length);

    return v;
  }

  /** the internal help-function */
  private static ConvexHull internal(Vertex[] points, int start, int end) {
    if(end - start > 1) {
      ConvexHull ch1 = internal(points, start, (start + end) / 2);
      ConvexHull ch2 = internal(points, (start + end) / 2, end);
      return merge(points, ch1, ch2, start, (start + end) / 2, end);
    }
    else {
      ConvexHull ch = new ConvexHull();
      ch.points = new Coord[1];
      ch.points[0] = points[start];
     
      return ch;
    }
  }

  /** merges two adjacent exclusive Voronoi diagrams */
  private static ConvexHull merge(Vertex[] points, ConvexHull ch1, ConvexHull ch2, int start, int mid, int end) { 
    ConvexHull ch = new ConvexHull();

    Segment[] supports = ConvexHull.getSupportLine(ch1, ch2, ch);
    Segment support = supports[0];
    Segment line = Segment.getMidNormal(support);

    List intersects;

    intersects = getAllIntersects(line, support);
    CompoundLine p = findFirstPoint(line, intersects, support);

    Vertex v1 = (Vertex)support.p1;
    Vertex v2 = (Vertex)support.p2;

    VoronoiSegment l = new VoronoiSegment(v1, v2);
    l.p1 = line.p1;
    l.p2 = line.p2;
    l.i1 = line.i1;
    l.i2 = line.i2;

    v1.edges.insertTail(l);
    v2.edges.insertTail(l);

    while(p != null) {
      if(supports[1] != null && support.p1 == supports[1].p2 && support.p2 == supports[1].p1) {
	break;
      }

      support = findSupportLine(p, v1, v2);
      line = Segment.getMidNormal(support);

      cutLine(line, p, support);
      cutOldLine(v1, v2, p);

      intersects = getAllIntersects(line, support);
      p = findNextPoint(line, intersects, support);

      v1 = (Vertex)support.p1;
      v2 = (Vertex)support.p2;

      l = new VoronoiSegment(v1, v2);
      l.p1 = line.p1;
      l.p2 = line.p2;
      l.i1 = line.i1;
      l.i2 = line.i2;

      v1.edges.insertTail(l);
      v2.edges.insertTail(l);
    }

    return ch;
  }

  private static void cutOldLine(Vertex v1, Vertex v2, CompoundLine p) {
    VoronoiSegment l = (VoronoiSegment)p.line;
    Vertex O, A;
    double dx, dy;

    if(v1 == l.v1 || v2 == l.v1) {
      O = l.v1;
    }
    else {
      O = l.v2;
    }

    if(v1 == O) {
      A = v2;
    }
    else {
      A = v1;
    }

    dx = l.p1.x - l.p2.x;
    dy = l.p1.y - l.p2.y;
    if(l.i1) {
      l.p1.x = p.point.x + dx;
      l.p1.y = p.point.y + dy;
    }
    if(l.i2) {
      l.p2.x = p.point.x - dx;
      l.p2.y = p.point.y - dy;
    }

    double dx1, dy1, dx2, dy2;

    dx1 = l.p1.x - O.x;
    dy1 = l.p1.y - O.y;
    dx2 = l.p1.x - A.x;
    dy2 = l.p1.y - A.y;

    Coord oldp = null;
    if(dx1 * dx1 + dy1 * dy1 < dx2 * dx2 + dy2 * dy2) {
      if(!l.i2) {
	oldp = l.p2;
      }
      l.p2 = p.point;
      l.i2 = false;
    }
    else {
      if(!l.i1) {
	oldp = l.p1;
      }
      l.p1 = p.point;
      l.i1 = false;
    }

    if(oldp != null && oldp != p.point) {
      removeOldLines(oldp, O);
    }
  }

  /** remove "floating" lines */
  private static void removeOldLines(Coord p, Vertex v) {
    Coord oldp = null;
    while(oldp != p) {
      oldp = p;
      Enumeration i = v.edges.elements();
      while(i.hasMoreElements()) {
	VoronoiSegment s = (VoronoiSegment)i.nextElement();
	if(s.floating)
	  continue;
	if(!s.i1 && s.p1 == p) {
	  s.floating = true;
	  if(!s.i2) {
	    p = s.p2;
	  }
	  break;
	}
	else if(!s.i2 && s.p2 == p) {
	  s.floating = true;
	  if(!s.i1) {
	    p = s.p1;
	  }
	  break;
	}
      }
    }
  }
  
  /** Cut the mid-normal line */
  private static Segment cutLine(final Segment line, CompoundLine p, Segment s) {
    double dx, dy;
    dx = line.p1.x - line.p2.x;
    dy = line.p1.y - line.p2.y;

    line.p1 = p.point;
    line.i1 = false;

    if(s.p1.x > s.p2.x) {
      if(dy < 0) {
	dy = -dy;
	dx = -dx;
      }

      line.p2 = new Coord();
      line.p2.x = line.p1.x - dx;
      line.p2.y = line.p1.y - dy;
    }
    else if(s.p1.x < s.p2.x) {
      if(dy < 0) {
	dy = -dy;
	dx = -dx;
      }
      line.p2 = new Coord();
      line.p2.x = line.p1.x + dx;
      line.p2.y = line.p1.y + dy;
    }
    else if(s.p1.y < s.p2.y) {
      if(dx < 0) {
	dx = -dx;
	dy = -dy;
      }
      line.p2 = new Coord();
      line.p2.x = line.p1.x - dx;
      line.p2.y = line.p1.y - dy;
    }
    else {
      if(dx < 0) {
	dx = -dx;
	dy = -dy;
      }
      line.p2 = new Coord();
      line.p2.x = line.p1.x + dx;
      line.p2.y = line.p1.y + dy;
    }

    return line;
  }

  /** find next supporting line */
  private static Segment findSupportLine(CompoundLine p, Vertex v1, Vertex v2) {
    VoronoiSegment s = (VoronoiSegment)p.line;
    Segment r = new Segment();

    if(s.v1 == v1) {
      r.p1 = s.v2;
      r.p2 = v2;
    }
    else if(s.v1 == v2) {
      r.p1 = v1;
      r.p2 = s.v2;
    }
    else if(s.v2 == v1) {
      r.p1 = s.v1;
      r.p2 = v2;
    }
    else {
      r.p1 = v1;
      r.p2 = s.v1;
    }

    return r;
  }

  /** get all intersects */
  private static List getAllIntersects(final Segment l, final Segment s) {
    List intersects = new List();
    Vertex v1 = (Vertex)s.p1;
    Vertex v2 = (Vertex)s.p2;
    Segment t = new Segment();
    t.p1 = l.p1;
    t.p2 = l.p2;
    t.i1 = l.i1;
    t.i2 = l.i2;

    Enumeration i;
    i = v1.edges.elements();
    while(i.hasMoreElements()) {
      VoronoiSegment ss = (VoronoiSegment)i.nextElement();
      Segment sss = new Segment();
      sss.p1 = ss.p1;
      sss.p2 = ss.p2;
      sss.i1 = ss.i1;
      sss.i2 = ss.i2;
      Coord p = Segment.getIntersect(sss, t);
      if(p != null) {
	CompoundLine ll = new CompoundLine();
	ll.point = p;
	ll.line = ss;
	intersects.insertTail(ll);
      }
    }
    i = v2.edges.elements();
    while(i.hasMoreElements()) {
      VoronoiSegment ss = (VoronoiSegment)i.nextElement();
      Segment sss = new Segment();
      sss.p1 = ss.p1;
      sss.p2 = ss.p2;
      sss.i1 = ss.i1;
      sss.i2 = ss.i2;
      Coord p = Segment.getIntersect(sss, t);
      if(p != null) {
	CompoundLine ll = new CompoundLine();
	ll.point = p;
	ll.line = ss;
	intersects.insertTail(ll);
      }
    }

    return intersects;
  } 

  /** find first point */
  private static CompoundLine findFirstPoint(Segment line, final List intersects, final Segment s) {
    CompoundLine m;
    Enumeration i = intersects.elements();
    if(!i.hasMoreElements())
      return null;

    m = (CompoundLine)i.nextElement();
    double dx = line.p1.x - line.p2.x;
    double dy = line.p1.y - line.p2.y;

    if(s.p1.x > s.p2.x) {
      // find max y
      while(i.hasMoreElements()) {
	CompoundLine k = (CompoundLine)i.nextElement();
	if(k.point.y > m.point.y) {
	  m = k;
	}
      }

      if(dy < 0) {
	dx = -dx;
	dy = -dy;
      }

      line.p1 = m.point;
      line.p2.x = m.point.x + dx;
      line.p2.y = m.point.y + dy;
      line.i1 = false;
    }
    else if(s.p1.x < s.p2.x) {
      // find min y
      while(i.hasMoreElements()) {
	CompoundLine k = (CompoundLine)i.nextElement();
	if(k.point.y < m.point.y) {
	  m = k;
	}
      }

      if(dy < 0) {
	dx = -dx;
	dy = -dy;
      }

      line.p1 = m.point;
      line.p2.x = m.point.x - dx;
      line.p2.y = m.point.y - dy;
      line.i1 = false;
    }
    else if(s.p1.y < s.p2.y) {
      // find max x
      while(i.hasMoreElements()) {
	CompoundLine k = (CompoundLine)i.nextElement();
	if(k.point.x > m.point.x) {
	  m = k;
	}
      }

      if(dx < 0) {
	dx = -dx;
	dy = -dy;
      }

      line.p1 = m.point;
      line.p2.x = m.point.x + dx;
      line.p2.y = m.point.y + dy;
      line.i1 = false;
    }
    else {
      // find min x
      while(i.hasMoreElements()) {
	CompoundLine k = (CompoundLine)i.nextElement();
	if(k.point.x < m.point.x) {
	  m = k;
	}
      }

      if(dx < 0) {
	dx = -dx;
	dy = -dy;
      }

      line.p1 = m.point;
      line.p2.x = m.point.x - dx;
      line.p2.y = m.point.y - dy;
      line.i1 = false;
    }

    return m;
  }

  /** find next point */
  private static CompoundLine findNextPoint(Segment line, final List intersects, final Segment s) {
    CompoundLine m;
    Enumeration i = intersects.elements();
    if(!i.hasMoreElements())
      return null;

    do {
      if(!i.hasMoreElements())
	return null;
      m = (CompoundLine)i.nextElement();
    } while((!line.i1 && m.point.equals(line.p1)) ||
	    (!line.i2 && m.point.equals(line.p2)));

    if(s.p1.x < s.p2.x) {
      // find min y
      while(i.hasMoreElements()) {
	CompoundLine k = (CompoundLine)i.nextElement();
	if((!line.i1 && k.point.equals(line.p1)) ||
	   (!line.i2 && k.point.equals(line.p2)))
	  continue;
	if(k.point.y < m.point.y) {
	  m = k;
	}
      }
    }
    else if(s.p1.x > s.p2.x) {
      // find max y
      while(i.hasMoreElements()) {
	CompoundLine k = (CompoundLine)i.nextElement();
	if((!line.i1 && k.point.equals(line.p1)) || 
	   (!line.i2 && k.point.equals(line.p2)))
	  continue;
	if(k.point.y > m.point.y) {
	  m = k;

	}
      }
    }
    else if(s.p1.y > s.p2.y) {
      // find min x
      while(i.hasMoreElements()) {
	CompoundLine k = (CompoundLine)i.nextElement();
	if((!line.i1 && k.point.equals(line.p1)) ||
	   (!line.i2 && k.point.equals(line.p2)))
	  continue;
	if(k.point.x < m.point.x) {
	  m = k;
	}
      }
    }
    else {
      // find max x
      while(i.hasMoreElements()) {
	CompoundLine k = (CompoundLine)i.nextElement();
	if((!line.i1 && k.point.equals(line.p1)) ||
	   (!line.i2 && k.point.equals(line.p2)))
	  continue;
	if(k.point.x > m.point.x) {
	  m = k;
	}
      }
    }

    line.p2 = m.point;
    line.i2 = false;

    return m;
  }

  /** compares two points by x and y */
  private static int compare(final Coord p1, final Coord p2) {
    if(p1.x < p2.x) {
      return 1;
    }
    else if(p1.x > p2.x) {
      return -1;
    }
    else if(p1.y < p2.y) {
      return 1;
    }
    else if(p1.y > p2.y) {
      return -1;
    }
    else {
      return 0;
    }
  }

  /** sorts an array of points (by merge sort) */
  private static void sort(Coord[] points, int start, int end) {
    if(end - start > 1) {
      sort(points, start, (start + end) / 2);
      sort(points, (start + end) / 2, end);

      Coord[] t = new Coord[end - start];
      int i = start;
      int j = (start + end) / 2;
      int k = 0;
      while(i < (start + end) / 2 && j < end) {
	if(compare(points[i], points[j]) > 0) {
	  t[k++] = points[i++];
	}
	else {
	  t[k++] = points[j++];
	}
      }

      while(i < (start + end) / 2) {
	t[k++] = points[i++];
      }

      while(j < end) {
	t[k++] = points[j++];
      }

      for(k = start; k < end; k++) {
	points[k] = t[k - start];
      }
    }
  }
}
