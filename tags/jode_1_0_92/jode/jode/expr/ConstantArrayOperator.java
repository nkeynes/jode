/* ConstantArrayOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.type.Type;
import jode.type.ArrayType;
import jode.decompiler.TabbedPrintWriter;

public class ConstantArrayOperator extends Operator {
    boolean isInitializer;
    ConstOperator empty;
    Type argType;

    public ConstantArrayOperator(Type type, int size) {
        super(type);
        argType = (type instanceof ArrayType) 
            ? Type.tSubType(((ArrayType)type).getElementType()) : Type.tError;

	Object emptyVal;
	if (argType == type.tError || argType.isOfType(Type.tUObject))
	    emptyVal = null;
	else if (argType.isOfType(Type.tBoolUInt))
	    emptyVal = new Integer(0);
	else if (argType.isOfType(Type.tLong))
	    emptyVal = new Long(0);
	else if (argType.isOfType(Type.tFloat))
	    emptyVal = new Float(0);
	else if (argType.isOfType(Type.tDouble))
	    emptyVal = new Double(0);
	else
	    throw new IllegalArgumentException("Illegal Type: "+argType);
	    
        empty = new ConstOperator(emptyVal);
	empty.setType(argType);
        empty.makeInitializer();
	initOperands(size);
	for (int i=0; i < subExpressions.length; i++)
	    setSubExpressions(i, empty);
    }

    public void updateSubTypes() {
        argType = (type instanceof ArrayType) 
            ? Type.tSubType(((ArrayType)type).getElementType()) : Type.tError;
	for (int i=0; i< subExpressions.length; i++)
	    if (subExpressions[i] != null)
		subExpressions[i].setType(argType);
    }

    public void updateType() {
    }

    public boolean setValue(int index, Expression value) {
        if (index < 0 || 
	    index > subExpressions.length || 
	    subExpressions[index] != empty)
            return false;
        value.setType(argType);
        setType(Type.tSuperType(Type.tArray(value.getType())));
        subExpressions[index] = value;
        value.parent = this;
        value.makeInitializer();
        return true;
    }

    public int getPriority() {
        return 200;
    }

    public void makeInitializer() {
        isInitializer = true;
    }

    public Expression simplify() {
	for (int i=0; i< subExpressions.length; i++) {
	    if (subExpressions[i] != null)
		subExpressions[i] = subExpressions[i].simplify();
	}
	return this;
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	if (!isInitializer) {
	    writer.print("new ");
	    writer.printType(type.getHint());
	    writer.print(" ");
	}
	writer.openBraceNoSpace();
	writer.tab();
        for (int i=0; i< subExpressions.length; i++) {
            if (i>0) {
		if (i % 10 == 0)
		    writer.println(",");
		else
		    writer.print(", ");
	    }
	    if (subExpressions[i] != null)
		subExpressions[i].dumpExpression(writer, 0);
	    else
		empty.dumpExpression(writer, 0);
        }
	writer.println();
	writer.untab();
	writer.closeBraceNoSpace();
    }
}