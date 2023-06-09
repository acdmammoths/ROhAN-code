package rohan.cases;

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
import rohan.matrices.Matrix;

public class MatrixNumSwapPairsTestCase {
  public final Matrix matrix;
  public final int swappableRowIndex1;
  public final int swappableRowIndex2;
  public final int matrixNumSwapPairs;

  public MatrixNumSwapPairsTestCase(
      Matrix matrix, int swappableRowIndex1, int swappableRowIndex2, int matrixNumSwapPairs) {
    this.matrix = matrix;
    this.swappableRowIndex1 = swappableRowIndex1;
    this.swappableRowIndex2 = swappableRowIndex2;
    this.matrixNumSwapPairs = matrixNumSwapPairs;
  }
}
