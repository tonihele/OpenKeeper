package toniarts.openkeeper.utils;

public final class Point {
    public final int x;
    public final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point() {
        x = y = 0;
	}

	@Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Point point = (Point) obj;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "(" + x + ',' + y + ')';
    }

	public double distance(Point p) {
        double px = p.x - this.x;
        double py = p.y - this.y;
        return Math.sqrt(px * px + py * py);
	}
}