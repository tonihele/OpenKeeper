/*
 * Copyright (C) 2014-2020 OpenKeeper
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
package toniarts.openkeeper.tools.convert.conversion.graph;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A simple graph node with incoming and outgoing connections (edges)
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @param <T> The node type
 */
public class Node<T extends Node> {

    private final int id;
    private final Set<T> incomingEdges = new LinkedHashSet<>();
    private final Set<T> outgoingEdges = new LinkedHashSet<>();

    public Node(int id) {
        this.id = id;
    }

    /**
     * Adds the node as an incoming edge to the node
     *
     * @param node node to add
     */
    public void addIncomingNode(final T node) {
        incomingEdges.add(node);
    }

    /**
     * Adds the node as an outgoing edge to the node
     *
     * @param node node to add
     */
    public void addOutgoingNode(final T node) {
        outgoingEdges.add(node);
    }

    /**
     * Get a readonly copy of the incoming edges
     *
     * @return incoming edges
     */
    public Set<T> getIncomingNodes() {
        return Collections.unmodifiableSet(incomingEdges);
    }

    /**
     * Get a readonly copy of the outgoing edges
     *
     * @return outgoing edges
     */
    public Set<T> getOutgoingNodes() {
        return Collections.unmodifiableSet(outgoingEdges);
    }

    /**
     * Does the node have any incoming edges, kinda is this a root node
     *
     * @return has any incoming nodes
     */
    public boolean hasIncomingNodes() {
        return !incomingEdges.isEmpty();
    }

    /**
     * Does the node have any outgoing edges, children
     *
     * @return has any outgoing nodes
     */
    public boolean hasOutgoingNodes() {
        return !outgoingEdges.isEmpty();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Node<?> other = (Node<?>) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

}
