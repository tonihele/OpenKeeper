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
package toniarts.openkeeper.view.text;

import com.simsilica.es.Entity;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Objective;
import toniarts.openkeeper.tools.convert.map.GameLevel.TextTable;
import toniarts.openkeeper.tools.convert.map.Thing;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The texts DK II picks for the creature tooltip: the name of the Lord of the
 * Land and the job of a creature on its way somewhere, and what the Query Tool
 * shows of a creature (0x48D340)
 */
class CreatureTextParserTest {

    @Test
    void namesTheLordOfTheLandAfterTheCampaignLevel() {
        assertEquals("2866", CreatureTextParser.getLordOfTheLandNameId(TextTable.LEVEL_1));
        assertEquals("2870", CreatureTextParser.getLordOfTheLandNameId(TextTable.LEVEL_5));
        assertEquals("2871", CreatureTextParser.getLordOfTheLandNameId(TextTable.LEVEL_6A));
        assertEquals("2871", CreatureTextParser.getLordOfTheLandNameId(TextTable.LEVEL_6B));
        assertEquals("2872", CreatureTextParser.getLordOfTheLandNameId(TextTable.LEVEL_7));
        assertEquals("2876", CreatureTextParser.getLordOfTheLandNameId(TextTable.LEVEL_12));
        assertEquals("2878", CreatureTextParser.getLordOfTheLandNameId(TextTable.LEVEL_15B));
        assertEquals("2879", CreatureTextParser.getLordOfTheLandNameId(TextTable.LEVEL_16));
    }

    @Test
    void callsTheLordOfTheLandMichaelOnTheOtherLevels() {
        assertEquals("3079", CreatureTextParser.getLordOfTheLandNameId(TextTable.LEVEL_11A));
        assertEquals("3079", CreatureTextParser.getLordOfTheLandNameId(TextTable.LEVEL_13));
        assertEquals("3079", CreatureTextParser.getLordOfTheLandNameId(TextTable.LEVEL_17));
        assertEquals("3079", CreatureTextParser.getLordOfTheLandNameId(TextTable.MY_PET_DUNGEON_2));
        assertEquals("3079", CreatureTextParser.getLordOfTheLandNameId(TextTable.NONE));
        assertEquals("3079", CreatureTextParser.getLordOfTheLandNameId(null));
    }

    @Test
    void showsTheJobAHeroIsHeadingTo() {
        assertEquals("2645", CreatureTextParser.getEnRouteTextId(objective(Thing.HeroParty.Objective.KILL_PLAYER), false));
        assertEquals("2645", CreatureTextParser.getEnRouteTextId(objective(Thing.HeroParty.Objective.KILL_CREATURES), false));
        assertEquals("2642", CreatureTextParser.getEnRouteTextId(objective(Thing.HeroParty.Objective.STEAL_GOLD), false));
        assertEquals("2643", CreatureTextParser.getEnRouteTextId(objective(Thing.HeroParty.Objective.STEAL_GOLD), true));
        assertEquals("2599", CreatureTextParser.getEnRouteTextId(objective(Thing.HeroParty.Objective.SEND_TO_ACTION_POINT), false));
    }

    @Test
    void isEnRouteWithoutAnObjective() {
        assertEquals("2670", CreatureTextParser.getEnRouteTextId(null, false));
        assertEquals("2670", CreatureTextParser.getEnRouteTextId(objective(Thing.HeroParty.Objective.NONE), false));
    }

    @Test
    void theQueryToolShowsTheBloodGroupTheWageAndTheGoldHeld() {
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.bloodType = "AB+";
        creatureComponent.pay = 270;
        Entity entity = (Entity) Proxy.newProxyInstance(Entity.class.getClassLoader(), new Class<?>[]{Entity.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "get" -> args[0] == CreatureComponent.class ? creatureComponent
                            : args[0] == Gold.class ? new Gold(125, 500) : null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        // The end of text 2409
        assertEquals("Blood Group: AB+.  Current Wage: 270.  Gold Held: 125.",
                new CreatureTextParser(null).parseText("Blood Group: %34.  Current Wage: %35.  Gold Held: %36.", entity, null));
    }

    private static Objective objective(Thing.HeroParty.Objective objective) {
        return new Objective(objective, (short) 0, 0);
    }

}
