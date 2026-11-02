/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;

/** Parser-package fixtures for native sleep-cycle integration tests. */
public final class CreatureSleepFixtures {
    private CreatureSleepFixtures() {
    }

    public static GameLevel level(int ticksPerSecond) {
        GameLevel level = new GameLevel();
        level.setTicksPerSec(ticksPerSecond);
        return level;
    }

    public static Creature sleeper(int timeSleep) {
        Creature creature = new Creature();
        creature.setCreatureId((short) 6);
        creature.setName("Dark Elf");
        creature.setLairObjectId((short) 1);
        creature.setFlags(EnumSet.noneOf(Creature.CreatureFlag.class));
        creature.getAttributes().setTimeAwake(20);
        creature.getAttributes().setTimeSleep(timeSleep);
        creature.getAttributes().setUnhappyThreshold((short) 50);
        return creature;
    }

    public static Variable.MiscVariable variable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

    /**
     * A creature whose melee animation, the one DK II's dig swing plays, has
     * the given frames
     */
    public static Creature digger(String name, int frames, int fps, int startFrames, int endFrames) {
        ArtResource melee = new ArtResource();
        melee.setFlags(EnumSet.noneOf(ArtResource.ArtResourceFlag.class));
        melee.setData(ArtResource.KEY_FRAMES, frames);
        melee.setData(ArtResource.KEY_FPS, fps);
        if (startFrames > 0) {
            melee.getFlags().add(ArtResource.ArtResourceFlag.HAS_START_ANIMATION);
            melee.setData(ArtResource.KEY_START_AF, startFrames);
        }
        if (endFrames > 0) {
            melee.getFlags().add(ArtResource.ArtResourceFlag.HAS_END_ANIMATION);
            melee.setData(ArtResource.KEY_END_AF, endFrames);
        }
        Creature creature = new Creature();
        creature.setName(name);
        creature.setAnimation(Creature.AnimationType.MELEE_ATTACK, melee);
        return creature;
    }
}
