/*
 * Copyright (C) 2014-2020 OpenKeeper
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
package toniarts.openkeeper.view.animation;

import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.action.BaseAction;

/**
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LoopAction extends BaseAction {

    private final LoopMode loopMode;
    private double lastTime = 0.0;
    private boolean forward = false;

    public LoopAction(Tween tween, LoopMode loopMode) {
        super(tween);

        this.loopMode = loopMode;
    }

    @Override
    public boolean interpolate(double t) {
        boolean running = super.interpolate(t);
        if (!running) {
            switch (loopMode) {
                case DontLoop: {
                    setSpeed(0);
                    break;
                }
                case Cycle: {
                    setSpeed(-getSpeed());
                    break;
                }
            }
        }
        lastTime = t;
        forward = isForward();

        return running;
    }

}
