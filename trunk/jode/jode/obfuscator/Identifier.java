/* Identifier Copyright (C) 1999 Jochen Hoenicke.
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
import jode.GlobalOptions;
import jode.Obfuscator;
import java.io.*;
import java.util.Hashtable;

public abstract class Identifier {
    /**
     * This is a doubly list of identifiers, that must have always
     * have the same names, and same preserved settings.
     */
    private Identifier right = null;
    private Identifier left  = null;

    private boolean reachable = false;
    private boolean preserved = false;

    private String alias = null;
    private boolean wasAliased = false;
    
    public Identifier(String alias) {
	this.alias = alias;
    }


    /**
     * Returns true, if this identifier is reachable in some way, false if it
     * is dead and can be removed.
     */
    public final boolean isReachable() {
	return reachable;
    }

    /**
     * true, if this identifier must preserve its name, false if the
     * name may be obfuscated.
     */
    public final boolean isPreserved() {
	return preserved;
    }

    /**
     * Marks this identifier as preserved. This will also make the
     * identifier reachable, if it isn't already.
     *
     * You shouldn't call this directly, but use setPreserved instead.
     */
    protected void setSinglePreserved() {
    }

    /**
     * Marks this identifier as reachable.  
     *
     * You should override this method for method identifier, which may
     * mark other methods as reachable.
     *
     * You shouldn't call this directly, but use setReachable instead.
     */
    protected void setSingleReachable() {
	if (getParent() != null)
	    getParent().setReachable();
    }

    /**
     * Mark all shadows as reachable.
     */
    public void setReachable() {
	if (!reachable) {
	    reachable = true;
	    setSingleReachable();
	}
    }

    /**
     * Mark all shadows as preserved.
     */
    public void setPreserved() {
	if (!preserved) {
	    preserved = true;
	    Identifier ptr = this;
	    while (ptr != null) {
		ptr.setSinglePreserved();
		ptr = ptr.left;
	    }
	    ptr = right;
	    while (ptr != null) {
		ptr.setSinglePreserved();
		ptr = ptr.right;
	    }
	}
    }

    public Identifier getRepresentative() {
	Identifier ptr = this;
	while (ptr.left != null)
	    ptr = ptr.left;
	return ptr;
    }

    public final boolean isRepresentative() {
	return left == null;
    }

    public final void setAlias(String name) {
	if (name != null)
	    getRepresentative().alias = name;
    }

    public final String getAlias() {
	return getRepresentative().alias;
    }

    /**
     * Mark that this identifier and the given identifier must always have
     * the same name.
     */
    public void addShadow(Identifier orig) {
	if (isPreserved() && !orig.isPreserved())
	    orig.setPreserved();
	else if (!isPreserved() && orig.isPreserved())
	    setPreserved();
	
	Identifier ptr = this;
	while (ptr.right != null)
	    ptr = ptr.right;

	/* Check if orig is already on the ptr chain */
	Identifier check = orig;
	while (check.right != null)
	    check = check.right;
	if (check == ptr)
	    return;

	while (orig.left != null)
	    orig = orig.left;
	ptr.right = orig;
	orig.left = ptr;
    }

    static int serialnr = 0;
    public void buildTable(int renameRule) {
	if (isPreserved()) {
	    if (GlobalOptions.verboseLevel > 4)
		GlobalOptions.err.println(toString() + " is preserved");
	} else if (renameRule != Obfuscator.RENAME_NONE) {
	    Identifier rep = getRepresentative();
	    if (rep.wasAliased)
		return;
	    rep.wasAliased = true;

	    if (renameRule == Obfuscator.RENAME_UNIQUE)
		rep.setAlias("xxx" + serialnr++);
	    else {
		StringBuffer newAlias = new StringBuffer();
	    next_alias:
		for (;;) {
		okay:
		    do {
			for (int pos = 0; pos < newAlias.length(); pos++) {
			    char c = newAlias.charAt(pos);
			    if (renameRule == Obfuscator.RENAME_WEAK) {
				if (c == '9') {
				    newAlias.setCharAt(pos, 'A');
				    break okay;
				} else if (c == 'Z') {
				    newAlias.setCharAt(pos, 'a');
				    break okay;
				} else if (c != 'z') {
				    newAlias.setCharAt(pos, (char)(c+1));
				    break okay;
				}
				newAlias.setCharAt(pos, pos == 0 ? 'A': '0');
			    } else {
				while (c++ < 126) {
				    if (Character.isJavaIdentifierPart(c)) {
					newAlias.setCharAt(pos, c);
					break okay;
				    }
				}
				newAlias.setCharAt(pos, '0');
			    }
			}
			newAlias.append(renameRule == Obfuscator.RENAME_WEAK
					&& newAlias.length() == 0 ? "A": "0");
		    } while (false);
		    Identifier ptr = rep;
		    while (ptr != null) {
			if (ptr.conflicting(newAlias.toString(), 
					    renameRule 
					    == Obfuscator.RENAME_STRONG))
			    continue next_alias;
			ptr = ptr.right;
		    }
		    setAlias(newAlias.toString());
		    return;
		}
	    }
	}
    }

    public void writeTable(Hashtable table) {
	table.put(getFullAlias(), getName());
    }

    public void readTable(Hashtable table) {
	Identifier rep = getRepresentative();
	if (!rep.wasAliased) {
	    String newAlias = (String) table.get(getFullName());
	    if (newAlias != null) {
		rep.wasAliased = true;
		rep.setAlias(newAlias);
	    }
	}
    }

    public abstract void applyPreserveRule(int preserveRule);
    public abstract Identifier getParent();
    public abstract String getName();
    public abstract String getType();
    public abstract String getFullName();
    public abstract String getFullAlias();
    public abstract boolean conflicting(String newAlias, boolean strong);

    /**
     * This is called by ClassBundle when it a class is added with
     * ClassBundle.analyzeIdentifier().
     */
    public void analyze() {
    }
}
