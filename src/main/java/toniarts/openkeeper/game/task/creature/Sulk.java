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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Sulks in the lair. Like in DK II, once a second the lair calms the creature
 * and heals it as if it slept there, and the creature gets over it when the
 * anger the lair calms is gone
 */
public final class Sulk extends AbstractTileTask {

    private final EntityData entityData;
    private final int angerPerSecond;
    private final int healthPerSecond;

    public Sulk(final INavigationService navigationService, final IMapController mapController, ICreatureController creature,
            EntityData entityData, Map<MiscType, MiscVariable> gameSettings) {
        super(navigationService, mapController, creature.getLairLocation(), creature.getOwnerId());

        this.entityData = entityData;

        angerPerSecond = (int) gameSettings.get(MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND).getValue();
        healthPerSecond = (int) gameSettings.get(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND).getValue();
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        CreatureMood mood = entityData.getComponent(creature.getEntityId(), CreatureMood.class);
        return creature.hasLair() && mood != null && mood.other > 0;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        if (!isWorkStepDue(creature, executionDuration)) {
            return;
        }

        EntityId entityId = creature.getEntityId();
        CreatureMood mood = entityData.getComponent(entityId, CreatureMood.class);
        if (mood != null) {
            entityData.setComponent(entityId, mood.add(CreatureMood.REASON_OTHER, angerPerSecond));
        }

        Health health = entityData.getComponent(entityId, Health.class);
        if (health != null && health.health < health.maxHealth) {
            int heal = Math.min(healthPerSecond, health.maxHealth - health.health);
            entityData.setComponent(entityId, new Health(health.health + heal, health.maxHealth));
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.SULK;
    }

}
