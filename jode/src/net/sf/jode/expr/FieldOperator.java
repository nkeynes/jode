/* FieldOperator Copyright (C) 1998-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */

package net.sf.jode.expr;
import net.sf.jode.GlobalOptions;
import net.sf.jode.type.Type;
import net.sf.jode.type.NullType;
import net.sf.jode.type.ClassInfoType;
import net.sf.jode.bytecode.FieldInfo;
import net.sf.jode.bytecode.ClassInfo;
import net.sf.jode.bytecode.ClassPath;
import net.sf.jode.bytecode.Reference;
import net.sf.jode.bytecode.TypeSignature;
import net.sf.jode.decompiler.MethodAnalyzer;
import net.sf.jode.decompiler.ClassAnalyzer;
import net.sf.jode.decompiler.MethodAnalyzer;
import net.sf.jode.decompiler.FieldAnalyzer;
import net.sf.jode.decompiler.Options;
import net.sf.jode.decompiler.TabbedPrintWriter;
import net.sf.jode.decompiler.Scope;

import java.io.IOException;
import java.lang.reflect.Modifier;
///#def COLLECTIONS java.util
import java.util.Collection;
///#enddef

/**
 * This class contains everything shared between PutFieldOperator and
 * GetFieldOperator
 */
public abstract class FieldOperator extends Operator {
    MethodAnalyzer methodAnalyzer;
    boolean staticFlag;
    Reference ref;
    Type classType;
    ClassInfo classInfo;
    ClassPath classPath;
    String callerPackage;

    public FieldOperator(MethodAnalyzer methodAnalyzer, boolean staticFlag,
			 Reference ref) {
        super(Type.tUnknown);

	this.classPath = methodAnalyzer.getClassAnalyzer().getClassPath();

        this.methodAnalyzer = methodAnalyzer;
        this.staticFlag = staticFlag;
	this.type = Type.tType(methodAnalyzer, ref.getType());
        this.classType = Type.tType(methodAnalyzer, ref.getClazz());
	this.ref = ref;
        if (staticFlag)
            methodAnalyzer.useType(classType);
	initOperands(staticFlag ? 0 : 1);

	callerPackage = methodAnalyzer.getClassAnalyzer().getClass().getName();
	int dot = callerPackage.lastIndexOf('.');
	callerPackage = callerPackage.substring(0, dot);
	if (classType instanceof ClassInfoType) {
	    classInfo = ((ClassInfoType) classType).getClassInfo();
	    if ((Options.options & Options.OPTION_ANON) != 0
		|| (Options.options & Options.OPTION_INNER) != 0) {
		try {
		    classInfo.load(ClassInfo.OUTERCLASS);
		} catch (IOException ex) {
		    classInfo.guess(ClassInfo.OUTERCLASS);
		}
	    }
	}
    }

    public int getPriority() {
        return 950;
    }

    public void updateSubTypes() {
	if (!staticFlag)
	    subExpressions[0].setType(Type.tSubType(classType));
    }

    public void updateType() {
	updateParentType(getFieldType());
    }

    public boolean isStatic() {
        return staticFlag;
    }

    public ClassInfo getClassInfo() {
	return classInfo;
    }

    /**
     * Returns the field analyzer for the field, if the field is
     * declared in the same class or some outer class as the method
     * containing this instruction.  Otherwise it returns null.
     * @return see above.  
     */
    public FieldAnalyzer getField() {
	ClassInfo clazz = classInfo;
	if (clazz != null) {
	    ClassAnalyzer ana = methodAnalyzer.getClassAnalyzer();
	    while (true) {
		if (clazz == ana.getClazz()) {
		    int field = ana.getFieldIndex
			(ref.getName(), Type.tType(methodAnalyzer, ref.getType()));
		    if (field >= 0)
			return ana.getField(field);
		    return null;
		}
		if (ana.getParent() == null)
		    return null;
		if (ana.getParent() instanceof MethodAnalyzer)
		    ana = ((MethodAnalyzer) ana.getParent())
			.getClassAnalyzer();
		else if (ana.getParent() instanceof ClassAnalyzer)
		    ana = (ClassAnalyzer) ana.getParent();
		else 
		    throw new InternalError("Unknown parent");
	    }
	}
	return null;
    }

    public String getFieldName() {
        return ref.getName();
    }

    public Type getFieldType() {
        return Type.tType(methodAnalyzer, ref.getType());
    }

    private void loadFields(ClassInfo clazz) {
	int howMuch = (clazz.getName().startsWith(callerPackage)
		       && (clazz.getName().lastIndexOf('.')
			   < callerPackage.length()))
	    ? ClassInfo.DECLARATIONS : ClassInfo.PUBLICDECLARATIONS;
	try {
	    clazz.load(howMuch);
	} catch (IOException ex) {
	    GlobalOptions.err.println("Warning: Can't find fields of "
				      +clazz+" to detect hiding conflicts");
	    clazz.guess(howMuch);
	}
    }

    private FieldInfo getFieldInfo(ClassInfo clazz, 
				   String name, String type) {
	while (clazz != null) {
	    loadFields(clazz);
	    FieldInfo field = clazz.findField(name, type);
	    if (field != null)
		return field;

	    ClassInfo[] ifaces = clazz.getInterfaces();
	    for (int i = 0; i < ifaces.length; i++) {
		field = getFieldInfo(ifaces[i], name, type);
		if (field != null)
		    return field;
	    }

	    clazz = clazz.getSuperclass();
	}
	return null;
    }
    
    public FieldInfo getFieldInfo() {
        return getFieldInfo(classInfo, ref.getName(), ref.getType());
    }

    /**
     * Checks if we need a cast to the super class, to which the field
     * belongs.
     * @param type the canonic type of the zeroth subexpression.
     */
    public boolean needsCast(Type type) {
	if (type instanceof NullType)
	    return true;
	if (!(type instanceof ClassInfoType
	      && classType instanceof ClassInfoType))
	    return false;
	
	ClassInfo subexprClass = ((ClassInfoType) type).getClassInfo();
	FieldInfo field = getFieldInfo();
	if (field == null) {
	    /* Weird, field not existing? */
	    return false;
	}	    

	/**
	 * We need an explicit cast if we access a private field 
	 * of a parent class (which is only possible if the parent
	 * class is an outer class of the current class).
	 */
	if (Modifier.isPrivate(field.getModifiers()))
	    return subexprClass != classInfo;
	else if ((field.getModifiers() 
		  & (Modifier.PROTECTED | Modifier.PUBLIC)) == 0) {
	    /* Field is protected.  We need a cast if subexprClass is in
	     * other package than classInfo.
	     */
	    int lastDot = classInfo.getName().lastIndexOf('.');
	    if (lastDot == -1
		|| lastDot != subexprClass.getName().lastIndexOf('.')
		|| !(subexprClass.getName()
		     .startsWith(classInfo.getName().substring(0,lastDot))))
		return true;
	}
	    
	/* We also need an explicit cast if the field is hidden by a
	 * declaration in param class.
	 */
	while (classInfo != subexprClass && classInfo != null) {
	    FieldInfo[] fields = subexprClass.getFields();
	    for (int i = 0; i < fields.length; i++) {
		if (fields[i].getName().equals(ref.getName()))
		    return true;
	    }
	    subexprClass = subexprClass.getSuperclass();
	}
	return false;
    }

    /**
     * We add the named method scoped classes to the declarables.
     */
    public void fillDeclarables(Collection used) {
	ClassInfo clazz = getClassInfo();
	ClassAnalyzer clazzAna = methodAnalyzer.getClassAnalyzer(clazz);

	if ((Options.options & Options.OPTION_ANON) != 0
	    && clazz != null
	    && clazz.isMethodScoped() && clazz.getClassName() != null
	    && clazzAna != null
	    && clazzAna.getParent() == methodAnalyzer) {

	    /* This is a named method scope class, declare it.
	     * But first declare all method scoped classes,
	     * that are used inside; order does matter.
	     */
	    clazzAna.fillDeclarables(used);
	    used.add(clazzAna);
	}
	super.fillDeclarables(used);
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	boolean opIsThis = !staticFlag
	    && subExpressions[0] instanceof ThisOperator;
	String fieldName = ref.getName();
	if (staticFlag) {
	    if (!classType.equals(Type.tClass(methodAnalyzer.getClazz()))
		|| methodAnalyzer.findLocal(fieldName) != null) {
		writer.printType(classType);
		writer.breakOp();
		writer.print(".");
	    }
	} else if (needsCast(subExpressions[0].getType().getCanonic())) {
	    writer.print("(");
	    writer.startOp(writer.EXPL_PAREN, 1);
	    writer.print("(");
	    writer.printType(classType);
	    writer.print(") ");
	    writer.breakOp();
	    subExpressions[0].dumpExpression(writer, 700);
	    writer.endOp();
	    writer.print(")");
	    writer.breakOp();
	    writer.print(".");
	} else if (opIsThis) {
	    ThisOperator thisOp = (ThisOperator) subExpressions[0];
	    Scope scope = writer.getScope(thisOp.getClassInfo(),
					  Scope.CLASSSCOPE);
	    
	    if (scope == null || writer.conflicts(fieldName, scope, 
						  Scope.FIELDNAME)) {
		thisOp.dumpExpression(writer, 950);
		writer.breakOp();
		writer.print(".");
	    } else if (writer.conflicts(fieldName, scope, 
					    Scope.AMBIGUOUSNAME)
		       || (/* This is a inherited field conflicting
			    * with a field name in some outer class.
			    */
			   getField() == null 
			   && writer.conflicts(fieldName, null,
					       Scope.NOSUPERFIELDNAME))) {
		thisOp.dumpExpression(writer, 950);
		writer.breakOp();
		writer.print(".");
	    }
	} else {
	    subExpressions[0].dumpExpression(writer, 950);
	    writer.breakOp();
	    writer.print(".");
	}
	writer.print(fieldName);
    }
}
