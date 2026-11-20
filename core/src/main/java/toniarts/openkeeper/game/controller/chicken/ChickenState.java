/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.game.controller.chicken;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;

/**
 * State machine for chicken AI
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public enum ChickenState implements State<IChickenController> {

    HATCHING_START() {
        @Override
        public void enter(IChickenController entity) {

        }

        @Override
        public void update(IChickenController entity) {
            if (entity.isStateTimeExceeded()) {
                entity.getStateMachine().changeState(HATCHING_END);
            }
        }

        @Override
        public void exit(IChickenController entity) {

        }

        @Override
        public boolean onMessage(IChickenController entity, Telegram telegram) {
            return true;
        }

    },
    HATCHING_END() {
        @Override
        public void enter(IChickenController entity) {

        }

        @Override
        public void update(IChickenController entity) {
            if (entity.isStateTimeExceeded()) {
                entity.getStateMachine().changeState(WANDERING);
            }
        }

        @Override
        public void exit(IChickenController entity) {
            entity.growIntoChicken();
        }

        @Override
        public boolean onMessage(IChickenController entity, Telegram telegram) {
            return true;
        }

    },
    WANDERING() {
        @Override
        public void enter(IChickenController entity) {
            entity.navigateToRandomPoint();
        }

        @Override
        public void update(IChickenController entity) {
            if (entity.isStopped()) {
                entity.getStateMachine().changeState(PECKING);
            }
        }

        @Override
        public void exit(IChickenController entity) {

        }

        @Override
        public boolean onMessage(IChickenController entity, Telegram telegram) {
            return true;
        }

    },
    PECKING() {
        @Override
        public void enter(IChickenController entity) {

        }

        @Override
        public void update(IChickenController entity) {
            if (entity.isStateTimeExceeded()) {
                entity.getStateMachine().changeState(WANDERING);
            }
        }

        @Override
        public void exit(IChickenController entity) {

        }

        @Override
        public boolean onMessage(IChickenController entity, Telegram telegram) {
            return true;
        }

    }
}
