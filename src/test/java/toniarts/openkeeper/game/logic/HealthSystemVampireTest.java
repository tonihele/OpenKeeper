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

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A killed Vampire above level 1 comes back in its keeper's Graveyard one level
 * lower while the Graveyards have a free place (DKII.EXE 0x48FFC0, 0x4DDCA0,
 * 0x48AFA0)
 */
class HealthSystemVampireTest {

    private static final short KEEPER = 3;
    private static final short ENEMY = 4;
    private static final short VAMPIRE_ID = 8;
    private static final Point GRAVEYARD_CENTRE = new Point(11, 21);

    private final EntityData entityData = new DefaultEntityData();
    private final List<List<Object>> levelChanges = new ArrayList<>();
    private final List<CreatureState> states = new ArrayList<>();
    private int graveyardPlaces = 1;

    @Test
    void killedVampireComesBackInTheGraveyardALevelLower() {
        EntityId vampire = createVampire(3, new Owner(KEEPER, KEEPER));
        entityData.setComponent(vampire, new Damage(500));

        createHealthSystem().processTick(0.25f);

        assertEquals(List.of(List.of(vampire, 2, 0)), levelChanges);
        Health health = entityData.getComponent(vampire, Health.class);
        assertEquals(health.maxHealth, health.health);
        Vector3f position = entityData.getComponent(vampire, Position.class).position;
        assertEquals(WorldUtils.pointToVector3f(GRAVEYARD_CENTRE).x, position.x);
        assertEquals(WorldUtils.pointToVector3f(GRAVEYARD_CENTRE).z, position.z);
        assertEquals(List.of(CreatureState.IDLE), states);
        assertNull(entityData.getComponent(vampire, Unconscious.class));
    }

    @Test
    void imprisonedVampireComesBackFree() {
        EntityId vampire = createVampire(2, new Owner(KEEPER, ENEMY));
        entityData.setComponents(vampire, new CreatureImprisoned(0, 0), new Damage(500));

        createHealthSystem().processTick(0.25f);

        assertEquals(List.of(List.of(vampire, 1, 0)), levelChanges);
        assertNull(entityData.getComponent(vampire, CreatureImprisoned.class));
        assertEquals(KEEPER, entityData.getComponent(vampire, Owner.class).controlId);
    }

    @Test
    void levelOneVampireDies() {
        EntityId vampire = createVampire(1, new Owner(KEEPER, KEEPER));
        entityData.setComponent(vampire, new Damage(500));

        createHealthSystem().processTick(0.25f);

        assertTrue(levelChanges.isEmpty());
        assertNotNull(entityData.getComponent(vampire, Unconscious.class));
    }

    @Test
    void vampireDiesWhenTheOtherVampiresFillTheGraveyards() {
        createVampire(5, new Owner(KEEPER, KEEPER));
        createVampire(5, new Owner(ENEMY, ENEMY));
        EntityId vampire = createVampire(3, new Owner(KEEPER, KEEPER));
        entityData.setComponent(vampire, new Damage(500));

        createHealthSystem().processTick(0.25f);

        assertTrue(levelChanges.isEmpty());
        assertNotNull(entityData.getComponent(vampire, Unconscious.class));
    }

    @Test
    void vampireComesBackWhileAPlaceIsLeftBesideTheOtherVampires() {
        graveyardPlaces = 2;
        createVampire(5, new Owner(KEEPER, KEEPER));
        EntityId vampire = createVampire(3, new Owner(KEEPER, KEEPER));
        entityData.setComponent(vampire, new Damage(500));

        createHealthSystem().processTick(0.25f);

        assertEquals(List.of(List.of(vampire, 2, 0)), levelChanges);
    }

    private EntityId createVampire(int level, Owner owner) {
        EntityId vampire = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = VAMPIRE_ID;
        entityData.setComponents(vampire, creatureComponent, owner, new Health(40, 100),
                new CreatureExperience(level, 50, 100, 0, 0), new Position(0, new Vector3f(1, 0, 1)));
        return vampire;
    }

    private HealthSystem createHealthSystem() {
        Creature vampire = ShotFixtures.creature(VAMPIRE_ID,
                EnumSet.of(Creature.CreatureFlag.GENERATE_DEAD_BODY, Creature.CreatureFlag.CAN_BE_RESURECTED),
                EnumSet.noneOf(Creature.CreatureFlag3.class));
        IKwdFile kwdFile = proxy(IKwdFile.class, (method, args) -> method.equals("getCreature") ? vampire : null);
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> method.equals("getLevelData") ? kwdFile : null);
        IMapTileInformation tile = proxy(IMapTileInformation.class, (method, args) -> switch (method) {
            case "getLocation" -> new Point(1, 1);
            case "getTerrainId" -> (short) 0;
            default -> null;
        });
        IEntityPositionLookup positionLookup = proxy(IEntityPositionLookup.class,
                (method, args) -> method.equals("getEntityLocation") ? tile : null);
        RoomInstance roomInstance = new RoomInstance(new Room());
        roomInstance.addCoordinate(GRAVEYARD_CENTRE);
        IRoomObjectControl<?> bodies = proxy(IRoomObjectControl.class,
                (method, args) -> method.equals("getMaxCapacity") ? graveyardPlaces : null);
        IRoomController graveyard = proxy(IRoomController.class, (method, args) -> switch (method) {
            case "getObjectControl" -> bodies;
            case "getRoomInstance" -> roomInstance;
            default -> null;
        });
        IMapController mapController = proxy(IMapController.class, (method, args) -> switch (method) {
            case "getRoomsByFunction" -> KEEPER == (Short) args[1] ? List.of(graveyard) : List.of();
            default -> null;
        });
        StateMachine<?, ?> stateMachine = proxy(StateMachine.class, (method, args) -> {
            if (method.equals("changeState")) {
                states.add((CreatureState) args[0]);
            }
            return null;
        });
        ICreatureController creatureController = proxy(ICreatureController.class,
                (method, args) -> method.equals("getStateMachine") ? stateMachine : null);
        ICreaturesController creaturesController = proxy(ICreaturesController.class, (method, args) -> switch (method) {
            case "levelUpCreature" -> {
                levelChanges.add(List.of(args));
                yield null;
            }
            case "createController" -> creatureController;
            default -> null;
        });

        return new HealthSystem(entityData, positionLookup,
                Map.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, CreatureSleepFixtures.variable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, 10),
                        MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, CreatureSleepFixtures.variable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, -5),
                        MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, CreatureSleepFixtures.variable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, 10)),
                creaturesController, levelInfo, List.of(), mapController, () -> 0.0, proxy(GameEventService.class, (method, args) -> null));
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
