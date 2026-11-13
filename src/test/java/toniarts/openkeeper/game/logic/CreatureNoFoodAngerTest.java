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
import toniarts.openkeeper.game.component.CreatureHunger;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureHungerFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A hungry creature gets angrier every tick while it waits for its whole fill
 * of chickens and its keeper can build or has a Hatchery.
 */
class CreatureNoFoodAngerTest {

    private static final short KEEPER_ID = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final Room hatchery = RoomFixtures.room((short) 4, 0, Room.RoomFlag.BUILDABLE);
    private final Creature creature = CreatureHungerFixtures.creature((short) 110);

    @Test
    void hungryCreatureGetsAngryEveryTick() {
        EntityId entityId = creature(2);

        CreatureMoodSystem system = system(true);
        system.processTick(0.25f);
        system.processTick(0.25f);

        assertEquals(54, entityData.getComponent(entityId, CreatureMood.class).noFood);
    }

    @Test
    void creatureThatHasStartedEatingGetsNoAnger() {
        EntityId entityId = creature(1);

        system(true).processTick(0.25f);

        assertEquals(0, entityData.getComponent(entityId, CreatureMood.class).noFood);
    }

    @Test
    void creatureWithoutHatcheryGetsNoAnger() {
        EntityId entityId = creature(2);

        system(false).processTick(0.25f);

        assertEquals(0, entityData.getComponent(entityId, CreatureMood.class).noFood);
    }

    private EntityId creature(int chickensNeeded) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = 6;
        creatureComponent.hungerFill = 2;
        entityData.setComponents(entityId, creatureComponent, new CreatureMood(),
                new CreatureHunger(0, chickensNeeded), new CreatureSleep(), new Owner(KEEPER_ID, KEEPER_ID));
        return entityId;
    }

    private CreatureMoodSystem system(boolean hatcheryAvailable) {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" -> creature;
            case "getRoomById" -> (int) args[0] == 4 ? hatchery : null;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        Keeper keeper = new Keeper(false, KEEPER_ID);
        PlayerRoomControl roomControl = new PlayerRoomControl(keeper, List.of(hatchery));
        roomControl.setTypeAvailable(hatchery, hatcheryAvailable, hatcheryAvailable);
        IPlayerController player = (IPlayerController) Proxy.newProxyInstance(IPlayerController.class.getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getKeeper" -> keeper;
            case "getRoomControl" -> roomControl;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        return new CreatureMoodSystem(entityData, kwdFile, List.of(player), () -> 0.0,
                Map.of(Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND,
                        CreatureSleepFixtures.variable(
                                Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND, -550)));
    }
}
