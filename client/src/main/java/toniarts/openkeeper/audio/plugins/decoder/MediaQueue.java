/* Copyright (C) 2003-2014 Michael Scheerer. All Rights Reserved. */

/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package toniarts.openkeeper.audio.plugins.decoder;

import java.util.*;

/**
 * The
 * <code>MediaQueue</code> class contains and administrates media data packets.
 *
 * @author Michael Scheerer
 */
public final class MediaQueue {

    private Object[] elementData;
    private int elementCount;

    /**
     * Constructor.
     */
    public MediaQueue(int initialCapacity) {
        this.elementData = new Object[initialCapacity];
    }

    /**
     * Constructor.
     */
    public MediaQueue() {
        this(100);
    }

    private void update(int minCapacity) {
        int oldCapacity = elementData.length;
        Object oldData[] = elementData;
        int newCapacity = oldCapacity << 1;

        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }

        elementData = new Object[newCapacity];
        System.arraycopy(oldData, 0, elementData, 0, elementCount);
    }

    /**
     * Returns the number of components in this vector.
     *
     * @return the number of components in this vector.
     */
    public int size() {
        return elementCount;
    }

    /**
     * Tests if this vector has no components.
     *
     * @return  <code>true</code> if this vector has no components;
     * <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        return elementCount == 0;
    }

    /**
     * Searches for the first occurence of the given argument, testing for
     * equality using the
     * <code>equals</code> method.
     *
     * @param elem an object.
     * @return the index of the first occurrence of the argument in this vector;
     * returns <code>-1</code> if the object is not found.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public int indexOf(Object elem) {
        return indexOf(elem, 0);
    }

    /**
     * Searches for the first occurence of the given argument, beginning the
     * search at
     * <code>index</code>, and testing for equality using the
     * <code>equals</code> method.
     *
     * @param elem an object.
     * @param index the index to start searching from.
     * @return the index of the first occurrence of the object argument in this
     * vector at position <code>index</code> or later in the vector;
     * returns <code>-1</code> if the object is not found.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public int indexOf(Object elem, int index) {
        for (int i = index; i < elementCount; i++) {
            if (elem.equals(elementData[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the first component of this vector.
     *
     * @return the first component of this vector.
     * @exception NoSuchElementException if this vector has no components.
     */
    public Object firstElement() {
        if (elementCount == 0) {
            throw new NoSuchElementException();
        }
        return elementData[0];
    }

    /**
     * Returns the last component of this vector.
     *
     * @return the last component of this vector.
     * @exception NoSuchElementException if this vector has no components.
     */
    public Object lastElement() {
        if (elementCount == 0) {
            throw new NoSuchElementException();
        }
        return elementData[elementCount - 1];
    }

    /**
     * Deletes the component at the specified index. Each component in this
     * vector with an index greater or equal to the specified
     * <code>index</code> is shifted downward to have an index one smaller than
     * the value it had previously.
     * <p>
     *
     * The index must be a value greater than or equal to
     * <code>0</code> and less than the current size of the vector.
     *
     * @param index the index of the object to remove.
     * @exception ArrayIndexOutOfBoundsException if the index was invalid.
     * @see java.util.Vector#size()
     */
    public void removeElementAt(int index) {
        if (index >= elementCount) {
            throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
        } else if (index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        int j = elementCount - index - 1;

        if (j > 0) {
            System.arraycopy(elementData, index + 1, elementData, index, j);
        }
        elementCount--;
        elementData[elementCount] = null;
    }

    /**
     * Gets the component at the specified index.
     * <p>
     *
     * The index must be a value greater than or equal to
     * <code>0</code> and less than the current size of the vector.
     *
     * @param index the index of the object to remove.
     * @return the current component of this vector.
     * @exception ArrayIndexOutOfBoundsException if the index was invalid.
     * @see java.util.Vector#size()
     */
    public Object getElementAt(int index) {
        if (index >= elementCount) {
            throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
        } else if (index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return elementData[index];
    }

    /**
     * Adds the specified component to the end of this vector, increasing its
     * size by one. The capacity of this vector is increased if its size becomes
     * greater than its capacity.
     *
     * @param obj the component to be added.
     */
    public void addElement(Object obj) {
        int newcount = elementCount + 1;

        if (newcount > elementData.length) {
            update(newcount);
        }
        elementData[elementCount++] = obj;
    }

    /**
     * Removes the first occurrence of the argument from this vector. If the
     * object is found in this vector, each component in the vector with an
     * index greater or equal to the object's index is shifted downward to have
     * an index one smaller than the value it had previously.
     *
     * @param obj the component to be removed.
     * @return  <code>true</code> if the argument was a component of this        vector; <code>false</code> otherwise.
     */
    public boolean removeElement(Object obj) {
        int i = indexOf(obj);

        if (i >= 0) {
            removeElementAt(i);
            return true;
        }
        return false;
    }

    /**
     * Removes all components from this vector and sets its size to zero.
     */
    public void removeAllElements() {
        for (int i = 0; i < elementCount; i++) {
            elementData[i] = null;
        }
        elementCount = 0;
    }

    /**
     * Frees all system resources, which are bounded to this object.
     */
    public void close() {
        for (int i = 0; i < elementCount; i++) {
            elementData[i] = null;
        }
        if (elementData != null) {
            elementData = null;
        }
    }
}
