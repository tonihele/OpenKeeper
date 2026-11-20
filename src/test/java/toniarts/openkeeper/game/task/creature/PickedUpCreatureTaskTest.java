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

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A creature picked up into the keeper's hand loses its position, possibly
 * while its AI is still running (#670)
 */
class PickedUpCreatureTaskTest {

    @Test
    void creatureWithoutPositionCantReachItsLair() {
        IMapTileInformation tile = stub(IMapTileInformation.class, (name, args) -> switch (name) {
            case "getLocation" -> new Point(5, 5);
            case "getX", "getY" -> 5;
            default -> null;
        });
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (name, args) -> switch (name) {
            case "getTile" -> tile;
            case "getWidth", "getHeight" -> 20;
            default -> null;
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getMapData" -> mapData;
            default -> null;
        });
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> switch (name) {
            case "isAccessible" -> true;
            default -> null;
        });
        ICreatureController pickedUp = stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getLairLocation" -> new Point(5, 5);
            case "getOwnerId" -> (short) 3;
            case "hasLair" -> true;
            default -> null; // No position, it is in the hand
        });

        GoToSleep task = new GoToSleep(navigationService, mapController, pickedUp);

        assertFalse(task.isReachable(pickedUp));
    }

    private interface Answer {

        Object answer(String methodName, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

}
