package edu.uiowa.physics.pw.das.math.DelaunayTriangulation.Chen;

/**
 * Voronoi diagram
 *
 * Copyright(c) 1998 Ping-Che Chen
 *
 * This program deals with the convex hull in the Voronoi problem.
 */

public class ConvexHull {

  public Coord[] points;

  private static class NewCoord {
    public Coord point;
    public double angle;
    public int group;
  }

  /** calculates (one of) the supporting line
   *  note: no optimized version, need O(N log N) time
   */
  public static Segment[] getSupportLine(final ConvexHull ch1, final ConvexHull ch2, ConvexHull newch) {
    NewCoord[] a = new NewCoord[ch1.points.length + ch2.points.length];
    Coord o = new Coord();
    int[] c = new int[ch1.points.length + ch2.points.length];

    if(ch1.points[0].y < ch2.points[0].y) {
      o = ch1.points[0];
    }
    else {
      o = ch2.points[0];
    }

    for(int i = 0; i < ch1.points.length; i++) {
      a[i] = new NewCoord();
      a[i].point = ch1.points[i];
      a[i].angle = angle(o, ch1.points[i]);
      a[i].group = 1;
    }

    for(int i = 0; i < ch2.points.length; i++) {
      a[i + ch1.points.length] = new NewCoord();
      a[i + ch1.points.length].point = ch2.points[i];
      a[i + ch1.points.length].angle = angle(o, ch2.points[i]);
      a[i + ch1.points.length].group = 2;
    }

    sort(a, 0, a.length);

    for(int i = 0; i < c.length; i++) {
      c[i] = -1;
    }

    c[0] = 0;
    c[1] = 1;
    int j = 2;
    for(int i = 2; i < c.length; i++) {
      while(ccw(a[c[j-2]].point, a[c[j-1]].point, a[i].point) < 0) {
	j--;
      }
      c[j++] = i;
    }

    Segment[] s = new Segment[2];

    j = 1;
    while(j < c.length && c[j] >= 0) {
      if(a[c[j]].group != a[c[j-1]].group) {
	break;
      }
      j++;
    }

    s[0] = new Segment();
    s[0].p1 = a[c[j-1]].point;
    s[0].p2 = a[c[j]].point;

    j++;
    while(j < c.length && c[j] >= 0) {
      if(a[c[j]].group != a[c[j-1]].group) {
	break;
      }
      j++;
    }

    if(j < c.length && c[j] >= 0) {
      s[1] = new Segment();
      s[1].p1 = a[c[j-1]].point;
      s[1].p2 = a[c[j]].point;
    }
    else if(a[c[j-1]].group != a[c[0]].group) {
      s[1] = new Segment();
      s[1].p1 = a[c[j-1]].point;
      s[1].p2 = a[c[0]].point;
    }
    else {
      s[1] = null;
    }

    if(s[1] != null && s[1].p1 == s[0].p1 && s[1].p2 == s[0].p2) {
      s[1] = null;
    }

    if(newch != null) {
      int i = 0;
      while(i < c.length && c[i] >= 0) i++;
      newch.points = new Coord[i];
      for(int k = 0; k < i; k++) {
	newch.points[k] = a[c[k]].point;
      }
    }

    return s;
  }

  private static int ccw(final Coord p1, final Coord p2, final Coord p3) {
    double a, b, x, y;

    x = p2.x - p1.x;
    y = p2.y - p1.y;
    a = p3.x - p1.x;
    b = p3.y - p1.y;

    double t = b * x - a * y;

    if(t > 0)
      return 1;
    else if(t < 0)
      return -1;
    else
      return 0;
  }

  /** compute the 'angle' which is not truly the angle */
  private static double angle(final Coord p1, final Coord p2) {
    double x, y;

    x = p2.x - p1.x;
    y = p2.y - p1.y;

    if(x == 0 && y == 0)
      return -1;

    double t = y / (Math.abs(x) + Math.abs(y));

    if(x < 0) {
      t = 2 - t;
    }
    else if(y < 0) {
      t = 4 + t;
    }

    return t;
  }

  /** sort */
  private static void sort(NewCoord[] s, int start, int end) {
    if(end - start > 1) {
      sort(s, start, (start + end) / 2);
      sort(s, (start + end) / 2, end);

      NewCoord[] t = new NewCoord[end - start];
      int i = start;
      int j = (start + end) / 2;
      int k = 0;
      while(i < (start + end) / 2 && j < end) {
	if(s[i].angle < s[j].angle) {
	  t[k++] = s[i++];
	}
	else {
	  t[k++] = s[j++];
	}
      }

      while(i < (start + end) / 2) {
	t[k++] = s[i++];
      }

      while(j < end) {
	t[k++] = s[j++];
      }

      for(k = start; k < end; k++) {
	s[k] = t[k - start];
      }
    }
  }
}
