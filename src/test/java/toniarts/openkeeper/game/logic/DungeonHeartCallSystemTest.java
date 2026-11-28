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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DungeonHeart;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A living Dungeon Heart sends its keeper's creatures within 7 tiles to an
 * enemy creature that comes within 4 tiles of its centre, every fourth tick
 * (DKII.EXE 0x4D9690)
 */
class DungeonHeartCallSystemTest {

    private static final short KEEPER = 3;
    private static final short ENEMY = 4;
    private static final Point HEART = new Point(20, 20);
    private static final int TICKS_PER_SECOND = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Point, List<EntityId>> entitiesByTile = new HashMap<>();
    private final Map<EntityId, Point> called = new HashMap<>();
    private double gameTime;

    @Test
    void heartSendsTheCreaturesAroundItToTheEnemyNearIt() {
        createHeart(false);
        createCreature(ENEMY, 23, 20, new Health(100, 100));
        EntityId goblin = createCreature(KEEPER, 26, 20, new Health(100, 100));
        EntityId imp = createCreature(KEEPER, 20, 14, new Health(100, 100));
        createCreature(KEEPER, 27, 20, new Health(100, 100));
        createCreature(KEEPER, 25, 25, new Health(100, 100));

        tick();

        // Within 7 tiles of the heart's centre, measured in a circle
        assertEquals(Map.of(goblin, new Point(23, 20), imp, new Point(23, 20)), called);
    }

    @Test
    void enemyFourTilesAwayOrOutColdIsNotReported() {
        createHeart(false);
        createCreature(ENEMY, 24, 20, new Health(100, 100));
        entityData.setComponent(createCreature(ENEMY, 21, 20, new Health(0, 100)), new Unconscious(0));
        createCreature(KEEPER, 21, 21, new Health(100, 100));

        tick();

        assertTrue(called.isEmpty());
    }

    @Test
    void prisonersAndOtherKeepersCreaturesAreNotCalled() {
        createHeart(false);
        createCreature(ENEMY, 22, 20, new Health(100, 100));
        EntityId prisoner = createCreature(KEEPER, 21, 21, new Health(100, 100));
        entityData.setComponent(prisoner, new Owner(KEEPER, ENEMY));

        tick();

        assertTrue(called.isEmpty());
    }

    @Test
    void destroyedHeartCallsNobody() {
        createHeart(true);
        createCreature(ENEMY, 22, 20, new Health(100, 100));
        createCreature(KEEPER, 21, 21, new Health(100, 100));

        tick();

        assertTrue(called.isEmpty());
    }

    @Test
    void heartCallsEveryFourthTick() {
        EntityId heart = createHeart(false);
        createCreature(ENEMY, 22, 20, new Health(100, 100));
        createCreature(KEEPER, 21, 21, new Health(100, 100));
        DungeonHeartCallSystem system = createSystem();

        int calls = 0;
        for (int tick = 0; tick < 8; tick++) {
            gameTime = (double) tick / TICKS_PER_SECOND;
            called.clear();
            system.processTick(0.25f);
            if (!called.isEmpty()) {
                assertEquals(0, (tick + heart.getId()) % 4);
                calls++;
            }
        }

        assertEquals(2, calls);
    }

    private void tick() {

        // On a tick that is the heart's turn
        DungeonHeartCallSystem system = createSystem();
        for (int tick = 0; tick < 4; tick++) {
            gameTime = (double) tick / TICKS_PER_SECOND;
            system.processTick(0.25f);
        }
    }

    private EntityId createHeart(boolean destroyed) {
        EntityId heart = entityData.createEntity();
        entityData.setComponents(heart, new DungeonHeart(), new RoomComponent((short) 5, destroyed, null, HEART), new Owner(KEEPER, KEEPER));
        return heart;
    }

    private EntityId createCreature(short ownerId, int x, int y, Health health) {
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new CreatureComponent(), new Owner(ownerId, ownerId), health, new Position(0, new Vector3f(x, 1, y)));
        entitiesByTile.computeIfAbsent(new Point(x, y), key -> new ArrayList<>()).add(creature);
        return creature;
    }

    private DungeonHeartCallSystem createSystem() {
        Keeper keeper = new Keeper(false, KEEPER);
        IKwdFile kwdFile = proxy(IKwdFile.class, (method, args) -> method.equals("getGameLevel") ? ShotFixtures.gameLevel(TICKS_PER_SECOND) : null);
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> switch (method) {
            case "getPlayer" -> keeper;
            case "getLevelData" -> kwdFile;
            default -> null;
        });
        IEntityPositionLookup positionLookup = proxy(IEntityPositionLookup.class,
                (method, args) -> entitiesByTile.getOrDefault(new Point((int) args[0], (int) args[1]), List.of()));
        ICreaturesController creaturesController = proxy(ICreaturesController.class,
                (method, args) -> proxy(ICreatureController.class, (controllerMethod, controllerArgs) -> {
                    if (controllerMethod.equals("answerHeartCall")) {
                        called.put((EntityId) args[0], (Point) controllerArgs[0]);
                        return true;
                    }
                    return null;
                }));

        return new DungeonHeartCallSystem(entityData,
                Map.of(MiscType.DUNGEON_HEART_REPORTING_DISTANCE_TILES, CreatureSleepFixtures.variable(MiscType.DUNGEON_HEART_REPORTING_DISTANCE_TILES, 4),
                        MiscType.DUNGEON_HEART_CREATURE_GATHERING_DISTANCE_TILES, CreatureSleepFixtures.variable(MiscType.DUNGEON_HEART_CREATURE_GATHERING_DISTANCE_TILES, 7)),
                levelInfo, positionLookup, creaturesController, () -> gameTime);
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
