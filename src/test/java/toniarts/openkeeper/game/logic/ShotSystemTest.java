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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotFlight;
import toniarts.openkeeper.game.component.ShotTarget;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.IShotsController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShotSystemTest {

    private static final float DELTA = 0.0001f;
    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;
    private static final short IMP_ID = 1;

    private DefaultEntityData entityData;
    private final Shot fireball = ShotFixtures.fireball();
    private final Shot guidedBolt = ShotFixtures.guidedBolt();
    private final Shot raiseDead = ShotFixtures.raiseDead();
    private final Shot cyclone = ShotFixtures.cyclone();
    private final Map<Short, Shot> shotsById = Map.of(ShotFixtures.FIREBALL_ID, fireball, ShotFixtures.GUIDED_BOLT_ID, guidedBolt,
            ShotFixtures.RAISE_DEAD_ID, raiseDead, ShotFixtures.CYCLONE_ID, cyclone,
            ShotFixtures.GAS_MISSILE_ID, ShotFixtures.gasMissile(), ShotFixtures.GRENADE_ID, ShotFixtures.grenade(),
            ShotFixtures.HAIL_STORM_ID, ShotFixtures.hailStorm());
    private final Creature imp = ShotFixtures.creature(IMP_ID, EnumSet.noneOf(Creature.CreatureFlag.class),
            EnumSet.noneOf(Creature.CreatureFlag3.class));
    private final List<EntityId> creatures = new ArrayList<>();
    private final Set<Point> solidTiles = new HashSet<>();
    private final List<EntityId[]> hits = new ArrayList<>();
    private final List<Vector3f> turns = new ArrayList<>();
    private final List<Object[]> effects = new ArrayList<>();
    private final List<EntityId> areaDamages = new ArrayList<>();
    private final List<Object[]> childShots = new ArrayList<>();
    private ShotSystem shotSystem;
    private double gameTime = 0;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        ShotFixtures.setSize(imp, 0.14f, 0.5f);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getShotById" -> shotsById.get((Short) args[0]);
            case "getGameLevel" -> ShotFixtures.gameLevel(4);
            case "getCreature" -> imp;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IShotsController shotsController = (IShotsController) Proxy.newProxyInstance(IShotsController.class.getClassLoader(),
                new Class<?>[]{IShotsController.class}, (proxy, method, args) -> {
                    if (method.getName().equals("hit")) {
                        hits.add(new EntityId[]{(EntityId) args[0], (EntityId) args[1]});
                        return null;
                    }
                    if (method.getName().equals("fly")) {
                        turns.add(positionOf((EntityId) args[0]).clone());
                        return null;
                    }
                    if (method.getName().equals("damageArea")) {
                        areaDamages.add((EntityId) args[0]);
                        return null;
                    }
                    if (method.getName().equals("createChildShot")) {
                        childShots.add(args);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        IEntityPositionLookup positionLookup = (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(),
                new Class<?>[]{IEntityPositionLookup.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getEntitiesInLocation") && args.length == 2) {
                        return creaturesOn(new Point((Integer) args[0], (Integer) args[1]));
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        FakeMapData mapData = new FakeMapData(10, 10);
        IMapInformation<?> mapInformation = (IMapInformation<?>) Proxy.newProxyInstance(IMapInformation.class.getClassLoader(),
                new Class<?>[]{IMapInformation.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            case "isSolid" -> solidTiles.contains((Point) args[0]);
            default -> throw new UnsupportedOperationException(method.getName());
        });

        GameEventService gameEventService = (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(),
                new Class<?>[]{GameEventService.class}, (proxy, method, args) -> {
                    if (method.getName().equals("emitEffect")) {
                        effects.add(args);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        shotSystem = new ShotSystem(entityData, kwdFile, gameEventService, shotsController, positionLookup, mapInformation, Map.of(),
                () -> gameTime);
    }

    @AfterEach
    void tearDown() {
        shotSystem.stop();
        entityData.close();
    }

    @Test
    void shotFliesItsVelocityEachTurn() {
        EntityId shot = createShot(new Vector3f(1, 1.3f, 1), new Vector3f(0.9f, 0, 0), 0);

        shotSystem.processTick(0.25f);

        assertEquals(1.9f, positionOf(shot).x, DELTA);
        assertEquals(59, entityData.getComponent(shot, ShotFlight.class).health);
    }

    @Test
    void shotHitsAnArmedEnemyTrap() {
        EntityId trap = createTrap(ENEMY_ID, new Vector3f(2, 1, 1), false);
        EntityId shot = createShot(new Vector3f(1.9f, 1.1f, 1), new Vector3f(0.9f, 0, 0), 0);

        shotSystem.processTick(0.25f);

        assertEquals(1, hits.size());
        assertEquals(trap, hits.get(0)[1]);
    }

    @Test
    void shotFliesOverAHiddenTrap() {
        createTrap(ENEMY_ID, new Vector3f(2, 1, 1), true);
        EntityId shot = createShot(new Vector3f(1.9f, 1.1f, 1), new Vector3f(0.9f, 0, 0), 0);

        shotSystem.processTick(0.25f);

        assertTrue(hits.isEmpty());
        assertNotNull(positionOf(shot));
    }

    @Test
    void shotHitsAnEnemyCreatureAndDies() {
        EntityId enemy = createCreature(ENEMY_ID, new Vector3f(2, 1, 1));
        EntityId shot = createShot(new Vector3f(1.8f, 1.3f, 1), new Vector3f(0.9f, 0, 0), 0);

        shotSystem.processTick(0.25f);

        assertEquals(1, hits.size());
        assertEquals(shot, hits.get(0)[0]);
        assertEquals(enemy, hits.get(0)[1]);
        assertNull(positionOf(shot));
    }

    @Test
    void shotDoesNotStepOverACreatureHalfwayAlongItsMove() {
        EntityId enemy = createCreature(ENEMY_ID, new Vector3f(1.45f, 1, 1));
        EntityId shot = createShot(new Vector3f(1, 1.3f, 1), new Vector3f(0.9f, 0, 0), 0);

        shotSystem.processTick(0.25f);
        assertTrue(hits.isEmpty());
        assertEquals(1.45f, positionOf(shot).x, DELTA);

        shotSystem.processTick(0.25f);
        assertEquals(1, hits.size());
        assertEquals(enemy, hits.get(0)[1]);
    }

    @Test
    void hitSetsOffTheShotsHitEffectWhereTheShotIs() {
        createCreature(ENEMY_ID, new Vector3f(2, 1, 1));
        createShot(new Vector3f(1.8f, 1.3f, 1), new Vector3f(0.9f, 0, 0), 0);

        shotSystem.processTick(0.25f);

        assertEquals(1, effects.size());
        assertEquals(ShotFixtures.FIREBALL_HIT_EFFECT_ID, effects.get(0)[0]);
        assertEquals(new Vector3f(1.8f, 1.3f, 1), effects.get(0)[1]);
    }

    @Test
    void shotWithoutAWallEffectSetsOffNothingAtTheWall() {
        solidTiles.add(new Point(2, 1));
        createShot(new Vector3f(1, 1.3f, 1), new Vector3f(0.9f, 0, 0), 0);

        shotSystem.processTick(0.25f);

        assertTrue(effects.isEmpty());
    }

    @Test
    void shotFliesThroughItsOwnersCreatures() {
        createCreature(PLAYER_ID, new Vector3f(2, 1, 1));
        EntityId shot = createShot(new Vector3f(1.8f, 1.3f, 1), new Vector3f(0.9f, 0, 0), 0);

        shotSystem.processTick(0.25f);

        assertTrue(hits.isEmpty());
        assertNotNull(positionOf(shot));
    }

    @Test
    void raiseDeadHitsADeadBodyAndDies() {
        createCreature(ENEMY_ID, new Vector3f(1.5f, 1, 1));
        EntityId deadBody = createCreature(ENEMY_ID, new Vector3f(2, 1, 1));
        entityData.removeComponent(deadBody, Health.class);
        entityData.setComponent(deadBody, new Death(0));
        EntityId shot = createShot(new Vector3f(1.3f, 1.3f, 1), new Vector3f(0.5f, 0, 0), 3);
        entityData.setComponent(shot, new ShotFlight(ShotFixtures.RAISE_DEAD_ID, PLAYER_ID, null, null, 9, 0, new Vector3f(0.5f, 0, 0),
                new Vector3f(1.3f, 1.3f, 1), 3, raiseDead.getHealth()));

        // Through the living on the first turn
        shotSystem.processTick(0.25f);
        assertTrue(hits.isEmpty());

        shotSystem.processTick(0.25f);
        assertEquals(1, hits.size());
        assertEquals(deadBody, hits.get(0)[1]);
        assertNull(positionOf(shot));
    }

    @Test
    void shotAtCreaturesFliesThroughADeadBody() {
        EntityId deadBody = createCreature(ENEMY_ID, new Vector3f(2, 1, 1));
        entityData.removeComponent(deadBody, Health.class);
        entityData.setComponent(deadBody, new Death(0));
        EntityId shot = createShot(new Vector3f(1.8f, 1.3f, 1), new Vector3f(0.9f, 0, 0), 0);

        shotSystem.processTick(0.25f);

        assertTrue(hits.isEmpty());
        assertNotNull(positionOf(shot));
    }

    @Test
    void shotDiesAtAWall() {
        solidTiles.add(new Point(2, 1));
        EntityId shot = createShot(new Vector3f(1, 1.3f, 1), new Vector3f(0.9f, 0, 0), 0);

        shotSystem.processTick(0.25f);

        assertNull(positionOf(shot));
    }

    @Test
    void guidedShotTurnsTowardsItsTargetOnlyOnEveryEighthTurn() {
        EntityId target = createCreature(ENEMY_ID, new Vector3f(5, 1, 4));
        EntityId shot = createGuidedShot(new Vector3f(1, 1.25f, 1), new Vector3f(0.7f, 0, 0), target);

        // Turn 7, the shot flies straight
        gameTime = 7 / 4.0;
        shotSystem.processTick(0.25f);

        assertEquals(new Vector3f(1.7f, 1.25f, 1), positionOf(shot));

        // Turn 8, the shot turns to the middle of its target: 3.3 tiles east and 3 tiles south of it, at its height
        gameTime = 8 / 4.0;
        shotSystem.processTick(0.25f);

        Vector3f velocity = entityData.getComponent(shot, ShotFlight.class).velocity;
        assertEquals(0.7f, velocity.length(), DELTA);
        assertEquals(3.3f / 3, velocity.x / velocity.z, DELTA);
        assertEquals(0, velocity.y, DELTA);
        assertEquals(1.7f + velocity.x, positionOf(shot).x, DELTA);
        assertEquals(1 + velocity.z, positionOf(shot).z, DELTA);
    }

    @Test
    void guidedShotWhoseTargetIsGoneFliesOnStraight() {
        EntityId target = createCreature(ENEMY_ID, new Vector3f(5, 1, 4));
        EntityId shot = createGuidedShot(new Vector3f(1, 1.25f, 1), new Vector3f(0.7f, 0, 0), target);
        creatures.remove(target);
        entityData.removeEntity(target);

        gameTime = 8 / 4.0;
        shotSystem.processTick(0.25f);

        assertEquals(new Vector3f(1.7f, 1.25f, 1), positionOf(shot));
    }

    @Test
    void shotDiesOutOfTheSpellsRange() {
        EntityId shot = createShot(new Vector3f(1, 1.3f, 1), new Vector3f(0.9f, 0, 0), 1.5f);

        shotSystem.processTick(0.25f);
        assertNotNull(positionOf(shot));

        shotSystem.processTick(0.25f);
        shotSystem.processTick(0.25f);
        assertNull(positionOf(shot));
    }

    @Test
    void shotThatWorksEveryTurnHasItsTurnBeforeItMoves() {
        EntityId shot = createShot(new Vector3f(1, 1.3f, 1), new Vector3f(0.25f, 0, 0), 0.4f);
        entityData.setComponent(shot, new ShotFlight(ShotFixtures.CYCLONE_ID, PLAYER_ID, null, null, 3, 0, new Vector3f(0.25f, 0, 0),
                new Vector3f(1, 1.3f, 1), 0.4f, cyclone.getHealth()));

        shotSystem.processTick(0.25f);
        shotSystem.processTick(0.25f);

        assertEquals(List.of(new Vector3f(1, 1.3f, 1), new Vector3f(1.25f, 1.3f, 1)), turns);
    }

    @Test
    void shotThatWorksOnAHitHasNoTurn() {
        createShot(new Vector3f(1, 1.3f, 1), new Vector3f(0.9f, 0, 0), 0);

        shotSystem.processTick(0.25f);

        assertTrue(turns.isEmpty());
    }

    @Test
    void shotDiesWhenItsHealthRunsOut() {
        EntityId shot = createShot(new Vector3f(1, 1.3f, 1), new Vector3f(0, 0, 0), 0);
        entityData.setComponent(shot, new ShotFlight(entityData.getComponent(shot, ShotFlight.class), 2));

        shotSystem.processTick(0.25f);
        assertNotNull(positionOf(shot));

        shotSystem.processTick(0.25f);
        assertNull(positionOf(shot));
        assertTrue(childShots.isEmpty());
    }

    @Test
    void shotDamagesAroundItOnEveryTurnItLives() {
        EntityId shot = createShot(new Vector3f(1, 1.3f, 1), new Vector3f(0.9f, 0, 0), 0);

        shotSystem.processTick(0.25f);
        shotSystem.processTick(0.25f);

        assertEquals(List.of(shot, shot), areaDamages);
    }

    @Test
    void gasMissileLeavesItsCloudWhereItHitsACreature() {
        createCreature(ENEMY_ID, new Vector3f(2, 1, 1));
        EntityId shot = createShot(ShotFixtures.GAS_MISSILE_ID, new Vector3f(1.8f, 1.3f, 1), new Vector3f(0.5f, 0, 0), 0);

        shotSystem.processTick(0.25f);

        assertChildShot(shot, ShotFixtures.GAS_MISSILE_CLOUD_ID, new Vector3f(1.8f, 1.3f, 1));
        assertNull(positionOf(shot));
    }

    @Test
    void gasMissileLeavesItsCloudAtAWall() {
        solidTiles.add(new Point(2, 1));
        EntityId shot = createShot(ShotFixtures.GAS_MISSILE_ID, new Vector3f(1, 1.3f, 1), new Vector3f(0.9f, 0, 0), 0);

        shotSystem.processTick(0.25f);

        // The last step before the wall
        assertChildShot(shot, ShotFixtures.GAS_MISSILE_CLOUD_ID, new Vector3f(1.45f, 1.3f, 1));
        assertNull(positionOf(shot));
    }

    @Test
    void grenadeExplodesWhenItsTimeRunsOut() {
        EntityId shot = createShot(ShotFixtures.GRENADE_ID, new Vector3f(1, 1.3f, 1), new Vector3f(0.3f, 0, 0), 0);
        entityData.setComponent(shot, new ShotFlight(entityData.getComponent(shot, ShotFlight.class), 1));

        shotSystem.processTick(0.25f);

        assertChildShot(shot, ShotFixtures.GRENADE_EXPLOSION_ID, new Vector3f(1.3f, 1.3f, 1));
        assertNull(positionOf(shot));
    }

    @Test
    void grenadeOutOfTheSpellsRangeExplodesAtTheEndOfItsTurn() {
        EntityId shot = createShot(ShotFixtures.GRENADE_ID, new Vector3f(1, 1.3f, 1), new Vector3f(0.3f, 0, 0), 0.5f);

        shotSystem.processTick(0.25f);
        shotSystem.processTick(0.25f);
        assertTrue(childShots.isEmpty());

        shotSystem.processTick(0.25f);
        assertChildShot(shot, ShotFixtures.GRENADE_EXPLOSION_ID, new Vector3f(1.9f, 1.3f, 1));
        assertNull(positionOf(shot));
    }

    @Test
    void hailStormGoesOntoItsTargetAndLeavesItsStormThere() {

        // An enemy where the shot starts isn't hit, the shot hits its target
        createCreature(ENEMY_ID, new Vector3f(1, 1, 1));
        EntityId target = createCreature(ENEMY_ID, new Vector3f(4, 1, 1));
        EntityId shot = createHailStorm(target);

        shotSystem.processTick(0.25f);

        assertEquals(1, hits.size());
        assertEquals(target, hits.get(0)[1]);
        assertChildShot(shot, ShotFixtures.HAIL_STORM_CLOUD_ID, new Vector3f(4, 1, 1));
        assertNull(positionOf(shot));
    }

    @Test
    void hailStormWithoutItsTargetLeavesItsStormWhereItIs() {
        EntityId shot = createHailStorm(null);

        shotSystem.processTick(0.25f);

        assertTrue(hits.isEmpty());
        assertChildShot(shot, ShotFixtures.HAIL_STORM_CLOUD_ID, new Vector3f(1, 1.3f, 1));
        assertNull(positionOf(shot));
    }

    @Test
    void shotCastOnItsShooterHitsOnlyItsShooter() {
        createCreature(ENEMY_ID, new Vector3f(2, 1, 1));
        EntityId shooter = createCreature(ENEMY_ID, new Vector3f(2, 1, 1));
        EntityId shot = entityData.createEntity();
        entityData.setComponents(shot,
                new Position(0, new Vector3f(2, 1.3f, 1)),
                new ShotFlight(ShotFixtures.FIREBALL_ID, PLAYER_ID, shooter, shooter, 50, 0, new Vector3f(), new Vector3f(2, 1.3f, 1),
                        0, fireball.getHealth()));

        shotSystem.processTick(0.25f);

        assertEquals(1, hits.size());
        assertEquals(shooter, hits.get(0)[1]);
    }

    private EntityId createHailStorm(EntityId target) {
        EntityId shot = entityData.createEntity();
        entityData.setComponents(shot,
                new Position(0, new Vector3f(1, 1.3f, 1)),
                new ShotFlight(ShotFixtures.HAIL_STORM_ID, PLAYER_ID, null, target, 10, 2, new Vector3f(), new Vector3f(1, 1.3f, 1),
                        4, 1));
        return shot;
    }

    private void assertChildShot(EntityId shot, short shotTypeId, Vector3f position) {
        assertEquals(1, childShots.size());
        assertEquals(shot, childShots.get(0)[0]);
        assertEquals(shotTypeId, childShots.get(0)[1]);
        Vector3f childPosition = (Vector3f) childShots.get(0)[2];
        assertEquals(position.x, childPosition.x, DELTA);
        assertEquals(position.y, childPosition.y, DELTA);
        assertEquals(position.z, childPosition.z, DELTA);
    }

    private EntityId createShot(Vector3f position, Vector3f velocity, float maxRange) {
        return createShot(ShotFixtures.FIREBALL_ID, position, velocity, maxRange);
    }

    private EntityId createShot(short shotId, Vector3f position, Vector3f velocity, float maxRange) {
        EntityId shot = entityData.createEntity();
        entityData.setComponents(shot,
                new Position(0, new Vector3f(position)),
                new ShotFlight(shotId, PLAYER_ID, null, null, 50, 0, velocity, new Vector3f(position), maxRange,
                        shotsById.get(shotId).getHealth()));
        return shot;
    }

    private EntityId createGuidedShot(Vector3f position, Vector3f velocity, EntityId target) {
        EntityId shot = entityData.createEntity();
        entityData.setComponents(shot,
                new Position(0, new Vector3f(position)),
                new ShotFlight(ShotFixtures.GUIDED_BOLT_ID, PLAYER_ID, null, target, 50, 0, velocity, new Vector3f(position), 0,
                        guidedBolt.getHealth()),
                new ShotTarget(target));
        return shot;
    }

    private EntityId createCreature(short ownerId, Vector3f position) {
        EntityId creature = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = IMP_ID;
        creatureComponent.height = imp.getAttributes().getHeight();
        entityData.setComponents(creature,
                new Position(0, position),
                creatureComponent,
                new Health(100, 100),
                new Owner(ownerId, ownerId));
        creatures.add(creature);
        return creature;
    }

    private EntityId createTrap(short ownerId, Vector3f position, boolean hidden) {
        EntityId trap = entityData.createEntity();
        entityData.setComponents(trap,
                new Position(0, position),
                new TrapViewState((short) 1, false, hidden),
                new Health(100, 100),
                new Owner(ownerId, ownerId));
        creatures.add(trap);
        return trap;
    }

    private List<EntityId> creaturesOn(Point tile) {
        List<EntityId> result = new ArrayList<>();
        for (EntityId creature : creatures) {
            if (tile.equals(WorldUtils.vectorToPoint(positionOf(creature)))) {
                result.add(creature);
            }
        }
        return result;
    }

    private Vector3f positionOf(EntityId entity) {
        Position position = entityData.getComponent(entity, Position.class);
        return position != null ? position.position : null;
    }

}
