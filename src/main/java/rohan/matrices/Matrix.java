package rohan.matrices;

/*
 * Copyright (C) 2022 Alexander Lee and Matteo Riondato
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import rohan.structures.Edge;
import rohan.structures.SparseMatrix;
import rohan.structures.Vector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * A wrapper class around an instance of a {@link SparseMatrix} and useful data structures that
 * store the matrix's properties.
 */
public class Matrix {
  /** A 0-1 matrix representation of the dataset. */
  SparseMatrix matrix;

  /** An array of the matrix's row sums indexed by row. */
  int[] rowSums;

  /** An array of the matrix's column sums indexed by column. */
  int[] colSums;

  /** A list of the edges from the bipartite graph representation of the matrix. */
  final List<Edge> edges = new ArrayList<>();

  /** A map where each key is a row and the value is the number of rows equal to that row. */
  private final Map<Vector, Integer> rowToNumEqRows = new HashMap<>();

  Matrix() {}

  /**
   * Creates an instance of {@link Matrix} from a 0-1 {@link SparseMatrix} by initializing necessary
   * data structures from the matrix.
   *
   * @param inMatrix a 0-1 matrix representation of the dataset
   */
  public Matrix(SparseMatrix inMatrix) {
    this.matrix = new SparseMatrix(inMatrix.getNumRows(), inMatrix.getNumCols());
    this.rowSums = new int[inMatrix.getNumRows()];
    this.colSums = new int[inMatrix.getNumCols()];
    for (int r = 0; r < inMatrix.getNumRows(); r++) {
      for (int c : inMatrix.getNonzeroIndices(r)) {
        this.setVal(r, c, inMatrix.get(r, c));
        this.edges.add(new Edge(r, c, this.edges.size()));
        this.rowSums[r]++;
        this.colSums[c]++;
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o == null) {
      return false;
    } else if (this.getClass() != o.getClass()) {
      return false;
    } else {
      Matrix otherMatrix = (Matrix) o;
      return Objects.equals(this.matrix, otherMatrix.matrix);
    }
  }

  @Override
  public int hashCode() {
    return this.matrix.hashCode();
  }

  @Override
  public String toString() {
    return this.matrix.toString();
  }

  public SparseMatrix getMatrix() {
    return this.matrix;
  }

  int getVal(int row, int col) {
    return this.matrix.get(row, col);
  }

  void setVal(int row, int col, int val) {
    this.matrix.set(row, col, val);
  }

  private Vector getRowCopy(int row) {
    return this.matrix.getRowCopy(row);
  }

  public Vector getRowInstance(int row) {
    return this.matrix.getRowInstance(row);
  }

  Set<Integer> getNonzeroIndices(int row) {
    return this.matrix.getNonzeroIndices(row);
  }

  public int getSum() {
    return this.edges.size();
  }

  int getNumRows() {
    return this.matrix.getNumRows();
  }

  public int[] getRowSums() {
    return this.rowSums;
  }

  int getRowSum(int row) {
    return this.rowSums[row];
  }

  public int[] getColSums() {
    return this.colSums;
  }

  int getColSum(int col) {
    return this.colSums[col];
  }

  /**
   * Returns edges as a set without their indices since sets do not have indices.
   *
   * @return edges as a set
   */
  public Set<Edge> getEdgesSet() {
    Set<Edge> edgesSet = new HashSet<>();
    for (Edge edge : this.edges) {
      edgesSet.add(new Edge(edge.row, edge.col));
    }
    return edgesSet;
  }

  int getNumEdges() {
    return this.edges.size();
  }

  /**
   * Gets the index in the matrix's list of edges for the given edge to find. This is done by only
   * comparing the row and column of the edge to find with those of each edge in the list. The index
   * of the given edge to find may be different from what is returned since the input edge may be
   * from another matrix's list of edges (e.g., in the case of unit tests) and thus may have a
   * different index.
   *
   * @param edgeToFind the edge to find in the matrix's list of edges
   * @return the index in the matrix's list of edges for the given edge to find
   */
  public int getEdgeIndex(Edge edgeToFind) {
    for (Edge edge : this.edges) {
      if (edge.row == edgeToFind.row && edge.col == edgeToFind.col) {
        return edge.index;
      }
    }
    throw new IllegalArgumentException("edgeToFind is not in edges.");
  }

  private Edge getEdge(int index) {
    return this.edges.get(index);
  }

  /**
   * Samples edges from the graph representation of the matrix uniformly at random.
   *
   * @param rnd a {@link Random} instance
   * @return the two sampled edges as a length two array of {@link Edge}
   */
  Edge[] sampleEdges(Random rnd) {
    final int edge1Index = rnd.nextInt(this.getNumEdges());
    int edge2Index;

    do {
      edge2Index = rnd.nextInt(this.getNumEdges());
    } while (edge1Index == edge2Index);

    final Edge edge1 = this.getEdge(edge1Index);
    final Edge edge2 = this.getEdge(edge2Index);

    return new Edge[] {edge1, edge2};
  }

  /**
   * Swaps 1s across two rows in the matrix.
   *
   * @param swappableEdge1 the first swappable edge
   * @param swappableEdge2 the second swappable edge
   * @param newEdge1 the first new edge to be added
   * @param newEdge2 the second new edge to be added
   */
  private void swapVals(Edge swappableEdge1, Edge swappableEdge2, Edge newEdge1, Edge newEdge2) {
    this.setVal(swappableEdge1.row, swappableEdge1.col, 0);
    this.setVal(swappableEdge2.row, swappableEdge2.col, 0);
    this.setVal(newEdge1.row, newEdge1.col, 1);
    this.setVal(newEdge2.row, newEdge2.col, 1);
  }

  /**
   * Swaps the edges in the graph representation of the matrix by updating the list of edges with
   * the new edges. Specifically, this is done by setting the index of the old (i.e., swappable)
   * edges with th new edges.
   *
   * @param newEdge1 the first new edge to be added
   * @param newEdge2 the second new edge to be added
   */
  private void swapEdges(Edge newEdge1, Edge newEdge2) {
    this.edges.set(newEdge1.index, newEdge1);
    this.edges.set(newEdge2.index, newEdge2);
  }

  /**
   * Gets the new rows of the matrix defined by the swappable and new edges.
   *
   * @param swappableEdge1 the first swappable edge
   * @param swappableEdge2 the second swappable edge
   * @param newEdge1 the first new edge to be added
   * @param newEdge2 the second new edge to be added
   * @return the two new rows of the matrix
   */
  public Vector[] getNewRows(
      Edge swappableEdge1, Edge swappableEdge2, Edge newEdge1, Edge newEdge2) {
    final Vector newRow1 = this.getRowCopy(newEdge1.row);
    newRow1.set(swappableEdge1.col, 0);
    newRow1.set(newEdge1.col, 1);

    final Vector newRow2 = this.getRowCopy(newEdge2.row);
    newRow2.set(swappableEdge2.col, 0);
    newRow2.set(newEdge2.col, 1);

    return new Vector[] {newRow1, newRow2};
  }

  public Map<Vector, Integer> getRowToNumEqRowsMap() {
    return this.rowToNumEqRows;
  }

  int getNumEqRows(Vector row) {
    return this.rowToNumEqRows.getOrDefault(row, 0);
  }

  private void setNumEqRows(Vector row, int eqRowsNum) {
    if (eqRowsNum == 0) {
      this.removeNumEqRows(row);
    } else {
      // create a copy of the row only if map doesn't contain it already
      if (!this.rowToNumEqRows.containsKey(row)) {
        row = row.copy();
      }
      // map won't use instance of row if it already contains one
      this.rowToNumEqRows.put(row, eqRowsNum);
    }
  }

  private void removeNumEqRows(Vector row) {
    this.rowToNumEqRows.remove(row);
  }

  void incNumEqRows(Vector row) {
    final int num = this.getNumEqRows(row);
    this.setNumEqRows(row, num + 1);
  }

  /**
   * Decrement the number of equal rows for the input row by 1.
   *
   * @param row the input row
   * @throws IllegalArgumentException if the current number of equal rows is not greater than 0
   */
  private void decNumEqRows(Vector row) {
    final int num = getNumEqRows(row);
    if (num <= 0) {
      throw new IllegalArgumentException(
          "The number of rows equal to row " + row + " is " + num + ", which is non positive.");
    }
    this.setNumEqRows(row, num - 1);
  }

  /**
   * Transitions to the next state in the chain by updating the current matrix to the adjacent
   * matrix.
   *
   * @param swappableEdge1 the first swappable edge that transitions to the adjacent matrix
   * @param swappableEdge2 the second swappable edge that transitions to the adjacent matrix
   * @param newEdge1 the first new edge that transitions to the adjacent matrix
   * @param newEdge2 the second new edge that transitions to the adjacent matrix
   */
  public void transition(Edge swappableEdge1, Edge swappableEdge2, Edge newEdge1, Edge newEdge2) {
    this.swapVals(swappableEdge1, swappableEdge2, newEdge1, newEdge2);
    this.swapEdges(newEdge1, newEdge2);
  }

  /**
   * Transitions to the next state in the chain by updating the current matrix to the adjacent
   * matrix and the rowToNumEqRows map.
   *
   * @param swappableEdge1 the first swappable edge that transitions to the adjacent matrix
   * @param swappableEdge2 the second swappable edge that transitions to the adjacent matrix
   * @param newEdge1 the first new edge that transitions to the adjacent matrix
   * @param newEdge2 the second new edge that transitions to the adjacent matrix
   * @param swappableRow1 the first swappable row of the matrix
   * @param swappableRow2 the second swappable row of the matrix
   * @param newRow1 the first new row of the adjacent matrix
   * @param newRow2 the second new row of the adjacent matrix
   */
  public void transition(
      Edge swappableEdge1,
      Edge swappableEdge2,
      Edge newEdge1,
      Edge newEdge2,
      Vector swappableRow1,
      Vector swappableRow2,
      Vector newRow1,
      Vector newRow2) {
    // update number of equal rows
    this.decNumEqRows(swappableRow1);
    this.decNumEqRows(swappableRow2);
    this.incNumEqRows(newRow1);
    this.incNumEqRows(newRow2);

    // update matrix and edges
    // call this last so that the swap doesn't update the Vector instances first
    this.transition(swappableEdge1, swappableEdge2, newEdge1, newEdge2);
  }
}
