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
package toniarts.openkeeper.game.controller.creature;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.PlayerSpell;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DK II takes UPGRADED_POSSESSION_COST_PERCENTAGE of the possession cost once
 * the keeper has upgraded the Possession spell
 */
class PossessionCostTest {

    private static final short PLAYER_ID = 3;
    private static final short POSSESSION_ID = 2;

    @Test
    void possessionCostsTheCreaturesCost() {
        assertEquals(300, possessionCost(false));
    }

    @Test
    void upgradedPossessionCostsLess() {
        assertEquals(210, possessionCost(true));
    }

    private static int possessionCost(boolean upgraded) {
        EntityData entityData = new DefaultEntityData();
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.posessionManaCost = 300;
        entityData.setComponents(entity, creatureComponent, new Owner(PLAYER_ID, PLAYER_ID));

        PlayerSpell possession = new PlayerSpell(POSSESSION_ID, true);
        possession.setUpgraded(upgraded);
        Keeper keeper = new Keeper(false, PLAYER_ID);
        keeper.getAvailableSpells().add(possession);

        KeeperSpell keeperSpell = ShotFixtures.keeperSpell(KeeperSpell.CastRule.ANYWHERE, KeeperSpell.TargetRule.POSESSION, 500);
        IKwdFile kwdFile = proxy(IKwdFile.class, (method, args) -> switch (method) {
            case "getKeeperSpellById" -> keeperSpell;
            default -> throw new UnsupportedOperationException(method);
        });
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> switch (method) {
            case "getPlayer" -> keeper;
            case "getLevelData" -> kwdFile;
            default -> throw new UnsupportedOperationException(method);
        });
        Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
        settings.put(MiscType.UPGRADED_POSSESSION_COST_PERCENTAGE,
                CreatureSleepFixtures.variable(MiscType.UPGRADED_POSSESSION_COST_PERCENTAGE, 70));

        CreatureController controller = new CreatureController(entity, entityData, null, null, null, () -> 0,
                settings, null, null, null, levelInfo, null, null, null);
        controller.setPossession(true);

        int manaDrain = entityData.getComponent(entity, Possessed.class).manaDrain;
        assertEquals(-manaDrain, entityData.getComponent(entity, Mana.class).manaGeneration);
        return manaDrain;
    }

    @FunctionalInterface
    private interface Invocation {

        Object call(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (object, method, args) -> switch (method.getName()) {
                    case "toString" -> type.getSimpleName();
                    case "hashCode" -> System.identityHashCode(object);
                    case "equals" -> object == args[0];
                    default -> call.call(method.getName(), args);
                });
    }
}
