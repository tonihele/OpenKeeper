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
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureMoodFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Native DKII anger only runs for the creatures of keepers, not for heroes
 * or neutral creatures.
 */
public class CreatureMoodSystemTest {

    private static final short KEEPER_ID = 3;
    private static final short CREATURE_ID = 7;

    private final EntityData entityData = new DefaultEntityData();

    @Test
    public void keeperCreatureWithoutLairGetsAngry() {
        EntityId creature = creature(KEEPER_ID);

        tick();

        assertTrue(entityData.getComponent(creature, CreatureMood.class).noLair > 0);
    }

    @Test
    public void heroWithoutLairGetsNoAnger() {
        EntityId creature = creature(Player.GOOD_PLAYER_ID);

        tick();

        assertEquals(0, entityData.getComponent(creature, CreatureMood.class).noLair);
    }

    @Test
    public void neutralCreatureWithoutLairGetsNoAnger() {
        EntityId creature = creature(Player.NEUTRAL_PLAYER_ID);

        tick();

        assertEquals(0, entityData.getComponent(creature, CreatureMood.class).noLair);
    }

    private EntityId creature(short ownerId) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = CREATURE_ID;
        entityData.setComponents(entityId, creatureComponent, new CreatureMood(),
                new CreatureSleep(), new Owner(ownerId, ownerId));
        return entityId;
    }

    private void tick() {
        Room lair = RoomFixtures.room((short) 2, 0, Room.RoomFlag.BUILDABLE);
        Creature creature = CreatureMoodFixtures.creature(CREATURE_ID, "Knight", (short) 6, (short) 100, (short) 50);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getRoomById" -> lair;
            case "getCreature" -> creature;
            default -> throw new UnsupportedOperationException(method.getName());
        });

        // Every player has the lair available, like the levels do
        java.util.ArrayList<IPlayerController> controllers = new java.util.ArrayList<>();
        for (short playerId : new short[]{Player.GOOD_PLAYER_ID, Player.NEUTRAL_PLAYER_ID, KEEPER_ID}) {
            Keeper keeper = new Keeper(false, playerId);
            PlayerRoomControl roomControl = new PlayerRoomControl(keeper, List.of(lair));
            roomControl.setTypeAvailable(lair, true, true);
            controllers.add((IPlayerController) Proxy.newProxyInstance(IPlayerController.class.getClassLoader(),
                    new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getKeeper" -> keeper;
                case "getRoomControl" -> roomControl;
                default -> throw new UnsupportedOperationException(method.getName());
            }));
        }

        CreatureMoodSystem system = new CreatureMoodSystem(entityData, kwdFile, controllers, () -> 0.0,
                Map.of(Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND,
                        CreatureSleepFixtures.variable(
                                Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND, -550)));
        system.processTick(0.25f);
        system.stop();
    }

    @Test
    public void lairRecoveryChangesOnlySharedOtherAnger() {
        CreatureMood mood = new CreatureMood(100, 200, 300, 400, 500, 600);

        CreatureMood updated = CreatureMoodSystem.recoverInLair(mood, -550);

        assertEquals(100, updated.general);
        assertEquals(200, updated.noFood);
        assertEquals(300, updated.noLair);
        assertEquals(400, updated.noWork);
        assertEquals(500, updated.noPay);
        assertEquals(50, updated.other);
    }

    @Test
    public void lairRecoveryCannotMakeAngerNegative() {
        CreatureMood updated = CreatureMoodSystem.recoverInLair(
                new CreatureMood(0, 0, 0, 0, 0, 100), -550);

        assertEquals(0, updated.other);
    }
}
