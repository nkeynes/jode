/* 
 * StoreInstruction (c) 1998 Jochen Hoenicke
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

public abstract class StoreInstruction extends Operator
    implements CombineableOperator {

    public String lvCasts;
    Type lvalueType;
    Type rvalueType = null;

    public StoreInstruction(Type type, int operator) {
        super(Type.tVoid, operator);
        lvalueType = type;
        lvCasts = lvalueType.toString();
    }

    public void makeOpAssign(int operator, Type rvalueType) {
	setOperatorIndex(operator);
	this.rvalueType = rvalueType;
    }

    public Type getType() {
        return type == Type.tVoid ? type : getLValueType();
    }

    public Type getLValueType() {
        return lvalueType;
    }

    /**
     * Checks if the value of the given expression can change, due to
     * side effects in this expression.  If this returns false, the 
     * expression can safely be moved behind the current expresion.
     * @param expr the expression that should not change.
     */
    public boolean hasSideEffects(Expression expr) {
	return expr.containsConflictingLoad(this);
    }

    /**
     * Makes a non void expression out of this store instruction.
     */
    public void makeNonVoid() {
        if (type != Type.tVoid)
            throw new jode.AssertError("already non void");
        type = lvalueType;
        if (parent != null && parent.getOperator() == this)
            parent.type = lvalueType;
    }

    public abstract boolean matches(Operator loadop);
    public abstract int getLValueOperandCount();
    public abstract int getLValueOperandPriority(int i);
    public abstract Type getLValueOperandType(int i);
    public abstract void setLValueOperandType(Type [] t);

    /**
     * Sets the type of the lvalue (and rvalue).
     */
    public void setLValueType(Type type) {
        lvalueType = lvalueType.intersection(type);
    }

    public abstract String getLValueString(String[] operands);

    public int getPriority() {
        return 100;
    }

    public int getOperandPriority(int i) {
        if (i == getLValueOperandCount())
            return 100;
        else
            return getLValueOperandPriority(i);
    }

    public Type getOperandType(int i) {
        if (i == getLValueOperandCount()) {
	    if (getOperatorIndex() == ASSIGN_OP)
		/* In a direct assignment, lvalueType is rvalueType */
		return getLValueType(); 
	    else
		return rvalueType;
        } else
            return getLValueOperandType(i);
    }

    public void setOperandType(Type[] t) {
        int count = getLValueOperandCount();
        if (count > 0)
            setLValueOperandType(t);
	if (getOperatorIndex() == ASSIGN_OP)
	    /* In a direct assignment, lvalueType is rvalueType */
	    setLValueType(t[count]);
	else
	    rvalueType = rvalueType.intersection(t[count]);
    }

    public int getOperandCount() {
        return 1 + getLValueOperandCount();
    }

    public String toString(String[] operands)
    {
        return getLValueString(operands) + getOperatorString() +
            operands[getLValueOperandCount()];
    }
}