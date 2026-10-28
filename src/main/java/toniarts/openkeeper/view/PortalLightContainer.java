/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view;

import com.jme3.scene.Node;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityContainer;
import com.simsilica.es.EntityData;
import toniarts.openkeeper.game.component.PortalViewState;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/** Owns idle lights independently of the Portal's preparation/arrival effects. */
final class PortalLightContainer extends EntityContainer<PortalLight> {
    private final Node root;
    private final IFogOfWarInformation fog;

    PortalLightContainer(EntityData entityData, Node root, IFogOfWarInformation fog) {
        super(entityData, PortalViewState.class, RoomComponent.class);
        this.root = root;
        this.fog = fog;
    }

    void update(float tpf) {
        super.update();
        for (PortalLight light : getArray()) {
            light.update(tpf, fog.isVisible(light.getTile()));
        }
    }

    @Override
    protected PortalLight addObject(Entity entity) {
        PortalLight light = new PortalLight(entity.get(RoomComponent.class));
        light.update(0, fog.isVisible(light.getTile()));
        root.addLight(light.getLight());
        return light;
    }

    @Override
    protected void updateObject(PortalLight light, Entity entity) {
        light.setRoom(entity.get(RoomComponent.class));
    }

    @Override
    protected void removeObject(PortalLight light, Entity entity) {
        root.removeLight(light.getLight());
    }
}
