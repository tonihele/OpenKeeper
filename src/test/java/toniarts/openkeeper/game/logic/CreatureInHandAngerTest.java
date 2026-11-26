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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.ViewType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureInHandFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A creature held in the keeper's hand for over 20 seconds changes its "other"
 * anger by its in-hand anger once every game second.
 */
class CreatureInHandAngerTest {

    private static final short KEEPER_ID = 3;

    private final EntityData entityData = new DefaultEntityData();
    private double gameTime;

    @Test
    void heldCreatureCalmsDownEverySecondAfterTwentySeconds() {
        CreatureMoodSystem system = system(CreatureInHandFixtures.creature((short) -500));
        EntityId creature = creature(new InHand(0, KEEPER_ID, ViewType.CREATURE, (short) 6, 0));

        assertEquals(1000, tick(system, creature, 20.0));
        assertEquals(875, tick(system, creature, 21.0));
        assertEquals(875, tick(system, creature, 21.25));
        assertEquals(750, tick(system, creature, 22.0));
    }

    @Test
    void heldCreatureCanGetAngrier() {
        CreatureMoodSystem system = system(CreatureInHandFixtures.creature((short) 1000));
        EntityId creature = creature(new InHand(0, KEEPER_ID, ViewType.CREATURE, (short) 6, 0));

        assertEquals(1250, tick(system, creature, 21.0));
    }

    @Test
    void creatureOutOfTheHandKeepsItsAnger() {
        CreatureMoodSystem system = system(CreatureInHandFixtures.creature((short) -500));
        EntityId creature = creature(null);

        assertEquals(1000, tick(system, creature, 21.0));
    }

    private EntityId creature(InHand inHand) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = 6;
        entityData.setComponents(entityId, creatureComponent, new CreatureMood(0, 0, 0, 0, 0, 1000),
                new CreatureSleep(), new Owner(KEEPER_ID, KEEPER_ID));
        if (inHand != null) {
            entityData.setComponent(entityId, inHand);
        }
        return entityId;
    }

    private CreatureMoodSystem system(Creature creature) {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" -> creature;
            default -> null;
        });
        return new CreatureMoodSystem(entityData, kwdFile, List.of(), () -> gameTime,
                Map.of(Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND,
                        CreatureSleepFixtures.variable(
                                Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND, -550)));
    }

    private int tick(CreatureMoodSystem system, EntityId creature, double time) {
        gameTime = time;
        system.processTick(0.25f);
        return entityData.getComponent(creature, CreatureMood.class).other;
    }
}
