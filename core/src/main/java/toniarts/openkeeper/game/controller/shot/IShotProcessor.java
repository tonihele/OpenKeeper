/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.controller.shot;

/**
 * Applies what one kind of shot does to the world. The shot data says what a
 * shot does through its {@link toniarts.openkeeper.tools.convert.map.Shot.ProcessType},
 * so there is one processor per process type, and adding a new spell is
 * mostly adding a processor for its process type.
 */
@FunctionalInterface
public interface IShotProcessor {

    /**
     * Fire the shot
     *
     * @param request the shot and where, and at whom, it was fired
     */
    void process(ShotRequest request);

}
