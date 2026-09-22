/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.state;

import java.util.function.Function;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.CreatureFlag;

/** Resolves an elite creature to the regular creature's shared HUD card. */
public final class CreaturePortraitResolver {

    private CreaturePortraitResolver() {
    }

    public static short cardTypeId(Creature creature) {
        return creature.getFlags().contains(CreatureFlag.IS_UNIQUE)
                ? creature.getCloneCreatureId() : creature.getCreatureId();
    }

    public static ArtResource resolve(Creature creature, Function<Short, Creature> creaturesById) {
        Creature portraitCreature = creature;
        if (creature.getFlags().contains(CreatureFlag.IS_UNIQUE)) {
            portraitCreature = creaturesById.apply(creature.getCloneCreatureId());
        }
        return portraitCreature == null ? null : portraitCreature.getPortraitResource();
    }
}
