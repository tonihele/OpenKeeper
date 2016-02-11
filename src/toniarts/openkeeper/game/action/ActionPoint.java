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
import java.util.EnumSet;
import toniarts.openkeeper.game.control.Container;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 *
 * @author ArchDemon
 */


public class ActionPoint extends Container {

    private int id;
    private int triggerId;
    private Vector2f start;
    private Vector2f end;
    private EnumSet<Thing.ActionPoint.ActionPointFlag> flags;
    private int waitDelay;
    private int nextWaypointId;

    public ActionPoint(Thing.ActionPoint acionPoint) {
        id = acionPoint.getId();
        start = new Vector2f(acionPoint.getStartX(), acionPoint.getStartY());
        end = new Vector2f(acionPoint.getEndX(), acionPoint.getEndY());
        waitDelay = acionPoint.getWaitDelay();
        nextWaypointId = acionPoint.getNextWaypointId();
        flags = acionPoint.getFlags();
        triggerId = acionPoint.getTriggerId();
    }

    public int getId() {
        return id;
    }

    public int getTriggerId() {
        return triggerId;
    }

    public Vector2f getStart() {
        return start;
    }

    public Vector2f getEnd() {
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
}
