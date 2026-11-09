/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view.map;

import com.jme3.scene.Node;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/** Keeps a room's running trail separate from its disposable geometry. */
public final class RoomFlameManager {
    private record RoomKey(short type, short owner, List<Point> tiles) {
        RoomKey(RoomInstance room) {
            this(room.getRoom().getRoomId(), room.getOwnerId(), List.copyOf(room.getCoordinates()));
        }
    }

    private final Map<RoomKey, Node> trails = new HashMap<>();
    private final Node parent;
    private final EffectManagerState effects;
    private final IFogOfWarInformation fog;

    public RoomFlameManager(Node parent, EffectManagerState effects, IFogOfWarInformation fog) {
        this.parent = parent;
        this.effects = effects;
        this.fog = fog;
    }

    public void add(RoomInstance room) {
        if (!RoomFlameControl.isEligible(room)) {
            return;
        }
        trails.computeIfAbsent(new RoomKey(room), key -> {
            Node trail = new Node("Room perimeter flame");
            trail.addControl(new RoomFlameControl(room, effects, fog));
            parent.attachChild(trail);
            return trail;
        });
    }

    /** Called after a complete geometry update, once replacement room instances are registered. */
    public void retainRooms(Collection<RoomInstance> rooms) {
        Set<RoomKey> live = new HashSet<>();
        for (RoomInstance room : rooms) {
            if (RoomFlameControl.isEligible(room)) {
                live.add(new RoomKey(room));
            }
        }
        trails.entrySet().removeIf(entry -> {
            if (live.contains(entry.getKey())) {
                return false;
            }
            Node trail = entry.getValue();
            trail.getControl(RoomFlameControl.class).clear();
            trail.removeFromParent();
            return true;
        });
    }
}
