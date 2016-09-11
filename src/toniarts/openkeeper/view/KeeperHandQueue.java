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
package toniarts.openkeeper.view;

import javax.annotation.Nonnull;
import toniarts.openkeeper.world.control.IInteractiveControl;

/**
 * Fixed size keeper hand LIFO queue (a.k.a. stack), optimized for performance
 * and simplicity :)<br>
 * <br>
 * Derived from:<br>
 * <a href="http://stackoverflow.com/questions/15840443/java-stack-with-elements-limit">http://stackoverflow.com/questions/15840443/java-stack-with-elements-limit</a>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KeeperHandQueue {

    private final IInteractiveControl[] stack;
    private int count = 0;

    /**
     * Creates a new queue
     *
     * @param size the fixed size of this queue
     */
    public KeeperHandQueue(int size) {
        this.stack = new IInteractiveControl[size];
    }

    /**
     * Adds an object to the front of the queue
     *
     * @param object the object to add to the queue
     * @throws IllegalStateException if the queue is already full
     * @throws NullPointerException this queue doesn't accept {@code null}
     * elements
     */
    public void push(@Nonnull IInteractiveControl object) throws IllegalStateException {

        // Don't allow to push if full
        if (isFull()) {
            throw new IllegalStateException("Queue is already full!");
        }

        // Advance counters & set the object
        stack[count] = object;
        count++;
    }

    /**
     * Gets the last inserted object and removes it from the queue
     *
     * @return the last inserted object, or {@code null} if the queue is empty
     */
    public IInteractiveControl pop() {

        // Null if empty
        if (isEmpty()) {
            return null;
        }

        // Get the object and clear the reference
        IInteractiveControl object = stack[count - 1];
        stack[count - 1] = null;
        count--;
        return object;
    }

    /**
     * Gets the last inserted object but doesn't remove it from the queue
     *
     * @return the last inserted object, or {@code null} if the queue is empty
     */
    public IInteractiveControl peek() {
        return peek(0);
    }

    /**
     * Gets the inserted object by id from top but doesn't remove it from the queue
     *
     * @param i index of object from the top
     * @return the inserted object by id from top, or {@code null} if the queue is not long
     */
    public IInteractiveControl peek(final int i) {
        return count - 1 - i < 0 ? null : stack[count - 1 - i];
    }

    /**
     * Get the queue object count
     *
     * @return the object count
     */
    public int getCount() {
        return count;
    }

    /**
     * See if the queue is full
     *
     * @return {@code true} if the queue is full
     */
    public boolean isFull() {
        return (count == stack.length);
    }

    /**
     * See if the queue is empty
     *
     * @return {@code true} if the queue is empty
     */
    public boolean isEmpty() {
        return (count == 0);
    }

}
