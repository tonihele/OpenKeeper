package toniarts.openkeeper.game.controller;

import java.util.Set;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * Shared drop rules for the keeper hand, used by both client and server
 */
public final class DropValidator {

    private DropValidator() {
    }

    public static boolean canDrop(Interaction interaction, IMapTileInformation tile, Terrain terrain, short playerId) {
        if (interaction == null || tile == null || terrain == null) {
            return false;
        }
        Set<Terrain.TerrainFlag> flags = terrain.getFlags();
        if (flags.contains(Terrain.TerrainFlag.SOLID)) {
            return false;
        }

        // Normal rule: own claimed land
        if (tile.getOwnerId() == playerId && flags.contains(Terrain.TerrainFlag.OWNABLE)) {
            return true;
        }

        // Workers: also unclaimed floor (Dirt Path) and neutral land, but not water/lava
        if (interaction.canBeDroppedOnAnyLand) {
            boolean unclaimed = !flags.contains(Terrain.TerrainFlag.OWNABLE)
                    || tile.getOwnerId() == Player.NEUTRAL_PLAYER_ID;
            return unclaimed
                    && !flags.contains(Terrain.TerrainFlag.WATER)
                    && !flags.contains(Terrain.TerrainFlag.LAVA);
        }
        return false;
    }
}