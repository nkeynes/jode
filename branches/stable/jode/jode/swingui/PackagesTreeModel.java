/* PackagesTreeModel Copyright (C) 1999 Jochen Hoenicke.
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

package jode.swingui;
import jode.Decompiler;
import jode.bytecode.ClassInfo;
///#ifndef OLDSWING
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;
///#else
///import com.sun.java.swing.tree.TreeModel;
///import com.sun.java.swing.tree.TreePath;
///import com.sun.java.swing.event.TreeModelListener;
///import com.sun.java.swing.event.TreeModelEvent;
///#endif

///#ifdef JDK12
///import java.util.Arrays;
///import java.util.TreeSet;
///import java.util.HashSet;
///import java.util.Set;
///import java.util.HashMap;
///import java.util.Map;
///#else
import jode.util.Comparable;
import jode.util.Arrays;
import jode.util.TreeSet;
import jode.util.HashSet;
import jode.util.Set;
import jode.util.HashMap;
import jode.util.Map;
///#endif

import java.util.Enumeration;
import java.util.NoSuchElementException;

public class PackagesTreeModel implements TreeModel {
    Map cachedChildrens = new HashMap();

    class TreeElement implements Comparable {
	String fullName;
	String name;
	boolean leaf;

	public TreeElement(String prefix, String name, boolean isLeaf) {
	    this.fullName = prefix+name;
	    this.name = name;
	    this.leaf = isLeaf;
	}

	public String getFullName() {
	    return fullName;
	}

	public String getName() {
	    return name;
	}

	public boolean isLeaf() {
	    return leaf;
	}

	public String toString() {
	    return name;
	}

	public int compareTo(Object o) {
	    TreeElement other = (TreeElement) o;
	    if (leaf != other.leaf)
		// files come after directories
		return leaf ? 1 : -1;
	    return fullName.compareTo(other.fullName);
	}

	public boolean equals(Object o) {
	    return (o instanceof TreeElement)
		&& fullName.equals(((TreeElement)o).fullName);
	}

	public int hashCode() {
	    return fullName.hashCode();
	}
    }

    TreeElement root = new TreeElement("", "", false);
    Set listeners = new HashSet();

    public void rebuild() {
	cachedChildrens.clear();
	TreeModelListener[] ls;
	synchronized (listeners) {
	    ls = (TreeModelListener[]) 
		listeners.toArray(new TreeModelListener[listeners.size()]);
	}
	TreeModelEvent ev = new TreeModelEvent(this, new Object[] { root });
	for (int i=0; i< ls.length; i++)
	    ls[i].treeStructureChanged(ev);
    }

    public TreeElement[] getChildrens(TreeElement parent) {
	TreeElement[] result = 
	    (TreeElement[]) cachedChildrens.get(parent);
	if (result == null) {
	    TreeSet v = new TreeSet();
	    String prefix = parent == root ? "" : parent.getFullName() + ".";
	    Enumeration enum = 
		ClassInfo.getClassesAndPackages(parent.getFullName());
	    while (enum.hasMoreElements()) {
		//insert sorted and remove double elements;
		String name = (String)enum.nextElement();
		String fqn = prefix + name;
		boolean isClass = !ClassInfo.isPackage(fqn);

		if (isClass && Decompiler.skipClass(ClassInfo.forName(fqn)))
		    continue;
		TreeElement newElem = new TreeElement(prefix, name, isClass);
		v.add(newElem);
	    }
	    result = (TreeElement[]) v.toArray(new TreeElement[v.size()]);
	    cachedChildrens.put(parent, result);
	}
	return result;
    }

    public void addTreeModelListener(TreeModelListener l) {
	listeners.add(l);
    }
    public void removeTreeModelListener(TreeModelListener l) {
	listeners.remove(l);
    }
    public void valueForPathChanged(TreePath path, Object newValue) {
	// we don't allow values
    }

    public Object getChild(Object parent, int index) {
	return getChildrens((TreeElement) parent)[index];
    }

    public int getChildCount(Object parent) {
	return getChildrens((TreeElement) parent).length;
    }

    public int getIndexOfChild(Object parent, Object child) {
	TreeElement[] childrens = getChildrens((TreeElement) parent);
	int i = Arrays.binarySearch(childrens, child);
	if (i >= 0)
	    return i;
	throw new NoSuchElementException
	    (((TreeElement)parent).getFullName() + "." + child);
    }

    public Object getRoot() {
	return root;
    }

    public boolean isLeaf(Object node) {
	return ((TreeElement)node).isLeaf();
    }

    public String getFullName(Object node) {
	return ((TreeElement) node).getFullName();
    }
}