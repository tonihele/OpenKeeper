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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Placeable;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerGoldControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LooseObjectSystemTest {

    private static final Point TILE = new Point(36, 16);

    private final EntityData entityData = new DefaultEntityData();
    private final Keeper heroKeeper = new Keeper(false, Player.GOOD_PLAYER_ID);
    private final PlayerGoldControl heroGold = new PlayerGoldControl(heroKeeper);
    private final List<Object> storedInRoom = new ArrayList<>();

    @Test
    void heroTreasuryTakesInTheGoldLyingOnIt() {

        // Level14 places 3000 gold on the hero Treasury, DK II counts it as the heroes' gold
        heroGold.setGoldMax(9000);
        EntityId gold = addObject(ObjectType.GOLD);
        entityData.setComponent(gold, new Gold(3000, 2000));

        newSystem().processTick(0.25f);

        assertNull(entityData.getComponent(gold, ObjectComponent.class));
        assertEquals(List.of(3000), storedInRoom);
        assertEquals(3000, heroKeeper.getGold());
    }

    @Test
    void heroRoomsLeaveOtherObjectsLying() {
        EntityId crate = addObject(ObjectType.CRATE);

        newSystem().processTick(0.25f);

        assertNotNull(entityData.getComponent(crate, ObjectComponent.class));
        assertTrue(storedInRoom.isEmpty());
    }

    private EntityId addObject(ObjectType objectType) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new ObjectComponent((short) 1, objectType));
        entityData.setComponent(entityId, new Owner(Player.NEUTRAL_PLAYER_ID, Player.NEUTRAL_PLAYER_ID));
        entityData.setComponent(entityId, new Position(0, WorldUtils.pointToVector3f(TILE)));
        entityData.setComponent(entityId, new Placeable());
        return entityId;
    }

    /**
     * The system on a map with one hero room, that has space for any object
     */
    private LooseObjectSystem newSystem() {
        IRoomObjectControl<?> storage = stub(IRoomObjectControl.class, (name, args) -> switch (name) {
            case "isFullCapacity" ->
                false;
            case "addItem" -> {
                storedInRoom.add(args[0]);
                yield args[0] instanceof Integer ? 0 : null;
            }
            default ->
                throw new UnsupportedOperationException(name);
        });
        IRoomController room = stub(IRoomController.class, (name, args) -> switch (name) {
            case "hasObjectControl" ->
                true;
            case "getObjectControl" ->
                storage;
            case "getOwnerId" ->
                Player.GOOD_PLAYER_ID;
            default ->
                throw new UnsupportedOperationException(name);
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> {
            if (name.equals("getRoomControllerByCoordinates")) {
                return room;
            }
            throw new UnsupportedOperationException(name);
        });
        IMapTileInformation tile = stub(IMapTileInformation.class, (name, args) -> switch (name) {
            case "getOwnerId" ->
                Player.GOOD_PLAYER_ID;
            case "getLocation" ->
                TILE;
            default ->
                throw new UnsupportedOperationException(name);
        });
        IEntityPositionLookup positions = stub(IEntityPositionLookup.class, (name, args) -> {
            if (name.equals("getEntityLocation")) {
                return tile;
            }
            throw new UnsupportedOperationException(name);
        });
        IPlayerController heroes = stub(IPlayerController.class, (name, args) -> {
            if (name.equals("getGoldControl")) {
                return heroGold;
            }
            throw new UnsupportedOperationException(name);
        });
        return new LooseObjectSystem(entityData, mapController, Map.of(Player.GOOD_PLAYER_ID, heroes), positions);
    }

    private interface Answer {

        Object answer(String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

}
