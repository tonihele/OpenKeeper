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

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Jack in the Box that goes off hurts the enemy on it and turns into Jack,
 * who walks to its keeper's Dungeon Heart and dies there (DKII.EXE 0x4B02E0,
 * 0x46E820)
 */
class JackInTheBoxTest {

    private static final short JACK_SHOT_ID = 64;
    private static final short JACK_ID = 46;
    private static final short KEEPER = 3;
    private static final short ENEMY = 4;
    private static final int DAMAGE = 10000;
    private static final Point HEART = new Point(20, 20);

    private final EntityData entityData = new DefaultEntityData();
    private final Keeper keeper = new Keeper(false, KEEPER);
    private final List<EntityId> removed = new ArrayList<>();
    private final List<Object> spawned = new ArrayList<>();
    private final List<CreatureState> states = new ArrayList<>();
    private final List<Point> destinations = new ArrayList<>();
    private EntityId jack;

    @Test
    void goesOffUnderAnEnemyAndTurnsIntoJack() {
        EntityId trap = createThing(5, 5, new TrapComponent((short) 16), new Owner(KEEPER, KEEPER));
        EntityId victim = createThing(5, 5, new CreatureComponent(), new Owner(ENEMY, ENEMY), new Health(500, 500));

        createShotsController().createShot(JACK_SHOT_ID, DAMAGE, 2, KEEPER, WorldUtils.pointToVector3f(5, 5), victim, null);

        assertEquals(DAMAGE, entityData.getComponent(victim, Damage.class).damage);
        assertEquals(List.of(trap), removed);
        assertEquals(List.of(JACK_ID, KEEPER), spawned);
        assertEquals(List.of(CreatureState.JACK_IN_THE_BOX), states);
        assertEquals(CreatureState.JACK_IN_THE_BOX, entityData.getComponent(jack, CreatureAi.class).getCreatureState());
    }

    @Test
    void staysShutUnderItsOwnKeepersCreature() {
        createThing(5, 5, new TrapComponent((short) 16), new Owner(KEEPER, KEEPER));
        EntityId imp = createThing(5, 5, new CreatureComponent(), new Owner(KEEPER, KEEPER), new Health(500, 500));

        createShotsController().createShot(JACK_SHOT_ID, DAMAGE, 2, KEEPER, WorldUtils.pointToVector3f(5, 5), imp, null);

        assertNull(entityData.getComponent(imp, Damage.class));
        assertTrue(removed.isEmpty());
        assertTrue(spawned.isEmpty());
    }

    @Test
    void jackLosesHealthOnItsWayToTheTileDiagonallyPastTheHeartsMiddle() {
        keeper.setDungeonHeartLocation(HEART);
        EntityId creature = createThing(5, 5, new CreatureComponent(), new Owner(KEEPER, KEEPER), new Health(240, 240));

        createCreatureController(creature).walkToHeartToDie();

        assertEquals(1, entityData.getComponent(creature, Damage.class).damage);
        assertEquals(List.of(new Point(HEART.x + 1, HEART.y + 1)), destinations);
    }

    @Test
    void jackDiesFastOnceItStandsAtTheHeart() {
        keeper.setDungeonHeartLocation(HEART);
        EntityId creature = createThing(HEART.x + 1, HEART.y + 1, new CreatureComponent(), new Owner(KEEPER, KEEPER), new Health(100, 240));

        createCreatureController(creature).walkToHeartToDie();

        assertEquals(51, entityData.getComponent(creature, Damage.class).damage);
        assertTrue(destinations.isEmpty());
    }

    private EntityId createThing(int x, int y, EntityComponent... components) {
        EntityId thing = entityData.createEntity();
        entityData.setComponents(thing, components);
        entityData.setComponent(thing, new Position(0, WorldUtils.pointToVector3f(x, y)));
        return thing;
    }

    private ICreatureController createCreatureController(EntityId creature) {
        INavigationService navigationService = proxy(INavigationService.class, (method, args) -> {
            destinations.add((Point) args[1]);
            return null;
        });
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> method.equals("getPlayer") ? keeper : null);
        return new CreatureController(creature, entityData, TrapFiringFixtures.creature(JACK_ID), navigationService, null, () -> 0,
                Map.of(), null, null, null, levelInfo, null, null, null);
    }

    private ShotsController createShotsController() {
        Shot shot = ShotFixtures.shot(JACK_SHOT_ID, Shot.ProcessType.JACK_IN_THE_BOX, 1, EnumSet.of(Shot.ShotProcessFlag.AT_START));
        IKwdFile kwdFile = proxy(IKwdFile.class, (method, args) -> method.equals("getShotById") ? shot : null);
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
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> method.equals("getPlayer") ? keeper : null);
        StateMachine<?, ?> stateMachine = proxy(StateMachine.class, (method, args) -> {
            if (method.equals("changeState")) {
                states.add((CreatureState) args[0]);
            }
            return null;
        });
        ICreatureController jackController = proxy(ICreatureController.class,
                (method, args) -> method.equals("getStateMachine") ? stateMachine : null);
        ICreaturesController creaturesController = proxy(ICreaturesController.class, (method, args) -> switch (method) {
            case "spawnCreature" -> {
                spawned.add(args[0]);
                spawned.add(args[1]);
                jack = entityData.createEntity();
                yield jack;
            }
            case "createController" -> jackController;
            default -> null;
        });

        return new ShotsController(kwdFile, entityData, Map.of(), () -> 0, gameController, null, levelInfo, null, creaturesController,
                proxy(GameEventService.class, (method, args) -> null));
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
