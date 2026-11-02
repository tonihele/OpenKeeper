/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view;

import com.jme3.app.state.AppStateManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.VisualEffect;
import static org.junit.jupiter.api.Assertions.*;

class PortalEntranceClientTest {
    @Test
    void entranceEffectUsesSubscribedOwnerWithoutSynchronousServerLookup() throws Exception {
        var server = new DefaultEntityData();
        var client = new ClientEntityData(server); // Refuses direct component lookups, even in a local test.
        var id = server.createEntity();
        server.setComponents(id, new Position(0, new Vector3f(4, 1, 6)),
                new CreatureViewState((short) 7, 0, Creature.AnimationType.ENTRANCE),
                new Owner((short) 4, (short) 4));
        var creature = new Creature();
        set(creature, "name", "Synthetic arrival");
        set(creature, "flags", EnumSet.noneOf(Creature.CreatureFlag.class));
        set(creature, "entranceEffectId", 77);
        var effect = new Effect();
        set(effect, "flags", EnumSet.noneOf(Effect.EffectFlag.class));
        set(effect, "generateIds", List.of());
        IKwdFile level = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getCreature" -> creature;
                    case "getEffect" -> effect;
                    default -> throw new AssertionError(method.getName());
                });
        var assets = new DesktopAssetManager(true);
        var states = new AppStateManager(null);
        short[] owner = {-1};
        var effects = new EffectManagerState(level, assets) {
            @Override
            public VisualEffect load(Node node, Vector3f location, int effectId, boolean infinite, short ownerId) {
                assertEquals(77, effectId);
                owner[0] = ownerId;
                assertEquals(new Vector3f(4, WorldUtils.UNDERFLOOR_HEIGHT, 6), node.getLocalTranslation());
                return super.load(node, location, effectId, infinite, ownerId);
            }
        };
        states.attach(effects);
        var view = new PlayerEntityViewState(level, assets, client, (short) 3, null, new Node());
        try {
            view.initialize(states, null);
            assertEquals(4, owner[0]);
            view.cleanup();
        } finally {
            server.close();
        }
    }

    private static void set(Object object, String name, Object value) throws Exception {
        var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
}
