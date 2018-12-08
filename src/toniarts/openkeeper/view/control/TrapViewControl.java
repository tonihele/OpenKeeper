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
import com.jme3.scene.Spatial;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Objects;
import java.util.ResourceBundle;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.text.TextParser;

/**
 * View control that is intended specifically for traps
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TrapViewControl extends EntityViewControl<Trap, TrapViewState> {

    private boolean initialized = false;

    public TrapViewControl(EntityId entityId, EntityData entityData, Trap data, TrapViewState viewState,
            AssetManager assetManager, TextParser textParser) {
        super(entityId, entityData, data, viewState, assetManager, textParser);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        // Play the starting animation
        if (spatial != null && !initialized) {
            setState(null, targetState);
            initialized = true;
        }
    }

    @Override
    public String getTooltip(short playerId) {
        ResourceBundle bundle = Utils.getMainTextResourceBundle();
        Trap trap = getDataObject();
        String tooltip;
        if (trap.getFlags().contains(Trap.TrapFlag.GUARD_POST)) {
            if (playerId == getOwnerId()) {
                tooltip = bundle.getString("2538");
            } else {
                tooltip = bundle.getString(Integer.toString(trap.getNameStringId()));
            }
        } else {

            // Regular traps
            if (playerId == getOwnerId()) {
                tooltip = bundle.getString("2534");
            } else {
                tooltip = bundle.getString("2542");
            }
        }

        return textParser.parseText(tooltip, getEntityId(), trap);
    }


    @Override
    public boolean isStopAnimation() {
        return true;
    }

    @Override
    public void setTargetState(TrapViewState viewState) {
        super.setTargetState(viewState);

        setState(currentState, targetState);
    }

    private void setState(TrapViewState current, TrapViewState target) {
        Objects.requireNonNull(target, "You need to give the target state!");

        if (current == null || current.blueprint != target.blueprint) {
            if (target.blueprint) {
                AssetUtils.setBlueprint(assetManager, spatial);
            } else {
                // TODO: Unblueprint?
            }
        }
        currentState = target;
    }

    @Override
    public CursorFactory.CursorType getInHandCursor() {
        return CursorFactory.CursorType.HOLD_THING;
    }

    @Override
    protected ArtResource getAnimationData(TrapViewState viewState) {
        return getDataObject().getMeshResource();
    }

    @Override
    public void onHover(short playerId) {
        if (!currentState.blueprint) {
            super.onHover(playerId);
        }
    }

    @Override
    public void onHoverEnd(short playerId) {
        super.onHoverEnd(playerId);

        // Restore blueprint state
        if (currentState.blueprint) {
            AssetUtils.setBlueprint(assetManager, spatial);
        }
    }
}
