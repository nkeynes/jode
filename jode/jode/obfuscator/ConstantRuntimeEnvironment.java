/* ConstantRuntimeEnvironment Copyright (C) 1999 Jochen Hoenicke.
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

package jode.obfuscator;
import jode.jvm.*;
import jode.bytecode.Reference;
import jode.bytecode.BytecodeInfo;
import jode.type.*;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ConstantRuntimeEnvironment extends SimpleRuntimeEnvironment {

    /**
     * The references that may be used in constant methods.
     */
///#ifdef JDK12
///    static Set whiteList = new HashSet();
///
///    static void addWhite(Reference ref) {
///	/* note that this gets inlined */
///	whiteList.add(ref);
///    }
///
///    public static boolean isWhite(Reference ref) {
///	return whiteList.contains(ref);
///    }
///#else
    static Hashtable whiteList = new Hashtable();

    static void addWhite(Reference ref) {
	/* note that this gets inlined */
	whiteList.put(ref, ref);
    }

    public static boolean isWhite(Reference ref) {
	return whiteList.containsKey(ref);
    }
///#endif

    static {
	addWhite(new Reference("Ljava/lang/String;", "toCharArray", "()[C"));
	addWhite(new Reference("Ljava/lang/StringBuffer;", "<init>", 
			       "(Ljava/lang/String;)V"));
	addWhite(new Reference("Ljava/lang/StringBuffer;", "append", 
			     "(Ljava/lang/String;)Ljava/lang/StringBuffer;"));
	addWhite(new Reference("Ljava/lang/StringBuffer;", "append", 
			       "(C)Ljava/lang/StringBuffer;"));
	addWhite(new Reference("Ljava/lang/StringBuffer;", "append", 
			       "(B)Ljava/lang/StringBuffer;"));
	addWhite(new Reference("Ljava/lang/StringBuffer;", "append", 
			       "(S)Ljava/lang/StringBuffer;"));
	addWhite(new Reference("Ljava/lang/StringBuffer;", "append", 
			       "(Z)Ljava/lang/StringBuffer;"));
	addWhite(new Reference("Ljava/lang/StringBuffer;", "append", 
			       "(F)Ljava/lang/StringBuffer;"));
	addWhite(new Reference("Ljava/lang/StringBuffer;", "append", 
			       "(I)Ljava/lang/StringBuffer;"));
	addWhite(new Reference("Ljava/lang/StringBuffer;", "append", 
			       "(J)Ljava/lang/StringBuffer;"));
	addWhite(new Reference("Ljava/lang/StringBuffer;", "append", 
			       "(D)Ljava/lang/StringBuffer;"));
	addWhite(new Reference("Ljava/lang/StringBuffer;", "toString", 
			       "()Ljava/lang/String;"));
	addWhite(new Reference("Ljava/lang/String;", "<init>", "()V"));
	addWhite(new Reference("Ljava/lang/String;", "<init>", "([C)V"));
	addWhite(new Reference("Ljava/lang/String;", "<init>", "([CII)V"));
	addWhite(new Reference("Ljava/lang/String;", "<init>", 
			       "(Ljava/lang/String;)V"));
	addWhite(new Reference("Ljava/lang/String;", "<init>", 
			       "(Ljava/lang/StringBuffer;)V"));
	addWhite(new Reference("Ljava/lang/String;", "length", "()I"));
	addWhite(new Reference("Ljava/lang/String;", "replace", 
			       "(CC)Ljava/lang/String;"));
	addWhite(new Reference("Ljava/lang/String;", "valueOf", 
			       "(Z)Ljava/lang/String;"));
	addWhite(new Reference("Ljava/lang/String;", "valueOf", 
			       "(B)Ljava/lang/String;"));
	addWhite(new Reference("Ljava/lang/String;", "valueOf", 
			       "(S)Ljava/lang/String;"));
	addWhite(new Reference("Ljava/lang/String;", "valueOf", 
			       "(C)Ljava/lang/String;"));
	addWhite(new Reference("Ljava/lang/String;", "valueOf", 
			       "(D)Ljava/lang/String;"));
	addWhite(new Reference("Ljava/lang/String;", "valueOf", 
			       "(F)Ljava/lang/String;"));
	addWhite(new Reference("Ljava/lang/String;", "valueOf", 
			       "(I)Ljava/lang/String;"));
	addWhite(new Reference("Ljava/lang/String;", "valueOf", 
			       "(J)Ljava/lang/String;"));
	addWhite(new Reference("Ljava/lang/String;", "valueOf", 
			       "(Ljava/lang/Object;)Ljava/lang/String;"));
	addWhite(new Reference("Ljava/lang/String;", "substring", 
			       "(I)Ljava/lang/String;"));
	addWhite(new Reference("Ljava/lang/String;", "substring", 
			       "(II)Ljava/lang/String;"));
	addWhite(new Reference("Ljava.lang/reflect/Modifier;", "toString", 
			       "(I)Ljava/lang/String;"));
	addWhite(new Reference("Ljava/lang/Math;", "abs", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "abs", "(F)F"));
	addWhite(new Reference("Ljava/lang/Math;", "abs", "(I)I"));
	addWhite(new Reference("Ljava/lang/Math;", "abs", "(J)J"));
	addWhite(new Reference("Ljava/lang/Math;", "acos", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "asin", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "atan", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "atan2", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "ceil", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "cos", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "exp", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "floor", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "IEEEremainder", "(DD)D"));
	addWhite(new Reference("Ljava/lang/Math;", "log", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "max", "(DD)D"));
	addWhite(new Reference("Ljava/lang/Math;", "max", "(FF)F"));
	addWhite(new Reference("Ljava/lang/Math;", "max", "(II)I"));
	addWhite(new Reference("Ljava/lang/Math;", "max", "(JJ)J"));
	addWhite(new Reference("Ljava/lang/Math;", "min", "(DD)D"));
	addWhite(new Reference("Ljava/lang/Math;", "min", "(FF)F"));
	addWhite(new Reference("Ljava/lang/Math;", "min", "(II)I"));
	addWhite(new Reference("Ljava/lang/Math;", "min", "(JJ)J"));
	addWhite(new Reference("Ljava/lang/Math;", "pow", "(DD)D"));
	addWhite(new Reference("Ljava/lang/Math;", "rint", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "round", "(D)J"));
	addWhite(new Reference("Ljava/lang/Math;", "round", "(F)I"));
	addWhite(new Reference("Ljava/lang/Math;", "sin", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "sqrt", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "tan", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "toDegrees", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "toRadians", "(D)D"));
	addWhite(new Reference("Ljava/lang/Math;", "E", "D"));
	addWhite(new Reference("Ljava/lang/Math;", "PI", "D"));
    }

    MethodIdentifier m;
    public ConstantRuntimeEnvironment(MethodIdentifier method) {
	m = method;
    }

    public Object getField(Reference ref, Object obj)
	throws InterpreterException {
	if (isWhite(ref))
	    return super.getField(ref, obj);
	Type type = Type.tType(ref.getType());
	FieldIdentifier fi = (FieldIdentifier) 
	    m.clazz.bundle.getIdentifier(ref);
	if (fi != null && !fi.isNotConstant()) {
	    Object result = fi.getConstant();
	    if (result == null)
		result = type.getDefaultValue();
		return result;
	}
	throw new InterpreterException("Field " + ref + " not constant");
    }

    public void putField(Reference ref, Object obj, Object value)
	throws InterpreterException {
	throw new InterpreterException("Modifiing Field " + ref + ".");
    }
    
    public Object invokeConstructor(Reference ref, Object[] params)
	throws InterpreterException, InvocationTargetException {
	if (isWhite(ref))
	    return super.invokeConstructor(ref, params);
	throw new InterpreterException("Creating new Object " + ref + ".");
    }
    
    public Object invokeMethod(Reference ref, boolean isVirtual, 
			       Object cls, Object[] params) 
	throws InterpreterException, InvocationTargetException {
	if (isWhite(ref))
	    return super.invokeMethod(ref, isVirtual, cls, params);
	Type type = Type.tType(ref.getType());
	MethodIdentifier mi = (MethodIdentifier) 
	    m.clazz.bundle.getIdentifier(ref);
	if (mi != null) {
	    BytecodeInfo code = mi.info.getBytecode();
	    if (code != null) {
		MethodType mt = (MethodType) Type.tType(ref.getType());
		Value[] locals = new Value[code.getMaxLocals()];
		for (int i=0; i< locals.length; i++)
		    locals[i] = new Value();
		int slot = 0;
		if (cls != null)
		    locals[slot++].setObject(cls);
		for (int i = 0; i < params.length; i++) {
		    locals[slot].setObject(params[i]);
		    slot += mt.getParameterTypes()[i].stackSize();
		}
		return Interpreter.interpretMethod(this, code, locals);
	    }
	}
	throw new InterpreterException("Invoking library method " + ref + ".");
    }
    
    public boolean instanceOf(Object obj, String className)
	throws InterpreterException {
	Class clazz;
	try {
	    clazz = Class.forName(className);
	} catch (ClassNotFoundException ex) {
	    throw new InterpreterException
		("Class "+ex.getMessage()+" not found");
	}
	return obj != null && !clazz.isInstance(obj);
    }

    public Object newArray(String type, int[] dimensions)
	throws InterpreterException, NegativeArraySizeException {
	if (type.length() == 1) {
	    Class clazz;
	    try {
		clazz = Class.forName(type);
	    } catch (ClassNotFoundException ex) {
		throw new InterpreterException
		    ("Class "+ex.getMessage()+" not found");
	    }
	    return Array.newInstance(clazz, dimensions);
	}
	throw new InterpreterException("Creating object array.");
    }

    public void enterMonitor(Object obj)
	throws InterpreterException {
	throw new InterpreterException("monitorenter not implemented");
    }
    public void exitMonitor(Object obj)
	throws InterpreterException {
	throw new InterpreterException("monitorenter not implemented");
    }
}