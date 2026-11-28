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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A keeper's creature with nothing at all to do walks to its Dungeon Heart:
 * to one of the four tiles two tiles off each axis of the heart's reference
 * tile, which is the one right of and below the middle (DKII.EXE 0x478680,
 * 0x4E4590)
 */
class CreatureHeartWalkTest {

    private static final Point HEART = new Point(12, 12);
    private static final Set<Point> SPOTS = Set.of(new Point(11, 11), new Point(15, 11), new Point(11, 15), new Point(15, 15));

    private final Set<Point> cantStandOn = new HashSet<>();
    private final Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
    private boolean heartDestroyed;
    private EntityData entityData;
    private EntityId entity;
    private CreatureController controller;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        entity = entityData.createEntity();
        entityData.setComponent(entity, new Owner(Player.KEEPER1_ID, Player.KEEPER1_ID));
        entityData.setComponent(entity, new Position(0, WorldUtils.pointToVector3f(1, 1)));
        keeper.setDungeonHeartLocation(HEART);
        controller = controller();
    }

    @Test
    void walksToEachOfTheFourSpotsAtRandom() {
        Set<Point> targets = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            controller.navigateToDungeonHeart();
            targets.add(entityData.getComponent(entity, Navigation.class).target);
        }

        assertEquals(SPOTS, targets);
    }

    @Test
    void staysWhenTheSpotCantBeStoodOn() {

        // Like the spot inside the heart
        cantStandOn.add(new Point(11, 11));
        boolean stayed = false;
        for (int i = 0; i < 200; i++) {
            entityData.removeComponent(entity, Navigation.class);
            controller.navigateToDungeonHeart();
            Navigation navigation = entityData.getComponent(entity, Navigation.class);
            if (navigation == null) {
                stayed = true;
            } else {
                assertTrue(SPOTS.contains(navigation.target));
                assertFalse(cantStandOn.contains(navigation.target));
            }
        }

        assertTrue(stayed);
    }

    @Test
    void staysWithoutAHeart() {
        heartDestroyed = true;
        controller.navigateToDungeonHeart();
        assertNull(entityData.getComponent(entity, Navigation.class));

        heartDestroyed = false;
        keeper.setDungeonHeartLocation(null);
        controller.navigateToDungeonHeart();
        assertNull(entityData.getComponent(entity, Navigation.class));
    }

    @Test
    void onlyAKeepersCreatureThatIsNoWorkerGoesToTheHeart() {
        assertEquals(List.of("navigateToDungeonHeart"), idle(Player.KEEPER1_ID, false));
        assertEquals(List.of("navigateToRandomPoint"), idle(Player.KEEPER1_ID, true));
        assertEquals(List.of("navigateToRandomPoint"), idle(Player.NEUTRAL_PLAYER_ID, false));
        assertEquals(List.of(), idle(Player.GOOD_PLAYER_ID, false));
    }

    /**
     * Lets an idle creature that finds no work choose its walk
     *
     * @return the walks it took
     */
    @SuppressWarnings("unchecked")
    private static List<String> idle(short ownerId, boolean worker) {
        List<String> walks = new ArrayList<>();
        ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getOwnerId" -> ownerId;
                    case "isWorker" -> worker;
                    case "findWork" -> {
                        ((Consumer<Boolean>) args[0]).accept(false);
                        yield null;
                    }
                    case "navigateToDungeonHeart", "navigateToRandomPoint" -> {
                        walks.add(method.getName());
                        yield null;
                    }
                    default -> defaultValue(method.getReturnType());
                });
        CreatureState.IDLE.enter(creature);
        return walks;
    }

    private CreatureController controller() {
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> "getPlayer".equals(method.getName()) ? keeper : null);
        IRoomController heart = (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> "isDestroyed".equals(method.getName())
                ? heartDestroyed : defaultValue(method.getReturnType()));
        FakeMapData mapData = new FakeMapData(30, 30);
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getMapData" -> mapData;
                    case "getRoomControllerByCoordinates" -> HEART.equals(args[0]) ? heart : null;
                    default -> defaultValue(method.getReturnType());
                });
        INavigationService navigationService = (INavigationService) Proxy.newProxyInstance(INavigationService.class.getClassLoader(),
                new Class<?>[]{INavigationService.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isAccessible" -> !cantStandOn.contains(((IMapTileInformation) args[1]).getLocation());
                    case "findPath" -> new DefaultGraphPath<>();
                    default -> defaultValue(method.getReturnType());
                });
        Creature creature = ShotFixtures.creature((short) 1, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class));
        return new CreatureController(entity, entityData, creature, navigationService, null, () -> 100, Map.of(), null, null, mapController, levelInfo, null, null, null);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == float.class) {
            return 0f;
        }
        return 0;
    }

}
