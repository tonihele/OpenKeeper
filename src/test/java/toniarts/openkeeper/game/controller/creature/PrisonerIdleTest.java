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
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureHunger;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Food;
import toniarts.openkeeper.game.component.FoodHunt;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureMoodFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An idle prisoner now and then walks to a tile of the cage next to its own
 * (DKII.EXE 0x473350, 0x4731F0, 0x4E50F0). A hungry one goes for the chickens
 * in the Prison instead, and so does every prisoner when food is dropped into
 * the cage (0x473550, 0x473750)
 */
class PrisonerIdleTest {

    private static final int TICKS = 4000;
    private static final short ANGER_NO_FOOD = 400;
    private static final int HP_FROM_CHICKEN = 250;

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId prisoner = entityData.createEntity();
    private final Set<Point> prisonTiles = new HashSet<>();
    private final Set<Point> cageTiles = new HashSet<>();
    private final Map<Point, List<EntityId>> thingsByTile = new HashMap<>();
    private final List<EntityId> eaten = new ArrayList<>();

    PrisonerIdleTest() {

        // A 5 x 4 Prison at 10, 10 with a cage of 3 x 2 in the middle
        for (int x = 10; x < 15; x++) {
            for (int y = 10; y < 14; y++) {
                prisonTiles.add(new Point(x, y));
                if (x > 10 && x < 14 && y > 10 && y < 13) {
                    cageTiles.add(new Point(x, y));
                }
            }
        }
        entityData.setComponent(prisoner, new Owner((short) 1, (short) 3));
    }

    @Test
    void movesOnOneTickInTenToACageTileNextToItsOwn() {
        CreatureController controller = createController();
        Point corner = new Point(11, 11);
        List<Point> destinations = new ArrayList<>();

        for (int tick = 0; tick < TICKS; tick++) {
            entityData.setComponent(prisoner, new Position(0, new Vector3f(corner.x, 1, corner.y)));
            controller.idleInPrison();
            Navigation navigation = entityData.getComponent(prisoner, Navigation.class);
            if (navigation != null) {
                destinations.add(navigation.target);
                entityData.removeComponent(prisoner, Navigation.class);
            }
        }

        // 400 moves expected, the deviation is 19
        assertTrue(destinations.size() > 280 && destinations.size() < 520, "Moved " + destinations.size() + " times");
        assertEquals(Set.of(new Point(12, 11), new Point(11, 12)), new HashSet<>(destinations));
    }

    @Test
    void staysPutWhereNoCageTileIsNextToIt() {
        cageTiles.removeIf(tile -> !tile.equals(new Point(12, 11)));
        CreatureController controller = createController();
        entityData.setComponent(prisoner, new Position(0, new Vector3f(12, 1, 11)));

        for (int tick = 0; tick < TICKS; tick++) {
            controller.idleInPrison();
        }

        assertNull(entityData.getComponent(prisoner, Navigation.class));
    }

    @Test
    void hungryPrisonerGoesToTheNearestFoodInThePrison() {
        CreatureController controller = createController();
        entityData.setComponents(prisoner, new Position(0, new Vector3f(11, 1, 11)), new CreatureHunger(0, 2));
        placeChicken(new Point(13, 11));
        placeChicken(new Point(14, 13));

        for (int tick = 0; tick < TICKS && entityData.getComponent(prisoner, Navigation.class) == null; tick++) {
            controller.idleInPrison();
        }

        assertEquals(new Point(13, 11), entityData.getComponent(prisoner, Navigation.class).target);
        assertTrue(eaten.isEmpty());
    }

    @Test
    void prisonerAfterFoodEatsTheChickenItStandsOnAndIsFull() {
        CreatureController controller = createController();
        entityData.setComponents(prisoner, new Position(0, new Vector3f(11, 1, 11)), new CreatureHunger(0, 2),
                new CreatureMood(0, 1000, 0, 0, 0, 0), new FoodHunt());
        EntityId chicken = placeChicken(new Point(11, 11));

        controller.idleInPrison();

        assertEquals(List.of(chicken), eaten);
        assertEquals(0, entityData.getComponent(prisoner, CreatureHunger.class).amountNeeded);
        assertEquals(-HP_FROM_CHICKEN, entityData.getComponent(prisoner, Damage.class).damage);
        assertEquals(0, entityData.getComponent(prisoner, CreatureMood.class).noFood);
    }

    @Test
    void prisonerThatIsNotHungryLeavesTheChickenAlone() {
        CreatureController controller = createController();
        entityData.setComponents(prisoner, new Position(0, new Vector3f(11, 1, 11)), new CreatureHunger(0, 0));
        placeChicken(new Point(11, 11));

        for (int tick = 0; tick < TICKS; tick++) {
            controller.idleInPrison();
            entityData.removeComponent(prisoner, Navigation.class);
        }

        assertTrue(eaten.isEmpty());
    }

    @Test
    void prisonerSentAfterDroppedFoodEatsItAllThoughItIsNotHungry() {
        CreatureController controller = createController();
        entityData.setComponents(prisoner, new Position(0, new Vector3f(11, 1, 11)), new CreatureHunger(0, 0),
                new CreatureMood(), new FoodHunt());
        EntityId near = placeChicken(new Point(11, 11));
        EntityId far = placeChicken(new Point(13, 12));

        // Eats the one it stands on, then sets off at once for the other
        controller.idleInPrison();
        controller.idleInPrison();
        assertEquals(List.of(near), eaten);
        assertEquals(new Point(13, 12), entityData.getComponent(prisoner, Navigation.class).target);

        // There it eats that one too. With no food left the hunt is over, and it is angry about it
        entityData.removeComponent(prisoner, Navigation.class);
        entityData.setComponent(prisoner, new Position(0, new Vector3f(13, 1, 12)));
        controller.idleInPrison();
        assertEquals(List.of(near, far), eaten);
        controller.idleInPrison();
        assertNull(entityData.getComponent(prisoner, FoodHunt.class));
        assertEquals(CreatureMood.toRuntimeValue(ANGER_NO_FOOD), entityData.getComponent(prisoner, CreatureMood.class).noFood);
    }

    @Test
    void hungryPrisonerWithNoFoodInThePrisonGetsAngryAboutIt() {
        CreatureController controller = createController();
        entityData.setComponents(prisoner, new Position(0, new Vector3f(11, 1, 11)), new CreatureHunger(0, 2),
                new CreatureMood());
        placeChicken(new Point(20, 20));

        for (int tick = 0; tick < TICKS && entityData.getComponent(prisoner, CreatureMood.class).noFood == 0; tick++) {
            controller.idleInPrison();
        }

        // It looked for food instead of moving on, and looks again only after a while
        assertEquals(CreatureMood.toRuntimeValue(ANGER_NO_FOOD), entityData.getComponent(prisoner, CreatureMood.class).noFood);
        assertNull(entityData.getComponent(prisoner, Navigation.class));
        for (int tick = 0; tick < TICKS; tick++) {
            controller.idleInPrison();
            entityData.removeComponent(prisoner, Navigation.class);
        }
        assertEquals(CreatureMood.toRuntimeValue(ANGER_NO_FOOD), entityData.getComponent(prisoner, CreatureMood.class).noFood);
        assertFalse(entityData.getComponent(prisoner, CreatureHunger.class).lastEatAttemptTime < 0);
    }

    private EntityId placeChicken(Point tile) {
        EntityId chicken = entityData.createEntity();
        entityData.setComponent(chicken, new Food());
        thingsByTile.computeIfAbsent(tile, key -> new ArrayList<>()).add(chicken);
        return chicken;
    }

    @Test
    void prisonerOnItsWayIsLeftAlone() {
        List<String> calls = new ArrayList<>();
        ICreatureController walking = proxy(ICreatureController.class, (method, args) -> {
            calls.add(method);
            return false;
        });
        ICreatureController standing = proxy(ICreatureController.class, (method, args) -> {
            calls.add(method);
            return true;
        });

        CreatureState.IMPRISONED.update(walking);
        assertEquals(List.of("isStopped"), calls);

        CreatureState.IMPRISONED.update(standing);
        assertEquals(List.of("isStopped", "isStopped", "idleInPrison"), calls);
    }

    private CreatureController createController() {
        RoomInstance roomInstance = new RoomInstance(new Room());
        roomInstance.addCoordinates(prisonTiles);
        IRoomController prison = proxy(IRoomController.class, (method, args) -> switch (method) {
            case "isTileAccessible" -> cageTiles.contains((Point) args[0]) == cageTiles.contains((Point) args[1]);
            case "getRoomInstance" -> roomInstance;
            default -> null;
        });
        IMapController mapController = proxy(IMapController.class, (method, args) -> method.equals("getRoomControllerByCoordinates")
                && prisonTiles.contains((Point) args[0]) ? prison : null);
        INavigationService navigationService = proxy(INavigationService.class, (method, args) -> {
            DefaultGraphPath<IMapTileInformation> path = new DefaultGraphPath<>();
            for (Point tile : List.of((Point) args[0], (Point) args[1])) {
                path.add(proxy(IMapTileInformation.class, (tileMethod, tileArgs) -> tile));
            }
            return path;
        });
        IEntityPositionLookup entityPositionLookup = proxy(IEntityPositionLookup.class, (method, args) -> switch (method) {
            case "getEntitiesInLocation" -> thingsByTile.getOrDefault((Point) args[0], List.of());
            case "getEntityController" -> proxy(IEntityController.class, (controllerMethod, controllerArgs) -> {
                if (controllerMethod.equals("remove")) {
                    eaten.add((EntityId) args[0]);
                    thingsByTile.values().forEach(things -> things.remove((EntityId) args[0]));
                }
                return null;
            });
            default -> null;
        });
        IKwdFile kwdFile = proxy(IKwdFile.class, (method, args) -> method.equals("getGameLevel") ? CreatureSleepFixtures.level(4) : null);
        Creature creature = CreatureMoodFixtures.withAngerNoFood(CreatureSleepFixtures.sleeper(10), ANGER_NO_FOOD);

        return new CreatureController(prisoner, entityData, creature, navigationService, null, () -> 0, Map.of(), null,
                entityPositionLookup, mapController,
                proxy(ILevelInfo.class, (method, args) -> method.equals("getLevelData") ? kwdFile : null), null, null, null);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

    private interface Answer {

        Object answer(String method, Object[] args);
    }
}
