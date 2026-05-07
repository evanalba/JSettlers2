/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file copyright (C) 2026 JSettlers2 contributors
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
 **/
package soc.robot;

import soc.game.SOCGame;

/**
 * The distinct game-state phases the robot brain traverses during the
 * opening (initial-settlement/road) placement sequence.
 *<P>
 * Historically {@link SOCRobotBrain} tracked these phases through a dozen
 * parallel {@code boolean} fields ({@code expectSTART1A}, {@code expectSTART1B},
 * ..., {@code expectPUTPIECE_FROM_START3B}). That encoding is a classic
 * "primitive obsession / Golden Hammer" -- any change in phase means flipping
 * several unrelated booleans in several places, and the set of legal phases
 * is implicit in which flag is currently true.
 *<P>
 * This enum captures the legal phases as a single first-class <em>State</em>
 * type: one value per distinct {@link SOCGame} game-state constant that falls
 * inside the initial-placement sequence. Each value carries its corresponding
 * {@link SOCGame} constant so callers can convert back and forth without
 * duplicating the game-state/phase mapping.
 *<P>
 * The enum is intentionally minimal and immutable; transitions between phases
 * (including clearing the matching "I've sent a PUTPIECE, now waiting"
 * expectation) live on {@link SOCRobotBrain}, where the existing boolean
 * fields are mutated. Subsequent PRs can migrate more of those call sites
 * behind this enum.
 *
 * @since 2.8.00
 */
public enum InitialPlacementPhase
{
    /** Brain is expecting the {@link SOCGame#START1A} game state (first settlement). */
    START1A(SOCGame.START1A),

    /** Brain is expecting the {@link SOCGame#START1B} game state (first road/ship). */
    START1B(SOCGame.START1B),

    /** Brain is expecting the {@link SOCGame#START2A} game state (second settlement). */
    START2A(SOCGame.START2A),

    /** Brain is expecting the {@link SOCGame#START2B} game state (second road/ship). */
    START2B(SOCGame.START2B),

    /** Brain is expecting the {@link SOCGame#START3A} game state (third settlement, rare scenarios). */
    START3A(SOCGame.START3A),

    /** Brain is expecting the {@link SOCGame#START3B} game state (third road/ship, rare scenarios). */
    START3B(SOCGame.START3B);

    private final int gameState;

    InitialPlacementPhase(final int gameState)
    {
        this.gameState = gameState;
    }

    /**
     * The {@link SOCGame} game-state constant this phase corresponds to
     * (e.g. {@link SOCGame#START1A}).
     */
    public int gameState()
    {
        return gameState;
    }

    /**
     * Convert a {@link SOCGame} game-state constant into the matching
     * {@code InitialPlacementPhase}, or {@code null} if the game state
     * is not one of the six initial-placement states.
     *
     * @param gameState a {@code SOCGame.STARTxY} value, or any other game state
     * @return the matching phase, or {@code null} if none matches
     */
    public static InitialPlacementPhase fromGameState(final int gameState)
    {
        for (final InitialPlacementPhase p : values())
            if (p.gameState == gameState)
                return p;
        return null;
    }
}
