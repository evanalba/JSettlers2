package soctest.game;

import org.junit.Test;

import soc.game.SOCGame;
import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.Assert.*;

public class TestRobberyGuards
{
    private static void setIntField(Object obj, String fieldName, int value) throws Exception
    {
        Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setInt(obj, value);
    }

    private static Object getField(Object obj, String fieldName) throws Exception
    {
        Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(obj);
    }

    /**
     * A player cannot bypass the robber steal step by choosing -1.
     * 
     * The normal scenario: 1. Client shows UI: “Choose a player to rob” 2. Player
     * clicks someone → client sends message with that player's number 3. Server
     * receives the message and calls something like: game.canChoosePlayer(pn);
     * 
     * Note: pn >= 0 means choose that player number pn == -1 special case: choose
     * no victim
     */
    @Test
    public void cannotBypassRobbery() throws Exception
    {
        SOCGame g = new SOCGame("test");
        setIntField(g, "gameState", SOCGame.WAITING_FOR_ROB_CHOOSE_PLAYER);
        assertFalse(g.canChoosePlayer(-1));
    }

    /**
     * Simulates the robbery phase when there are zero eligible victims and checks
     * that you cannot choose any real player (0, 1, 2, 3) to rob.
     * 
     * You may steal from a player only if: - They have a settlement/city adjacent
     * to that hex - They have at least 1 resource
     * 
     * How Zero Victims Happens in Practice: Case 1: Robber moved to a hex with no
     * opponent settlements touching it Case 2: Adjacent players have zero resources
     */
    @Test
    public void noEligibleVictimsMeansNoOneIsChoosable() throws Exception
    {
        SOCGame g = new SOCGame("test");
        setIntField(g, "gameState", SOCGame.WAITING_FOR_ROB_CHOOSE_PLAYER);

        Object currentRoll = getField(g, "currentRoll");
        Field victimsField = currentRoll.getClass().getDeclaredField("sc_robPossibleVictims");
        victimsField.set(currentRoll, Collections.emptyList());

        assertFalse(g.canChoosePlayer(0));
        assertFalse(g.canChoosePlayer(1));
        assertFalse(g.canChoosePlayer(2));
        assertFalse(g.canChoosePlayer(3));
    }

}