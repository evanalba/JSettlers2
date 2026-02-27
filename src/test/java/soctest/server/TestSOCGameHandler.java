/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2018-2021,2023 Jeremy D Monin <jeremy@nand.net>
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

import java.util.Random;

import soc.game.SOCGame;
import soc.game.SOCGameOption;
import soc.game.SOCGameOptionSet;
import soc.game.SOCScenario;
import soc.message.SOCMessage;
import soc.message.SOCPotentialSettlements;
import soc.server.SOCGameHandler;
import soc.server.SOCGameListAtServer;
import soc.util.SOCFeatureSet;
import soctest.game.GameTestUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link SOCGameHandler}.
 *<P>
 * Covers {@code calcGameClientFeaturesRequired}, game creation through
 * {@link soctest.game.GameTestUtils}, board layout message generation,
 * and debug command help output.
 *
 * @since 2.0.00
 */
public class TestSOCGameHandler
{
    private static SOCGameHandler sgh;
    private static SOCGameListAtServer gl;

    @BeforeClass
    public static void setUpOnce()
    {
        sgh = new SOCGameHandler(null);
        gl = new SOCGameListAtServer(new Random(), SOCGameOptionSet.getAllKnownOptions());
    }

    /** Tests for {@link SOCGameHandler#calcGameClientFeaturesRequired(SOCGame)}. */
    @Test
    public void testCalcGameClientFeaturesRequired()
    {
        final SOCGameOptionSet knownOpts = SOCGameOptionSet.getAllKnownOptions();

        /**
         * Game opts and expected resulting client features.
         * When one client feature is expected, will test with String.equals.
         * When multiple client features are expected, will test with featsObj.findMissingAgainst(otherFeatsSet),
         * which unfortunately also returns true if an int-valued feature > its value in otherFeatsSet.
         */
        final String[][] gameoptFeatPairs =
            {
                { null, null },
                { "SBL=t", ';' + SOCFeatureSet.CLIENT_SEA_BOARD + ';' },
                { "PL=4", null },
                { "PL=5", ';' + SOCFeatureSet.CLIENT_6_PLAYERS + ';' },
                { "PLB=f", null },
                { "PLB=t", ';' + SOCFeatureSet.CLIENT_6_PLAYERS + ';' },
                { "SC=" + SOCScenario.K_SC_4ISL, ';' + SOCFeatureSet.CLIENT_SCENARIO_VERSION + "=2000;" },
                { "SC=_NONEXISTENT_", ';' + SOCFeatureSet.CLIENT_SCENARIO_VERSION + "=" + Integer.MAX_VALUE + ";" },
                { "SBL=t,PL=5", ';' + SOCFeatureSet.CLIENT_SEA_BOARD + ';' + SOCFeatureSet.CLIENT_6_PLAYERS + ';' },
                { "_3P=7", ";com.example.js.feat.t3p;" },
            };
            // OTYPE_*: Test the new type's fields by adding an option here having setClientFeature.
            // See SGH.calcGameClientFeaturesRequired jdoc for how it checks whether the option is being used.

        // test FLAG_3RD_PARTY and its feature
        SOCGameOption opt3PKnown = new SOCGameOption
            ("_3P", 2000, 2500, 0, 0, 0xFFFF, SOCGameOption.FLAG_3RD_PARTY, "For unit test");
        opt3PKnown.setClientFeature("com.example.js.feat.t3p");
        assertTrue(opt3PKnown.hasFlag(SOCGameOption.FLAG_3RD_PARTY));
        knownOpts.addKnownOption(opt3PKnown);

        for (String[] pair : gameoptFeatPairs)
        {
            final String gameopts = pair[0], featsStr = pair[1];

            final SOCGame ga = new SOCGame
                ("testname", SOCGameOption.parseOptionsToSet(gameopts, knownOpts), knownOpts);
            sgh.calcGameClientFeaturesRequired(ga);
            final SOCFeatureSet cliFeats = ga.getClientFeaturesRequired();
            if (featsStr == null)
            {
                if (cliFeats != null)
                    fail("For gameopts " + gameopts + " expected no cli feats but got " + cliFeats.getEncodedList());
            } else if (cliFeats == null) {
                fail("For gameopts " + gameopts + " expected some cli feats but got null");
            } else {
                final boolean hasMulti = (featsStr.indexOf(';', 2) < (featsStr.length() - 1));
                final boolean passed = (hasMulti)
                    ? (null == cliFeats.findMissingAgainst(new SOCFeatureSet(featsStr), true))
                    : featsStr.equals(cliFeats.getEncodedList());
                if (! passed)
                    fail("For gameopts " + gameopts + " expected cli feats " + featsStr
                         + " but got " + cliFeats.getEncodedList());
            }
        }
    }

    /**
     * Creating a standard 4-player game through {@link GameTestUtils#createGame}
     * should produce a valid game with the expected player count and state.
     */
    @Test
    public void testCreateGame_classic4p()
    {
        final SOCGame ga = GameTestUtils.createGame(4, null, null, "test4p", gl, sgh);
        try
        {
            assertEquals(4, ga.maxPlayers);
            assertFalse(ga.hasSeaBoard);
            assertNotNull(ga.getGameOptions());
        }
        finally
        {
            gl.deleteGame(ga.getName());
        }
    }

    /**
     * A 6-player game should set maxPlayers to 6 and the PLB option should be active.
     */
    @Test
    public void testCreateGame_6player()
    {
        final SOCGame ga = GameTestUtils.createGame(6, null, null, "test6p", gl, sgh);
        try
        {
            assertEquals(6, ga.maxPlayers);
            assertTrue("PLB should be set for 6-player game",
                ga.isGameOptionSet("PLB"));
        }
        finally
        {
            gl.deleteGame(ga.getName());
        }
    }

    /**
     * A game created with the Four Islands scenario should carry
     * the {@code SC} option and require the sea board.
     */
    @Test
    public void testCreateGame_scenario4ISL()
    {
        final SOCGame ga = GameTestUtils.createGame(
            4, SOCScenario.K_SC_4ISL, null, "test4ISL", gl, sgh);
        try
        {
            assertTrue("scenario game needs sea board", ga.hasSeaBoard);
            assertEquals(SOCScenario.K_SC_4ISL,
                ga.getGameOptionStringValue("SC"));
        }
        finally
        {
            gl.deleteGame(ga.getName());
        }
    }

    /**
     * {@link SOCGameHandler#getBoardLayoutMessage(SOCGame)} should return
     * a non-null message for a classic 4-player game after it has been started.
     */
    @Test
    public void testGetBoardLayoutMessage_classic()
    {
        final SOCGame ga = GameTestUtils.createGame(4, null, null, "testBLM", gl, sgh);
        try
        {
            for (int pn = 0; pn < 4; pn++)
                ga.addPlayer("testplayer" + pn, pn);
            ga.startGame();

            SOCMessage msg = SOCGameHandler.getBoardLayoutMessage(ga);
            assertNotNull("board layout message should not be null", msg);
        }
        finally
        {
            gl.deleteGame(ga.getName());
        }
    }

    /**
     * {@link SOCGameHandler#getDebugCommandsHelp()} should return a non-empty
     * array of strings, and should contain expected entries.
     */
    @Test
    public void testGetDebugCommandsHelp()
    {
        final String[] help = sgh.getDebugCommandsHelp();
        assertNotNull("debug help should not be null", help);
        assertTrue("debug help should have entries", help.length > 0);

        boolean foundRsrcs = false;
        boolean foundDev = false;
        for (String line : help)
        {
            assertNotNull(line);
            if (line.startsWith("rsrcs:"))
                foundRsrcs = true;
            if (line.startsWith("dev:"))
                foundDev = true;
        }
        assertTrue("debug help should contain rsrcs command", foundRsrcs);
        assertTrue("debug help should contain dev command", foundDev);
    }

    /**
     * {@link SOCGameHandler#gatherBoardPotentials(SOCGame, int)} should
     * return a non-null array for a started classic game.
     */
    @Test
    public void testGatherBoardPotentials_classicGame()
    {
        final SOCGame ga = GameTestUtils.createGame(4, null, null, "testGBP", gl, sgh);
        try
        {
            for (int pn = 0; pn < 4; pn++)
                ga.addPlayer("gbpPlayer" + pn, pn);
            ga.startGame();

            SOCPotentialSettlements[] potentials =
                SOCGameHandler.gatherBoardPotentials(ga, Integer.MAX_VALUE);
            assertNotNull("potentials array should not be null", potentials);
            assertTrue("potentials should have entries", potentials.length > 0);
        }
        finally
        {
            gl.deleteGame(ga.getName());
        }
    }

}
