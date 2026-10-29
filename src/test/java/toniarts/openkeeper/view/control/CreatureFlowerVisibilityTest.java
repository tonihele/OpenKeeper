package toniarts.openkeeper.view.control;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.tools.convert.map.Creature;

import static org.junit.jupiter.api.Assertions.*;

class CreatureFlowerVisibilityTest {
    private final DefaultEntityData data = new DefaultEntityData();
    private final EntityId creature = data.createEntity();
    // Use the shipped material definition, but no converted/proprietary textures or GPU.
    private final DesktopAssetManager assets = new DesktopAssetManager(true) {
        @Override
        public Texture loadTexture(String name) {
            return new Texture2D();
        }
    };
    private final CreatureFlowerControl flower = new CreatureFlowerControl(creature, data, new Creature(), assets);

    @BeforeEach
    void setUp() {
        data.setComponents(creature, new Owner((short) 3, (short) 3), new Health(100, 100));
        flower.update(0);
    }

    @AfterEach
    void close() {
        flower.cleanup();
        data.close();
    }

    @Test
    void combatStatesHoldFlowerButOrdinaryStatesDoNot() {
        data.setComponent(creature, new Health(100, 100));
        for (CreatureState state : CreatureState.values()) {
            data.setComponent(creature, new CreatureAi(0, state, (short) 7));
            flower.update(0);
            boolean combat = state == CreatureState.FIGHT || state == CreatureState.MELEE_ATTACK
                    || state == CreatureState.CAST_SPELL;
            assertEquals(combat, flower.isPersistentVisibilityRequired(), state.name());
        }
    }

    @Test
    void criticalHealthUsesStrictNativeIntegerBoundary() {
        assertHealth(19, 120, true);
        assertHealth(20, 120, false);
        assertHealth(21, 120, false);
        assertHealth(82, 500, true);
        assertHealth(83, 500, false);
        assertHealth(1, Integer.MAX_VALUE, true);
        assertHealth(0, 120, false);
        assertHealth(-1, 120, false);
        assertHealth(1, 0, false);
        data.removeComponent(creature, Health.class);
        flower.update(0);
        assertFalse(flower.isPersistentVisibilityRequired());
    }

    @Test
    void existingAngryMoodStillHoldsFlower() {
        data.setComponents(creature, new Health(100, 100), new CreatureMood(40000, 0, 0, 0, 0, 0));
        flower.update(0);
        assertTrue(flower.isPersistentVisibilityRequired());
    }

    @Test
    void combatAutoShowsWithoutHoverSurvivesTimerAndHidesAfterwards() {
        Node model = new Node();
        data.setComponent(creature, new Health(100, 100));
        model.addControl(flower);
        flower.update(3);
        assertFalse(flower.isEnabled());
        data.setComponent(creature, new CreatureAi(0, CreatureState.FIGHT, (short) 7));
        flower.update(.1f);
        assertTrue(flower.isEnabled());
        flower.update(10);
        assertTrue(flower.isEnabled());
        assertEquals(1, model.getQuantity());
        data.setComponent(creature, new CreatureAi(0, CreatureState.IDLE, (short) 7));
        flower.update(.1f);
        assertFalse(flower.isEnabled());
        assertEquals(0, model.getQuantity());
    }

    @Test
    void lowHealthAutoShowsUntilRecoveryAndPreservesFogCulling() {
        Node model = new Node();
        model.setCullHint(Spatial.CullHint.Always);
        data.setComponent(creature, new Health(100, 100));
        model.addControl(flower);
        flower.update(3);
        assertFalse(flower.isEnabled());
        data.setComponent(creature, new Health(10, 100));
        flower.update(.1f);
        flower.update(10);
        assertTrue(flower.isEnabled());
        assertEquals(Spatial.CullHint.Always, model.getCullHint());
        data.setComponent(creature, new Health(100, 100));
        flower.update(.1f);
        assertFalse(flower.isEnabled());
    }

    private void assertHealth(int current, int maximum, boolean expected) {
        data.setComponent(creature, new Health(current, maximum));
        flower.update(0);
        assertEquals(expected, flower.isPersistentVisibilityRequired(), current + "/" + maximum);
    }
}
