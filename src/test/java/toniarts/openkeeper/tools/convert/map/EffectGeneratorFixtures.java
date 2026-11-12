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

import java.util.List;

public final class EffectGeneratorFixtures {
    private EffectGeneratorFixtures() {
    }

    public static Thing.EffectGenerator generator(int id, int frequency, int width, int height, Integer... effects) {
        var thing = new Thing.EffectGenerator();
        thing.setId((short) id);
        thing.setPosX(10);
        thing.setPosY(20);
        thing.setWidth(width);
        thing.setHeight(height);
        thing.setFrequency((short) frequency);
        thing.setEffectIds(List.of(effects));
        return thing;
    }
}
