/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;
import toniarts.openkeeper.game.controller.creature.CreatureState;

/**
 * Simple creature AI component
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureAi implements EntityComponent {

    public double stateStartTime;
    private Integer currentCreatureState;
    private Integer nextCreatureState;
    public short creatureId;

    public CreatureAi() {
    }

    public CreatureAi(double stateStartTime, CreatureState nextCreatureState, short creatureId) {
        this.stateStartTime = stateStartTime;
        if (nextCreatureState != null) {
            this.nextCreatureState = nextCreatureState.ordinal();
        }
        this.creatureId = creatureId;
    }

    public CreatureAi(double stateStartTime, CreatureState currentCreatureState, CreatureState nextCreatureState, short creatureId) {
        this.stateStartTime = stateStartTime;
        if (currentCreatureState != null) {
            this.currentCreatureState = currentCreatureState.ordinal();
        }
        if (nextCreatureState != null) {
            this.nextCreatureState = nextCreatureState.ordinal();
        }
        this.creatureId = creatureId;
    }

    public final CreatureState getCurrentCreatureState() {

        /**
         * The creature state enum is not final class, so FieldSerializer wont
         * serialize it. Easiest for us now is just to emulate the serialization
         * like this
         */
        if (currentCreatureState == null) {
            return null;
        }

        return CreatureState.class.getEnumConstants()[currentCreatureState];
    }

    public final CreatureState getNextCreatureState() {

        /**
         * The creature state enum is not final class, so FieldSerializer wont
         * serialize it. Easiest for us now is just to emulate the serialization
         * like this
         */
        if (nextCreatureState == null) {
            return null;
        }

        return CreatureState.class.getEnumConstants()[nextCreatureState];
    }

}
