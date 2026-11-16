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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A prisoner that dies in the Prison rises as a Skeleton, and the mentor tells
 * the keeper of the Prison (DKII.EXE CRoom_sub_4E30F0, message 0x41)
 */
class HealthSystemPrisonTest {

    private static final short KEEPER = 3;
    private static final short HERO = 1;
    private static final short PRISONER_ID = 13;
    private static final short SKELETON_ID = 9;

    private final EntityData entityData = new DefaultEntityData();
    private final List<Short> skeletonsFor = new ArrayList<>();
    private final List<List<Object>> playerEvents = new ArrayList<>();

    @Test
    void prisonerThatRisesAsASkeletonIsToldToTheKeeperOfThePrison() {
        EntityId prisoner = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = PRISONER_ID;
        entityData.setComponents(prisoner, creatureComponent, new Owner(HERO, KEEPER), new Health(1, 100),
                new CreatureImprisoned(0, 0), new Position(0, new Vector3f()));
        HealthSystem healthSystem = createHealthSystem();

        // The Prison takes its health once a second
        for (int tick = 0; tick < 4; tick++) {
            healthSystem.processTick(0.25f);
        }

        assertEquals(List.of(KEEPER), skeletonsFor);
        assertEquals(List.of(List.of(GameEventType.PRISON_SKELETON_RISEN, KEEPER)), playerEvents);
    }

    private HealthSystem createHealthSystem() {
        Creature prisoner = ShotFixtures.creature(PRISONER_ID, EnumSet.of(Creature.CreatureFlag.GENERATE_DEAD_BODY),
                EnumSet.noneOf(Creature.CreatureFlag3.class));
        IKwdFile kwdFile = proxy(IKwdFile.class, (method, args) -> method.equals("getCreature") ? prisoner : null);
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> method.equals("getLevelData") ? kwdFile : null);
        IMapTileInformation tile = proxy(IMapTileInformation.class,
                (method, args) -> method.equals("getLocation") ? new Point(0, 0) : null);
        IEntityPositionLookup positionLookup = proxy(IEntityPositionLookup.class,
                (method, args) -> method.equals("getEntityLocation") ? tile : null);
        IRoomController prison = proxy(IRoomController.class,
                (method, args) -> method.equals("getRoom") ? RoomFixtures.roomCreating(SKELETON_ID) : null);
        IMapController mapController = proxy(IMapController.class, (method, args) -> switch (method) {
            case "getRoomControllerByCoordinates" -> prison;
            case "getPlayerSkeletonCapacity" -> 1;
            default -> null;
        });
        ICreaturesController creaturesController = proxy(ICreaturesController.class, (method, args) -> {
            if (method.equals("turnCreatureIntoAnother")) {
                skeletonsFor.add((Short) args[1]);
            }
            return null;
        });
        Keeper keeper = new Keeper(false, KEEPER);
        PlayerCreatureControl creatureControl = new PlayerCreatureControl(keeper, null, List.of());
        IPlayerController player = proxy(IPlayerController.class, (method, args) -> switch (method) {
            case "getKeeper" -> keeper;
            case "getCreatureControl" -> creatureControl;
            default -> null;
        });
        GameEventService gameEventService = proxy(GameEventService.class, (method, args) -> {
            if (method.equals("emitToPlayer")) {
                playerEvents.add(List.of(args));
            }
            return null;
        });

        return new HealthSystem(entityData, positionLookup,
                Map.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, CreatureSleepFixtures.variable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, 10),
                        MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, CreatureSleepFixtures.variable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, -5),
                        MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, CreatureSleepFixtures.variable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, 10)),
                creaturesController, levelInfo, List.of(player), mapController, () -> 0.0, gameEventService);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

    private interface Answer {

        Object answer(String method, Object[] args);
    }
}
