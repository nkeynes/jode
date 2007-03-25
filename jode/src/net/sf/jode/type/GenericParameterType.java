/* GenericParameterType Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: GenericParameterType.java,v 1.3 2004/06/01 08:46:10 hoenicke Exp $
 */

package net.sf.jode.type;

/**
 * This class represents the singleton set containing one parameter type. For
 * example in the context of the class <code>Enum&lt;E extends
 * Enum&lt;E&gt;&gt;</code> the identifier <code>E</code> denotes such a
 * parameter type.  It has the super class <code>Enum&lt;E&gt;</code> and
 * implements no interfaces.
 *
 * @author Jochen Hoenicke 
 */
public class GenericParameterType extends ClassType {
    ClassType superType;
    ClassType[] ifacesTypes;

    /**
     * @param className The name of this system class, must be interned.
     */
    public GenericParameterType(String className, 
				ClassType superType, 
				ClassType[] ifacesTypes) {
	super(TC_SYSCLASS, className, null);
	this.superType = superType;
	this.ifacesTypes = ifacesTypes;
    }

    public boolean isInterface() {
	return false;
    }

    public boolean isFinal() {
	return false;
    }

    public boolean isUnknown() {
	return false;
    }

    public ClassType getSuperClass() {
	return superType;
    }

    public ClassType[] getInterfaces() {
	return ifacesTypes;
    }
}
