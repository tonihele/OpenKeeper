/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * These are the identifiers for each different data type
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public enum MapDataTypeEnum implements IValueEnum {

    GLOBALS(0), // As in overrides the regular ones
    MAP(100),
    TERRAIN(110),
    ROOMS(120),
    TRAPS(130),
    DOORS(140),
    KEEPER_SPELLS(150),
    CREATURE_SPELLS(160),
    CREATURES(170),
    PLAYERS(180),
    THINGS(190),
    TRIGGERS(210),
    LEVEL(220),
    VARIABLES(230),
    OBJECTS(240),
    EFFECT_ELEMENTS(250),
    SHOTS(260),
    EFFECTS(270);

    private MapDataTypeEnum(int id) {
        this.id = id;
    }

    @Override
    public int getValue() {
        return id;
    }
    private final int id;
}
