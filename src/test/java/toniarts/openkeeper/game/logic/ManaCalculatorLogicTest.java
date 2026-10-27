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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManaCalculatorLogicTest {

    private static final short PLAYER_ID = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final Keeper keeper = new Keeper(false, PLAYER_ID);
    private final PlayerManaControl manaControl = new PlayerManaControl(keeper, settings());

    @Test
    void manaVaultRaisesTheGainLimitByItsMaximumGain() {
        addManaSource(600, 0); // Claimed tiles
        addManaSource(30, 0); // Dungeon heart
        addManaSource(100, 100); // Claimed mana vault
        addManaSource(-7, 0); // Imp

        ManaCalculatorLogic logic = new ManaCalculatorLogic(List.of(playerController()), entityData);
        logic.processTick(1f);

        assertEquals(600, keeper.getManaGain());
        assertEquals(7, keeper.getManaLoose());
        assertEquals(593, keeper.getMana());
    }

    @Test
    void losingTheVaultLowersTheGainLimitAgain() {
        addManaSource(600, 0);
        EntityId vault = addManaSource(100, 100);

        ManaCalculatorLogic logic = new ManaCalculatorLogic(List.of(playerController()), entityData);
        entityData.setComponent(vault, new Owner((short) 4, (short) 4));
        logic.processTick(1f);

        assertEquals(500, keeper.getManaGain());
    }

    @Test
    void vaultBuiltOnOwnedTileRaisesTheGainLimit() {
        addManaSource(600, 0);
        EntityId tile = addManaSource(0, 0);

        ManaCalculatorLogic logic = new ManaCalculatorLogic(List.of(playerController()), entityData);
        entityData.setComponent(tile, new Mana(100, 100));
        logic.processTick(1f);

        assertEquals(600, keeper.getManaGain());
    }

    @Test
    void possessedCreatureCostsOnlyWhatGoesOverTheGainLimit() {
        keeper.setMana(1000);
        addManaSource(200, 0); // Claimed tiles
        addManaSource(30, 0); // Dungeon heart
        EntityId vampire = addManaSource(-600, 0); // CreatureController puts the cost in its Mana when possessed
        entityData.setComponent(vampire, new Possessed(600, 0));

        ManaCalculatorLogic logic = new ManaCalculatorLogic(List.of(playerController()), entityData);
        logic.processTick(1f);

        assertEquals(500, keeper.getManaGain());
        assertEquals(600, keeper.getManaLoose());
        assertEquals(900, keeper.getMana());
    }

    @Test
    void anotherKeepersPossessionDoesNotRaiseTheGain() {
        addManaSource(200, 0);
        addManaSource(30, 0);
        EntityId enemyVampire = entityData.createEntity();
        entityData.setComponents(enemyVampire, new Mana(-600), new Possessed(600, 0), new Owner((short) 4, (short) 4));

        ManaCalculatorLogic logic = new ManaCalculatorLogic(List.of(playerController()), entityData);
        logic.processTick(1f);

        assertEquals(230, keeper.getManaGain());
        assertEquals(0, keeper.getManaLoose());
    }

    private EntityId addManaSource(int manaGeneration, int maxManaGeneration) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new Mana(manaGeneration, maxManaGeneration), new Owner(PLAYER_ID, PLAYER_ID));
        return entity;
    }

    private IPlayerController playerController() {
        return (IPlayerController) Proxy.newProxyInstance(IPlayerController.class.getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getKeeper" -> keeper;
                    case "getManaControl" -> manaControl;
                    default -> null;
                });
    }

    private static Map<MiscType, MiscVariable> settings() {
        Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
        settings.put(MiscType.MAXIMUM_MANA_THRESHOLD, TrapFiringFixtures.miscVariable(MiscType.MAXIMUM_MANA_THRESHOLD, 200000));
        settings.put(MiscType.MAXIMUM_MANA_GAIN_PER_SECOND, TrapFiringFixtures.miscVariable(MiscType.MAXIMUM_MANA_GAIN_PER_SECOND, 500));
        return settings;
    }

}
