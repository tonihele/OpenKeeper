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
package toniarts.openkeeper.game.controller.player;

import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.PlayerSpell;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ResearchOrderFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.ResearchOrder;
import toniarts.openkeeper.tools.convert.map.Variable.ResearchOrder.ResearchOrderType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * DK II researches in the order of the research order variables, and each
 * step takes the research points given there, not the research times of the
 * rooms, doors, traps and spells
 */
class ResearchOrderTest {

    private static final short SPELL_A = 4;
    private static final short SPELL_B = 6;
    private static final short DOOR = 1;
    private static final short ROOM = 7;

    @Test
    void researchesInTheResearchOrder() {
        Keeper keeper = new Keeper(false, (short) 3);
        PlayerSpell spellA = addSpell(keeper, SPELL_A, false);
        PlayerSpell spellB = addSpell(keeper, SPELL_B, false);
        ResearchableEntity door = addDoor(keeper, false);

        PlayerResearchControl researchControl = researchControl(keeper,
                step(ResearchOrderType.SPELL, SPELL_B), step(ResearchOrderType.DOOR, DOOR), step(ResearchOrderType.SPELL, SPELL_A));
        researchControl.initialize();
        assertSame(spellB, keeper.getCurrentResearch());

        assertSame(spellB, researchControl.research(1000));
        assertSame(door, keeper.getCurrentResearch());
        spellB.setDiscovered(true); // The spellbook materializes

        assertSame(door, researchControl.research(1000));
        assertSame(spellA, keeper.getCurrentResearch());
    }

    @Test
    void discoveringTakesTheResearchPointsOfTheOrder() {
        Keeper keeper = new Keeper(false, (short) 3);
        PlayerSpell spell = addSpell(keeper, SPELL_A, false);

        // The spell's own research time is 500
        PlayerResearchControl researchControl = researchControl(keeper, step(ResearchOrderType.SPELL, SPELL_A));
        researchControl.initialize();

        assertNull(researchControl.research(999));
        assertSame(spell, researchControl.research(1));
    }

    @Test
    void upgradesComeAfterAllTheDiscoveries() {
        Keeper keeper = new Keeper(false, (short) 3);
        PlayerSpell spellA = addSpell(keeper, SPELL_A, true);
        addSpell(keeper, SPELL_B, true);
        ResearchableEntity door = addDoor(keeper, false);

        PlayerResearchControl researchControl = researchControl(keeper,
                step(ResearchOrderType.SPELL, SPELL_A), step(ResearchOrderType.SPELL, SPELL_B), step(ResearchOrderType.DOOR, DOOR));
        researchControl.initialize();
        assertSame(door, keeper.getCurrentResearch());

        researchControl.research(1000);
        assertSame(spellA, keeper.getCurrentResearch());
    }

    @Test
    void upgradingTakesTheResearchPointsOfTheOrder() {
        Keeper keeper = new Keeper(false, (short) 3);
        PlayerSpell spell = addSpell(keeper, SPELL_A, true);

        // The spell's own upgrade research time is 1500
        PlayerResearchControl researchControl = researchControl(keeper, step(ResearchOrderType.SPELL, SPELL_A));
        researchControl.initialize();

        assertNull(researchControl.research(999));
        assertSame(spell, researchControl.research(1));
    }

    @Test
    void whatIsNotInTheOrderIsNotResearched() {
        Keeper keeper = new Keeper(false, (short) 3);
        keeper.getAvailableRooms().add(new ResearchableEntity(ROOM, ResearchableType.ROOM));

        PlayerResearchControl researchControl = researchControl(keeper, step(ResearchOrderType.SPELL, SPELL_A));
        researchControl.initialize();

        assertFalse(researchControl.isAnythingToReaseach());
    }

    private static PlayerSpell addSpell(Keeper keeper, short id, boolean discovered) {
        PlayerSpell spell = new PlayerSpell(id, discovered);
        keeper.getAvailableSpells().add(spell);
        return spell;
    }

    private static ResearchableEntity addDoor(Keeper keeper, boolean discovered) {
        ResearchableEntity door = new ResearchableEntity(DOOR, discovered, ResearchableType.DOOR);
        keeper.getAvailableDoors().add(door);
        return door;
    }

    private static ResearchOrder step(ResearchOrderType type, short id) {
        return ResearchOrderFixtures.researchOrder(type, id, 1000);
    }

    private static PlayerResearchControl researchControl(Keeper keeper, ResearchOrder... researchOrder) {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getResearchOrder" -> List.of(researchOrder);
                    case "getKeeperSpellById" -> ResearchOrderFixtures.keeperSpell(((Integer) args[0]).shortValue(), 500, 1500);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new PlayerResearchControl(keeper, kwdFile);
    }

}
