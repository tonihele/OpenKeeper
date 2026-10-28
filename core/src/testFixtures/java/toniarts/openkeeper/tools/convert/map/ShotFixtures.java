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

import java.util.EnumSet;

public final class ShotFixtures {

    /**
     * Shot 21 as it ships in the original data
     */
    public static final short THUNDERBOLT_ID = 21;

    private ShotFixtures() {
    }

    public static Shot shot(short shotId, Shot.ProcessType processType, int health, EnumSet<Shot.ShotProcessFlag> processFlags) {
        Shot shot = new Shot();
        shot.setShotId(shotId);
        shot.setName("Shot " + shotId);
        shot.setProcessType(processType);
        shot.setHealth(health);
        shot.setShotProcessFlags(processFlags);
        return shot;
    }

    /**
     * The keeper's Thunderbolt, with the values of the original data
     */
    public static Shot thunderbolt() {
        return shot(THUNDERBOLT_ID, Shot.ProcessType.LIGHTNING, 8, EnumSet.of(Shot.ShotProcessFlag.AT_START));
    }

    public static Creature creature(short creatureId, EnumSet<Creature.CreatureFlag> flags, EnumSet<Creature.CreatureFlag3> flags3) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setName("Creature " + creatureId);
        creature.setFlags(flags);
        creature.setFlags3(flags3);
        return creature;
    }

    public static KeeperSpell keeperSpell(KeeperSpell.CastRule castRule, KeeperSpell.TargetRule targetRule, int manaCost) {
        KeeperSpell keeperSpell = new KeeperSpell();
        keeperSpell.setName("Spell");
        keeperSpell.setCastRule(castRule);
        keeperSpell.setTargetRule(targetRule);
        keeperSpell.setManaCost(manaCost);
        return keeperSpell;
    }

}
