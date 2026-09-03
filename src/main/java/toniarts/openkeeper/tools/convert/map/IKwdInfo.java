package toniarts.openkeeper.tools.convert.map;

/**
 * Common KWD info
 */
interface IKwdInfo extends Comparable<IKwdInfo> {

    GameMap getMap();

    GameLevel getGameLevel();

}
