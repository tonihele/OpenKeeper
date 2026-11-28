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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.TempleFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrayTest {

    private static final short PLAYER_ID = 3;
    private static final int ANGER_PER_SECOND = -275;
    private static final int HEALTH_PER_SECOND = 33;
    private static final int MANA_PER_SECOND = 12;
    private static final float DELTA = 0.0001f;

    private EntityData entityData;
    private EntityId entity;
    private ICreatureController creature;
    private Keeper keeper;
    private Map<MiscType, MiscVariable> gameSettings;
    private PlayerManaControl manaControl;
    private Pray pray;
    private final List<EntityId> worshippers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.manaGenPrayer = MANA_PER_SECOND;
        entityData.setComponent(entity, creatureComponent);
        entityData.setComponent(entity, new CreatureMood(1000, 2000, 100, 0, 3000, 500));
        entityData.setComponent(entity, new Health(80, 100));
        creature = creature(entity);

        gameSettings = new EnumMap<>(MiscType.class);
        gameSettings.put(MiscType.MODIFY_CREATURE_ANGER_WHILE_PRAYING_PER_SECOND,
                TempleFixtures.miscVariable(MiscType.MODIFY_CREATURE_ANGER_WHILE_PRAYING_PER_SECOND, ANGER_PER_SECOND));
        gameSettings.put(MiscType.MODIFY_CREATURE_HEALTH_WHILE_PRAYING_PER_SECOND,
                TempleFixtures.miscVariable(MiscType.MODIFY_CREATURE_HEALTH_WHILE_PRAYING_PER_SECOND, HEALTH_PER_SECOND));
        gameSettings.put(MiscType.MAXIMUM_MANA_THRESHOLD,
                TempleFixtures.miscVariable(MiscType.MAXIMUM_MANA_THRESHOLD, 200000));
        keeper = new Keeper(false, PLAYER_ID);
        manaControl = new PlayerManaControl(keeper, gameSettings);

        pray = pray(new Point(2, 3));
        pray.assign(creature, false);
    }

    @Test
    void worshipperFacesTheRoomCentre() {
        assertTrue(pray.isFaceTarget());
        assertEquals(new Point(5, 7), pray.getFaceLocation());
    }

    @Test
    void worshipperStandsAQuarterTileFromTheMiddleOfItsTileOnTheSideAwayFromTheCentre() {

        // From the centre (5, 7) the tile (2, 3) is 3 tiles west and 4 north, 5 tiles away
        Vector2f spot = pray.getWorkPosition(creature);
        assertEquals(2 - 0.25f * 3 / 5, spot.x, DELTA);
        assertEquals(3 - 0.25f * 4 / 5, spot.y, DELTA);
        assertSame(spot, pray.getWorkPosition(creature));
    }

    @Test
    void worshipperOnTheCentreTileStandsOnItsNorthSide() {
        assertEquals(new Vector2f(5, 6.75f), pray(new Point(5, 7)).getWorkPosition(creature));
    }

    @Test
    void worshipperIsCountedInTheTempleOnceItPrays() {
        assertEquals(List.of(), worshippers);

        pray.executeTask(creature, 0);

        assertEquals(List.of(entity), worshippers);
    }

    @Test
    void prayingDoesNothingBeforeTheFirstSecondAndATick() {
        pray.executeTask(creature, 1.0f);

        assertEquals(80, entityData.getComponent(entity, Health.class).health);
        assertEquals(0, keeper.getMana());
        assertEquals(1000, entityData.getComponent(entity, CreatureMood.class).general);
    }

    @Test
    void prayingCalmsEveryAngerOnceASecond() {
        pray.executeTask(creature, 1.25f);

        CreatureMood mood = entityData.getComponent(entity, CreatureMood.class);
        assertEquals(1000 + ANGER_PER_SECOND, mood.general);
        assertEquals(2000 + ANGER_PER_SECOND, mood.noFood);
        assertEquals(0, mood.noLair);
        assertEquals(0, mood.noWork);
        assertEquals(3000 + ANGER_PER_SECOND, mood.noPay);
        assertEquals(500 + ANGER_PER_SECOND, mood.other);
    }

    @Test
    void prayingHeals() {
        entityData.setComponent(entity, new Health(50, 100));

        pray.executeTask(creature, 1.25f);

        assertEquals(50 + HEALTH_PER_SECOND, entityData.getComponent(entity, Health.class).health);
    }

    @Test
    void prayingDoesNotHealPastTheMaximum() {
        entityData.setComponent(entity, new Health(90, 100));

        pray.executeTask(creature, 1.25f);

        assertEquals(100, entityData.getComponent(entity, Health.class).health);
    }

    @Test
    void prayingGivesTheKeeperTheCreaturesPrayerMana() {
        pray.executeTask(creature, 1.25f);

        assertEquals(MANA_PER_SECOND, keeper.getMana());
    }

    @Test
    void prayingWorksEveryFiveTicksEvenWhenExecutedEveryTick() {

        // DK II counts the ticks of a second down to 0 before each prayer, 15 ticks are three prayers (0x47B970)
        for (int tick = 1; tick <= 15; tick++) {
            pray.executeTask(creature, tick * 0.25f);
        }

        assertEquals(3 * MANA_PER_SECOND, keeper.getMana());
        assertEquals(1000 + 3 * ANGER_PER_SECOND, entityData.getComponent(entity, CreatureMood.class).general);
    }

    private Pray pray(Point p) {
        return new Pray(null, null, p, PLAYER_ID, room(), null, entityData, gameSettings, manaControl, 4, new Random(0));
    }

    private static ICreatureController creature(EntityId entityId) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> entityId;
                    case "getOwnerId" -> PLAYER_ID;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "creature " + entityId;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    /**
     * A temple from (2, 3) to (8, 11), its centre is at (5, 7)
     */
    private IRoomController room() {
        RoomInstance roomInstance = new RoomInstance(TempleFixtures.temple());
        roomInstance.addCoordinate(new Point(2, 3));
        roomInstance.addCoordinate(new Point(8, 11));
        IRoomObjectControl control = (IRoomObjectControl) Proxy.newProxyInstance(IRoomObjectControl.class.getClassLoader(),
                new Class<?>[]{IRoomObjectControl.class}, (proxy, method, args) -> method.getName().equals("addItem") && worshippers.add((EntityId) args[0]) ? args[0] : null);
        return (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "temple";
                    case "getObjectControl" -> control;
                    case "getRoomInstance" -> roomInstance;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
