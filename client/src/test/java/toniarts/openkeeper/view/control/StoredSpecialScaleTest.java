package toniarts.openkeeper.view.control;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.tools.convert.map.GameObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static toniarts.openkeeper.tools.convert.map.GameObjectFixtures.gameObject;

class StoredSpecialScaleTest {
    @Test
    void replicatedStorageShrinksSpecialAndRemovalRestoresModelScale() {
        EntityData data = new DefaultEntityData();
        EntityId id = data.createEntity();
        // Object 109 skips animation loading, allowing a synthetic model without assets.
        GameObject object = gameObject("Synthetic special", (short) 109, 0);
        ObjectViewControl control = new ObjectViewControl(id, data, object, new ObjectViewState(), null, null);
        Node model = new Node();
        Vector3f normal = new Vector3f(2, 3, 4);
        model.setLocalScale(normal);
        model.addControl(control);
        data.setComponent(id, new Stored(data.createEntity(), ObjectType.SPECIAL));
        control.controlUpdate(0.1f);
        assertEquals(normal.mult(75f / 128), model.getLocalScale());
        control.controlUpdate(0.1f);
        assertEquals(normal.mult(75f / 128), model.getLocalScale());
        data.removeComponent(id, Stored.class);
        control.controlUpdate(0.1f);
        assertEquals(normal, model.getLocalScale());
        data.setComponent(id, new Stored(data.createEntity(), ObjectType.SPELL_BOOK));
        control.controlUpdate(0.1f);
        assertEquals(normal, model.getLocalScale());
        control.cleanup();
    }
}
