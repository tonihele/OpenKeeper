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
package toniarts.openkeeper.game.state;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Disguised;
import toniarts.openkeeper.game.component.Hypnotised;
import toniarts.openkeeper.game.component.PossessedAbility;
import toniarts.openkeeper.tools.convert.map.Creature.SpecialAbility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The possession HUD marks the abilities that are on
 */
public class PossessionAbilityActiveTest {

    private final PlayerScreenController controller = new PlayerScreenController(null, null);
    private final EntityData data = new DefaultEntityData();

    @Test
    public void nothingIsActiveByDefault() {
        EntityId creature = data.createEntity();

        for (SpecialAbility ability : SpecialAbility.values()) {
            assertFalse(controller.isPossessionAbilityActive(data, creature, ability));
        }
    }

    @Test
    public void toggledAbilityIsActiveOnlyForItself() {
        EntityId creature = data.createEntity();
        data.setComponent(creature, new PossessedAbility(SpecialAbility.PRAY, 0));

        assertTrue(controller.isPossessionAbilityActive(data, creature, SpecialAbility.PRAY));
        assertFalse(controller.isPossessionAbilityActive(data, creature, SpecialAbility.TURN_TO_BAT));
    }

    @Test
    public void disguiseIsActiveWhileDisguised() {
        EntityId creature = data.createEntity();
        data.setComponent(creature, new Disguised((short) 3));

        assertTrue(controller.isPossessionAbilityActive(data, creature, SpecialAbility.DISGUISE_N_STEAL));
    }

    @Test
    public void hypnotiseIsActiveWhileACreatureIsHeld() {
        EntityId creature = data.createEntity();
        EntityId other = data.createEntity();
        data.setComponent(other, new Hypnotised(creature, (short) 4));

        assertTrue(controller.isPossessionAbilityActive(data, creature, SpecialAbility.HYPNOTISE));
        assertFalse(controller.isPossessionAbilityActive(data, other, SpecialAbility.HYPNOTISE));
    }
}
