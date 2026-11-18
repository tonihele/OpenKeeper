package toniarts.openkeeper.tools.convert.map;

/**
 * Builds map data fixtures with a sound category for tests. Lives in the map package because the setters are
 * {@code protected} (parser-only).
 */
public final class SoundFixtures {

    private SoundFixtures() {
    }

    public static GameObject object(String soundCategory) {
        GameObject object = new GameObject();
        object.setSoundCategory(soundCategory);
        return object;
    }

    public static Room room(String soundCategory) {
        Room room = new Room();
        room.setSoundCategory(soundCategory);
        return room;
    }

}
