/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game.action;

import com.jme3.math.Vector2f;
import java.awt.Point;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import toniarts.openkeeper.game.control.Container;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 *
 * @author ArchDemon
 */
public class ActionPoint extends Container {

    private final int id;
    private ActionPointState parent;
    private final int triggerId;
    private final Point start;
    private final Point end;
    private final EnumSet<Thing.ActionPoint.ActionPointFlag> flags;
    private final int waitDelay;
    private final int nextWaypointId;
    private final List<Point> points;

    public ActionPoint(Thing.ActionPoint acionPoint) {
        id = acionPoint.getId();
        start = new Point(acionPoint.getStartX(), acionPoint.getStartY());
        end = new Point(acionPoint.getEndX(), acionPoint.getEndY());
        waitDelay = acionPoint.getWaitDelay();
        nextWaypointId = acionPoint.getNextWaypointId();
        flags = acionPoint.getFlags();
        triggerId = acionPoint.getTriggerId();

        points = new ArrayList<>((end.x - start.x + 1) * (end.y - start.y + 1));
        for (int x = start.x; x <= end.x; x++) {
            for (int y = start.y; y <= end.y; y++) {
                points.add(new Point(x, y));
            }
        }
    }

    public int getId() {
        return id;
    }

    public ActionPointState getParent() {
        return parent;
    }

    protected void setParent(ActionPointState parent) {
        this.parent = parent;
    }

    public int getTriggerId() {
        return triggerId;
    }

    public Point getStart() {
        return start;
    }

    public Point getEnd() {
        return end;
    }

    public EnumSet<Thing.ActionPoint.ActionPointFlag> getFlags() {
        return flags;
    }

    public int getWaitDelay() {
        return waitDelay;
    }

    public int getNextWaypointId() {
        return nextWaypointId;
    }

    public Vector2f getCenter() {
        return new Vector2f((start.x + end.x) / 2, (start.y + end.y) / 2);
    }

    public List<Point> getPoints() {
        return points;
    }

    @Override
    public String toString() {
        return "ActionPoint { id=" + id + ", triggerId=" + triggerId + ", start=" + start + ", end="
                + end + ", flags=" + flags + ", waitDelay=" + waitDelay + ", nextWaypointId=" + nextWaypointId + " }";
    }
}
