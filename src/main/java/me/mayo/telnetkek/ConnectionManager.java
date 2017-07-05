/* 
 * Copyright (C) 2012-2017 Steven Lawson
 *
 * This file is part of FreedomTelnetClient.
 *
 * FreedomTelnetClient is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.mayo.telnetkek;

import me.mayo.telnetkek.player.PlayerListDecoder;
import me.mayo.telnetkek.player.PlayerInfo;
import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Timer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.net.telnet.TelnetClient;

public class ConnectionManager
{

    private static final Pattern LOGIN_MESSAGE = Pattern.compile("\\[.+?@BukkitTelnet\\]\\$ Logged in as (.+)\\.");

    private final TelnetClient telnetClient = new TelnetClient();
    private Thread connectThread;
    private String hostname;
    private int port;
    private boolean canDoDisconnect = false;
    private String loginName;

    public ConnectionManager()
    {
    }

    public void triggerConnect(final String hostname, final int port)
    {
        final MainPanel btc = TelnetKek.mainPanel;

        btc.getBtnConnect().setEnabled(false);
        btc.getTxtServer().setEnabled(false);
        btc.getBtnDisconnect().setEnabled(true);

        btc.writeToConsole(new ConsoleMessage("Connecting to " + hostname + ":" + port + "", Color.GREEN));

        this.hostname = hostname;
        this.port = port;
        this.loginName = null;
        updateTitle(true);

        startConnectThread();
    }

    public void triggerConnect(final String hostnameAndPort)
    {
        final String[] parts = StringUtils.split(hostnameAndPort, ":");

        if (parts.length <= 1)
        {
            this.triggerConnect(parts[0], 23);
        }
        else
        {
            int _port = 23;

            try
            {
                _port = Integer.parseInt(parts[1]);
            }
            catch (NumberFormatException ex)
            {
            }

            this.triggerConnect(parts[0], _port);
        }
    }

    public void triggerDisconnect()
    {
        if (this.canDoDisconnect)
        {
            this.canDoDisconnect = false;

            try
            {
                this.telnetClient.disconnect();
            }
            catch (IOException ex)
            {
                TelnetKek.LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    public void finishDisconnect()
    {
        final MainPanel btc = TelnetKek.mainPanel;

        btc.getBtnConnect().setEnabled(true);
        btc.getTxtServer().setEnabled(true);
        btc.getBtnDisconnect().setEnabled(false);
        btc.getBtnSend().setEnabled(false);
        btc.getTxtCommand().setEnabled(false);

        loginName = null;

        updateTitle(false);

        btc.writeToConsole(new ConsoleMessage("Disconnected.", Color.RED));
    }

    public void sendCommand(final String text)
    {
        sendCommand(text, true);
    }

    public void sendCommand(final String text, final boolean verbose)
    {
        try
        {
            if (verbose)
            {
                TelnetKek.mainPanel.writeToConsole(new ConsoleMessage(":" + text));
            }

            final OutputStream out = this.telnetClient.getOutputStream();
            if (out == null)
            {
                return;
            }

            this.telnetClient.getOutputStream().write((text + "\r\n").getBytes(StandardCharsets.UTF_8));
            this.telnetClient.getOutputStream().flush();
        }
        catch (IOException ex)
        {
            TelnetKek.LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public void sendDelayedCommand(final String text, final boolean verbose, final int delay)
    {
        final Timer timer = new Timer(delay, event -> sendCommand(text, verbose));
        timer.setRepeats(false);
        timer.start();
    }

    private void startConnectThread()
    {
        if (this.connectThread != null)
        {
            return;
        }

        this.connectThread = new Thread(()
                -> 
                {
                    final MainPanel btc = TelnetKek.mainPanel;

                    try
                    {
                        ConnectionManager.this.telnetClient.connect(hostname, port);
                        ConnectionManager.this.canDoDisconnect = true;

                        btc.getBtnSend().setEnabled(true);
                        btc.getTxtCommand().setEnabled(true);
                        btc.getTxtCommand().requestFocusInWindow();

                        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(telnetClient.getInputStream())))
                        {
                            String line;
                            while ((line = reader.readLine()) != null)
                            {
                                String _loginName = null;
                                if (ConnectionManager.this.loginName == null)
                                {
                                    _loginName = checkForLoginMessage(line);
                                }
                                if (_loginName != null)
                                {
                                    ConnectionManager.this.loginName = _loginName;
                                    updateTitle(true);
                                    sendDelayedCommand("telnet.enhanced", false, 100);
                                }
                                else
                                {
                                    final PlayerInfo selectedPlayer = btc.getSelectedPlayer();
                                    String selectedPlayerName = null;
                                    if (selectedPlayer != null)
                                    {
                                        selectedPlayerName = selectedPlayer.getName();
                                    }

                                    if (PlayerListDecoder.checkForPlayerListMessage(line, btc.getPlayerList()))
                                    {
                                        btc.updatePlayerList(selectedPlayerName);
                                    }
                                    else
                                    {
                                        final TelnetMessage message = new TelnetMessage(line);
                                        if (!message.skip())
                                        {
                                            btc.writeToConsole(message);
                                        }
                                    }
                                }
                            }
                        }

                        triggerDisconnect();
                    }
                    catch (IOException ex)
                    {
                        btc.writeToConsole(new ConsoleMessage(ex.getMessage() + SystemUtils.LINE_SEPARATOR + ExceptionUtils.getStackTrace(ex)));
                    }

                    finishDisconnect();

                    ConnectionManager.this.connectThread = null;
        });
        this.connectThread.start();
    }

    public static final String checkForLoginMessage(String message)
    {
        final Matcher matcher = LOGIN_MESSAGE.matcher(message);
        if (matcher.find())
        {
            return matcher.group(1);
        }

        return null;
    }

    public final void updateTitle(final boolean isConnected)
    {
        final MainPanel mainPanel = TelnetKek.mainPanel;
        if (mainPanel == null)
        {
            return;
        }

        String title;

        if (isConnected)
        {
            if (loginName == null)
            {
                title = String.format("TelnetKek - %s - %s:%d", TelnetKek.VERSION_STRING, hostname, port);
            }
            else
            {
                title = String.format("TelnetKek - %s - %s@%s:%d", TelnetKek.VERSION_STRING, loginName, hostname, port);
            }
        }
        else
        {
            title = String.format("TelnetKek - %s - Disconnected", TelnetKek.VERSION_STRING);
        }

        mainPanel.setTitle(title);
    }
}
