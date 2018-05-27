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
package toniarts.openkeeper.game.trigger.actionpoint;

import toniarts.openkeeper.game.data.ActionPoint;
import java.awt.Point;
import java.util.List;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.game.control.IContainer;

/**
 *
 * @author ArchDemon
 */
public class FlashControl extends Control {

    private float time;
    private List<Point> points;

    public FlashControl() {
    }

    public FlashControl(int time) {
        this.time = time;
    }

    @Override
    public void setParent(IContainer parent) {
        if (parent != null) {
            points = ((ActionPoint) parent).getPoints();
        }
        super.setParent(parent);
    }

    @Override
    protected void updateControl(float tpf) {

//        if (time < 0) {
//            ((ActionPoint) parent).getParent().getWorldState().flashTile(false, points);
//            ((ActionPoint) parent).removeControl(this);
//        }
        time -= tpf;
    }
}
