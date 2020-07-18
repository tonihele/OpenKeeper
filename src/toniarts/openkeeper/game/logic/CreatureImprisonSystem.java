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
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * Handles creatures left imprisoned and rotting. Essentially just decreases the
 * health as we go. Raises them to a skeleton if enough capacity
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureImprisonSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final EntitySet imprisonedEntities;
    private final int healthRegeneratePerSecond;

    public CreatureImprisonSystem(EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.entityData = entityData;
        healthRegeneratePerSecond = (int) gameSettings.get(Variable.MiscVariable.MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND).getValue();

        // Have the position also here, since the player may move imprisoned entities between jails, kinda still imprisoned but not counting towards death at the time
        imprisonedEntities = entityData.getEntities(CreatureImprisoned.class, Health.class, CreatureComponent.class, Position.class);
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // Add new & remove old
        imprisonedEntities.applyChanges();

        // Process ticks
        for (Entity entity : imprisonedEntities) {
            Health health = entity.get(Health.class);

            // TODO: Join the Skeleton army!!

            // Health
            CreatureImprisoned imprisoned = entity.get(CreatureImprisoned.class);
            if (gameTime - imprisoned.healthCheckTime >= 1) {
                entityData.setComponent(entity.getId(), new CreatureImprisoned(imprisoned.startTime, imprisoned.healthCheckTime + 1));
                entityData.setComponent(entity.getId(), new Health(Math.min(health.health + healthRegeneratePerSecond, health.maxHealth), health.maxHealth));
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        imprisonedEntities.release();
    }

}
