/* MethodIdentifier Copyright (C) 1999 Jochen Hoenicke.
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
import jode.AssertError;
import jode.GlobalOptions;
import jode.bytecode.*;
import jode.type.Type;

///#ifdef JDK12
///import java.util.Collections;
///import java.util.Iterator;
///import java.util.Map;
///#else
import jode.util.Collections;
import jode.util.Iterator;
import jode.util.Map;
///#endif

///#ifdef JDK12
///import java.lang.ref.SoftReference;
///#endif
import java.lang.reflect.Modifier;
import java.util.BitSet;

public class MethodIdentifier extends Identifier implements Opcodes {
    ClassIdentifier clazz;
    MethodInfo info;
    String name;
    String type;

    boolean globalSideEffects;
    BitSet localSideEffects;

    /**
     * The code analyzer of this method, or null if there isn't any.
     */
    CodeAnalyzer codeAnalyzer;

    public MethodIdentifier(ClassIdentifier clazz, MethodInfo info) {
	super(info.getName());
	this.name = info.getName();
	this.type = info.getType();
	this.clazz = clazz;
	this.info  = info;

	BytecodeInfo bytecode = info.getBytecode();
	if (bytecode != null) {
	    if ((Main.stripping & Main.STRIP_LVT) != 0)
		info.getBytecode().setLocalVariableTable(null);
	    if ((Main.stripping & Main.STRIP_LNT) != 0)
		info.getBytecode().setLineNumberTable(null);
	    codeAnalyzer = Main.createCodeAnalyzer();
	}

    }

    public Iterator getChilds() {
	return Collections.EMPTY_LIST.iterator();
    }

    public void setSingleReachable() {
	super.setSingleReachable();
	Main.getClassBundle().analyzeIdentifier(this);
    }

    public void analyze() {
	if (GlobalOptions.verboseLevel > 1)
	    GlobalOptions.err.println("Analyze: "+this);

	String type = getType();
	int index = type.indexOf('L');
	while (index != -1) {
	    int end = type.indexOf(';', index);
	    Main.getClassBundle()
		.reachableIdentifier(type.substring(index+1, end)
					     , false);
	    index = type.indexOf('L', end);
	}

	String[] exceptions = info.getExceptions();
	if (exceptions != null) {
	    for (int i=0; i< exceptions.length; i++)
		Main.getClassBundle()
		    .reachableIdentifier(exceptions[i], false);
	}

	BytecodeInfo code = info.getBytecode();
	if (code != null)
	    codeAnalyzer.analyzeCode(this, code);
    }

    public void readTable(Map table) {
	setAlias((String) table.get(getFullName() + "." + getType()));
    }

    public void writeTable(Map table) {
	table.put(getFullAlias() + "."
		  + Main.getClassBundle().getTypeAlias(getType()), getName());
    }

    public Identifier getParent() {
	return clazz;
    }

    public String getFullName() {
	return clazz.getFullName() + "." + getName();
    }

    public String getFullAlias() {
	return clazz.getFullAlias() + "." + getAlias();
    }

    public String getName() {
	return name;
    }

    public String getType() {
	return type;
    }

    public boolean conflicting(String newAlias) {
	return clazz.methodConflicts(this, newAlias);
    }

    public String toString() {
	return "MethodIdentifier "+getFullName()+"."+getType();
    }

    public boolean hasGlobalSideEffects() {
	return globalSideEffects;
    }

    public boolean getLocalSideEffects(int paramNr) {
	return globalSideEffects || localSideEffects.get(paramNr);
    }

    public void setGlobalSideEffects() {
	globalSideEffects = true;
    }

    public void setLocalSideEffects(int paramNr) {
	localSideEffects.set(paramNr);
    }

    /**
     * This method does the code transformation.  This include
     * <ul><li>new slot distribution for locals</li>
     *     <li>obfuscating transformation of flow</li>
     *     <li>renaming field, method and class references</li>
     * </ul>
     */
    boolean wasTransformed = false;
    public void doTransformations() {
	if (wasTransformed)
	    throw new jode.AssertError
		("doTransformation called on transformed method");
	wasTransformed = true;
	info.setName(getAlias());
	info.setType(Main.getClassBundle().getTypeAlias(type));
	if (codeAnalyzer != null) {
	    BytecodeInfo bytecode = info.getBytecode();
	    try {
		codeAnalyzer.transformCode(bytecode);
		for (Iterator i = Main.getCodeTransformers().iterator(); 
		     i.hasNext(); ) {
		    CodeTransformer transformer = (CodeTransformer) i.next();
		    transformer.transformCode(bytecode);
		}
	    } catch (RuntimeException ex) {
		ex.printStackTrace(GlobalOptions.err);
		bytecode.dumpCode(GlobalOptions.err);
	    }

	    for (Instruction instr = bytecode.getFirstInstr(); 
		 instr != null; instr = instr.nextByAddr) {
		switch (instr.opcode) {
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokeinterface:
		case opc_invokevirtual: {
		    instr.objData = Main.getClassBundle()
			.getReferenceAlias((Reference) instr.objData);
		    break;

		}
		case opc_putstatic:
		case opc_putfield:
		case opc_getstatic:
		case opc_getfield: {
		    instr.objData = Main.getClassBundle()
			.getReferenceAlias((Reference) instr.objData);
		    break;
		}
		case opc_new:
		case opc_checkcast:
		case opc_instanceof:
		case opc_multianewarray: {
		    instr.objData = Main.getClassBundle()
			.getTypeAlias((String) instr.objData);
		    break;
		}
		}
	    }
	
	    Handler[] handlers = bytecode.getExceptionHandlers();
	    for (int i=0; i< handlers.length; i++) {
		if (handlers[i].type != null) {
		    ClassIdentifier ci = Main.getClassBundle()
			.getClassIdentifier(handlers[i].type);
		    if (ci != null)
		    handlers[i].type = ci.getFullAlias();
		}
	    }
	    info.setBytecode(bytecode);
	}
    }
}
