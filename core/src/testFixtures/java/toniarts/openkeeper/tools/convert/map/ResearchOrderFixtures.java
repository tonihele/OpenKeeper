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

/**
 * Builds research order and keeper spell fixtures for the research tests.
 * Lives in the data classes' own package because their setters are
 * {@code protected} (parser-only).
 */
public final class ResearchOrderFixtures {

    private ResearchOrderFixtures() {
    }

    public static Variable.ResearchOrder researchOrder(Variable.ResearchOrder.ResearchOrderType type, int typeId, int researchPoints) {
        Variable.ResearchOrder researchOrder = new Variable.ResearchOrder();
        researchOrder.setType(type);
        researchOrder.setTypeId(typeId);
        researchOrder.setResearchPoints(researchPoints);
        return researchOrder;
    }

    public static KeeperSpell keeperSpell(short id, int researchTime, int bonusResearchTime) {
        KeeperSpell keeperSpell = new KeeperSpell();
        keeperSpell.setKeeperSpellId(id);
        keeperSpell.setName("Spell " + id);
        keeperSpell.setResearchTime(researchTime);
        keeperSpell.setBonusRTime(bonusResearchTime);
        return keeperSpell;
    }

}
