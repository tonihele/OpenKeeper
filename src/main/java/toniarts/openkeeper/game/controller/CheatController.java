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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.controller.player.PlayerDoorControl;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.player.PlayerSpellControl;
import toniarts.openkeeper.game.controller.player.PlayerTrapControl;
import toniarts.openkeeper.game.state.CheatState;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Trap;
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

    private void toggleGodMode(short playerId) {
        boolean godMode = !gameWorldController.isGodMode(playerId);
        gameWorldController.setGodMode(playerId, godMode);
        logger.log(Level.INFO, "God mode {0}", godMode ? "on" : "off");
        if (!godMode) {
            return;
        }

        // Everything is available
        onCheat(CheatState.CheatType.UNLOCK_ROOMS, playerId);
        onCheat(CheatState.CheatType.UNLOCK_SPELLS, playerId);
        onCheat(CheatState.CheatType.UNLOCK_DOORS_TRAPS, playerId);
    }

    @Override
    public boolean isGodMode(short playerId) {
        return gameWorldController.isGodMode(playerId);
    }

    @Override
    public void dig(Vector2f start, Vector2f end, short playerId) {
        for (Point p : getArea(start, end)) {
            Terrain terrain = kwdFile.getTerrain(mapController.getMapData().getTile(p).getTerrainId());
            if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE)) {
                mapController.alterTerrain(p, terrain.getDestroyedTypeTerrainId(), Player.NEUTRAL_PLAYER_ID);
            }
        }
    }

    @Override
    public void changeTerrain(Vector2f start, Vector2f end, short terrainId, short playerId) {
        for (Point p : getArea(start, end)) {
            Terrain current = kwdFile.getTerrain(mapController.getMapData().getTile(p).getTerrainId());
            if (current.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                continue; // Rooms have their own bookkeeping, sell them first
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
                        terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) ? playerId : Player.NEUTRAL_PLAYER_ID);
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
    public void addGold(int amount, short playerId) {
        gameWorldController.addGold(playerId, amount);
    }

    @Override
    public void addMana(int amount, short playerId) {
        gameController.getPlayerController(playerId).getManaControl().addMana(amount);
    }

}
