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
package toniarts.openkeeper.game.trigger;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.CreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Player Creatures trigger counts. Level19's good player script creates its hero parties and checks
 * "creatures of the party leader's type = 0" on the same tick; the count has to see the new creatures.
 */
class PlayerCreaturesTriggerTest {

    private static final short GOOD_PLAYER = 1;
    private static final short KEEPER = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final TriggerControl triggerControl;

    PlayerCreaturesTriggerTest() {
        IKwdFile[] kwdFile = new IKwdFile[1];
        kwdFile[0] = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getTriggers" -> Map.of(1, new TriggerGeneric(kwdFile[0]));
                    case "getThings" -> List.of();
                    default -> null;
                });
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(), new Class<?>[]{ILevelInfo.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getLevelData" -> kwdFile[0];
                    case "getPlayer" -> new Keeper(false, (short) args[0]);
                    default -> null;
                });

        // The player's creature control hasn't had its tick yet, so it hasn't seen any of the creatures
        PlayerCreatureControl creatureControl = new PlayerCreatureControl(new Keeper(false, GOOD_PLAYER), null, List.of());
        IPlayerController playerController = (IPlayerController) Proxy.newProxyInstance(IPlayerController.class.getClassLoader(), new Class<?>[]{IPlayerController.class},
                (proxy, method, args) -> method.getName().equals("getCreatureControl") ? creatureControl : null);
        IGameController gameController = (IGameController) Proxy.newProxyInstance(IGameController.class.getClassLoader(), new Class<?>[]{IGameController.class},
                (proxy, method, args) -> method.getName().equals("getPlayerController") ? playerController : null);

        CreaturesController creaturesController = new CreaturesController(kwdFile[0], entityData, Map.of(), null, gameController, null, levelInfo);
        triggerControl = new PlayerTriggerControl(gameController, levelInfo, null, null, creaturesController, 1, GOOD_PLAYER, null);
    }

    @Test
    void creatureSpawnedOnThisTickCountsAtOnce() {
        createCreature((short) 2, GOOD_PLAYER, true);

        assertFalse(triggerControl.isActive(playerCreatures((short) 2, 0)), "the party leader is alive");
        assertTrue(triggerControl.isActive(playerCreatures((short) 2, 1)));
    }

    @Test
    void countsOnlyThePlayersLivingCreatures() {
        createCreature((short) 2, GOOD_PLAYER, true);
        createCreature((short) 2, KEEPER, true);
        createCreature((short) 2, GOOD_PLAYER, false);
        createCreature((short) 26, GOOD_PLAYER, true);

        assertTrue(triggerControl.isActive(playerCreatures((short) 2, 1)));
        assertTrue(triggerControl.isActive(playerCreatures((short) 0, 2)));
    }

    private void createCreature(short creatureId, short ownerId, boolean alive) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creature = new CreatureComponent();
        creature.creatureId = creatureId;
        entityData.setComponents(entityId, creature, new Owner(ownerId, ownerId));
        if (alive) {
            entityData.setComponent(entityId, new Health(100, 100));
        }
    }

    private static TriggerGenericData playerCreatures(short creatureId, int value) {
        TriggerGenericData trigger = new TriggerGenericData(2);
        trigger.setType(TriggerGeneric.TargetType.PLAYER_CREATURES);
        trigger.setComparison(TriggerGeneric.ComparisonType.EQUAL_TO);
        trigger.setUserData("creatureId", creatureId);
        trigger.setUserData("flag", (short) 1);
        trigger.setUserData("playerId", (short) 0);
        trigger.setUserData("value", value);
        return trigger;
    }
}
