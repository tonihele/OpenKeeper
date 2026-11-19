/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.state.loop;

import toniarts.openkeeper.game.controller.IGameController;

/** Prepared simulation owned by one server state; retirement may wait and belongs off the render thread. */
public interface IGameLoopManager {
    void start();
    void pause();
    void resume();
    void stop();
    IGameController getGameController();
}
