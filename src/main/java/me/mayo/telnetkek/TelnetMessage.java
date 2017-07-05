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

import java.awt.Color;
import java.util.regex.Pattern;

public class TelnetMessage extends ConsoleMessage
{

    private static final String PATTERN_PREFIX = "^:\\[.+? INFO\\]: ";
    private static final Color PURPLE = new Color(128, 0, 128);
    private static final Color DARK_GREEN = new Color(86, 130, 3);

    private static final Pattern ERROR_MESSAGE = Pattern.compile("^:\\[.+? (?:(WARN)|(ERROR))\\]: ");
    private static final Pattern INFO_MESSAGE = Pattern.compile(PATTERN_PREFIX);

    private final LogMessageType messageType;

    public TelnetMessage(String message)
    {
        super(message);
        this.messageType = LogMessageType.getMessageType(message);
    }

    public LogMessageType getMessageType()
    {
        return this.messageType;
    }

    public boolean isErrorMessage()
    {
        return ERROR_MESSAGE.matcher(this.getMessage()).find();
    }

    public boolean isInfoMessage()
    {
        return INFO_MESSAGE.matcher(this.getMessage()).find();
    }

    private boolean isType(final LogMessageType checkType)
    {
        return this.messageType != null ? this.messageType == checkType : false;
    }

    public boolean skip()
    {
        final MainPanel mainPanel = TelnetKek.mainPanel;

        if (mainPanel == null)
        {
            return false;
        }

        if (mainPanel.getChkShowChatOnly().isSelected())
        {
            return !isType(LogMessageType.CHAT_MESSAGE)
                    && !isType(LogMessageType.CSAY_MESSAGE)
                    && !isType(LogMessageType.SAY_MESSAGE)
                    && !isType(LogMessageType.SA_ADMIN)
                    && !isType(LogMessageType.STA_ADMIN)
                    && !isType(LogMessageType.SRA_ADMIN)
                    && !isType(LogMessageType.DEV_ADMIN)
                    && !isType(LogMessageType.OWNER_ADMIN)
                    && !isType(LogMessageType.FOUNDER_ADMIN)
                    && !isType(LogMessageType.CONSOLE_ADMIN);
        }

        if (mainPanel.getChkIgnoreServerCommands().isSelected() && isType(LogMessageType.ISSUED_SERVER_COMMAND))
        {
            return true;
        }

        if (mainPanel.getChkIgnorePlayerCommands().isSelected() && isType(LogMessageType.PLAYER_COMMAND))
        {
            return true;
        }

        if (mainPanel.getChkIgnoreErrors().isSelected())
        {
            if (!isType(LogMessageType.CHAT_MESSAGE)
                    && !isType(LogMessageType.CSAY_MESSAGE)
                    && !isType(LogMessageType.SAY_MESSAGE)
                    && !isType(LogMessageType.SA_ADMIN)
                    && !isType(LogMessageType.STA_ADMIN)
                    && !isType(LogMessageType.SRA_ADMIN)
                    && !isType(LogMessageType.DEV_ADMIN)
                    && !isType(LogMessageType.OWNER_ADMIN)
                    && !isType(LogMessageType.FOUNDER_ADMIN)
                    && !isType(LogMessageType.CONSOLE_ADMIN))
            {
                return false;
            }
        }

        return false;
    }

    @Override
    public Color getColor()
    {
        if (this.messageType == null)
        {
            return super.getColor();
        }
        else
        {
            return this.messageType.getColor();
        }
    }

    public static enum LogMessageType
    {
        CHAT_MESSAGE(PATTERN_PREFIX + "\\<", Color.BLUE),
        SAY_MESSAGE(PATTERN_PREFIX + "\\[Server:", Color.BLUE),
        CSAY_MESSAGE(PATTERN_PREFIX + "\\[CONSOLE\\]<", Color.BLUE),
        //
        SA_ADMIN(PATTERN_PREFIX + "\\[TotalFreedomMod\\] \\[ADMIN\\] .+? \\[SA\\]: ", Color.CYAN),
        STA_ADMIN(PATTERN_PREFIX + "\\[TotalFreedomMod\\] \\[ADMIN\\] .+? \\[STA\\]: ", DARK_GREEN),
        SRA_ADMIN(PATTERN_PREFIX + "\\[TotalFreedomMod\\] \\[ADMIN\\] .+? \\[SrA\\]: ", Color.ORANGE),
        DEV_ADMIN(PATTERN_PREFIX + "\\[TotalFreedomMod\\] \\[ADMIN\\] .+? \\[Dev\\]: ", PURPLE),
        OWNER_ADMIN(PATTERN_PREFIX + "\\[TotalFreedomMod\\] \\[ADMIN\\] .+? \\[Owner\\]: ", Color.MAGENTA),
        FOUNDER_ADMIN(PATTERN_PREFIX + "\\[TotalFreedomMod\\] \\[ADMIN\\] .+? \\[Founder\\]: ", Color.MAGENTA),
        CONSOLE_ADMIN(PATTERN_PREFIX + "\\[TotalFreedomMod\\] \\[ADMIN\\] .+? \\[Console\\]: ", PURPLE),
        //
        WORLD_EDIT(PATTERN_PREFIX + "WorldEdit: ", Color.RED),
        //
        PREPROCESS_COMMAND(PATTERN_PREFIX + "\\[PREPROCESS_COMMAND\\] ", DARK_GREEN),
        //
        ISSUED_SERVER_COMMAND(PATTERN_PREFIX + ".+? issued server command: "),
        PLAYER_COMMAND(PATTERN_PREFIX + "\\[PLAYER_COMMAND\\] "),
        //
        SA_JOIN_MSG(PATTERN_PREFIX + ".+? is a Super Admin", Color.CYAN),
        SA_JOIN_MSG2(PATTERN_PREFIX + ".+? is a Super Admin ", Color.CYAN),
        STA_JOIN_MSG(PATTERN_PREFIX + ".+? is a Super Telnet Admin", DARK_GREEN),
        STA_JOIN_MSG2(PATTERN_PREFIX + ".+? is a Super Telnet Admin ", DARK_GREEN),
        STA_JOIN_MSG3(PATTERN_PREFIX + ".+? is a Telnet Admin", DARK_GREEN),
        STA_JOIN_MSG4(PATTERN_PREFIX + ".+? is a Telnet Admin ", DARK_GREEN),
        TCA_JOIN_MSG(PATTERN_PREFIX + ".+? is a Telnet Clan Admin", Color.GREEN),
        TCA_JOIN_MSG2(PATTERN_PREFIX + ".+? is a Telnet Clan Admin ", Color.GREEN),
        TCA_JOIN_MSG3(PATTERN_PREFIX + ".+? is a Telnet Clan. Admin", Color.GREEN),
        TCA_JOIN_MSG4(PATTERN_PREFIX + ".+? is a Telnet Clan. Admin ", Color.GREEN),
        SRA_JOIN_MSG(PATTERN_PREFIX + ".+? is a Senior Admin", Color.ORANGE),
        SRA_JOIN_MSG2(PATTERN_PREFIX + ".+? is a Senior Admin ", Color.ORANGE);

        private final Pattern messagePattern;
        private final Color color;

        private LogMessageType(final String messagePatternStr)
        {
            this.messagePattern = Pattern.compile(messagePatternStr);
            this.color = Color.BLACK;
        }

        private LogMessageType(final String messagePatternStr, final Color color)
        {
            this.messagePattern = Pattern.compile(messagePatternStr);
            this.color = color;
        }

        public Pattern getMessagePattern()
        {
            return this.messagePattern;
        }

        public Color getColor()
        {
            return this.color;
        }

        public static LogMessageType getMessageType(final String message)
        {
            for (final LogMessageType type : values())
            {
                if (type.getMessagePattern().matcher(message).find())
                {
                    return type;
                }
            }
            return null;
        }
    }
}
