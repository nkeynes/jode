/* RangeType Copyright (C) 1998-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */

package net.sf.jode.type;
import net.sf.jode.GlobalOptions;
import net.sf.jode.bytecode.ClassInfo;
import java.util.Hashtable;

/**
 * This class represents a set of reference types.  The set contains
 * all types that are castable to all the bottom types by a widening
 * cast and to which one of the top types can be casted to by a
 * widening cast. <br>
 *
 * For a totally unknown reference type bottomType is tObject and
 * topType is tNull.  The bottomType is always guaranteed to be not
 * tNull.  And all topTypes must be castable to all bottom types with
 * a widening cast. <br>
 *
 * To do intersection on range types, the reference types need three
 * more operations: specialization, generalization and
 * createRange. <p>
 *
 * specialization chooses all common sub type of two types.  It is
 * used to find the bottom of the intersected interval. <p>
 *
 * generalization chooses the common super type of two types. It
 * is used to find the top of the intersected interval. <p>
 *
 * When the new interval is created with <code>createRangeType</code>
 * the bottom and top are adjusted so that they only consists of
 * possible types.  It then decides, if it needs a range type, or if
 * the reference types already represents all types.
 *
 * @author Jochen Hoenicke
 * @see ReferenceType
 * @date 98/08/06 */
public class RangeType extends Type {
    /**
     * The top type set.  Each type in the set represented by
     * this range type can be casted to all types in bottom type.
     */
    final ReferenceType topType;
    /**
     * The bottom type set.  For each type in this range type, there is a 
     * top type, that can be casted to this type.
     */
    final ReferenceType bottomType;

    /**
     * Create a new range type with the given bottom and top set.
     */
    RangeType(ReferenceType topType, ReferenceType bottomType) {
        super(TC_RANGE);
	if (topType == tNull)
	    throw new InternalError("top is NULL");
	this.topType    = topType;
	this.bottomType = bottomType;
    }

    /**
     * Returns the bottom type set.  All types in this range type can
     * be casted to all bottom types by a widening cast.
     * @return the bottom type set 
     */
    ReferenceType getTop() {
        return topType;
    }

    /**
     * Returns the bottom type set.  For each type in this range type,
     * there is a bottom type, that can be casted to this type.  
     * @return the bottom type set
     */
    ReferenceType getBottom() {
        return bottomType;
    }

    
    /**
     * Returns the hint type of this range type set.  This returns the
     * singleton set containing only the first top type, except if it
     * is null and there is a unique bottom type, in which case it
     * returns the bottom type.
     * @return the hint type.  
     */
    public Type getHint() {
	Type topHint = topType.getHint();
	Type bottomHint = bottomType.getHint();
	
	if (bottomType == tNull && topType.equals(topHint))
	    return topHint;

	return bottomHint;
    }

    /**
     * Returns the canonic type of this range type set.  This returns the
     * singleton set containing only the first top type.
     * @return the canonic type.  
     */
    public Type getCanonic() {
	return bottomType.getCanonic();
    }

    /**
     * The set of super types of this type.  This is the set of
     * super types of the top type.
     * @return the set of super types.
     */
    public Type getSuperType() {
	return bottomType.getSuperType();
    }

    /**
     * The set of sub types of this type.  This is the set of
     * sub types of the bottom types.
     * @return the set of super types.
     */
    public Type getSubType() {
	return tRange(topType, tNull);
    }
	    
    /**
     * Checks if we need to cast to a middle type, before we can cast from
     * fromType to this type.
     * @return the middle type, or null if it is not necessary.
     */
    public Type getCastHelper(Type fromType) {
	return bottomType.getCastHelper(fromType);
    }

    public String getTypeSignature() {
        if (bottomType.isClassType() || !topType.isValidType())
            return bottomType.getTypeSignature();
        else
            return topType.getTypeSignature();
    }

    public Class getTypeClass() throws ClassNotFoundException {
        if (bottomType.isClassType() || !topType.isValidType())
            return bottomType.getTypeClass();
        else
            return topType.getTypeClass();
    }

    public String toString()
    {
	return "<" + topType + "-" + bottomType + ">";
    }

    public String getDefaultName() {
	throw new InternalError("getDefaultName() called on range");
    }

    public int hashCode() {
	int hashcode = bottomType.hashCode();
	return (hashcode << 16 | hashcode >>> 16) ^ topType.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof RangeType) {
            RangeType type = (RangeType) o;
            return bottomType.equals(type.bottomType) 
                && topType.equals(type.topType);
        }
        return false;
    }

    public boolean containsClass(ClassInfo clazz) {
	ClassType clazzType = Type.tClass(clazz, null);
	if (!topType.maybeSuperTypeOf(clazzType))
	    return false;
	if (bottomType == tNull)
	    return true;
	if (bottomType instanceof ClassType)
	    return clazzType.maybeSuperTypeOf((ClassType) bottomType);
	if (bottomType instanceof MultiClassType) {
	    ClassType[] classes = ((MultiClassType) bottomType).classes;
	    for (int i = 0; i < classes.length; i++) {
		if (clazzType.maybeSuperTypeOf(classes[i]))
		    return true;
	    }
	}
	return false;
    }

    /**
     * Intersect this type with another type and return the new type.
     * @param type the other type.
     * @return the intersection, or tError, if a type conflict happens.
     */
    public Type intersection(Type type) {
	if (type == tError)
	    return type;
	if (type == Type.tUnknown)
	    return this;

	Type top, bottom, result;
	bottom = topType.getSpecializedType(type);
	top = bottomType.getGeneralizedType(type);
	if (top.equals(bottom))
	    result = top;
	else if (top instanceof ReferenceType
		 && bottom instanceof ReferenceType)
	    result = ((ReferenceType)top)
		.createRangeType((ReferenceType)bottom);
	else
	    result = tError;

        if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0) {
	    GlobalOptions.err.println("intersecting "+ this +" and "+ type +
				      " to <" + bottom + "," + top +
				      "> to " + result);
	}	    
        return result;
    }
}

