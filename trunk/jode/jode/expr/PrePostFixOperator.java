/* 
 * PrePostFixOperator (c) 1998 Jochen Hoenicke
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

package jode.expr;
import jode.Type;

public class PrePostFixOperator extends Operator {
    StoreInstruction store;
    boolean postfix;

    public PrePostFixOperator(Type type, int op, 
                              StoreInstruction store, boolean postfix) {
        super(type, op);
	this.store = store;
        this.postfix = postfix;
    }
    
    public int getPriority() {
        return postfix ? 800 : 700;
    }

    public int getOperandPriority(int i) {
        return getPriority();
    }

    public Type getOperandType(int i) {
	return store.getLValueOperandType(i);
    }

    public int getOperandCount() {
        return store.getLValueOperandCount();
    }

    /**
     * Checks if the value of the given expression can change, due to
     * side effects in this expression.  If this returns false, the 
     * expression can safely be moved behind the current expresion.
     * @param expr the expression that should not change.
     */
    public boolean hasSideEffects(Expression expr) {
	return store.hasSideEffects(expr);
    }

    /**
     * Sets the return type of this operator.
     */
    public void setType(Type type) {
        store.setLValueType(type);
        super.setType(store.getLValueType());
    }

    public void setOperandType(Type[] inputTypes) {
        store.setLValueOperandType(inputTypes);
    }

    public String toString(String[] operands) {
        if (postfix)
            return store.getLValueString(operands) + getOperatorString();
        else
            return getOperatorString() + store.getLValueString(operands);
    }
}