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

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.player.PlayerDoorControl;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.player.PlayerSpellControl;
import toniarts.openkeeper.game.controller.player.PlayerTrapControl;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.state.CheatState;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;

/**
 * Executes cheats for a single player game
 */
public final class CheatController implements ICheatController {

    private static final Logger logger = System.getLogger(CheatController.class.getName());

    private static final int CHEAT_MANA_AMOUNT = 100000;
    private static final int CHEAT_MONEY_AMOUNT = 100000;

    private final IKwdFile kwdFile;
    private final IGameController gameController;
    private final IGameWorldController gameWorldController;
    private final IMapController mapController;

    public CheatController(IKwdFile kwdFile, IGameController gameController) {
        this.kwdFile = kwdFile;
        this.gameController = gameController;
        this.gameWorldController = gameController.getGameWorldController();
        this.mapController = gameWorldController.getMapController();
    }

    @Override
    public void onCheat(CheatState.CheatType cheat, short playerId) {
        switch (cheat) {
            case GOD_MODE: {
                toggleGodMode(playerId);
                break;
            }
            case KILL_CREATURES:
            case RESURRECT_CREATURES:
            case DESTROY_DOORS:
            case INSTANT_DIG:
            case CLAIM_LAND:
            case TERRAIN_CHANGE:
            case SPAWN_CREATURES: {
                toggle(cheat, playerId);
                break;
            }
            case LEVEL_MAX: {
                gameWorldController.getCreaturesController().levelUpCreatures(playerId, Utils.MAX_CREATURE_LEVEL);
                break;
            }
            case MANA: {
                gameController.getPlayerController(playerId).getManaControl().addMana(CHEAT_MANA_AMOUNT);
                break;
            }
            case MONEY: {
                gameWorldController.addGold(playerId, CHEAT_MONEY_AMOUNT);
                break;
            }
            case REMOVE_FOW: {
                mapController.disableFogOfWar(playerId);
                break;
            }
            case RESET_FOW: {
                mapController.resetFogOfWar(playerId);
                break;
            }
            case UNLOCK_ROOMS: {
                PlayerRoomControl playerRoomControl = gameController.getPlayerController(playerId).getRoomControl();
                for (Room room : kwdFile.getRooms()) {
                    playerRoomControl.setTypeAvailable(room, true);
                }
                break;
            }
            case UNLOCK_DOORS_TRAPS: {
                PlayerDoorControl playerDoorControl = gameController.getPlayerController(playerId).getDoorControl();
                for (Door door : kwdFile.getDoors()) {
                    playerDoorControl.setTypeAvailable(door, true);
                }

                PlayerTrapControl playerTrapControl = gameController.getPlayerController(playerId).getTrapControl();
                for (Trap trap : kwdFile.getTraps()) {
                    playerTrapControl.setTypeAvailable(trap, true);
                }
                break;
            }
            case UNLOCK_SPELLS: {
                PlayerSpellControl playerSpellControl = gameController.getPlayerController(playerId).getSpellControl();
                for (KeeperSpell keeperSpell : kwdFile.getKeeperSpells()) {
                    playerSpellControl.setTypeAvailable(keeperSpell, true);
                    playerSpellControl.setSpellDiscovered(keeperSpell, true);
                }
                break;
            }
            case WIN_LEVEL: {
                gameController.endGame(playerId, true);
                break;
            }
            default:
                logger.log(Level.INFO, "Cheat {0} not implemented!", cheat);
        }
    }

    private boolean toggle(CheatState.CheatType cheat, short playerId) {
        boolean enabled = !gameWorldController.isCheat(playerId, cheat);
        gameWorldController.setCheat(playerId, cheat, enabled);
        logger.log(Level.INFO, "{0} {1}", cheat, enabled ? "on" : "off");
        return enabled;
    }

    private void toggleGodMode(short playerId) {
        if (!toggle(CheatState.CheatType.GOD_MODE, playerId)) {
            return;
        }

        // Everything is available
        onCheat(CheatState.CheatType.UNLOCK_ROOMS, playerId);
        onCheat(CheatState.CheatType.UNLOCK_SPELLS, playerId);
        onCheat(CheatState.CheatType.UNLOCK_DOORS_TRAPS, playerId);
    }

    @Override
    public boolean isCheat(CheatState.CheatType cheat, short playerId) {
        return gameWorldController.isCheat(playerId, cheat);
    }

    @Override
    public void dig(Vector2f start, Vector2f end, short playerId) {
        for (Point p : getArea(start, end)) {
            IMapTileController tile = mapController.getMapData().getTile(p);
            Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
            boolean god = gameWorldController.isCheat(playerId, CheatState.CheatType.GOD_MODE);
            if (!terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                    || !(god || terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE))) {
                continue;
            }

            if (terrain.getFlags().contains(Terrain.TerrainFlag.IMPENETRABLE) && tile.getGold() > 0) {
                // Gems never run out, they only give their yield, same as when mined
                dropGold(p, (int) gameController.getLevelVariable(Variable.MiscVariable.MiscType.GOLD_MINED_FROM_GEMS));
                continue;
            }

            // The gold in the seam comes out as loose gold
            int gold = tile.getGold();
            mapController.alterTerrain(p, terrain.getDestroyedTypeTerrainId(), Player.NEUTRAL_PLAYER_ID);
            dropGold(p, gold);
        }
    }

    private void dropGold(Point tile, int amount) {
        int maxPile = Math.max(1, (int) gameController.getLevelVariable(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY));
        for (int left = amount; left > 0; left -= maxPile) {
            gameWorldController.getObjectsController().addLooseGold(tile.x, tile.y, Math.min(left, maxPile), maxPile);
        }
    }

    @Override
    public void changeTerrain(Vector2f start, Vector2f end, short terrainId, short ownerId, short playerId) {
        if (!gameWorldController.isCheat(playerId, CheatState.CheatType.GOD_MODE)) {
            return;
        }
        Set<EntityId> claimedRooms = new HashSet<>();
        for (Point p : getArea(start, end)) {
            Terrain current = kwdFile.getTerrain(mapController.getMapData().getTile(p).getTerrainId());
            if (current.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                // Rooms can be claimed, a terrain change would leave them with stale bookkeeping, sell them first
                IRoomController room = mapController.getRoomControllerByCoordinates(p);
                if (terrainId == CLAIM_TERRAIN && room != null && claimedRooms.add(room.getEntityId())
                        && room.getRoomInstance().getOwnerId() != playerId) {
                    for (Point roomPoint : List.copyOf(room.getRoomInstance().getCoordinates())) {
                        mapController.alterTerrain(roomPoint, mapController.getMapData().getTile(roomPoint).getTerrainId(), playerId);
                    }
                    room.captured(playerId);
                    room.setHealth(room.getMaxHealth());
                }
                continue;
            }
            if (terrainId == CLAIM_TERRAIN) {
                // Claimable ground turns into its claimed form, owned ground changes hands
                if (current.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                    mapController.alterTerrain(p, current.getTerrainId(), playerId);
                } else if (TileWork.convertsAtFullHealth(current)) {
                    mapController.alterTerrain(p, current.getMaxHealthTypeTerrainId(), playerId);
                }
            } else {
                Terrain terrain = kwdFile.getTerrain(terrainId);
                mapController.alterTerrain(p, terrainId,
                        terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) ? ownerId : Player.NEUTRAL_PLAYER_ID);
            }
        }
    }

    private List<Point> getArea(Vector2f start, Vector2f end) {
        List<Point> tiles = new ArrayList<>();
        for (int x = Math.max(0, (int) start.x); x <= Math.min(kwdFile.getMap().getWidth() - 1, (int) end.x); x++) {
            for (int y = Math.max(0, (int) start.y); y <= Math.min(kwdFile.getMap().getHeight() - 1, (int) end.y); y++) {
                tiles.add(new Point(x, y));
            }
        }
        return tiles;
    }

    @Override
    public void spawnCreature(short creatureId, int level, int amount, short playerId) {
        gameWorldController.getCreaturesController().spawnCreatures(creatureId, playerId, level, amount);
    }

    @Override
    public void spawnCreature(short creatureId, int level, int amount, short ownerId, Vector2f position, short playerId) {
        if (!gameWorldController.isCheat(playerId, CheatState.CheatType.GOD_MODE)) {
            return;
        }
        Point tile = new Point((int) position.x, (int) position.y);
        if (tile.x < 0 || tile.y < 0 || tile.x >= kwdFile.getMap().getWidth() || tile.y >= kwdFile.getMap().getHeight()
                || kwdFile.getTerrain(mapController.getMapData().getTile(tile).getTerrainId()).getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            return; // Nothing can stand in the rock
        }
        for (int i = 0; i < amount; i++) {
            gameWorldController.getCreaturesController().spawnCreature(creatureId, ownerId, Math.clamp(level, 1, Utils.MAX_CREATURE_LEVEL), position, ICreaturesController.SpawnType.PLACE);
        }
    }

    @Override
    public void addGold(int amount, short playerId) {
        gameWorldController.addGold(playerId, amount);
    }

    @Override
    public void addMana(int amount, short playerId) {
        gameController.getPlayerController(playerId).getManaControl().addMana(amount);
    }

}
