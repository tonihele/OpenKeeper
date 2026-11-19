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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LooseGoldPileTest {

    private static final short PLAYER_ID = 3;
    private static final short OTHER_PLAYER_ID = 4;
    private static final Point TILE = new Point(5, 7);

    private final EntityData entityData = new DefaultEntityData();
    private final List<EntityId> positionedEntities = new ArrayList<>();
    private final IEntityPositionLookup entityPositionLookup = entitiesByTile();

    @Test
    void findsLooseGoldPileOnTile() {
        EntityId pile = addGold(TILE, PLAYER_ID, ObjectType.GOLD);

        assertEquals(pile, GameWorldController.findLooseGoldPile(entityData, entityPositionLookup, TILE));
    }

    @Test
    void ignoresGoldOnOtherTiles() {
        addGold(new Point(6, 7), PLAYER_ID, ObjectType.GOLD);

        assertNull(GameWorldController.findLooseGoldPile(entityData, entityPositionLookup, TILE));
    }

    @Test
    void ignoresGoldStoredInRooms() {
        EntityId pile = addGold(TILE, PLAYER_ID, ObjectType.GOLD);
        entityData.setComponent(pile, new Stored(entityData.createEntity(), ObjectType.GOLD));

        assertNull(GameWorldController.findLooseGoldPile(entityData, entityPositionLookup, TILE));
    }

    @Test
    void findsGoldOfOtherPlayers() {

        // Loose gold belongs to the owner of the land, so the pile owner doesn't matter
        EntityId pile = addGold(TILE, OTHER_PLAYER_ID, ObjectType.GOLD);

        assertEquals(pile, GameWorldController.findLooseGoldPile(entityData, entityPositionLookup, TILE));
    }

    @Test
    void ignoresGoldCarriedByCreatures() {

        // Creatures carry their gold as a component, they are no gold objects
        EntityId creature = entityData.createEntity();
        entityData.setComponent(creature, new Gold(100, 300));
        entityData.setComponent(creature, new Owner(PLAYER_ID, PLAYER_ID));
        entityData.setComponent(creature, new Position(0, WorldUtils.pointToVector3f(TILE)));
        positionedEntities.add(creature);

        assertNull(GameWorldController.findLooseGoldPile(entityData, entityPositionLookup, TILE));
    }

    @Test
    void ignoresOtherObjectsOnTheTile() {
        addGold(TILE, PLAYER_ID, ObjectType.SPELL_BOOK);

        assertNull(GameWorldController.findLooseGoldPile(entityData, entityPositionLookup, TILE));
    }

    private EntityId addGold(Point tile, short ownerId, ObjectType objectType) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new ObjectComponent((short) 1, objectType));
        entityData.setComponent(entityId, new Gold(250, 1000));
        entityData.setComponent(entityId, new Owner(ownerId, ownerId));
        entityData.setComponent(entityId, new Position(0, WorldUtils.pointToVector3f(tile)));
        positionedEntities.add(entityId);
        return entityId;
    }

    /**
     * Stands in for the position system: the entities on a tile
     */
    private IEntityPositionLookup entitiesByTile() {
        return (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(),
                new Class<?>[]{IEntityPositionLookup.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("getEntitiesInLocation") || !(args[0] instanceof Point tile)) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    List<EntityId> result = new ArrayList<>();
                    for (EntityId entityId : positionedEntities) {
                        Position position = entityData.getComponent(entityId, Position.class);
                        if (tile.equals(WorldUtils.vectorToPoint(position.position))) {
                            result.add(entityId);
                        }
                    }
                    return result;
                });
    }

}
