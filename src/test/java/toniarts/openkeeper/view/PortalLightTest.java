/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view;

import com.jme3.scene.Node;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.PortalViewState;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;
import static org.junit.jupiter.api.Assertions.*;

class PortalLightTest {
    private RoomComponent room(boolean destroyed) {
        return new RoomComponent((short) 9, destroyed, new Point(13, 39));
    }

    @Test
    void reproducesNativeFiftyUpdateCycleAndIntegerChannels() {
        var light = new PortalLight(room(false));
        for (int cycle = 0; cycle < 3; cycle++) {
            for (int step = 0; step < 50; step++) {
                int phase = step <= 25 ? 1 + step * 10 : 501 - step * 10;
                light.update(0.05f, true);
                var color = light.getLight().getColor();
                assertEquals((255 * phase >> 8) / 255f, color.r);
                assertEquals((26 * phase >> 8) / 255f, color.g);
                assertEquals((33 * phase >> 8) / 255f, color.b);
            }
        }
        assertEquals(10112 / 4096f, light.getLight().getRadius());
        assertEquals(2000 / 4096f, light.getLight().getPosition().y);
        assertEquals(13, light.getLight().getPosition().x);
        assertEquals(39, light.getLight().getPosition().z);
    }

    @Test
    void respectsSubstepsAndNativeSingleAdvanceAfterDelayedFrame() {
        var light = new PortalLight(room(false));
        light.update(0.025f, true);
        assertEquals(1, light.getLight().getColor().r);
        light.update(0.025f, true); // First pulse output, phase 1.
        assertEquals(0, light.getLight().getColor().r);
        light.update(0.5f, true); // Ten elapsed ticks, one native phase advance.
        assertEquals(10 / 255f, light.getLight().getColor().r);
    }

    @Test
    void followsFogDestructionRemovalAndViewCleanupWithoutAnArrival() {
        var data = new DefaultEntityData();
        var root = new Node();
        boolean[] visible = {false};
        var fog = (IFogOfWarInformation) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{IFogOfWarInformation.class},
                (proxy, method, args) -> visible[0]);
        var container = new PortalLightContainer(data, root, fog);
        var id = data.createEntity();
        data.setComponents(id, room(false), new PortalViewState(-1));
        container.start();
        container.update(0.05f);
        assertEquals(1, root.getLocalLightList().size());
        var light = root.getLocalLightList().get(0);
        assertFalse(light.isEnabled());
        visible[0] = true;
        container.update(0.05f);
        assertTrue(light.isEnabled());
        data.setComponent(id, room(true));
        container.update(0.05f);
        assertFalse(light.isEnabled());
        data.removeEntity(id);
        container.update(0.05f);
        assertEquals(0, root.getLocalLightList().size());
        var second = data.createEntity();
        data.setComponents(second, room(false), new PortalViewState(-1));
        container.update(0.05f);
        assertEquals(1, root.getLocalLightList().size());
        container.stop();
        assertEquals(0, root.getLocalLightList().size());
        data.close();
    }
}
