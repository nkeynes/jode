/* InvokeOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.expr;
import jode.decompiler.CodeAnalyzer;
import jode.decompiler.ClassAnalyzer;
import jode.MethodType;
import jode.Decompiler;
import jode.Type;
import jode.bytecode.*;
import jode.jvm.*;

public final class InvokeOperator extends Operator 
    implements MatchableOperator {
    CodeAnalyzer codeAnalyzer;
    boolean staticFlag;
    boolean specialFlag;
    MethodType methodType;
    String methodName;
    ClassInfo clazz;

    public InvokeOperator(CodeAnalyzer codeAnalyzer,
			  boolean staticFlag, boolean specialFlag, 
			  Reference reference) {
        super(Type.tUnknown, 0);
        this.methodType = (MethodType) Type.tType(reference.getType());
        this.methodName = reference.getName();
        this.clazz = ClassInfo.forName(reference.getClazz());
        this.type = methodType.getReturnType();
        this.codeAnalyzer  = codeAnalyzer;
	this.staticFlag = staticFlag;
        this.specialFlag = specialFlag;
        if (staticFlag)
            Type.tClass(clazz.getName()).useType();
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
     * Checks if the value of the operator can be changed by this expression.
     */
    public boolean matches(Operator loadop) {
        return (loadop instanceof InvokeOperator
		|| loadop instanceof ConstructorOperator
		|| loadop instanceof GetFieldOperator);
    }

    public final boolean isStatic() {
        return staticFlag;
    }

    public MethodType getMethodType() {
        return methodType;
    }

    public String getMethodName() {
        return methodName;
    }

    public Type getClassType() {
        return Type.tClass(clazz.getName());
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandCount() {
        return (isStatic()?0:1) 
            + methodType.getParameterTypes().length;
    }

    public int getOperandPriority(int i) {
        if (!isStatic() && i == 0)
            return 950;
        return 0;
    }

    public Type getOperandType(int i) {
        if (!isStatic()) {
            if (i == 0)
                return getClassType();
            i--;
        }
        return methodType.getParameterTypes()[i];
    }

    public void setOperandType(Type types[]) {
    }

    public boolean isConstructor() {
        return methodName.equals("<init>");
    }

    /**
     * Checks, whether this is a call of a method from this class.
     * @XXX check, if this class implements the method and if not
     * allow super class
     */
    public boolean isThis() {
        return (clazz == codeAnalyzer.getClazz());
    }

    /**
     * Checks, whether this is a call of a method from the super class.
     * @XXX check, if its the first super class that implements the method.
     */
    public boolean isSuperOrThis() {
        return clazz.superClassOf(codeAnalyzer.getClazz());
    }

    public String toString(String[] operands) {
        String object = specialFlag 
            ? (operands[0].equals("this") 
               ? (/* XXX check if this is a private or final method. */
                  isThis() ? "" : "super")
               : (/* XXX check if this is a private or final method. */
                  isThis() ? operands[0] : "NON VIRTUAL " + operands[0]))
            : (isStatic()
               ? (isThis() ? "" : clazz.toString())
               : (operands[0].equals("this") ? "" 
		  : operands[0].equals("null") 
		  ? "((" + clazz.getName() + ") null)"
		  : operands[0]));

        int arg = isStatic() ? 0 : 1;
        String method = isConstructor() 
            ? (object.length() == 0 ? "this" : object)
            : (object.length() == 0 ? methodName : object + "." + methodName);

        StringBuffer params = new StringBuffer();
        for (int i=0; i < methodType.getParameterTypes().length; i++) {
            if (i>0)
                params.append(", ");
            params.append(operands[arg++]);
        }
        return method+"("+params+")";
    }

    /**
     * Checks if the method is the magic class$ method.
     * @return true if this is the magic class$ method, false otherwise.
     */
    public boolean isGetClass() {
	return isThis() 
	    && codeAnalyzer.getClassAnalyzer()
	    .getMethod(methodName, methodType).isGetClass();
    }

    public ConstOperator deobfuscateString(ConstOperator op) {
	if (!isThis() || !isStatic()
	    || methodType.getParameterTypes().length != 1
	    || !methodType.getParameterTypes()[0].equals(Type.tString)
	    || !methodType.getReturnType().equals(Type.tString))
	    return null;
	ClassAnalyzer clazz = codeAnalyzer.getClassAnalyzer();
	CodeAnalyzer ca = clazz.getMethod(methodName, methodType).getCode();
	if (ca == null)
	    return null;
	BytecodeInfo info = ca.getBytecodeInfo();
	Value[] locals = new Value[info.getMaxLocals()];
	for (int i=0; i< locals.length; i++)
	    locals[i] = new Value();
	Value[] stack = new Value[info.getMaxStack()];
	for (int i=0; i< stack.length; i++)
	    stack[i] = new Value();
	locals[0].setObject(op.getValue());
	String result;
	try {
	    result = (String) Interpreter.interpretMethod
		(clazz, info, locals, stack);
	} catch (InterpreterException ex) {
	    Decompiler.err.println("Warning: Can't interpret method "
				   +methodName);
	    ex.printStackTrace(Decompiler.err);
	    return null;
	} catch (ClassFormatException ex) {
	    ex.printStackTrace(Decompiler.err);
	    return null;
	}
	return new ConstOperator(result);
    }

    /* Invokes never equals: they may return different values even if
     * they have the same parameters.
     */
}
