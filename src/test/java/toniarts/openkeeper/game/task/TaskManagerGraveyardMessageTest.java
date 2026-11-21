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
package toniarts.openkeeper.game.task;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.RoomAttackMessages;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.task.hero.RoomTileWorld;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.DestroyRoomsFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A body on the keeper's land that can't be cleared is told to the keeper
 * (DKII.EXE 0x46CF40 and 0x46D3C0): message 0x29 for room type 14, "You need
 * a Graveyard" (speech 1310), when the keeper has none but can build one, and
 * message 0x2A, the Graveyard is too small (speech 206), when none has room
 */
class TaskManagerGraveyardMessageTest {

    private static final short CREATURE_ID = 7;

    private final EntityData entityData = new DefaultEntityData();
    private final List<Integer> speeches = new ArrayList<>();
    private final List<IRoomController> graveyards = new ArrayList<>();
    private final Room graveyard = DestroyRoomsFixtures.destroyRoomsRoom("Graveyard", (short) 23, Room.RoomFlag.BUILDABLE);
    private final Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
    private final PlayerRoomControl roomControl;
    private short tileOwner = Player.KEEPER1_ID;

    TaskManagerGraveyardMessageTest() {
        DestroyRoomsFixtures.setRoomId(graveyard, (short) 14);
        roomControl = new PlayerRoomControl(keeper, List.of(graveyard));
        EntityId corpse = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = CREATURE_ID;
        entityData.setComponents(corpse, creatureComponent, new Death(0), new Position(0, WorldUtils.pointToVector3f(RoomTileWorld.TILE)));
    }

    @Test
    void aKeeperThatCanBuildAGraveyardIsToldItNeedsOne() {
        roomControl.setTypeAvailable(graveyard, true, true);

        createTaskManager();

        assertEquals(List.of(1310), speeches);
    }

    @Test
    void aKeeperThatCanNotBuildAGraveyardIsNotTold() {
        createTaskManager();

        assertEquals(List.of(), speeches);
    }

    @Test
    void aKeeperWhoseGraveyardsAreFullIsToldTheyAreTooSmall() {
        graveyards.add(graveyard(true));

        createTaskManager();

        assertEquals(List.of(206), speeches);
    }

    @Test
    void aGraveyardWithRoomIsNotTold() {
        graveyards.add(graveyard(true));
        graveyards.add(graveyard(false));

        createTaskManager();

        assertEquals(List.of(), speeches);
    }

    @Test
    void aBodyOnSomebodyElsesLandIsNotTold() {
        roomControl.setTypeAvailable(graveyard, true, true);
        tileOwner = Player.KEEPER2_ID;

        createTaskManager();

        assertEquals(List.of(), speeches);
    }

    private IRoomController graveyard(boolean full) {
        return RoomTileWorld.stub(IRoomController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRoom" ->
                graveyard;
            case "isFullCapacity" ->
                full;
            default ->
                RoomTileWorld.defaultValue(proxy, method);
        });
    }

    private TaskManager createTaskManager() {
        PlayerService playerService = RoomTileWorld.stub(PlayerService.class, (proxy, method, args) -> {
            if (method.getName().equals("showMentorMessage")) {
                speeches.add((Integer) args[0]);
            }
            return RoomTileWorld.defaultValue(proxy, method);
        });
        RoomAttackMessages messages = new RoomAttackMessages(entityData, playerService, () -> 0, 4);
        IGameWorldController gameWorldController = RoomTileWorld.stub(IGameWorldController.class, (proxy, method, args)
                -> method.getName().equals("getRoomAttackMessages") ? messages : RoomTileWorld.defaultValue(proxy, method));
        IMapTileController tile = RoomTileWorld.stub(IMapTileController.class, (proxy, method, args)
                -> method.getName().equals("getOwnerId") ? tileOwner : RoomTileWorld.defaultValue(proxy, method));
        IMapDataInformation<?> mapData = RoomTileWorld.stub(IMapDataInformation.class, (proxy, method, args)
                -> method.getName().equals("getTile") && RoomTileWorld.TILE.equals(args[0]) ? tile : RoomTileWorld.defaultValue(proxy, method));
        IMapController mapController = RoomTileWorld.stub(IMapController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" ->
                mapData;
            case "getRoomsByFunction" ->
                graveyards;
            default ->
                RoomTileWorld.defaultValue(proxy, method);
        });
        IPlayerController player = RoomTileWorld.stub(IPlayerController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getKeeper" ->
                keeper;
            case "getRoomControl" ->
                roomControl;
            default ->
                RoomTileWorld.defaultValue(proxy, method);
        });
        Creature creature = ShotFixtures.creature(CREATURE_ID, EnumSet.of(Creature.CreatureFlag.GENERATE_DEAD_BODY),
                EnumSet.noneOf(Creature.CreatureFlag3.class));
        IKwdFile kwdFile = RoomTileWorld.stub(IKwdFile.class, (proxy, method, args)
                -> method.getName().equals("getCreature") ? creature : RoomTileWorld.defaultValue(proxy, method));
        ILevelInfo levelInfo = RoomTileWorld.stub(ILevelInfo.class, (proxy, method, args)
                -> method.getName().equals("getLevelData") ? kwdFile : RoomTileWorld.defaultValue(proxy, method));
        return new TaskManager(entityData, gameWorldController, mapController, null, null, null, List.of(player), levelInfo, null, Map.of());
    }

}
