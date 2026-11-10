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
package toniarts.openkeeper.game.task.worker;

import com.jme3.math.Vector3f;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A worker only fetches an object while a room has room for that kind of
 * object, except gold, which it fetches while it can carry more
 */
class FetchObjectTaskTest {

    private static final short PLAYER_ID = 3;

    @Test
    void specialIsFetchedWhileTheLibraryHasRoomForIt() {
        assertTrue(fetch(ObjectType.SPECIAL, List.of(room(false))).isValid(null));
    }

    @Test
    void specialIsLeftWhenTheLibraryHasNoRoomForIt() {

        // The library's researchers don't fill it up, its own storage for the specials counts
        assertFalse(fetch(ObjectType.SPECIAL, List.of(room(true))).isValid(null));
    }

    @Test
    void goldIsFetchedWhenTheTreasuriesAreFull() {

        // DK II's gold pile job (0x46B6D0) refuses only a full worker, it doesn't look at the treasuries
        assertTrue(fetch(ObjectType.GOLD, List.of(room(true))).isValid(null));
        assertTrue(fetch(ObjectType.GOLD, List.of()).isValid(null));
    }

    @Test
    void goldIsLeftByAWorkerThatCarriesAllItCan() {
        ICreatureController fullWorker = proxy(ICreatureController.class, method -> switch (method) {
            case "isTooMuchGold" ->
                true;
            default ->
                throw new UnsupportedOperationException(method);
        });

        assertFalse(fetch(ObjectType.GOLD, List.of(room(false))).isValid(fullWorker));
    }

    private static FetchObjectTask fetch(ObjectType type, List<IRoomController> rooms) {
        IMapController mapController = proxy(IMapController.class, method -> switch (method) {
            case "getRoomsByFunction" ->
                rooms;
            default ->
                throw new UnsupportedOperationException(method);
        });
        IObjectController object = proxy(IObjectController.class, method -> switch (method) {
            case "getPosition" ->
                new Vector3f(2, 1, 2);
            case "getType" ->
                type;
            case "isRemoved", "isStoredInRoom", "isDragged" ->
                false;
            case "isPickableByPlayerCreature" ->
                true;
            default ->
                throw new UnsupportedOperationException(method);
        });

        return new FetchObjectTask(null, null, mapController, object, PLAYER_ID);
    }

    private static IRoomController room(boolean storageFull) {
        IRoomObjectControl<?> storage = proxy(IRoomObjectControl.class, method -> switch (method) {
            case "isFullCapacity" ->
                storageFull;
            default ->
                throw new UnsupportedOperationException(method);
        });
        return proxy(IRoomController.class, method -> switch (method) {
            case "isFullCapacity" ->
                false;
            case "getObjectControl" ->
                storage;
            default ->
                throw new UnsupportedOperationException(method);
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.util.function.Function<String, Object> answers) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answers.apply(method.getName()));
    }

}
