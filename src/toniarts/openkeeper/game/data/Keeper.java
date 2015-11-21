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
package toniarts.openkeeper.game.data;

import toniarts.openkeeper.tools.convert.map.AI.AIType;

/**
 * Your friendly neighbourhood Keeper, or not
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Keeper {

    private boolean ai;
    private AIType aiType = AIType.MASTER_KEEPER;
    private String name;
    private boolean ready = false;

    public Keeper(boolean ai, String name) {
        this.ai = ai;
        this.name = name;

        // AI is always ready
        ready = ai;
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    public String toString() {
        return (ai ? aiType.toString() : name);
    }
}
