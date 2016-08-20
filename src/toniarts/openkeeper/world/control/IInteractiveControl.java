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
package toniarts.openkeeper.world.control;

import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;

/**
 * A small interface telling that the control in interactive
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IInteractiveControl extends Control {

    public String getTooltip(short playerId);

    public boolean isPickable(short playerId);

    public boolean isInteractable(short playerId);

    public IInteractiveControl pickUp(short playerId);

    public CursorFactory.CursorType getInHandCursor();

    public ArtResource getInHandMesh();

    public Spatial getSpatial();

    public boolean interact(short playerId);

    public void onHover();

}
