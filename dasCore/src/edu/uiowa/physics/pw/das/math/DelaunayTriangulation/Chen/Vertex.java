package edu.uiowa.physics.pw.das.math.DelaunayTriangulation.Chen;

/**
 * Voronoi diagram
 *
 * Copyright(c) 1998 Ping-Che Chen
 *
 * This program defines the vertices in a voronoi diagram.
 * This is a internal data structure.
 */

public class Vertex extends Coord {

  public List edges = null;
  public int number;

  public Vertex() {
    edges = new List();
  }
}
