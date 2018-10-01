/*
 * Copyright (C) 2014-2016 OpenKeeper
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

import com.jme3.util.SafeArrayList;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import java.awt.Point;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.GameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.ObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.Player;

/**
 * A simple state to scan the loose gold inside treasuries. The loose gold is
 * added to the treasury automatically if there is some room left
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LooseGoldSystem implements IGameLogicUpdatable {

    private final EntitySet looseGoldEntities;

    private final EntityData entityData;
    private final SafeArrayList<EntityId> looseGoldEntityIds;
    private final IMapController mapController;
    private final Map<Short, IPlayerController> playerControllers;
    private final IEntityPositionLookup entityPositionLookup;

    public LooseGoldSystem(EntityData entityData, IMapController mapController, Map<Short, IPlayerController> playerControllers,
            IEntityPositionLookup entityPositionLookup) {
        this.entityData = entityData;
        this.mapController = mapController;
        this.playerControllers = playerControllers;
        this.entityPositionLookup = entityPositionLookup;

        // TODO: Figure out a better way to flag loose gold
        looseGoldEntities = entityData.getEntities(
                new FieldFilter(ObjectComponent.class, "objectId", ObjectsController.OBJECT_GOLD_ID),
                ObjectComponent.class, Gold.class, Position.class);
        looseGoldEntityIds = new SafeArrayList<>(EntityId.class);
        processAddedEntities(looseGoldEntities);
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // Add new & remove old
        if (looseGoldEntities.applyChanges()) {

            processAddedEntities(looseGoldEntities.getAddedEntities());

            processDeletedEntities(looseGoldEntities.getRemovedEntities());

        }

        // Attach loose gold to rooms
        for (EntityId entityId : looseGoldEntityIds.getArray()) {
            Point point = entityPositionLookup.getEntityLocation(entityId).getLocation();
            IRoomController roomController = mapController.getRoomControllerByCoordinates(point);
            if (roomController != null && roomController.canStoreGold() && !roomController.isFullCapacity()) {
                short ownerId = roomController.getRoomInstance().getOwnerId();
                if (ownerId != Player.GOOD_PLAYER_ID && ownerId != Player.NEUTRAL_PLAYER_ID) {
                    synchronized (GameWorldController.GOLD_LOCK) {
                        Gold gold = entityData.getComponent(entityId, Gold.class);
                        int goldLeft = (int) roomController.getObjectControl(AbstractRoomController.ObjectType.GOLD).addItem(gold.gold, point);
                        playerControllers.get(ownerId).getGoldControl().addGold(gold.gold - goldLeft);
                        if (goldLeft == 0) {
                            entityData.removeEntity(entityId);
                        } else {
                            entityData.setComponent(entityId, new Gold(goldLeft, gold.maxGold));
                        }
                    }
                }
            }
        }
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            int index = Collections.binarySearch(looseGoldEntityIds, entity.getId());
            looseGoldEntityIds.add(~index, entity.getId());
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            int index = Collections.binarySearch(looseGoldEntityIds, entity.getId());
            looseGoldEntityIds.remove(index);
        }
    }


    @Override
    public void start() {

    }

    @Override
    public void stop() {
        looseGoldEntities.release();
        looseGoldEntityIds.clear();
    }

}
