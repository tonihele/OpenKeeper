/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.navigation.steering;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.tools.convert.map.Creature;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatureSizingTest {
    @ParameterizedTest
    @ValueSource(floats = {.14f, .32f, .44f, .46f})
    void bodyRadiusComesFromTheAuthoredWidth(float width) throws Exception {
        Creature definition = new Creature();
        var field = definition.getAttributes().getClass().getDeclaredField("width");
        field.setAccessible(true);
        field.setFloat(definition.getAttributes(), width);
        assertEquals(width / 2, CreatureSizing.getBodyRadius(definition));
        assertEquals(width, CreatureSizing.getBodyWidth(definition));
    }
}
