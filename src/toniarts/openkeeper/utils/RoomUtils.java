/*
 * Copyright (C) 2014-2017 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.utils;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by wietse on 17/04/17.
 */
public class RoomUtils {

    /**
     * Based on a histogram approach, every time you read a true (compared to the value above you on rows after the first), increment the current matrix value with 1
     * unless you're on the first row, then the value just becomes 1.
     * If you read a false, that means you're looking at a tile that is not part of the type, the histogram value gets reset to 0 (zero).
     *
     * Then, for each row (histogram), calculate the max rectangle that can be fitted in the part that is TRUE with the maxArea function
     * if for any rectangle holds that its height and width equal or surpass squareSideLength, that means a square of
     * squareSideLength x squareSideLength fits in the TRUE part of the coordinates matrix.
     *
     * Example: for the Temple we need at least a 5 x 5 square before the hand should be added.
     *
     * @param coordinatesMatrix boolean 2D matrix, TRUE means of part of the room type, FALSE means not part of the room type
     * @param squareSideLength square side length to compare with
     * @return whether the desired square fits in the matrix or not
     */
    public static boolean matrixContainsSquare(boolean [][] coordinatesMatrix, int squareSideLength) {
        final int rows = coordinatesMatrix.length;
        final int cols = coordinatesMatrix[0].length;

        //Calculate F (1 histogram per row)
        int[][] F = new int[rows][cols];
        for(int i = 0; i < rows; ++i) {
            for(int j = 0; j < cols; ++j) {
                if(coordinatesMatrix[i][j]) {
                    if(i > 0) {
                        F[i][j] = 1 + F[i - 1][j];
                    } else {
                        F[i][j] = 1;
                    }
                } else if(!coordinatesMatrix[i][j])  {
                    F[i][j] = 0;
                }
            }
        }

        for(int i = rows - 1; i >= 0; --i) {
            if(maxArea(F[i]).getX() >= squareSideLength && maxArea(F[i]).getY() >= squareSideLength) {
                return true;
            }
        }
        return false;

    }

    /**
     * @param histogram histogram detailing the amount of sequential room tiles
     * @return the x and y coordinates of the largest fitting rectangle up to this point in the matrix
     */
    public static Dimension maxArea(int[] histogram) {

        Dimension dimension = new Dimension();

        final int length = histogram.length;
        final Deque<Integer> deque = new ArrayDeque<Integer>();

        int maxArea = 0;
        int topValue;
        int areaWithTop;

        int i = 0;
        while (i < length)
        {
            if (deque.isEmpty() || histogram[deque.peek()] <= histogram[i])
                deque.push(i++);
            else
            {
                topValue = deque.peek();
                deque.pop();
                areaWithTop = histogram[topValue] * (deque.isEmpty() ? i : i - deque.peek() - 1);
                if (maxArea < areaWithTop)
                {
                    dimension.setX(histogram[topValue]);
                    dimension.setY((deque.isEmpty() ? i : i - deque.peek() - 1));
                    maxArea = areaWithTop;
                }
            }
        }

        while (!deque.isEmpty())
        {
            topValue = deque.peek();
            deque.pop();
            areaWithTop = histogram[topValue] * (deque.isEmpty() ? i : i - deque.peek() - 1);
            if (maxArea < areaWithTop)
            {
                dimension.setX(histogram[topValue]); ;
                dimension.setY((deque.isEmpty() ? i : i - deque.peek() - 1));
                maxArea = areaWithTop;
            }
        }
        return dimension;

    }

    /**
     * a tile is considered a border (not water) if it's either first or last in a row or column,
     * or if any of its direct neighbours are not of the room type
     *
     * @param coordinatesAsMatrix boolean 2D matrix, TRUE means of part of the room type, FALSE means not part of the room type
     * @return boolean array that defines water tiles by TRUE; non-water by FALSE
     */
    public static boolean[][] calculateWaterArea(boolean[][] coordinatesAsMatrix) {
        final int rows = coordinatesAsMatrix.length;
        final int cols = coordinatesAsMatrix[0].length;

        boolean[][] waterTiles = new boolean[rows][cols];

        for(int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                waterTiles[i][j] = !isBorderTile(coordinatesAsMatrix, i, j, rows, cols);
            }
        }
        return waterTiles;
    }

    /**
     * @param coordinatesAsMatrix boolean 2D matrix, TRUE means of part of the room type, FALSE means not part of the room type
     * @param row current row
     * @param col current column
     * @param rows total rows
     * @param cols total columns
     * @return whether the current tile is a border tile or not
     */
    private static boolean isBorderTile(boolean[][] coordinatesAsMatrix, int row, int col, int rows, int cols) {
        if(row == 0 || col == 0 || row == rows - 1 || col == cols - 1 ) {
            return true;
        }

        return (   !coordinatesAsMatrix[row - 1][col - 1] || !coordinatesAsMatrix[row - 1][col] || !coordinatesAsMatrix[row - 1][col + 1]
                || !coordinatesAsMatrix[row][col - 1] || !coordinatesAsMatrix[row][col + 1]
                || !coordinatesAsMatrix[row + 1][col - 1] || !coordinatesAsMatrix[row + 1][col] || !coordinatesAsMatrix[row + 1][col + 1]);
    }

    public static boolean[][] calculateBorderArea(boolean[][] coordinatesAsMatrix, boolean[][] waterArea) {
        final int rows = coordinatesAsMatrix.length;
        final int cols = coordinatesAsMatrix[0].length;

        boolean[][] borderTiles = new boolean[rows][cols];

        for(int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                borderTiles[i][j] = !waterArea[i][j] && coordinatesAsMatrix[i][j];

            }
        }
        return borderTiles;
    }
}