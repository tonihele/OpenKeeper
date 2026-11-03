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
 * Builds level variable and creature fixtures for the casino tests. Lives in
 * the data classes' own package because their setters are {@code protected}
 * (parser-only).
 */
public final class CasinoVariableFixtures {

    private CasinoVariableFixtures() {
    }

    public static Variable.MiscVariable miscVariable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

    /**
     * A creature with the given animations, each lasting a second
     *
     * @param creatureId the creature id
     * @param animations the animation slots that are filled
     * @return the creature
     */
    public static Creature creatureWithAnimations(short creatureId, Creature.AnimationType... animations) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        for (Creature.AnimationType animation : animations) {
            ArtResource resource = new ArtResource();
            resource.setName("Test-" + animation);
            resource.setType(ArtResource.ArtResourceType.ANIMATING_MESH);
            resource.setData(ArtResource.KEY_FRAMES, 30);
            resource.setData(ArtResource.KEY_FPS, 30);
            creature.setAnimation(animation, resource);
        }
        return creature;
    }

}
