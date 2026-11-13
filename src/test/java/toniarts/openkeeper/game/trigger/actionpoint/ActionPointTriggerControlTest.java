/*
 * Copyright (C) 2026 OpenKeeper
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
package toniarts.openkeeper.game.trigger.actionpoint;

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.logic.PositionSystem;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.game.trigger.TriggerLoader;
import toniarts.openkeeper.tools.convert.map.APCongregateInTriggerFixtures;
import toniarts.openkeeper.tools.convert.map.ActionPointFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionPointTriggerControlTest {

    private static final ActionPoint ACTION_POINT = actionPoint(1, 1, 2, 2);

    @ParameterizedTest
    @CsvSource({
        "LESS_THAN,2,1,true", "LESS_THAN,2,2,false",
        "LESS_OR_EQUAL_TO,2,2,true", "LESS_OR_EQUAL_TO,2,3,false",
        "EQUAL_TO,2,2,true", "EQUAL_TO,2,1,false",
        "GREATER_THAN,2,3,true", "GREATER_THAN,2,2,false",
        "GREATER_OR_EQUAL_TO,2,2,true", "GREATER_OR_EQUAL_TO,2,1,false",
        "NOT_EQUAL_TO,2,1,true", "NOT_EQUAL_TO,2,2,false"
    })
    void appliesEveryComparisonOperatorToTheCurrentOccupancy(String comparisonName,
            int threshold, int count, boolean expected) {
        TriggerGeneric.ComparisonType comparison = TriggerGeneric.ComparisonType.valueOf(comparisonName);
        IKwdFile kwd = APCongregateInTriggerFixtures.congregateIn(comparison,
                (short) 0, (short) 0, (short) 3, threshold);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        for (int i = 0; i < count; i++) {
            fixture.addCreature((short) 1, (short) (10 + i), 1, 1);
        }

        assertTrue(expected == fixture.isActive(), comparisonName + " with count " + count);
    }

    @Test
    void countsOnlyMatchingCreaturesWithinTheInclusiveActionPointRectangle() {
        IKwdFile kwd = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.EQUAL_TO,
                (short) 1, (short) 7, (short) 3, 2);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        fixture.addCreature((short) 1, (short) 7, 1, 1); // top-left edge
        fixture.addCreature((short) 1, (short) 7, 2, 2); // bottom-right edge
        fixture.addCreature((short) 1, (short) 7, 3, 2); // outside
        fixture.addCreature((short) 1, (short) 8, 1, 2); // wrong creature type
        fixture.addCreature((short) 2, (short) 7, 2, 1); // wrong owner

        assertTrue(fixture.isActive());
    }

    @Test
    void typeZeroPreservesTheStockMapCreatureSelectorBehavior() {
        IKwdFile kwd = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.EQUAL_TO,
                (short) 0, (short) 0, (short) 0, 2);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        fixture.addCreature((short) 1, (short) 7, 1, 1);
        fixture.addCreature((short) 2, (short) 8, 2, 2);

        assertTrue(fixture.isActive());
    }

    @Test
    void countsObjectsByOwnerAndObjectDefinitionId() {
        IKwdFile kwd = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.EQUAL_TO,
                (short) 1, (short) 74, (short) 6, 1);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        fixture.addObject((short) 1, (short) 74, 1, 1);
        fixture.addObject((short) 1, (short) 74, 3, 1); // outside
        fixture.addObject((short) 1, (short) 75, 2, 1); // wrong object definition
        fixture.addObject((short) 2, (short) 74, 2, 2); // wrong owner

        assertTrue(fixture.isActive());
    }

    @Test
    void tracksCurrentOccupancyAcrossMovementOwnershipCreationAndRemoval() {
        IKwdFile kwd = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.GREATER_OR_EQUAL_TO,
                (short) 1, (short) 7, (short) 3, 1);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        Occupant creature = fixture.addCreature((short) 1, (short) 7, 0, 0);

        assertFalse(fixture.isActive());
        creature.moveTo(2, 2);
        assertTrue(fixture.isActive());
        creature.ownerId = 2;
        assertFalse(fixture.isActive());
        creature.ownerId = 1;
        assertTrue(fixture.isActive());
        fixture.remove(creature);
        assertFalse(fixture.isActive());

        Occupant replacement = fixture.addCreature((short) 1, (short) 7, 1, 2);
        assertTrue(fixture.isActive());
        fixture.remove(replacement);
        assertFalse(fixture.isActive());
    }

    @Test
    void unsupportedSelectorsMissingComparisonsAndOutOfMapAreasFailClosed() {
        IKwdFile unsupported = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.EQUAL_TO,
                (short) 0, (short) 0, (short) 5, 0);
        Fixture fixture = new Fixture(unsupported, ACTION_POINT, 4, 4);
        fixture.addCreature((short) 1, (short) 7, 1, 1);
        assertFalse(fixture.isActive());

        IKwdFile noComparison = APCongregateInTriggerFixtures.congregateIn(null,
                (short) 0, (short) 0, (short) 3, 0);
        assertFalse(new Fixture(noComparison, ACTION_POINT, 4, 4).isActive());

        IKwdFile missingCount = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.EQUAL_TO,
                (short) 0, (short) 0, (short) 3, null);
        assertFalse(new Fixture(missingCount, ACTION_POINT, 4, 4).isActive());

        IKwdFile outsideMap = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.EQUAL_TO,
                (short) 0, (short) 0, (short) 3, 0);
        Fixture outside = new Fixture(outsideMap, actionPoint(4, 4, 4, 4), 4, 4);
        assertDoesNotThrow(() -> assertFalse(outside.isActive()));
        assertTrue(outside.spatialLookup.queriedTiles.isEmpty());
    }

    @Test
    void actionPointRejectsReversedOrUnrepresentableRectangles() {
        Thing.ActionPoint reversed = ActionPointFixtures.rectangle(1, 2, 1, 1, 2, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new ActionPoint(reversed));

        Thing.ActionPoint tooLarge = ActionPointFixtures.rectangle(1, 0, 0,
                Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new ActionPoint(tooLarge));
    }

    @Test
    void activatesTheExistingChildActionWhenTheThresholdIsReached() {
        IKwdFile kwd = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.GREATER_OR_EQUAL_TO,
                (short) 1, (short) 7, (short) 3, 1, true);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        ActionPointTriggerControl control = fixture.control();

        control.update(0.25f);
        control.update(0.25f);
        assertFalse(fixture.flags.containsKey((short) 2));

        fixture.addCreature((short) 1, (short) 7, 1, 1);
        control.update(0.25f);
        control.update(0.25f);
        assertTrue(fixture.flags.get((short) 2) == 1);
    }

    @Test
    void usesThePositionSystemForObjectPresenceMovementOwnershipAndRemoval() {
        IKwdFile kwd = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.EQUAL_TO,
                (short) 1, (short) 74, (short) 6, 1);
        Fixture triggerFixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        TriggerGenericData condition = (TriggerGenericData) new TriggerLoader(kwd).load(1).getChild(0);

        try (PositionSystemFixture spatial = new PositionSystemFixture(4, 4)) {
            ActionPointTriggerControl control = new ActionPointTriggerControl(null, triggerFixture.levelInfo,
                    null, spatial.mapController, null, 1, triggerFixture.ap, spatial.positionSystem);
            EntityId object = spatial.addObject((short) 1, (short) 74, 0, 0);
            spatial.tick();
            assertFalse(control.isActive(condition));

            spatial.move(object, 2, 2);
            spatial.tick();
            assertTrue(control.isActive(condition));

            spatial.setOwner(object, (short) 2);
            assertFalse(control.isActive(condition));
            spatial.setOwner(object, (short) 1);
            assertTrue(control.isActive(condition));

            spatial.move(object, 3, 2);
            spatial.tick();
            assertFalse(control.isActive(condition));

            spatial.remove(object);
            spatial.tick();
            assertFalse(control.isActive(condition));
        }
    }

    @Test
    void evaluatesParsedCongregateInRecordsFromRetailCampaignMapsWhenAvailable() throws IOException {
        String gameDirectory = System.getProperty("openkeeper.dk2.game", System.getenv("OPENKEEPER_DK2_GAME"));
        Assumptions.assumeTrue(gameDirectory != null && !gameDirectory.isBlank(),
                "Set OPENKEEPER_DK2_GAME to the retail DK2 install directory to run this integration test");

        Map<String, Integer> expectedRecords = Map.of("Level15a", 14, "Level20", 8, "Secret1", 3);
        for (Map.Entry<String, Integer> mapEntry : expectedRecords.entrySet()) {
            Path mapFile = Path.of(gameDirectory, "Data", "Editor", "Maps", mapEntry.getKey() + ".kwd");
            Assumptions.assumeTrue(Files.isRegularFile(mapFile), "Missing stock map " + mapFile);
            IKwdFile kwd = KwdFile.load(gameDirectory, mapFile);
            int width = kwd.getMap().getWidth();
            int height = kwd.getMap().getHeight();
            int recordCount = 0;

            for (Thing.ActionPoint thing : kwd.getThings(Thing.ActionPoint.class)) {
                if (thing.getTriggerId() == 0) {
                    continue;
                }
                ActionPoint ap = new ActionPoint(thing);
                TriggerGenericData tree = new TriggerLoader(kwd).load(thing.getTriggerId());
                List<TriggerGenericData> congregateTriggers = new ArrayList<>();
                collectCongregateTriggers(tree, congregateTriggers);

                for (TriggerGenericData trigger : congregateTriggers) {
                    recordCount++;
                    short targetType = trigger.getUserData("targetType", short.class);
                    assertTrue(targetType == 0 || targetType == 3 || targetType == 6,
                            mapEntry.getKey() + " trigger " + trigger.getId() + " selector " + targetType);
                    int matchingCount = countThatSatisfies(trigger);
                    assertTrue(matchingCount <= 64, "Unexpectedly large stock trigger count in " + mapEntry.getKey());

                    IKwdFile levelData = kwd;
                    Fixture fixture = new Fixture(levelData, ap, width, height);
                    Point location = ap.getStart();
                    short playerId = trigger.getUserData("playerId", short.class);
                    short targetId = trigger.getUserData("targetId", short.class);
                    for (int i = 0; i < matchingCount; i++) {
                        short owner = playerId == 0 ? 1 : playerId;
                        if (targetType == 6) {
                            fixture.addObject(owner, targetId == 0 ? (short) 74 : targetId, location.x, location.y);
                        } else {
                            fixture.addCreature(owner, targetId == 0 ? (short) 7 : targetId, location.x, location.y);
                        }
                    }

                    ActionPointTriggerControl control = fixture.control(thing.getTriggerId());
                    assertTrue(control.isActive(trigger), mapEntry.getKey() + " trigger " + trigger.getId());
                }
            }

            assertEquals(mapEntry.getValue(), recordCount, "AP_CONGREGATE_IN records in " + mapEntry.getKey());
        }
    }

    private static void collectCongregateTriggers(TriggerGenericData trigger, List<TriggerGenericData> result) {
        if (trigger.getType() == TriggerGeneric.TargetType.AP_CONGREGATE_IN) {
            result.add(trigger);
        }
        for (var child : trigger.getChildren()) {
            if (child instanceof TriggerGenericData generic) {
                collectCongregateTriggers(generic, result);
            }
        }
    }

    private static int countThatSatisfies(TriggerGenericData trigger) {
        int threshold = trigger.getUserData("value", int.class);
        return switch (trigger.getComparison()) {
            case LESS_THAN -> threshold - 1;
            case LESS_OR_EQUAL_TO, EQUAL_TO, GREATER_OR_EQUAL_TO -> threshold;
            case GREATER_THAN -> threshold + 1;
            case NOT_EQUAL_TO -> threshold == 0 ? 1 : 0;
            case NONE -> throw new AssertionError("Stock AP_CONGREGATE_IN record has no comparison");
        };
    }

    private static ActionPoint actionPoint(int startX, int startY, int endX, int endY) {
        return new ActionPoint(ActionPointFixtures.rectangle(1, startX, startY, endX, endY, 0, 0));
    }

    private static final class Fixture {

        private final IKwdFile kwd;
        private final ActionPoint ap;
        private final SpatialLookup spatialLookup = new SpatialLookup();
        private final Map<Short, Integer> flags = new HashMap<>();
        private final IMapController mapController;
        private final ILevelInfo levelInfo;

        private Fixture(IKwdFile kwd, ActionPoint ap, int width, int height) {
            this.kwd = kwd;
            this.ap = ap;

            IMapDataInformation mapData = (IMapDataInformation) Proxy.newProxyInstance(
                    IMapDataInformation.class.getClassLoader(), new Class<?>[]{IMapDataInformation.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getWidth" -> width;
                        case "getHeight" -> height;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                    new Class<?>[]{IMapController.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getMapData")) {
                            return mapData;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
            levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                    new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getLevelData" -> kwd;
                        case "getFlag" -> flags.getOrDefault(((Number) args[0]).shortValue(), 0);
                        case "setFlag" -> {
                            flags.put(((Number) args[0]).shortValue(), ((Number) args[1]).intValue());
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private Occupant addCreature(short ownerId, short creatureId, int x, int y) {
            Occupant occupant = new Occupant(ownerId, creatureId, x, y);
            Creature definition = APCongregateInTriggerFixtures.creatureDefinition(creatureId);
            occupant.controller = Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                    new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getOwnerId" -> occupant.ownerId;
                        case "getCreature" -> definition;
                        default -> defaultValue(method.getReturnType());
                    });
            spatialLookup.occupants.add(occupant);
            return occupant;
        }

        private Occupant addObject(short ownerId, short objectId, int x, int y) {
            Occupant occupant = new Occupant(ownerId, objectId, x, y);
            occupant.controller = Proxy.newProxyInstance(IObjectController.class.getClassLoader(),
                    new Class<?>[]{IObjectController.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getOwnerId" -> occupant.ownerId;
                        case "getObjectId" -> occupant.targetId;
                        default -> defaultValue(method.getReturnType());
                    });
            spatialLookup.occupants.add(occupant);
            return occupant;
        }

        private void remove(Occupant occupant) {
            spatialLookup.occupants.remove(occupant);
        }

        private ActionPointTriggerControl control() {
            return control(1);
        }

        private ActionPointTriggerControl control(int triggerId) {
            return new ActionPointTriggerControl(null, levelInfo, null, mapController,
                    (ICreaturesController) null, triggerId, ap, spatialLookup.lookup);
        }

        private boolean isActive() {
            ActionPointTriggerControl control = control();
            TriggerGenericData root = new TriggerLoader(kwd).load(1);
            return control.isActive((TriggerGenericData) root.getChild(0));
        }
    }

    private static final class SpatialLookup {

        private final List<Occupant> occupants = new ArrayList<>();
        private final List<String> queriedTiles = new ArrayList<>();
        private final IEntityPositionLookup lookup = (IEntityPositionLookup) Proxy.newProxyInstance(
                IEntityPositionLookup.class.getClassLoader(), new Class<?>[]{IEntityPositionLookup.class},
                (proxy, method, args) -> {
                    if (!method.getName().equals("getEntityTypesInLocation")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    int x = (Integer) args[0];
                    int y = (Integer) args[1];
                    queriedTiles.add(x + "," + y);
                    Class<?> type = (Class<?>) args[2];
                    List<Object> result = new ArrayList<>();
                    for (Occupant occupant : occupants) {
                        if (occupant.x == x && occupant.y == y && type.isInstance(occupant.controller)) {
                            result.add(occupant.controller);
                        }
                    }
                    return result;
                });
    }

    private static final class PositionSystemFixture implements AutoCloseable {

        private final DefaultEntityData entityData = new DefaultEntityData();
        private final IMapController mapController;
        private final PositionSystem positionSystem;
        private final Map<EntityId, IObjectController> objectControllers = new HashMap<>();
        private final IObjectsController objectsController;

        private PositionSystemFixture(int width, int height) {
            IMapTileInformation[][] tiles = new IMapTileInformation[width][height];
            IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(
                    IMapDataInformation.class.getClassLoader(), new Class<?>[]{IMapDataInformation.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getWidth" -> width;
                        case "getHeight" -> height;
                        case "getTile" -> args.length == 1
                                ? tile(tiles, ((Point) args[0]).x, ((Point) args[0]).y)
                                : tile(tiles, (Integer) args[0], (Integer) args[1]);
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                    new Class<?>[]{IMapController.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getMapData")) {
                            return mapData;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
            objectsController = (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                    new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "isValidEntity" -> objectControllers.containsKey(args[0]);
                        case "createController" -> objectControllers.get(args[0]);
                        default -> defaultValue(method.getReturnType());
                    });
            positionSystem = new PositionSystem(mapController, entityData, null, null, objectsController);
        }

        private EntityId addObject(short ownerId, short objectId, int x, int y) {
            EntityId entityId = entityData.createEntity();
            IObjectController controller = (IObjectController) Proxy.newProxyInstance(
                    IObjectController.class.getClassLoader(), new Class<?>[]{IObjectController.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getOwnerId" -> entityData.getComponent(entityId, Owner.class).ownerId;
                        case "getObjectId" -> entityData.getComponent(entityId, ObjectComponent.class).objectId;
                        default -> defaultValue(method.getReturnType());
                    });
            objectControllers.put(entityId, controller);
            entityData.setComponents(entityId, new Position(0, WorldUtils.pointToVector3f(x, y)),
                    new Owner(ownerId, ownerId), new ObjectComponent(objectId, null));
            return entityId;
        }

        private void move(EntityId entityId, int x, int y) {
            entityData.setComponent(entityId, new Position(0, WorldUtils.pointToVector3f(x, y)));
        }

        private void setOwner(EntityId entityId, short ownerId) {
            entityData.setComponent(entityId, new Owner(ownerId, ownerId));
        }

        private void remove(EntityId entityId) {
            entityData.removeEntity(entityId);
            objectControllers.remove(entityId);
        }

        private void tick() {
            positionSystem.processTick(0.25f);
        }

        private static IMapTileInformation tile(IMapTileInformation[][] tiles, int x, int y) {
            if (x < 0 || y < 0 || x >= tiles.length || y >= tiles[x].length) {
                return null;
            }
            if (tiles[x][y] == null) {
                tiles[x][y] = (IMapTileInformation) Proxy.newProxyInstance(
                        IMapTileInformation.class.getClassLoader(), new Class<?>[]{IMapTileInformation.class},
                        (proxy, method, args) -> switch (method.getName()) {
                            case "getX" -> x;
                            case "getY" -> y;
                            default -> defaultValue(method.getReturnType());
                        });
            }
            return tiles[x][y];
        }

        @Override
        public void close() {
            positionSystem.stop();
            entityData.close();
        }
    }

    private static final class Occupant {

        private short ownerId;
        private final short targetId;
        private int x;
        private int y;
        private Object controller;

        private Occupant(short ownerId, short targetId, int x, int y) {
            this.ownerId = ownerId;
            this.targetId = targetId;
            this.x = x;
            this.y = y;
        }

        private void moveTo(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        return null;
    }
}
