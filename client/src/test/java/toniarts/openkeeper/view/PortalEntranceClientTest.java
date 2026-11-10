/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
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
import com.simsilica.event.EventBus;
import toniarts.openkeeper.game.event.ClientEvents;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEventType;
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
        set(effect, "flags", EnumSet.of(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS));
        set(effect, "deathEffectId", 78);
        var tail = new Effect();
        set(tail, "flags", EnumSet.noneOf(Effect.EffectFlag.class));
        set(tail, "generateIds", List.of());
        set(effect, "generateIds", List.of());
        IKwdFile level = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getCreature" -> creature;
                    case "getEffect" -> (int) args[0] == 77 ? effect : tail;
                    default -> throw new AssertionError(method.getName());
                });
        var assets = new DesktopAssetManager(true);
        var states = new AppStateManager(null);
        short[] owner = {-1};
        int[] plays = {0};
        var locations = new java.util.ArrayList<Vector3f>();
        var effects = new EffectManagerState(level, assets) {
            @Override
            public VisualEffect load(Node node, Vector3f location, int effectId, boolean infinite, short ownerId) {
                assertEquals(77, effectId);
                owner[0] = ownerId;
                plays[0]++;
                locations.add(node.getLocalTranslation().clone());
                return super.load(node, location, effectId, infinite, ownerId);
            }
        };
        states.attach(effects);
        var root = new Node();
        boolean[] visible = {true};
        var fog = (toniarts.openkeeper.view.fogofwar.IFogOfWarInformation) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{toniarts.openkeeper.view.fogofwar.IFogOfWarInformation.class},
                (proxy, method, args) -> visible[0]);
        var view = new PortalEffectState(level, client, root, fog);
        try {
            view.initialize(states, null);
            assertEquals(4, owner[0]);
            assertEquals(new Vector3f(4, WorldUtils.UNDERFLOOR_HEIGHT, 6), locations.getFirst());
            server.setComponents(id, new Position(0, new Vector3f(5, 1, 7)),
                    new Owner((short) 5, (short) 5));
            view.update(.05f);
            assertEquals(1, plays[0]); // Position/owner updates do not replay the same arrival.
            visible[0] = false;
            view.update(.05f);
            var effectsRoot = (Node) root.getChild("Portal effects");
            assertEquals(com.jme3.scene.Spatial.CullHint.Always, effectsRoot.getChild(0).getLocalCullHint());
            server.setComponent(id, new CreatureViewState((short) 7, 1, Creature.AnimationType.IDLE_1));
            view.update(.05f);
            effects.update(.05f); // The arrival now creates its chained death effect.
            view.update(.05f);
            assertEquals(1, effectsRoot.getQuantity(), "Chained effects survive the end of ENTRANCE");
            effects.update(.05f); // Its synthetic child completes naturally on the next frame.
            view.update(.05f);
            assertEquals(0, effectsRoot.getQuantity(), "Completed chains release their anchors");
            server.setComponent(id, new CreatureViewState((short) 7, 2, Creature.AnimationType.ENTRANCE));
            view.update(.05f);
            assertEquals(2, plays[0]);
            assertEquals(5, owner[0]);
            assertEquals(new Vector3f(5, WorldUtils.UNDERFLOOR_HEIGHT, 7), locations.getLast());
            server.setComponent(id, new CreatureViewState((short) 7, 3, Creature.AnimationType.IDLE_1));
            view.update(.05f);
            assertEquals(1, effectsRoot.getQuantity());
            server.removeEntity(id);
            view.update(.05f);
            assertEquals(0, effectsRoot.getQuantity());
        } finally {
            view.cleanup();
            assertEquals(0, root.getQuantity());
            server.close();
        }
    }

    @Test
    void preparationEventsStartBuildUpAndClearOnCapture() throws Exception {
        var server = new DefaultEntityData();
        var id = server.createEntity();
        server.setComponents(id, new toniarts.openkeeper.game.component.RoomComponent((short) 3, false,
                new toniarts.openkeeper.utils.Point(2, 3)), new Owner((short) 4, (short) 4));
        var room = new toniarts.openkeeper.tools.convert.map.Room();
        set(room, "effects", List.of(0, 0, 77));
        var effect = new Effect();
        set(effect, "flags", EnumSet.of(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS));
        set(effect, "generateIds", List.of());
        set(effect, "minHp", 2);
        set(effect, "maxHp", 2);
        IKwdFile level = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getRoomById" -> room;
                    case "getEffect" -> effect;
                    default -> throw new AssertionError(method.getName());
                });
        var states = new AppStateManager(null);
        int[] plays = {0};
        states.attach(new EffectManagerState(level, null) {
            @Override
            public VisualEffect load(Node node, Vector3f location, int effectId, boolean infinite, short ownerId) {
                assertEquals(77, effectId);
                assertEquals(4, ownerId);
                plays[0]++;
                return super.load(node, location, effectId, infinite, ownerId);
            }
        });
        var root = new Node();
        var view = new PortalEffectState(level, new ClientEntityData(server), root,
                toniarts.openkeeper.view.fogofwar.IFogOfWarInformation.ALWAYS_VISIBLE);
        try {
            view.initialize(states, null);
            assertEquals(0, plays[0]);
            publishPreparation(id);
            view.update(.05f);
            view.update(.05f);
            assertEquals(1, plays[0]);
            server.setComponent(id, new Owner((short) 5, (short) 5));
            view.update(.05f);
            assertEquals(0, ((Node) root.getChild("Portal effects")).getQuantity());
            server.setComponent(id, new Owner((short) 4, (short) 4));
            publishPreparation(id);
            view.update(.05f);
            assertEquals(2, plays[0]);
            var effectsRoot = (Node) root.getChild("Portal effects");
            server.removeEntity(id);
            view.update(.05f);
            assertEquals(0, effectsRoot.getQuantity());

            // The event may precede room replication; render once the subscribed data arrives.
            var delayed = server.createEntity();
            publishPreparation(delayed);
            view.update(.05f);
            assertEquals(2, plays[0]);
            server.setComponents(delayed, new toniarts.openkeeper.game.component.RoomComponent((short) 3, false,
                    new toniarts.openkeeper.utils.Point(2, 3)), new Owner((short) 4, (short) 4));
            view.update(.05f);
            assertEquals(3, plays[0]);
            server.setComponent(delayed, new toniarts.openkeeper.game.component.RoomComponent((short) 3, true,
                    new toniarts.openkeeper.utils.Point(2, 3)));
            view.update(.05f);
            assertEquals(0, effectsRoot.getQuantity());

            var stale = server.createEntity();
            publishPreparation(stale);
            view.update(3.1f);
            server.setComponents(stale, new toniarts.openkeeper.game.component.RoomComponent((short) 3, false,
                    new toniarts.openkeeper.utils.Point(2, 3)), new Owner((short) 4, (short) 4));
            view.update(.05f);
            assertEquals(3, plays[0], "Do not replay an expired preparation after delayed replication");
        } finally {
            view.cleanup();
            assertEquals(0, root.getQuantity());
            server.close();
        }
    }

    private static void publishPreparation(com.simsilica.es.EntityId id) {
        EventBus.publish(ClientEvents.GAME_EVENT, new GameEvent(GameEventType.PORTAL_PREPARING,
                GameEvent.ThingFlag.ROOM, 3, null, 0, id, new toniarts.openkeeper.utils.Point(2, 3)));
    }

    private static void set(Object object, String name, Object value) throws Exception {
        var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
}
