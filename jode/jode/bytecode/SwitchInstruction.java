/* SwitchInstruction Copyright (C) 1999 Jochen Hoenicke.
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
import jode.util.StringQuoter;

/**
 * This class represents an instruction in the byte code.
 *
 */
public class SwitchInstruction extends Instruction {
    /**
     * The values for this switch.
     */
    private int[] values;

    /**
     * Standard constructor: creates an opcode with parameter and
     * lineNr.  
     */
    public SwitchInstruction(int opcode, int[] values, int lineNr) {
	super(opcode, lineNr);
	if (opcode != opc_lookupswitch)
	    throw new IllegalArgumentException("Instruction is no switch");
	this.values = values;
    }

    /**
     * Creates a simple opcode, without any parameters.
     */
    public SwitchInstruction(int opcode, int[] values) {
	this(opcode, values, -1);
    }

    public final int[] getValues() 
    {
	return values;
    }

    public final void setValues(int[] values)
    {
	this.values = values;
    }

    public String toString() {
	StringBuffer sb = new StringBuffer(opcodeString[getOpcode()]);
	for (int i=0; i< values.length; i++)
	    sb.append(' ').append(values[i]);
	return sb.toString();
    }
}