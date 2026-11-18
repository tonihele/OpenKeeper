/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.control;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.ObjectViewState;
import static org.junit.jupiter.api.Assertions.*;
class ObjectViewAnimationIdentityTest {
    @Test void visibilityAndRepublishedComponentDoNotResetTheMesh() {
        var a = new ObjectViewState((short) 13, null, ObjectViewState.GameObjectAnimState.MESH_RESOURCE, true);
        var b = new ObjectViewState((short) 13, null, ObjectViewState.GameObjectAnimState.MESH_RESOURCE, false);
        assertTrue(ObjectViewControl.sameAnimation(a, b));
        b.animState = ObjectViewState.GameObjectAnimState.ADDITIONAL_RESOURCE_1;
        assertFalse(ObjectViewControl.sameAnimation(a, b));
        assertFalse(ObjectViewControl.sameAnimation(a, null));
    }
}
