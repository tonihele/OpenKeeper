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
package toniarts.openkeeper.game.controller.creature;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Call to Arms banner in the middle of a Dungeon Heart, where the computer
 * player puts it as DK II does, can't be stood on here. The creatures make for
 * the nearest tile of the heart instead
 */
class CreatureCallToArmsTest {

    @Test
    void answersABannerInTheMiddleOfTheHeartAtTheNearestTileOfTheHeart() {
        EntityData entityData = new DefaultEntityData();
        RoomInstance room = new RoomInstance(new Room());
        for (int x = 10; x < 15; x++) {
            for (int y = 10; y < 15; y++) {
                room.addCoordinate(new Point(x, y));
            }
        }
        FiveByFiveRotatedController heart = new FiveByFiveRotatedController(entityData.createEntity(), entityData, null, room, null,
                Map.of(MiscType.MAX_GOLD_PER_DUNGEON_HEART_TILE, CreatureSleepFixtures.variable(MiscType.MAX_GOLD_PER_DUNGEON_HEART_TILE, 100)),
                null);
        IMapController mapController = (IMapController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> {
                    if (!"getRoomControllerByCoordinates".equals(method.getName())) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return room.hasCoordinate((Point) args[0]) ? heart : null;
                });

        // No way onto the heart's middle, a straight one anywhere else
        INavigationService navigationService = (INavigationService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{INavigationService.class}, (proxy, method, args) -> {
                    if (!"findPath".equals(method.getName())) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    Point to = (Point) args[1];
                    if (room.hasCoordinate(to) && !heart.isTileAccessible(null, to)) {
                        return null;
                    }
                    DefaultGraphPath<IMapTileInformation> path = new DefaultGraphPath<>();
                    path.add(tile((Point) args[0]));
                    path.add(tile(to));
                    return path;
                });

        EntityId banner = entityData.createEntity();
        entityData.setComponent(banner, new Position(0, WorldUtils.pointToVector3f(11, 11)));
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new Owner(Player.KEEPER1_ID, Player.KEEPER1_ID),
                new Position(0, WorldUtils.pointToVector3f(2, 2)));
        CreatureController creature = new CreatureController(entityId, entityData, new Creature(), navigationService, null, null, Map.of(),
                null, null, mapController, null, null, null, null);

        assertTrue(creature.answerCallToArms(banner));
        assertEquals(new Point(11, 10), entityData.getComponent(entityId, Navigation.class).target);
    }

    private IMapTileInformation tile(Point p) {
        return (IMapTileInformation) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> {
                    if (!"getLocation".equals(method.getName())) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return p;
                });
    }

}
