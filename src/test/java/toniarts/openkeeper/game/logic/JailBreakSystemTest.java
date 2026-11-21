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
import toniarts.openkeeper.game.component.PrisonEscape;
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
 * A hero that frees its friends on a jail break is sent to do so when it
 * stands in an enemy Prison that holds friends of the hero.
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
    void heroInEnemyPrisonStartsTheJailBreak() {
        EntityId hero = createHero(GOOD, TILE);
        EntityId prisoner = imprison(GOOD);

        tick(0.25f);

        assertEquals(CreatureState.JAIL_BREAK, entityData.getComponent(hero, CreatureAi.class).getTargetState());
        assertEquals(CreatureState.WANDER, entityData.getComponent(hero, CreatureAi.class).getCreatureState());

        // The prisoners are let out by the hero, from the door
        assertNotNull(entityData.getComponent(prisoner, Stored.class));
        assertNotNull(entityData.getComponent(prisoner, CreatureImprisoned.class));
    }

    @Test
    void enemyPrisonersAreLeftInside() {
        EntityId hero = createHero(GOOD, TILE);
        imprison(OTHER_KEEPER);

        tick(0.25f);

        assertNoJailBreak(hero);
    }

    @Test
    void heroWithoutTheFlagDoesNothing() {
        EntityId hero = createHero(GOOD, TILE);
        entityData.removeComponent(hero, JailBreaker.class);
        imprison(GOOD);

        tick(0.25f);

        assertNoJailBreak(hero);
    }

    @Test
    void workersDoNothing() {
        EntityId hero = createHero(GOOD, TILE);
        CreatureComponent worker = entityData.getComponent(hero, CreatureComponent.class);
        worker.worker = true;
        entityData.setComponent(hero, worker);
        imprison(GOOD);

        tick(0.25f);

        assertNoJailBreak(hero);
    }

    @Test
    void heroOutsideThePrisonDoesNothing() {
        EntityId hero = createHero(GOOD, new Point(9, 9));
        imprison(GOOD);

        tick(0.25f);

        assertNoJailBreak(hero);
    }

    @Test
    void heroInItsOwnPrisonDoesNothing() {
        prisonOwner = GOOD;
        EntityId hero = createHero(GOOD, TILE);
        imprison(GOOD);

        tick(0.25f);

        assertNoJailBreak(hero);
    }

    @Test
    void imprisonedHeroDoesNothing() {
        EntityId hero = createHero(GOOD, TILE);
        entityData.setComponent(hero, new CreatureImprisoned(0, 0));
        imprison(GOOD);

        tick(0.25f);

        assertNoJailBreak(hero);
    }

    @Test
    void torturedHeroDoesNothing() {
        EntityId hero = createHero(GOOD, TILE);
        entityData.setComponent(hero, new CreatureTortured(0, 0, 0));
        imprison(GOOD);

        tick(0.25f);

        assertNoJailBreak(hero);
    }

    @Test
    void unconsciousHeroDoesNothing() {
        EntityId hero = createHero(GOOD, TILE);
        entityData.setComponent(hero, new Unconscious(0));
        imprison(GOOD);

        tick(0.25f);

        assertNoJailBreak(hero);
    }

    @Test
    void friendBehindAnEnemyPrisonerStartsTheJailBreak() {
        EntityId hero = createHero(GOOD, TILE);
        imprison(OTHER_KEEPER);
        imprison(GOOD);

        tick(0.25f);

        assertEquals(CreatureState.JAIL_BREAK, entityData.getComponent(hero, CreatureAi.class).getTargetState());
    }

    @Test
    void prisonerOfAnAllyStartsTheJailBreak() {
        EntityId hero = createHero(GOOD, TILE);
        imprison(ALLY);

        tick(0.25f);

        assertEquals(CreatureState.JAIL_BREAK, entityData.getComponent(hero, CreatureAi.class).getTargetState());
    }

    @Test
    void prisonerThatIsNoLongerImprisonedIsSkipped() {
        EntityId hero = createHero(GOOD, TILE);
        EntityId released = imprison(GOOD);
        entityData.removeComponent(released, CreatureImprisoned.class);

        tick(0.25f);

        assertNoJailBreak(hero);
    }

    @Test
    void friendAlreadyOnItsWayOutIsSkipped() {
        EntityId hero = createHero(GOOD, TILE);
        EntityId escaping = imprison(GOOD);
        entityData.setComponent(escaping, new PrisonEscape());

        tick(0.25f);

        assertNoJailBreak(hero);
    }

    @Test
    void heroBusyWithTheJailBreakIsNotAskedAgain() {
        EntityId hero = createHero(GOOD, TILE);
        entityData.setComponent(hero, new CreatureAi(3, CreatureState.JAIL_BREAK, (short) 5));
        imprison(GOOD);

        tick(0.25f);

        assertNoJailBreak(hero);
        assertEquals(3, entityData.getComponent(hero, CreatureAi.class).stateStartTime);
    }

    private void assertNoJailBreak(EntityId hero) {
        assertNull(entityData.getComponent(hero, CreatureAi.class).getTargetState());
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
        entityData.setComponent(hero, new CreatureAi(0, CreatureState.WANDER, (short) 5));
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
