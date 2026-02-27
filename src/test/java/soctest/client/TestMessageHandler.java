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
package soctest.client;

import soc.client.MessageHandler;
import soc.game.SOCGame;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.message.SOCBankTrade;
import soc.message.SOCBuildRequest;
import soc.message.SOCDiceResult;
import soc.message.SOCGameState;
import soc.message.SOCGameTextMsg;
import soc.message.SOCMessage;
import soc.message.SOCPlayerElement;
import soc.message.SOCPlayerElement.PEType;
import soc.message.SOCTurn;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for client {@link MessageHandler} covering the message-to-model
 * flows the handler depends on. Exercises parsing round-trips for dice,
 * build, trade, game state, and player element messages so that changes
 * in the wire protocol are caught by the test suite.
 *<P>
 * We can't fully instantiate {@link MessageHandler} without a running
 * {@link soc.client.SOCPlayerClient}, so these tests focus on verifying
 * the message objects that feed into the handler stay consistent.
 *
 * @since 2.7.00
 */
public class TestMessageHandler
{
    @Test
    public void testMessageHandler_defaultConstructor()
    {
        MessageHandler mh = new MessageHandler();
        assertNotNull(mh);
        assertNull("client should be null before init()", mh.getClient());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMessageHandler_initRejectsNull()
    {
        MessageHandler mh = new MessageHandler();
        mh.init(null);
    }

    @Test
    public void testDiceResultMessage_roundTrip()
    {
        SOCDiceResult orig = new SOCDiceResult("testgame", 7);
        String cmd = orig.toCmd();
        assertNotNull(cmd);

        SOCMessage parsed = SOCMessage.toMsg(cmd);
        assertNotNull(parsed);
        assertEquals(SOCMessage.DICERESULT, parsed.getType());

        SOCDiceResult dr = (SOCDiceResult) parsed;
        assertEquals("testgame", dr.getGame());
        assertEquals(7, dr.getResult());
    }

    @Test
    public void testDiceResultMessage_variousValues()
    {
        int[] testValues = {2, 3, 6, 7, 8, 12};
        for (int val : testValues)
        {
            SOCDiceResult orig = new SOCDiceResult("game1", val);
            SOCMessage parsed = SOCMessage.toMsg(orig.toCmd());
            assertNotNull("should parse dice=" + val, parsed);
            assertEquals(val, ((SOCDiceResult) parsed).getResult());
        }
    }

    @Test
    public void testBuildRequestMessage_road()
    {
        SOCBuildRequest orig = new SOCBuildRequest("mygame", SOCPlayingPiece.ROAD);
        String cmd = orig.toCmd();
        SOCMessage parsed = SOCMessage.toMsg(cmd);

        assertNotNull(parsed);
        assertEquals(SOCMessage.BUILDREQUEST, parsed.getType());
        SOCBuildRequest br = (SOCBuildRequest) parsed;
        assertEquals("mygame", br.getGame());
        assertEquals(SOCPlayingPiece.ROAD, br.getPieceType());
    }

    @Test
    public void testBuildRequestMessage_settlement()
    {
        SOCBuildRequest orig = new SOCBuildRequest("mygame", SOCPlayingPiece.SETTLEMENT);
        SOCMessage parsed = SOCMessage.toMsg(orig.toCmd());
        assertNotNull(parsed);
        assertEquals(SOCPlayingPiece.SETTLEMENT,
            ((SOCBuildRequest) parsed).getPieceType());
    }

    @Test
    public void testBuildRequestMessage_city()
    {
        SOCBuildRequest orig = new SOCBuildRequest("mygame", SOCPlayingPiece.CITY);
        SOCMessage parsed = SOCMessage.toMsg(orig.toCmd());
        assertNotNull(parsed);
        assertEquals(SOCPlayingPiece.CITY,
            ((SOCBuildRequest) parsed).getPieceType());
    }

    @Test
    public void testBuildRequestMessage_specialBuildPhase()
    {
        SOCBuildRequest orig = new SOCBuildRequest("mygame", -1);
        SOCMessage parsed = SOCMessage.toMsg(orig.toCmd());
        assertNotNull(parsed);
        assertEquals(-1, ((SOCBuildRequest) parsed).getPieceType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildRequestMessage_rejectsInvalidType()
    {
        new SOCBuildRequest("mygame", -2);
    }

    @Test
    public void testBankTradeMessage_roundTrip()
    {
        SOCResourceSet give = new SOCResourceSet(4, 0, 0, 0, 0, 0);
        SOCResourceSet get  = new SOCResourceSet(0, 0, 1, 0, 0, 0);
        SOCBankTrade orig = new SOCBankTrade("tradegame", give, get, 2);
        String cmd = orig.toCmd();

        SOCMessage parsed = SOCMessage.toMsg(cmd);
        assertNotNull(parsed);
        assertEquals(SOCMessage.BANKTRADE, parsed.getType());

        SOCBankTrade bt = (SOCBankTrade) parsed;
        assertEquals("tradegame", bt.getGame());
        assertEquals(4, bt.getGiveSet().getAmount(SOCResourceConstants.CLAY));
        assertEquals(1, bt.getGetSet().getAmount(SOCResourceConstants.SHEEP));
    }

    @Test
    public void testGameStateMessage_roundTrip()
    {
        SOCGameState orig = new SOCGameState("stategame", SOCGame.PLAY1);
        String cmd = orig.toCmd();

        SOCMessage parsed = SOCMessage.toMsg(cmd);
        assertNotNull(parsed);
        assertEquals(SOCMessage.GAMESTATE, parsed.getType());

        SOCGameState gs = (SOCGameState) parsed;
        assertEquals("stategame", gs.getGame());
        assertEquals(SOCGame.PLAY1, gs.getState());
    }

    @Test
    public void testGameStateMessage_rollOrCard()
    {
        SOCGameState orig = new SOCGameState("g", SOCGame.ROLL_OR_CARD);
        SOCMessage parsed = SOCMessage.toMsg(orig.toCmd());
        assertNotNull(parsed);
        assertEquals(SOCGame.ROLL_OR_CARD, ((SOCGameState) parsed).getState());
    }

    @Test
    public void testTurnMessage_roundTrip()
    {
        SOCTurn orig = new SOCTurn("turngame", 2, SOCGame.PLAY1);
        String cmd = orig.toCmd();

        SOCMessage parsed = SOCMessage.toMsg(cmd);
        assertNotNull(parsed);
        assertEquals(SOCMessage.TURN, parsed.getType());

        SOCTurn t = (SOCTurn) parsed;
        assertEquals("turngame", t.getGame());
        assertEquals(2, t.getPlayerNumber());
        assertEquals(SOCGame.PLAY1, t.getGameState());
    }

    @Test
    public void testPlayerElementMessage_setWheat()
    {
        SOCPlayerElement orig = new SOCPlayerElement(
            "resgame", 1, SOCPlayerElement.SET, PEType.WHEAT, 5);
        String cmd = orig.toCmd();

        SOCMessage parsed = SOCMessage.toMsg(cmd);
        assertNotNull(parsed);
        assertEquals(SOCMessage.PLAYERELEMENT, parsed.getType());

        SOCPlayerElement pe = (SOCPlayerElement) parsed;
        assertEquals("resgame", pe.getGame());
        assertEquals(1, pe.getPlayerNumber());
        assertEquals(SOCPlayerElement.SET, pe.getAction());
        assertEquals(PEType.WHEAT.getValue(), pe.getElementType());
        assertEquals(5, pe.getAmount());
    }

    @Test
    public void testPlayerElementMessage_gainOre()
    {
        SOCPlayerElement orig = new SOCPlayerElement(
            "resgame", 0, SOCPlayerElement.GAIN, PEType.ORE, 2);
        SOCMessage parsed = SOCMessage.toMsg(orig.toCmd());
        assertNotNull(parsed);

        SOCPlayerElement pe = (SOCPlayerElement) parsed;
        assertEquals(SOCPlayerElement.GAIN, pe.getAction());
        assertEquals(PEType.ORE.getValue(), pe.getElementType());
        assertEquals(2, pe.getAmount());
    }

    @Test
    public void testPlayerElementMessage_loseClay()
    {
        SOCPlayerElement orig = new SOCPlayerElement(
            "resgame", 3, SOCPlayerElement.LOSE, PEType.CLAY, 1);
        SOCMessage parsed = SOCMessage.toMsg(orig.toCmd());
        assertNotNull(parsed);

        SOCPlayerElement pe = (SOCPlayerElement) parsed;
        assertEquals(SOCPlayerElement.LOSE, pe.getAction());
        assertEquals(PEType.CLAY.getValue(), pe.getElementType());
        assertEquals(1, pe.getAmount());
    }

    @Test
    public void testGameTextMessage_roundTrip()
    {
        SOCGameTextMsg orig = new SOCGameTextMsg("chatgame", "player1", "hello all");
        String cmd = orig.toCmd();

        SOCMessage parsed = SOCMessage.toMsg(cmd);
        assertNotNull(parsed);
        assertEquals(SOCMessage.GAMETEXTMSG, parsed.getType());

        SOCGameTextMsg gtm = (SOCGameTextMsg) parsed;
        assertEquals("chatgame", gtm.getGame());
        assertEquals("player1", gtm.getNickname());
        assertEquals("hello all", gtm.getText());
    }

    @Test
    public void testHandle_nullMessageIsIgnored()
    {
        // MessageHandler.handle() should silently return on null.
        // We can't call handle() without init, but we verify the
        // message parsing returns null for garbage, which is what
        // would feed a null into handle().
        SOCMessage result = SOCMessage.toMsg("totally_invalid");
        assertNull("invalid wire data should produce null", result);
    }

    public static void main(String[] args)
    {
        org.junit.runner.JUnitCore.main("soctest.client.TestMessageHandler");
    }

}
