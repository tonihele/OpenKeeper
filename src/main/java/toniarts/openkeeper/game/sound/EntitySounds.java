/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.game.sound;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Tells the sounds of entities, i.e. on the sound category of the creature or object type of the entity, as
 * {@link SoundCues}. The clients play them.
 */
public final class EntitySounds {

    private final EntityData entityData;
    private final IKwdFile kwdFile;

    public EntitySounds(EntityData entityData, IKwdFile kwdFile) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
    }

    /**
     * @return the sound category of the type of the entity, {@code null} if it has none
     */
    @Nullable
    public String getCategory(EntityId entityId) {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        if (creatureComponent != null) {
            Creature creature = kwdFile.getCreature(creatureComponent.creatureId);
            return creature != null ? creature.getSoundCategory() : null;
        }
        ObjectComponent objectComponent = entityData.getComponent(entityId, ObjectComponent.class);
        if (objectComponent != null) {
            GameObject object = kwdFile.getObject(objectComponent.objectId);
            return object != null ? object.getSoundCategory() : null;
        }
        return null;
    }

    /**
     * Makes a sound at the current position of the entity.
     */
    public void play(EntityId entityId, int eventId) {
        Position position = entityData.getComponent(entityId, Position.class);
        if (position != null) {
            play(entityId, eventId, position.position);
        }
    }

    public void play(EntityId entityId, int eventId, Vector3f position) {
        SoundCues.emit(entityData, getCategory(entityId), eventId, position);
    }

    /**
     * Makes a sound at the current position of the entity, unless its previous sound of the same kind still plays.
     */
    public void playExclusive(EntityId entityId, int eventId) {
        Position position = entityData.getComponent(entityId, Position.class);
        if (position != null) {
            SoundCues.emitExclusive(entityData, getCategory(entityId), eventId,
                    WorldUtils.vectorToPoint(position.position), entityId);
        }
    }

    public boolean isCreature(EntityId entityId) {
        return entityData.getComponent(entityId, CreatureComponent.class) != null;
    }
}
