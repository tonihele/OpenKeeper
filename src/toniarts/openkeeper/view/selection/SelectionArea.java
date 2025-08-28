package toniarts.openkeeper.view.selection;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * @author 7willuwe : Philip Willuweit
 */
public final class SelectionArea implements Iterable<Set<Point>> {

    private Vector2f start = new Vector2f();
    private Vector2f end = new Vector2f();
    private float scale = 1;

    public SelectionArea(Vector2f start, Vector2f end) {
        this.start = start;
        this.end = end;
    }

    public SelectionArea(float appScaled) {
        this.scale = appScaled;
        this.start = new Vector2f(Vector2f.ZERO);
        this.end = new Vector2f(Vector2f.ZERO);
    }

    /**
     * For single square use only
     *
     * @param appScaled
     * @param start Start position
     * @param end End position
     */
    public SelectionArea(float appScaled, Vector2f start, Vector2f end) {
        this.start = start;
        this.end = end;
        this.scale = appScaled;
    }

    /**
     * @return the start
     */
    public Vector2f getStart() {
        return new Vector2f(Math.min(start.x, end.x), Math.min(start.y, end.y));
    }

    /**
     * Get the real starting coordinates, the first click
     *
     * @return
     */
    public Vector2f getRealStart() {
        return start;
    }

    /**
     * @param position the start to set
     */
    public void setStart(Vector2f position) {
        start.set(position);
        end.set(position);
    }

    /**
     * @return the end
     */
    public Vector2f getEnd() {
        return new Vector2f(Math.max(start.x, end.x), Math.max(start.y, end.y));
    }

    /**
     * Get the real ending coordinates, the click release
     *
     * @return
     */
    public Vector2f getRealEnd() {
        return end;
    }

    /**
     * @param position the end to set
     */
    public void setEnd(Vector2f position) {
        end.set(position);
    }

    /**
     * @return the scale
     */
    public float getScale() {
        return scale;
    }

    /**
     * @param scale the scale to set
     */
    public void setScale(float scale) {
        this.scale = scale;
    }

    public Vector2f getCenter() {
        return new Vector2f((end.x + start.x) / 2, (start.y + end.y) / 2);
    }

    /**
     * @return the delta x axis
     */
    public float getDeltaX() {
        return (Math.abs(end.x - start.x) + 1) / scale;
    }

    /**
     * @return the delta y axis
     */
    public float getDeltaY() {
        return (Math.abs(end.y - start.y) + 1) / scale;
    }

    @Override
    public Iterator<Set<Point>> iterator() {
        return new SelectionArea.AreaIterator(getRealStart(), getRealEnd());
    }

    public Iterator<Point> simpleIterator() {
        return new SimpleIterator(getStart(), getEnd());
    }

    /**
     * An optimized version of AbstractList.Itr
     */
    public final static class AreaIterator implements Iterator<Set<Point>> {

        private final Point end;
        private final Point start;
        private final Point realStart;
        private final Point realEnd;
        private final Point delta;

        private Set<Point> cursor = new HashSet<>();

        public AreaIterator(final Vector2f start, final Vector2f end) {
            this.realStart = WorldUtils.vectorToPoint(start);
            this.realEnd = WorldUtils.vectorToPoint(end);
            this.end = new Point(Math.max(realStart.x, realEnd.x), Math.max(realStart.y, realEnd.y));
            this.start = new Point(Math.min(realStart.x, realEnd.x), Math.min(realStart.y, realEnd.y));
            this.delta = new Point(FastMath.sign(realStart.x - realEnd.x),
                    FastMath.sign(realStart.y - realEnd.y));
        }

        @Override
        public boolean hasNext() {
            return !(cursor.size() == 1 && cursor.contains(realStart));
        }

        @Override
        public Set<Point> next() {
            Set result = new HashSet<>();

            if (cursor.isEmpty()) {
                result.add(realEnd);
            }

            for (Point p : cursor) {
                if (delta.x != 0) {
                    Point neighborhood = new Point(p.x + delta.x, p.y);
                    if (check(neighborhood)) {
                        result.add(neighborhood);
                    }
                }
                if (delta.y != 0) {
                    Point neighborhood = new Point(p.x, p.y + delta.y);
                    if (check(neighborhood)) {
                        result.add(neighborhood);
                    }
                }
            }

            cursor = result;

            return result;
        }

        private boolean check(Point p) {
            return p.x <= end.x && p.x >= start.x && p.y <= end.y && p.y >= start.y;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super Set<Point>> consumer) {
            throw new UnsupportedOperationException();
        }
    }

    public static final class SimpleIterator implements Iterator<Point> {

        private final Point start;
        private final Point end;
        private Point cursor;

        public SimpleIterator(final Vector2f start, final Vector2f end) {
            this.start = WorldUtils.vectorToPoint(start);
            this.end = WorldUtils.vectorToPoint(end);
        }

        @Override
        public boolean hasNext() {
            return !end.equals(cursor);
        }

        @Override
        public Point next() {
            if (cursor == null) {
                cursor = (Point) start.clone();
                return cursor;
            }

            cursor.x++;
            if (cursor.x > end.x) {
                cursor.x = start.x;
                cursor.y++;
            }

            return cursor;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super Point> consumer) {
            throw new UnsupportedOperationException();
        }
    }
}
