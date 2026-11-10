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
 * Builds {@link Creature} fixtures for tests. Lives in {@code Creature}'s own
 * package because its setters are {@code protected} (parser-only).
 */
public final class CreatureMoodFixtures {

    private CreatureMoodFixtures() {
    }

    public static Creature creature(short creatureId, String name, short lairObjectId,
            short angerNoLair, short unhappyThreshold) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setName(name);
        creature.setLairObjectId(lairObjectId);
        creature.getAttributes().setAngerNoLair(angerNoLair);
        creature.getAttributes().setUnhappyThreshold(unhappyThreshold);
        return creature;
    }

}
