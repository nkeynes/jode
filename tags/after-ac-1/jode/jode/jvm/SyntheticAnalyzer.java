/* SyntheticAnalyzer Copyright (C) 1999 Jochen Hoenicke.
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

package jode.jvm;
import jode.GlobalOptions;
import jode.type.Type;
import jode.type.MethodType;
import java.lang.reflect.Modifier;
import jode.bytecode.*;

public class SyntheticAnalyzer implements jode.bytecode.Opcodes {
    public final static int UNKNOWN = 0;
    public final static int GETCLASS = 1;
    public final static int ACCESSGETFIELD = 2;
    public final static int ACCESSPUTFIELD = 3;
    public final static int ACCESSMETHOD = 4;
    public final static int ACCESSGETSTATIC = 5;
    public final static int ACCESSPUTSTATIC = 6;
    public final static int ACCESSSTATICMETHOD = 7;
    
    int kind = UNKNOWN;
    Reference reference;
    MethodInfo method;

    public SyntheticAnalyzer(MethodInfo method, boolean checkName) {
	this.method = method;
	if (method.getBytecode() == null)
	    return;
	if (!checkName || method.getName().equals("class$"))
	    if (checkGetClass())
		return;
	if (!checkName || method.getName().startsWith("access$"))
	    if (checkAccess())
		return;
    }

    public int getKind() {
	return kind;
    }

    public Reference getReference() {
	return reference;
    }

    private static final int[] getClassOpcodes = {
	opc_aload, opc_invokestatic, opc_areturn, 
	opc_astore, opc_new, opc_dup, opc_aload, 
	opc_invokevirtual, opc_invokespecial, opc_athrow
    };
    private static final Object[] getClassRefs = {
	null, Reference.getReference("Ljava/lang/Class;", "forName",
				     "(Ljava/lang/String;)Ljava/lang/Class;"),
	null, null, "Ljava/lang/NoClassDefFoundError;", null, null,
	Reference.getReference("Ljava/lang/Throwable;", "getMessage",
			       "()Ljava/lang/String;"),
	Reference.getReference("Ljava/lang/NoClassDefFoundError;", "<init>",
			       "(Ljava/lang/String;)V"), null
    };


    boolean checkGetClass() {
	if (!method.isStatic()
	    || !(method.getType()
		 .equals("(Ljava/lang/String;)Ljava/lang/Class;")))
	    return false;
	
	BytecodeInfo bytecode = method.getBytecode();

	Handler[] excHandlers = bytecode.getExceptionHandlers();
	if (excHandlers.length != 1)
	    return false;

	int excSlot = -1;
	Instruction instr = bytecode.getFirstInstr();
	if (excHandlers[0].start != instr)
	    return false;
	for (int i=0; i< getClassOpcodes.length; i++) {
	    if (instr.opcode != getClassOpcodes[i])
		return false;
	    if (i==0 && instr.localSlot != 0)
		return false;
	    if (i == 3)
		excSlot = instr.localSlot;
	    if (i == 6 && instr.localSlot != excSlot)
		return false;
	    if (i == 2 && excHandlers[0].end != instr)
		return false;
	    if (i == 3 && excHandlers[0].catcher != instr)
		return false;
	    if (getClassRefs[i] != null
		&& !getClassRefs[i].equals(instr.objData))
		return false;
	    instr = instr.nextByAddr;
	}
	if (instr != null)
	    return false;
	this.kind = GETCLASS;
	return true;
    }

    private final int modifierMask = (Modifier.PRIVATE | Modifier.PROTECTED | 
				      Modifier.PUBLIC | Modifier.STATIC);

    public boolean checkStaticAccess() {
	ClassInfo clazzInfo = method.getClazzInfo();
	BytecodeInfo bytecode = method.getBytecode();
	Instruction instr = bytecode.getFirstInstr();

	if (instr.opcode == opc_getstatic) {
	    Reference ref = (Reference) instr.objData;
	    String refClazz = ref.getClazz().substring(1);
	    if (!(refClazz.substring(0, refClazz.length()-1)
		  .equals(clazzInfo.getName().replace('.','/'))))
		return false;
	    FieldInfo refField
		= clazzInfo.findField(ref.getName(), ref.getType());
	    if ((refField.getModifiers() & modifierMask) != 
		(Modifier.PRIVATE | Modifier.STATIC))
		return false;
	    instr = instr.nextByAddr;
	    if (instr.opcode < opc_ireturn || instr.opcode > opc_areturn)
		return false;
	    if (instr.nextByAddr != null)
		return false;
	    /* For valid bytecode the type matches automatically */
	    reference = ref;
	    kind = ACCESSGETSTATIC;
	    return true;
	}
	int params = 0, slot = 0;
	while (instr.opcode >= opc_iload && instr.opcode <= opc_aload
	       && instr.localSlot == slot) {
	    params++;
	    slot += (instr.opcode == opc_lload 
		     || instr.opcode == opc_dload) ? 2 : 1;
	    instr = instr.nextByAddr;
	}
	if (instr.opcode == opc_putstatic) {
	    if (params != 1)
		return false;
	    /* For valid bytecode the type of param matches automatically */
	    Reference ref = (Reference) instr.objData;
	    String refClazz = ref.getClazz().substring(1);
	    if (!(refClazz.substring(0, refClazz.length()-1)
		  .equals(clazzInfo.getName().replace('.','/'))))
		return false;
	    FieldInfo refField
		= clazzInfo.findField(ref.getName(), ref.getType());
	    if ((refField.getModifiers() & modifierMask) != 
		(Modifier.PRIVATE | Modifier.STATIC))
		return false;
	    instr = instr.nextByAddr;
	    if (instr.opcode != opc_return)
		return false;
	    if (instr.nextByAddr != null)
		return false;
	    reference = ref;
	    kind = ACCESSPUTSTATIC;
	    return true;
	}
	if (instr.opcode == opc_invokestatic) {
	    Reference ref = (Reference) instr.objData;
	    String refClazz = ref.getClazz().substring(1);
	    if (!(refClazz.substring(0, refClazz.length()-1)
		  .equals(clazzInfo.getName().replace('.','/'))))
		return false;
	    MethodInfo refMethod
		= clazzInfo.findMethod(ref.getName(), ref.getType());
	    MethodType refType = Type.tMethod(ref.getType());
	    if ((refMethod.getModifiers() & modifierMask) != 
		(Modifier.PRIVATE | Modifier.STATIC)
		|| refType.getParameterTypes().length != params)
		return false;
	    instr = instr.nextByAddr;
	    if (refType.getReturnType() == Type.tVoid) {
		if (instr.opcode != opc_return)
		    return false;
	    } else {
		if (instr.opcode < opc_ireturn || instr.opcode > opc_areturn)
		    return false;
	    }
	    if (instr.nextByAddr != null)
		return false;

	    /* For valid bytecode the types matches automatically */
	    reference = ref;
	    kind = ACCESSSTATICMETHOD;
	    return true;
	}
	return false;
    }

    public boolean checkAccess() {
	ClassInfo clazzInfo = method.getClazzInfo();
	BytecodeInfo bytecode = method.getBytecode();
	Handler[] excHandlers = bytecode.getExceptionHandlers();
	if (excHandlers != null && excHandlers.length != 0)
	    return false;

	if (method.isStatic()) {
	    if (checkStaticAccess())
		return true;
	}

	Instruction instr = bytecode.getFirstInstr();
	if (instr.opcode != opc_aload || instr.localSlot != 0)
	    return false;
	instr = instr.nextByAddr;

	if (instr.opcode == opc_getfield) {
	    Reference ref = (Reference) instr.objData;
	    String refClazz = ref.getClazz().substring(1);
	    if (!(refClazz.substring(0, refClazz.length()-1)
		  .equals(clazzInfo.getName().replace('.','/'))))
		return false;
	    FieldInfo refField
		= clazzInfo.findField(ref.getName(), ref.getType());
	    if ((refField.getModifiers() & modifierMask) != Modifier.PRIVATE)
		return false;
	    instr = instr.nextByAddr;
	    if (instr.opcode < opc_ireturn || instr.opcode > opc_areturn)
		return false;
	    if (instr.nextByAddr != null)
		return false;
	    /* For valid bytecode the type matches automatically */
	    reference = ref;
	    kind = ACCESSGETFIELD;
	    return true;
	}
	int params = 0, slot = 1;
	while (instr.opcode >= opc_iload && instr.opcode <= opc_aload
	       && instr.localSlot == slot) {
	    params++;
	    slot += (instr.opcode == opc_lload 
		     || instr.opcode == opc_dload) ? 2 : 1;
	    instr = instr.nextByAddr;
	}
	if (instr.opcode == opc_putfield) {
	    if (params != 1)
		return false;
	    /* For valid bytecode the type of param matches automatically */
	    Reference ref = (Reference) instr.objData;
	    String refClazz = ref.getClazz().substring(1);
	    if (!(refClazz.substring(0, refClazz.length()-1)
		  .equals(clazzInfo.getName().replace('.','/'))))
		return false;
	    FieldInfo refField
		= clazzInfo.findField(ref.getName(), ref.getType());
	    if ((refField.getModifiers() & modifierMask) != Modifier.PRIVATE)
		return false;
	    instr = instr.nextByAddr;
	    if (instr.opcode != opc_return)
		return false;
	    if (instr.nextByAddr != null)
		return false;
	    reference = ref;
	    kind = ACCESSPUTFIELD;
	    return true;
	}
	if (instr.opcode == opc_invokespecial) {
	    Reference ref = (Reference) instr.objData;
	    String refClazz = ref.getClazz().substring(1);
	    if (!(refClazz.substring(0, refClazz.length()-1)
		  .equals(clazzInfo.getName().replace('.','/'))))
		return false;
	    MethodInfo refMethod
		= clazzInfo.findMethod(ref.getName(), ref.getType());
	    MethodType refType = Type.tMethod(ref.getType());
	    if ((refMethod.getModifiers() & modifierMask) != Modifier.PRIVATE
		|| refType.getParameterTypes().length != params)
		return false;
	    instr = instr.nextByAddr;
	    if (refType.getReturnType() == Type.tVoid) {
		if (instr.opcode != opc_return)
		    return false;
	    } else {
		if (instr.opcode < opc_ireturn || instr.opcode > opc_areturn)
		    return false;
	    }
	    if (instr.nextByAddr != null)
		return false;

	    /* For valid bytecode the types matches automatically */
	    reference = ref;
	    kind = ACCESSMETHOD;
	    return true;
	}
	return false;
    }
}