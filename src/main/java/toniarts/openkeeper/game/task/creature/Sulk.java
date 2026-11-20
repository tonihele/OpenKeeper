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

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Sulks in the lair. Like in DK II, once a second the lair calms the creature
 * and heals it as if it slept there, and the creature gets over it when the
 * anger the lair calms is gone. A creature that is unhappy for another reason
 * walks to its lair all the same and gives up when it gets there.<br>
 * Like DK II's sulking state (0x482F80), the creature stands for 5 to 8
 * seconds, sulks out loud when it starts, every 4 seconds and when it is done,
 * then steps to a new spot 0.4 tiles from the middle of its lair, in a random
 * direction. Each time it sulks out loud, but the last, it plays its angry
 * animation once, and while it stands it turns on the spot by up to 90 degrees
 * every 2 seconds, counted back from the step
 */
public final class Sulk extends AbstractTileTask {

    private static final int STAND_TIME = 5;
    private static final int STAND_TIME_RANDOM = 4;
    private static final int SOUND_INTERVAL = 4;
    private static final int TURN_INTERVAL = 2;
    private static final float SPOT_DISTANCE = 0.4f;

    private final EntityData entityData;
    private final int angerPerSecond;
    private final int healthPerSecond;
    private Vector2f spot;
    private float standEnd = -1;
    private float nextSound;
    private float nextTurn;

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
        if (!creature.hasLair() || mood == null) {
            return false;
        }

        // Like in DK II, a creature unhappy for any reason walks to its lair to sulk (0x482EF0),
        // and the anger the lair calms is only asked for once it is there (0x476FC0, 0x47AC70)
        TaskComponent task = entityData.getComponent(creature.getEntityId(), TaskComponent.class);
        return mood.other > 0 || task == null || !task.taskStarted;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    public Vector2f getWorkPosition(ICreatureController creature) {
        return spot != null ? spot : getTarget(creature);
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        sulk(creature, executionDuration);
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

    private void sulk(ICreatureController creature, float executionDuration) {
        if (standEnd < 0) {
            int standTime = STAND_TIME + FastMath.rand.nextInt(STAND_TIME_RANDOM);
            standEnd = executionDuration + standTime;
            nextSound = standEnd - SOUND_INTERVAL;
            nextTurn = standEnd - standTime / TURN_INTERVAL * TURN_INTERVAL;
            sulkOutLoud(creature);
        } else if (executionDuration >= standEnd) {
            creature.fireEvent(GameEventType.CREATURE_SULK, getTaskLocation());
            float angle = FastMath.rand.nextFloat() * FastMath.TWO_PI;
            spot = getTarget(creature).addLocal(FastMath.cos(angle) * SPOT_DISTANCE, FastMath.sin(angle) * SPOT_DISTANCE);
            standEnd = -1;
            creature.navigateToAssignedTask();
            return;
        } else if (executionDuration >= nextSound) {
            sulkOutLoud(creature);
            nextSound = standEnd;
        }

        if (executionDuration >= nextTurn) {
            nextTurn += TURN_INTERVAL;
            Position position = entityData.getComponent(creature.getEntityId(), Position.class);
            if (position != null) {
                float rotation = (position.rotation + FastMath.rand.nextFloat() * FastMath.HALF_PI) % FastMath.TWO_PI;
                entityData.setComponent(creature.getEntityId(), new Position(rotation, position.position));
            }
        }
    }

    private void sulkOutLoud(ICreatureController creature) {
        creature.fireEvent(GameEventType.CREATURE_SULK, getTaskLocation());
        creature.playAnimation(Creature.AnimationType.ANGRY);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.SULK;
    }

}
