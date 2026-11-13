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
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.JailBreaker;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.RoomPrisonerControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A hero that frees its friends on a jail break frees the friendly prisoners
 * when it stands in an enemy Prison.
 */
class JailBreakSystemTest {

    private static final short GOOD = 1;
    private static final short KEEPER = 4;
    private static final short OTHER_KEEPER = 5;
    private static final short ALLY = 6;
    private static final Point TILE = new Point(4, 4);

    private EntityData entityData;
    private RoomPrisonerControl prison;
    private IRoomController room;
    private short prisonOwner;
    private JailBreakSystem system;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        prisonOwner = KEEPER;
        createPrison();
        system = new JailBreakSystem(entityData, levelInfo(), mapController());
    }

    @Test
    void heroInEnemyPrisonFreesFriendlyPrisoners() {
        EntityId hero = createHero(GOOD, TILE);
        EntityId first = imprison(GOOD);
        EntityId second = imprison(GOOD);

        tick(0.25f);

        assertNull(entityData.getComponent(first, Stored.class));
        assertNull(entityData.getComponent(first, CreatureImprisoned.class));
        assertEquals(GOOD, entityData.getComponent(first, Owner.class).controlId);
        assertNull(entityData.getComponent(second, Stored.class));
        assertNotNull(entityData.getComponent(hero, JailBreaker.class));
    }

    @Test
    void enemyPrisonersAreLeftInside() {
        createHero(GOOD, TILE);
        EntityId prisoner = imprison(OTHER_KEEPER);

        tick(0.25f);

        assertNotNull(entityData.getComponent(prisoner, Stored.class));
    }

    @Test
    void heroWithoutTheFlagDoesNothing() {
        EntityId hero = createHero(GOOD, TILE);
        entityData.removeComponent(hero, JailBreaker.class);
        EntityId prisoner = imprison(GOOD);

        tick(0.25f);

        assertNotNull(entityData.getComponent(prisoner, Stored.class));
    }

    @Test
    void workersDoNothing() {
        EntityId hero = createHero(GOOD, TILE);
        CreatureComponent worker = entityData.getComponent(hero, CreatureComponent.class);
        worker.worker = true;
        entityData.setComponent(hero, worker);
        EntityId prisoner = imprison(GOOD);

        tick(0.25f);

        assertNotNull(entityData.getComponent(prisoner, Stored.class));
    }

    @Test
    void heroOutsideThePrisonDoesNothing() {
        createHero(GOOD, new Point(9, 9));
        EntityId prisoner = imprison(GOOD);

        tick(0.25f);

        assertNotNull(entityData.getComponent(prisoner, Stored.class));
    }

    @Test
    void heroInItsOwnPrisonDoesNothing() {
        prisonOwner = GOOD;
        createHero(GOOD, TILE);
        EntityId prisoner = imprison(GOOD);

        tick(0.25f);

        assertNotNull(entityData.getComponent(prisoner, Stored.class));
    }

    @Test
    void imprisonedHeroDoesNothing() {
        EntityId hero = createHero(GOOD, TILE);
        entityData.setComponent(hero, new CreatureImprisoned(0, 0));
        EntityId prisoner = imprison(GOOD);

        tick(0.25f);

        assertNotNull(entityData.getComponent(prisoner, Stored.class));
    }

    @Test
    void torturedHeroDoesNothing() {
        EntityId hero = createHero(GOOD, TILE);
        entityData.setComponent(hero, new CreatureTortured(0, 0, 0));
        EntityId prisoner = imprison(GOOD);

        tick(0.25f);

        assertNotNull(entityData.getComponent(prisoner, Stored.class));
    }

    @Test
    void unconsciousHeroDoesNothing() {
        EntityId hero = createHero(GOOD, TILE);
        entityData.setComponent(hero, new Unconscious(0));
        EntityId prisoner = imprison(GOOD);

        tick(0.25f);

        assertNotNull(entityData.getComponent(prisoner, Stored.class));
    }

    @Test
    void friendBehindAnEnemyPrisonerIsFreed() {
        createHero(GOOD, TILE);
        EntityId enemy = imprison(OTHER_KEEPER);
        EntityId friend = imprison(GOOD);

        tick(0.25f);

        assertNotNull(entityData.getComponent(enemy, Stored.class));
        assertNull(entityData.getComponent(friend, Stored.class));
    }

    @Test
    void prisonerOfAnAllyIsFreed() {
        createHero(GOOD, TILE);
        EntityId prisoner = imprison(ALLY);

        tick(0.25f);

        assertNull(entityData.getComponent(prisoner, Stored.class));
    }

    @Test
    void prisonerThatIsNoLongerImprisonedIsSkipped() {
        createHero(GOOD, TILE);
        EntityId released = imprison(GOOD);
        entityData.removeComponent(released, CreatureImprisoned.class);
        EntityId prisoner = imprison(GOOD);

        tick(0.25f);

        assertNotNull(entityData.getComponent(released, Stored.class));
        assertNull(entityData.getComponent(prisoner, Stored.class));
    }

    @Test
    void allFriendlyPrisonersAreFreedOnTheSameTick() {
        createHero(GOOD, TILE);
        EntityId first = imprison(GOOD);
        EntityId enemy = imprison(OTHER_KEEPER);
        EntityId second = imprison(GOOD);

        tick(0.25f);

        assertNull(entityData.getComponent(first, Stored.class));
        assertNotNull(entityData.getComponent(enemy, Stored.class));
        assertNull(entityData.getComponent(second, Stored.class));
    }


    private void tick(float tpf) {
        system.processTick(tpf);
    }

    private EntityId createHero(short owner, Point tile) {
        EntityId hero = entityData.createEntity();
        entityData.setComponent(hero, new Owner(owner, owner));
        entityData.setComponent(hero, new Position(0, new Vector3f(tile.x, 0, tile.y)));
        entityData.setComponent(hero, new Health(100, 100));
        entityData.setComponent(hero, new CreatureComponent());
        entityData.setComponent(hero, new JailBreaker());
        return hero;
    }

    private EntityId imprison(short owner) {
        EntityId prisoner = entityData.createEntity();
        entityData.setComponent(prisoner, new Owner(owner, prisonOwner));
        entityData.setComponent(prisoner, new CreatureImprisoned(0, 0));
        entityData.setComponent(prisoner, new CreatureAi(0, CreatureState.IMPRISONED, (short) 5));
        prison.addItem(prisoner, TILE);
        return prisoner;
    }

    private void createPrison() {
        RoomInstance roomInstance = new RoomInstance(new Room());
        roomInstance.addCoordinate(TILE);
        EntityId roomEntity = entityData.createEntity();
        room = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> roomEntity;
                    case "getOwnerId" -> prisonOwner;
                    case "getRoomInstance" -> roomInstance;
                    case "hasObjectControl" -> args[0] == ObjectType.PRISONER;
                    case "getObjectControl" -> prison;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        prison = new RoomPrisonerControl(null, room, entityData, () -> 10) {
            @Override
            protected Collection<Point> getCoordinates() {
                return roomInstance.getCoordinates();
            }

            @Override
            protected int getNumberOfAccessibleTiles() {
                return 4;
            }
        };
    }

    private IMapController mapController() {
        return (IMapController) Proxy.newProxyInstance(
                IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getRoomControllerByCoordinates")) {
                        return TILE.equals(args[0]) ? room : null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private ILevelInfo levelInfo() {
        Keeper good = new Keeper(false, GOOD);
        Keeper keeper = new Keeper(false, KEEPER);
        Keeper other = new Keeper(false, OTHER_KEEPER);
        Keeper ally = new Keeper(false, ALLY);
        keeper.createAlliance(OTHER_KEEPER);
        good.createAlliance(ALLY);
        return (ILevelInfo) Proxy.newProxyInstance(
                ILevelInfo.class.getClassLoader(), new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getPlayer")) {
                        short id = (short) args[0];
                        return id == GOOD ? good : id == KEEPER ? keeper : id == ALLY ? ally : other;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
