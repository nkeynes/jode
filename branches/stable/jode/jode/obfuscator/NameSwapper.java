/* NameSwapper Copyright (C) 1999 Jochen Hoenicke.
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
import java.util.Random;

///#ifdef JDK12
///import java.util.Collection;
///import java.util.Set;
///import java.util.HashSet;
///import java.util.Iterator;
///#else
import jode.util.Collection;
import jode.util.Set;
import jode.util.HashSet;
import jode.util.Iterator;
///#endif


public class NameSwapper implements Renamer {
    private Random rand;
    private Set packs, clazzes, methods, fields, locals;

    public NameSwapper(boolean swapAll, long seed) {
	if (swapAll) {
	    packs = clazzes = methods = fields = locals = new HashSet();
	} else {
	    packs = new HashSet();
	    clazzes = new HashSet();
	    methods = new HashSet();
	    fields = new HashSet();
	    locals = new HashSet();
	}
    }

    public NameSwapper(boolean swapAll) {
	this(swapAll, System.currentTimeMillis());
    }

    private void addName(Collection coll, String name) {
	coll.add(name);
    }

    private String getName(Collection coll) {
///#ifdef JDK12
///	int pos = rand.nextInt(coll.size());
///#else
 	int pos = rand.nextInt() % coll.size();
///#endif
	Iterator i = coll.iterator();
	while (pos > 0)
	    i.next();
	return (String) i.next();
    }

    public final void addPackageName(String name) {
	addName(packs, name);
    }

    public final void addClassName(String name) {
	addName(clazzes, name);
    }

    public final void addMethodName(String name) {
	addName(methods, name);
    }

    public final void addFieldName(String name) {
	addName(fields, name);
    }

    public final void addLocalName(String name) {
	addName(locals, name);
    }

    public final String getPackageName() {
	return getName(packs);
    }

    public final String getClassName() {
	return getName(clazzes);
    }

    public final String getMethodName() {
	return getName(methods);
    }

    public final String getFieldName() {
	return getName(fields);
    }

    public final String getLocalName() {
	return getName(locals);
    }

    public String generateName(Identifier ident, String lastName) {
	Collection coll = null;
	if (ident instanceof PackageIdentifier)
	    coll = packs;
	else if (ident instanceof ClassIdentifier)
	    coll = clazzes;
	else if (ident instanceof MethodIdentifier)
	    coll = methods;
	else if (ident instanceof FieldIdentifier)
	    coll = fields;
//  	else if (ident instanceof LocalIdentifier)
//  	    coll = locals;
	return getName(coll);
    }
}
