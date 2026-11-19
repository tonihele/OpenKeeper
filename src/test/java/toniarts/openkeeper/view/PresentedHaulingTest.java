/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresentedHaulingTest {
    @Test void followsPresentedCarrierAcrossDifferentParentsAndRetainsWorldHeight() {
        Node carrierParent = new Node(), victimParent = new Node(), carrier = new Node(), victim = new Node();
        carrierParent.setLocalTranslation(10,2,3); victimParent.setLocalTranslation(-4,1,-5);
        carrierParent.attachChild(carrier); victimParent.attachChild(victim);
        carrier.setLocalTranslation(1,5,2); victim.setLocalTranslation(4,.5f,5);
        carrierParent.updateGeometricState(); victimParent.updateGeometricState();
        Vector3f scratch = new Vector3f();
        PlayerEntityViewState.followPresentedHauler(victim,carrier,scratch);
        assertEquals(11,victim.getWorldTranslation().x); assertEquals(5,victim.getWorldTranslation().z);
        assertEquals(1.5f,victim.getWorldTranslation().y);
        carrier.move(2,0,3);
        PlayerEntityViewState.followPresentedHauler(victim,carrier,scratch);
        assertEquals(13,victim.getWorldTranslation().x); assertEquals(8,victim.getWorldTranslation().z);
        assertEquals(1.5f,victim.getWorldTranslation().y);
    }
}
