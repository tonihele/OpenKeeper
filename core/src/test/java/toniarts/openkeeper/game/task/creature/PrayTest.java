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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.TempleFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrayTest {

    private static final short PLAYER_ID = 3;
    private static final int ANGER_PER_SECOND = -275;
    private static final int HEALTH_PER_SECOND = 33;
    private static final int MANA_PER_SECOND = 12;

    private EntityData entityData;
    private EntityId entity;
    private ICreatureController creature;
    private Keeper keeper;
    private Pray pray;

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

        Map<MiscType, MiscVariable> gameSettings = new EnumMap<>(MiscType.class);
        gameSettings.put(MiscType.MODIFY_CREATURE_ANGER_WHILE_PRAYING_PER_SECOND,
                TempleFixtures.miscVariable(MiscType.MODIFY_CREATURE_ANGER_WHILE_PRAYING_PER_SECOND, ANGER_PER_SECOND));
        gameSettings.put(MiscType.MODIFY_CREATURE_HEALTH_WHILE_PRAYING_PER_SECOND,
                TempleFixtures.miscVariable(MiscType.MODIFY_CREATURE_HEALTH_WHILE_PRAYING_PER_SECOND, HEALTH_PER_SECOND));
        gameSettings.put(MiscType.MAXIMUM_MANA_THRESHOLD,
                TempleFixtures.miscVariable(MiscType.MAXIMUM_MANA_THRESHOLD, 200000));
        keeper = new Keeper(false, PLAYER_ID);
        PlayerManaControl manaControl = new PlayerManaControl(keeper, gameSettings);

        pray = new Pray(null, null, new Point(2, 3), PLAYER_ID, room(), null, entityData, gameSettings, manaControl);
        pray.assign(creature, false);
    }

    @Test
    void prayingDoesNothingBeforeTheFirstSecond() {
        pray.executeTask(creature, 0.75f);

        assertEquals(80, entityData.getComponent(entity, Health.class).health);
        assertEquals(0, keeper.getMana());
        assertEquals(1000, entityData.getComponent(entity, CreatureMood.class).general);
    }

    @Test
    void prayingCalmsEveryAngerOnceASecond() {
        pray.executeTask(creature, 1.0f);

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

        pray.executeTask(creature, 1.0f);

        assertEquals(50 + HEALTH_PER_SECOND, entityData.getComponent(entity, Health.class).health);
    }

    @Test
    void prayingDoesNotHealPastTheMaximum() {
        entityData.setComponent(entity, new Health(90, 100));

        pray.executeTask(creature, 1.0f);

        assertEquals(100, entityData.getComponent(entity, Health.class).health);
    }

    @Test
    void prayingGivesTheKeeperTheCreaturesPrayerMana() {
        pray.executeTask(creature, 1.0f);

        assertEquals(MANA_PER_SECOND, keeper.getMana());
    }

    @Test
    void prayingWorksOnceASecondEvenWhenExecutedEveryTick() {
        for (float time = 0.25f; time <= 3.0f; time += 0.25f) {
            pray.executeTask(creature, time);
        }

        assertEquals(3 * MANA_PER_SECOND, keeper.getMana());
        assertEquals(1000 + 3 * ANGER_PER_SECOND, entityData.getComponent(entity, CreatureMood.class).general);
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

    private static IRoomController room() {
        return (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "temple";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
