/* ParameterType Copyright (C) 2005 Jochen Hoenicke.
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
 * $Id: ParameterType.java,v 1.3 2004/06/01 08:46:10 hoenicke Exp $
 */

package net.sf.jode.type;
import net.sf.jode.bytecode.ClassInfo;
import net.sf.jode.GlobalOptions;

import java.lang.reflect.Modifier;
import java.io.IOException;
import java.util.Vector;
import java.util.Stack;
import java.util.Hashtable;

///#def COLLECTIONS java.util
import java.util.Map;
///#enddef

/**
 * This class represents the singleton set containing one parameter type. For
 * example in the context of the class <code>Enum&lt;E extends
 * Enum&lt;E&gt;&gt;</code> the identifier <code>E</code> denotes such a
 * parameter type.  It has the super class <code>Enum&lt;E&gt;</code> and
 * implements no interfaces.
 *
 * @author Jochen Hoenicke 
 */
public class ParameterType extends ClassType {
    ClassType   superClass;
    ClassType[] interfaces;

    public ParameterType(String name, 
			 ClassInfo superInfo, ClassInfo[] ifaceInfos) {
        super(TC_PARAMETER, name);

	superClass = Type.tClass(superInfo);
	if (ifaceInfos.length == 0) {
	    interfaces = EMPTY_IFACES;
	} else {
	    interfaces = new ClassType[ifaceInfos.length];
	    for (int i=0; i < interfaces.length; i++)
		interfaces[i] = Type.tClass(ifaceInfos[i]);
	}
    }

    public boolean isUnknown() {
	return false;
    }
    public boolean isFinal() {
	return false;
    }
    public boolean isInterface() {
	return false;
    }

    public ClassType getSuperClass() {
	return superClass;
    }

    public ClassType[] getInterfaces() {
	return interfaces;
    }

    public boolean equals(Object o) {
	if (o instanceof ParameterType)
	    return ((ParameterType) o).className == className;
	return false;
    }
}




