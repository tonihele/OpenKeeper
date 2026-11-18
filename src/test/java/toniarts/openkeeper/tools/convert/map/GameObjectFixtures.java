package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;
import java.util.List;

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

    public static GameObject gameObject(String name, short objectId, int tooltipStringId, GameObject.ObjectFlag... flags) {
        GameObject object = gameObject(name, flags);
        object.setObjectId(objectId);
        object.setTooltipStringId(tooltipStringId);
        return object;
    }

    public static GameObject gameObject(short objectId, ArtResource meshResource, ArtResource... additionalResources) {
        GameObject object = gameObject("Object " + objectId);
        object.setObjectId(objectId);
        object.setMeshResource(meshResource);
        object.setAdditionalResources(List.of(additionalResources));
        return object;
    }

    public static ArtResource animation(int frames, int fps) {
        ArtResource animation = new ArtResource();
        animation.setData(ArtResource.KEY_FRAMES, frames);
        animation.setData(ArtResource.KEY_FPS, fps);
        return animation;
    }

}
