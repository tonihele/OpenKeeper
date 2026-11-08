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
package toniarts.openkeeper.view;

import com.simsilica.es.ComponentFilter;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.StringIndex;
import com.simsilica.es.WatchedEntity;
import java.util.Set;

/**
 * The entity data the client gets to see. Over the network the client works on
 * a {@code RemoteEntityData}, where looking up a single entity or component
 * ({@link #getComponent(EntityId, Class)}, {@link #getEntity(EntityId, Class...)},
 * the {@code find*} methods) waits for an answer from the server. This wraps
 * the client's entity data, also in a local game, and refuses those calls so
 * that they are caught early. The client reads entities through entity sets
 * and watched entities instead, those are kept up to date in the background.
 * <p>
 * Like {@code RemoteEntityData}, it is read-only.
 *
 * @see <a href="https://github.com/tonihele/OpenKeeper/issues/604">#604</a>
 */
public final class ClientEntityData implements EntityData {

    private final EntityData entityData;

    public ClientEntityData(EntityData entityData) {
        this.entityData = entityData;
    }

    @Override
    public EntitySet getEntities(Class... types) {
        return entityData.getEntities(types);
    }

    @Override
    public EntitySet getEntities(ComponentFilter filter, Class... types) {
        return entityData.getEntities(filter, types);
    }

    @Override
    public WatchedEntity watchEntity(EntityId entityId, Class... types) {
        return entityData.watchEntity(entityId, types);
    }

    @Override
    public StringIndex getStrings() {
        return entityData.getStrings();
    }

    @Override
    public void close() {

        // Not ours to close, in a local game it is the server's
    }

    @Override
    public <T extends EntityComponent> T getComponent(EntityId entityId, Class<T> type) {
        throw lookUp("getComponent");
    }

    @Override
    public Entity getEntity(EntityId entityId, Class... types) {
        throw lookUp("getEntity");
    }

    @Override
    public EntityId findEntity(ComponentFilter filter, Class... types) {
        throw lookUp("findEntity");
    }

    @Override
    public Set<EntityId> findEntities(ComponentFilter filter, Class... types) {
        throw lookUp("findEntities");
    }

    @Override
    public EntityId createEntity() {
        throw readOnly();
    }

    @Override
    public void removeEntity(EntityId entityId) {
        throw readOnly();
    }

    @Override
    public <T extends EntityComponent> void setComponent(EntityId entityId, T component) {
        throw readOnly();
    }

    @Override
    public void setComponents(EntityId entityId, EntityComponent... components) {
        throw readOnly();
    }

    @Override
    public <T extends EntityComponent> boolean removeComponent(EntityId entityId, Class<T> type) {
        throw readOnly();
    }

    @Override
    public void removeComponents(EntityId entityId, Class... types) {
        throw readOnly();
    }

    private static UnsupportedOperationException lookUp(String method) {
        return new UnsupportedOperationException(method + "() waits for the server on the client, use an entity set or a watched entity instead");
    }

    private static UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("The client entity data is read-only, ask the server to make the change");
    }

}
