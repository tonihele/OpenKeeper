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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DungeonHeart;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A living Dungeon Heart calls for help. Every fourth game tick it looks for
 * an enemy creature within the level's heart reporting distance of its centre,
 * 4 tiles in the original levels, and sends its keeper's creatures within the
 * heart's creature gathering distance, 7 tiles, to where that enemy is. Imps
 * go too (DKII.EXE 0x4D9690). Which creatures answer is up to
 * {@link toniarts.openkeeper.game.controller.creature.ICreatureController#answerHeartCall(Point)}.
 */
public final class DungeonHeartCallSystem implements IGameLogicUpdatable {

    private static final int CALL_INTERVAL_TICKS = 4;

    private final EntityData entityData;
    private final ILevelInfo levelInfo;
    private final IEntityPositionLookup entityPositionLookup;
    private final ICreaturesController creaturesController;
    private final IGameTimer gameTimer;
    private final int reportingDistance;
    private final int gatheringDistance;
    private final EntitySet hearts;

    /**
     * The Dungeon Heart's call for help
     *
     * @param entityData the entity data
     * @param gameSettings the level variables, for the two distances
     * @param levelInfo the level, for the ticks and who is whose enemy
     * @param entityPositionLookup finds the creatures around the heart
     * @param creaturesController the creatures that are called
     * @param gameTimer the game time
     */
    public DungeonHeartCallSystem(EntityData entityData, Map<MiscType, Variable.MiscVariable> gameSettings, ILevelInfo levelInfo,
            IEntityPositionLookup entityPositionLookup, ICreaturesController creaturesController, IGameTimer gameTimer) {
        this.entityData = entityData;
        this.levelInfo = levelInfo;
        this.entityPositionLookup = entityPositionLookup;
        this.creaturesController = creaturesController;
        this.gameTimer = gameTimer;
        reportingDistance = (int) gameSettings.get(MiscType.DUNGEON_HEART_REPORTING_DISTANCE_TILES).getValue();
        gatheringDistance = (int) gameSettings.get(MiscType.DUNGEON_HEART_CREATURE_GATHERING_DISTANCE_TILES).getValue();

        hearts = entityData.getEntities(DungeonHeart.class, RoomComponent.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        hearts.applyChanges();

        // The hearts take turns by their id, like the original's rooms
        long tick = Math.round(gameTimer.getGameTime() * levelInfo.getLevelData().getGameLevel().getTicksPerSec());
        for (Entity heart : hearts) {
            RoomComponent room = heart.get(RoomComponent.class);
            if (!room.destroyed && (tick + heart.getId().getId()) % CALL_INTERVAL_TICKS == 0) {
                call(room.location, heart.get(Owner.class).ownerId);
            }
        }
    }

    private void call(Point centre, short ownerId) {
        Keeper keeper = levelInfo.getPlayer(ownerId);
        Point enemyTile = null;
        for (EntityId creature : getCreaturesWithin(centre, reportingDistance)) {
            Owner owner = entityData.getComponent(creature, Owner.class);
            Health health = entityData.getComponent(creature, Health.class);
            if (owner != null && keeper != null && keeper.isEnemy(owner.ownerId) && health != null && health.health > 0
                    && entityData.getComponent(creature, Unconscious.class) == null
                    && entityData.getComponent(creature, InHand.class) == null) {
                enemyTile = WorldUtils.vectorToPoint(entityData.getComponent(creature, Position.class).position);
                break;
            }
        }
        if (enemyTile == null) {
            return;
        }

        for (EntityId creature : getCreaturesWithin(centre, gatheringDistance)) {
            Owner owner = entityData.getComponent(creature, Owner.class);
            if (owner != null && owner.ownerId == ownerId && owner.controlId == ownerId) {
                creaturesController.createController(creature).answerHeartCall(enemyTile);
            }
        }
    }

    private List<EntityId> getCreaturesWithin(Point centre, int distance) {
        List<EntityId> creatures = new ArrayList<>();
        Vector3f centrePosition = WorldUtils.pointToVector3f(centre);
        for (int x = centre.x - distance; x <= centre.x + distance; x++) {
            for (int y = centre.y - distance; y <= centre.y + distance; y++) {
                for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    Position position = entityData.getComponent(entityId, Position.class);
                    if (position != null && entityData.getComponent(entityId, CreatureComponent.class) != null
                            && WorldUtils.vector3fToVector2f(position.position).distanceSquared(centrePosition.x, centrePosition.z)
                            < distance * distance) {
                        creatures.add(entityId);
                    }
                }
            }
        }

        return creatures;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        hearts.release();
    }

}
