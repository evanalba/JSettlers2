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
package soc.game;

/**
 * Narrow server-side board capabilities that the {@code soc.game} package
 * needs to reach for at runtime.
 *<P>
 * This interface exists purely to break a layer violation: {@link SOCGame}
 * and {@link SOCGameDiceHandler} were importing {@code soc.server.SOCBoardAtServer}
 * directly and down-casting to it, which is a circular dependency between the
 * game model and the server implementation. Declaring the needed hooks here
 * lets game-layer code depend on an abstraction that lives in its own package
 * while the concrete implementation (the server's {@code SOCBoardAtServer})
 * plugs in via <em>Adapter</em> at runtime.
 *<P>
 * Classic Adapter shape:
 *<UL>
 *  <LI> Target: {@code BoardServerExtension} (this interface, in {@code soc.game}).
 *  <LI> Adaptee: the existing methods on {@code soc.server.SOCBoardAtServer}.
 *  <LI> Adapter: {@code SOCBoardAtServer} itself, which now {@code implements}
 *       this interface. Its method signatures already match, so no wrapper
 *       class is needed -- it is its own class-level adapter.
 *</UL>
 *<P>
 * Game-layer callers should check {@code board instanceof BoardServerExtension}
 * instead of a concrete server class, and cast to this interface. That keeps
 * the client build clean of any {@code soc.server.*} imports in the game
 * package while leaving server behavior untouched.
 *
 * @since 2.8.00
 */
public interface BoardServerExtension
{
    /**
     * Some scenarios mark a particular Land Area as "bonus-excluded". At the
     * server this is stored on the server-side board subclass; game logic in
     * {@link SOCGame#updateAtGameFirstTurn()} needs to read it to encode each
     * player's second starting land area. Clients should never call this
     * because the server translates the value into per-player state.
     *
     * @return the bonus-excluded Land Area number, or 0 if unused
     * @see SOCPlayer#setStartingLandAreasEncoded(int)
     */
    int getBonusExcludeLandArea();

    /**
     * When a dice roll is being resolved at the server for a sea board running
     * the Cloth Trade scenario ({@code _SC_CLVI}), villages may distribute
     * cloth to players adjacent to them. {@link SOCGameDiceHandler} needs to
     * invoke this at roll time; clients never do.
     *
     * @param game        the game being rolled in; callers pass {@code this} (or the outer game)
     * @param rollRes     the in-progress {@link SOCGame.RollResult} so added cloth is recorded
     * @param dice        the dice total just rolled
     * @return true if distributing the cloth caused a player to reach the
     *         scenario's cloth-count victory target (the caller should then
     *         check for a winner); false otherwise
     */
    boolean distributeClothFromRoll(SOCGame game, SOCGame.RollResult rollRes, int dice);
}
