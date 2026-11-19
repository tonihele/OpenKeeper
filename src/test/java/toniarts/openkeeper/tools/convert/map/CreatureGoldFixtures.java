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
package toniarts.openkeeper.tools.convert.map;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * Builds creature records and placed creatures with gold values for tests.
 * Lives in the parser's package because the setters are {@code protected}.
 */
public final class CreatureGoldFixtures {

    private CreatureGoldFixtures() {
    }

    /**
     * A creature record with the gold attributes set
     *
     * @param creatureId the creature ID
     * @param maxGoldHeld the most gold the creature can carry
     * @param initialGoldHeld the "initial gold held" attribute of the record
     * @return the creature record
     */
    public static Creature goldCreature(short creatureId, int maxGoldHeld, short initialGoldHeld) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setFlags(EnumSet.of(Creature.CreatureFlag.IS_WORKER));
        creature.setMeleeAttackType(Creature.AttackType.MELEE_BLUNT);
        creature.setSpells(new ArrayList<>());
        creature.getAttributes().setHp(1000);
        creature.getAttributes().setMaxGoldHeld(maxGoldHeld);
        creature.getAttributes().setInitialGoldHeld(initialGoldHeld);
        return creature;
    }

    /**
     * A keeper's creature placed on the map in the editor
     *
     * @param creatureId the creature ID
     * @param playerId the owner
     * @param goldHeldPercent the gold it carries, in percent of its max gold
     * @return the placed creature
     */
    public static Thing.KeeperCreature placedKeeperCreature(short creatureId, short playerId, int goldHeldPercent) {
        Thing.KeeperCreature creature = new Thing.KeeperCreature();
        creature.setCreatureId(creatureId);
        creature.setPlayerId(playerId);
        creature.setLevel((short) 1);
        creature.setInitialHealth(100);
        creature.setGoldHeld(goldHeldPercent);
        return creature;
    }

    /**
     * A hero placed on the map in the editor
     *
     * @param creatureId the creature ID
     * @param goldHeldPercent the gold it carries, in percent of its max gold
     * @return the placed hero
     */
    public static Thing.GoodCreature placedHero(short creatureId, int goldHeldPercent) {
        Thing.GoodCreature creature = new Thing.GoodCreature();
        creature.setCreatureId(creatureId);
        creature.setLevel((short) 1);
        creature.setInitialHealth(100);
        creature.setGoldHeld(goldHeldPercent);
        return creature;
    }

}
