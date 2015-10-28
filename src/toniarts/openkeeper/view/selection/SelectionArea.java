package toniarts.openkeeper.view.selection;

import com.jme3.math.Vector2f;

/**
 * @author 7willuwe : Philip Willuweit
 */
public class SelectionArea {

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
     * @param vector2f
     * @param appScaled
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
    public Vector2f getActualStartingCoordinates() {
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
     * @return the delta_xaxis
     */
    public float getDeltaX() {
        return (Math.abs(end.x - start.x) + 1) / scale;
    }

    /**
     * @return the delta_yaxis
     */
    public float getDeltaY() {
        return (Math.abs(end.y - start.y) + 1) / scale;
    }
}
