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
package toniarts.openkeeper.view.control;

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureFixtures;
import toniarts.openkeeper.view.text.CreatureTextParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatureViewControlTest {

    /**
     * The Mistress walk, Mis_Walk: 48 frames at 20 a second, start distance 573
     */
    private static final float MISTRESS_START_DISTANCE = 573 / 4096f;

    @Test
    void creatureOfAnotherKeeperHasNoTooltip() {
        DefaultEntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponent(creature, new Owner((short) 4, (short) 4));

        CreatureViewControl control = new CreatureViewControl(creature, entityData, new Creature(), null, null, null, new CreatureTextParser(null));

        assertEquals("", control.getTooltip((short) 3));
    }

    @Test
    void creatureBeingRemovedIsControlledByNoPlayer() {
        DefaultEntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponent(creature, new Owner((short) 3, (short) 3));
        CreatureViewControl control = new CreatureViewControl(creature, entityData, new Creature(), null, null, null, new CreatureTextParser(null));

        // The game logic removes the creature while it is still under the cursor
        entityData.removeEntity(creature);
        control.update(0);

        assertEquals(-1, control.getControlId());
    }

    @Test
    void walkPlaysAtItsFrameRateAtTheStartDistanceSpeed() {
        CreatureViewControl control = mistress(Creature.AnimationType.WALK, 573);

        // A tenth of a second at 573/4096 tiles a second, the 20 frames a second of the walk on clips timed at 30
        assertEquals(20 / 30f, control.getWalkSpeed(MISTRESS_START_DISTANCE / 10, 0, 0.1f), 1e-4f);

        // Twice as fast plays twice as fast
        assertEquals(40 / 30f, control.getWalkSpeed(0, -MISTRESS_START_DISTANCE / 5, 0.1f), 1e-4f);
    }

    @Test
    void walkCyclePerStrideOfTheMistress() {
        CreatureViewControl control = mistress(Creature.AnimationType.WALK, 573);

        // One cycle of 48 frames every 48 / 20 * 573 / 4096 = 0.34 tiles, here walked in 30 frames of 1/60 s
        float stride = 48 / 20f * MISTRESS_START_DISTANCE;
        float tpf = 1 / 60f;
        float framesPerStep = control.getWalkSpeed(stride / 30, 0, tpf) * tpf * 30;
        assertEquals(0.336f, stride, 1e-3f);
        float frames = framesPerStep * 30;
        assertEquals(48, frames, 1e-2f);
    }

    @Test
    void diagonalStepIsTheLongerSidePlusHalfTheShorter() {
        CreatureViewControl control = mistress(Creature.AnimationType.WALK, 573);

        float side = MISTRESS_START_DISTANCE / 10;
        assertEquals(1.5f * 20 / 30f, control.getWalkSpeed(side, side, 0.1f), 1e-4f);
        assertEquals(1.25f * 20 / 30f, control.getWalkSpeed(side / 2, -side, 0.1f), 1e-4f);
    }

    @Test
    void walkPlaysLessThanACycleATick() {
        CreatureViewControl control = mistress(Creature.AnimationType.WALK, 573);
        control.setTicksPerSecond(4);

        // Dropped five tiles away in one frame
        float framesPerSecond = control.getWalkSpeed(5, 0, 1 / 60f) * 30;
        assertEquals(48 * 4 * 4095 / 4096f, framesPerSecond, 1e-2f);
    }

    @Test
    void walkWithoutStartDistanceDoesNotPlay() {
        CreatureViewControl control = mistress(Creature.AnimationType.WALK, 0);

        assertEquals(0, control.getWalkSpeed(MISTRESS_START_DISTANCE / 10, 0, 0.1f));
    }

    @Test
    void runIsPlayedByTheDistanceButOtherAnimationsAreNot() {
        assertEquals(20 / 30f, mistress(Creature.AnimationType.RUN, 573).getWalkSpeed(MISTRESS_START_DISTANCE / 10, 0, 0.1f), 1e-4f);
        assertEquals(0, mistress(Creature.AnimationType.RUN, 573).getWalkSpeed(0, 0, 0.1f));

        // A drunk walk, stealth walk or idle plays at the speed of its clip
        assertEquals(1, mistress(Creature.AnimationType.DRUNKED_WALK, 573).getWalkSpeed(MISTRESS_START_DISTANCE / 10, 0, 0.1f));
        assertEquals(1, mistress(Creature.AnimationType.STEALTH_WALK, 573).getWalkSpeed(MISTRESS_START_DISTANCE / 10, 0, 0.1f));
        assertEquals(1, mistress(Creature.AnimationType.IDLE_1, 573).getWalkSpeed(MISTRESS_START_DISTANCE / 10, 0, 0.1f));
    }

    private static CreatureViewControl mistress(Creature.AnimationType animation, int startDistance) {
        DefaultEntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        Creature data = CreatureFixtures.creatureWithAnimation(animation, 48, 20, startDistance);
        return new CreatureViewControl(creature, entityData, data, animation, null, null);
    }

}
