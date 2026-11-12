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
public final class CreatureFixtures {

    private CreatureFixtures() {
    }

    /**
     * A creature with one animation slot filled
     *
     * @param animation the animation slot
     * @param resourceType the resource in the slot, {@code NONE} for an empty
     * slot as in the data
     * @return the creature
     */
    public static Creature creatureWithAnimation(Creature.AnimationType animation, ArtResource.ArtResourceType resourceType) {
        ArtResource resource = new ArtResource();
        resource.setName(resourceType == ArtResource.ArtResourceType.NONE ? "" : "Test-" + animation);
        resource.setType(resourceType);

        Creature creature = new Creature();
        creature.setAnimation(animation, resource);
        return creature;
    }

}
