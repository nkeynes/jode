/* PackageIdentifier Copyright (C) 1999 Jochen Hoenicke.
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
import jode.bytecode.ClassInfo;
import jode.bytecode.FieldInfo;
import jode.bytecode.MethodInfo;
import java.lang.reflect.Modifier;
import java.io.*;
import java.util.Vector;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

///#ifdef JDK12
///import java.util.Map;
///import java.util.HashMap;
///import java.util.Iterator;
///#else
import jode.util.Map;
import jode.util.HashMap;
import jode.util.Iterator;
///#endif

public class PackageIdentifier extends Identifier {
    ClassBundle bundle;
    PackageIdentifier parent;
    String name;

    boolean loadOnDemand;
    Map loadedClasses;

    public PackageIdentifier(ClassBundle bundle, 
			     PackageIdentifier parent,
			     String name, boolean loadOnDemand) {
	super(name);
	this.bundle = bundle;
	this.parent = parent;
	this.name = name;
	this.loadedClasses = new HashMap();
	if (loadOnDemand)
	    setLoadOnDemand();
    }

    /**
     * Marks the parent package as preserved, too.
     */
    protected void setSinglePreserved() {
	if (parent != null)
	    parent.setPreserved();
    }

    public void setLoadOnDemand() {
	if (loadOnDemand)
	    return;
	loadOnDemand = true;
	if ((Main.stripping & Main.STRIP_UNREACH) == 0) {
	    // Load all classes and packages now, so they don't get stripped
	    Vector v = new Vector();
	    Enumeration enum = 
		ClassInfo.getClassesAndPackages(getFullName());
	    while (enum.hasMoreElements()) {
		//insert sorted and remove double elements;
		String subclazz = (String)enum.nextElement();
		for (int i=0; ; i++) {
		    if (i == v.size()) {
			v.addElement(subclazz);
			break;
		    }
		    int compare = subclazz.compareTo((String)v.elementAt(i));
		    if (compare < 0) {
			v.insertElementAt(subclazz, i);
			break;
		    } else if (compare == 0)
			break;
		}
	    }
	    enum = v.elements();
	    while (enum.hasMoreElements()) {
		String subclazz = (String) enum.nextElement();
		String fullname = getFullName();
		fullname = (fullname.length() > 0)
		    ? fullname + "."+ subclazz
		    : subclazz;
		if (ClassInfo.isPackage(fullname)) {
		    Identifier ident = new PackageIdentifier
			(bundle, this, subclazz, true);
		    loadedClasses.put(subclazz, ident);
		} else {
		    Identifier ident = new ClassIdentifier
			(this, subclazz, ClassInfo.forName(fullname));
		    loadedClasses.put(subclazz, ident);
		    ((ClassIdentifier) ident).initClass();
		    if (bundle.preserveRule != null)
			ident.applyPreserveRule(bundle.preserveRule);
		}
	    }		
	}
    }

    public Identifier getIdentifier(String name) {
	if (loadOnDemand) {
	    Identifier ident = loadClass(name);
	    return ident;
	}

	int index = name.indexOf('.');
	if (index == -1)
	    return (Identifier) loadedClasses.get(name);
	else {
	    PackageIdentifier pack = (PackageIdentifier) 
		loadedClasses.get(name.substring(0, index));
	    if (pack != null)
		return pack.getIdentifier(name.substring(index+1));
	    else
		return null;
	}
    }

    public Identifier loadClass(String name) {
	int index = name.indexOf('.');
	if (index == -1) {
	    Identifier ident = (Identifier) loadedClasses.get(name);
	    if (ident == null) {
		String fullname = getFullName();
		fullname = (fullname.length() > 0)
		    ? fullname + "."+ name
		    : name;
		if (ClassInfo.isPackage(fullname)) {
		    ident = new PackageIdentifier(bundle, this, name, true);
		    loadedClasses.put(name, ident);
		} else if (!ClassInfo.exists(fullname)) {
		    GlobalOptions.err.println("Warning: Can't find class "
					      + fullname);
		    Thread.dumpStack();
		} else {
		    ident = new ClassIdentifier(this, name, 
						ClassInfo.forName(fullname));
		    loadedClasses.put(name, ident);
		    ((ClassIdentifier) ident).initClass();
		    if (bundle.preserveRule != null)
			ident.applyPreserveRule(bundle.preserveRule);
		}
	    }
	    return ident;
	} else {
	    String subpack = name.substring(0, index);
	    PackageIdentifier pack = 
		(PackageIdentifier) loadedClasses.get(subpack);
	    if (pack == null) {
		String fullname = getFullName();
		fullname = (fullname.length() > 0)
		    ? fullname + "."+ subpack : subpack;
		if (ClassInfo.isPackage(fullname)) {
		    pack = new PackageIdentifier(bundle, this, 
						 subpack, loadOnDemand);
		    loadedClasses.put(subpack, pack);
		}
	    }
	    
	    if (pack != null)
		return pack.loadClass(name.substring(index+1));
	    else
		return null;
	}
    }

    public void loadMatchingClasses(WildCard wildcard) {
	String component = wildcard.getNextComponent(getFullName());
	if (component != null) {
	    Identifier ident = (Identifier) loadedClasses.get(component);
	    if (ident == null) {
		String fullname = getFullName();
		fullname = (fullname.length() > 0)
		    ? fullname + "." + component
		    : component;
		if (ClassInfo.isPackage(fullname)) {
		    ident = new PackageIdentifier(bundle, this, 
						  component, loadOnDemand);
		    loadedClasses.put(component, ident);
		} else if (ClassInfo.exists(fullname)
			   && wildcard.matches(fullname)) {
		    if (GlobalOptions.verboseLevel > 1)
			GlobalOptions.err.println("loading Class " +fullname);
		    ident = new ClassIdentifier(this, component, 
						ClassInfo.forName(fullname));
		    loadedClasses.put(component, ident);
		    ((ClassIdentifier) ident).initClass();
		    if (bundle.preserveRule != null)
			ident.applyPreserveRule(bundle.preserveRule);
		} else {
		    GlobalOptions.err.println
			("Warning: Can't find class/package " + fullname);
		}
	    }
	    if (ident instanceof PackageIdentifier) {
		if (wildcard.matches(ident.getFullName())) {
		    if (GlobalOptions.verboseLevel > 0)
			GlobalOptions.err.println("loading Package "
						  +ident.getFullName());
		    ((PackageIdentifier) ident).setLoadOnDemand();
		}

		if (wildcard.startsWith(ident.getFullName()+"."))
		    ((PackageIdentifier) ident).loadMatchingClasses(wildcard);
	    }
	} else {
	    String fullname = getFullName();
	    if (fullname.length() > 0)
		fullname += ".";
	    /* Load all matching classes and packages */
	    Enumeration enum = 
		ClassInfo.getClassesAndPackages(getFullName());
	    while (enum.hasMoreElements()) {
		String subclazz = (String)enum.nextElement();
		if (loadedClasses.containsKey(subclazz))
		    continue;
		String subFull = fullname + subclazz;
		
		if (wildcard.matches(subFull)) {
		    if (ClassInfo.isPackage(subFull)) {
			if (GlobalOptions.verboseLevel > 0)
			    GlobalOptions.err.println("loading Package " +subFull);
			Identifier ident = new PackageIdentifier
			    (bundle, this, subclazz, true);
			loadedClasses.put(subclazz, ident);
		    } else {
			if (GlobalOptions.verboseLevel > 1)
			    GlobalOptions.err.println("loading Class " +subFull);
			ClassIdentifier ident = new ClassIdentifier
			    (this, subclazz, ClassInfo.forName(subFull));
			loadedClasses.put(subclazz, ident);
			((ClassIdentifier) ident).initClass();
			if (bundle.preserveRule != null)
			    ident.applyPreserveRule(bundle.preserveRule);
		    }
		} else if (ClassInfo.isPackage(subFull)
			   && wildcard.startsWith(subFull + ".")) {
		    Identifier ident = new PackageIdentifier
			(bundle, this, subclazz, loadOnDemand);
		    loadedClasses.put(subclazz, ident);
		}
	    }
	    for (Iterator i = loadedClasses.values().iterator(); 
		 i.hasNext(); ) {
		Identifier ident = (Identifier) i.next();
		if (ident instanceof PackageIdentifier) {
		    if (wildcard.matches(ident.getFullName()))
			((PackageIdentifier) ident).setLoadOnDemand();
		    
		    if (wildcard.startsWith(ident.getFullName()+"."))
			((PackageIdentifier) ident).loadMatchingClasses(wildcard);
		}
	    }
	}
    }

    public void applyPreserveRule(ModifierMatcher preserveRule) {
	for (Iterator i = loadedClasses.values().iterator(); i.hasNext(); )
	    ((Identifier) i.next()).applyPreserveRule(preserveRule);
    }

    public void reachableIdentifier(String fqn, boolean isVirtual) {
	int index = fqn.indexOf('.');
	String component = index == -1 ? fqn : fqn.substring(0, index);
	Identifier ident = getIdentifier(component);
	if (ident == null)
	    return;

	if (index == -1) {
	    ident.setReachable();
	    return;
	}

	if (ident instanceof PackageIdentifier)
	    ((PackageIdentifier) ident).reachableIdentifier
		(fqn.substring(index+1), isVirtual);
	else {
	    String method = fqn.substring(index+1);
	    index = method.indexOf('.');
	    if (index == -1) {
		((ClassIdentifier) ident).reachableIdentifier
		    (method, null, isVirtual);
	    } else {
		((ClassIdentifier) ident).reachableIdentifier
		    (method.substring(0, index), method.substring(index+1), 
		     isVirtual);
	    }
	}
    }

    public void preserveMatchingIdentifier(WildCard wildcard) {
	String component = wildcard.getNextComponent(getFullName());
	if (component != null) {
	    String fullname = getFullName();
	    fullname = (fullname.length() > 0)
		? fullname + "."+ component : component;
	    Identifier ident = (Identifier) loadedClasses.get(component);
	    if (ident == null) {
		if (!loadOnDemand) {
		    GlobalOptions.err.println
			("Warning: Didn't load package/class "+ fullname);
		    return;
		}
		if (ClassInfo.isPackage(fullname)) {
		    ident = new PackageIdentifier(bundle, this, 
						  component, loadOnDemand);
		    loadedClasses.put(component, ident);
		} else if (ClassInfo.exists(fullname)) {
		    ident = new ClassIdentifier(this, component, 
						ClassInfo.forName(fullname));
		    loadedClasses.put(component, ident);
		    ((ClassIdentifier) ident).initClass();
		    if (bundle.preserveRule != null)
			ident.applyPreserveRule(bundle.preserveRule);
		} else {
		    GlobalOptions.err.println("Warning: Can't find class "
					      + fullname);
		    return;
		}
	    }
	    if (wildcard.matches(fullname)) {
		if (GlobalOptions.verboseLevel > 1)
		    GlobalOptions.err.println("preserving "+ident);
		ident.setPreserved();
	    }
	    if (wildcard.startsWith(fullname+".")) {
		if (ident instanceof PackageIdentifier)
		    ((PackageIdentifier) ident)
			.preserveMatchingIdentifier(wildcard);
		else
		    ((ClassIdentifier) ident)
			.preserveMatchingIdentifier(wildcard);
	    }
	} else {
	    String fullname = getFullName();
	    if (fullname.length() > 0)
		fullname += ".";
	    if (loadOnDemand) {
		/* Load all matching classes and packages */
		Enumeration enum = 
		    ClassInfo.getClassesAndPackages(getFullName());
		while (enum.hasMoreElements()) {
		    String subclazz = (String)enum.nextElement();
		    if (loadedClasses.containsKey(subclazz))
			continue;
		    String subFull = fullname + subclazz;
		    
		    if (wildcard.startsWith(subFull)) {
			if (ClassInfo.isPackage(subFull)) {
			    System.err.println("is package: "+subFull);
			    Identifier ident = new PackageIdentifier
				(bundle, this, subclazz, true);
			    loadedClasses.put(subclazz, ident);
			} else {
			    ClassIdentifier ident = new ClassIdentifier
				(this, subclazz, ClassInfo.forName(subFull));
			    loadedClasses.put(subclazz, ident);
			    ((ClassIdentifier) ident).initClass();
			    if (bundle.preserveRule != null)
				ident.applyPreserveRule(bundle.preserveRule);
			}
		    } 
		}
	    }
	    for (Iterator i = loadedClasses.values().iterator(); 
		 i.hasNext(); ) {
		Identifier ident = (Identifier) i.next();
		if (wildcard.matches(ident.getFullName())) {
		    if (GlobalOptions.verboseLevel > 1)
			GlobalOptions.err.println("Preserving "+ident);
		    ident.setPreserved();
		}
		if (wildcard.startsWith(ident.getFullName()+".")) {
		    if (ident instanceof PackageIdentifier)
			((PackageIdentifier) ident)
			    .preserveMatchingIdentifier(wildcard);
		    else
			((ClassIdentifier) ident)
			    .preserveMatchingIdentifier(wildcard);
		}
	    }
	}
    }

    /**
     * @return the full qualified name.
     */
    public String getFullName() {
	if (parent != null) {
	    if (parent.getFullName().length() > 0)
		return parent.getFullName() + "." + getName();
	    else 
		return getName();
	} else
	    return "";
    }

    /**
     * @return the full qualified alias.
     */
    public String getFullAlias() {
	if (parent != null) {
	    if (parent.getFullAlias().length() > 0)
		return parent.getFullAlias() + "." + getAlias();
	    else 
		return getAlias();
	}
	return "";
    }

    public String findAlias(String className) {
	int index = className.indexOf('.');
	if (index == -1) {
	    Identifier ident = getIdentifier(className);
	    if (ident != null)
		return ident.getFullAlias();
	} else {
	    Identifier pack = getIdentifier(className.substring(0, index));
	    if (pack != null)
		return ((PackageIdentifier)pack)
		    .findAlias(className.substring(index+1));
	}
	return className;
    }

    public void buildTable(Renamer renameRule) {
	loadOnDemand = false;
	super.buildTable(renameRule);
    }

    public void doTransformations() {
	for (Iterator i = loadedClasses.values().iterator(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if (ident instanceof ClassIdentifier) {
		((ClassIdentifier) ident).doTransformations();
	    } else
		((PackageIdentifier) ident).doTransformations();
	}
    }

    public void readTable(Map table) {
	if (parent != null)
	    setAlias((String) table.get(getFullName()));
	for (Iterator i = loadedClasses.values().iterator(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if ((Main.stripping & Main.STRIP_UNREACH) == 0
		|| ident.isReachable())
		ident.readTable(table);
	}
    }

    public void writeTable(Map table) {
	if (parent != null)
	    table.put(getFullAlias(), getName());
	for (Iterator i = loadedClasses.values().iterator(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if ((Main.stripping & Main.STRIP_UNREACH) == 0
		|| ident.isReachable())
		ident.writeTable(table);
	}
    }

    public Identifier getParent() {
	return parent;
    }

    public String getName() {
	return name;
    }

    public String getType() {
	return "package";
    }

    public Iterator getChilds() {
	return loadedClasses.values().iterator();
    }

    public void storeClasses(ZipOutputStream zip) {
	for (Iterator i = loadedClasses.values().iterator(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if ((Main.stripping & Main.STRIP_UNREACH) != 0
		&& !ident.isReachable()) {
		if (GlobalOptions.verboseLevel > 4)
		    GlobalOptions.err.println("Class/Package "
					   + ident.getFullName()
					   + " is not reachable");
		continue;
	    }
	    if (ident instanceof PackageIdentifier)
		((PackageIdentifier) ident).storeClasses(zip);
	    else {
		try {
		    String filename = ident.getFullAlias().replace('.','/')
			+ ".class";
		    zip.putNextEntry(new ZipEntry(filename));
		    DataOutputStream out = new DataOutputStream
			(new BufferedOutputStream(zip));
		    ((ClassIdentifier) ident).storeClass(out);
		    out.flush();
		    zip.closeEntry();
		} catch (java.io.IOException ex) {
		    GlobalOptions.err.println("Can't write Class "
					      + ident.getName());
		    ex.printStackTrace(GlobalOptions.err);
		}
	    }
	}
    }

    public void storeClasses(File destination) {
	File newDest = (parent == null) ? destination 
	    : new File(destination, getAlias());
	if (!newDest.exists() && !newDest.mkdir()) {
	    GlobalOptions.err.println("Could not create directory "
				   +newDest.getPath()+", check permissions.");
	}
	for (Iterator i = loadedClasses.values().iterator(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if ((Main.stripping & Main.STRIP_UNREACH) != 0
		&& !ident.isReachable()) {
		if (GlobalOptions.verboseLevel > 4)
		    GlobalOptions.err.println("Class/Package "
					   + ident.getFullName()
					   + " is not reachable");
		continue;
	    }
	    if (ident instanceof PackageIdentifier)
		((PackageIdentifier) ident)
		    .storeClasses(newDest);
	    else {
		try {
		    File file = new File(newDest, ident.getAlias()+".class");
// 		    if (file.exists()) {
// 			GlobalOptions.err.println
// 			    ("Refuse to overwrite existing class file "
// 			     +file.getPath()+".  Remove it first.");
// 			return;
// 		    }
		    DataOutputStream out = new DataOutputStream
			(new BufferedOutputStream
			 (new FileOutputStream(file)));
		    ((ClassIdentifier) ident).storeClass(out);
		    out.close();
		} catch (java.io.IOException ex) {
		    GlobalOptions.err.println("Can't write Class "
					   + ident.getName());
		    ex.printStackTrace(GlobalOptions.err);
		}
	    }
	}
    }

    public String toString() {
	return (parent == null) ? "base package" : getFullName();
    }

    public boolean contains(String newAlias, Identifier except) {
	for (Iterator i = loadedClasses.values().iterator(); i.hasNext(); ) {
	    Identifier ident = (Identifier)i.next();
	    if (((Main.stripping & Main.STRIP_UNREACH) == 0
		 || ident.isReachable())
		&& ident != except
		&& ident.getAlias().equals(newAlias))
		return true;
	}
	return false;
    }

    public boolean conflicting(String newAlias) {
	return parent.contains(newAlias, this);
    }
}