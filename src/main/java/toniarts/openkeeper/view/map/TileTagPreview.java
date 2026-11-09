package toniarts.openkeeper.view.map;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import toniarts.openkeeper.utils.Point;

/** Local drag tint, retained after release until the server confirms it. */
public final class TileTagPreview {
    private final Map<Point, Boolean> pending = new HashMap<>();
    private final Map<Point, Boolean> active = new HashMap<>();

    public boolean isTagged(Point point, boolean serverTagged) {
        return active.getOrDefault(point, pending.getOrDefault(point, serverTagged));
    }

    /** Returns only tiles whose local override changed, including a shrinking drag. */
    public Set<Point> replace(Collection<Point> points, boolean tagged) {
        Map<Point, Boolean> before = overrides();
        active.clear();
        for (Point point : points) {
            active.put(new Point(point.x, point.y), tagged);
        }
        return changed(before);
    }

    public Set<Point> cancel() {
        Map<Point, Boolean> before = overrides();
        active.clear();
        return changed(before);
    }

    public void commit() {
        pending.putAll(active);
        active.clear();
    }

    /** Older server updates must not erase a newer, opposite local prediction. */
    public void acknowledge(Collection<Point> points, Predicate<Point> serverTagged, Predicate<Point> eligible) {
        for (Point point : points) {
            Boolean tagged = pending.get(point);
            if (!eligible.test(point)) {
                active.remove(point);
                pending.remove(point);
            } else if (tagged != null && tagged == serverTagged.test(point)) {
                pending.remove(point);
            }
        }
    }

    private Map<Point, Boolean> overrides() {
        Map<Point, Boolean> result = new HashMap<>(pending);
        result.putAll(active);
        return result;
    }

    private Set<Point> changed(Map<Point, Boolean> before) {
        Map<Point, Boolean> after = overrides();
        Set<Point> changed = new HashSet<>(before.keySet());
        changed.addAll(after.keySet());
        changed.removeIf(point -> java.util.Objects.equals(before.get(point), after.get(point)));
        return changed;
    }
}
