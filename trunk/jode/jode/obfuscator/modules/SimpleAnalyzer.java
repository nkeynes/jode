/* SimpleAnalyzer Copyright (C) 1999 Jochen Hoenicke.
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

package jode.obfuscator.modules;
import jode.bytecode.Opcodes;
import jode.bytecode.ClassInfo;
import jode.bytecode.BasicBlocks;
import jode.bytecode.Block;
import jode.bytecode.Handler;
import jode.bytecode.Instruction;
import jode.bytecode.Reference;
import jode.bytecode.TypeSignature;
import jode.obfuscator.Identifier;
import jode.obfuscator.*;
import jode.GlobalOptions;

///#def COLLECTIONS java.util
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
///#enddef

public class SimpleAnalyzer implements CodeAnalyzer, Opcodes {

    public Identifier canonizeReference(Instruction instr) {
	Reference ref = instr.getReference();
	Identifier ident = Main.getClassBundle().getIdentifier(ref);
	String clName = ref.getClazz();
	String realClazzName;
	if (ident != null) {
	    ClassIdentifier clazz = (ClassIdentifier)ident.getParent();
	    realClazzName = "L" + (clazz.getFullName()
				   .replace('.', '/')) + ";";
	} else {
	    /* We have to look at the ClassInfo's instead, to
	     * point to the right method.
	     */
	    ClassInfo clazz;
	    if (clName.charAt(0) == '[') {
		/* Arrays don't define new methods (well clone(),
		 * but that can be ignored).
		 */
		clazz = ClassInfo.forName("java.lang.Object");
	    } else {
		clazz = ClassInfo.forName
		    (clName.substring(1, clName.length()-1)
		     .replace('/','.'));
	    }
	    if (instr.getOpcode() >= opc_invokevirtual) {
		while (clazz != null
		       && clazz.findMethod(ref.getName(), 
					   ref.getType()) == null)
		    clazz = clazz.getSuperclass();
	    } else {
		while (clazz != null
		       && clazz.findField(ref.getName(), 
					  ref.getType()) == null)
		    clazz = clazz.getSuperclass();
	    }

	    if (clazz == null) {
		GlobalOptions.err.println("WARNING: Can't find reference: "
					  +ref);
		realClazzName = clName;
	    } else
		realClazzName = "L" + clazz.getName().replace('.', '/') + ";";
	}
	if (!realClazzName.equals(ref.getClazz())) {
	    ref = Reference.getReference(realClazzName, 
					 ref.getName(), ref.getType());
	    instr.setReference(ref);
	}
	return ident;
    }


    /**
     * Reads the opcodes out of the code info and determine its 
     * references
     * @return an enumeration of the references.
     */
    public void analyzeCode(MethodIdentifier m, BasicBlocks bb) {
	Block[] blocks = bb.getBlocks();
	for (int i=0; i < blocks.length; i++) {
	    Instruction[] instrs = blocks[i].getInstructions();
	    for (int idx = 0; idx < instrs.length; idx++) {
		int opcode = instrs[idx].getOpcode();
		switch (opcode) {
		case opc_checkcast:
		case opc_instanceof:
		case opc_multianewarray: {
		    String clName = instrs[idx].getClazzType();
		    int k = 0;
		    while (k < clName.length() && clName.charAt(k) == '[')
			k++;
		    if (k < clName.length() && clName.charAt(k) == 'L') {
			clName = clName.substring(k+1, clName.length()-1)
			    .replace('/','.');
			Main.getClassBundle().reachableClass(clName);
		    }
		    break;
		}
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokeinterface:
		case opc_invokevirtual:
		case opc_putstatic:
		case opc_putfield:
		    m.setGlobalSideEffects();
		    /* fall through */
		case opc_getstatic:
		case opc_getfield: {
		    Identifier ident = canonizeReference(instrs[idx]);
		    if (ident != null) {
			if (opcode == opc_putstatic 
			    || opcode == opc_putfield) {
			    FieldIdentifier fi = (FieldIdentifier) ident;
			    if (!fi.isNotConstant())
				fi.setNotConstant();
			} else if (opcode == opc_invokevirtual ||
				   opcode == opc_invokeinterface) {
			    ((ClassIdentifier) ident.getParent())
				.reachableReference
				(instrs[idx].getReference(), true);
			} else {
			    ident.setReachable();
			}
		    }
		    break;
		}
		}
	    }
	}

	Handler[] handlers = bb.getExceptionHandlers();
	for (int i=0; i< handlers.length; i++) {
	    if (handlers[i].getType() != null)
		Main.getClassBundle()
		    .reachableClass(handlers[i].getType());
	}
    }

    public void transformCode(BasicBlocks bb) {
	Block[] blocks = bb.getBlocks();
	for (int i=0; i < blocks.length; i++) {
	    Instruction[] instrs = blocks[i].getInstructions();
	    ArrayList newCode = new ArrayList();
	    Block[] newSuccs = blocks[i].getSuccs();
	    for (int idx = 0; idx < instrs.length; idx++) {
		int opcode = instrs[idx].getOpcode();
		if (opcode == opc_putstatic || opcode == opc_putfield) {
		    Reference ref = instrs[idx].getReference();
		    FieldIdentifier fi = (FieldIdentifier)
			Main.getClassBundle().getIdentifier(ref);
		    if (fi != null
			&& (Main.stripping & Main.STRIP_UNREACH) != 0
			&& !fi.isReachable()) {
			/* Replace instruction with pop opcodes. */
			int stacksize = 
			    (opcode == Instruction.opc_putstatic) ? 0 : 1;
			stacksize += TypeSignature.getTypeSize(ref.getType());
			switch (stacksize) {
			case 1:
			    newCode.add(Instruction.forOpcode
					(Instruction.opc_pop));
			    continue;
			case 2:
			    newCode.add(Instruction.forOpcode
					(Instruction.opc_pop2));
			    continue;
			case 3:
			    newCode.add(Instruction.forOpcode
					(Instruction.opc_pop2));
			    newCode.add(Instruction.forOpcode
					(Instruction.opc_pop));
			    continue;
			}
		    }
		}
		newCode.add(instrs[idx]);
	    }
	    blocks[i].setCode((Instruction []) newCode.toArray(instrs),
			      newSuccs);
	}
    }
}
