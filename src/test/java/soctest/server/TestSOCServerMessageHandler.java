/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2026 Ricky Sparks
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The maintainer of this program can be reached at jsettlers@nand.net
 **/
package soctest.server;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

import soc.message.SOCJoinGame;
import soc.message.SOCLeaveGame;
import soc.message.SOCMessage;
import soc.message.SOCNewGameWithOptionsRequest;
import soc.message.SOCSitDown;
import soc.message.SOCVersion;
import soc.server.SOCServerMessageHandler;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link SOCServerMessageHandler} covering message dispatch routing,
 * join-game message parsing round-trips, and the savegame filename regex.
 * <P>
 * These tests verify the message layer that the handler depends on, without
 * needing a running {@code SOCServer} instance. They ensure that if message
 * parsing or type constants change, the breakage is caught early.
 *
 * @since 2.7.00
 */
public class TestSOCServerMessageHandler
{

    // ---- Savegame filename regex (used by *LOADGAME* / *SAVEGAME* debug commands) ----

    @Test
    public void testSavegameFilenameRegex_validNames()
        throws Exception
    {
        Pattern regex = getSavegameRegex();
        assertTrue("letters only", regex.matcher("mygame").matches());
        assertTrue("digits only", regex.matcher("12345").matches());
        assertTrue("mixed alphanumeric", regex.matcher("save01game").matches());
        assertTrue("with dashes", regex.matcher("my-saved-game").matches());
        assertTrue("with underscores", regex.matcher("my_saved_game").matches());
        assertTrue("mixed dash underscore", regex.matcher("game-2_final").matches());
    }

    @Test
    public void testSavegameFilenameRegex_invalidNames()
        throws Exception
    {
        Pattern regex = getSavegameRegex();
        assertFalse("empty string", regex.matcher("").matches());
        assertFalse("contains space", regex.matcher("my game").matches());
        assertFalse("contains dot", regex.matcher("save.txt").matches());
        assertFalse("contains slash", regex.matcher("path/file").matches());
        assertFalse("contains special char", regex.matcher("game@home").matches());
        assertFalse("contains semicolon", regex.matcher("game;drop").matches());
    }

    // ---- SOCJoinGame message round-trip parsing ----

    @Test
    public void testJoinGameMessage_roundTrip()
    {
        SOCJoinGame orig = new SOCJoinGame("testuser", "pass123", "-", "mygame");
        String cmd = orig.toCmd();
        assertNotNull("toCmd should not be null", cmd);

        SOCMessage parsed = SOCMessage.toMsg(cmd);
        assertNotNull("parsed message should not be null", parsed);
        assertEquals("parsed type should be JOINGAME",
            SOCMessage.JOINGAME, parsed.getType());

        SOCJoinGame parsedJG = (SOCJoinGame) parsed;
        assertEquals("testuser", parsedJG.getNickname());
        assertEquals("mygame", parsedJG.getGame());
    }

    @Test
    public void testJoinGameMessage_emptyPassword()
    {
        SOCJoinGame orig = new SOCJoinGame("player1", "", "-", "lobby");
        String cmd = orig.toCmd();
        SOCMessage parsed = SOCMessage.toMsg(cmd);

        assertNotNull(parsed);
        assertTrue("should parse as SOCJoinGame", parsed instanceof SOCJoinGame);
        SOCJoinGame jg = (SOCJoinGame) parsed;
        assertEquals("player1", jg.getNickname());
        assertEquals("lobby", jg.getGame());
        assertEquals("empty password should be preserved as empty string",
            "", jg.getPassword());
    }

    @Test
    public void testJoinGameMessage_parseDataStr_garbled()
    {
        SOCJoinGame result = SOCJoinGame.parseDataStr("garbled");
        assertNull("garbled data should return null", result);
    }

    // ---- SOCVersion message round-trip ----

    @Test
    public void testVersionMessage_roundTrip()
    {
        SOCVersion orig = new SOCVersion(2700, "2.7.00", "JM20260101", null, null);
        String cmd = orig.toCmd();
        assertNotNull(cmd);

        SOCMessage parsed = SOCMessage.toMsg(cmd);
        assertNotNull("version message should parse", parsed);
        assertEquals(SOCMessage.VERSION, parsed.getType());

        SOCVersion pv = (SOCVersion) parsed;
        assertEquals(2700, pv.getVersionNumber());
        assertEquals("2.7.00", pv.getVersionString());
    }

    // ---- SOCLeaveGame message round-trip ----

    @Test
    public void testLeaveGameMessage_roundTrip()
    {
        SOCLeaveGame orig = new SOCLeaveGame("quitter", "-", "testgame");
        String cmd = orig.toCmd();
        assertNotNull(cmd);

        SOCMessage parsed = SOCMessage.toMsg(cmd);
        assertNotNull(parsed);
        assertEquals(SOCMessage.LEAVEGAME, parsed.getType());

        SOCLeaveGame plg = (SOCLeaveGame) parsed;
        assertEquals("quitter", plg.getNickname());
        assertEquals("testgame", plg.getGame());
    }

    // ---- SOCSitDown message round-trip ----

    @Test
    public void testSitDownMessage_roundTrip()
    {
        SOCSitDown orig = new SOCSitDown("mygame", "player1", 2, false);
        String cmd = orig.toCmd();
        assertNotNull(cmd);

        SOCMessage parsed = SOCMessage.toMsg(cmd);
        assertNotNull(parsed);
        assertEquals(SOCMessage.SITDOWN, parsed.getType());

        SOCSitDown psd = (SOCSitDown) parsed;
        assertEquals("mygame", psd.getGame());
        assertEquals("player1", psd.getNickname());
        assertEquals(2, psd.getPlayerNumber());
        assertFalse(psd.isRobot());
    }

    @Test
    public void testSitDownMessage_robotFlag()
    {
        SOCSitDown orig = new SOCSitDown("mygame", "robot1", 0, true);
        String cmd = orig.toCmd();

        SOCMessage parsed = SOCMessage.toMsg(cmd);
        assertNotNull(parsed);
        SOCSitDown psd = (SOCSitDown) parsed;
        assertTrue("robot flag should be preserved", psd.isRobot());
    }

    // ---- Message type constants sanity ----

    @Test
    public void testMessageTypeConstants_distinct()
    {
        assertNotEquals("JOINGAME != VERSION",
            SOCMessage.JOINGAME, SOCMessage.VERSION);
        assertNotEquals("JOINGAME != LEAVEGAME",
            SOCMessage.JOINGAME, SOCMessage.LEAVEGAME);
        assertNotEquals("JOINGAME != SITDOWN",
            SOCMessage.JOINGAME, SOCMessage.SITDOWN);
        assertNotEquals("SERVERPING != VERSION",
            SOCMessage.SERVERPING, SOCMessage.VERSION);
    }

    @Test
    public void testMessageTypeConstants_joingameValue()
    {
        assertEquals("JOINGAME type constant", 1013, SOCMessage.JOINGAME);
    }

    @Test
    public void testMessageTypeConstants_versionValue()
    {
        assertEquals("VERSION type constant", 9998, SOCMessage.VERSION);
    }

    // ---- null/malformed message handling ----

    @Test
    public void testToMsg_nullInput()
    {
        // SOCMessage.toMsg should handle garbled input gracefully
        SOCMessage result = SOCMessage.toMsg("not_a_valid_message");
        assertNull("completely garbled input should return null", result);
    }

    @Test
    public void testToMsg_emptyString()
    {
        SOCMessage result = SOCMessage.toMsg("");
        assertNull("empty string should return null", result);
    }

    // ---- helper ----

    /**
     * Access the protected static {@code DEBUG_COMMAND_SAVEGAME_FILENAME_REGEX}
     * pattern via reflection so we can test it directly.
     */
    private static Pattern getSavegameRegex()
        throws Exception
    {
        Field f = SOCServerMessageHandler.class.getDeclaredField(
            "DEBUG_COMMAND_SAVEGAME_FILENAME_REGEX");
        f.setAccessible(true);
        return (Pattern) f.get(null);
    }

    public static void main(String[] args)
    {
        org.junit.runner.JUnitCore.main("soctest.server.TestSOCServerMessageHandler");
    }

}
