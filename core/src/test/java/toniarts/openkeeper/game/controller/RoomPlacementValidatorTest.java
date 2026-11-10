package toniarts.openkeeper.game.controller;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.GameObject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.GameObject.ObjectFlag.CAN_BE_DROPPED_ON_ANY_LAND;
import static toniarts.openkeeper.tools.convert.map.GameObject.ObjectFlag.CAN_BE_PICKED_UP;
import static toniarts.openkeeper.tools.convert.map.GameObject.ObjectFlag.CAN_BE_SLAPPED;
import static toniarts.openkeeper.tools.convert.map.GameObject.ObjectFlag.OBJECT_TYPE_CRATE;
import static toniarts.openkeeper.tools.convert.map.GameObject.ObjectFlag.OBJECT_TYPE_FOOD;
import static toniarts.openkeeper.tools.convert.map.GameObject.ObjectFlag.OBJECT_TYPE_GOLD;
import static toniarts.openkeeper.tools.convert.map.GameObject.ObjectFlag.OBJECT_TYPE_SPELL_BOOK;
import static toniarts.openkeeper.tools.convert.map.GameObject.ObjectFlag.OBSTACLE;
import static toniarts.openkeeper.tools.convert.map.GameObject.ObjectFlag.PLACEABLE;
import static toniarts.openkeeper.tools.convert.map.GameObject.ObjectFlag.SOLID_OBSTACLE;
import static toniarts.openkeeper.tools.convert.map.GameObjectFixtures.gameObject;

/**
 * The object flags mirror the DKII object data.
 */
class RoomPlacementValidatorTest {

    @Test
    void looseGoldDoesNotBlockConstruction() {
        GameObject gold = gameObject("Gold", OBJECT_TYPE_GOLD, CAN_BE_PICKED_UP, PLACEABLE);

        assertFalse(RoomPlacementValidator.isConstructionBlockingObject(gold));
    }

    @Test
    void spellBooksAndCratesDoNotBlockConstruction() {
        GameObject spellBook = gameObject("Spell Book", OBJECT_TYPE_SPELL_BOOK, PLACEABLE);
        GameObject crate = gameObject("Crate Alarm", OBJECT_TYPE_CRATE, PLACEABLE);

        assertFalse(RoomPlacementValidator.isConstructionBlockingObject(spellBook));
        assertFalse(RoomPlacementValidator.isConstructionBlockingObject(crate));
    }

    @Test
    void obstaclesBlockConstruction() {
        GameObject barrel = gameObject("Barrel", OBSTACLE, SOLID_OBSTACLE);
        GameObject heroArmouryTrestle = gameObject("Hero Armoury Trestle", OBSTACLE, PLACEABLE);

        assertTrue(RoomPlacementValidator.isConstructionBlockingObject(barrel));
        assertTrue(RoomPlacementValidator.isConstructionBlockingObject(heroArmouryTrestle));
    }

    @Test
    void chickensAndCallToArmsDoNotBlockConstruction() {
        GameObject chicken = gameObject("Chicken", OBJECT_TYPE_FOOD, CAN_BE_PICKED_UP, CAN_BE_SLAPPED);
        GameObject callToArms = gameObject("Call To Arms", CAN_BE_PICKED_UP, CAN_BE_SLAPPED, CAN_BE_DROPPED_ON_ANY_LAND);

        assertFalse(RoomPlacementValidator.isConstructionBlockingObject(chicken));
        assertFalse(RoomPlacementValidator.isConstructionBlockingObject(callToArms));
    }

    @Test
    void unknownObjectBlocksConstruction() {
        assertTrue(RoomPlacementValidator.isConstructionBlockingObject(null));
    }

}
