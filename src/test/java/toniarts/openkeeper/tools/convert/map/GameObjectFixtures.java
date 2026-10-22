package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;

/**
 * Builds {@link GameObject} fixtures for tests. Lives in {@code GameObject}'s
 * own package because its setters are {@code protected} (parser-only).
 */
public final class GameObjectFixtures {

    private GameObjectFixtures() {
    }

    public static GameObject gameObject(String name, GameObject.ObjectFlag... flags) {
        EnumSet<GameObject.ObjectFlag> set = EnumSet.noneOf(GameObject.ObjectFlag.class);
        for (GameObject.ObjectFlag flag : flags) {
            set.add(flag);
        }

        GameObject object = new GameObject();
        object.setName(name);
        object.setFlags(set);
        return object;
    }

}
