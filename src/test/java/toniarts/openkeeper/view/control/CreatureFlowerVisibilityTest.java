package toniarts.openkeeper.view.control;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
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
            boolean persistent = state == CreatureState.FIGHT || state == CreatureState.MELEE_ATTACK
                    || state == CreatureState.CAST_SPELL || state == CreatureState.UNCONSCIOUS
                    || state == CreatureState.DRAGGED;
            assertEquals(persistent, flower.isPersistentVisibilityRequired(), state.name());
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

    @Test
    void unconsciousComponentOverridesAnAlreadyVisibleSadFaceAndRecoveryRestoresIt() {
        data.setComponents(creature, new CreatureExperience(1, 0, 100, 0, 0),
                new CreatureMood(100, 0, 0, 0, 0, 0));
        flower.update(0);
        flower.onUpdate(.5f);
        assertEquals("Textures/GUI/moods/SM-Sad.png", flower.getCenterIcon());

        // HealthSystem sets this component independently of the AI state.
        data.setComponents(creature, new Unconscious(0), new Health(0, 100));
        flower.update(0);
        assertEquals("Textures/GUI/moods/SL-01.png", flower.getCenterIcon());
        assertTrue(flower.isPersistentVisibilityRequired());
        // Finish the old mood phase, then enter the unconscious phase.
        flower.onUpdate(.5f);
        assertEquals("Textures/GUI/moods/SL-01.png", flower.getCenterIcon());
        flower.onUpdate(.5f);
        assertEquals("Textures/GUI/moods/SJ-Unconscious.png", flower.getCenterIcon());

        data.removeComponent(creature, Unconscious.class);
        data.setComponent(creature, new Health(100, 100));
        flower.update(0);
        flower.onUpdate(.5f);
        assertEquals("Textures/GUI/moods/SM-Sad.png", flower.getCenterIcon());
    }

    @Test
    void unconsciousIconAlternatesWithLevelForAllKeepersAndNeutralButNotHeroes() {
        data.setComponents(creature, new CreatureExperience(1, 0, 100, 0, 0),
                new CreatureMood(100, 0, 0, 0, 0, 0), new Unconscious(0), new Health(0, 100));
        for (short owner = 1; owner <= 7; owner++) {
            data.setComponent(creature, new Owner(owner, owner));
            flower.update(0);
            flower.onHide();
            assertTrue(flower.isPersistentVisibilityRequired());
            for (int cycle = 0; cycle < 4; cycle++) {
                assertEquals("Textures/GUI/moods/SL-01.png", flower.getCenterIcon());
                flower.onUpdate(.5f);
                assertEquals(owner == 1 ? "Textures/GUI/moods/SL-01.png"
                        : "Textures/GUI/moods/SJ-Unconscious.png", flower.getCenterIcon());
                flower.onUpdate(.5f);
            }
        }
    }

    @Test
    void heroMoodAndOrdinaryActivityDoNotReplaceLevel() {
        data.setComponents(creature, new Owner((short) 1, (short) 1),
                new CreatureExperience(1, 0, 100, 0, 0), new CreatureMood(100, 0, 0, 0, 0, 0),
                new CreatureAi(0, CreatureState.FIGHT, (short) 14));
        flower.update(0);
        for (int cycle = 0; cycle < 8; cycle++) {
            flower.onUpdate(.5f);
            assertEquals("Textures/GUI/moods/SL-01.png", flower.getCenterIcon());
        }
    }

    @Test
    void unconsciousZeroHealthFlowerAutoShowsAndHidesAfterRecovery() {
        Node model = new Node();
        model.addControl(flower);
        flower.update(3);
        assertFalse(flower.isEnabled());
        data.setComponents(creature, new Unconscious(0), new Health(0, 100));
        flower.update(.1f);
        flower.update(10);
        assertTrue(flower.isEnabled());
        data.removeComponent(creature, Unconscious.class);
        data.setComponent(creature, new Health(100, 100));
        flower.update(.1f);
        assertFalse(flower.isEnabled());
    }

    @Test
    void brightnessPulseUsesStrictHealthBoundaryAndStopsAfterRecovery() {
        Material material = new Material(assets, "MatDefs/UnitFlower.j3md");
        flower.onMaterialCreated(material);
        for (int health : new int[]{0, 1, 19, 20, 21, 120}) {
            data.setComponent(creature, new Health(health, 120));
            flower.update(0);
            flower.onMaterialGenerated(material);
            assertEquals(health < 20, material.getParam("PulseBrightness").getValue(), "Health " + health);
            assertEquals(false, material.getParam("FlashColors").getValue());
        }
        data.removeComponent(creature, Health.class);
        flower.update(0);
        flower.onMaterialGenerated(material);
        assertEquals(false, material.getParam("PulseBrightness").getValue());
    }

    @Test
    void ownershipChangesSwitchBetweenNeutralColorsAndHeroOrKeeperBrightness() {
        Material material = new Material(assets, "MatDefs/UnitFlower.j3md");
        flower.onMaterialCreated(material);
        data.setComponents(creature, new Unconscious(0), new Health(0, 120));
        for (short owner : new short[]{3, 2, 1, 4, 2}) {
            data.setComponent(creature, new Owner(owner, owner));
            flower.update(0);
            flower.onMaterialGenerated(material);
            assertEquals(owner != 2, material.getParam("PulseBrightness").getValue());
            assertEquals(owner == 2, material.getParam("FlashColors").getValue());
            for (int color = 1; color <= 7; color++) {
                assertNotNull(material.getParam("Color" + color));
            }
        }
    }
}
