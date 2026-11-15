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
package toniarts.openkeeper.game.controller.room;

import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.TempleFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.SacrificeRewardType;
import toniarts.openkeeper.tools.convert.map.Variable.SacrificeType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SacrificeHistoryTest {

    private static final short IMP = 1;
    private static final short BILE_DEMON = 3;
    private static final short MISTRESS = 4;
    private static final short DARK_ELF = 6;
    private static final short GOBLIN = 7;
    private static final short TROLL = 10;
    private static final short FAIRY = 20;

    private static final Variable.Sacrifice IMP_AND_DARK_ELF = TempleFixtures.creatureSacrifice(SacrificeRewardType.OBJECT, 5, IMP, DARK_ELF);
    private static final Variable.Sacrifice TWO_MISTRESSES = TempleFixtures.creatureSacrifice(SacrificeRewardType.CREATURE, 9, MISTRESS, MISTRESS);
    private static final Variable.Sacrifice TWO_MISTRESSES_AND_FAIRY = TempleFixtures.creatureSacrifice(SacrificeRewardType.OBJECT, 125, MISTRESS, MISTRESS, FAIRY);
    private static final Variable.Sacrifice GOBLIN_AND_SPELL = TempleFixtures.sacrifice(SacrificeRewardType.CREATURE, 14,
            new SacrificeType[]{SacrificeType.CREATURE, SacrificeType.KEEPER_SPELL}, new int[]{GOBLIN, 4});

    @Test
    void creaturesSacrificedInTheRecipeOrderMatchIt() {
        SacrificeHistory history = new SacrificeHistory();

        assertNull(history.offer(SacrificeType.CREATURE, IMP, 1, List.of(IMP_AND_DARK_ELF)));
        SacrificeHistory.Match match = history.offer(SacrificeType.CREATURE, DARK_ELF, 1, List.of(IMP_AND_DARK_ELF));

        assertNotNull(match);
        assertSame(IMP_AND_DARK_ELF, match.recipe());
    }

    @Test
    void theOrderOfTheSacrificesMatters() {
        SacrificeHistory history = new SacrificeHistory();

        history.offer(SacrificeType.CREATURE, DARK_ELF, 1, List.of(IMP_AND_DARK_ELF));

        assertNull(history.offer(SacrificeType.CREATURE, IMP, 1, List.of(IMP_AND_DARK_ELF)));
    }

    @Test
    void onlyTheLatestSacrificesHaveToMatch() {
        SacrificeHistory history = new SacrificeHistory();

        history.offer(SacrificeType.CREATURE, TROLL, 1, List.of(IMP_AND_DARK_ELF));
        history.offer(SacrificeType.CREATURE, IMP, 1, List.of(IMP_AND_DARK_ELF));

        assertNotNull(history.offer(SacrificeType.CREATURE, DARK_ELF, 1, List.of(IMP_AND_DARK_ELF)));
    }

    @Test
    void onlyTheLastThreeSacrificesAreRemembered() {
        SacrificeHistory history = new SacrificeHistory();
        List<Variable.Sacrifice> recipes = List.of(TWO_MISTRESSES_AND_FAIRY);

        history.offer(SacrificeType.CREATURE, MISTRESS, 1, recipes);
        history.offer(SacrificeType.CREATURE, MISTRESS, 1, recipes);
        history.offer(SacrificeType.CREATURE, BILE_DEMON, 1, recipes);

        assertEquals(3, history.size());
        assertNull(history.offer(SacrificeType.CREATURE, FAIRY, 1, recipes));
        assertEquals(3, history.size());
    }

    @Test
    void aMatchStartsTheSacrificesOver() {
        SacrificeHistory history = new SacrificeHistory();

        history.offer(SacrificeType.CREATURE, IMP, 1, List.of(IMP_AND_DARK_ELF));
        history.offer(SacrificeType.CREATURE, DARK_ELF, 1, List.of(IMP_AND_DARK_ELF));

        assertEquals(0, history.size());
        assertNull(history.offer(SacrificeType.CREATURE, DARK_ELF, 1, List.of(IMP_AND_DARK_ELF)));
    }

    @Test
    void theFirstMatchingRecipeWins() {
        SacrificeHistory history = new SacrificeHistory();
        List<Variable.Sacrifice> recipes = List.of(TWO_MISTRESSES, TWO_MISTRESSES_AND_FAIRY);

        history.offer(SacrificeType.CREATURE, MISTRESS, 1, recipes);
        SacrificeHistory.Match match = history.offer(SacrificeType.CREATURE, MISTRESS, 1, recipes);

        assertSame(TWO_MISTRESSES, match.recipe());
    }

    @Test
    void aRewardCreatureGetsTheAverageLevelOfTheSacrificedCreatures() {
        SacrificeHistory history = new SacrificeHistory();

        history.offer(SacrificeType.CREATURE, MISTRESS, 3, List.of(TWO_MISTRESSES));
        SacrificeHistory.Match match = history.offer(SacrificeType.CREATURE, MISTRESS, 6, List.of(TWO_MISTRESSES));

        assertEquals(4, match.level());
    }

    @Test
    void keeperSpellsCountTowardsTheRecipe() {
        SacrificeHistory history = new SacrificeHistory();

        history.offer(SacrificeType.CREATURE, GOBLIN, 5, List.of(GOBLIN_AND_SPELL));
        assertNull(history.offer(SacrificeType.CREATURE, (short) 4, 1, List.of(GOBLIN_AND_SPELL)));
        history.offer(SacrificeType.CREATURE, GOBLIN, 5, List.of(GOBLIN_AND_SPELL));
        SacrificeHistory.Match match = history.offer(SacrificeType.KEEPER_SPELL, (short) 4, 0, List.of(GOBLIN_AND_SPELL));

        assertSame(GOBLIN_AND_SPELL, match.recipe());
        assertEquals(5, match.level());
    }

}
