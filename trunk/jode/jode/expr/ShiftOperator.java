/* ShiftOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.expr;
import jode.Type;

/**
 * ShiftOpcodes are special, because their second operand is an UIndex
 */
public class ShiftOperator extends BinaryOperator {

    public ShiftOperator(Type type, int op) {
        super(type, op);
    }

    public Type getOperandType(int i) {
        return (i==0) ? type : Type.tInt;
    }

    public void setOperandType(Type[] inputTypes) {
	setType(inputTypes[0]);
    }
}