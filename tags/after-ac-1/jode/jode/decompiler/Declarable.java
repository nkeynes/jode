/* Declarable Copyright (C) 1999 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */

package jode.decompiler;

/**
 * This is the interface for objects, that a method can declare
 */
public interface Declarable {
    /**
     * Get the name of this declarable.
     */
    public String getName();

    /**
     * Set the name of this local.
     */
    public void makeNameUnique();

    public void dumpDeclaration(TabbedPrintWriter writer)
        throws java.io.IOException;
}