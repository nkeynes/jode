/* ClassInfo Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.bytecode;
import jode.Decompiler;
import jode.type.Type;
import java.io.*;
import java.util.*;
///#ifdef JDK12
///import java.lang.ref.WeakReference;
///import java.lang.ref.ReferenceQueue;
///#endif
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * This class does represent a class similar to java.lang.Class.  You
 * can get the super class and the interfaces.
 *
 * The main difference to java.lang.Class is, that the objects are builded
 * from a stream containing the .class file, and that it uses the 
 * <code>Type</code> to represent types instead of Class itself.
 *
 * @author Jochen Hoenicke
 */
public class ClassInfo extends BinaryInfo {
    private String name;

    private static SearchPath classpath;
///#ifdef JDK12
///    private static final Map classes = new HashMap();
///    private static final ReferenceQueue queue = new ReferenceQueue();
///#else
    private static final Hashtable classes = new Hashtable();
///#endif

    private int status = 0;

    private int modifiers = -1;
    private ClassInfo    superclass;
    private ClassInfo[]  interfaces;
    private FieldInfo[]  fields;
    private MethodInfo[] methods;
    private InnerClassInfo[] innerClasses;
    private String sourceFile;

    public final static ClassInfo javaLangObject = forName("java.lang.Object");
    
    public static void setClassPath(String path) {
        classpath = new SearchPath(path);
///#ifdef JDK12
///	java.lang.ref.Reference died;
///	while ((died = queue.poll()) != null) {
///	    classes.values().remove(died);
///	}
///	Iterator i = classes.values().iterator();
///	while (i.hasNext()) {
///	    ClassInfo ci = (ClassInfo) ((WeakReference)i.next()).get();
///	    if (ci == null) {
///		i.remove();
///		continue;
///	    }
///#else
	Enumeration enum = classes.elements();
	while (enum.hasMoreElements()) {
	    ClassInfo ci = (ClassInfo) enum.nextElement();
///#endif
	    ci.status = 0;
	    ci.superclass = null;
	    ci.fields = null;
	    ci.interfaces = null;
	    ci.methods = null;
	    ci.unknownAttributes = null;
	}
    }

    public static boolean exists(String name) {
        return classpath.exists(name.replace('.', '/') + ".class");
    }
    
    public static boolean isPackage(String name) {
        return classpath.isDirectory(name.replace('.', '/'));
    }
    
    public static Enumeration getClassesAndPackages(final String packageName) {
        final Enumeration enum = 
            classpath.listFiles(packageName.replace('.','/'));
        return new Enumeration() {
            public boolean hasMoreElements() {
                return enum.hasMoreElements();
            }
            public Object nextElement() {
                String name = (String) enum.nextElement();
                if (!name.endsWith(".class"))
		    // This is a package
		    return name;
                return name.substring(0, name.length()-6);

            }
        };
    }
    
    public static ClassInfo forName(String name) {
//          name = name.replace('/', '.');
	if (name == null
	    || name.indexOf(';') != -1
	    || name.indexOf('[') != -1
	    || name.indexOf('/') != -1)
	    throw new IllegalArgumentException("Illegal class name: "+name);
///#ifdef JDK12
///	java.lang.ref.Reference died;
///	while ((died = queue.poll()) != null) {
///	    classes.values().remove(died);
///	}
///	WeakReference ref = (WeakReference) classes.get(name);
///	ClassInfo clazz = (ref == null) ? null : (ClassInfo) ref.get();
///#else
	ClassInfo clazz = (ClassInfo) classes.get(name);
///#endif
        if (clazz == null) {
            clazz = new ClassInfo(name);
///#ifdef JDK12
///            classes.put(name, new WeakReference(clazz, queue));
///#else
            classes.put(name, clazz);
///#endif
        }
        return clazz;
    }

    public ClassInfo(String name) {
        this.name = name;
    }

    private void readHeader(DataInputStream input, int howMuch) 
	throws IOException {
	if (input.readInt() != 0xcafebabe)
	    throw new ClassFormatException("Wrong magic");
	if (input.readUnsignedShort() > 3) 
	    throw new ClassFormatException("Wrong minor");
	if (input.readUnsignedShort() != 45) 
	    throw new ClassFormatException("Wrong major");
    }

    private ConstantPool readConstants(DataInputStream input, int howMuch)
	throws IOException {
        ConstantPool cpool = new ConstantPool();
        cpool.read(input);
        return cpool;
    }

    private void readNameAndSuper(ConstantPool cpool, 
                                  DataInputStream input, int howMuch)
	throws IOException {
	modifiers = input.readUnsignedShort();
        String className = cpool.getClassName(input.readUnsignedShort());
        if (!name.equals(className))
            throw new ClassFormatException("wrong name " + className);
	String superName = cpool.getClassName(input.readUnsignedShort());
        superclass = superName != null ? ClassInfo.forName(superName) : null;
    }

    private void readInterfaces(ConstantPool cpool,
                                DataInputStream input, int howMuch)
	throws IOException {
	int count = input.readUnsignedShort();
	interfaces = new ClassInfo[count];
	for (int i=0; i< count; i++) {
            interfaces[i] = ClassInfo.forName
                (cpool.getClassName(input.readUnsignedShort()));
	}
    }

    private void readFields(ConstantPool cpool,
                            DataInputStream input, int howMuch)
	throws IOException {
        if ((howMuch & FIELDS) != 0) {
            int count = input.readUnsignedShort();
            fields = new FieldInfo[count];
            for (int i=0; i< count; i++) {
                fields[i] = new FieldInfo(this); 
                fields[i].read(cpool, input, howMuch);
            }
        } else {
            int count = input.readUnsignedShort();
            for (int i=0; i< count; i++) {
                input.readUnsignedShort();  // modifier
                input.readUnsignedShort();  // name
                input.readUnsignedShort();  // type
                skipAttributes(input);
            }
        }
    }

    private void readMethods(ConstantPool cpool, 
                             DataInputStream input, int howMuch)
	throws IOException {
        if ((howMuch & METHODS) != 0) {
            int count = input.readUnsignedShort();
            methods = new MethodInfo[count];
            for (int i=0; i< count; i++) {
                methods[i] = new MethodInfo(this); 
                methods[i].read(cpool, input, howMuch);
            }
        } else {
            int count = input.readUnsignedShort();
            for (int i=0; i< count; i++) {
                input.readUnsignedShort();  // modifier
                input.readUnsignedShort();  // name
                input.readUnsignedShort();  // type
                skipAttributes(input);
            }
        }
    }

    protected void readAttribute(String name, int length,
				 ConstantPool cp,
				 DataInputStream input, 
				 int howMuch) throws IOException {
	if (name.equals("SourceFile")) {
	    if (length != 2)
		throw new ClassFormatException("SourceFile attribute"
					       + " has wrong length");
	    sourceFile = cp.getUTF8(input.readUnsignedShort());
	} else if (name.equals("InnerClasses")) {
	    int count = input.readUnsignedShort();
	    innerClasses = new InnerClassInfo[count];
	    for (int i=0; i< count; i++) {
		int innerIndex = input.readUnsignedShort();
		int outerIndex = input.readUnsignedShort();
		int nameIndex = input.readUnsignedShort();
		String inner = cp.getClassName(innerIndex);
		String outer = 
		    outerIndex != 0 ? cp.getClassName(outerIndex) : null;
		String innername = 
		    nameIndex != 0 ? cp.getUTF8(nameIndex) : null;
		int access = input.readUnsignedShort();
		innerClasses[i] = new InnerClassInfo
		    (inner, outer, innername, access);
	    }
	    if (length != 2 + 8 * count)
		throw new ClassFormatException
		    ("InnerClasses attribute has wrong length");
	} else
	    super.readAttribute(name, length, cp, input, howMuch);
    }

    public void read(DataInputStream input, int howMuch) throws IOException {
	/* header */
	if (input.readInt() != 0xcafebabe)
	    throw new ClassFormatException("Wrong magic");
	if (input.readUnsignedShort() > 3) 
	    throw new ClassFormatException("Wrong minor");
	if (input.readUnsignedShort() != 45) 
	    throw new ClassFormatException("Wrong major");

	/* constant pool */
        ConstantPool cpool = new ConstantPool();
        cpool.read(input);

	/* always read modifiers, name, super, ifaces */
	{
	    status |= HIERARCHY;
	    modifiers = input.readUnsignedShort();
	    String className = cpool.getClassName(input.readUnsignedShort());
	    if (!name.equals(className))
		throw new ClassFormatException("wrong name " + className);
	    String superName = cpool.getClassName(input.readUnsignedShort());
	    superclass = superName != null ? ClassInfo.forName(superName) : null;
	    int count = input.readUnsignedShort();
	    interfaces = new ClassInfo[count];
	    for (int i=0; i< count; i++) {
		interfaces[i] = ClassInfo.forName
		    (cpool.getClassName(input.readUnsignedShort()));
	    }
	}	    

	/* fields */
        if ((howMuch & FIELDS) != 0) {
            int count = input.readUnsignedShort();
            fields = new FieldInfo[count];
            for (int i=0; i< count; i++) {
                fields[i] = new FieldInfo(this); 
                fields[i].read(cpool, input, howMuch);
            }
        } else {
	    byte[] skipBuf = new byte[6];
            int count = input.readUnsignedShort();
            for (int i=0; i< count; i++) {
		input.readFully(skipBuf); // modifier, name, type
                skipAttributes(input);
            }
        }

	/* methods */
        if ((howMuch & METHODS) != 0) {
            int count = input.readUnsignedShort();
            methods = new MethodInfo[count];
            for (int i=0; i< count; i++) {
                methods[i] = new MethodInfo(this); 
                methods[i].read(cpool, input, howMuch);
            }
        } else {
	    byte[] skipBuf = new byte[6];
            int count = input.readUnsignedShort();
            for (int i=0; i< count; i++) {
		input.readFully(skipBuf); // modifier, name, type
                skipAttributes(input);
            }
        }

	/* attributes */
	readAttributes(cpool, input, howMuch);
    }

    public void reserveSmallConstants(GrowableConstantPool gcp) {
	for (int i=0; i < fields.length; i++)
	    fields[i].reserveSmallConstants(gcp);

	for (int i=0; i < methods.length; i++)
	    methods[i].reserveSmallConstants(gcp);
    }

    public void prepareWriting(GrowableConstantPool gcp) {
	gcp.putClassName(name);
	gcp.putClassName(superclass.getName());
	for (int i=0; i < interfaces.length; i++)
	    gcp.putClassName(interfaces[i].getName());

	for (int i=0; i < fields.length; i++)
	    fields[i].prepareWriting(gcp);

	for (int i=0; i < methods.length; i++)
	    methods[i].prepareWriting(gcp);

	if (sourceFile != null) {
	    gcp.putUTF8("SourceFile");
	    gcp.putUTF8(sourceFile);
	}
	if (innerClasses != null) {
	    gcp.putUTF8("InnerClasses");
	    int count = innerClasses.length;
	    for (int i=0; i< count; i++) {
		gcp.putClassName(innerClasses[i].inner);
		if (innerClasses[i].outer != null)
		    gcp.putClassName(innerClasses[i].outer);
		if (innerClasses[i].name != null)
		    gcp.putUTF8(innerClasses[i].name);
	    }
	}
        prepareAttributes(gcp);
    }

    protected int getKnownAttributeCount() {
	int count = 0;
	if (sourceFile != null)
	    count++;
	if (innerClasses != null)
	    count++;
	return count;
    }

    public void writeKnownAttributes(GrowableConstantPool gcp,
				     DataOutputStream output) 
	throws IOException {
	if (sourceFile != null) {
	    output.writeShort(gcp.putUTF8("SourceFile"));
	    output.writeInt(2);
	    output.writeShort(gcp.putUTF8(sourceFile));
	}
	if (innerClasses != null) {
	    output.writeShort(gcp.putUTF8("InnerClasses"));
	    int count = innerClasses.length;
	    output.writeInt(2 + count * 8);
	    output.writeShort(count);
	    for (int i=0; i< count; i++) {
		output.writeShort(gcp.putClassName(innerClasses[i].inner));
		output.writeShort(innerClasses[i].outer != null ? 
				  gcp.putClassName(innerClasses[i].outer) : 0);
		output.writeShort(innerClasses[i].name != null ?
				  gcp.putUTF8(innerClasses[i].name) : 0);
		output.writeShort(innerClasses[i].modifiers);
	    }
	}
    }

    public void write(DataOutputStream out) throws IOException {
	GrowableConstantPool gcp = new GrowableConstantPool();
	reserveSmallConstants(gcp);
	prepareWriting(gcp);

	out.writeInt(0xcafebabe);
	out.writeShort(3);
	out.writeShort(45);
	gcp.write(out);

	out.writeShort(modifiers);
	out.writeShort(gcp.putClassName(name));
	out.writeShort(gcp.putClassName(superclass.getName()));
	out.writeShort(interfaces.length);
	for (int i=0; i < interfaces.length; i++)
	    out.writeShort(gcp.putClassName(interfaces[i].getName()));

	out.writeShort(fields.length);
	for (int i=0; i < fields.length; i++)
	    fields[i].write(gcp, out);

	out.writeShort(methods.length);
	for (int i=0; i < methods.length; i++)
	    methods[i].write(gcp, out);

        writeAttributes(gcp, out);
    }

    public void loadInfoReflection(Class clazz, int howMuch) 
	throws SecurityException {
	if ((howMuch & HIERARCHY) != 0) {
	    modifiers = clazz.getModifiers();
	    if (clazz.getSuperclass() == null)
		superclass = null;
	    else
		superclass = ClassInfo.forName
		    (clazz.getSuperclass().getName());
	    Class[] ifaces = clazz.getInterfaces();
	    interfaces = new ClassInfo[ifaces.length];
	    for (int i=0; i<ifaces.length; i++)
		interfaces[i] = ClassInfo.forName(ifaces[i].getName());
	    status |= HIERARCHY;
	}
	if ((howMuch & FIELDS) != 0 && fields == null) {
	    Field[] fs;
	    try {
		fs = clazz.getDeclaredFields();
	    } catch (SecurityException ex) {
		fs = clazz.getFields();
		Decompiler.err.println
		    ("Could only get public fields of class "
		     + name + ".");
	    }
	    fields = new FieldInfo[fs.length];
	    for (int i = fs.length; --i >= 0; ) {
		String type = Type.getSignature(fs[i].getType());
		fields[i] = new FieldInfo
		    (this, fs[i].getName(), type, fs[i].getModifiers());
	    }
	}
	if ((howMuch & METHODS) != 0 && methods == null) {
	    Method[] ms;
	    try {
		ms = clazz.getDeclaredMethods();
	    } catch (SecurityException ex) {
		ms = clazz.getMethods();
		Decompiler.err.println
		    ("Could only get public methods of class "
		     + name + ".");
	    }
	    methods = new MethodInfo[ms.length];
	    for (int i = ms.length; --i >= 0; ) {
		String type = Type.getSignature
		    (ms[i].getParameterTypes(), ms[i].getReturnType());
		methods[i] = new MethodInfo
		    (this, ms[i].getName(), type, ms[i].getModifiers());
	    }
	}
	status |= howMuch;
    }
    
    public void loadInfo(int howMuch) {
        try {
            DataInputStream input = 
                new DataInputStream(classpath.getFile(name.replace('.', '/')
                                                      + ".class"));
	    read(input, howMuch);            
            status |= howMuch;

        } catch (IOException ex) {
	    String message = ex.getMessage();
            if ((howMuch & ~(FIELDS|METHODS|HIERARCHY)) != 0) {
		Decompiler.err.println
		    ("Can't read class " + name + ".");
		ex.printStackTrace(Decompiler.err);
		throw new NoClassDefFoundError(name);
	    }
	    // Try getting the info through the reflection interface
	    // instead.
	    Class clazz = null;
	    try {
		clazz = Class.forName(name);
	    } catch (ClassNotFoundException ex2) {
	    } catch (NoClassDefFoundError ex2) {
	    }
	    try {
		loadInfoReflection(clazz, howMuch);
		return;
	    } catch (SecurityException ex2) {
		Decompiler.err.println
		    (ex2+" while collecting info about class " + name + ".");
	    }
	    
	    // Give a warning and ``guess'' the hierarchie, methods etc.
	    Decompiler.err.println
		("Can't read class " + name + ", types may be incorrect. ("
		 + ex.getClass().getName()
		 + (message != null ? ": " + message : "") + ")");
	    
	    if ((howMuch & HIERARCHY) != 0) {
		modifiers = Modifier.PUBLIC;
		if (name.equals("java.lang.Object"))
		    superclass = null;
		else
		    superclass = ClassInfo.forName("java.lang.Object");
		interfaces = new ClassInfo[0];
	    }
	    if ((howMuch & METHODS) != 0)
		methods = new MethodInfo[0];
	    if ((howMuch & FIELDS) != 0)
		fields = new FieldInfo[0];
	    status |= howMuch;
        }
    }

    public String getName() {
        return name;
    }

    public ClassInfo getSuperclass() {
        if ((status & HIERARCHY) == 0)
            loadInfo(HIERARCHY);
        return superclass;
    }
    
    public ClassInfo[] getInterfaces() {
        if ((status & HIERARCHY) == 0)
            loadInfo(HIERARCHY);
        return interfaces;
    }

    public int getModifiers() {
        if ((status & HIERARCHY) == 0)
            loadInfo(HIERARCHY);
        return modifiers;
    }

    public boolean isInterface() {
        return Modifier.isInterface(getModifiers());
    }

    public FieldInfo findField(String name, String typeSig) {
        if ((status & FIELDS) == 0)
            loadInfo(FIELDS);
        for (int i=0; i< methods.length; i++)
            if (fields[i].getName().equals(name)
                && fields[i].getType().equals(typeSig))
                return fields[i];
        return null;
    }

    public MethodInfo findMethod(String name, String typeSig) {
        if ((status & METHODS) == 0)
            loadInfo(METHODS);
        for (int i=0; i< methods.length; i++)
            if (methods[i].getName().equals(name)
                && methods[i].getType().equals(typeSig))
                return methods[i];
        return null;
    }

    public MethodInfo[] getMethods() {
        if ((status & METHODS) == 0)
            loadInfo(METHODS);
        return methods;
    }

    public FieldInfo[] getFields() {
        if ((status & FIELDS) == 0)
            loadInfo(FIELDS);
        return fields;
    }

    public void setName(String newName) {
	name = newName;
    }

    public void setSuperclass(ClassInfo newSuper) {
	superclass = newSuper;
    }
    
    public void setInterfaces(ClassInfo[] newIfaces) {
        interfaces = newIfaces;
    }

    public void setModifiers(int newModifiers) {
        modifiers = newModifiers;
    }

    public void setMethods(MethodInfo[] mi) {
        methods = mi;
    }

    public void setFields(FieldInfo[] fi) {
        fields = fi;
    }

    public boolean superClassOf(ClassInfo son) {
        while (son != this && son != null) {
            son = son.getSuperclass();
        }
        return son == this;
    }

    public boolean implementedBy(ClassInfo clazz) {
        while (clazz != this && clazz != null) {
            ClassInfo[] ifaces = clazz.getInterfaces();
            for (int i=0; i< ifaces.length; i++) {
                if (implementedBy(ifaces[i]))
                    return true;
            }
            clazz = clazz.getSuperclass();
        }
        return clazz == this;
    }

    public String toString() {
        return name;
    }
}
