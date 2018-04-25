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
package toniarts.openkeeper.world.object;

import com.jme3.scene.control.AbstractControl;
import java.util.logging.Logger;
import toniarts.openkeeper.utils.AssetUtils;
import static toniarts.openkeeper.world.MapLoader.COLOR_FLASH;
import toniarts.openkeeper.world.control.IInteractiveControl;

/**
 *
 * @author ArchDemon
 */
public abstract class HighlightControl extends AbstractControl implements IInteractiveControl {

    private boolean active = false;

    private static final Logger logger = Logger.getLogger(HighlightControl.class.getName());

    @Override
    public void onHoverStart() {
        setHighlight(true);
    }

    @Override
    public void onHoverEnd() {
        setHighlight(false);
    }

    private void setHighlight(final boolean enabled) {
        if (active != enabled) {
            active = enabled;
            AssetUtils.setModelHighlight(spatial, COLOR_FLASH, enabled);
        }
    }
}
