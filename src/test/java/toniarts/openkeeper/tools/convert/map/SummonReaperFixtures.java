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
 * Builds the level data the Summon Reaper spell needs in tests. Lives in the
 * data classes' own package because their setters are {@code protected}
 * (parser-only).
 */
public final class SummonReaperFixtures {

    private SummonReaperFixtures() {
    }

    public static KeeperSpell keeperSpell(short keeperSpellId, short shotTypeId, int shotData1, int manaCost, int manaDrain) {
        KeeperSpell keeperSpell = new KeeperSpell();
        keeperSpell.setKeeperSpellId(keeperSpellId);
        keeperSpell.setName("Spell " + keeperSpellId);
        keeperSpell.setShotTypeId(shotTypeId);
        keeperSpell.setShotData1(shotData1);
        keeperSpell.setManaCost(manaCost);
        keeperSpell.setManaDrain(manaDrain);
        keeperSpell.setCastRule(KeeperSpell.CastRule.OWN_LAND);
        keeperSpell.setTargetRule(KeeperSpell.TargetRule.LAND);
        return keeperSpell;
    }

    public static Shot shot(short shotId, Shot.ProcessType processType) {
        Shot shot = new Shot();
        shot.setShotId(shotId);
        shot.setName("Shot " + shotId);
        shot.setProcessType(processType);
        return shot;
    }

    public static Creature creature(short creatureId, Creature.CreatureFlag... flags) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setName("Creature " + creatureId);
        EnumSet<Creature.CreatureFlag> flagSet = EnumSet.noneOf(Creature.CreatureFlag.class);
        for (Creature.CreatureFlag flag : flags) {
            flagSet.add(flag);
        }
        creature.setFlags(flagSet);
        return creature;
    }

    public static Variable.MiscVariable miscVariable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

    public static GameLevel gameLevel(short oneShotHornyLevel) {
        GameLevel gameLevel = new GameLevel();
        gameLevel.setOneShotHornyLev(oneShotHornyLevel);
        return gameLevel;
    }

}
