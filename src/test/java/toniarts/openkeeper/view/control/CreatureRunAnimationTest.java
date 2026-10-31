package toniarts.openkeeper.view.control;

import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;
import toniarts.openkeeper.tools.convert.map.CreatureAnimationFixtures;

import static org.junit.jupiter.api.Assertions.*;

class CreatureRunAnimationTest {
    private final DefaultEntityData data = new DefaultEntityData();

    @AfterEach
    void close() {
        data.close();
    }

    @Test
    void runLoopsInsteadOfStoppingAfterItsFirstCycle() {
        var run = new ArtResource();
        var creature = CreatureAnimationFixtures.creature(new ArtResource(), run);
        var control = new CreatureViewControl(data.createEntity(), data, creature, AnimationType.RUN, null, null);
        assertSame(run, control.getAnimationData(AnimationType.RUN));
        assertFalse(control.isStopAnimation());
        control.onAnimationCycleDone();
        assertFalse(control.isStopAnimation());
    }

    @Test
    void missingRunUsesWalkWithoutDiscardingTheCreatureModel() {
        var walk = new ArtResource();
        var creature = CreatureAnimationFixtures.creature(walk, null);
        var control = new CreatureViewControl(data.createEntity(), data, creature, AnimationType.RUN, null, null);
        assertSame(walk, control.getAnimationData(AnimationType.RUN));
        assertFalse(control.isStopAnimation());
    }
}
