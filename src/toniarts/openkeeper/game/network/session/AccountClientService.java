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

import com.jme3.network.service.AbstractClientService;
import com.jme3.network.service.ClientServiceManager;
import com.jme3.network.service.rmi.RmiClientService;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import toniarts.openkeeper.game.network.NetworkConstants;

/**
 * Provides super-basic account services like logging in. This could be expanded
 * to be more complicated based on a real game's needs. The basics have been
 * included here as a minimal example that includes the basic types of
 * communication necessary.
 *
 * @author Paul Speed
 */
public class AccountClientService extends AbstractClientService
        implements AccountSession {

    private static final Logger logger = System.getLogger(AccountClientService.class.getName());

    private RmiClientService rmiService;
    private AccountSession delegate;

    private final AccountSessionCallback sessionCallback = new AccountSessionCallback();
    private final List<AccountSessionListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public String getServerInfo() {
        return delegate.getServerInfo();
    }

    @Override
    public void login(String playerName, int systemMemory) {
        delegate.login(playerName, systemMemory);
    }

    /**
     * Adds a listener that will be notified about account-related events. Note
     * that these listeners are called on the networking thread and as such are
     * not suitable for modifying the visualization directly.
     */
    public void addAccountSessionListener(AccountSessionListener l) {
        listeners.add(l);
    }

    public void removeAccountSessionListener(AccountSessionListener l) {
        listeners.remove(l);
    }

    @Override
    protected void onInitialize(ClientServiceManager s) {
        logger.log(Level.TRACE, "onInitialize({0})", s);
        this.rmiService = getService(RmiClientService.class);
        if (rmiService == null) {
            throw new RuntimeException("AccountClientService requires RMI service");
        }
        logger.log(Level.TRACE, "Sharing session callback.");
        rmiService.share(NetworkConstants.LOBBY_CHANNEL, sessionCallback, AccountSessionListener.class);
    }

    /**
     * Called during connection setup once the server-side services have been
     * initialized for this connection and any shared objects, etc. should be
     * available.
     */
    @Override
    public void start() {
        logger.log(Level.TRACE, "start()");
        super.start();
        this.delegate = rmiService.getRemoteObject(AccountSession.class);
        logger.log(Level.TRACE, "delegate:{0}", delegate);
        if (delegate == null) {
            throw new RuntimeException("No account session found during connection setup");
        }
    }

    /**
     * Shared with the server over RMI so that it can notify us about account
     * related stuff.
     */
    private class AccountSessionCallback implements AccountSessionListener {

        @Override
        public void notifyLoginStatus(boolean loggedIn) {
            logger.log(Level.TRACE, "notifyLoginStatus({0})", loggedIn);
            for (AccountSessionListener l : listeners) {
                l.notifyLoginStatus(loggedIn);
            }
        }

    }
}
