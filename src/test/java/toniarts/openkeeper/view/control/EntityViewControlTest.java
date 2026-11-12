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
package toniarts.openkeeper.view.control;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.ArtResource;

import static org.junit.jupiter.api.Assertions.assertSame;

class EntityViewControlTest {

    @Test
    void lightingConfigurerReceivesSpatialNeedingSetup() {
        DefaultEntityData entityData = new DefaultEntityData();
        EntityId entityId = entityData.createEntity();
        TestEntityViewControl control = new TestEntityViewControl(entityId, entityData);
        Spatial changedModel = new Node("changed-model");
        AtomicReference<Spatial> configured = new AtomicReference<>();

        control.setVertexLightingConfigurer(configured::set);
        control.requestVertexLighting(changedModel);

        assertSame(changedModel, configured.get());
        control.cleanup();
        entityData.close();
    }

    private static final class TestEntityViewControl extends EntityViewControl<Object, Object> {

        TestEntityViewControl(EntityId entityId, DefaultEntityData entityData) {
            super(entityId, entityData, null, null, null, null);
        }

        @Override
        protected ArtResource getAnimationData(Object state) {
            return null;
        }

        void requestVertexLighting(Spatial spatial) {
            configureVertexLighting(spatial);
        }
    }
}
