/* Type Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.type;
import jode.AssertError;
import jode.GlobalOptions;
import jode.bytecode.ClassInfo;
///#ifdef JDK12
///import java.lang.ref.WeakReference;
///import java.lang.ref.ReferenceQueue;
///import java.util.Map;
///import java.util.HashMap;
///#else
import java.util.Hashtable;
///#endif

/**
 * This is my type class.  It differs from java.lang.class, in that it
 * represents a set of types.  Since most times this set is infinite, it
 * needs a special representation. <br>
 *
 * The main operation on a type sets are tSuperType, tSubType and
 * intersection.
 *
 * @author Jochen Hoenicke */
public class Type {
    public static final int TC_BOOLEAN = 0;
    public static final int TC_BYTE = 1;
    public static final int TC_CHAR = 2;
    public static final int TC_SHORT = 3;
    public static final int TC_INT = 4;
    public static final int TC_LONG = 5;
    public static final int TC_FLOAT = 6;
    public static final int TC_DOUBLE = 7;
    public static final int TC_NULL = 8;
    public static final int TC_ARRAY = 9;
    public static final int TC_CLASS = 10;
    public static final int TC_VOID = 11;
    public static final int TC_METHOD = 12;
    public static final int TC_ERROR = 13;
    public static final int TC_UNKNOWN = 101;
    public static final int TC_RANGE = 103;
    public static final int TC_INTEGER = 107;

///#ifdef JDK12
///    private static final Map classHash = new HashMap();
///    private static final ReferenceQueue classQueue = new ReferenceQueue();
///    private static final Map arrayHash = new HashMap();    
///    private static final ReferenceQueue arrayQueue = new ReferenceQueue();
///    private static final Map methodHash = new HashMap();    
///    private static final ReferenceQueue methodQueue = new ReferenceQueue();
///#else
    private static final Hashtable classHash = new Hashtable();
    private static final Hashtable arrayHash = new Hashtable();    
    private static final Hashtable methodHash = new Hashtable();    
///#endif

    /**
     * This type represents the singleton set containing the boolean type.
     */
    public static final Type tBoolean = new IntegerType(IntegerType.IT_Z);
    /**
     * This type represents the singleton set containing the byte type.
     */
    public static final Type tByte    = new IntegerType(IntegerType.IT_B);
    /**
     * This type represents the singleton set containing the char type.
     */
    public static final Type tChar    = new IntegerType(IntegerType.IT_C);
    /**
     * This type represents the singleton set containing the short type.
     */
    public static final Type tShort   = new IntegerType(IntegerType.IT_S);
    /**
     * This type represents the singleton set containing the int type.
     */
    public static final Type tInt     = new IntegerType(IntegerType.IT_I);
    /**
     * This type represents the singleton set containing the long type.
     */
    public static final Type tLong    = new Type(TC_LONG);
    /**
     * This type represents the singleton set containing the float type.
     */
    public static final Type tFloat   = new Type(TC_FLOAT);
    /**
     * This type represents the singleton set containing the double type.
     */
    public static final Type tDouble  = new Type(TC_DOUBLE);
    /**
     * This type represents the void type.  It is really not a type at
     * all.  
     */
    public static final Type tVoid    = new Type(TC_VOID);
    /**
     * This type represents the empty set, and probably means, that something
     * has gone wrong.
     */
    public static final Type tError   = new Type(TC_ERROR);
    /**
     * This type represents the set of all possible types.
     */
    public static final Type tUnknown = new Type(TC_UNKNOWN);
    /**
     * This type represents the set of all integer types, up to 32 bit.
     */
    public static final Type tUInt    = new IntegerType(IntegerType.IT_I
							| IntegerType.IT_B
							| IntegerType.IT_C
							| IntegerType.IT_S);
    /**
     * This type represents the set of the boolean and int type.
     */
    public static final Type tBoolInt = new IntegerType(IntegerType.IT_I
							| IntegerType.IT_Z);
    /**
     * This type represents the set of boolean and all integer types,
     * up to 32 bit.  
     */
    public static final Type tBoolUInt= new IntegerType(IntegerType.IT_I
							| IntegerType.IT_B
							| IntegerType.IT_C
							| IntegerType.IT_S
							| IntegerType.IT_Z);
    /**
     * This type represents the set of the boolean and byte type.
     */
    public static final Type tBoolByte= new IntegerType(IntegerType.IT_B
							| IntegerType.IT_Z);
    /**
     * This type represents the singleton set containing 
     * <code>java.lang.Object</code>.
     */
    public static final ClassInterfacesType tObject = 
	tClass("java.lang.Object");
    /**
     * This type represents the singleton set containing the special 
     * null type (the type of null).
     */
    public static final ReferenceType tNull = new NullType();
    /**
     * This type represents the set of all reference types, including
     * class types, array types, interface types and the null type.  
     */
    public static final Type tUObject = tRange(tObject, tNull);
    /**
     * This type represents the singleton set containing 
     * <code>java.lang.String</code>.
     */
    public static final Type tString  = tClass("java.lang.String");
    /**
     * This type represents the singleton set containing 
     * <code>java.lang.StringBuffer</code>.
     */
    public static final Type tStringBuffer = tClass("java.lang.StringBuffer");
    /**
     * This type represents the singleton set containing 
     * <code>java.lang.Class</code>.
     */
    public static final Type tJavaLangClass = tClass("java.lang.Class");

    /**
     * This is a private method for generating the signature of a
     * given type.  
     */
    private static final StringBuffer appendSignature(StringBuffer sb,
						      Class javaType) {
	if (javaType.isPrimitive()) {
	    if (javaType == Boolean.TYPE)
		return sb.append('Z');
	    else if (javaType == Byte.TYPE)
		return sb.append('B');
	    else if (javaType == Character.TYPE)
		return sb.append('C');
	    else if (javaType == Short.TYPE)
		return sb.append('S');
	    else if (javaType == Integer.TYPE)
		return sb.append('I');
	    else if (javaType == Long.TYPE)
		return sb.append('J');
	    else if (javaType == Float.TYPE)
		return sb.append('F');
	    else if (javaType == Double.TYPE)
		return sb.append('D');
	    else if (javaType == Void.TYPE)
		return sb.append('V');
	    else
		throw new AssertError("Unknown primitive type: "+javaType);
	} else if (javaType.isArray()) {
	    return appendSignature(sb.append('['), 
				   javaType.getComponentType());
	} else {
	    return sb.append('L')
		.append(javaType.getName().replace('.','/')).append(';');
	}
    }

    /**
     * Generate the signature for the given Class.
     * @param clazz a java.lang.Class, this may also be a primitive or
     * array type.
     * @return the type signature (see section 4.3.2 Field Descriptors
     * of the JVM specification)
     */
    public static String getSignature(Class clazz) {
	return appendSignature(new StringBuffer(), clazz).toString();
    }

    /**
     * Generate a method signature.
     * @param paramT the java.lang.Class of the parameter types of the method.
     * @param returnT the java.lang.Class of the return type of the method.
     * @return the method signature (see section 4.3.3 Method Descriptors
     * of the JVM specification)
     */
    public static String getSignature(Class paramT[], Class returnT) {
	StringBuffer sig = new StringBuffer("(");
	for (int i=0; i< paramT.length; i++)
	    appendSignature(sig, paramT[i]);
	return appendSignature(sig.append(')'), returnT).toString();
    }

    /**
     * Generate the singleton set of the type represented by the given
     * string.
     * @param type the type signature (or method signature).  
     * @return a singleton set containing the given type.
     */
    public static final Type tType(String type) {
        if (type == null || type.length() == 0)
            return tError;
        switch(type.charAt(0)) {
        case 'Z':
            return tBoolean;
        case 'B':
            return tByte;
        case 'C':
            return tChar;
        case 'S':
            return tShort;
        case 'I':
            return tInt;
        case 'F':
            return tFloat;
        case 'J':
            return tLong;
        case 'D':
            return tDouble;
        case 'V':
            return tVoid;
        case '[':
            return tArray(tType(type.substring(1)));
        case 'L':
            int index = type.indexOf(';');
            if (index != type.length()-1)
                return tError;
            return tClass(type.substring(1, index));
	case '(':
	    return tMethod(type);
        }
        throw new AssertError("Unknown type signature: "+type);
    }

    /**
     * Generate the singleton set of the type represented by the given
     * class.
     * @param javaType the type class.
     * @return a singleton set containing the given type.
     */
    public static final Type tType(Class javaType) {
	return Type.tType(getSignature(javaType));
    }

    /**
     * Generate the singleton set of the type represented by the given
     * class name.
     * @param clazzname the full qualified name of the class. 
     * The packages may be separated by `.' or `/'.
     * @return a singleton set containing the given type.
     */
    public static final ClassInterfacesType tClass(String clazzname) {
	return tClass(ClassInfo.forName(clazzname.replace('/','.')));
    }

    /**
     * Generate the singleton set of the type represented by the given
     * class info.
     * @param clazzinfo the jode.bytecode.ClassInfo.
     * @return a singleton set containing the given type.
     */
    public static final ClassInterfacesType tClass(ClassInfo clazzinfo) {
///#ifdef JDK12
///	java.lang.ref.Reference died;
///	while ((died = classQueue.poll()) != null)
///	    classHash.values().remove(died);
///	WeakReference ref = (WeakReference) classHash.get(clazzinfo);
///        Object result = (ref == null) ? null : ref.get();
///#else
        Object result = classHash.get(clazzinfo);
///#endif
        if (result == null) {
            result = new ClassInterfacesType(clazzinfo);
///#ifdef JDK12
///            classHash.put(clazzinfo, new WeakReference(result, classQueue));
///#else
            classHash.put(clazzinfo, result);
///#endif
        }
        return (ClassInterfacesType) result;
    }

    /**
     * Generate/look up the set of the array type whose element types
     * are in the given type set.
     * @param type the element types (which may be the empty set tError).
     * @return the set of array types (which may be the empty set tError).
     */
    public static final Type tArray(Type type) {
        if (type == tError) 
            return type;
///#ifdef JDK12
///	java.lang.ref.Reference died;
///	while ((died = arrayQueue.poll()) != null)
///	    arrayHash.values().remove(died);
///	WeakReference ref = (WeakReference) arrayHash.get(type);
///        Type result = (ref == null) ? null : (Type) ref.get();
///#else
        Type result = (Type) arrayHash.get(type);
///#endif
        if (result == null) {
            result = new ArrayType(type);
///#ifdef JDK12
///            arrayHash.put(type, new WeakReference(result, arrayQueue));
///#else
            arrayHash.put(type, result);
///#endif
        }
        return result;
    }

    /**
     * Generate/look up the method type for the given signature 
     * @param signature the method decriptor.
     * @return a method type (a singleton set).
     */
    public static MethodType tMethod(String signature) {
///#ifdef JDK12
///	java.lang.ref.Reference died;
///	while ((died = methodQueue.poll()) != null)
///	    methodHash.values().remove(died);
///	WeakReference ref = (WeakReference) methodHash.get(signature);
///        MethodType result = (ref == null) ? null : (MethodType) ref.get();
///#else
	MethodType result = (MethodType) methodHash.get(signature);
///#endif
	if (result == null) {
	    result = new MethodType(signature);
///#ifdef JDK12
///	    methodHash.put(signature, new WeakReference(result, methodQueue));
///#else
            methodHash.put(signature, result);
///#endif
        }
        return result;
    }

    /**
     * Generate/look up the method type for the given class 
     * @param paramT the parameter types of the method.
     * @param returnT the return type of the method.
     * @return a method type (a singleton set).
     */
    public static MethodType tMethod(Class paramT[], Class returnT) {
	return tMethod(getSignature(paramT, returnT));
    }

    /**
     * Generate the range type from bottom to top.  This should
     * represent all reference types, that can be casted to bottom by
     * a widening cast and where top can be casted to.  You should not
     * use this method directly; use tSubType, tSuperType and
     * intersection instead, which is more general.
     * @param bottom the bottom type.
     * @param top the top type.  
     * @return the range type.
     */
    public static final Type tRange(ReferenceType bottom, 
				    ReferenceType top) {
        return new RangeType(bottom, top);
    }
     
    /**
     * Generate the set of types, to which one of the types in type can
     * be casted to by a widening cast.  The following holds:
     * <ul><li>tSuperType(tObject) = tObject </li>
     * <li>tSuperType(tError) = tError </li>
     * <li>type.intersection(tSuperType(type)).equals(type)
     *  (this means type is a subset of tSuperType(type).</li>
     * <li>tSuperType(tNull) = tUObject</li>
     * <li>tSuperType(tChar) = {tChar, tInt } </li></ul>
     * @param type a set of types.
     * @return the super types of type.
     */
    public static Type tSuperType(Type type) {
	return type.getSuperType();
    }

    /**
     * Generate the set of types, which can be casted to one of the
     * types in type by a widening cast.  The following holds:
     * <ul><li>tSubType(tObject) = tUObject </li>
     * <li>tSubType(tError) = tError </li>
     * <li>type.intersection(tSubType(type)).equals(type)
     *  (this means type is a subset of tSubType(type).</li>
     * <li>tSuperType(tSubType(type)) is a subset of type </li>
     * <li>tSubType(tSuperType(type)) is a subset of type </li>
     * <li>tSubType(tNull) = tNull</li>
     * <li>tSubType(tBoolean, tShort) = { tBoolean, tByte, tShort }</li></ul>
     * @param type a set of types.
     * @return the sub types of type.  
     */
    public static Type tSubType(Type type) {
        return type.getSubType();
    }

    /**
     * The typecode of this type. This should be one of the TC_ constants.
     */
    final int typecode;

    /**
     * Create a new type with the given type code.
     */
    protected Type(int tc) {
        typecode = tc;
    }

    /**
     * The sub types of this type.
     * @return tSubType(this).
     */
    public Type getSubType() {
        return this;
    }

    /**
     * The super types of this type.
     * @return tSuperType(this).
     */
    public Type getSuperType() {
        return this;
    }

    /**
     * Returns the hint type of this type set.  This returns the singleton
     * set containing only the `most likely' type in this set.  This doesn't
     * work for <code>tError</code> or <code>tUnknown</code>, and may lead
     * to errors for certain range types.
     * @return the hint type.
     */
    public Type getHint() {
	return getCanonic();
    }

    /**
     * Returns the canonic type of this type set.  The intention is, to 
     * return for each expression the type, that the java compiler would
     * assign to this expression.
     * @return the canonic type.
     */
    public Type getCanonic() {
	return this;
    }

    /**
     * Returns the type code of this type.  Don't use this; it is
     * merily needed by the sub types (and the bytecode verifier, which
     * has its own type merging methods).
     * @return the type code of the type.  
     */
    public final int getTypeCode() {
        return typecode;
    }

    /**
     * Returns the number of stack/local entries an object of this type
     * occupies.
     * @return 0 for tVoid, 2 for tDouble and tLong and 
     * 1 for every other type.
     */
    public int stackSize()
    {
        switch(typecode) {
        case TC_VOID:
            return 0;
        case TC_ERROR:
        default:
            return 1;
        case TC_DOUBLE:
        case TC_LONG:
            return 2;
        }
    }

    /**
     * Intersect this set of types with another type set and return the
     * intersection.
     * @param type the other type set.
     * @return the intersection, tError, if the intersection is empty.
     */
    public Type intersection(Type type) {
	if (this == tError || type == tError)
	    return tError;
	if (this == tUnknown)
	    return type;
	if (type == tUnknown || this == type)
	    return this;
	/* We have two different singleton sets now.
	 */
	if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0)
	    GlobalOptions.err.println("intersecting "+ this +" and "+ type
				   + " to <error>");
	return tError;
    }

    /**
     * Checks if we need to cast to a middle type, before we can cast from
     * fromType to this type.  For example it is impossible to cast a 
     * String to a StringBuffer, but if we cast to Object in between this
     * is allowed (it doesn't make much sense though).
     * @return the middle type, or null if it is not necessary.
     */
    public Type getCastHelper(Type fromType) {
	return null;
    }

    /**
     * Checks if this type represents a valid singleton type.
     */
    public boolean isValidType() {
	return typecode <= TC_DOUBLE;
    }

    /**
     * Checks if this is a class or array type (but not a null type).
     * @XXX remove this?
     * @return true if this is a class or array type.
     */
    public boolean isClassType() {
        return false;
    }

    /**
     * Check if this type set and the other type set are not disjunct.
     * @param type the other type set.
     * @return true if this they aren't disjunct.
     */
    public boolean isOfType(Type type) {
	return this.intersection(type) != Type.tError;
    }

    /**
     * Generates the default name, that is the `natural' choice for
     * local of this type.
     * @return the default name of a local of this type.  
     */
    public String getDefaultName() {
        switch (typecode) {
        case TC_LONG:
            return "l";
        case TC_FLOAT:
            return "f";
        case TC_DOUBLE:
            return "d";
        default:
            return "local";
        }
    }

    /**
     * Generates the default value, that is the initial value of a field
     * of this type.
     * @return the default value of a field of this type.  
     */
    public Object getDefaultValue() {
        switch (typecode) {
        case TC_LONG:
            return new Long(0);
        case TC_FLOAT:
            return new Float(0);
        case TC_DOUBLE:
            return new Double(0);
        default:
            return null;
        }
    }

    /**
     * Returns the type signature of this type.  You should only call
     * this on singleton types.
     * @return the type (or method) signature of this type.
     */
    public String getTypeSignature() {
        switch (typecode) {
        case TC_LONG:
            return "J";
        case TC_FLOAT:
            return "F";
        case TC_DOUBLE:
            return "D";
        default:
            return "?";
        }
    }

    /**
     * Returns the java.lang.Class representing this type.  You should
     * only call this on singleton types.  
     * @return the Class object representing this type.
     */
    public Class getTypeClass() throws ClassNotFoundException {
        switch (typecode) {
        case TC_LONG:
            return Long.TYPE;
        case TC_FLOAT:
            return Float.TYPE;
        case TC_DOUBLE:
            return Double.TYPE;
        default:
	    throw new AssertError("getTypeClass() called on illegal type");
        }
    }
    
    /**
     * Returns a string representation describing this type set.
     * @return a string representation describing this type set.
     */
    public String toString() {
        switch (typecode) {
        case TC_LONG:
            return "long";
        case TC_FLOAT:
            return "float";
        case TC_DOUBLE:
            return "double";
        case TC_NULL:
            return "null";
        case TC_VOID:
            return "void";
        case TC_UNKNOWN:
            return "<unknown>";
        case TC_ERROR:
        default:
            return "<error>";
        }
    }
}