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

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Senses;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A creature senses the entities it can reach within its sense range, going
 * from tile to tile around the solid ones.
 */
public class PositionSystemSensingTest {

    /**
     * # is solid, the creature that senses stands in the top left corner
     */
    private static final String[] MAP = {
        "......",
        ".#####",
        "..#..."};

    private static final Terrain FLOOR = TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(), 0, 0, 0, (short) 0);
    private static final Terrain ROCK = TerrainFixtures.terrain((short) 2, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID), 0, 0, 0, (short) 0);

    private final DefaultEntityData entityData = new DefaultEntityData();
    private final IMapTileInformation[][] tiles = new IMapTileInformation[MAP[0].length()][MAP.length];

    @Test
    public void sensesWhatIsInWalkingRangeAroundTheWalls() {
        EntityId creature = createEntity(0, 0);
        entityData.setComponents(creature, new Senses(4, 4), new Owner(Player.KEEPER1_ID, Player.KEEPER1_ID));
        EntityId atRange = createEntity(4, 0);
        createEntity(5, 0); // One tile too far
        EntityId aroundTheCorner = createEntity(1, 2);
        createEntity(3, 2); // Two tiles away, but walled off
        EntityId sameTile = createEntity(0, 0);

        PositionSystem positionSystem = new PositionSystem(createMapController(), entityData, null, null, null);

        assertEquals(Set.of(atRange, aroundTheCorner, sameTile), positionSystem.getSensedEntities(creature));
    }

    private EntityId createEntity(int x, int y) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new Position(0, WorldUtils.pointToVector3f(x, y)));
        return entity;
    }

    private IMapTileInformation getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= tiles.length || y >= MAP.length) {
            return null;
        }
        if (tiles[x][y] == null) {
            tiles[x][y] = stub(IMapTileInformation.class, (proxy, name, args) -> switch (name) {
                case "getX" -> x;
                case "getY" -> y;
                case "hashCode" -> y * tiles.length + x;
                case "equals" -> proxy == args[0];
                default -> null;
            });
        }
        return tiles[x][y];
    }

    private IMapController createMapController() {
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (proxy, name, args) -> switch (name) {
            case "getWidth" -> tiles.length;
            case "getHeight" -> MAP.length;
            case "getTile" -> args.length == 1 ? getTile(((Point) args[0]).x, ((Point) args[0]).y) : getTile((Integer) args[0], (Integer) args[1]);
            default -> null;
        });
        return stub(IMapController.class, (proxy, name, args) -> switch (name) {
            case "getMapData" -> mapData;
            case "getTerrain" -> {
                IMapTileInformation tile = (IMapTileInformation) args[0];
                yield MAP[tile.getY()].charAt(tile.getX()) == '#' ? ROCK : FLOOR;
            }
            default -> null;
        });
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(Object proxy, String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(proxy, method.getName(), args));
    }

}
