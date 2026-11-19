package toniarts.openkeeper.view.control;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.tools.convert.map.GameObject;

import static org.junit.jupiter.api.Assertions.*;
import static toniarts.openkeeper.tools.convert.map.GameObjectFixtures.gameObject;

class SpecialHoverTest {

    private final EntityData data = new DefaultEntityData();
    private final EntityId id = data.createEntity();
    private final Position position = new Position(0.7f, new Vector3f(2, 1, 3));
    private final Node model = new Node();
    private ObjectViewControl control;

    @AfterEach
    void close() {
        if (control != null) {
            control.cleanup();
        }
        data.close();
    }

    @Test
    void hoverContinuesWithinNativeBoundsAndRestoresYawOnExitAndReentry() {
        create(true);
        Quaternion idle = model.getLocalRotation().clone();
        control.onHoverStart((short) 3);
        model.updateLogicalState(0.05f);
        assertEquals(idle, model.getLocalRotation()); // First native call only captures the base angles.
        float movement = 0;
        for (int i = 0; i < 100; i++) {
            model.updateLogicalState(0.05f);
            float[] angles = model.getLocalRotation().toAngles(null);
            float limit = 25 * FastMath.TWO_PI / 2048 + 0.00001f;
            assertTrue(Math.abs(angles[0]) <= limit);
            assertTrue(Math.abs(angles[1] - position.rotation) <= limit);
            assertTrue(Math.abs(angles[2]) <= limit);
            movement += Math.abs(angles[0]) + Math.abs(angles[2]);
        }
        assertTrue(movement > 0);
        assertEquals(position.position, model.getLocalTranslation());
        assertEquals(position.rotation, data.getComponent(id, Position.class).rotation);
        assertNull(data.getComponent(id, InHand.class));
        control.onHoverEnd((short) 3);
        assertEquals(idle, model.getLocalRotation());
        model.updateLogicalState(1);
        assertEquals(idle, model.getLocalRotation());
        control.onHoverStart((short) 3);
        model.updateLogicalState(0.05f);
        assertEquals(idle, model.getLocalRotation());
    }

    @Test
    void hoverUpdatesBetweenSimulationTicks() {
        create(true);
        control.onHoverStart((short) 3);
        model.updateLogicalState(0.001f);
        Quaternion previous = model.getLocalRotation().clone();
        int changedFrames = 0;
        for (int i = 0; i < 10; i++) {
            // All these view updates together are shorter than one game tick.
            model.updateLogicalState(0.001f);
            if (!previous.equals(model.getLocalRotation())) {
                changedFrames++;
            }
            previous.set(model.getLocalRotation());
        }
        assertTrue(changedFrames >= 9, "Hover poses must not wait for simulation ticks");
    }

    @Test
    void storedSpecialsWobbleWithoutPickupPermission() {
        data.setComponent(id, new Stored(data.createEntity(), ObjectType.SPECIAL));
        create(true);
        assertFalse(control.isPickable((short) 3));
        control.onHoverStart((short) 3);
        model.updateLogicalState(0.05f);
        assertWobbling();
    }

    @Test
    void hiddenOrInHandSpecialsStopWobbling() {
        create(true);
        control.onHoverStart((short) 3);
        model.updateLogicalState(0.05f);
        assertWobbling();
        model.setCullHint(Node.CullHint.Always);
        model.updateLogicalState(0.05f);
        assertIdle();
        model.setCullHint(Node.CullHint.Inherit);
        data.setComponent(id, new InHand());
        model.updateLogicalState(0.05f);
        assertIdle();
        data.removeComponent(id, InHand.class);
        model.updateLogicalState(0.05f);
        assertWobbling();
        data.removeComponent(id, Position.class);
        model.updateLogicalState(0.05f);
        assertIdle();
    }

    @Test
    void ordinaryObjectsDoNotWobble() {
        create(false);
        control.onHoverStart((short) 3);
        model.updateLogicalState(1);
        assertIdle();
    }

    private void create(boolean special) {
        data.setComponent(id, position);
        GameObject object = special ? gameObject("Special", GameObject.ObjectFlag.OBJECT_TYPE_SPECIAL)
                : gameObject("Crate", GameObject.ObjectFlag.OBJECT_TYPE_CRATE);
        var state = new ObjectViewState((short) 117, GameObject.State.SPECIAL,
                ObjectViewState.GameObjectAnimState.MESH_RESOURCE, true);
        // An empty synthetic model keeps this check independent of converted DKII assets and a graphics context.
        model.setLocalTranslation(position.position);
        model.setLocalRotation(new Quaternion().fromAngles(0, position.rotation, 0));
        control = new ObjectViewControl(id, data, object, state, null, null, null, false, 20);
        model.addControl(control);
    }

    private void assertWobbling() {
        Quaternion idle = new Quaternion().fromAngles(0, position.rotation, 0);
        boolean moved = false;
        for (int i = 0; i < 20; i++) {
            model.updateLogicalState(0.05f);
            moved |= !idle.equals(model.getLocalRotation());
        }
        assertTrue(moved);
    }

    private void assertIdle() {
        assertEquals(new Quaternion().fromAngles(0, position.rotation, 0), model.getLocalRotation());
    }
}
