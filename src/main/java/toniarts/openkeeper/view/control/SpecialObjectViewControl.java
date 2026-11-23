/*
 * Copyright (C) 2014-2018 OpenKeeper
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

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.scene.Spatial;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.view.text.EntityTextParser;

/** The world presentation of a special box, including its hover wobble. */
public final class SpecialObjectViewControl extends ObjectViewControl {

    private boolean hovered;
    private boolean hoverWobbling;
    private float hoverBaseYaw;
    private float hoverPitch;
    private float hoverYaw;
    private float hoverRoll;

    public SpecialObjectViewControl(EntityId entityId, EntityData entityData, GameObject data, ObjectViewState state,
            AssetManager assetManager, EntityTextParser<GameObject> textParser,
            IMapDataInformation<? extends IMapTileInformation> mapData, boolean uniqueCreatureBed, int ticksPerSecond) {
        super(entityId, entityData, data, state, assetManager, textParser, mapData, uniqueCreatureBed, ticksPerSecond);
    }

    @Override
    protected Collection<Class<? extends EntityComponent>> getWatchedComponents() {
        Collection<Class<? extends EntityComponent>> components = super.getWatchedComponents();
        components.add(InHand.class);
        return components;
    }

    @Override
    public void onHoverStart(short playerId) {
        super.onHoverStart(playerId);
        hovered = true;
    }

    @Override
    public void onHoverEnd(short playerId) {
        super.onHoverEnd(playerId);
        hovered = false;
        resetHoverWobble();
    }

    @Override
    protected void controlUpdate(float tpf) {
        super.controlUpdate(tpf);
        updateHoverWobble();
    }

    @Override
    public void cleanup() {
        resetHoverWobble();
        super.cleanup();
    }

    /**
     * The native mouse handler (0x40C4AB) calls 0x4B4430 on a pointed-at special,
     * before checking whether the player can pick it up. It shakes the static
     * mesh on all three axes by random angles in [-25, 24] / 2048 of a turn;
     * there is no separate jump animation resource. Stored specials use the
     * same handler. The keeper's hand uses a separate presentation.
     * The native mouse path has no game-tick gate, so sample a new pose on
     * each view update rather than holding it until the next simulation tick.
     */
    private void updateHoverWobble() {
        Position position = getEntity().get(Position.class);
        if (!hovered || position == null
                || getEntity().get(InHand.class) != null || !currentState.visible
                || spatial.getCullHint() == Spatial.CullHint.Always) {
            resetHoverWobble();
            return;
        }
        hoverBaseYaw = position.rotation;
        if (!hoverWobbling) {
            hoverWobbling = true;
        } else {
            hoverPitch = randomHoverAngle();
            hoverYaw = randomHoverAngle();
            hoverRoll = randomHoverAngle();
        }
        spatial.setLocalRotation(spatial.getLocalRotation().fromAngles(
                hoverPitch, position.rotation + hoverYaw, hoverRoll));
    }

    private static float randomHoverAngle() {
        return ThreadLocalRandom.current().nextInt(-25, 25) * FastMath.TWO_PI / 2048;
    }

    private void resetHoverWobble() {
        if (hoverWobbling) {
            Position position = getEntity().get(Position.class);
            if (spatial != null) {
                spatial.setLocalRotation(spatial.getLocalRotation().fromAngles(0,
                        position != null ? position.rotation : hoverBaseYaw, 0));
            }
            hoverWobbling = false;
            hoverPitch = hoverYaw = hoverRoll = 0;
        }
    }

}
