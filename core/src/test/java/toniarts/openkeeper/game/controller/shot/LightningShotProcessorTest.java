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
package toniarts.openkeeper.game.controller.shot;

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.ShotViewState;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules are the ones of DKII.EXE's lightning shot handler (0x4AF1E0)
 */
class LightningShotProcessorTest {

    private static final double GAME_TIME = 12.5;
    private static final short PLAYER_ID = 3;

    /**
     * The shot data is the damage as it is, 150 for the Thunderbolt
     */
    private static final int DAMAGE = 150;

    private static final short TROLL_ID = 10;
    private static final short STONE_KNIGHT_ID = 28;
    private static final short HORNY_ID = 27;
    private static final Vector3f ON_FLOOR = new Vector3f(3.5f, 1f, 4.5f);
    private static final Vector3f ON_WATER = new Vector3f(6.5f, 1f, 4.5f);

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Short, Creature> creatures = new HashMap<>();
    private final Set<Point> waterTiles = new HashSet<>();
    private final Map<Object, Point> tileLocations = new IdentityHashMap<>();
    private final Map<Point, IMapTileController> tiles = new HashMap<>();
    private final Set<EntityId> knockedDown = new HashSet<>();
    private final LightningShotProcessor processor;
    private final Shot thunderbolt = ShotFixtures.thunderbolt();

    LightningShotProcessorTest() {
        addCreature(TROLL_ID, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class));
        addCreature(STONE_KNIGHT_ID, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.of(Creature.CreatureFlag3.IS_IMMUNE_TO_LIGHTNING));
        addCreature(HORNY_ID, EnumSet.of(Creature.CreatureFlag.IS_HORNY), EnumSet.noneOf(Creature.CreatureFlag3.class));
        waterTiles.add(WorldUtils.vectorToPoint(ON_WATER));

        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" -> creatures.get((Short) args[0]);
            default -> throw new UnsupportedOperationException(method.getName());
        });
        processor = new LightningShotProcessor(entityData, () -> GAME_TIME, kwdFile, map(), creaturesController());
    }

    @Test
    void strikeDamagesAndKnocksDownTheCreature() {
        EntityId creature = createCreature(TROLL_ID, ON_FLOOR);

        processor.process(strike(creature));

        assertEquals(DAMAGE, entityData.getComponent(creature, Damage.class).damage);
        assertTrue(knockedDown.contains(creature));
    }

    @Test
    void upgradedStrikeDamagesTwoHundred() {
        EntityId creature = createCreature(TROLL_ID, ON_FLOOR);

        processor.process(new ShotRequest(thunderbolt, 200, 0, PLAYER_ID, new Vector3f(), creature));

        assertEquals(200, entityData.getComponent(creature, Damage.class).damage);
    }

    @Test
    void strikeOnWaterDoublesTheDamage() {
        EntityId creature = createCreature(TROLL_ID, ON_WATER);

        processor.process(strike(creature));

        assertEquals(2 * DAMAGE, entityData.getComponent(creature, Damage.class).damage);
    }

    @Test
    void someCreaturesAreNeitherHurtNorKnockedDown() {
        EntityId stoneKnight = createCreature(STONE_KNIGHT_ID, ON_FLOOR);

        processor.process(strike(stoneKnight));

        assertNull(entityData.getComponent(stoneKnight, Damage.class));
        assertFalse(knockedDown.contains(stoneKnight));
        assertEquals(1, countShotEntities());
    }

    @Test
    void possessedImprisonedAndHornyStayOnTheirFeet() {
        EntityId possessed = createCreature(TROLL_ID, ON_FLOOR, new Possessed());
        EntityId prisoner = createCreature(TROLL_ID, ON_FLOOR, new CreatureImprisoned());
        EntityId horny = createCreature(HORNY_ID, ON_FLOOR);

        for (EntityId creature : new EntityId[]{possessed, prisoner, horny}) {
            processor.process(strike(creature));

            assertEquals(DAMAGE, entityData.getComponent(creature, Damage.class).damage);
            assertFalse(knockedDown.contains(creature));
        }
    }

    @Test
    void strikeLeavesTheBoltWhereTheTargetStands() {
        EntityId creature = createCreature(TROLL_ID, ON_FLOOR);

        // The cursor is a bit off, the bolt still hits the creature
        processor.process(new ShotRequest(thunderbolt, DAMAGE, 0, PLAYER_ID, new Vector3f(3.9f, 1f, 4.1f), creature));

        EntitySet shots = entityData.getEntities(ShotViewState.class, Position.class, Decay.class);
        assertEquals(1, shots.size());
        Entity shot = shots.iterator().next();
        assertEquals(ShotFixtures.THUNDERBOLT_ID, shot.get(ShotViewState.class).shotId);
        assertEquals(ON_FLOOR, shot.get(Position.class).position);
        assertEquals(GAME_TIME, shot.get(Decay.class).startTime);
        assertEquals(0.8, shot.get(Decay.class).duration, 1e-9);
        shots.release();
    }

    @Test
    void everyCastIsItsOwnStrike() {
        EntityId creature = createCreature(TROLL_ID, ON_FLOOR);

        // Ten casts in a row, as fast as a player clicks
        for (int i = 0; i < 10; i++) {
            processor.process(strike(creature));
        }

        assertEquals(10 * DAMAGE, entityData.getComponent(creature, Damage.class).damage);
        assertEquals(10, countShotEntities());
    }

    @Test
    void somethingElseThanACreatureIsOnlyDamaged() {
        EntityId thing = entityData.createEntity();
        entityData.setComponent(thing, new Position(0, new Vector3f(2.5f, 1f, 2.5f)));

        processor.process(strike(thing));

        assertEquals(DAMAGE, entityData.getComponent(thing, Damage.class).damage);
        assertFalse(knockedDown.contains(thing));
    }

    @Test
    void targetThatIsGoneIsNotStruck() {
        EntityId gone = entityData.createEntity();

        processor.process(strike(gone));
        processor.process(strike(null));

        assertNull(entityData.getComponent(gone, Damage.class));
        assertEquals(0, countShotEntities());
    }

    private void addCreature(short creatureId, EnumSet<Creature.CreatureFlag> flags, EnumSet<Creature.CreatureFlag3> flags3) {
        creatures.put(creatureId, ShotFixtures.creature(creatureId, flags, flags3));
    }

    private EntityId createCreature(short creatureId, Vector3f position, EntityComponent... components) {
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new Position(0, position.clone()), creatureComponent);
        entityData.setComponents(creature, components);
        return creature;
    }

    private ShotRequest strike(EntityId target) {
        return new ShotRequest(thunderbolt, DAMAGE, 0, PLAYER_ID, new Vector3f(), target);
    }

    private int countShotEntities() {
        EntitySet shots = entityData.getEntities(ShotViewState.class);
        int count = shots.size();
        shots.release();
        return count;
    }

    private IMapController map() {
        Terrain floor = TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(), 0, 0, 0, (short) 0);
        Terrain water = TerrainFixtures.terrain((short) 2, TerrainFixtures.flags(Terrain.TerrainFlag.WATER), 0, 0, 0, (short) 0);
        IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(IMapDataInformation.class.getClassLoader(),
                new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getTile" -> tile(args.length == 1 ? (Point) args[0] : new Point((int) args[0], (int) args[1]));
            default -> throw new UnsupportedOperationException(method.getName());
        });

        return (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            case "getTerrain" -> waterTiles.contains(tileLocations.get(args[0])) ? water : floor;
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private IMapTileController tile(Point point) {
        return tiles.computeIfAbsent(point, p -> {
            IMapTileController tile = (IMapTileController) Proxy.newProxyInstance(IMapTileController.class.getClassLoader(),
                    new Class<?>[]{IMapTileController.class}, (proxy, method, args) -> {
                        throw new UnsupportedOperationException(method.getName());
                    });
            tileLocations.put(tile, p);
            return tile;
        });
    }

    private ICreaturesController creaturesController() {
        return (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(), new Class<?>[]{ICreaturesController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "createController" -> knockingDown((EntityId) args[0]);
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private ICreatureController knockingDown(EntityId creature) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "knockDown" -> knockedDown.add(creature);
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

}
