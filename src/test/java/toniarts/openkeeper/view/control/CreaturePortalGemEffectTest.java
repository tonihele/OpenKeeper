package toniarts.openkeeper.view.control;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;
import toniarts.openkeeper.tools.convert.map.PortalGemEffectFixtures;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.PortalGemEffect;
import toniarts.openkeeper.tools.convert.KmfModelMarkers;

import static org.junit.jupiter.api.Assertions.*;

class CreaturePortalGemEffectTest {
    private final DefaultEntityData data = new DefaultEntityData();
    private final com.simsilica.es.EntityId id = data.createEntity();
    private final Node model = new Node("Creature");
    private final CreatureViewControl control = new CreatureViewControl(id, data,
            PortalGemEffectFixtures.creature(), null, null, null);

    CreaturePortalGemEffectTest() {
        data.setComponent(id, new Owner((short) 1, (short) 1));
        control.setEffectManager(new EffectManagerState(PortalGemEffectFixtures.level(), PortalGemEffectFixtures.assets()));
        model.addControl(control);
    }

    @AfterEach
    void close() {
        control.cleanup();
        data.close();
    }

    @Test
    void attachesOnlyWhileCarryingAndDoesNotDuplicate() {
        control.update(0.05f);
        assertEquals(0, model.getQuantity());
        data.setComponent(id, new PortalGem());
        control.update(0.05f);
        Spatial attachment = model.getChild("Carried portal gem");
        assertNotNull(attachment);
        assertInstanceOf(PortalGemEffect.class, ((Node) attachment).getChild(0));
        control.update(0.05f);
        assertSame(attachment, model.getChild("Carried portal gem"));
        assertEquals(1, model.getQuantity());
        data.removeComponent(id, PortalGem.class);
        control.update(0.05f);
        assertNull(attachment.getParent());
        assertEquals(0, model.getQuantity());
    }

    @Test
    void followsMovementSurvivesAnimationChangesAndInheritsFog() {
        data.setComponent(id, new PortalGem());
        control.update(0.05f);
        Spatial attachment = model.getChild("Carried portal gem");
        for (AnimationType state : new AnimationType[]{AnimationType.WALK, AnimationType.MELEE_ATTACK}) {
            control.setTargetState(state);
            control.update(0.05f);
            assertSame(attachment, model.getChild("Carried portal gem"));
        }
        model.setLocalTranslation(4, 0, 7);
        model.updateGeometricState();
        assertEquals(new Vector3f(4, 0, 7), attachment.getWorldTranslation());
        model.setCullHint(Spatial.CullHint.Always);
        assertEquals(Spatial.CullHint.Always, attachment.getCullHint());
        model.setCullHint(Spatial.CullHint.Inherit);
        assertNotEquals(Spatial.CullHint.Always, attachment.getCullHint());
    }

    @Test
    void onlyVisibleNativeMarkersEmitAndMultipleViewportsDoNotDuplicateRequests() {
        var marker = new Node("gem marker");
        marker.setUserData(KmfModelMarkers.MARKER_ID, KmfModelMarkers.PORTAL_GEM);
        marker.setUserData(toniarts.openkeeper.utils.AssetUtils.USER_DATA_KEY_REMOVABLE, false);
        marker.setLocalTranslation(0, 0.4f, 0);
        model.attachChild(marker);
        data.setComponent(id, new PortalGem());
        control.update(0.05f);
        model.updateGeometricState();
        var emitter = (PortalGemEffect) ((Node) model.getChild("Carried portal gem")).getChild(0);
        control.controlRender(null, null);
        control.controlRender(null, null);
        control.update(0.05f);
        assertEquals(2, emitter.getNumVisibleParticles());
        marker.setCullHint(Spatial.CullHint.Always);
        control.controlRender(null, null);
        control.update(0.05f);
        assertEquals(4, emitter.getNumVisibleParticles());
        control.update(0.05f);
        assertEquals(4, emitter.getNumVisibleParticles());
    }

    @Test
    void deathAndModelRemovalCleanUpAttachment() {
        data.setComponent(id, new PortalGem());
        control.update(0.05f);
        Spatial attachment = model.getChild("Carried portal gem");
        data.setComponent(id, new Death(0));
        control.update(0.05f);
        assertNull(attachment.getParent());
        data.removeComponent(id, Death.class);
        control.update(0.05f);
        attachment = model.getChild("Carried portal gem");
        assertNotNull(attachment);
        model.removeControl(control);
        assertNull(attachment.getParent());
    }
}
