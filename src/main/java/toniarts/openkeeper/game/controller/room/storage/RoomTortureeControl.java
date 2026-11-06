/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.game.controller.room.storage;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Holds out the creatures tortured in a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class RoomTortureeControl extends AbstractRoomObjectControl<EntityId> {

    private static final short ELECTRIC_CHAIR_OBJECT_ID = 61;
    private static final short TORTURE_COALS_OBJECT_ID = 62;
    private static final short TORTURE_WATER_OBJECT_ID = 63;

    public RoomTortureeControl(IKwdFile kwdFile, IRoomController parent, EntityData entityData, IGameTimer gameTimer) {
        super(kwdFile, parent, entityData, gameTimer, ObjectType.TORTUREE);
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }

    /**
     * Puts a victim on a torture device. Like in DK II, each device holds one
     * victim. The victim goes on the free device closest to where it was
     * dropped.
     *
     * @param torturee the victim
     * @param p where the victim was dropped, may be {@code null}
     * @return the device the victim is on, {@code null} if every device is
     * taken
     */
    @Override
    public EntityId addItem(EntityId torturee, Point p) {
        EntityId device = getFreeDevice(torturee, p);
        if (device == null) {
            return null;
        }

        objectsByCoordinate.computeIfAbsent(getDevicePoint(device), point -> new ArrayList<>()).add(torturee);
        setRoomStorageToItem(torturee, false);
        addCurrentCapacity(1);

        return device;
    }

    /**
     * Puts the victims on the devices again after the room has been constructed
     * again (e.g. extended), as the old devices are gone. A victim stays on the
     * tile it was on if there is a device there still. Otherwise it goes on the
     * closest free device, or is set free if there is none.
     */
    public void refreshDevices() {
        Map<Point, EntityId> devicesByPoint = new HashMap<>();
        for (EntityId device : getDevices()) {
            devicesByPoint.put(getDevicePoint(device), device);
        }
        Map<EntityId, Point> moved = new LinkedHashMap<>();
        for (Iterator<Map.Entry<Point, Collection<EntityId>>> it = objectsByCoordinate.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Point, Collection<EntityId>> entry = it.next();
            EntityId device = devicesByPoint.get(entry.getKey());
            for (EntityId victim : entry.getValue()) {
                if (device != null) {
                    setDevice(victim, device);
                } else {
                    moved.put(victim, entry.getKey());
                }
            }
            if (device == null) {
                it.remove();
            }
        }
        for (Map.Entry<EntityId, Point> entry : moved.entrySet()) {
            EntityId victim = entry.getKey();
            EntityId device = getFreeDevice(victim, entry.getValue());
            if (device == null) {
                entityData.removeComponent(victim, Stored.class);
                entityData.removeComponent(victim, CreatureTortured.class);
                addCurrentCapacity(-1);
                continue;
            }
            objectsByCoordinate.computeIfAbsent(getDevicePoint(device), point -> new ArrayList<>()).add(victim);
            Position devicePosition = entityData.getComponent(device, Position.class);
            entityData.setComponent(victim, new Position(devicePosition.rotation,
                    new Vector3f(devicePosition.position.x, WorldUtils.FLOOR_HEIGHT, devicePosition.position.z)));
            setDevice(victim, device);
        }
    }

    private void setDevice(EntityId victim, EntityId device) {
        CreatureTortured tortured = entityData.getComponent(victim, CreatureTortured.class);
        if (tortured != null) {
            entityData.setComponent(victim, new CreatureTortured(tortured.timeTortured,
                    tortured.tortureCheckTime, tortured.healthCheckTime, device));
        }
    }

    @Override
    public void removeItem(EntityId object) {
        if (!isVictim(object)) {
            return;
        }
        super.removeItem(object);

        addCurrentCapacity(-1);
    }

    @Override
    public void destroy() {

        // The victims are set free
        for (Collection<EntityId> victims : objectsByCoordinate.values()) {
            for (EntityId victim : victims) {
                entityData.removeComponent(victim, Stored.class);
                entityData.removeComponent(victim, CreatureTortured.class);
            }
        }
        objectsByCoordinate.clear();

        super.destroy();
    }

    private boolean isVictim(EntityId object) {
        for (Collection<EntityId> victims : objectsByCoordinate.values()) {
            if (victims.contains(object)) {
                return true;
            }
        }
        return false;
    }

    private EntityId getFreeDevice(EntityId torturee, Point p) {
        boolean electrocutable = canBeElectrocuted(torturee);
        EntityId closest = null;
        int closestDistance = Integer.MAX_VALUE;
        for (EntityId device : getDevices()) {

            // DK II electrocutes only the creatures flagged for it (data +0x686 bit 0x80000), the others don't stay
            // on the chair
            if (!electrocutable && isElectricChair(device)) {
                continue;
            }
            Point devicePoint = getDevicePoint(device);
            Collection<EntityId> victims = objectsByCoordinate.get(devicePoint);
            if (victims != null && !victims.isEmpty()) {
                continue;
            }
            int distance = (p == null ? 0 : Math.abs(devicePoint.x - p.x) + Math.abs(devicePoint.y - p.y));
            if (distance < closestDistance) {
                closest = device;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private boolean canBeElectrocuted(EntityId creature) {
        CreatureComponent creatureComponent = entityData.getComponent(creature, CreatureComponent.class);
        return creatureComponent == null
                || kwdFile.getCreature(creatureComponent.creatureId).getFlags().contains(Creature.CreatureFlag.CAN_BE_ELECTROCUTED);
    }

    private boolean isElectricChair(EntityId device) {
        ObjectComponent objectComponent = entityData.getComponent(device, ObjectComponent.class);
        return objectComponent != null && objectComponent.objectId == ELECTRIC_CHAIR_OBJECT_ID;
    }

    private List<EntityId> getDevices() {
        List<EntityId> devices = new ArrayList<>(parent.getFloorFurniture());
        devices.addAll(parent.getWallFurniture());

        // The coals and water troughs under the wheels on the wall hold no victim
        devices.removeIf(device -> {
            ObjectComponent objectComponent = entityData.getComponent(device, ObjectComponent.class);
            return objectComponent != null && (objectComponent.objectId == TORTURE_COALS_OBJECT_ID
                    || objectComponent.objectId == TORTURE_WATER_OBJECT_ID);
        });
        return devices;
    }

    private Point getDevicePoint(EntityId device) {
        return WorldUtils.vectorToPoint(entityData.getComponent(device, Position.class).position);
    }

    @Override
    public void captured(short playerId) {

    }

    @Override
    protected Collection<Point> getCoordinates() {

        // Only furniture, once per tile
        Set<Point> coordinates = new LinkedHashSet<>(parent.getFloorFurnitureCount() + parent.getWallFurnitureCount());
        for (EntityId oc : parent.getFloorFurniture()) {
            coordinates.add(WorldUtils.vectorToPoint(entityData.getComponent(oc, Position.class).position));
        }
        for (EntityId oc : parent.getWallFurniture()) {
            coordinates.add(WorldUtils.vectorToPoint(entityData.getComponent(oc, Position.class).position));
        }
        return coordinates;
    }

}
