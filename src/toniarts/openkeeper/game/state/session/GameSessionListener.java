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
package toniarts.openkeeper.game.state.session;

import com.jme3.math.Vector3f;
import com.jme3.network.service.rmi.Asynchronous;
import com.simsilica.es.EntityId;
import java.util.Collection;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.listener.PlayerListener;
import toniarts.openkeeper.tools.convert.map.TriggerAction;

/**
 * The game callbacks the server sends to the client
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface GameSessionListener extends MapListener, PlayerListener {

    /**
     * Client should start to load the game data up visually
     *
     * @param players the players
     */
    @Asynchronous
    public void onGameDataLoaded(Collection<Keeper> players);

    /**
     * Signal that a player is ready and loaded up
     *
     * @param keeperId the keeper ID of the player
     */
    @Asynchronous
    public void onLoadComplete(short keeperId);

    /**
     * Game loading status update from a client
     *
     * @param progress current progress of a player
     * @param keeperId the keeper ID of the player
     */
    @Asynchronous
    public void onLoadStatusUpdate(float progress, short keeperId);

    /**
     * Client should start the visuals
     */
    @Asynchronous
    public void onGameStarted();

    /**
     * The game has been paused
     */
    @Asynchronous
    public void onGamePaused();

    /**
     * The game has been resumed
     */
    @Asynchronous
    public void onGameResumed();

    /**
     * The client should set widescreen mode
     *
     * @param enable on/off
     */
    @Asynchronous
    public void onSetWidescreen(boolean enable);

    /**
     * The client should play a speech
     *
     * @param speechId     speech ID
     * @param showText     show subtitles
     * @param introduction introduction
     * @param pathId       camera path ID
     */
    @Asynchronous
    public void onPlaySpeech(int speechId, boolean showText, boolean introduction, int pathId);

    /**
     * The client should do a transition
     *
     * @param pathId the camera path ID
     * @param start  the starting coordinates
     */
    @Asynchronous
    public void onDoTransition(short pathId, Vector3f start);

    @Asynchronous
    public void onFlashButton(TriggerAction.MakeType buttonType, short targetId, TriggerAction.ButtonType targetButtonType, boolean enabled, int time);

    @Asynchronous
    public void onRotateViewAroundPoint(Vector3f point, boolean relative, int angle, int time);

    @Asynchronous
    public void onShowMessage(int textId);

    @Asynchronous
    public void onZoomViewToPoint(Vector3f point);

    @Asynchronous
    public void onZoomViewToEntity(EntityId entityId);

    @Asynchronous
    public void onShowUnitFlower(EntityId entityId, int interval);

}
