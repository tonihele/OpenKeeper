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
import java.util.random.RandomGenerator;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Prays in the Temple. Like in DK II, once a second the prayer calms every
 * kind of anger the creature has, heals it and gives its keeper the creature's
 * prayer mana. DK II counts that second down from the ticks per second to 0,
 * so it takes a tick more than a second (DKII.EXE 0x47B8C0, 0x47B970). The creature
 * faces the room's centre and prays 2 or 3 times, then it either stays or moves to
 * another tile (0x472AC0, 0x472D30)
 */
public final class Pray extends AbstractCapacityCriticalRoomTask {

    private static final int[] ANGER_REASONS = {CreatureMood.REASON_GENERAL, CreatureMood.REASON_NO_FOOD,
        CreatureMood.REASON_NO_LAIR, CreatureMood.REASON_NO_WORK, CreatureMood.REASON_NO_PAY, CreatureMood.REASON_OTHER};

    private final EntityData entityData;
    private final PlayerManaControl manaControl;
    private final int angerPerSecond;
    private final int healthPerSecond;
    private final float prayerInterval;
    private final RandomGenerator random;
    private int prayers;
    private Vector2f workPosition;

    /**
     * Praying in the Temple
     *
     * @param navigationService the navigation service
     * @param mapController the map controller
     * @param p the spot to pray at
     * @param playerId the owner of the Temple
     * @param room the Temple
     * @param taskManager the task manager
     * @param entityData the entity data
     * @param gameSettings the level variables
     * @param manaControl the keeper's mana, for the prayer mana
     * @param ticksPerSecond the level's ticks per second
     * @param random the random numbers for the number of prayers and the move
     */
    public Pray(final INavigationService navigationService, final IMapController mapController, Point p, short playerId,
            IRoomController room, TaskManager taskManager, EntityData entityData,
            Map<MiscType, MiscVariable> gameSettings, PlayerManaControl manaControl, int ticksPerSecond,
            RandomGenerator random) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.entityData = entityData;
        this.manaControl = manaControl;
        prayerInterval = (ticksPerSecond + 1) / (float) ticksPerSecond;
        this.random = random;
        prayers = 2 + random.nextInt(2);

        angerPerSecond = (int) gameSettings.get(MiscType.MODIFY_CREATURE_ANGER_WHILE_PRAYING_PER_SECOND).getValue();
        healthPerSecond = (int) gameSettings.get(MiscType.MODIFY_CREATURE_HEALTH_WHILE_PRAYING_PER_SECOND).getValue();
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    public boolean isFaceTarget() {
        return true;
    }

    @Override
    public Point getFaceLocation() {
        return getRoom().getRoomInstance().getCenter();
    }

    @Override
    public Vector2f getWorkPosition(ICreatureController creature) {

        // DK II stands the worshipper a quarter of a tile from the middle of its tile, on the side away from the
        // room's centre. On the centre tile itself it stands on the north side (0x472AC0)
        if (workPosition == null) {
            Vector2f middle = WorldUtils.pointToVector2f(getTaskLocation());
            Vector2f away = middle.subtract(WorldUtils.pointToVector2f(getFaceLocation()));
            if (away.lengthSquared() == 0) {
                away.set(0, -1);
            }
            workPosition = middle.addLocal(away.normalizeLocal().multLocal(WorldUtils.TILE_WIDTH / 4));
        }
        return workPosition;
    }

    @Override
    public boolean moveToOtherSpot(ICreatureController creature, float executionDuration) {

        // DK II prays 2 or 3 times, then picks its spot again: 36 times out of 100 another free tile of the Temple,
        // otherwise the same spot (0x472D30, 0x472AC0). Without the animation the creature stays where it is
        double prayerTime = CreatureController.getAnimationTime(creature.getCreature(), Creature.AnimationType.PRAYING);
        if (prayerTime <= 0 || executionDuration < prayers * prayerTime) {
            return false;
        }
        if (random.nextInt(100) < 36 && taskManager.assignFreeRoomTask(creature, getRoom(), ObjectType.WORSHIPPER)) {
            return true;
        }
        prayers += 2 + random.nextInt(2);
        return false;
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.WORSHIPPER;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        super.executeTask(creature, executionDuration);

        // Once a second and a tick, timed from the start of the prayer
        float lastPrayer = getExecutionDuration(creature);
        if (executionDuration - lastPrayer < prayerInterval) {
            return;
        }
        setExecutionDuration(creature, lastPrayer + prayerInterval);

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
