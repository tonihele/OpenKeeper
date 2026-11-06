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
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Prays in the Temple. Like in DK II, once a second the prayer calms every
 * kind of anger the creature has, heals it and gives its keeper the creature's
 * prayer mana
 */
public final class Pray extends AbstractCapacityCriticalRoomTask {

    private static final int[] ANGER_REASONS = {CreatureMood.REASON_GENERAL, CreatureMood.REASON_NO_FOOD,
        CreatureMood.REASON_NO_LAIR, CreatureMood.REASON_NO_WORK, CreatureMood.REASON_NO_PAY, CreatureMood.REASON_OTHER};

    private final EntityData entityData;
    private final PlayerManaControl manaControl;
    private final int angerPerSecond;
    private final int healthPerSecond;

    public Pray(final INavigationService navigationService, final IMapController mapController, Point p, short playerId,
            IRoomController room, TaskManager taskManager, EntityData entityData,
            Map<MiscType, MiscVariable> gameSettings, PlayerManaControl manaControl) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.entityData = entityData;
        this.manaControl = manaControl;

        angerPerSecond = (int) gameSettings.get(MiscType.MODIFY_CREATURE_ANGER_WHILE_PRAYING_PER_SECOND).getValue();
        healthPerSecond = (int) gameSettings.get(MiscType.MODIFY_CREATURE_HEALTH_WHILE_PRAYING_PER_SECOND).getValue();
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.WORSHIPPER;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // Once a second, timed from the start of the prayer
        float lastPrayer = getExecutionDuration(creature);
        if (executionDuration - lastPrayer < 1.0f) {
            return;
        }
        setExecutionDuration(creature, lastPrayer + 1.0f);

        EntityId entityId = creature.getEntityId();
        CreatureMood mood = entityData.getComponent(entityId, CreatureMood.class);
        if (mood != null) {
            for (int reason : ANGER_REASONS) {
                mood = mood.add(reason, angerPerSecond);
            }
            entityData.setComponent(entityId, mood);
        }

        Health health = entityData.getComponent(entityId, Health.class);
        if (health != null && health.health < health.maxHealth) {
            int heal = Math.min(healthPerSecond, health.maxHealth - health.health);
            entityData.setComponent(entityId, new Health(health.health + heal, health.maxHealth));
        }

        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        if (creatureComponent != null && manaControl != null) {
            manaControl.addMana(creatureComponent.manaGenPrayer);
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.PRAY;
    }

    @Override
    public boolean isRemovable() {
        return true;
    }

}
