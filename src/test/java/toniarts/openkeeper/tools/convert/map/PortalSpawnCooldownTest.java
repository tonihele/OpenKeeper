/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.tools.convert.map;

import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.GameTimeController;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.room.LairController;
import toniarts.openkeeper.game.controller.room.ThreeByThreeController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.game.logic.CreatureSpawnSystem;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests live spawn decisions; this package also permits parser-only fixture setters. */
class PortalSpawnCooldownTest {

    @Test
    void latePortalWaitsFullCooldownBeforeAttractingCreature() {
        DefaultEntityData entities = new DefaultEntityData();
        GameTimeController timer = new GameTimeController();
        Keeper keeper = new Keeper(false, (short) 3);
        Creature goblin = new Creature();
        goblin.setCreatureId((short) 7);
        Creature.Attraction lairRequirement = new Creature.Attraction();
        lairRequirement.setRoomId(2);
        lairRequirement.setPresent(1);
        goblin.setAttractions(new Creature.Attraction[]{lairRequirement});
        Creature imp = new Creature();
        imp.setCreatureId((short) 1);
        Room portalDefinition = new Room();
        portalDefinition.setRoomId((short) 3);
        Room lairDefinition = new Room();
        lairDefinition.setRoomId((short) 2);
        PlayerRoomControl rooms = new PlayerRoomControl(keeper, List.of(portalDefinition, lairDefinition));
        PlayerCreatureControl creatures = new PlayerCreatureControl(keeper, imp, List.of(goblin, imp));
        creatures.setTypeAvailable(goblin, true);
        IPlayerController player = stub(IPlayerController.class, (method, args) -> switch (method) {
            case "getKeeper" -> keeper;
            case "getRoomControl" -> rooms;
            case "getCreatureControl" -> creatures;
            default -> throw new AssertionError(method);
        });
        Variable.CreaturePool pool = new Variable.CreaturePool();
        pool.setValue(10);
        IKwdFile level = stub(IKwdFile.class, (method, args) -> switch (method) {
            case "getCreaturePool" -> Map.of(7, pool);
            case "getPortal" -> portalDefinition;
            case "getRoomById" -> lairDefinition;
            default -> throw new AssertionError(method);
        });
        RoomInstance lair = new RoomInstance(lairDefinition);
        lair.setOwnerId((short) 3);
        lair.addCoordinate(new Point(1, 1));
        rooms.onBuild(new LairController(entities.createEntity(), entities, level, lair, null, timer));
        List<RoomListener> listeners = new ArrayList<>();
        IMapController map = stub(IMapController.class, (method, args) -> {
            if (!method.equals("addListener")) {
                throw new AssertionError(method);
            }
            listeners.add((RoomListener) args[1]);
            return null;
        });
        List<Short> spawned = new ArrayList<>();
        ICreaturesController spawner = stub(ICreaturesController.class, (method, args) -> {
            if (!method.equals("spawnCreature")) {
                throw new AssertionError(method);
            }
            spawned.add((Short) args[0]);
            return entities.createEntity();
        });
        Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> settings =
                new EnumMap<>(Variable.MiscVariable.MiscType.class);
        setting(settings, Variable.MiscVariable.MiscType.ENTRANCE_GENERATION_SPEED_SECONDS, 18);
        setting(settings, Variable.MiscVariable.MiscType.MINIMUM_IMP_THRESHOLD, 0);
        setting(settings, Variable.MiscVariable.MiscType.CREATURES_SUPPORTED_BY_FIRST_PORTAL, 10);
        setting(settings, Variable.MiscVariable.MiscType.CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL, 5);
        setting(settings, Variable.MiscVariable.MiscType.TIME_BEFORE_FREE_IMP_GENERATED_SECONDS, 30);
        CreatureSpawnSystem system = new CreatureSpawnSystem(entities, spawner, List.of(player), settings,
                stub(ILevelInfo.class, (method, args) -> level), map,
                stub(GameEventService.class, (method, args) -> null));
        timer.processTick(109.5f);
        system.processTick(109.5f);
        RoomInstance room = new RoomInstance(portalDefinition);
        room.setOwnerId((short) 3);
        room.addCoordinate(new Point(5, 5));
        ThreeByThreeController portal = new ThreeByThreeController(
                entities.createEntity(), entities, level, room, null, timer);
        rooms.onBuild(portal);
        listeners.forEach(listener -> listener.onBuild(portal));

        // The tutorial's scripted Goblin is generated at 12 seconds after capture.
        timer.processTick(12);
        system.processTick(12);
        assertEquals(List.of(), spawned);
        timer.processTick(5.75f);
        system.processTick(5.75f);
        assertEquals(List.of(), spawned);
        timer.processTick(0.25f);
        system.processTick(0.25f);
        assertEquals(List.of((short) 7), spawned);
    }

    private static void setting(Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> settings,
            Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable setting = new Variable.MiscVariable();
        setting.setVariableId(type);
        setting.setValue(value);
        settings.put(type, setting);
    }

    private interface Calls {
        Object call(String method, Object[] args);
    }

    private static <T> T stub(Class<T> type, Calls calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> calls.call(method.getName(), args)));
    }
}
