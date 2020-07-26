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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureRecuperating;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * Handles creatures recuperating from wounds. Essentially just increases the
 * health as we go
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureRecuperatingSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final EntitySet recuperatingEntities;
    private final int healthRegeneratePerSecond;
    private final int moodRegeneratePerSecond;

    public CreatureRecuperatingSystem(EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.entityData = entityData;
        healthRegeneratePerSecond = (int) gameSettings.get(Variable.MiscVariable.MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND).getValue();
        moodRegeneratePerSecond = (int) gameSettings.get(Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND).getValue();

        recuperatingEntities = entityData.getEntities(CreatureRecuperating.class, Health.class, CreatureComponent.class);
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // Add new & remove old
        recuperatingEntities.applyChanges();

        // Process ticks
        for (Entity entity : recuperatingEntities) {
            Health health = entity.get(Health.class);
            if (health.health == health.maxHealth) {
                entityData.removeComponent(entity.getId(), CreatureRecuperating.class);
                continue;
            }

            // Health
            CreatureRecuperating creatureRecuperating = entity.get(CreatureRecuperating.class);
            if (gameTime - creatureRecuperating.healthCheckTime >= 1) {
                entityData.setComponent(entity.getId(), new CreatureRecuperating(creatureRecuperating.startTime, creatureRecuperating.healthCheckTime + 1));
                entityData.setComponent(entity.getId(), new Health(Math.min(health.health + healthRegeneratePerSecond, health.maxHealth), health.maxHealth));
                entityData.removeComponent(entity.getId(), Unconscious.class);
            }

            // TODO: mood
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        recuperatingEntities.release();
    }

}
