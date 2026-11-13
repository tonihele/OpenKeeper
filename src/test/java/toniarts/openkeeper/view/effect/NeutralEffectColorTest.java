/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view.effect;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Player;
import static org.junit.jupiter.api.Assertions.*;

class NeutralEffectColorTest {
    @Test
    void neutralTintSamplesSevenColoursInSharedHundredMillisecondSteps() {
        var colours = new HashSet<Short>();
        for (long time = 0; time < 700; time += 100) {
            short colour = VisualEffect.colorOwnerId(Player.NEUTRAL_PLAYER_ID, time);
            colours.add(colour);
            assertEquals(colour, VisualEffect.colorOwnerId(Player.NEUTRAL_PLAYER_ID, time + 99));
            assertEquals(colour, VisualEffect.colorOwnerId(Player.NEUTRAL_PLAYER_ID, time + 700));
        }
        assertEquals(7, colours.size());
    }

    @Test
    void halfSecondFlameEmissionsVisitEveryColourBeforeRepeating() {
        var colours = new HashSet<Short>();
        for (long time = 0; time < 3500; time += 500) {
            colours.add(VisualEffect.colorOwnerId(Player.NEUTRAL_PLAYER_ID, time));
        }
        assertEquals(7, colours.size());
        assertEquals(VisualEffect.colorOwnerId(Player.NEUTRAL_PLAYER_ID, 0),
                VisualEffect.colorOwnerId(Player.NEUTRAL_PLAYER_ID, 3500));
    }

    @Test
    void neutralCycleIncludesWhiteRatherThanTheNeutralMapTint() {
        assertEquals(java.awt.Color.WHITE,
                VisualEffect.getPlayerColor(Player.NEUTRAL_PLAYER_ID, 100));
    }

    @Test
    void capturedEffectsAlwaysUseTheirKeeperColour() {
        for (short keeper = Player.KEEPER1_ID; keeper <= Player.KEEPER5_ID; keeper++) {
            for (long time = 0; time < 700; time += 100) {
                assertEquals(keeper, VisualEffect.colorOwnerId(keeper, time));
            }
        }
    }
}
