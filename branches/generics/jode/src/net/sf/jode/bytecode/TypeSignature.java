/* TypeSignature Copyright (C) 1999-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */

package net.sf.jode.bytecode;
import net.sf.jode.util.UnifyHash;
///#def COLLECTIONS java.util
import java.util.Map;
///#enddef

/**
 * This class contains some static methods to handle type signatures. <br>
 *
 * A type signature is a compact textual representation of a java
 * types.  It is described in the Java Virtual Machine Specification.
 * Primitive types have a one letter type signature.  Type signature
 * of classes contains the class name. Type signatures for arrays and
 * methods are recursively build from the type signatures of their
 * elements.  <br> Since java 5 there is a new class of type
 * signatures supporting generics.  These can be accessed with the
 * getSignature methods of ClassInfo, MethodInfo and FieldInfo.
 *
 * Here are a few examples:
 * <table><tr><th>type signature</th><th>Java type</th></tr>
 * <tr><td><code>Z</code></td><td><code>boolean</code></td></tr>
 * <tr><td><code>B</code></td><td><code>byte</code></td></tr>
 * <tr><td><code>S</code></td><td><code>short</code></td></tr>
 * <tr><td><code>C</code></td><td><code>char</code></td></tr>
 * <tr><td><code>I</code></td><td><code>int</code></td></tr>
 * <tr><td><code>F</code></td><td><code>float</code></td></tr>
 * <tr><td><code>J</code></td><td><code>long</code></td></tr>
 * <tr><td><code>D</code></td><td><code>double</code></td></tr>
 * <tr><td><code>Ljava/lang/Object;</code></td>
 *     <td><code>java.lang.Object</code></td></tr>
 * <tr><td><code>[[I</code></td><td><code>int[][]</code></td></tr>
 * <tr><td><code>(Ljava/lang/Object;I)V</code></td>
 *     <td>method with argument types <code>Object</code> and
 *         <code>int</code> and <code>void</code> return type.</td></tr>
 * <tr><td><code>()I</code></td>
 *     <td> method without arguments 
 *          and <code>int</code> return type.</td></tr>
 * <tr><td colspan="2"><code>&lt;E:Ljava/lang/Object;&gt;Ljava/lang/Object;Ljava/util/Collection&lt;TE;&gt;;</code></td>
 *     </tr><tr><td></td>
 *     <td> generic class over &lt;E extends Object&gt; extending
 *          Object and implementing Collections&lt;E&gt;</td></tr>
 * <tr><td colspan="2"><code>&lt;T:Ljava/lang/Object;&gt;([TT;)[TT;</code></td>
 *     </tr><tr><td></td>
 *     <td> generic method over &lt;T extends Object&gt; taking an
 *          array of T as parameters and returning an array of T.</td></tr>
 * </table>
 *
 *
 * @author Jochen Hoenicke
 */
public class TypeSignature {
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
		throw new InternalError("Unknown primitive type: "+javaType);
	} else if (javaType.isArray()) {
	    return appendSignature(sb.append('['), 
				   javaType.getComponentType());
	} else {
	    return sb.append('L')
		.append(javaType.getName().replace('.','/')).append(';');
	}
    }

    /**
     * Generates the type signature of the given Class.
     * @param clazz a java.lang.Class, this may also be a primitive or
     * array type.
     * @return the type signature.
     */
    public static String getSignature(Class clazz) {
	return appendSignature(new StringBuffer(), clazz).toString();
    }
 
    /**
     * Generates a method signature.
     * @param paramT the java.lang.Class of the parameter types of the method.
     * @param returnT the java.lang.Class of the return type of the method.
     * @return the method type signature
     */
    public static String getSignature(Class paramT[], Class returnT) {
	StringBuffer sig = new StringBuffer("(");
	for (int i=0; i< paramT.length; i++)
	    appendSignature(sig, paramT[i]);
	return appendSignature(sig.append(')'), returnT).toString();
    }

    /**
     * Generates a Class object for a type signature.  This is the
     * inverse function of getSignature.
     * @param typeSig a single type signature
     * @return the Class object representing that type.
     */
    public static Class getClass(String typeSig) 
	throws ClassNotFoundException 
    {
        switch(typeSig.charAt(0)) {
        case 'Z':
            return Boolean.TYPE;
        case 'B':
            return Byte.TYPE;
        case 'C':
            return Character.TYPE;
        case 'S':
            return Short.TYPE;
        case 'I':
            return Integer.TYPE;
        case 'F':
            return Float.TYPE;
        case 'J':
            return Long.TYPE;
        case 'D':
            return Double.TYPE;
        case 'V':
            return Void.TYPE;
        case 'L':
	    typeSig = typeSig.substring(1, typeSig.length()-1)
		.replace('/','.');
	    /* fall through */
        case '[':
	    return Class.forName(typeSig);
        }
        throw new IllegalArgumentException(typeSig);
    }

    /**
     * Check if the given type is a two slot type.  The only two slot 
     * types are long and double.
     */
    private static boolean usingTwoSlots(char type) {
	return "JD".indexOf(type) >= 0;
    }

    /**
     * Returns the number of words, an object of the given simple type
     * signature takes.  For long and double this is two, for all other
     * types it is one.
     */
    public static int getTypeSize(String typeSig) {
	return usingTwoSlots(typeSig.charAt(0)) ? 2 : 1;
    }

    /**
     * Gets the element type of an array.  
     * @param typeSig type signature of the array.
     * @return type signature for the element type.
     * @exception IllegalArgumentException if typeSig is not an array
     * type signature.
     */
    public static String getElementType(String typeSig) {
	if (typeSig.charAt(0) != '[')
	    throw new IllegalArgumentException();
	return typeSig.substring(1);
    }

    /**
     * Gets the ClassInfo for a class type.
     * @param classpath the classpath in which the ClassInfo is searched.
     * @param typeSig type signature of the class.
     * @return the ClassInfo object for the class.
     * @exception IllegalArgumentException if typeSig is not an class
     * type signature.
     */
    public static ClassInfo getClassInfo(ClassPath classpath, String typeSig) {
	if (typeSig.charAt(0) != 'L')
	    throw new IllegalArgumentException();
	return classpath.getClassInfo
	    (typeSig.substring(1, typeSig.length()-1).replace('/', '.'));
    }

    /**
     * Skips the next entry of a method type signature
     * @param methodTypeSig type signature of the method.
     * @param position the index to the last entry.
     * @return the index to the next entry.
     */
    public static int skipType(String methodTypeSig, int position) {
	char c = methodTypeSig.charAt(position++);
	while (c == '[')
	    c = methodTypeSig.charAt(position++);
	if (c == 'L' || c == 'T') {
	    int angledepth = 0;
	    c = methodTypeSig.charAt(position++);
	    while (c != ';' || angledepth > 0) {
		if (c == '<')
		    angledepth++;
		else if (c == '>')
		    angledepth--;
		c = methodTypeSig.charAt(position++);
	    }
	}
	return position;
    }
    
    /**
     * Gets the number of words the parameters for the given method
     * type signature takes.  This is the sum of getTypeSize() for
     * each parameter type.
     * @param methodTypeSig the method type signature.
     * @return the number of words the parameters take.
     */
    public static int getParameterSize(String methodTypeSig) {
	int nargs = 0;
	int i = 1;
	for (;;) {
	    char c = methodTypeSig.charAt(i);
	    if (c == ')')
		return nargs;
	    i = skipType(methodTypeSig, i);
	    if (usingTwoSlots(c))
		nargs += 2;
	    else 
		nargs++;
	}
    }

    /**
     * Gets the size of the return type of the given method in words.
     * This is zero for void return type, two for double or long return
     * type and one otherwise.
     * @param methodTypeSig the method type signature.
     * @return the size of the return type in words.
     */
    public static int getReturnSize(String methodTypeSig) {
	int length = methodTypeSig.length();
	if (methodTypeSig.charAt(length - 2) == ')') {
	    // This is a single character return type.
	    char returnType = methodTypeSig.charAt(length - 1);
	    return returnType == 'V' ? 0 
		: usingTwoSlots(returnType) ? 2 : 1;
	} else
	    // All multi character return types take one parameter
	    return 1;
    }

    /**
     * Gets the argument type signatures of the given template signature.
     * @param templateTypeSig the template type signature.
     * @return an array containing all parameter types in correct order.
     */
    public static String[] getArgumentTypes(String templateTypeSig) {
	System.err.println(templateTypeSig);
	int pos = 1;
	int count = 0;
	char c;
	while ((c = templateTypeSig.charAt(pos)) != '>') {
	    if (c == '*') {
		pos++;
	    } else {
		if (c == '+' || c == '-')
		    pos++;
		pos = skipType(templateTypeSig, pos);
	    }
	    count++;
	}
	String[] params = new String[count];
	pos = 1;
	for (int i = 0; i < count; i++) {
	    int start = pos;
	    c = templateTypeSig.charAt(pos);
	    if (c == '*') {
		pos++;
	    } else {
		if (c == '+' || c == '-')
		    pos++;
		pos = skipType(templateTypeSig, pos);
	    }
	    params[i] = templateTypeSig.substring(start, pos);
	}
	return params;
    }

    /**
     * Gets the parameter type signatures of the given method signature.
     * @param methodTypeSig the method type signature.
     * @return an array containing all parameter types in correct order.
     */
    public static String[] getParameterTypes(String methodTypeSig) {
	System.err.println(methodTypeSig);
	int pos = 1;
	int count = 0;
	while (methodTypeSig.charAt(pos) != ')') {
	    pos = skipType(methodTypeSig, pos);
	    count++;
	}
	String[] params = new String[count];
	pos = 1;
	for (int i = 0; i < count; i++) {
	    int start = pos;
	    pos = skipType(methodTypeSig, pos);
	    params[i] = methodTypeSig.substring(start, pos);
	}
	return params;
    }

    /**
     * Gets the return type for a method signature
     * @param methodTypeSig the method signature.
     * @return the return type for a method signature, `V' for void methods.
     */
    public static String getReturnType(String methodTypeSig) {
	return methodTypeSig.substring(methodTypeSig.lastIndexOf(')')+1);
    }

    /**
     * Gets the names of the generic parameters of the given type signature.
     * @param typeSig the type signature.
     * @return an array containing all generic parameter types 
     * in correct order, or null if there aren't any generic parameters.
     */
    public static String[] getGenericNames(String typeSig) {
	System.err.println(typeSig);
	if (typeSig.charAt(0) != '<')
	    return null;
	int pos = 1;
	int count = 0;
	while (typeSig.charAt(pos) != '>') {
	    while (typeSig.charAt(pos) != ':')
		pos++;
	    /* check for empty entry */
	    if (typeSig.charAt(pos+1) == ':')
		pos++;
	    while (typeSig.charAt(pos) == ':') {
		/* skip colon and type */
		pos = skipType(typeSig, pos + 1);
	    }
	    count++;
	}
	String[] params = new String[count];
	pos = 1;
	count = 0;
	while (typeSig.charAt(pos) != '>') {
	    int spos = pos;
	    while (typeSig.charAt(pos) != ':')
		pos++;
	    params[count++] = typeSig.substring(spos, pos);
	    /* check for empty entry */
	    if (typeSig.charAt(pos+1) == ':')
		pos++;
	    while (typeSig.charAt(pos) == ':') {
		/* skip colon and type */
		pos = skipType(typeSig, pos + 1);
	    }
	}
	return params;
    }

    private static int mapGenericsInType(String typeSig, Map generics,
					 StringBuffer mapped, int spos) {
	int pos = spos;
	char c = typeSig.charAt(pos++);
	while (c == '[')
	    c = typeSig.charAt(pos++);
	if (c == 'T') {
	    int epos = typeSig.indexOf(';', pos);
	    String key = typeSig.substring(pos, epos);
	    String mapval = (String) generics.get(key);
	    if (mapval != null) {
		mapped.append(typeSig.substring(spos, pos - 1))
		    .append(key);
		spos = epos + 1;
	    }
	    pos = epos + 1;
	} else if (c == 'L') {
	    c = typeSig.charAt(pos++);
	    while (c != ';' && c != '<')
		c = typeSig.charAt(pos++);
	    if (c == '<') {
		mapped.append(typeSig.substring(spos, pos));
		while (typeSig.charAt(pos) != '>') {
		    pos = mapGenericsInType(typeSig, generics, mapped, pos);
		}
		spos = pos;
		pos += 2;
	    }
	}
	mapped.append(typeSig.substring(spos, pos));
	return pos;
    }

    /**
     * Map the names of the generic parameters in the given type signature
     * and return the type signature with the generic parameters mapped to
     * (more or less) real types.
     * @param typeSig the type signature.
     * @param generics A map from generic names to type signatures.
     * @return the mapped generic type signature.
     */
    public static String mapGenerics(String typeSig, Map generics) {
	StringBuffer mapped = new StringBuffer();
	int pos = 0;
	int spos = 0;
	if (typeSig.length() == 0)
	    return "";
	char c = typeSig.charAt(pos++);
	if (c == '<') {
	    c = typeSig.charAt(pos++);
	    while (c != '>') {
		while (c != ':') {
		    c = typeSig.charAt(pos++);
		}
		if (typeSig.charAt(pos) == ':')
		    pos++;
		while (c == ':') {
		    mapped.append(typeSig.substring(spos, pos));
		    pos = mapGenericsInType(typeSig, generics, mapped, pos);
		    spos = pos;
		    c = typeSig.charAt(pos);
		}
	    }
	}
	if (c == '(') {
	    while (typeSig.charAt(pos) != ')') {
		mapped.append(typeSig.substring(spos, pos));
		pos = mapGenericsInType(typeSig, generics, mapped, pos);
		spos = pos;
	    }
	    pos++;
	}
	mapped.append(typeSig.substring(spos, pos));
	while (pos < typeSig.length()) {
	    pos = mapGenericsInType(typeSig, generics, mapped, pos);
	}
	return mapped.toString();
    }

    /**
     * Gets the default value an object of the given type has.  It is
     * null for objects and arrays, Integer(0) for boolean and short
     * integer types or Long(0L), Double(0.0), Float(0.0F) for long,
     * double and float.  This seems strange, but this way the type
     * returned is the same as for FieldInfo.getConstant().
     *
     * @param typeSig the type signature.
     * @return the default value.
     * @exception IllegalArgumentException if this is a method type signature.
     */
    public static Object getDefaultValue(String typeSig) {
	switch(typeSig.charAt(0)) {
	case 'Z':
	case 'B':
	case 'S':
	case 'C':
	case 'I':
	    return new Integer(0);
	case 'J':
	    return new Long(0L);
	case 'D':
	    return new Double(0.0);
	case 'F':
	    return new Float(0.0F);
	case 'L':
	case '[':
	    return null;
	default:
	    throw new IllegalArgumentException(typeSig);
	}
    }

    /**
     * Checks if there is a valid class name starting at index
     * in string typeSig and ending with a semicolon.
     * @return the index at which the class name ends.
     * @exception IllegalArgumentException if there was an illegal character.
     * @exception StringIndexOutOfBoundsException if the typeSig ended early.
     */
    private static int checkClassName(String clName, int i) 
	throws IllegalArgumentException, StringIndexOutOfBoundsException 
    {
	while (true) {
	    char c = clName.charAt(i++);
	    if (c == '<') {
		c = clName.charAt(i++);
		do {
		    if (c == '*')
			i++;
		    else { 
			if (c == '+' || c == '-')
			    c = clName.charAt(i++);
			if (c != 'L' && c != 'T' && c != '[')
			    throw new IllegalArgumentException
				("Wrong class instantiation: "+clName);
			i = checkTypeSig(clName, i - 1);
		    }
		    c = clName.charAt(i++);
		} while (c != '>');
		c = clName.charAt(i++);
		if (c != ';')
		    throw new IllegalArgumentException
			("no ; after > in "+clName);
	    }
	    if (c == ';')
		return i;
	    if (c != '/' && !Character.isJavaIdentifierPart(c))
		throw new IllegalArgumentException("Illegal java class name: "
						   + clName);
	}
    }

    /**
     * Checks if there is a valid class name starting at index
     * in string typeSig and ending with a semicolon.
     * @return the index at which the class name ends.
     * @exception IllegalArgumentException if there was an illegal character.
     * @exception StringIndexOutOfBoundsException if the typeSig ended early.
     */
    private static int checkTemplateName(String clName, int i) 
	throws IllegalArgumentException, StringIndexOutOfBoundsException 
    {
	while (true) {
	    char c = clName.charAt(i++);
	    if (c == ';')
		return i;
	    if (!Character.isJavaIdentifierPart(c))
		throw new IllegalArgumentException("Illegal java class name: "
						   + clName);
	}
    }

    /**
     * Checks if there is a valid simple type signature starting at index
     * in string typeSig.
     * @return the index at which the type signature ends.
     * @exception IllegalArgumentException if there was an illegal character.
     * @exception StringIndexOutOfBoundsException if the typeSig ended early.
     */
    private static int checkTypeSig(String typeSig, int index) {
	char c = typeSig.charAt(index++);
	while (c == '[')
	    c = typeSig.charAt(index++);
	if (c == 'L') {
	    index = checkClassName(typeSig, index);
	} else if (c == 'T') {
	    index = checkTemplateName(typeSig, index);
	} else {
	    if ("ZBSCIJFD".indexOf(c) == -1)
		throw new IllegalArgumentException("Type sig error: "+typeSig);
	}
	return index;
    }

    /**
     * Checks whether a given type signature starts with valid generic
     * part and returns the index where generics end.
     * @param typeSig the type signature.
     * @param i the start index.
     * @exception NullPointerException if typeSig is null.
     * @exception IllegalArgumentException if typeSig is not a valid
     * type signature.
     * @return 0 if no generics at beginning, otherwise the 
     * index after the &gt; sign.
     */
    private static int checkGenerics(String typeSig, int i) {
	if (typeSig.charAt(i) == '<') {
	    i++;
	    char c = typeSig.charAt(i++);
	    if (c == '>')
		throw new IllegalArgumentException("Empty Generics: "+typeSig);
	    while (c != '>') {
		if (c == ':')
		    throw new IllegalArgumentException("Empty type name: "+typeSig);
		while (c != ':') {
		    if (!Character.isJavaIdentifierPart(c))
			throw new IllegalArgumentException
			    ("Illegal generic name: "+ typeSig);
		    c = typeSig.charAt(i++);
		}
		c = typeSig.charAt(i++);
		if (c != 'L' && c != 'T' && c != ':')
		    throw new IllegalArgumentException
			("Wrong generic extends: "+typeSig);
		if (c != ':')
		    i = checkTypeSig(typeSig, i - 1);

		c = typeSig.charAt(i++);
		while(c == ':') {
		    i = checkTypeSig(typeSig, i);
		    c = typeSig.charAt(i++);
		}
	    }
	}
	return i;
    }

    /**
     * Checks whether a given type signature is a valid (not method)
     * type signature.  Throws an exception otherwise.
     * @param typeSig the type signature.
     * @exception NullPointerException if typeSig is null.
     * @exception IllegalArgumentException if typeSig is not a valid
     * type signature or if it's a method type signature.
     */
    public static void checkTypeSig(String typeSig) 
	throws IllegalArgumentException
    {
	try {
	    int i = checkGenerics(typeSig, 0);
	    if (checkTypeSig(typeSig, i) != typeSig.length())
		throw new IllegalArgumentException
		    ("Type sig too long: "+typeSig);
	} catch (StringIndexOutOfBoundsException ex) {
	    throw new IllegalArgumentException
		("Incomplete type sig: "+typeSig);
	}
    }

    /**
     * Checks whether a given type signature is a valid class
     * type signature.  Throws an exception otherwise.
     * A class type signature starts optionally with generics,
     * followed by type signature of super class, followed by
     * type signature of super interfaces.
     * @param typeSig the type signature.
     * @exception NullPointerException if typeSig is null.
     * @exception IllegalArgumentException if typeSig is not a valid
     * type signature or if it's a method type signature.
     */
    public static void checkClassTypeSig(String typeSig) 
	throws IllegalArgumentException
    {
	try {
	    int i = checkGenerics(typeSig, 0);
	    i = checkTypeSig(typeSig, i);
	    while (i != typeSig.length()) {
		i = checkTypeSig(typeSig, i);
	    }
	} catch (StringIndexOutOfBoundsException ex) {
	    throw new IllegalArgumentException
		("Incomplete type sig: "+typeSig);
	}
    }

    /**
     * Checks whether a given type signature is a valid method
     * type signature.  Throws an exception otherwise.
     * @param typeSig the type signature.
     * @exception NullPointerException if typeSig is null.
     * @exception IllegalArgumentException if typeSig is not a valid
     * method type signature.
     */
    public static void checkMethodTypeSig(String typeSig) 
	throws IllegalArgumentException
    {
	try {
	    int i = checkGenerics(typeSig, 0);
	    if (typeSig.charAt(i) != '(')
		throw new IllegalArgumentException
		    ("No method signature: "+typeSig);
	    i++;
	    while (typeSig.charAt(i) != ')')
		i = checkTypeSig(typeSig, i);
	    // skip closing parenthesis.
	    i++;
	    if (typeSig.charAt(i) == 'V')
		// accept void return type.
		i++;
	    else
		i = checkTypeSig(typeSig, i);
	    if (i != typeSig.length())
		throw new IllegalArgumentException
		    ("Type sig too long: "+typeSig);
	} catch (StringIndexOutOfBoundsException ex) {
	    throw new IllegalArgumentException
		("Incomplete type sig: "+typeSig);
	}
    }
}

