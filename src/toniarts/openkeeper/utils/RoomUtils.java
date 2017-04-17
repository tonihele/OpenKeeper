package toniarts.openkeeper.utils;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by wietse on 17/04/17.
 */
public class RoomUtils {

    /*
    coordinatesMatrix: TRUE means of part of the room type, FALSE means not part of the room type
    squareSideLength: square side length to compare with

    Based on a histogram approach, every time you read a true (compared to the value above you on rows after the first), increment the current matrix value with 1
    unless you're on the first row, then the value just becomes 1.
    If you read a false, that means you're looking at a tile that is not part of the type, the histogram value gets reset to 0 (zero).

    Then, for each row (histogram), calculate the max rectangle that can be fitted in the part that is TRUE with the maxArea function
    if for any rectangle holds that its height and width equal or surpass squareSideLength, that means a square of
    squareSideLength x squareSideLength fits in the TRUE part of the coordinates matrix.

    Example: for the Temple we need at least a 5 x 5 square before the hand should be added.
     */
    public static boolean matrixContainsASquareWithSideLength(boolean [][] coordinatesMatrix, int squareSideLength) {
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

    public static Dimension maxArea(int[] hist) {

        Dimension d = new Dimension();

        final int n = hist.length;
        final Deque<Integer> s = new ArrayDeque<Integer>();

        int max_area = 0;
        int tp;
        int area_with_top;

        int i = 0;
        while (i < n)
        {
            if (s.isEmpty() || hist[s.peek()] <= hist[i])
                s.push(i++);
            else
            {
                tp = s.peek();
                s.pop();
                area_with_top = hist[tp] * (s.isEmpty() ? i : i - s.peek() - 1);
                if (max_area < area_with_top)
                {
                    d.setX(hist[tp]);
                    d.setY((s.isEmpty() ? i : i - s.peek() - 1));
                    max_area = area_with_top;
                }
            }
        }

        while (!s.isEmpty())
        {
            tp = s.peek();
            s.pop();
            area_with_top = hist[tp] * (s.isEmpty() ? i : i - s.peek() - 1);
            if (max_area < area_with_top)
            {
                d.setX(hist[tp]); ;
                d.setY((s.isEmpty() ? i : i - s.peek() - 1));
                max_area = area_with_top;
            }
        }
        return d;

    }

}