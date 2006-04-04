/**
 * Voronoi Diagram
 *
 * Copyright(c) 1998 Ping-Che Chen
 *
 * This is the definition of the coordinate system.
 *
 */

package edu.uiowa.physics.pw.das.math.DelaunayTriangulation.Chen;

import java.lang.*;

public class Coord {

  public double x, y;

  public String toString() {
    return "(" + x + "," + y + ")";
  }

  public boolean equals(final Coord p) {
    if(Math.abs(p.x - x) < 1E-5 && Math.abs(p.y - y) < 1E-5)
      return true;
    else
      return false;
  }
}
