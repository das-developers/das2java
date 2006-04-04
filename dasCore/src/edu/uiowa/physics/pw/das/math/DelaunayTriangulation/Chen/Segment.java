package edu.uiowa.physics.pw.das.math.DelaunayTriangulation.Chen;

/**
 * Voronoi diagram
 *
 * Copyright(c) 1998 Ping-Che Chen
 *
 * This program defines the segemnt.
 *
 */

public class Segment {

  public Coord p1, p2;
  public boolean i1, i2;    // true if not the end of a segment

  public String toString() {
    return p1 + "-" + p2;
  }

  public static Segment getMidNormal(final Segment l) {
    if(l.i1 || l.i2) {
      return null;
    }

    Segment s = new Segment();
    s.p1 = new Coord();
    s.p2 = new Coord();

    s.i1 = s.i2 = true;

    if(l.p1.y == l.p2.y) {
      s.p1.x = s.p2.x = (l.p1.x + l.p2.x) / 2;
      s.p1.y = l.p1.y - 1;
      s.p2.y = l.p1.y + 1;
    }
    else {
      double x = (l.p2.x + l.p1.x) / 2;
      double y = (l.p2.y + l.p1.y) / 2;
      double m = (l.p1.x - l.p2.x) / (l.p2.y - l.p1.y);
      double b = y - m * x;

      s.p1.x = l.p1.x;
      s.p2.x = l.p2.x;
      if(s.p1.x == s.p2.x) {
	s.p2.x += 1;
      }
      s.p1.y = m * s.p1.x + b;
      s.p2.y = m * s.p2.x + b;
    }

    return s;
  }

  public static Coord getIntersect(final Segment l1, final Segment l2) {
    double m1, m2, b1, b2;
    Coord result = new Coord();

    l1.normalize();
    l2.normalize();

    if(l1.p2.x == l1.p1.x) {
      if(l2.p2.x == l2.p1.x) {
	return null;
      }
      else {
	m2 = (l2.p2.y - l2.p1.y) / (l2.p2.x - l2.p1.x);
	b2 = l2.p1.y - m2 * l2.p1.x;
	result.x = l1.p1.x;
	result.y = l1.p1.x * m2 + b2;
      }
    }
    else if(l2.p2.x == l2.p1.x) {
      if(l1.p2.x == l1.p1.x) {
	return null;
      }
      else {
	m1 = (l1.p2.y - l1.p1.y) / (l1.p2.x - l1.p1.x);
	b1 = l1.p1.y - m1 * l1.p1.x;
	result.x = l2.p1.x;
	result.y = l2.p1.x * m1 + b1;
      }
    }
    else {
      m1 = (l1.p2.y - l1.p1.y) / (l1.p2.x - l1.p1.x);
      m2 = (l2.p2.y - l2.p1.y) / (l2.p2.x - l2.p1.x);

      b1 = l1.p1.y - m1 * l1.p1.x;
      b2 = l2.p1.y - m2 * l2.p1.x;

      if(m1 == m2) {
	return null;
      }

      result.x = (b2 - b1) / (m1 - m2);
      result.y = (m1 * b2 - m2 * b1) / (m1 - m2);
    }

    if(!l1.i1) {
      if(result.x < l1.p1.x)
	return null;
      if(result.x == l1.p1.x && result.y < l1.p1.y)
	return null;
    }

    if(!l1.i2) {
      if(result.x > l1.p2.x)
	return null;
      if(result.x == l1.p2.x && result.y > l1.p2.y)
	return null;
    }

    if(!l2.i1) {
      if(result.x < l2.p1.x)
	return null;
      if(result.x == l2.p1.x && result.y < l2.p1.y)
	return null;
    }

    if(!l2.i2) {
      if(result.x > l2.p2.x)
	return null;
      if(result.x == l2.p2.x && result.y > l2.p2.y)
	return null;
    }

    return result;
  }

  public void normalize() {
    if(p1.x > p2.x || (p1.x == p2.x && p1.y > p2.y)) {
      Coord t;
      t = p1;
      p1 = p2;
      p2 = t;
      boolean t2;
      t2 = i1;
      i1 = i2;
      i2 = t2;
    }
  }
}
