/* 
 * CreateAssignExpression (c) 1998 Jochen Hoenicke
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
import jode.expr.*;
import jode.Type;

public class CreateAssignExpression {

    public static boolean transform(InstructionContainer ic,
                             StructuredBlock last) {
        if (!(last.outer instanceof SequentialBlock)
            || !(ic.getInstruction() instanceof StoreInstruction)
            || !(ic.getInstruction().isVoid()))
            return false;
        
        return (createAssignOp(ic, last) || createAssignExpression(ic, last));
    }

    public static boolean createAssignOp(InstructionContainer ic,
                                         StructuredBlock last) {

        /* Situation:
         *
         *   (push loadstoreOps)  <- not checked
         * sequBlock:
         *   dup (may be missing for static / local variables)
         * opBlock:
         *   (optional narrow) ((optional wide) load(stack) * rightHandSide)
         *   (optional dup_x)
         *   store(stack)
         *
         * We transform it to:
         *   (push loadstoreOps)
         *   rightHandSide
	 *   (optional dup_x)
         *   store(stack) *= (stack)
         *
         * If the optional dup is present the store*= becomes non void.  */
        SequentialBlock opBlock = (SequentialBlock) last.outer;
        StoreInstruction store = (StoreInstruction) ic.getInstruction();

        boolean isAssignOp = false;
        if (opBlock.subBlocks[0] instanceof SpecialBlock) {
            SpecialBlock dup = (SpecialBlock) opBlock.subBlocks[0];
            if (dup.type != SpecialBlock.DUP
                || dup.depth != store.getLValueOperandCount()
                || dup.count != store.getLValueType().stackSize()
                || !(opBlock.outer instanceof SequentialBlock))
                return false;
            opBlock = (SequentialBlock) opBlock.outer;
            isAssignOp = true;
        }

        if (!(opBlock.subBlocks[0] instanceof InstructionBlock))
            return false;

        InstructionBlock ib = (InstructionBlock) opBlock.subBlocks[0];
	if (!(ib.getInstruction() instanceof ComplexExpression))
	    return false;

	ComplexExpression expr = (ComplexExpression) ib.getInstruction();
	SpecialBlock dup = null;
        
	if (store.getLValueOperandCount() > 0) {
	    if (!(opBlock.outer instanceof SequentialBlock)
		|| !(opBlock.outer.getSubBlocks()[0] instanceof SpecialBlock))
		return false;
	    
	    SequentialBlock sequBlock = (SequentialBlock) opBlock.outer;
	    dup = (SpecialBlock) sequBlock.subBlocks[0];
	    
	    if (dup.type != SpecialBlock.DUP
		|| dup.depth != 0
		|| dup.count != store.getLValueOperandCount())
		return false;
	}
        int opIndex;
        Expression rightHandSide;
	Type rhsType;

        if (expr.getOperator() instanceof ConvertOperator
            && expr.getSubExpressions()[0] instanceof ComplexExpression
            && expr.getOperator().getType().isOfType(store.getLValueType())) {

	    /* This gets tricky.  We need to allow something like
	     *  s = (short) (int) ((double) s / 0.1);
	     */
            expr = (ComplexExpression) expr.getSubExpressions()[0];
	    while (expr.getOperator() instanceof ConvertOperator
		   && expr.getSubExpressions()[0] instanceof ComplexExpression)
		expr = (ComplexExpression) expr.getSubExpressions()[0];
        }
        if (expr.getOperator() instanceof BinaryOperator) {
            BinaryOperator binop = (BinaryOperator) expr.getOperator();
            
            opIndex = binop.getOperatorIndex();
            if (opIndex <  binop.ADD_OP || opIndex >= binop.ASSIGN_OP)
		return false;

	    Expression loadExpr = expr.getSubExpressions()[0];
	    while (loadExpr instanceof ComplexExpression
		   && loadExpr.getOperator() instanceof ConvertOperator)
		loadExpr = ((ComplexExpression)loadExpr)
		    .getSubExpressions()[0];
	    if (!(loadExpr instanceof Operator)
                || !store.matches((Operator) loadExpr))
                return false;
            rightHandSide = expr.getSubExpressions()[1];
	    rhsType = binop.getOperandType(1);
        } else {
	    /* For String += the situation is more complex.
	     * what is marked as load(stack) * rightHandSide above is
	     * really (after simplification):
	     *
	     *   PUSH ((load(stack) + right) + Hand) + Side
	     */
            Expression simple = expr.simplifyString();
            rightHandSide = simple;
            /* Now search for the leftmost operand ... */
            ComplexExpression lastExpr = null;
            while (simple instanceof ComplexExpression
                   && simple.getOperator() instanceof StringAddOperator) {
                lastExpr = (ComplexExpression) simple;
                simple = lastExpr.getSubExpressions()[0];
            }

            /* ... check it ... */
            if (lastExpr == null || !(simple instanceof Operator)
                || !store.matches((Operator) simple))
                return false;

	    rhsType = Type.tString;
            
            /* ... and remove it. */
            if (lastExpr.getParent() != null) {
		ComplexExpression ce = (ComplexExpression)lastExpr.getParent();
		StringAddOperator addOp = (StringAddOperator) ce.getOperator();
		addOp.clearFirstType();
		ce.setSubExpressions(0,lastExpr.getSubExpressions()[1]);
            } else
                rightHandSide = lastExpr.getSubExpressions()[1]; 

            opIndex = Operator.ADD_OP;
        }

	if (dup != null)
	    dup.removeBlock();
        ib.setInstruction(rightHandSide);
        
        store.makeOpAssign(store.OPASSIGN_OP+opIndex, rhsType);

        if (isAssignOp)
            store.makeNonVoid();
        last.replace(opBlock.subBlocks[1]);
        return true;
    }

    public static boolean createAssignExpression(InstructionContainer ic,
                                          StructuredBlock last) {
        /* Situation:
         * sequBlock:
         *   dup_X(lvalue_count)
         *   store instruction
         */
        SequentialBlock sequBlock = (SequentialBlock) last.outer;
        StoreInstruction store = (StoreInstruction) ic.getInstruction();

        if (sequBlock.subBlocks[0] instanceof SpecialBlock) {

            SpecialBlock dup = (SpecialBlock) sequBlock.subBlocks[0];
            if (dup.type != SpecialBlock.DUP
                || dup.depth != store.getLValueOperandCount()
                || dup.count != store.getLValueType().stackSize())
                return false;
            
            dup.removeBlock();
            store.makeNonVoid();
            return true;
        }
        return false;
    }
}