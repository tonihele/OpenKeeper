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
package toniarts.openkeeper.view;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityContainer;
import com.simsilica.es.EntityData;
import java.util.function.Consumer;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.Position;

/**
 * Watches the levels of the creatures on the map and tells when one of them
 * levels up. A creature that comes onto the map (or is dropped from the hand)
 * only has its level remembered, it didn't level up.
 */
final class CreatureLevelUpContainer extends EntityContainer<CreatureLevelUpContainer.Level> {

    private final Consumer<Entity> levelUpListener;

    /**
     * The level of a creature as last seen
     */
    static final class Level {

        private int level;

        private Level(int level) {
            this.level = level;
        }
    }

    /**
     * Watches the creature levels
     *
     * @param ed the entity data
     * @param levelUpListener called with the creature when it levels up, once
     * even if it gains several levels at once
     */
    CreatureLevelUpContainer(EntityData ed, Consumer<Entity> levelUpListener) {
        super(ed, Position.class, CreatureExperience.class);
        this.levelUpListener = levelUpListener;
    }

    @Override
    protected Level addObject(Entity e) {
        return new Level(e.get(CreatureExperience.class).level);
    }

    @Override
    protected void updateObject(Level object, Entity e) {
        int level = e.get(CreatureExperience.class).level;
        if (level > object.level) {
            levelUpListener.accept(e);
        }
        object.level = level;
    }

    @Override
    protected void removeObject(Level object, Entity e) {
    }
}
