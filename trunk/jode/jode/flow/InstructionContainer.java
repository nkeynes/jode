/* InstructionContainer Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.decompiler.LocalInfo;
import jode.expr.Expression;
import jode.expr.LocalVarOperator;

/**
 * This is a method for block containing a single instruction.
 */
public abstract class InstructionContainer extends StructuredBlock {
    /**
     * The instruction.
     */
    Expression instr;

    public InstructionContainer(Expression instr) {
        this.instr = instr;
        if (instr instanceof LocalVarOperator)
	  used.addElement(((LocalVarOperator)instr).getLocalInfo());
    }

    public InstructionContainer(Expression instr, Jump jump) {
	this(instr);
        setJump(jump);
    }

    public void setJump(Jump jump) {
	super.setJump(jump);
        if (instr instanceof LocalVarOperator) {
            LocalVarOperator varOp = (LocalVarOperator) instr;
            jump.gen.addElement(varOp.getLocalInfo());
            jump.kill.addElement(varOp.getLocalInfo());
        }
    }

    /** 
     * This method should remove local variables that are only written
     * and read one time directly after another.  <br>
     *
     * This is especially important for stack locals, that are created
     * when there are unusual swap or dup instructions, but also makes
     * inlined functions more pretty (but not that close to the
     * bytecode).  
     */
    public void removeOnetimeLocals() {
	if (instr != null)
	    instr = instr.removeOnetimeLocals();
	super.removeOnetimeLocals();
    }

    /**
     * Fill all in variables into the given VariableSet.
     * @param in The VariableSet, the in variables should be stored to.
     */
    public void fillInGenSet(VariableSet in, VariableSet gen) {
        if (instr instanceof LocalVarOperator) {
            LocalVarOperator varOp = (LocalVarOperator) instr;
            if (varOp.isRead()) {
                in.addElement(varOp.getLocalInfo());
            }
            gen.addElement(varOp.getLocalInfo());
        }
    }

    public boolean doTransformations() {
        StructuredBlock last = flowBlock.lastModified;
        return CreateNewConstructor.transform(this, last)
            || CreateAssignExpression.transform(this, last)
            || CreateExpression.transform(this, last)
            || CreatePrePostIncExpression.transform(this, last)
            || CreateIfThenElseOperator.create(this, last)
            || CreateConstantArray.transform(this, last)
	    || CreateCheckNull.transformJavac(this, last);
    }

    /**
     * Get the contained instruction.
     * @return the contained instruction.
     */
    public final Expression getInstruction() {
        return instr;
    }

    /**
     * Set the contained instruction.
     * @param instr the new instruction.
     */
    public final void setInstruction(Expression instr) {
        this.instr = instr;
    }
}