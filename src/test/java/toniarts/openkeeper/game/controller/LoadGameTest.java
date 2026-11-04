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
package toniarts.openkeeper.game.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Storage;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.IPartyController;
import toniarts.openkeeper.game.controller.creature.PartyController;
import toniarts.openkeeper.game.controller.creature.PartyType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.state.session.SaveGame;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.GameMap;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.GameObjectFixtures;
import toniarts.openkeeper.tools.convert.map.HeroGateFixtures;
import toniarts.openkeeper.tools.convert.map.HeroPartyFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;

/**
 * Loading a saved game into a new game controller
 */
class LoadGameTest {

    private static final short KEEPER_ID = Player.KEEPER1_ID;
    private static final short ROCK_ID = 1;
    private static final short TREASURY_ID = 2;
    private static final short PARTY_ID = 5;
    private static final Map<MiscType, Variable.MiscVariable> SETTINGS = new EnumMap<>(MiscType.class);
    private static final Thing.HeroParty HERO_PARTY = HeroPartyFixtures.heroParty(PARTY_ID, HeroPartyFixtures.member(),
            HeroPartyFixtures.member(Thing.Creature.CreatureFlag.LEADER));

    static {
        for (MiscType type : MiscType.values()) {
            SETTINGS.put(type, CreatureSleepFixtures.variable(type, 1000));
        }
    }

    /**
     * A 5 x 5 level of rock with a 3 x 3 Treasury of the keeper in the middle
     */
    private static final IKwdFile LEVEL = level();
    private static final PlayerService PLAYER_SERVICE = (PlayerService) Proxy.newProxyInstance(PlayerService.class.getClassLoader(),
            new Class<?>[]{PlayerService.class}, (proxy, method, args) -> null);

    @TempDir
    Path folder;

    @Test
    void testLoadedGameIsTheSavedGame() throws Exception {
        EntityData entityData = new DefaultEntityData();
        GameController game = new GameController(LEVEL, List.of(new Keeper(false, KEEPER_ID)), entityData, SETTINGS, PLAYER_SERVICE);
        game.start();
        game.getGameWorldController().addGold(KEEPER_ID, 1500);
        EntityId inHand = game.getGameWorldController().getObjectsController().addLooseGold(1, 1, 100, 1000);
        game.getPlayerController(KEEPER_ID).getHandControl().push(inHand);
        for (int tick = 0; tick < 4; tick++) {
            game.processTick(0.25f);
        }

        // A hero party whose members have died and are gone
        PartyController party = new PartyController(HERO_PARTY);
        party.setType(PartyType.INVASION_PARTY);
        party.create();
        long memberId = 900;
        for (Thing.GoodCreature member : HERO_PARTY.getHeroPartyMembers()) {
            party.addMemberInstance(member, deadCreature(new EntityId(memberId++)));
        }

        Path first = folder.resolve("first.json.gz");
        save(game, entityData, List.of(party)).write(first);
        SaveGame saveGame = SaveGame.read(first);
        EntityData loadedEntityData = saveGame.createEntityData();
        GameController loaded = new GameController(LEVEL, saveGame, loadedEntityData, SETTINGS, PLAYER_SERVICE);

        // Loading makes nothing new: the rooms take over their furniture and storage, the game is as it was
        Path second = folder.resolve("second.json.gz");
        save(loaded, loadedEntityData, loaded.getGameWorldController().getCreaturesController().getCreatedParties()).write(second);
        JsonObject firstJson = readJson(first);
        JsonObject secondJson = readJson(second);
        firstJson.remove("nextEntityId");
        secondJson.remove("nextEntityId");
        assertEquals(firstJson, secondJson);

        assertEquals(1.0, loaded.getGameTimer().getGameTime());
        Keeper keeper = loaded.getLevelInfo().getPlayer(KEEPER_ID);
        assertEquals(1500, keeper.getGold());
        IPlayerController player = loaded.getPlayerController(KEEPER_ID);
        assertEquals(game.getPlayerController(KEEPER_ID).getGoldControl().getGoldMax(), player.getGoldControl().getGoldMax());
        assertEquals(1, player.getHandControl().getCount());
        assertEquals(inHand, player.getHandControl().peek());
        IRoomController treasury = loaded.getGameWorldController().getMapController().getRoomControllers().iterator().next();
        assertEquals(game.getGameWorldController().getMapController().getRoomControllers().iterator().next().getEntityId(), treasury.getEntityId());

        // The party members count as dead for the party triggers
        IPartyController loadedParty = loaded.getGameWorldController().getCreaturesController().getParty(PARTY_ID);
        assertTrue(loadedParty.isCreated());
        assertEquals(2, loadedParty.getActualMembers().size());
        for (ICreatureController member : loadedParty.getActualMembers()) {
            assertTrue(member.isDead());
        }

        // The Treasury knows its gold piles, more gold goes on them
        loaded.start();
        int goldPiles = loadedEntityData.findEntities(null, Gold.class).size();
        int storages = loadedEntityData.findEntities(null, Storage.class).size();
        loaded.getGameWorldController().addGold(KEEPER_ID, 100);
        assertEquals(goldPiles, loadedEntityData.findEntities(null, Gold.class).size());
        assertEquals(storages, loadedEntityData.findEntities(null, Storage.class).size());
        assertEquals(1600, keeper.getGold());

        // And the game carries on
        for (int tick = 0; tick < 4; tick++) {
            loaded.processTick(0.25f);
            ((IGameLogicUpdatable) loaded.getTaskManager()).processTick(0.25f);
        }
        assertEquals(2.0, loaded.getGameTimer().getGameTime());
    }

    private static SaveGame save(GameController game, EntityData entityData, Collection<IPartyController> parties) {
        return new SaveGame("Test", game.getGameTimer().getGameTime(), game.getLevelInfo(), game.getPlayerControllers().values(),
                game.getTriggerControls(), parties, game.getGameWorldController().getMapController().getRoomControllers(), entityData);
    }

    private static ICreatureController deadCreature(EntityId entityId) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class},
                (proxy, method, args) -> "getEntityId".equals(method.getName()) ? entityId : null);
    }

    private static IKwdFile level() {
        Terrain rock = TerrainFixtures.terrain(ROCK_ID, flags(Terrain.TerrainFlag.SOLID), 0, 100, 100, ROCK_ID);
        Terrain treasuryTerrain = TerrainFixtures.terrain(TREASURY_ID, flags(Terrain.TerrainFlag.ROOM, Terrain.TerrainFlag.OWNABLE), 0, 100, 100, TREASURY_ID);
        Room treasury = HeroGateFixtures.heroGateRoom((short) 1, "Treasury", Room.TileConstruction.NORMAL);
        GameObject pillar = GameObjectFixtures.gameObject("Treasury Pillar");
        GameObject gold = GameObjectFixtures.gameObject("Gold", GameObject.ObjectFlag.OBJECT_TYPE_GOLD);
        Player player = new Player();
        GameMap map = new GameMap(5, 5);
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                boolean room = x > 0 && x < 4 && y > 0 && y < 4;
                map.getTiles()[x][y] = new Tile() {

                    @Override
                    public short getTerrainId() {
                        return room ? TREASURY_ID : ROCK_ID;
                    }

                    @Override
                    public short getPlayerId() {
                        return room ? KEEPER_ID : 0;
                    }
                };
            }
        }

        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "getMap" ->
                    map;
                case "getTerrain" ->
                    (short) args[0] == TREASURY_ID ? treasuryTerrain : rock;
                case "getRoomByTerrain" ->
                    treasury;
                case "getObject" ->
                    ((Number) args[0]).intValue() == ObjectsController.OBJECT_GOLD_PILE_ID
                    || ((Number) args[0]).intValue() == ObjectsController.OBJECT_GOLD_ID ? gold : pillar;
                case "getPlayers" ->
                    Map.of(KEEPER_ID, player);
                case "getPlayer" ->
                    player;
                case "getGameLevel" ->
                    CreatureSleepFixtures.level(4);
                case "getVariables" ->
                    SETTINGS;
                case "getThings" ->
                    args[0] == Thing.HeroParty.class ? List.of(HERO_PARTY) : List.of();
                default ->
                    Set.class.isAssignableFrom(method.getReturnType()) ? new HashSet<>()
                    : Collection.class.isAssignableFrom(method.getReturnType()) ? new ArrayList<>()
                    : Map.class.isAssignableFrom(method.getReturnType()) ? new HashMap<>() : null;
            };
        });
    }

    private static JsonObject readJson(Path file) throws IOException {
        try (Reader reader = new InputStreamReader(new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

}
