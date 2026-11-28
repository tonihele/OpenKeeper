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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Boulder;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotFlight;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.tools.convert.map.VariableFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Boulder Trap that fires turns into a boulder with the level's boulder
 * health, on its way to its victim's tile (DKII.EXE 0x4AEBD0)
 */
class BoulderTrapTest {

    private static final short KEEPER = 3;
    private static final short ENEMY = 4;
    private static final int BOULDER_HEALTH = 4000;
    private static final float DELTA = 0.0001f;

    private final EntityData entityData = new DefaultEntityData();
    private final List<EntityId> removed = new ArrayList<>();
    private final List<EntityId> boulders = new ArrayList<>();
    private final ShotsController shotsController = createShotsController();

    @Test
    void firedTrapTurnsIntoABoulderHeadingForItsVictim() {
        EntityId trap = createTrap(5, 5);
        EntityId victim = createVictim(8, 5);

        // A line of sight trap sends its shot flying
        assertTrue(shotsController.fireShot(ShotFixtures.BOULDER_ID, 1, 0, KEEPER, new Vector3f(5, 1, 5), victim, trap, 0));

        assertEquals(List.of(trap), removed);
        assertEquals(1, boulders.size());
        Health health = entityData.getComponent(boulders.getFirst(), Health.class);
        assertEquals(BOULDER_HEALTH, health.health);
        assertEquals(BOULDER_HEALTH, health.maxHealth);
        Boulder boulder = entityData.getComponent(boulders.getFirst(), Boulder.class);
        assertEquals(1, boulder.directionX, DELTA);
        assertEquals(0, boulder.directionY, DELTA);
        assertEquals(1, boulder.turnX);
        assertEquals(0, boulder.turnY);
    }

    @Test
    void theShotThatReachesTheVictimMakesNoSecondBoulder() {
        EntityId trap = createTrap(5, 5);
        EntityId victim = createVictim(5, 8);
        shotsController.fireShot(ShotFixtures.BOULDER_ID, 1, 0, KEEPER, new Vector3f(5, 1, 5), victim, trap, 0);

        shotsController.hit(entityData.findEntity(null, ShotFlight.class), victim);

        assertEquals(1, boulders.size());
    }

    @Test
    void boulderSetsOffAtAnAngleToAVictimOffTheLine() {
        EntityId trap = createTrap(5, 5);
        EntityId victim = createVictim(8, 1);

        shotsController.fireShot(ShotFixtures.BOULDER_ID, 1, 0, KEEPER, new Vector3f(5, 1, 5), victim, trap, 0);

        Boulder boulder = entityData.getComponent(boulders.getFirst(), Boulder.class);
        assertEquals(0.6f, boulder.directionX, DELTA);
        assertEquals(-0.8f, boulder.directionY, DELTA);
        assertEquals(1, boulder.turnX);
        assertEquals(-1, boulder.turnY);
    }

    private EntityId createTrap(int x, int y) {
        EntityId trap = entityData.createEntity();
        entityData.setComponents(trap, new TrapComponent((short) 2), new Owner(KEEPER, KEEPER), new Position(0, WorldUtils.pointToVector3f(x, y)));
        return trap;
    }

    private EntityId createVictim(int x, int y) {
        EntityId victim = entityData.createEntity();
        entityData.setComponents(victim, new CreatureComponent(), new Owner(ENEMY, ENEMY), new Health(100, 100),
                new Position(0, WorldUtils.pointToVector3f(x, y)));
        return victim;
    }

    private ShotsController createShotsController() {
        Shot shot = ShotFixtures.boulder();
        IKwdFile kwdFile = proxy(IKwdFile.class, (method, args) -> switch (method) {
            case "getShotById" -> shot;
            case "getGameLevel" -> ShotFixtures.gameLevel(4);
            default -> null;
        });
        IObjectsController objectsController = proxy(IObjectsController.class, (method, args) -> {
            EntityId boulder = entityData.createEntity();
            if (method.equals("loadObject") && (short) args[0] == ObjectsController.OBJECT_BOULDER_ID && (short) args[1] == KEEPER) {
                entityData.setComponent(boulder, new Position(0, WorldUtils.pointToVector3f((int) args[2], (int) args[3])));
                boulders.add(boulder);
            }
            return boulder;
        });
        IEntityPositionLookup entityPositionLookup = proxy(IEntityPositionLookup.class, (method, args) -> switch (method) {
            case "getEntitiesInLocation" -> entityData.findEntities(null, Position.class).stream()
                    .filter(entity -> args[0].equals(WorldUtils.vectorToPoint(entityData.getComponent(entity, Position.class).position)))
                    .toList();
            case "getEntityController" -> proxy(IEntityController.class, (controllerMethod, controllerArgs) -> {
                if (controllerMethod.equals("remove")) {
                    removed.add((EntityId) args[0]);
                }
                return null;
            });
            default -> null;
        });
        IGameController gameController = proxy(IGameController.class,
                (method, args) -> method.equals("getEntityLookupService") ? entityPositionLookup : null);

        return new ShotsController(kwdFile, entityData,
                Map.of(MiscType.BOULDER_INITIAL_HEALTH, VariableFixtures.miscVariable(MiscType.BOULDER_INITIAL_HEALTH, BOULDER_HEALTH)),
                () -> 0, gameController, null, null, objectsController, null, proxy(GameEventService.class, (method, args) -> null));
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
