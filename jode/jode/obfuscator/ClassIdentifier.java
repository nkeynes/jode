/* ClassIdentifier Copyright (C) 1999 Jochen Hoenicke.
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
import jode.bytecode.*;
import java.lang.reflect.Modifier;
import java.util.*;
import java.io.*;

public class ClassIdentifier extends Identifier {
    ClassBundle bundle;
    PackageIdentifier pack;
    String name;
    ClassInfo info;
    boolean willStrip;
    String superName;
    String[] ifaceNames;

    int fieldCount;
    /* The first fieldCount are of type FieldIdentifier, the remaining
     * are MethodIdentifier
     */
    Identifier[] identifiers;
    Vector knownSubClasses = new Vector();
    Vector virtualReachables = new Vector();

    public ClassIdentifier(ClassBundle bundle, PackageIdentifier pack, 
			   String name, ClassInfo info) {
	super(name);
	this.bundle = bundle;
	this.pack = pack;
	this.name = name;
	this.info = info;
    }

    public void addSubClass(ClassIdentifier ci) {
	knownSubClasses.addElement(ci);
	Enumeration enum = virtualReachables.elements();
	while (enum.hasMoreElements()) {
	    String[] method = (String[]) enum.nextElement();
	    ci.reachableIdentifier(method[0], method[1], true);
	}
    }

    public void preserveMatchingIdentifier(WildCard wildcard) {
	String fullName = getFullName() + ".";
	for (int i=0; i< identifiers.length; i++) {
	    if (wildcard.matches(fullName + identifiers[i].getName())
		|| wildcard.matches(fullName + identifiers[i].getName()
				    + "." +identifiers[i].getType())) {
		if (GlobalOptions.verboseLevel > 1)
		    GlobalOptions.err.println("preserving "+identifiers[i]);
		setPreserved();
		identifiers[i].setPreserved();
		identifiers[i].setReachable();
	    }		
	}
    }

    public void preserveIdentifier(String name, String typeSig) {
	for (int i=0; i< identifiers.length; i++) {
	    if (name.equals(identifiers[i].getName())
		&& typeSig.equals(identifiers[i].getType()))
		identifiers[i].setPreserved();
	}
    }

    public void applyPreserveRule(int preserveRule) {
	if ((preserveRule & (info.getModifiers() ^ Modifier.PRIVATE)) != 0) {
	    setReachable();
	    setPreserved();
	}
	for (int i=0; i< identifiers.length; i++)
	    identifiers[i].applyPreserveRule(preserveRule);
    }

    public void reachableIdentifier(String name, String typeSig,
				    boolean isVirtual) {
//  	if (!isVirtual || (info.getModifiers() & Modifier.ABSTRACT) == 0) {
	    for (int i=0; i< identifiers.length; i++) {
		if (name.equals(identifiers[i].getName())
		    && typeSig.equals(identifiers[i].getType()))
		    identifiers[i].setReachable();
	    }
//  	}
	if (isVirtual) {
	    Enumeration enum = knownSubClasses.elements();
	    while (enum.hasMoreElements())
		((ClassIdentifier)enum.nextElement())
		    .reachableIdentifier(name, typeSig, isVirtual);
	    virtualReachables.addElement
		(new String[] { name, typeSig });
	}
    }

    public void chainIdentifier(Identifier ident) {
	String name = ident.getName();
	String typeSig = ident.getType();
	for (int i=0; i< identifiers.length; i++) {
	    if (identifiers[i].getName().equals(ident.getName())
		&& (identifiers[i].getType().equals(typeSig)))
		ident.addShadow(identifiers[i]);
	}
    }

    /**
     * Preserve all fields, that are necessary, to serialize
     * a compatible class.
     */
    public void preserveSerializable() {
	preserveIdentifier("writeObject", "(Ljava.io.ObjectOutputStream)V");
	preserveIdentifier("readObject", "(Ljava.io.ObjectOutputStream)V");
	if (Obfuscator.preserveSerial) {
	    /* XXX - add a field serializableVersionUID if not existent */
	    preserveIdentifier("serializableVersionUID", "I");
	    for (int i=0; i < fieldCount; i++) {
		FieldIdentifier ident = (FieldIdentifier) identifiers[i];
		if ((ident.info.getModifiers() 
		     & (Modifier.TRANSIENT | Modifier.STATIC)) == 0)
		    identifiers[i].setPreserved();
	    }
	}
    }

    public void initSuperClasses(ClassInfo superclass) {
	while (superclass != null) {
	    if (superclass.getName().equals("java.lang.Serializable"))
		preserveSerializable();
	    
	    ClassIdentifier superident
		= bundle.getClassIdentifier(superclass.getName());
	    if (superident != null) {
		for (int i=superident.fieldCount;
		     i < superident.identifiers.length; i++) {
		    MethodIdentifier mid = (MethodIdentifier) 
			superident.identifiers[i];
		    // all virtual methods in superclass must be chained.
		    int modif = mid.info.getModifiers();
		    if (((Modifier.PRIVATE 
			  | Modifier.STATIC
			  | Modifier.FINAL) & modif) == 0
			&& !(mid.getName().equals("<init>"))) {
			// chain the preserved/same name lists.
			chainIdentifier(superident.identifiers[i]);
		    }
		}
	    } else {
		// all methods and fields in superclass are preserved!
		MethodInfo[] topmethods = superclass.getMethods();
		for (int i=0; i< topmethods.length; i++) {
		    // all virtual methods in superclass may be
		    // virtually reachable
		    int modif = topmethods[i].getModifiers();
		    if (((Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL)
			 & modif) == 0
			&& !topmethods[i].getName().equals("<init>")) {
			reachableIdentifier
			    (topmethods[i].getName(), topmethods[i].getType(),
			     true);
			preserveIdentifier
			    (topmethods[i].getName(), topmethods[i].getType());
		    }
		}
	    }
	    ClassInfo[] ifaces = superclass.getInterfaces();
	    for (int i=0; i < ifaces.length; i++)
		initSuperClasses(ifaces[i]);
	    superclass = superclass.getSuperclass();
	}
    }

    public void setSingleReachable() {
	super.setSingleReachable();
	bundle.analyzeIdentifier(this);
    }
    
    public void analyze() {
	if (GlobalOptions.verboseLevel > 0)
	    GlobalOptions.err.println("Reachable: "+this);
    }

    public void initClass() {
	info.loadInfo(info.FULLINFO);

	FieldInfo[] finfos   = info.getFields();
	MethodInfo[] minfos  = info.getMethods();
	if (Obfuscator.swapOrder) {
	    Random rand = new Random();
///#ifdef JDK12
///	    Collections.shuffle(Arrays.asList(finfos), rand);
///	    Collections.shuffle(Arrays.asList(minfos), rand);
///#else
	    for (int i=1; i < finfos.length; i++) {
		int j = (Math.abs(rand.nextInt()) % (i+1)); 
		if (j != i) {
		    FieldInfo tmp = finfos[i];
		    finfos[i] = finfos[j];
		    finfos[j] = tmp;
		}
	    }
	    for (int i=1; i < minfos.length; i++) {
		int j = (Math.abs(rand.nextInt()) % (i+1)); 
		if (j != i) {
		    MethodInfo tmp = minfos[i];
		    minfos[i] = minfos[j];
		    minfos[j] = tmp;
		}
	    }
///#endif
	}
	fieldCount = finfos.length;
	identifiers = new Identifier[finfos.length + minfos.length];
	for (int i=0; i< fieldCount; i++) {
	    identifiers[i] = new FieldIdentifier(this, finfos[i]);
	}
	for (int i=0; i< minfos.length; i++) {
	    identifiers[fieldCount + i]
		= new MethodIdentifier(this, minfos[i]);
	    if (identifiers[fieldCount + i].getName().equals("<clinit>")) {
		/* If there is a static initializer, it is automagically
		 * reachable (even if this class wouldn't be otherwise).
		 */
		identifiers[fieldCount + i].setPreserved();
		identifiers[fieldCount + i].setReachable();
	    } else if (identifiers[fieldCount + i].getName().equals("<init>"))
		identifiers[fieldCount + i].setPreserved();
	}

	ClassInfo[] ifaces = info.getInterfaces();
	ifaceNames = new String[ifaces.length];
	for (int i=0; i < ifaces.length; i++) {
	    ifaceNames[i] = ifaces[i].getName();
	    ClassIdentifier ifaceident
		= bundle.getClassIdentifier(ifaceNames[i]);
	    if (ifaceident != null) {
		ifaceident.addSubClass(this);
	    }
	    initSuperClasses(ifaces[i]);
	}

	if (info.getSuperclass() != null) {
	    superName = info.getSuperclass().getName();
	    ClassIdentifier superident
		= bundle.getClassIdentifier(superName);
	    if (superident != null) {
		superident.addSubClass(this);
	    }
	    initSuperClasses(info.getSuperclass());
	}
    }

    public void buildTable(int renameRule) {
	super.buildTable(renameRule);
	for (int i=0; i < identifiers.length; i++)
	    if (!Obfuscator.shouldStrip || identifiers[i].isReachable())
		identifiers[i].buildTable(renameRule);
    }

    public void readTable(Hashtable table) {
	super.readTable(table);
	for (int i=0; i < identifiers.length; i++)
	    if (!Obfuscator.shouldStrip || identifiers[i].isReachable())
		identifiers[i].readTable(table);
    }

    public void writeTable(Hashtable table) {
	super.writeTable(table);
	for (int i=0; i < identifiers.length; i++)
	    if (!Obfuscator.shouldStrip || identifiers[i].isReachable())
		identifiers[i].writeTable(table);
    }

    public void addIfaces(Vector result, String[] ifaces) {
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident
		= bundle.getClassIdentifier(ifaces[i]);
	    if (ifaceident != null && !ifaceident.isReachable())
		addIfaces(result, ifaceident.ifaceNames);
	    else
		result.addElement(ClassInfo.forName(ifaces[i]));
	}
    }

    /**
     * Generates the new super class and interfaces, removing super
     * classes and interfaces that are not reachable.
     * @return an array of class names (full qualified, dot separated)
     * where the first entry is the super class (may be null) and the
     * other entries are the interfaces.
     */
    public void transformSuperIfaces() {
	if (!Obfuscator.shouldStrip)
	    return;

	Vector newIfaces = new Vector();
	addIfaces(newIfaces, ifaceNames);
	String nameOfSuper = superName;
	while (true) {
	    ClassIdentifier superident
		= bundle.getClassIdentifier(nameOfSuper);
	    if (superident == null || superident.isReachable())
		break;

	    addIfaces(newIfaces, superident.ifaceNames);
	    nameOfSuper = superident.superName;
	}
	ClassInfo[] ifaces = new ClassInfo[newIfaces.size()];
	newIfaces.copyInto(ifaces);
	info.setSuperclass(ClassInfo.forName(nameOfSuper));
	info.setInterfaces(ifaces);
    }

    public void transformInnerClasses() {
	InnerClassInfo[] innerClasses = info.getInnerClasses();
	if (innerClasses == null)
	    return;

	int newInnerCount;
	if (Obfuscator.shouldStrip) {
	    newInnerCount = 0;
	    for (int i=0; i < innerClasses.length; i++) {
		ClassIdentifier innerIdent
		    = bundle.getClassIdentifier(innerClasses[i].inner);
		if (innerIdent == null || innerIdent.isReachable())
			newInnerCount++;
	    }
	} else
	    newInnerCount = innerClasses.length;

	InnerClassInfo[] newInners = new InnerClassInfo[newInnerCount];
	newInnerCount = 0;

	for (int i=0; i<innerClasses.length; i++) {
	    ClassIdentifier innerIdent
		= bundle.getClassIdentifier(innerClasses[i].inner);
	    if (innerIdent != null && !innerIdent.isReachable())
		continue;

	    String inner, outer, name;
	    if (innerIdent == null) {
		inner = innerClasses[i].inner;
	    } else {
		inner = innerIdent.getAlias();
	    }
	    if (innerClasses[i].outer == null) {
		outer = null;
	    } else {
		ClassIdentifier outerIdent
		    = bundle.getClassIdentifier(innerClasses[i].outer);
		if (outerIdent != null) {
		    if (Obfuscator.shouldStrip && !outerIdent.isReachable())
			outer = null;
		    else
			outer = outerIdent.getAlias();
		} else {
		    outer = innerClasses[i].outer;
		}
	    }
	    if (innerClasses[i].name == null)
		/* This is an anonymous class */
		name = null;
	    else if (outer != null && inner.startsWith(outer+"$"))
		name = inner.substring(outer.length()+1);
	    else
		name = inner;
	    newInners[newInnerCount++] = new InnerClassInfo
		(inner, outer, name, innerClasses[i].modifiers);
	}
	info.setInnerClasses(newInners);
    }

    public void doTransformations() {
	if (GlobalOptions.verboseLevel > 0)
	    GlobalOptions.err.println("Transforming "+this);
	info.setName(getFullAlias());
	transformSuperIfaces();
	transformInnerClasses();

	int newFieldCount = 0, newMethodCount = 0;
	if (Obfuscator.shouldStrip) {
	    for (int i=0; i < fieldCount; i++)
		if (identifiers[i].isReachable())
		    newFieldCount++;
	    for (int i=fieldCount; i < identifiers.length; i++)
		if (identifiers[i].isReachable())
		    newMethodCount++;
	} else {
	    newFieldCount = fieldCount;
	    newMethodCount = identifiers.length - fieldCount;
	}

	FieldInfo[] newFields = new FieldInfo[newFieldCount];
	MethodInfo[] newMethods = new MethodInfo[newMethodCount];
	newFieldCount = newMethodCount = 0;

	for (int i=0; i < fieldCount; i++) {
	    if (!Obfuscator.shouldStrip || identifiers[i].isReachable()) {
		((FieldIdentifier)identifiers[i]).doTransformations();
		newFields[newFieldCount++]
		    = ((FieldIdentifier)identifiers[i]).info;
	    }
	}
	for (int i=fieldCount; i < identifiers.length; i++) {
	    if (!Obfuscator.shouldStrip || identifiers[i].isReachable()) {
		((MethodIdentifier)identifiers[i]).doTransformations();
		newMethods[newMethodCount++]
		    = ((MethodIdentifier)identifiers[i]).info;
	    }
	}

	info.setFields(newFields);
	info.setMethods(newMethods);
    }
    
    public void storeClass(DataOutputStream out) throws IOException {
	if (GlobalOptions.verboseLevel > 0)
	    GlobalOptions.err.println("Writing "+this);
	info.write(out);
	info = null;
	identifiers = null;
    }

    public Identifier getParent() {
	return pack;
    }

    /**
     * @return the full qualified name, excluding trailing dot.
     */
    public String getFullName() {
	if (pack.parent == null)
	    return getName();
	else 
	    return pack.getFullName() + "." + getName();
    }

    /**
     * @return the full qualified alias, excluding trailing dot.
     */
    public String getFullAlias() {
	return pack.getFullAlias() + getAlias();
    }

    public String getName() {
	return name;
    }

    public String getType() {
	return "Ljava/lang/Class;";
    }

    public String toString() {
	return "ClassIdentifier "+getFullName();
    }

    public Identifier getIdentifier(String fieldName, String typeSig) {
	for (int i=0; i < identifiers.length; i++) {
	    if (identifiers[i].getName().equals(fieldName)
		&& identifiers[i].getType().startsWith(typeSig))
		return identifiers[i];
	}
	
	for (int i=0; i < ifaceNames.length; i++) {
	    ClassIdentifier ifaceident
		= bundle.getClassIdentifier(ifaceNames[i]);
	    if (ifaceident != null) {
		Identifier ident
		    = ifaceident.getIdentifier(fieldName, typeSig);
		if (ident != null)
		    return ident;
	    }
	}

	if (superName != null) {
	    ClassIdentifier superident
		= bundle.getClassIdentifier(superName);
	    if (superident != null) {
		Identifier ident 
		    = superident.getIdentifier(fieldName, typeSig);
		if (ident != null)
		    return ident;
	    }
	}
	return null;
    }

    public static boolean containFieldAlias(ClassInfo clazz, 
					    String fieldName, String typeSig) {
	FieldInfo[] finfos = clazz.getFields();
	for (int i=0; i< finfos.length; i++) {
	    if (finfos[i].getName().equals(fieldName)
		&& finfos[i].getType().startsWith(typeSig))
		return true;
	}
	
	ClassInfo[] ifaces = clazz.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    if (containFieldAlias(ifaces[i], fieldName, typeSig))
		return true;
	}

	if (clazz.getSuperclass() != null) {
	    if (containFieldAlias(clazz.getSuperclass(),
				  fieldName, typeSig))
		return true;
	}
	return false;
    }

    public boolean containsFieldAliasDirectly(String fieldName, 
					      String typeSig) {
	for (int i=0; i < fieldCount; i++) {
	    if ((!Obfuscator.shouldStrip || identifiers[i].isReachable())
		&& identifiers[i].getAlias().equals(fieldName)
		&& identifiers[i].getType().startsWith(typeSig))
		return true;
	}
	return false;
    }

    public boolean containFieldAlias(String fieldName, String typeSig) {
	if (containsFieldAliasDirectly(fieldName,typeSig))
	    return true;
	for (int i=0; i < fieldCount; i++) {
	    if ((!Obfuscator.shouldStrip || identifiers[i].isReachable())
		&& identifiers[i].getAlias().equals(fieldName)
		&& identifiers[i].getType().startsWith(typeSig))
		return true;
	}
	
	ClassInfo[] ifaces = info.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident
		= bundle.getClassIdentifier(ifaces[i].getName());
	    if (ifaceident != null) {
		if (ifaceident.containFieldAlias(fieldName, typeSig))
		    return true;
	    } else {
		if (containFieldAlias(ifaces[i], fieldName, typeSig))
		    return true;
	    }
	}

	if (info.getSuperclass() != null) {
	    ClassIdentifier superident
		= bundle.getClassIdentifier(info.getSuperclass().getName());
	    if (superident != null) {
		if (superident.containFieldAlias(fieldName, typeSig))
		    return true;
	    } else {
		if (containFieldAlias(info.getSuperclass(), 
				      fieldName, typeSig))
		    return true;
	    }
	}
	return false;
    }

    public static Object getMethod(ClassInfo clazz, 
				    String methodName, String paramType) {
	MethodInfo[] minfos = clazz.getMethods();
	for (int i=0; i< minfos.length; i++) {
	    if (minfos[i].getName().equals(methodName)
		&& minfos[i].getType().startsWith(paramType))
		return minfos[i];
	}
	
	ClassInfo[] ifaces = clazz.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    Object result = getMethod(ifaces[i], methodName, paramType);
	    if (result != null)
		return result;
	}

	if (clazz.getSuperclass() != null) {
	    Object result = getMethod(clazz.getSuperclass(),
				      methodName, paramType);
	    if (result != null)
		return result;
	}
	return null;
    }

    public boolean hasMethod(String methodName, String paramType) {
	for (int i=fieldCount; i< identifiers.length; i++) {
	    if ((!Obfuscator.shouldStrip || identifiers[i].isReachable())
		&& identifiers[i].getAlias().equals(methodName)
		&& identifiers[i].getType().startsWith(paramType))
		return true;
	}
	return false;
    }

    public Object getMethod(String methodName, String paramType) {
	for (int i=fieldCount; i< identifiers.length; i++) {
	    if ((!Obfuscator.shouldStrip || identifiers[i].isReachable())
		&& identifiers[i].getAlias().equals(methodName)
		&& identifiers[i].getType().startsWith(paramType))
		return identifiers[i];
	}
	ClassInfo[] ifaces = info.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident
		= bundle.getClassIdentifier(ifaces[i].getName());
	    if (ifaceident != null) {
		Object result = ifaceident.getMethod(methodName, paramType);
		if (result != null)
		    return result;
	    } else {
		Object result = getMethod(ifaces[i], methodName, paramType);
		if (result != null)
		    return result;
	    }
	}

	if (info.getSuperclass() != null) {
	    ClassIdentifier superident
		= bundle.getClassIdentifier(info.getSuperclass().getName());
	    if (superident != null) {
		Object result = superident.getMethod(methodName, paramType);
		if (result != null)
		    return result;
	    } else {
		Object result = getMethod(info.getSuperclass(), 
					  methodName, paramType);
		if (result != null)
		    return result;
	    }
	}
	return null;
    }

    public boolean conflicting(String newAlias, boolean strong) {
	return pack.contains(newAlias);
    }
}
