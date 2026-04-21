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
package soc.client;

import java.awt.Color;

/**
 * Shared UI theme constants for JSettlers client panels.
 *<P>
 * Intended as a small Flyweight-style holder for the section-header colors that
 * several dialog/panel classes were each re-declaring as private constants
 * (most notably {@link NewGameOptionsFrame} and {@link SOCConnectOrPracticePanel}).
 * Centralising these here removes code duplication and gives future panels a
 * single place to pick up the theme. Each {@link Color} object is allocated
 * exactly once at class-load time and shared by every caller, which is the
 * point of the Flyweight: many label widgets, one {@code Color} instance.
 *<P>
 * This class intentionally holds only <em>intrinsic</em> color state. Deciding
 * which constant to apply to a given {@code JLabel} is left to the caller
 * (extrinsic context) so that panels can still vary the foreground when it
 * matters for contrast.
 *
 * @since 2.8.00
 */
public final class ClientUITheme
{
    /**
     * Background color for section-header labels (a pale, high-contrast green).
     * Previously duplicated as {@code HEADER_LABEL_BG} in
     * {@link NewGameOptionsFrame} and {@link SOCConnectOrPracticePanel}.
     */
    public static final Color HEADER_LABEL_BG = new Color(220, 255, 220);

    /**
     * Default foreground for section-header labels on panels that want the
     * strongest contrast (used e.g. by {@link NewGameOptionsFrame}).
     */
    public static final Color HEADER_LABEL_FG_DEFAULT = Color.BLACK;

    /**
     * Muted dark-green foreground for section-header labels on panels that
     * want a softer look against {@link #HEADER_LABEL_BG}
     * (used e.g. by {@link SOCConnectOrPracticePanel}).
     */
    public static final Color HEADER_LABEL_FG_MUTED = new Color(50, 80, 50);

    /** Not instantiable. */
    private ClientUITheme() {}
}
