/* 
 * CombineIfGotoExpressions (c) 1998 Jochen Hoenicke
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

package jode;
import java.util.Vector;

public class CombineIfGotoExpressions implements Transformation{

    public InstructionHeader transform(InstructionHeader ih2) {
        InstructionHeader ih1;
        Expression[] e;
        int operator;
        try {
            if (ih2.getFlowType() != ih2.IFGOTO)
                return null;
            InstructionHeader[] dests2 = ih2.getSuccessors();

            /* if ih2.getSimpleUniquePredecessor.getOperator().isVoid() XXX */

            Vector predec = ih2.getPredecessors();
            if (predec.size() != 1)
                return null;

            ih1 = (InstructionHeader) predec.elementAt(0);
            if (ih1.getFlowType() != ih1.IFGOTO)
                return null;
            InstructionHeader[] dests1 = ih1.getSuccessors();
            if (dests1[0] != ih2)
                return null;

            if (dests1[1] == dests2[0]) {
                e = new Expression[2];
                operator = Operator.LOG_AND_OP;
                e[1] = (Expression)ih2.getInstruction();
                e[0] = ((Expression)ih1.getInstruction()).negate();
            } else if (dests1[1] == dests2[1]) {
                e = new Expression[2];
                operator = Operator.LOG_OR_OP;
                e[1] = (Expression)ih2.getInstruction();
                e[0] = (Expression)ih1.getInstruction();
            } else
                return null;
        } catch (ClassCastException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }

        Expression cond = 
            new Expression(new BinaryOperator(MyType.tBoolean, operator), e);

	return ih1.combineConditional(cond);
    }
}