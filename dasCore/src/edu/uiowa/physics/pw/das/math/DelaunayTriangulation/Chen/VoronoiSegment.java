package edu.uiowa.physics.pw.das.math.DelaunayTriangulation.Chen;

/**
 * Voronoi diagram
 *
 * Copyright(c) 1998 Ping-Che Chen
 *
 * This program defines the line segment used in the Voronoi
 * diagram problem.
 *
 */

public class VoronoiSegment extends Segment {

  VoronoiSegment(Vertex v1, Vertex v2) {
    this.v1 = v1;
    this.v2 = v2;
    floating = false;
  }

  public final Vertex v1, v2;
  public boolean floating;

}
