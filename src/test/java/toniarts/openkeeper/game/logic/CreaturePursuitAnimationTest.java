package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.*;

class CreaturePursuitAnimationTest {
    private final DefaultEntityData data = new DefaultEntityData();

    @AfterEach
    void close() {
        data.close();
    }

    @Test
    void pursuitRunsContinuouslyThroughRouteChangesThenReturnsToAttackAndWalk() {
        EntityId creature = data.createEntity();
        data.setComponents(creature, new Position(0, new Vector3f()),
                new CreatureViewState((short) 7, 0, AnimationType.WALK),
                new CreatureAi(0, CreatureState.FIGHT, (short) 7), navigation(3, true));
        var system = new CreatureViewSystem(data);
        try {
            system.processTick(.1f);
            var running = data.getComponent(creature, CreatureViewState.class);
            assertEquals(AnimationType.RUN, running.state);
            data.setComponent(creature, navigation(4, true));
            system.processTick(.1f);
            assertSame(running, data.getComponent(creature, CreatureViewState.class),
                    "Refreshing pursuit must not restart the RUN/start sequence");

            data.removeComponent(creature, Navigation.class);
            data.setComponent(creature, new CreatureAi(0, CreatureState.MELEE_ATTACK, (short) 7));
            system.processTick(.1f);
            assertEquals(AnimationType.MELEE_ATTACK, data.getComponent(creature, CreatureViewState.class).state);
            data.setComponent(creature, new CreatureAi(0, CreatureState.CAST_SPELL, (short) 7));
            system.processTick(.1f);
            assertEquals(AnimationType.CAST_SPELL, data.getComponent(creature, CreatureViewState.class).state);

            data.setComponent(creature, navigation(5, false));
            system.processTick(.1f);
            assertEquals(AnimationType.WALK, data.getComponent(creature, CreatureViewState.class).state);
            data.removeComponent(creature, Navigation.class);
            data.setComponent(creature, new CreatureAi(0, CreatureState.IDLE, (short) 7));
            system.processTick(.1f);
            assertEquals(AnimationType.IDLE_1, data.getComponent(creature, CreatureViewState.class).state);
        } finally {
            system.stop();
        }
    }

    @Test
    void fallingStillTakesPriorityOverPursuit() {
        EntityId creature = data.createEntity();
        data.setComponents(creature, new Position(0, new Vector3f()),
                new CreatureViewState((short) 7, 0, AnimationType.IDLE_1),
                navigation(3, true), new CreatureFall());
        var system = new CreatureViewSystem(data);
        try {
            system.processTick(.1f);
            assertEquals(AnimationType.FALLBACK, data.getComponent(creature, CreatureViewState.class).state);
        } finally {
            system.stop();
        }
    }

    private Navigation navigation(int x, boolean pursuit) {
        return new Navigation(new Point(x, 1), null, List.of(), pursuit);
    }
}
