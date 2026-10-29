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
package toniarts.openkeeper.gui.nifty;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.gui.nifty.AbstractCreatureCardControl.CreatureUIState;
import static toniarts.openkeeper.gui.nifty.AbstractCreatureCardControl.CreatureUIState.*;

/**
 * Tests which creature panel categories a creature falls in.
 */
public class CreatureUIStateTest {

    private static final int THRESHOLD = CreatureMood.toRuntimeThreshold(50);

    private final EntityData entityData = new DefaultEntityData();

    private Set<CreatureUIState> categories(EntityComponent... components) {
        EntityId id = entityData.createEntity();
        entityData.setComponents(id, components);
        Entity entity = entityData.getEntity(id, CreatureAi.class, AttackTarget.class, TaskComponent.class, CreatureMood.class);
        Set<CreatureUIState> result = EnumSet.noneOf(CreatureUIState.class);
        for (CreatureUIState state : CreatureUIState.values()) {
            if (state.matches(entity, THRESHOLD)) {
                result.add(state);
            }
        }
        return result;
    }

    private static CreatureAi ai(CreatureState state) {
        return new CreatureAi(0, state, (short) 1);
    }

    @Test
    public void idleCreatureIsIdleAndHappy() {
        assertEquals(EnumSet.of(IDLE, HAPPY), categories(ai(CreatureState.IDLE)));
    }

    @Test
    public void fightingCreatureIsNotBusy() {
        assertEquals(EnumSet.of(FIGHT, HAPPY),
                categories(ai(CreatureState.FIGHT), new AttackTarget(entityData.createEntity())));
    }

    @Test
    public void creatureWithTaskIsBusyAndWorking() {
        assertEquals(EnumSet.of(BUSY, WORK, HAPPY),
                categories(ai(CreatureState.WORK), new TaskComponent(1, null, null, null, 0, false)));
    }

    @Test
    public void moodFollowsAngerState() {
        assertEquals(EnumSet.of(BUSY, UNHAPPY),
                categories(ai(CreatureState.WORK), new CreatureMood(THRESHOLD, 0, 0, 0, 0, 0)));
        assertEquals(EnumSet.of(BUSY, ANGRY),
                categories(ai(CreatureState.WORK), new CreatureMood(0, 0, 0, 0, 0, 40000)));
    }
}
