/*
 * $Id$
 *
 * Copyright (c) 2016, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package toniarts.openkeeper.game.network.session;

import com.jme3.network.service.rmi.Asynchronous;

/**
 * A client's view of the account related services on the server.
 *
 * @author Paul Speed
 */
public interface AccountSession {

    /**
     * Returns information about the server. Currently this is just a
     * description. It would be better to split this into an asynchronous
     * request but this is way simpler. This could be expanded to include
     * capabilities, accepted password hashes, and so on.
     */
    public String getServerInfo();

    /**
     * Called by the client to provide the player name for this connection and
     * "login" to the game. The server will respond asynchronously with a
     * notifyLoginStatus() to the client's AccountSessionListener. Note: this
     * could have been done synchronously but synchronous calls should generally
     * be avoided when they can. a) it prevents odd logic deadlocks if one isn't
     * careful, and b) it makes user interfaces automatically more responsive
     * without having to write special background worker code. When possible, go
     * asynchronous.
     */
    @Asynchronous
    public void login(String playerName, int systemMemory);
}
