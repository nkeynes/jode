/* IncInstruction Copyright (C) 1999 Jochen Hoenicke.
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

package jode.bytecode;

/**
 * This class represents an instruction in the byte code.
 *
 */
public class IncInstruction extends SlotInstruction {
    /**
     * The amount of increment.
     */
    private int increment;

    /**
     * Standard constructor: creates an opcode with parameter and
     * lineNr.  
     */
    public IncInstruction(int opcode, int slot, int increment, int lineNr) {
	super(opcode, slot, lineNr);
	if (opcode != opc_iinc)
	    throw new IllegalArgumentException("Instruction has no increment");
	this.increment = increment;
    }

    /**
     * Creates a simple opcode, without any parameters.
     */
    public IncInstruction(int opcode, int slot, int increment) {
	this(opcode, slot, increment, -1);
    }

    /**
     * Get the increment for an opc_iinc instruction.
     */
    public final int getIncrement()
    {
	return increment;
    }

    /**
     * Set the increment for an opc_iinc instruction.
     */
    public final void setIncrement(int incr)
    {
	this.increment = incr;
    }


    public String toString() {
	return super.toString()+' '+increment;
    }
}