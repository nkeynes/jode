/* ConditionalBlock (c) 1998 Jochen Hoenicke
 *
 * You may distribute under the terms of the GNU General Public License.
 *
 * IN NO EVENT SHALL JOCHEN HOENICKE BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JOCHEN HOENICKE 
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JOCHEN HOENICKE SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
 * BASIS, AND JOCHEN HOENICKE HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * $Id$
 */
package jode.flow;
import jode.decompiler.TabbedPrintWriter;
import jode.expr.Expression;
import jode.expr.LocalVarOperator;

/**
 * An ConditionalBlock is the structured block representing an if
 * instruction.  The else part may be null.
 */
public class ConditionalBlock extends InstructionContainer {
    /**
     * The loads that are on the stack before instr is executed.
     */
    VariableStack stack;

    EmptyBlock trueBlock;
    
    public void checkConsistent() {
        super.checkConsistent();
        if (trueBlock.jump == null
            || !(trueBlock instanceof EmptyBlock))
            throw new jode.AssertError("Inconsistency");
    }

    /**
     * Creates a new if conditional block.
     */
    public ConditionalBlock(Expression cond, Jump condJump, Jump elseJump) {
        super(cond, elseJump);
        /* cond is a CompareBinary or CompareUnary operator, so no
         * check for LocalVarOperator (for condJump) is needed here.  
         */
        trueBlock = new EmptyBlock(condJump);
        trueBlock.outer = this;
    }

    /**
     * Creates a new if conditional block.
     */
    public ConditionalBlock(Expression cond) {
        super(cond);
        /* cond is a CompareBinary or CompareUnary operator, so no
         * check for LocalVarOperator (for condJump) is needed here.  
         */
        trueBlock = new EmptyBlock();
        trueBlock.outer = this;
    }

    /* The implementation of getNext[Flow]Block is the standard
     * implementation 
     */

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return new StructuredBlock[] { trueBlock };
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                                   StructuredBlock newBlock) {
        throw new jode.AssertError("replaceSubBlock on ConditionalBlock");
    }

    /**
     * This does take the instr into account and modifies stack
     * accordingly.  It then calls super.mapStackToLocal.
     * @param stack the stack before the instruction is called
     * @return stack the stack afterwards.
     */
    public VariableStack mapStackToLocal(VariableStack stack) {
	VariableStack newStack;
	int params = instr.getOperandCount();
	if (params > 0) {
	    this.stack = stack.peek(params);
	    newStack = stack.pop(params);
	} else 
	    newStack = stack;

	trueBlock.jump.stackMap = newStack;
	if (jump != null)
	    jump.stackMap = newStack;
	return newStack;
    }

    public void removePush() {
	if (stack != null)
	    instr = stack.mergeIntoExpression(instr, used);
	trueBlock.removePush();
    }

    /**
     * Print the source code for this structured block.  
     */
    public void dumpInstruction(TabbedPrintWriter writer)
        throws java.io.IOException
    {
        writer.println("IF ("+instr.simplify().toString()+")");
        writer.tab();
        trueBlock.dumpSource(writer);
        writer.untab();
    }

    public boolean doTransformations() {
        StructuredBlock last =  flowBlock.lastModified;
        return super.doTransformations()
            || CombineIfGotoExpressions.transform(this, last)
            || CreateIfThenElseOperator.createFunny(this, last);
    }
}