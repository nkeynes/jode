/* 
 * Expression (c) 1998 Jochen Hoenicke
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

public class ComplexExpression extends Expression {
    Operator     operator;
    Expression[] subExpressions;

    public ComplexExpression(Operator op, Expression[] sub) {
        super(Type.tUnknown);
        if (sub.length != op.getOperandCount())
            throw new AssertError ("Operand count mismatch: "+
                                   sub.length + " != " + 
                                   op.getOperandCount());
        operator = op;
        operator.parent = this;
        subExpressions = sub;
        for (int i=0; i< subExpressions.length; i++)
            subExpressions[i].parent = this;
        updateType();
    }

    public int getOperandCount() {
        if (subExpressions.length == 0)
            return 0;
        else
            /* The only sub expression that may have non resolved
             * operands may be the first.
             */
            return subExpressions[0].getOperandCount();
    }

    public Expression negate() {
        if (operator.getOperatorIndex() >= operator.COMPARE_OP && 
            operator.getOperatorIndex() < operator.COMPARE_OP+6) {
            operator.setOperatorIndex(operator.getOperatorIndex() ^ 1);
            return this;
        } else if (operator.getOperatorIndex() == operator.LOG_AND_OP || 
                   operator.getOperatorIndex() == operator.LOG_OR_OP) {
            operator.setOperatorIndex(operator.getOperatorIndex() ^ 1);
            for (int i=0; i< subExpressions.length; i++) {
                subExpressions[i] = subExpressions[i].negate();
                subExpressions[i].parent = this;
            }
            return this;
        } else if (operator.operator == operator.LOG_NOT_OP) {
            return subExpressions[0];
        }

        Operator negop = 
            new UnaryOperator(Type.tBoolean, Operator.LOG_NOT_OP);
        return new ComplexExpression(negop, new Expression[] { this });
    }

    /**
     * Checks if the given Expression (which should be a StoreInstruction)
     * can be combined into this expression.
     * @param e The store expression.
     * @return 1, if it can, 0, if no match was found and -1, if a
     * conflict was found.  You may wish to check for >0.
     */
    public int canCombine(Expression e) {
	if (e instanceof ComplexExpression
            && e.getOperator() instanceof StoreInstruction) {
            ComplexExpression ce = (ComplexExpression) e;
	    StoreInstruction store = (StoreInstruction) e.getOperator();
	    if (store.matches(operator)) {
		for (int i=0; i < ce.subExpressions.length-1; i++) {
		    if (!ce.subExpressions[i].equals(subExpressions[i]))
			return -1;
		}
                return 1;
	    }
	    for (int i=0; i < subExpressions.length; i++) {
		int can = subExpressions[i].canCombine(e);
		if (can != 0)
                    return can;
	    }
	}
	return 0;
    }
    
    /**
     * Combines the given Expression (which should be a StoreInstruction)
     * into this expression.  You must only call this if
     * canCombine returns the value 1.
     * @param e The store expression.
     * @return The combined expression.
     */
    public Expression combine(Expression e) {

        StoreInstruction store = (StoreInstruction) e.getOperator();
        if (store.matches(operator)) {
            ((ComplexExpression) e).operator = 
                new AssignOperator(store.getOperatorIndex(), store);
            this.subExpressions = ((ComplexExpression) e).subExpressions;
            return e;
        }
        for (int i=0; i < subExpressions.length; i++) {
            Expression combined = subExpressions[i].combine(e);
            if (combined != null) {
                subExpressions[i] = combined;
                subExpressions[i].parent = this;
                return this;
            }
        }
        throw new AssertError("combine didn't succeed");
    }

    public Operator getOperator() {
        return operator;
    }

    public Expression[] getSubExpressions() {
        return subExpressions;
    }

    void updateSubTypes() {
        for (int i=0; i < subExpressions.length; i++) {
            if (i == 0 && operator instanceof ArrayStoreOperator) {
                /* No rule without exception:
                 * We can always use tSubType, except for the
                 * array operand of an array store instruction.
                 */
                subExpressions[i].setType(operator.getOperandType(i));
            } else
                subExpressions[i].setType
                    (Type.tSubType(operator.getOperandType(i)));
        }
    }

    public void updateType() {
        if (subExpressions.length > 0) {
            Type types[] = new Type[subExpressions.length];
            while (true) {
                boolean changed = false;
                updateSubTypes();
                for (int i=0; i < types.length; i++) {
                    if (i == 0 && operator instanceof ArrayStoreOperator) {
                        /* No rule without exception:
                         * We can always use tSuperType, except for the
                         * array operand of an array store instruction.
                         */
                        types[i] = subExpressions[i].getType();
                    } else
                        types[i] = Type.tSuperType
                            (subExpressions[i].getType());
                    types[i] = 
                        types[i].intersection(operator.getOperandType(i));
                    if (!types[i].equals(operator.getOperandType(i))) {
                        if (Decompiler.isTypeDebugging)
                            System.err.println("change in "+this+": "
                                               +operator.getOperandType(i)
                                               +"->"+types[i]);
                        changed = true;
                    }
                }
                if (changed)
                    operator.setOperandType(types);
                else
                    break;
            }
        }
        setType(operator.getType());
    }

    public void setType(Type newType) {
	newType = type.intersection(newType);
        if (!newType.equals(type)) {
            type = newType;
            operator.setType(type);
            updateSubTypes();
            if (parent != null)
                parent.updateType();
        }
    }

    public boolean isVoid() {
        return operator.getType() == Type.tVoid;
    }

    public String toString() {
        String[] expr = new String[subExpressions.length];
        for (int i=0; i<subExpressions.length; i++) {
            expr[i] = subExpressions[i].
                toString(Decompiler.isTypeDebugging 
                         ? 700 /* type cast priority */
                         : operator.getOperandPriority(i));
            if (Decompiler.isTypeDebugging) {
                expr[i] = "("+
                    (operator.getOperandType(i)
                     .equals(subExpressions[i].getType())
                     ? "" : ""+subExpressions[i].getType()+"->")
                    + operator.getOperandType(i)+") "+expr[i];
                if (700 < operator.getOperandPriority(i))
                    expr[i] = "(" + expr[i] + ")";
            }
            else if (subExpressions[i].getType() == Type.tError)
                expr[i] = "(/*type error */" + expr[i]+")";
        }
        return operator.toString(expr);
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
	if (!(o instanceof ComplexExpression))
	    return false;
	ComplexExpression expr = (ComplexExpression) o;
        if (!operator.equals(expr.operator) ||
            subExpressions.length != expr.subExpressions.length)
            return false;

        for (int i=0; i<subExpressions.length; i++) {
            if (!subExpressions[i].equals(expr.subExpressions[i]))
                return false;
        }
        return true;
    }

    static Expression emptyString = 
        new EmptyStringOperator();

    Expression simplifyStringBuffer() {
        gnu.bytecode.CpoolRef field;
        if (operator instanceof InvokeOperator
            && ((field = ((InvokeOperator)operator).getField())
                .getCpoolClass().getName().getString()
                .replace(java.io.File.separatorChar, '.')
                .equals("java.lang.StringBuffer"))
            && !((InvokeOperator)operator).isStatic() &&
            field.getNameAndType().getName().getString().equals("append") &&
            ((InvokeOperator)operator).getMethodType().getArgumentTypes().length == 1) {

            Expression e = subExpressions[0].simplifyStringBuffer();
            if (e == null)
                return null;
            
            if (e.getOperator() instanceof EmptyStringOperator &&
                subExpressions[1].getType().isOfType(Type.tString))
                return subExpressions[1];

            return new ComplexExpression
                (new StringAddOperator(), new Expression[] 
                 { e, subExpressions[1].simplify() });
        }
        if (operator instanceof ConstructorOperator &&
            operator.getType().isOfType(Type.tStringBuffer)) {
            /* subExpressions[0] is always a "new StringBuffer" */
            if (subExpressions.length == 1)
                return emptyString;
            else if (subExpressions.length == 2 &&
                     subExpressions[1].getType().isOfType(Type.tString))
                return subExpressions[1].simplify();
        }
        return null;
    }

    public Expression simplify() {
        if (operator instanceof IfThenElseOperator &&
            operator.getType().isOfType(Type.tBoolean)) {
            if (subExpressions[1].getOperator() instanceof ConstOperator &&
                subExpressions[2].getOperator() instanceof ConstOperator) {
                ConstOperator c1 = 
                    (ConstOperator) subExpressions[1].getOperator();
                ConstOperator c2 = 
                    (ConstOperator) subExpressions[2].getOperator();
                if (c1.getValue().equals("1") &&
                    c2.getValue().equals("0"))
                    return subExpressions[0].simplify();
                if (c2.getValue().equals("1") &&
                    c1.getValue().equals("0"))
                    return subExpressions[0].negate().simplify();
            }
        }
        else if ((operator instanceof AssignOperator ||
                  operator instanceof StoreInstruction) &&
                 subExpressions[subExpressions.length-1]
                 .getOperator() instanceof ConstOperator) {

            StoreInstruction store;
            if (operator instanceof AssignOperator)
                store = ((AssignOperator)operator).getStore();
            else
                store = (StoreInstruction)operator;

            ConstOperator one = (ConstOperator) 
                subExpressions[subExpressions.length-1].getOperator();

            if ((operator.getOperatorIndex() == 
                 operator.OPASSIGN_OP+operator.ADD_OP ||
                 operator.getOperatorIndex() == 
                 operator.OPASSIGN_OP+operator.NEG_OP) &&
                (one.getValue().equals("1") || 
                 one.getValue().equals("-1"))) {

                int op = ((operator.getOperatorIndex() == 
                           operator.OPASSIGN_OP+operator.ADD_OP) ==
                          one.getValue().equals("1"))?
                    operator.INC_OP : operator.DEC_OP;

                Operator ppfixop = new PrePostFixOperator
                    (store.getLValueType(), op, store, 
                     operator instanceof StoreInstruction);
                if (subExpressions.length == 1)
                    return ppfixop.simplify();

                operator = ppfixop;
                ppfixop.parent = this;
            }
        }
        else if (operator instanceof CompareUnaryOperator &&
            subExpressions[0].getOperator() instanceof CompareToIntOperator) {
            
            CompareBinaryOperator newOp = new CompareBinaryOperator
                (subExpressions[0].getOperator().getOperandType(0),
                 operator.getOperatorIndex());
            
            if (subExpressions[0] instanceof ComplexExpression) {
                return new ComplexExpression
                    (newOp, 
                     ((ComplexExpression)subExpressions[0]).subExpressions).
                    simplify();
            } else
                return newOp.simplify();
        }
        else if (operator instanceof CompareUnaryOperator &&
            operator.getOperandType(0).isOfType(Type.tBoolean)) {
            /* xx == false */
            if (operator.getOperatorIndex() == operator.EQUALS_OP) 
                return subExpressions[0].negate().simplify();
            /* xx != false */
            if (operator.getOperatorIndex() == operator.NOTEQUALS_OP)
                return subExpressions[0].simplify();
        }
        else if (operator instanceof InvokeOperator
            && (((InvokeOperator)operator).getField()
                .getNameAndType().getName().getString().equals("toString"))
            && !((InvokeOperator)operator).isStatic()
            && (((InvokeOperator)operator).getField()
                .getCpoolClass().getName().getString()
                .replace(java.io.File.separatorChar, '.')
                .equals("java.lang.StringBuffer"))
            && subExpressions.length == 1) {
            Expression simple = subExpressions[0].simplifyStringBuffer();
            if (simple != null)
                return simple;
        }
        else if (operator instanceof InvokeOperator
            && (((InvokeOperator)operator).getField()
                .getNameAndType().getName().getString().equals("valueOf"))
            && ((InvokeOperator)operator).isStatic() 
            && (((InvokeOperator)operator).getField()
                .getCpoolClass().getName().getString()
                .replace(java.io.File.separatorChar, '.')
                .equals("java.lang.String"))
            && subExpressions.length == 1) {
            if (subExpressions[0].getType() == Type.tString)
                return subExpressions[0].simplify();

            return new ComplexExpression
                (new StringAddOperator(), new Expression[] 
                 { emptyString, subExpressions[0].simplify() });
        }
        for (int i=0; i< subExpressions.length; i++) {
            subExpressions[i] = subExpressions[i].simplify();
            subExpressions[i].parent = this;
        }
        return this;
    }
}
