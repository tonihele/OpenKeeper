/*
 * Copyright (C) 2014-2021 OpenKeeper
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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * Modifies [creature] moods. TODO: Currently a stub, collection of modifiers we
 * know we need
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureMoodSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final EntitySet moodEntities;

    private final int moodRegeneratePerSecondSleeping;

    public CreatureMoodSystem(EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.entityData = entityData;
        moodRegeneratePerSecondSleeping = (int) gameSettings.get(Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND).getValue();

        moodEntities = entityData.getEntities(CreatureMood.class);
    }

    @Override
    public void processTick(float tpf, double gameTime) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        moodEntities.release();
    }

}
