/* CreateCheckNull Copyright (C) 1999 Jochen Hoenicke.
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

package jode.flow;
import jode.expr.*;
import jode.type.Type;
import jode.decompiler.LocalInfo;

public class CreateCheckNull {

    /* Situation:
     * 
     * javac: 
     *  DUP
     *  stack_0.getClass();
     *
     * jikes:
     *  DUP
     *  if (!stack_0 != null)
     *    throw null;
     */

    public static boolean transformJavac(InstructionContainer ic,
					 StructuredBlock last) {
        if (!(last.outer instanceof SequentialBlock)
	    || !(ic.getInstruction() instanceof ComplexExpression)
	    || !(last.outer.getSubBlocks()[0] instanceof SpecialBlock))
            return false;

	SpecialBlock dup = (SpecialBlock) last.outer.getSubBlocks()[0];
	if (dup.type != SpecialBlock.DUP
	    || dup.count != 1 || dup.depth != 0)
	    return false;
	   
	ComplexExpression ce = (ComplexExpression) ic.getInstruction();

	if (!(ce.getOperator() instanceof PopOperator)
	    || !(ce.getSubExpressions()[0] instanceof InvokeOperator))
	    return false;

        InvokeOperator getClassCall
	    = (InvokeOperator) ce.getSubExpressions()[0];
	if (!getClassCall.getMethodName().equals("getClass")
	    || !(getClassCall.getMethodType().toString()
		 .equals("()Ljava/lang/Class;")))
	    return false;

	LocalInfo li = new LocalInfo();
	ic.setInstruction(new CheckNullOperator(Type.tUObject, li));
	ic.used.addElement(li);
	last.replace(last.outer);
        return true;
    }

    public static boolean transformJikes(IfThenElseBlock ifBlock,
					 StructuredBlock last) {
        if (!(last.outer instanceof SequentialBlock)
	    || !(last.outer.getSubBlocks()[0] instanceof SpecialBlock)
	    || ifBlock.elseBlock != null
	    || !(ifBlock.thenBlock instanceof ThrowBlock))
            return false;

	SpecialBlock dup = (SpecialBlock) last.outer.getSubBlocks()[0];
	if (dup.type != SpecialBlock.DUP
	    || dup.count != 1 || dup.depth != 0)
	    return false;
	   
	/* negate the instruction back to its original state */
	Expression expr = ifBlock.cond.negate();
	if (!(expr instanceof CompareUnaryOperator)
	    || expr.getOperator().getOperatorIndex() != Operator.NOTEQUALS_OP
	    || !(expr.getOperator().getOperandType(0).isOfType(Type.tUObject)))
	    return false;

	LocalInfo li = new LocalInfo();
	InstructionContainer ic = 
	    new InstructionBlock(new CheckNullOperator(Type.tUObject, li));
	ic.used.addElement(li);
	ifBlock.flowBlock.removeSuccessor(ifBlock.thenBlock.jump);
	ic.moveJump(ifBlock.jump);
	if (last == ifBlock) {
	    ic.replace(last.outer);
	    last = ic;
	} else {
	    ic.replace(ifBlock);
	    last.replace(last.outer);
	}
        return true;
    }
}