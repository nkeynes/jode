/* ClassInterfacesType Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.bytecode.ClassInfo;
import java.util.Vector;
import java.util.Stack;
import java.util.Hashtable;
import java.io.IOException;

/**
 * This class represents a type aproximation, consisting of multiple
 * interfaces and a class type.<p>
 *
 * If this is the bottom boundary, this specifies, which class our
 * type must extend and which interfaces it must implement.
 *
 * If this is the top boundary, this gives all interfaces and classes
 * that may extend the type.  I.e. at least one interface or class extends
 * the searched type.
 *
 * @author Jochen Hoenicke */
public class ClassInterfacesType extends ReferenceType {

    ClassInfo clazz;
    ClassInfo ifaces[];

    public ClassInfo getClazz() {
        return clazz != null ? clazz
	    : ClassInfo.forName("java.lang.Object");
    }

    public ClassInterfacesType(String clazzName) {
        this(ClassInfo.forName(clazzName));
    }

    public ClassInterfacesType(ClassInfo clazz) {
        super(TC_CLASS);
	try {
	    clazz.load(ClassInfo.HIERARCHY);
	} catch (IOException ex) {
	    clazz.guess(ClassInfo.HIERARCHY);
	}
	if (clazz.isInterface()) {
	    this.clazz = null;
	    ifaces = new ClassInfo[] { clazz };
	} else {
	    this.clazz = 
		(clazz.getName() == "java.lang.Object") ? null : clazz;
	    ifaces = new ClassInfo[0];
	}
    }

    public ClassInterfacesType(ClassInfo clazz, ClassInfo[] ifaces) {
        super(TC_CLASS);
        this.clazz = clazz;
        this.ifaces = ifaces;
    }

    static ClassInterfacesType create(ClassInfo clazz, ClassInfo[] ifaces) {
        /* Make sure that every {java.lang.Object} equals tObject */
        if (ifaces.length == 0 && clazz == null) 
            return tObject;
	if (ifaces.length == 0)
	    return tClass(clazz);
	if (ifaces.length == 1 && clazz == null)
	    return tClass(ifaces[0]);
        return new ClassInterfacesType(clazz, ifaces);
    }

    public Type getSubType() {
	if ((clazz == null && ifaces.length == 1)
	    || ifaces.length == 0)
	    return tRange(this, tNull);
	
	/* We don't implement the set of types, that are castable to some 
	 * of the given classes or interfaces.
	 */
	throw new jode.AssertError
	    ("getSubType called on set of classes and interfaces!");
    }

    public Type getHint() {
	if (ifaces.length == 0
	    || (clazz == null && ifaces.length == 1))
	    return this;
	if (clazz != null)
	    return Type.tClass(clazz);
	else 
	    return Type.tClass(ifaces[0]);
    }

    public Type getCanonic() {
	if (ifaces.length == 0
	    || (clazz == null && ifaces.length == 1))
	    return this;
	if (clazz != null)
	    return Type.tClass(clazz);
	else 
	    return Type.tClass(ifaces[0]);
    }

    /**
     * Create the type corresponding to the range from bottomType to
     * this.  Checks if the given type range may be not empty.  This
     * means, that bottom.clazz is extended by this.clazz and that all
     * interfaces in bottom are implemented by an interface or by
     * clazz.
     * @param bottom the start point of the range
     * @return the range type, or tError if range is empty.  
     */
    public Type createRangeType(ReferenceType bottomType) {
        if (bottomType.typecode != TC_CLASS)
            return tError;

	ClassInterfacesType bottom = (ClassInterfacesType) bottomType;

        if (bottomType == tObject)
            return (this == tObject) ? tObject : tRange(tObject, this);
        
	try {
        if (bottom.clazz != null) {
            /* The searched type must be a class type.
             */
            if (!bottom.clazz.superClassOf(this.clazz))
                return tError;
            
            /* All interfaces must be implemented by this.clazz
             */
            for (int i=0; i < bottom.ifaces.length; i++) {
                if (!bottom.ifaces[i].implementedBy(this.clazz))
                    return tError;
            }

            if (bottom.clazz == this.clazz
                && bottom.ifaces.length == 0)
                return bottom;

	    if (this.ifaces.length != 0)
		return tRange(bottom, create(this.clazz, new ClassInfo[0]));
            return tRange(bottom, this);
            
        } else {
            
            /* Now bottom.clazz is null (or tObject), find all
             * classes/interfaces that implement all bottom.ifaces.  
             */

            ClassInfo clazz = this.clazz;
            if (clazz != null) {
                for (int i=0; i < bottom.ifaces.length; i++) {
                    if (!bottom.ifaces[i].implementedBy(clazz)) {
                        clazz = null;
                        break;
                    }
                }
            }

            /* If bottom is a single interface and equals some top
             * interface, then bottom is the only possible type.
             */
            if (clazz == null && bottom.ifaces.length == 1) { 
                for (int i=0; i< this.ifaces.length; i++) {
                    if (this.ifaces[i] == bottom.ifaces[0])
                        return bottom;
                }
            }

            ClassInfo[] ifaces = new ClassInfo[this.ifaces.length];
            int count = 0;
        big_loop:
            for (int j=0; j < this.ifaces.length; j++) {
                for (int i=0; i < bottom.ifaces.length; i++) {
                    if (!bottom.ifaces[i].implementedBy(this.ifaces[j]))
                        continue big_loop;
                }
                ifaces[count++] = (this.ifaces[j]);
            }

	    if (clazz == null && count == 0) {
		/* There are no more possible interfaces or classes left.
		 * This is a type error.
		 */
		return tError;
	    } else if (count < ifaces.length) {
                ClassInfo[] shortIfaces = new ClassInfo[count];
                System.arraycopy(ifaces, 0, shortIfaces, 0, count);
                ifaces = shortIfaces;
            } else if (clazz == this.clazz)
                return tRange(bottom, this);
            return tRange(bottom, create(clazz, ifaces));
        }
	} catch (IOException ex) {
	    /*XXX : There is something better than tError*/
	    return tError;
	}
    }
    
    /**
     * Returns the specialized type of this and type.
     * We have two classes and multiple interfaces.  The result 
     * should be the object that extends both objects
     * and the union of all interfaces.
     */
    public Type getSpecializedType(Type type) {
        int code = type.typecode;
	if (code == TC_RANGE) {
	    type = ((RangeType) type).getBottom();
	    code = type.typecode;
	}
	if (code == TC_NULL)
	    return this;
        if (code == TC_ARRAY)
	    return ((ArrayType) type).getSpecializedType(this);
        if (code != TC_CLASS)
            return tError;

        ClassInterfacesType other = (ClassInterfacesType) type;
        ClassInfo clazz;

        /* First determine the clazz, one of the two classes must be a sub
         * class of the other or null.
         */

	try {
        if (this.clazz == null)
            clazz = other.clazz;
        else if (other.clazz == null)
            clazz = this.clazz;
        else if (this.clazz.superClassOf(other.clazz))
            clazz = other.clazz;
        else if (other.clazz.superClassOf(this.clazz))
            clazz = this.clazz;
        else
            return tError;
	} catch (IOException ex) {
	    /*XXX : There is something better than tError*/
	    return tError;
	}

        /* Most times (99.9999999 %) one of the two classes is already
         * more specialized.  Optimize for this case. (I know of one
         * class where at one intersection this doesn't succeed) 
	 */
        if (clazz == this.clazz 
            && implementsAllIfaces(this.clazz, this.ifaces, other.ifaces))
            return this;
        else if (clazz == other.clazz 
                 && implementsAllIfaces(other.clazz, other.ifaces, 
					this.ifaces))
            return other;

	try {
        /* The interfaces are simply the union of both interfaces set. 
         * But we can simplify this, if an interface is implemented by
         * another or by the class, we can omit it.
         */
        Vector ifaces = new Vector();
    big_loop_this:
        for (int i=0; i< this.ifaces.length; i++) {
            ClassInfo iface = this.ifaces[i];
            if (clazz != null && iface.implementedBy(clazz)) {
                continue big_loop_this;
            }
            for (int j=0; j<other.ifaces.length; j++) {
                if (iface.implementedBy(other.ifaces[j])) {
                    continue big_loop_this;
                }
            }

            /* This interface is not implemented by any of the other
             * ifaces.  Add it to the interfaces vector.
             */
            ifaces.addElement(iface);
        }
    big_loop_other:
        for (int i=0; i< other.ifaces.length; i++) {
            ClassInfo iface = other.ifaces[i];
            if (clazz != null && iface.implementedBy(clazz)) {
                continue big_loop_other;
            }
            for (int j=0; j<ifaces.size(); j++) {
                if (iface.implementedBy((ClassInfo) 
                                        ifaces.elementAt(j))) {
                    continue big_loop_other;
                }
            }

            /* This interface is not implemented by any of the other
             * ifaces.  Add it to the interfaces vector.
             */
            ifaces.addElement(iface);
        }
            
        ClassInfo[] ifaceArray = new ClassInfo[ifaces.size()];
        ifaces.copyInto(ifaceArray);
        return create(clazz, ifaceArray);
	} catch (IOException ex) {
	    /*XXX : There is something better than tError*/
	    return tError;
	}
    }

    /**
     * Returns the generalized type of this and type.  We have two
     * classes and multiple interfaces.  The result should be the
     * object that is the the super class of both objects and all
     * interfaces, that one class or interface of each type 
     * implements.  */
    public Type getGeneralizedType(Type type) {
	try{
        int code = type.typecode;
	if (code == TC_RANGE) {
	    type = ((RangeType) type).getTop();
	    code = type.typecode;
	}
        if (code == TC_NULL)
            return this;
	if (code == TC_ARRAY)
	    return ((ArrayType) type).getGeneralizedType(this);
        if (code != TC_CLASS)
            return tError;
        ClassInterfacesType other = (ClassInterfacesType) type;
        ClassInfo clazz;

        /* First the easy part, determine the clazz */
        if (this.clazz == null || other.clazz == null)
            clazz = null;
        else {
            clazz = this.clazz;
                
	    try {
		while(clazz != null) {
		    if (clazz.superClassOf(other.clazz))
			break;
		    clazz = clazz.getSuperclass();
		}
		if (clazz.getName() == "java.lang.Object")
		    clazz = null;
	    } catch (IOException ex) {
		/*XXX : There is something better than tObject*/
		clazz = null;
	    }
        }

        if (clazz == this.clazz 
            && implementsAllIfaces(other.clazz, other.ifaces, this.ifaces))
            return this;
        else if (clazz == other.clazz 
                 && implementsAllIfaces(this.clazz, this.ifaces, other.ifaces))
            return other;

        /* Now the more complicated part: find all interfaces, that are
         * implemented by one interface or class in each group.
         *
         * First get all interfaces of this.clazz and this.ifaces.
         */
            
        Stack allIfaces = new Stack();
        if (this.clazz != null) {
            ClassInfo c = this.clazz;
            while (clazz != c) {
                ClassInfo clazzIfaces[] = c.getInterfaces();
                for (int i=0; i<clazzIfaces.length; i++)
                    allIfaces.push(clazzIfaces[i]);
                c = c.getSuperclass();
            }
        }

        Vector ifaces = new Vector();

        for (int i=0; i<this.ifaces.length; i++)
            allIfaces.push(this.ifaces[i]);
            
            /* Now consider each interface.  If any clazz or interface
             * in other implements it, add it to the ifaces vector.
             * Otherwise consider all sub interfaces.
             */
    iface_loop:
        while (!allIfaces.isEmpty()) {
            ClassInfo iface = (ClassInfo) allIfaces.pop();
            if ((clazz != null && iface.implementedBy(clazz))
                || ifaces.contains(iface))
                /* We can skip this, as clazz or ifaces already imply it.
                 */
                continue iface_loop;

            if (other.clazz != null && iface.implementedBy(other.clazz)) {
                ifaces.addElement(iface);
                continue iface_loop;
            }
            for (int i=0; i<other.ifaces.length; i++) {
                if (iface.implementedBy(other.ifaces[i])) {
                    ifaces.addElement(iface);
                    continue iface_loop;
                }
            }

            /* This interface is not implemented by any of the other
             * ifaces.  Try its parent interfaces now.
             */
            ClassInfo clazzIfaces[] = iface.getInterfaces();
            for (int i=0; i<clazzIfaces.length; i++)
                allIfaces.push(clazzIfaces[i]);
        }
                
        ClassInfo[] ifaceArray = new ClassInfo[ifaces.size()];
        ifaces.copyInto(ifaceArray);
        return create(clazz, ifaceArray);
	} catch (IOException ex) {
	    /*XXX : There is something better than tError*/
	    return tError;
	}
    }

    public String getTypeSignature() {
	if (clazz != null)
	    return "L" + clazz.getName().replace('.','/') + ";";
	else if (ifaces.length > 0)
	    return "L" + ifaces[0].getName().replace('.','/') + ";";
	else
	    return "Ljava/lang/Object;";
    }

    public Class getTypeClass() throws ClassNotFoundException {
	if (clazz != null)
	    return Class.forName(clazz.getName());
	else if (ifaces.length > 0)
	    return Class.forName(ifaces[0].getName());
	else
	    return Class.forName("java.lang.Object");
    }

    public ClassInfo getClassInfo() {
	if (clazz != null)
	    return clazz;
	else if (ifaces.length > 0)
	    return ifaces[0];
	else
	    return ClassInfo.forName("java.lang.Object");
    }

    public String toString()
    {
	if (this == tObject)
	    return "java.lang.Object";
	
	if (ifaces.length == 0)
	    return clazz.getName();
	if (clazz == null && ifaces.length == 1)
	    return ifaces[0].getName();

	StringBuffer sb = new StringBuffer("{");
	String comma = "";
	if (clazz != null) {
	    sb = sb.append(clazz.getName());
                comma = ", ";
	}
	for (int i=0; i< ifaces.length; i++) {
	    sb.append(comma).append(ifaces[i].getName());
	    comma = ", ";
	}
	return sb.append("}").toString();
    }

    /**
     * Checks if we need to cast to a middle type, before we can cast from
     * fromType to this type.
     * @return the middle type, or null if it is not necessary.
     */
    public Type getCastHelper(Type fromType) {
	Type hintType = fromType.getHint();
	switch (hintType.getTypeCode()) {
	case TC_ARRAY:
	    if (clazz == null
		&& implementsAllIfaces(null, ArrayType.arrayIfaces, 
				       this.ifaces))
		return null;
	    else
		return tObject;
	case TC_CLASS:
	    try {
		ClassInterfacesType hint = (ClassInterfacesType) hintType;
		if (hint.clazz == null || clazz == null
		    || clazz.superClassOf(hint.clazz)
		    || hint.clazz.superClassOf(clazz))
		    return null;
		ClassInfo superClazz = clazz.getSuperclass();
		while (superClazz != null
		       && !superClazz.superClassOf(hint.clazz)) {
		    superClazz = superClazz.getSuperclass();
		}
		return tClass(superClazz.getName());
	    } catch (IOException ex) {
		/*XXX : There is something better than tObject*/
		return tObject;
	    }
	case TC_UNKNOWN:
	    return null;
	}
	return tObject;
    }

    /**
     * Checks if this type represents a valid type instead of a list
     * of minimum types.
     */
    public boolean isValidType() {
	return ifaces.length == 0
	    || (clazz == null && ifaces.length == 1);
    }

    /**
     * Checks if this is a class or array type (but not a null type).
     * @XXX remove this?
     * @return true if this is a class or array type.
     */
    public boolean isClassType() {
        return true;
    }

    private final static Hashtable keywords = new Hashtable();
    static {
	keywords.put("if", Boolean.TRUE);
	keywords.put("else", Boolean.TRUE);
	keywords.put("for", Boolean.TRUE);
	keywords.put("while", Boolean.TRUE);
	keywords.put("throw", Boolean.TRUE);
	keywords.put("return", Boolean.TRUE);
	keywords.put("class", Boolean.TRUE);
	keywords.put("interface", Boolean.TRUE);
	keywords.put("implements", Boolean.TRUE);
	keywords.put("extends", Boolean.TRUE);
	keywords.put("instanceof", Boolean.TRUE);
	keywords.put("new", Boolean.TRUE);
	keywords.put("int", Boolean.TRUE);
	keywords.put("boolean", Boolean.TRUE);
	keywords.put("long", Boolean.TRUE);
	keywords.put("float", Boolean.TRUE);
	keywords.put("double", Boolean.TRUE);
	keywords.put("short", Boolean.TRUE);
	keywords.put("public", Boolean.TRUE);
	keywords.put("protected", Boolean.TRUE);
	keywords.put("private", Boolean.TRUE);
	keywords.put("static", Boolean.TRUE);
	keywords.put("synchronized", Boolean.TRUE);
	keywords.put("strict", Boolean.TRUE);
	keywords.put("transient", Boolean.TRUE);
	keywords.put("abstract", Boolean.TRUE);
	keywords.put("volatile", Boolean.TRUE);
	keywords.put("final", Boolean.TRUE);
    }

    /**
     * Generates the default name, that is the `natural' choice for
     * local of this type.
     * @return the default name of a local of this type.  
     */
    public String getDefaultName() {
        ClassInfo type;
        if (clazz != null)
            type = clazz;
        else if (ifaces.length > 0)
            type = ifaces[0];
        else
            type = ClassInfo.forName("java.lang.Object");
        String name = type.getName();
        int dot = Math.max(name.lastIndexOf('.'), name.lastIndexOf('$'));
        if (dot >= 0)
            name = name.substring(dot+1);
        if (Character.isUpperCase(name.charAt(0))) {
	    name = name.toLowerCase();
	    if (keywords.get(name) != null)
		return "var_" + name;
            return name;
        } else
            return "var_" + name;
    }
    
    public int hashCode() {
	int hash = clazz == null ? 0 : clazz.hashCode();
	for (int i=0; i < ifaces.length; i++) {
	    hash ^= ifaces[i].hashCode();
	}
	return hash;
    }

    public boolean equals(Object o) {
        if (o == this) 
            return true;
        if (o instanceof Type && ((Type)o).typecode == TC_CLASS) {
            ClassInterfacesType type = (ClassInterfacesType) o;
            if (type.clazz == clazz
                && type.ifaces.length == ifaces.length) {
                big_loop:
                for (int i=0; i< type.ifaces.length; i++) {
                    for (int j=0; j<ifaces.length; j++) {
                        if (type.ifaces[i] == ifaces[j])
                            continue big_loop;
                    }
                    return false;
                }
                return true;
            }
        }
        return false;
    }
}
