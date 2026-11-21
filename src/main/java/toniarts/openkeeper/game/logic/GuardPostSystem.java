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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A Guard Post reports enemies. On eight game ticks of every sixteen it looks
 * for an enemy creature it can see within the level's guard post reporting
 * distance, 7 tiles in the original levels. It then weighs the threat of that
 * enemy's side within 5 tiles of the enemy against its own keeper's there, and
 * while the enemy has more than three quarters of the keeper's it sends the
 * creatures on guard in the keeper's nearest Guard Room to the enemy, one
 * after another until their threat makes up the difference. It sends nobody
 * while a creature that was called is already on its way to a spot within 5
 * tiles of the enemy, and a keeper without a Guard Room has nobody to send
 * (DKII.EXE 0x4A84E0, 0x4C61C0, 0x4FAC40, 0x4754D0).
 */
public final class GuardPostSystem implements IGameLogicUpdatable {

    /**
     * How far, in tiles, the creatures around the enemy count and how near to
     * it a creature already on its way holds the others back
     */
    private static final int STRENGTH_DISTANCE = 5;

    private final EntityData entityData;
    private final ILevelInfo levelInfo;
    private final IMapController mapController;
    private final IEntityPositionLookup entityPositionLookup;
    private final ICreaturesController creaturesController;
    private final IGameTimer gameTimer;
    private final int reportingDistance;
    private final EntitySet posts;
    private final EntitySet guards;

    /**
     * The Guard Posts' reports
     *
     * @param entityData the entity data
     * @param gameSettings the level variables, for the reporting distance
     * @param levelInfo the level, for the traps, the ticks and who is whose
     * enemy
     * @param mapController the map, for the Guard Rooms
     * @param entityPositionLookup finds the creatures around a spot and tells
     * what the post sees
     * @param creaturesController the creatures that are sent
     * @param gameTimer the game time
     */
    public GuardPostSystem(EntityData entityData, Map<MiscType, Variable.MiscVariable> gameSettings, ILevelInfo levelInfo,
            IMapController mapController, IEntityPositionLookup entityPositionLookup, ICreaturesController creaturesController,
            IGameTimer gameTimer) {
        this.entityData = entityData;
        this.levelInfo = levelInfo;
        this.mapController = mapController;
        this.entityPositionLookup = entityPositionLookup;
        this.creaturesController = creaturesController;
        this.gameTimer = gameTimer;
        reportingDistance = (int) gameSettings.get(MiscType.GUARD_POST_REPORTING_DISTANCE_TILES).getValue();

        posts = entityData.getEntities(TrapViewState.class, Owner.class, Position.class);
        guards = entityData.getEntities(TaskComponent.class, CreatureComponent.class, Owner.class, Threat.class);
    }

    @Override
    public void processTick(float tpf) {
        posts.applyChanges();

        // Every tick, enemy or not: a set keeps the changes it hasn't taken, here every task change of the game
        guards.applyChanges();

        // The posts take turns by their id, eight ticks on and eight off
        long tick = Math.round(gameTimer.getGameTime() * levelInfo.getLevelData().getGameLevel().getTicksPerSec());
        for (Entity post : posts) {
            TrapViewState trap = post.get(TrapViewState.class);
            if (!trap.blueprint && ((tick + post.getId().getId()) & 8) != 0
                    && levelInfo.getLevelData().getTrapById(trap.trapId).getFlags().contains(Trap.TrapFlag.GUARD_POST)) {
                report(post.get(Position.class).position, post.get(Owner.class).ownerId);
            }
        }
    }

    private void report(Vector3f postPosition, short ownerId) {
        Keeper keeper = levelInfo.getPlayer(ownerId);
        IRoomController guardRoom = getNearestGuardRoom(WorldUtils.vectorToPoint(postPosition), ownerId);
        EntityId enemy = keeper != null && guardRoom != null ? findEnemy(postPosition, keeper) : null;
        if (enemy == null) {
            return;
        }

        Vector3f enemyPosition = entityData.getComponent(enemy, Position.class).position;
        Point enemyTile = WorldUtils.vectorToPoint(enemyPosition);
        int threatToMeet = getThreat(entityData.getComponent(enemy, Owner.class).ownerId, enemyPosition)
                - getThreat(ownerId, enemyPosition) * 3 / 4;
        if (threatToMeet <= 0 || isSomebodyOnTheWay(ownerId, enemyTile)) {
            return;
        }

        for (Entity guard : guards) {
            TaskComponent task = guard.get(TaskComponent.class);
            Owner owner = guard.get(Owner.class);
            if (task.taskType == TaskType.GUARD && owner.ownerId == ownerId && owner.controlId == ownerId
                    && !guard.get(CreatureComponent.class).worker && task.targetLocation != null
                    && guardRoom.equals(mapController.getRoomControllerByCoordinates(task.targetLocation))
                    && creaturesController.createController(guard.getId()).answerHeartCall(enemyTile)) {
                threatToMeet -= guard.get(Threat.class).threat;
                if (threatToMeet < 0) {
                    break;
                }
            }
        }
    }

    private IRoomController getNearestGuardRoom(Point postTile, short ownerId) {
        IRoomController nearest = null;
        for (IRoomController room : mapController.getRoomsByFunction(ObjectType.GUARD, ownerId)) {
            if (nearest == null || room.getRoomInstance().getCenter().distance(postTile)
                    < nearest.getRoomInstance().getCenter().distance(postTile)) {
                nearest = room;
            }
        }

        return nearest;
    }

    private EntityId findEnemy(Vector3f postPosition, Keeper keeper) {
        Point postTile = WorldUtils.vectorToPoint(postPosition);
        for (int x = postTile.x - reportingDistance; x <= postTile.x + reportingDistance; x++) {
            for (int y = postTile.y - reportingDistance; y <= postTile.y + reportingDistance; y++) {
                for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    Owner owner = entityData.getComponent(entityId, Owner.class);
                    if (owner != null && keeper.isEnemy(owner.ownerId) && isStanding(entityId)
                            && isWithin(entityId, postPosition, reportingDistance)
                            && entityPositionLookup.hasLineOfSight(postPosition, entityData.getComponent(entityId, Position.class).position)) {
                        return entityId;
                    }
                }
            }
        }

        return null;
    }

    private int getThreat(short ownerId, Vector3f position) {
        int threat = 0;
        Point tile = WorldUtils.vectorToPoint(position);
        for (int x = tile.x - STRENGTH_DISTANCE; x <= tile.x + STRENGTH_DISTANCE; x++) {
            for (int y = tile.y - STRENGTH_DISTANCE; y <= tile.y + STRENGTH_DISTANCE; y++) {
                for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    Owner owner = entityData.getComponent(entityId, Owner.class);
                    Threat creatureThreat = entityData.getComponent(entityId, Threat.class);
                    if (owner != null && owner.ownerId == ownerId && creatureThreat != null && isStanding(entityId)
                            && isWithin(entityId, position, STRENGTH_DISTANCE)) {
                        threat += creatureThreat.threat;
                    }
                }
            }
        }

        return threat;
    }

    private boolean isSomebodyOnTheWay(short ownerId, Point enemyTile) {
        for (EntityId entityId : entityData.findEntities(null, CreatureAi.class, Navigation.class, Owner.class)) {
            Point target = entityData.getComponent(entityId, Navigation.class).target;
            if (entityData.getComponent(entityId, Owner.class).ownerId == ownerId
                    && entityData.getComponent(entityId, CreatureAi.class).getCreatureState() == CreatureState.HEART_CALL
                    && target != null && (target.x - enemyTile.x) * (target.x - enemyTile.x)
                    + (target.y - enemyTile.y) * (target.y - enemyTile.y) < STRENGTH_DISTANCE * STRENGTH_DISTANCE) {
                return true;
            }
        }

        return false;
    }

    private boolean isStanding(EntityId entityId) {
        Health health = entityData.getComponent(entityId, Health.class);
        return entityData.getComponent(entityId, CreatureComponent.class) != null && health != null && health.health > 0
                && entityData.getComponent(entityId, Unconscious.class) == null
                && entityData.getComponent(entityId, InHand.class) == null;
    }

    private boolean isWithin(EntityId entityId, Vector3f position, int distance) {
        Vector3f creaturePosition = entityData.getComponent(entityId, Position.class).position;
        float distanceX = creaturePosition.x - position.x;
        float distanceY = creaturePosition.z - position.z;

        return distanceX * distanceX + distanceY * distanceY < distance * distance;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        posts.release();
        guards.release();
    }

}
