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
package toniarts.openkeeper.world.control;

import java.awt.Point;
import toniarts.openkeeper.game.action.ActionPoint;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.game.control.IContainer;
import toniarts.openkeeper.world.WorldState;

/**
 *
 * @author ArchDemon
 */


public class FlashTileControl extends Control {

    private float time;
    private float tick = 0;
    private boolean flashed = false;
    private boolean unlimited = false;
    public final float PERIOD = 0.3f;

    private WorldState worldState;
    private Point[] points;

    public FlashTileControl() {
    }

    public FlashTileControl(int time, WorldState worldState) {
        this.time = time;
        this.worldState = worldState;
        if (this.time == 0) {
            unlimited = true;
        }
    }

    @Override
    public void setParent(IContainer parent) {
        if (parent == null) {
            time = -1;
            worldState.flashTile(false, points);
        } else {
            Point start = ((ActionPoint) parent).getStart();
            Point end = ((ActionPoint) parent).getEnd();
            points = new Point[(end.x - start.x + 1) * (end.y - start.y + 1)];
            int index = 0;
            for (int x = start.x; x <= end.x; x++) {
                for (int y = start.y; y <= end.y; y++) {
                    points[index++] = new Point(x, y);
                }
            }
        }
        super.setParent(parent);
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!enabled) {
            return;
        }

        if (tick >= PERIOD || time < 0) {
            if (time < 0) {
                parent.removeControl(this);
                flashed = false;
                enabled = false;
            } else {
                tick -= PERIOD;
                flashed = !flashed;
            }
            worldState.flashTile(flashed, points);
        }

        tick += tpf;
        if (!unlimited) {
            time -= tpf;
        }
    }
}
