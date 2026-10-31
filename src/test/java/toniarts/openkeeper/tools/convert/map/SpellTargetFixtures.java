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

/**
 * Builds the {@link Shot}, {@link KeeperSpell} and {@link Creature} fixtures
 * that keeper spell tests need. Lives in the data classes' own package because
 * their setters are {@code protected} (parser-only).
 */
public final class SpellTargetFixtures {

    private SpellTargetFixtures() {
    }

    /**
     * A shot with the given process type
     *
     * @param shotId the shot ID
     * @param processType what the shot does
     * @return the shot
     */
    public static Shot shot(short shotId, Shot.ProcessType processType) {
        Shot shot = new Shot();
        shot.setShotId(shotId);
        shot.setName("Test shot " + shotId);
        shot.setProcessType(processType);
        return shot;
    }

    /**
     * A free keeper spell with the given rules
     *
     * @param shotTypeId the shot the spell fires
     * @param targetRule what the spell can be cast on
     * @param castRule where the spell can be cast
     * @return the keeper spell
     */
    public static KeeperSpell keeperSpell(short shotTypeId, KeeperSpell.TargetRule targetRule, KeeperSpell.CastRule castRule) {
        KeeperSpell keeperSpell = new KeeperSpell();
        keeperSpell.setName("Test spell");
        keeperSpell.setShotTypeId(shotTypeId);
        keeperSpell.setTargetRule(targetRule);
        keeperSpell.setCastRule(castRule);
        keeperSpell.setManaCost(0);
        return keeperSpell;
    }

    /**
     * A creature with the given flags
     *
     * @param creatureId the creature ID
     * @param flags the creature flags
     * @return the creature
     */
    public static Creature creature(short creatureId, Creature.CreatureFlag... flags) {
        EnumSet<Creature.CreatureFlag> set = EnumSet.noneOf(Creature.CreatureFlag.class);
        for (Creature.CreatureFlag flag : flags) {
            set.add(flag);
        }

        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setName("Test creature " + creatureId);
        creature.setFlags(set);
        return creature;
    }

}
