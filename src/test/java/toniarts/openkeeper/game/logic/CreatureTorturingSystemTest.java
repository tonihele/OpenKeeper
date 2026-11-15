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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureTorturingSystemTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short ENEMY = Player.KEEPER1_ID + 1;
    private static final float TORTURE_TIME_TO_CONVERT = 10;

    private EntityData entityData;
    private List<Short> conversions;
    private EntityId victim;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        conversions = new ArrayList<>();

        victim = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.tortureTimeToConvert = TORTURE_TIME_TO_CONVERT;
        entityData.setComponents(victim, creatureComponent, new Owner(ENEMY, KEEPER), new Health(300, 1000),
                new Position(0, new Vector3f(5, 0, 5)), new CreatureTortured(TORTURE_TIME_TO_CONVERT, 0, 0));
    }

    @Test
    void victimJoinsTheTorturerWhenThereIsNoChanceOfDying() {
        createSystem(0).processTick(1f);

        assertEquals(List.of(KEEPER), conversions);
        assertNull(entityData.getComponent(victim, CreatureTortured.class));
        assertNull(entityData.getComponent(victim, Damage.class));
    }

    @Test
    void victimDiesInsteadOfJoiningByTheChanceOfDying() {
        createSystem(100).processTick(1f);

        assertTrue(conversions.isEmpty());
        assertEquals(300, entityData.getComponent(victim, Damage.class).damage);

        // The torture starts over, the health system lets the victim die
        CreatureTortured tortured = entityData.getComponent(victim, CreatureTortured.class);
        assertNotNull(tortured);
        assertEquals(0, tortured.timeTortured);
    }

    private CreatureTorturingSystem createSystem(int chanceOfDyingWhenConverted) {
        IMapTileInformation tile = stub(IMapTileInformation.class, (name, args) -> "getOwnerId".equals(name) ? KEEPER : null);
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (name, args) -> "getTile".equals(name) ? tile : null);
        IMapInformation<?> mapInformation = stub(IMapInformation.class, (name, args) -> "getMapData".equals(name) ? mapData : null);
        ICreatureController creatureController = stub(ICreatureController.class, (name, args) -> {
            if ("convertCreature".equals(name)) {
                conversions.add((Short) args[0]);
            }
            return null;
        });
        ICreaturesController creaturesController = stub(ICreaturesController.class,
                (name, args) -> "createController".equals(name) ? creatureController : null);

        Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings = new EnumMap<>(Variable.MiscVariable.MiscType.class);
        gameSettings.put(Variable.MiscVariable.MiscType.TORTURE_CHANCE_OF_DYING_WHEN_CONVERTED,
                CreatureSleepFixtures.variable(Variable.MiscVariable.MiscType.TORTURE_CHANCE_OF_DYING_WHEN_CONVERTED, chanceOfDyingWhenConverted));

        return new CreatureTorturingSystem(entityData, creaturesController, mapInformation, gameSettings);
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

}
