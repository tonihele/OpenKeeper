/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.simsilica.es.Entity;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SummonedReaper;

/**
 * A state that handles ended game actions, such as cinematics and stuff
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class EndGameState extends AbstractAppState implements RawInputListener {

    /**
     * How many times the Horned Reaper's height the picture shows, so that
     * there is room around him
     */
    private static final float REAPER_FRAMING = 2f;

    /**
     * The part of the picture the wide screen bars leave visible
     */
    private static final float VISIBLE_PART = 0.75f;

    /**
     * How much of the way to the Reaper the camera goes in a second
     */
    private static final float FOLLOW_SPEED = 2f;

    private Main app;
    private AppStateManager stateManager;
    private final PlayerState playerState;
    private EntitySet reapers;

    public EndGameState(PlayerState playerState) {
        this.playerState = playerState;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        this.app = (Main) app;
        this.stateManager = stateManager;

        // Add the listener
        app.getInputManager().addRawInputListener(this);

        // Add the texts
        playerState.setWideScreen(true);
        playerState.setGeneralText(2901);

        reapers = playerState.getEntityData().getEntities(SummonedReaper.class, Position.class, CreatureComponent.class);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        if (reapers == null) {
            return;
        }

        // The camera follows the Horned Reaper who comes for the Portal Gem
        reapers.applyChanges();
        for (Entity reaper : reapers) {
            if (reaper.get(SummonedReaper.class).gem != null) {
                followReaper(reaper.get(Position.class).position, reaper.get(CreatureComponent.class).height, tpf);
                return;
            }
        }
    }

    /**
     * Glides the camera over to look at the Reaper's chest and keeps it far
     * enough to show all of him, from his feet to his horns, inside the wide
     * screen bars. The camera keeps its angle.
     *
     * @param feet where the Reaper's feet are
     * @param height the height of the Reaper
     * @param tpf the time since the last frame
     */
    private void followReaper(Vector3f feet, float height, float tpf) {
        Camera camera = app.getCamera();
        Vector3f direction = camera.getDirection();
        Vector3f chest = feet.add(0, height / 2f, 0);
        float distance = (chest.y - camera.getLocation().y) / direction.y;
        Vector3f lookAt = direction.mult(distance).addLocal(camera.getLocation());
        float framingDistance = height * REAPER_FRAMING
                / (2f * FastMath.tan(camera.getFov() * FastMath.DEG_TO_RAD / 2f) * VISIBLE_PART);

        float amount = Math.min(1f, FOLLOW_SPEED * tpf);
        lookAt.interpolateLocal(chest, amount);
        distance += (framingDistance - distance) * amount;
        camera.setLocation(lookAt.subtractLocal(direction.mult(distance)));
    }

    @Override
    public void cleanup() {
        app.getInputManager().removeRawInputListener(this);

        // Gives the camera back
        if (reapers != null) {
            reapers.release();
            reapers = null;
        }
        super.cleanup();
    }

    @Override
    public void beginInput() {

    }

    @Override
    public void endInput() {

    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {

    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {

    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {

    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {

    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if (evt.isPressed()) {
            if (evt.getKeyCode() == KeyInput.KEY_SPACE) {
                stateManager.detach(this);
                playerState.quitToDebriefing();
            } else if (evt.getKeyCode() == KeyInput.KEY_ESCAPE || evt.getKeyCode() == KeyInput.KEY_LCONTROL) {
                playerState.setWideScreen(false);
                playerState.setGeneralText(2901);
                cleanup();
                playerState.setEnabled(true);
            }
        }
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {

    }

}
