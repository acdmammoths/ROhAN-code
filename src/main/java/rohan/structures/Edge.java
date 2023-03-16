package rohan.structures;

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
import java.util.Objects;

/** A class to represent an edge in the graph representation of a dataset. */
public class Edge {
  public final int row;
  public final int col;

  /** The index of the edge in a matrix's list of edge. Initialized to -1 to represent no index. */
  public int index = -1;

  public Edge(int row, int col) {
    this.row = row;
    this.col = col;
  }

  public Edge(int row, int col, int index) {
    this(row, col);
    this.index = index;
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
      final Edge otherEdge = (Edge) o;
      return this.row == otherEdge.row
          && this.col == otherEdge.col
          && this.index == otherEdge.index;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.row, this.col, this.index);
  }

  @Override
  public String toString() {
    return "(" + this.row + ", " + this.col + ", " + this.index + ")";
  }
}
