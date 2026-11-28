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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules are the ones of DKII.EXE's wind shot handler (0x4AE990)
 */
class WindShotProcessorTest {

    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;
    private static final short TROLL_ID = 10;
    private static final int TROLL_HEALTH = 600;
    private static final int MAP_SIZE = 10;

    /**
     * The Wind spell's shot data: 3 tiles around the shot and no damage
     */
    private static final int RADIUS = 3;
    private static final Vector3f WIND = new Vector3f(5.5f, 1.3f, 5.5f);
    private static final float DELTA = 0.0001f;

    private final EntityData entityData = new DefaultEntityData();
    private final Shot cyclone = ShotFixtures.cyclone();
    private final Creature troll = ShotFixtures.creature(TROLL_ID, EnumSet.noneOf(Creature.CreatureFlag.class),
            EnumSet.noneOf(Creature.CreatureFlag3.class));
    private final List<EntityId> creatures = new ArrayList<>();
    private final Set<EntityId> knockedDown = new HashSet<>();
    private final EntityId fairy = createCreature(PLAYER_ID, WIND);
    private final WindShotProcessor processor;

    WindShotProcessorTest() {
        ShotFixtures.setHealth(troll, TROLL_HEALTH);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" -> troll;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IEntityPositionLookup positionLookup = (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(),
                new Class<?>[]{IEntityPositionLookup.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getEntitiesInLocation" -> creaturesOn((int) args[0], (int) args[1]);
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IGameController gameController = (IGameController) Proxy.newProxyInstance(IGameController.class.getClassLoader(),
                new Class<?>[]{IGameController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getEntityLookupService" -> positionLookup;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(IMapDataInformation.class.getClassLoader(),
                new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getWidth", "getHeight" -> MAP_SIZE;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "createController" -> knockingDown((EntityId) args[0]);
            default -> throw new UnsupportedOperationException(method.getName());
        });
        processor = new WindShotProcessor(entityData, kwdFile, gameController, mapController, creaturesController);
    }

    @Test
    void windKnocksDownTheCreaturesAroundIt() {
        EntityId enemy = createCreature(ENEMY_ID, new Vector3f(7.5f, 1, 5.5f));
        EntityId own = createCreature(PLAYER_ID, new Vector3f(5.5f, 1, 3.5f));

        processor.process(wind(WIND, 0));

        assertEquals(Set.of(enemy, own), knockedDown);
    }

    @Test
    void windLeavesItsCasterAlone() {
        processor.process(wind(WIND, 0));

        assertFalse(knockedDown.contains(fairy));
        assertEquals(0, entityData.getComponent(fairy, Position.class).rotation, DELTA);
    }

    @Test
    void windDoesNotReachBeyondItsRadius() {
        EntityId enemy = createCreature(ENEMY_ID, new Vector3f(5.5f, 1, 2.4f));

        processor.process(wind(WIND, 0));

        assertTrue(knockedDown.isEmpty());
        assertEquals(0, entityData.getComponent(enemy, Position.class).rotation, DELTA);
    }

    @Test
    void windLeavesTheDeadAlone() {
        EntityId corpse = createCreature(ENEMY_ID, new Vector3f(6.5f, 1, 5.5f));
        entityData.removeComponent(corpse, Health.class);

        processor.process(wind(WIND, 0));

        assertTrue(knockedDown.isEmpty());
    }

    @Test
    void windSpinsTheCreaturesCloseToItFaster() {
        EntityId inTheWind = createCreature(ENEMY_ID, WIND);
        EntityId near = createCreature(ENEMY_ID, new Vector3f(6.5f, 1, 5.5f));
        EntityId far = createCreature(ENEMY_ID, new Vector3f(5.5f, 1, 7.5f));

        processor.process(wind(WIND, 0));

        // 0x600 of the 0x800 to a circle in the middle, less by radius / (distance + radius) further out
        assertEquals(-FastMath.TWO_PI * 0.75f, entityData.getComponent(inTheWind, Position.class).rotation, DELTA);
        assertEquals(-FastMath.TWO_PI * 0.75f * 3 / 4, entityData.getComponent(near, Position.class).rotation, DELTA);
        assertEquals(-FastMath.TWO_PI * 0.75f * 3 / 5, entityData.getComponent(far, Position.class).rotation, DELTA);
    }

    @Test
    void windOfTheSpellDoesNoDamage() {
        EntityId enemy = createCreature(ENEMY_ID, new Vector3f(6.5f, 1, 5.5f));

        processor.process(wind(WIND, 0));

        assertNull(entityData.getComponent(enemy, Damage.class));
    }

    @Test
    void windWithDamageHurtsTheOtherPlayersCreaturesByTheirHealth() {
        EntityId inTheWind = createCreature(ENEMY_ID, WIND);
        EntityId far = createCreature(ENEMY_ID, new Vector3f(5.5f, 1, 7.5f));
        EntityId own = createCreature(PLAYER_ID, new Vector3f(6.5f, 1, 5.5f));

        // The shot's own second value is 4: 4 % of the health at the edge, half of that in the middle
        processor.process(wind(WIND, 4));

        assertEquals(12, entityData.getComponent(inTheWind, Damage.class).damage);
        assertEquals(20, entityData.getComponent(far, Damage.class).damage);
        assertNull(entityData.getComponent(own, Damage.class));
        assertTrue(knockedDown.contains(own));
    }

    @Test
    void windAtTheEdgeOfTheMapStaysOnTheMap() {
        EntityId enemy = createCreature(ENEMY_ID, new Vector3f(0.5f, 1, 8.5f));

        processor.process(wind(new Vector3f(1.5f, 1.3f, 9.5f), 0));

        assertTrue(knockedDown.contains(enemy));
    }

    private ShotRequest wind(Vector3f position, int damage) {
        return new ShotRequest(cyclone, RADIUS, damage, PLAYER_ID, position, null, fairy);
    }

    private EntityId createCreature(short ownerId, Vector3f position) {
        EntityId creature = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = TROLL_ID;
        entityData.setComponents(creature, new Position(0, position), creatureComponent, new Health(TROLL_HEALTH, TROLL_HEALTH),
                new Owner(ownerId, ownerId));
        creatures.add(creature);
        return creature;
    }

    private List<EntityId> creaturesOn(int x, int y) {
        if (x < 0 || y < 0 || x >= MAP_SIZE || y >= MAP_SIZE) {
            throw new ArrayIndexOutOfBoundsException(x + ", " + y);
        }

        List<EntityId> result = new ArrayList<>();
        for (EntityId creature : creatures) {
            if (new Point(x, y).equals(WorldUtils.vectorToPoint(entityData.getComponent(creature, Position.class).position))) {
                result.add(creature);
            }
        }
        return result;
    }

    private ICreatureController knockingDown(EntityId creature) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "knockDown" -> knockedDown.add(creature);
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

}
