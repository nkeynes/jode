/* CreateConstantArray Copyright (C) 1997-1998 Jochen Hoenicke.
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
import jode.Decompiler;
import jode.expr.*;
import jode.Type;

public class CreateConstantArray {

    public static boolean transform(InstructionContainer ic,
                                    StructuredBlock last) {
        /* Situation:
         *  PUSH new Array[]      // or a constant array operator.
         *  DUP                   // duplicate array reference
         *  PUSH index
         *  PUSH value
         *  stack_2[stack_1] = stack_0
         *  ...
         */
        if (last.outer instanceof SequentialBlock) {

	    SequentialBlock sequBlock = (SequentialBlock) last.outer;

            if (!(ic.getInstruction() instanceof ArrayStoreOperator)
                || !(sequBlock.subBlocks[0] instanceof InstructionBlock)
                || !(sequBlock.outer instanceof SequentialBlock))
                return false;
            ArrayStoreOperator store 
                = (ArrayStoreOperator) ic.getInstruction();
            InstructionBlock ib = (InstructionBlock)sequBlock.subBlocks[0];
            sequBlock = (SequentialBlock) sequBlock.outer;

            if (!(sequBlock.subBlocks[0] instanceof InstructionBlock)
                || !(sequBlock.outer instanceof SequentialBlock))
                return false;

            Expression expr = ib.getInstruction();
            ib = (InstructionBlock)sequBlock.subBlocks[0];
            sequBlock = (SequentialBlock) sequBlock.outer;

            if (expr.getOperandCount() > 0 
                || !(ib.getInstruction() instanceof ConstOperator)
                || !(sequBlock.subBlocks[0] instanceof SpecialBlock)
                || !(sequBlock.outer instanceof SequentialBlock))
                return false;
                
            ConstOperator indexOp = (ConstOperator) ib.getInstruction();
            SpecialBlock dup = (SpecialBlock) sequBlock.subBlocks[0];
            sequBlock = (SequentialBlock) sequBlock.outer;

            if (!indexOp.getType().isOfType(Type.tUInt)
                || dup.type != SpecialBlock.DUP
                || dup.depth != 0 || dup.count != 1
                || !(sequBlock.subBlocks[0] instanceof InstructionBlock))
                return false;

            int index = Integer.parseInt(indexOp.getValue());
            ib = (InstructionBlock)sequBlock.subBlocks[0];

            if (ib.getInstruction() instanceof ComplexExpression
                && (ib.getInstruction().getOperator()
                    instanceof NewArrayOperator)) {
                /* This is the first element */
                ComplexExpression newArrayExpr = 
                    (ComplexExpression) ib.getInstruction();
                NewArrayOperator newArrayOp = 
                    (NewArrayOperator) newArrayExpr.getOperator();
                if (newArrayOp.getOperandCount() != 1
                    || !(newArrayExpr.getSubExpressions()[0]
                         instanceof ConstOperator))
                    return false;
                    
                ConstOperator countop = 
                    (ConstOperator) newArrayExpr.getSubExpressions()[0];
                if (!countop.getType().isOfType(Type.tUInt))
                    return false;

                int arraylength = Integer.parseInt(countop.getValue());
                if (arraylength <= index)
                    return false;

                if (Decompiler.isVerbose)
                    Decompiler.err.print('a');

                ConstantArrayOperator cao 
                    = new ConstantArrayOperator(newArrayOp.getType(), 
                                                arraylength);
                cao.setValue(index, expr);
                ic.setInstruction(cao);
                ic.moveDefinitions(sequBlock, last);
                last.replace(sequBlock);
                return true;

            } else if (ib.getInstruction() instanceof ConstantArrayOperator) {
                ConstantArrayOperator cao 
                    = (ConstantArrayOperator) ib.getInstruction();
                if (cao.setValue(index, expr)) {
                    /* adding Element succeeded */
                    ic.setInstruction(cao);
                    ic.moveDefinitions(sequBlock, last);
                    last.replace(sequBlock);
                    return true;
                }
            }
            
        }
        return false;
    }
}