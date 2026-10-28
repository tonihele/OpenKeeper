/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.controller.creature;

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.jme3.math.Vector3f;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.utils.WorldUtils;
import static org.junit.jupiter.api.Assertions.*;

class PortalEntrancePositionTest {
    @Test
    void goblinHandoffMatchesNativePositionDeltaAndFloorHeight() throws Exception {
        assertHandoff(43 / 4096f, 3139 / 4096f, 0);
    }

    @Test
    void usesEachCreaturesAuthoredOffsetIncludingSidewaysRogueEntrance() throws Exception {
        assertHandoff(0, 4395 / 4096f, 0); // Troll
        assertHandoff(-3999 / 4096f, 56 / 4096f, 0); // Rogue
        assertHandoff(0, 0, 0); // Entrance with no translation
    }

    private void assertHandoff(float x, float y, float z) throws Exception {
        var data = new DefaultEntityData();
        var id = data.createEntity();
        var original = new Position(1.2f, new Vector3f(13, 0, 39));
        data.setComponent(id, original);
        var creature = new Creature();
        var setter = Creature.class.getDeclaredMethod("setAnimationOffsets", Creature.OffsetType.class,
                float.class, float.class, float.class);
        setter.setAccessible(true);
        setter.invoke(creature, Creature.OffsetType.PORTAL_ENTRANCE, x, y, z);
        var controller = new CreatureController(id, data, creature, null, null, null,
                null, null, null, null, null, null, null, (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(), new Class<?>[]{GameEventService.class}, (proxy, method, args) -> null));
        controller.completeDungeonEntrance();
        var result = data.getComponent(id, Position.class);
        assertEquals(13 + x, result.position.x);
        assertEquals(39 + y, result.position.z);
        assertEquals(WorldUtils.FLOOR_HEIGHT, result.position.y);
        assertEquals(original.rotation, result.rotation);
        assertEquals(new Vector3f(13, 0, 39), original.position);
        data.close();
    }

    @Test
    void appliesHandoffOnlyOnCompletionAndBeforeChangingToIdle() {
        var events = new ArrayList<String>();
        boolean[] finished = {false};
        var machine = (StateMachine<?, ?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{StateMachine.class}, (proxy, method, args) -> {
                    if (method.getName().equals("changeState")) {
                        assertEquals(CreatureState.IDLE, args[0]);
                        events.add("idle");
                        return true;
                    }
                    return null;
                });
        var controller = (ICreatureController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isStateTimeExceeded" -> finished[0];
                    case "getStateMachine" -> machine;
                    case "completeDungeonEntrance" -> { events.add("position"); yield null; }
                    default -> null;
                });
        CreatureState.ENTERING_DUNGEON.update(controller);
        CreatureState.ENTERING_DUNGEON.exit(controller); // Interrupted: no completion offset.
        assertTrue(events.isEmpty());
        finished[0] = true;
        CreatureState.ENTERING_DUNGEON.update(controller);
        assertEquals(List.of("position", "idle"), events);
    }
}
