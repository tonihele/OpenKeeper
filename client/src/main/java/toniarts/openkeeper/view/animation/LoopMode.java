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

/**
 * Animation loop mode
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public enum LoopMode {
    /**
     * The animation will play repeatedly, when it reaches the end the animation
     * will play again from the beginning, and so on.
     */
    Loop,
    /**
     * The animation will not loop. It will play until the last frame, and then
     * freeze at that frame. It is possible to decide to play a new animation
     * when that happens by using a AnimEventListener.
     */
    DontLoop,
    /**
     * The animation will cycle back and forth. When reaching the end, the
     * animation will play backwards from the last frame until it reaches the
     * first frame.
     */
    Cycle

}
