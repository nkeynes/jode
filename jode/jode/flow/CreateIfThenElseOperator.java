/* 
 * CreateIfThenElseOperator (c) 1998 Jochen Hoenicke
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
import jode.Expression;
import jode.ComplexExpression;
import jode.IfThenElseOperator;
import jode.CompareUnaryOperator;
import java.util.Enumeration;
import java.util.Vector;

public class CreateIfThenElseOperator implements Transformation {

    /**
     * This handles the more complicated form of the ?-:-operator used
     * in a conditional block.  The simplest case is:
     * <pre>
     *   if (cond)
     *       push e1
     *   else {
     *       IF (c2)
     *           GOTO flow_2_
     *       push 0
     *   }
     * -&gt;IF (stack_0 == 0)
     *     GOTO flow_1_
     *   GOTO flow_2_
     * </pre>
     * is transformed to
     * <pre>
     *   push cond ? e1 : c2
     * -&gt;IF (stack_0 == 0)
     *     GOTO flow_1_
     *   GOTO flow_2_
     * </pre>
     *
     * The <code>-&gt;</code> points to the lastModified block.  Note
     * that both the if and the then part may contain this
     * condition+push0-block.  There may be even stack if-then-else-blocks
     * for very complicated nested ?-:-Operators. <p>
     *
     * Also note that the produced code is suboptimal:  The push-0 could
     * sometimes be better replaced with a correct jump.
     * @param flow The FlowBlock that is transformed 
     */
    public boolean createFunny(FlowBlock flow) {

        if (!(flow.lastModified instanceof ConditionalBlock))
            return false;

        ConditionalBlock conditional = (ConditionalBlock) flow.lastModified;

        if (!(conditional.trueBlock instanceof EmptyBlock)
            || conditional.trueBlock.jump == null 
            || conditional.jump == null
            || !(conditional.getInstruction() instanceof CompareUnaryOperator))
            return false;
        
        CompareUnaryOperator compare = 
            (CompareUnaryOperator) conditional.getInstruction();

        FlowBlock trueDestination;
        if (compare.getOperatorIndex() == compare.EQUALS_OP)
            trueDestination = conditional.jump.destination;
        else if (compare.getOperatorIndex() == compare.NOTEQUALS_OP)
            trueDestination = conditional.trueBlock.jump.destination;
        else
            return false;

        Expression[] e = new Expression[3];
        IfThenElseBlock ifBlock;
        try {
            SequentialBlock sequBlock = 
                (SequentialBlock) conditional.outer;

            ifBlock = (IfThenElseBlock) sequBlock.subBlocks[0];

            while (true) {
                if (ifBlock.thenBlock instanceof IfThenElseBlock)
                    ifBlock = (IfThenElseBlock) ifBlock.thenBlock;
                else if (ifBlock.elseBlock instanceof IfThenElseBlock)
                    ifBlock = (IfThenElseBlock) ifBlock.elseBlock;
                else
                    break;
            }

            e[0] = ifBlock.cond;

            StructuredBlock[] subBlocks = ifBlock.getSubBlocks();
            if (subBlocks.length != 2)
                return false;

            for (int i=0; i< 2; i++) {
                if (subBlocks[i] instanceof InstructionBlock) {
                    e[i+1] = ((InstructionBlock)subBlocks[i]).getInstruction();
                    continue;
                }

                sequBlock = (SequentialBlock) subBlocks[i];
                ConditionalBlock condBlock = 
                    (ConditionalBlock) sequBlock.subBlocks[0];
                InstructionBlock pushBlock =
                    (InstructionBlock) sequBlock.subBlocks[1];
                
                jode.ConstOperator zero = 
                    (jode.ConstOperator) pushBlock.getInstruction();
                if (!zero.getValue().equals("0"))
                    return false;
                
                if (!(condBlock.trueBlock instanceof EmptyBlock)
                    || condBlock.trueBlock.jump == null
                    || condBlock.jump != null
                    || condBlock.trueBlock.jump.destination 
                    != trueDestination)
                    return false;

                Expression cond = condBlock.getInstruction();
                flow.removeSuccessor(condBlock.trueBlock.jump);
                condBlock.trueBlock.removeJump();
                pushBlock.setInstruction(cond);
                pushBlock.replace(sequBlock, pushBlock);

                e[i+1] = cond;
            }
        } catch (ClassCastException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }

        if (jode.Decompiler.isVerbose)
            System.err.print('?');

        IfThenElseOperator iteo = new IfThenElseOperator
            (e[1].getType().intersection(e[2].getType()));

        ((InstructionBlock)ifBlock.thenBlock).
            setInstruction(new ComplexExpression(iteo, e));
        ifBlock.thenBlock.replace(ifBlock, ifBlock.thenBlock);
        return true;
    }

    /**
     * This handles the normal form of the ?-:-operator:
     * <pre>
     *   if (cond)
     *       push e1
     *       GOTO flow_1_
     * -&gt;push e2
     *   GOTO flow_2
     * </pre>
     * is transformed to
     * <pre>
     * -&gt;push cond ? e1 : e2
     * </pre>
     * The <code>-&gt;</code> points to the lastModified block.
     * @param flow The FlowBlock that is transformed
     */
    public boolean create(FlowBlock flow) {
        Expression e[] = new Expression[3];
        InstructionBlock thenBlock;
        try {
            InstructionBlock elseBlock = (InstructionBlock) flow.lastModified;

            SequentialBlock sequBlock = (SequentialBlock)elseBlock.outer;
            if (sequBlock.subBlocks[1] != elseBlock)
                return false;

            IfThenElseBlock ifBlock = (IfThenElseBlock)sequBlock.subBlocks[0];
            if (ifBlock.elseBlock != null)
                return false;

            thenBlock = (InstructionBlock) ifBlock.thenBlock;

            if (thenBlock.jump.destination != elseBlock.jump.destination)
                return false;

            e[1] = thenBlock.getInstruction();
            if (e[1].isVoid())
                return false;
            e[2] = elseBlock.getInstruction();
            if (e[2].isVoid())
                return false;
            e[0] = ifBlock.cond;

        } catch (ClassCastException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }

        if (jode.Decompiler.isVerbose)
            System.err.print('?');

        flow.removeSuccessor(thenBlock.jump);
        thenBlock.removeJump();

        IfThenElseOperator iteo = new IfThenElseOperator
            (e[1].getType().intersection(e[2].getType()));

        ((InstructionBlock)flow.lastModified).
            setInstruction(new ComplexExpression(iteo, e));
        flow.lastModified.replace(flow.lastModified.outer, flow.lastModified);
        return true;
    }

    public boolean transform(FlowBlock flow) {
        return createFunny(flow) || create(flow);
    }
}
