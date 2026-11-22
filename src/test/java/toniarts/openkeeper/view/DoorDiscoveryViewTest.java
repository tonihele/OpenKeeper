/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.map.FakeFogOfWarInformation;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.DoorFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.control.DoorViewControl;
import toniarts.openkeeper.view.control.IEntityViewControl;

import static org.junit.jupiter.api.Assertions.*;

class DoorDiscoveryViewTest {
    @Test
    void visibilityKeepsSecretKnowledgeSeparateFromFogAndRefreshesOnCapture() throws Exception {
        DefaultEntityData data = new DefaultEntityData();
        try {
            EntityId id = data.createEntity();
            short owner = 3, viewer = 4;
            DoorViewState hidden = new DoorViewState((short) 1, false, false, false, true);
            data.setComponents(id, hidden, new Position(0, new Vector3f(2, 0, 2)), new Owner(owner, owner), new Interaction(true, false, false, false, false));
            // A synthetic nonanimated model exercises the production view update without game assets or a GPU.
            Door definition = DoorFixtures.door((short) 1, 0, Door.DoorFlag.IS_BARRICADE);
            DoorViewControl control = new DoorViewControl(id, new ClientEntityData(data), definition, hidden, null, null, null);
            Node model = new Node();
            model.setLocalTranslation(2, 0, 2);
            model.addControl(control);
            FakeFogOfWarInformation fog = new FakeFogOfWarInformation();
            Point tile = new Point(2, 2);
            fog.explored.add(tile);
            IKwdFile level = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{IKwdFile.class}, (proxy, method, args) ->
                            method.getName().equals("getGameLevel") ? CreatureSleepFixtures.level(4) : null);
            PlayerEntityViewState state = new PlayerEntityViewState(level, null, new ClientEntityData(data), viewer, null, new Node(), null, fog);
            Method update = PlayerEntityViewState.class.getDeclaredMethod("updateDoorModelState", Spatial.class, Entity.class);
            update.setAccessible(true);
            update.invoke(state, model, data.getEntity(id, DoorViewState.class, Position.class, Owner.class));
            assertEquals(Spatial.CullHint.Always, model.getLocalCullHint());

            Field controlsField = PlayerEntityViewState.class.getDeclaredField("entityViewControls");
            controlsField.setAccessible(true);
            Map<EntityId, IEntityViewControl<?, ?>> controls = (Map<EntityId, IEntityViewControl<?, ?>>) controlsField.get(state);
            controls.put(id, control);
            state.onTilesDirty(new Point[]{tile});
            assertEquals(Spatial.CullHint.Always, model.getLocalCullHint()); // exploring cannot reveal it

            data.setComponent(id, new Owner(viewer, viewer));
            state.onTilesDirty(new Point[]{tile});
            assertEquals(Spatial.CullHint.Inherit, model.getLocalCullHint());
            update.invoke(state, model, data.getEntity(id, DoorViewState.class, Position.class, Owner.class));
            assertEquals(Spatial.CullHint.Inherit, model.getLocalCullHint()); // capture changes visibility immediately
            data.setComponents(id, new Owner(owner, owner), new DoorViewState((short) 1, false, false, false, false));
            update.invoke(state, model, data.getEntity(id, DoorViewState.class, Position.class, Owner.class));
            assertEquals(Spatial.CullHint.Inherit, model.getLocalCullHint());
            fog.explored.clear();
            state.onTilesDirty(new Point[]{tile});
            assertEquals(Spatial.CullHint.Always, model.getLocalCullHint()); // discovery does not bypass fog
            control.cleanup();
        } finally {
            data.close();
        }
    }
}
