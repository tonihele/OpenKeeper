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

import java.util.EnumSet;
import java.util.List;

/**
 * Builds {@link Creature} fixtures for tests. Lives in the data classes' own
 * package because their setters are {@code protected} (parser-only).
 */
public final class CreatureFixtures {

    private CreatureFixtures() {
    }

    public static Creature creature(short creatureId, short tortureMoodChange, Creature.JobClass jobClass, boolean emotionless) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setJobClass(jobClass);
        creature.setFlags2(emotionless ? EnumSet.of(Creature.CreatureFlag2.IS_EMOTIONLESS) : EnumSet.noneOf(Creature.CreatureFlag2.class));
        creature.getAttributes().setTortureMoodChange(tortureMoodChange);
        return creature;
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

    /**
     * Fills an animation slot of the creature
     *
     * @param creature the creature
     * @param animation the animation slot
     * @param frames the length of the animation in frames
     * @param fps the frames a second
     * @return the same creature
     */
    public static Creature withAnimation(Creature creature, Creature.AnimationType animation, int frames, int fps) {
        ArtResource resource = new ArtResource();
        resource.setName("Test-" + animation);
        resource.setType(ArtResource.ArtResourceType.ANIMATING_MESH);
        resource.setData(ArtResource.KEY_FRAMES, frames);
        resource.setData(ArtResource.KEY_FPS, fps);
        creature.setAnimation(animation, resource);
        return creature;
    }

    /**
     * A creature with one animation of the given length
     *
     * @param animation the animation slot
     * @param frames the frames in the animation
     * @param fps the frames per second
     * @return the creature
     */
    public static Creature creatureWithAnimation(Creature.AnimationType animation, int frames, int fps) {
        ArtResource resource = new ArtResource();
        resource.setData("frames", frames);
        resource.setData("fps", fps);

        Creature creature = new Creature();
        creature.setAnimation(animation, resource);
        return creature;
    }

    /**
     * A creature with one animation that plays by the distance walked
     *
     * @param animation the animation slot
     * @param frames the frames in the animation
     * @param fps the frames per second
     * @param startDistance the speed, in 1/4096 tiles a second, at which the
     * frames play at their frame rate
     * @return the creature
     */
    public static Creature creatureWithAnimation(Creature.AnimationType animation, int frames, int fps, int startDistance) {
        Creature creature = creatureWithAnimation(animation, frames, fps);
        creature.getAnimation(animation).setData(ArtResource.KEY_START_DIST, startDistance);
        return creature;
    }

    /**
     * A creature that works in the workshop
     *
     * @param id the creature ID
     * @param manufacturePerSecond the manufacture points per second in the
     * creature data
     * @return the creature
     */
    public static Creature manufacturer(short id, int manufacturePerSecond) {
        Creature creature = new Creature();
        creature.setCreatureId(id);
        creature.setSpells(List.of());
        creature.getAttributes().setManufacturePerSecond(manufacturePerSecond);
        return creature;
    }

}
