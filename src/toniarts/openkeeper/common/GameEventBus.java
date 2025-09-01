/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.common;

import com.google.common.eventbus.EventBus;

/**
 *
 * @author ArchDemon
 */
public class GameEventBus {

    private static final GameEventBus instance = new GameEventBus();

    private final EventBus eventBus = new EventBus();

    public static GameEventBus getInstance() {
        return instance;
    }

    protected GameEventBus() {
    }

    public void addListener(Object listener) {
        eventBus.register(listener);
    }

    public void removeListener(Object listener) {
        eventBus.unregister(listener);
    }

    public void publish(Object event) {
        eventBus.post(event);
    }
}
