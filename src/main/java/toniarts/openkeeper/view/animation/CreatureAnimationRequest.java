/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;

/** Immutable identity copied from replicated state, independent of Position changes. */
public record CreatureAnimationRequest(short creatureId, AnimationType animation, double startTime, long sequence) {
    public CreatureAnimationRequest(CreatureViewState state) {
        this(state.creatureId, state.state, state.stateStartTime, state.sequence);
    }
}
