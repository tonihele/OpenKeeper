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
package toniarts.openkeeper.tools.convert.map;

import com.jme3.anim.*;
import com.jme3.asset.AssetManager;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.view.control.ObjectViewControl;
import static org.junit.jupiter.api.Assertions.*;

class WorkbenchAnimationTest {
    Node model() {
        Node root = new Node("synthetic model");
        Node blade = new Node("Spin"); root.attachChild(blade);
        blade.setUserData(KmfModelLoader.FRAME_FACTOR_FUNCTION, "WRAP");
        AnimComposer composer = new AnimComposer(); blade.addControl(composer);
        AnimClip clip = new AnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
        clip.setTracks(new AnimTrack[]{new TransformTrack(blade, new float[]{0, 1},
            new Vector3f[]{new Vector3f(), new Vector3f(1,0,0)},
            new Quaternion[]{new Quaternion(), new Quaternion()},
            new Vector3f[]{new Vector3f(1,1,1), new Vector3f(1,1,1)})});
        composer.addAnimClip(clip);
        return root;
    }
    ObjectViewState view(GameObject.State state) {
        return new ObjectViewState((short) 46, state, ObjectViewState.GameObjectAnimState.MESH_RESOURCE, true);
    }
    @Test void realObjectControlHoldsIdleFrameAnimatesWhileActiveAndResetsAfterward() {
        DefaultEntityData data = new DefaultEntityData();
        GameObject object = new GameObject(); object.setObjectId((short) 46);
        ArtResource resource = new ArtResource(); resource.setName("SyntheticWorkbenchSpin");
        resource.setType(ArtResource.ArtResourceType.ANIMATING_MESH);
        resource.setFlags(EnumSet.noneOf(ArtResource.ArtResourceFlag.class));
        object.setMeshResource(resource);
        AssetManager assets = (AssetManager) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{AssetManager.class}, (p,m,a) -> {
                if (m.getName().equals("loadModel")) return model();
                throw new UnsupportedOperationException(m.getName());
            });
        ObjectViewControl control = new ObjectViewControl(data.createEntity(), data, object,
            view(GameObject.State.NONE), assets, null);
        Node root = new Node(); root.addControl(control);
        for(int i=0;i<5;i++) root.updateLogicalState(.13f);
        assertEquals(0, root.getChild("SyntheticWorkbenchSpin").getLocalTranslation().x);
        control.setTargetState(view(GameObject.State.ACTIVATE_ANIM));
        for(int i=0;i<3;i++) root.updateLogicalState(.13f);
        assertTrue(root.getChild("SyntheticWorkbenchSpin").getLocalTranslation().x > 0);
        control.setTargetState(view(GameObject.State.NONE));
        for(int i=0;i<5;i++) root.updateLogicalState(.13f);
        assertEquals(0, root.getChild("SyntheticWorkbenchSpin").getLocalTranslation().x);
        control.setTargetState(view(GameObject.State.ACTIVATE_ANIM));
        for(int i=0;i<3;i++) root.updateLogicalState(.13f);
        assertTrue(root.getChild("SyntheticWorkbenchSpin").getLocalTranslation().x > 0);
        control.cleanup(); data.close();
    }
}
