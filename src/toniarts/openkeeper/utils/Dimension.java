package toniarts.openkeeper.utils;

/**
 * Created by wietse on 17/04/17.
 */
public class Dimension {
    private int x;
    private int y;

    public Dimension() {
        this.x = 0;
        this.y = 0;
    }

    public Dimension(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getArea() {
        return x * y;
    }
}