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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.KeeperSpellCast;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.FakeMapInformation;
import toniarts.openkeeper.game.map.FakeMapTile;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureFlagFixtures;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeeperSpellCastValidatorTest {

    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final FakeMapInformation mapInformation = new FakeMapInformation(new FakeMapData(4, 4), Map.of());
    private final Creature creature = CreatureFlagFixtures.creature((short) 1, EnumSet.noneOf(Creature.CreatureFlag.class));
    private final KeeperSpell thunderbolt = ShotFixtures.keeperSpell(KeeperSpell.CastRule.OWN_LAND, KeeperSpell.TargetRule.ENEMY_CREATURES, 6000);
    private final KeeperSpell chicken = ShotFixtures.keeperSpell(KeeperSpell.CastRule.OWN_LAND, KeeperSpell.TargetRule.ALL_CREATURES, 0);
    private final KeeperSpell possession = ShotFixtures.keeperSpell(KeeperSpell.CastRule.OWN_LAND, KeeperSpell.TargetRule.POSESSION, 0);
    private final Keeper player = new Keeper(false, PLAYER_ID);
    private final FakeMapTile ownTile;

    KeeperSpellCastValidatorTest() {
        player.setMana(100000);
        ownTile = mapInformation.getMapData().getTile(1, 1);
        ownTile.ownerId = PLAYER_ID;
    }

    @Test
    void enemyCreatureCanBeStruck() {
        assertTrue(isValidCast(thunderbolt, creature, ENEMY_ID, false));
    }

    @Test
    void enemyThatIsNotACreatureCannotBeStruck() {
        assertFalse(isValidCast(thunderbolt, null, ENEMY_ID, false));
    }

    @Test
    void ownCreatureCannotBeStruck() {
        assertFalse(isValidCast(thunderbolt, creature, PLAYER_ID, false));
    }

    @Test
    void notEnoughManaCannotStrike() {
        player.setMana(5999);

        assertFalse(isValidCast(thunderbolt, creature, ENEMY_ID, false));
    }

    @Test
    void spellWithRechargeCannotBeCastAgainUntilItHasPassed() {
        KeeperSpell inferno = ShotFixtures.keeperSpell(KeeperSpell.CastRule.OWN_LAND, KeeperSpell.TargetRule.LAND, 0, 60f, (short) 2);
        KeeperSpell otherSpell = ShotFixtures.keeperSpell(KeeperSpell.CastRule.OWN_LAND, KeeperSpell.TargetRule.LAND, 0, 60f, (short) 3);
        assertTrue(KeeperSpellCastValidator.isRecharged(inferno, entityData, PLAYER_ID, 10));

        entityData.setComponent(entityData.createEntity(), new KeeperSpellCast(PLAYER_ID, inferno.getId(), 10));

        assertFalse(KeeperSpellCastValidator.isRecharged(inferno, entityData, PLAYER_ID, 69.5));
        assertTrue(KeeperSpellCastValidator.isRecharged(inferno, entityData, PLAYER_ID, 70));
        assertTrue(KeeperSpellCastValidator.isRecharged(inferno, entityData, ENEMY_ID, 11));
        assertTrue(KeeperSpellCastValidator.isRecharged(otherSpell, entityData, PLAYER_ID, 11));
    }

    @Test
    void spellWithoutRechargeCanBeCastAtOnce() {
        entityData.setComponent(entityData.createEntity(), new KeeperSpellCast(PLAYER_ID, thunderbolt.getId(), 10));

        assertTrue(KeeperSpellCastValidator.isRecharged(thunderbolt, entityData, PLAYER_ID, 10));
    }

    @Test
    void chickenCanBeCastOnAnyCreatureButNeutralOnes() {
        assertTrue(isValidCast(chicken, creature, ENEMY_ID, false));
        assertTrue(isValidCast(chicken, creature, PLAYER_ID, false));
        assertFalse(isValidCast(chicken, creature, Player.NEUTRAL_PLAYER_ID, false));
    }

    @Test
    void chickenCannotBePossessed() {
        assertTrue(isValidCast(possession, creature, PLAYER_ID, false));
        assertFalse(isValidCast(possession, creature, PLAYER_ID, true));
    }

    @Test
    void spellWithoutTargetCannotBeCastOnCreatures() {
        assertFalse(KeeperSpellCastValidator.isValidCast(chicken, mapInformation, ownTile, player, null, null, false, 0));
    }

    private boolean isValidCast(KeeperSpell keeperSpell, Creature target, short owner, boolean isChicken) {
        return KeeperSpellCastValidator.isValidCast(keeperSpell, mapInformation, ownTile, player, target, owner, isChicken, keeperSpell.getManaCost());
    }

}
