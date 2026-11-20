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

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Like in DK II, loose gold has no owner (the neutral player owns it) and the
 * land it lies on decides who can pick it up (#580)
 */
class EntityPickupValidatorTest {

    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;
    private static final Point TILE = new Point(5, 7);
    private static final ObjectComponent GOLD = new ObjectComponent(ObjectsController.OBJECT_GOLD_ID, ObjectType.GOLD);
    private static final ObjectComponent SPECIAL = new ObjectComponent((short) 3, ObjectType.SPECIAL);
    private static final ObjectComponent SPELL_BOOK = new ObjectComponent((short) 2, ObjectType.SPELL_BOOK);
    private static final Owner OWN = new Owner(PLAYER_ID, PLAYER_ID);
    private static final Owner ENEMY = new Owner(ENEMY_ID, ENEMY_ID);
    private static final Owner NEUTRAL = new Owner(Player.NEUTRAL_PLAYER_ID, Player.NEUTRAL_PLAYER_ID);

    @Test
    void ownAndNeutralObjectsCanBePickedUp() {
        assertTrue(EntityPickupValidator.isValidOwner(SPELL_BOOK, OWN, PLAYER_ID));
        assertTrue(EntityPickupValidator.isValidOwner(GOLD, NEUTRAL, PLAYER_ID));
    }

    @Test
    void enemyObjectsCantBePickedUp() {
        assertFalse(EntityPickupValidator.isValidOwner(GOLD, ENEMY, PLAYER_ID));
        assertFalse(EntityPickupValidator.isValidOwner(SPELL_BOOK, ENEMY, PLAYER_ID));
        assertFalse(EntityPickupValidator.isValidOwner(GOLD, null, PLAYER_ID));
    }

    @Test
    void neutralCreaturesCantBePickedUp() {
        assertFalse(EntityPickupValidator.isValidOwner(null, NEUTRAL, PLAYER_ID));
        assertTrue(EntityPickupValidator.isValidOwner(null, OWN, PLAYER_ID));
    }

    @Test
    void neutralGoldCanBePickedUpFromOwnLand() {
        assertTrue(EntityPickupValidator.isValidLocation(GOLD, NEUTRAL, positionOn(TILE), PLAYER_ID, landOwnedBy(PLAYER_ID)));
    }

    @Test
    void neutralGoldCantBePickedUpFromEnemyLand() {
        assertFalse(EntityPickupValidator.isValidLocation(GOLD, NEUTRAL, positionOn(TILE), PLAYER_ID, landOwnedBy(ENEMY_ID)));
    }

    @Test
    void neutralObjectsFollowTheOwnLandRule() {
        assertTrue(EntityPickupValidator.isValidLocation(SPELL_BOOK, NEUTRAL, positionOn(TILE), PLAYER_ID, landOwnedBy(PLAYER_ID)));
        assertFalse(EntityPickupValidator.isValidLocation(SPELL_BOOK, NEUTRAL, positionOn(TILE), PLAYER_ID, landOwnedBy(ENEMY_ID)));
    }

    @Test
    void ownObjectsAndCreaturesCanBePickedUpFromAnyLand() {
        assertTrue(EntityPickupValidator.isValidLocation(SPELL_BOOK, OWN, positionOn(TILE), PLAYER_ID, landOwnedBy(ENEMY_ID)));
        assertTrue(EntityPickupValidator.isValidLocation(null, OWN, positionOn(TILE), PLAYER_ID, landOwnedBy(ENEMY_ID)));
    }

    @Test
    void specialsFollowTheSameOwnLandRuleAsGold() {
        assertTrue(EntityPickupValidator.isValidLocation(SPECIAL, OWN, positionOn(TILE), PLAYER_ID, landOwnedBy(PLAYER_ID)));
        assertFalse(EntityPickupValidator.isValidLocation(SPECIAL, OWN, positionOn(TILE), PLAYER_ID, landOwnedBy(ENEMY_ID)));
    }

    @Test
    void goldWithoutPositionOrMapCantBePickedUp() {
        assertFalse(EntityPickupValidator.isValidLocation(GOLD, NEUTRAL, null, PLAYER_ID, landOwnedBy(PLAYER_ID)));
        assertFalse(EntityPickupValidator.isValidLocation(GOLD, NEUTRAL, positionOn(TILE), PLAYER_ID, null));
    }

    private static Position positionOn(Point tile) {
        return new Position(0, WorldUtils.pointToVector3f(tile));
    }

    /**
     * A map where every tile is owned by the given player
     */
    @SuppressWarnings("unchecked")
    private static IMapDataInformation<IMapTileInformation> landOwnedBy(short ownerId) {
        IMapTileInformation tile = (IMapTileInformation) Proxy.newProxyInstance(IMapTileInformation.class.getClassLoader(),
                new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("getOwnerId")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return ownerId;
                });
        return (IMapDataInformation<IMapTileInformation>) Proxy.newProxyInstance(IMapDataInformation.class.getClassLoader(),
                new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("getTile")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return tile;
                });
    }

}
