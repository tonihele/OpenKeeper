/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.game.state.loading;

import com.jme3.app.state.AppState;

/**
 * Simple interface which allows to notify about loading progress of a player
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IPlayerLoadingProgress extends AppState {

    /**
     * Notify about loading progress of a player
     *
     * @param progress loading progress
     * @param playerId the player ID of which progress this is
     */
    public void setProgress(final float progress, final short playerId);

}
