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

package jode.obfuscator;
import jode.bytecode.*;
import jode.Type;

public class SimpleAnalyzer implements CodeAnalyzer, Opcodes {
    MethodIdentifier m;
    BytecodeInfo bytecode;

    public SimpleAnalyzer(BytecodeInfo bytecode,MethodIdentifier m) {
	this.m = m;
	this.bytecode = bytecode;
    }

    /**
     * Reads the opcodes out of the code info and determine its 
     * references
     * @return an enumeration of the references.
     */
    public void analyzeCode() {
	for (Instruction instr = bytecode.getFirstInstr();
	     instr != null; instr = instr.nextByAddr) {
	    switch (instr.opcode) {
	    case opc_new:
	    case opc_anewarray:
	    case opc_checkcast:
	    case opc_instanceof:
	    case opc_multianewarray: {
		String clName = (String) instr.objData;
		if (clName.charAt(0) == '[') {
		    int i;
		    for (i=0; i< clName.length(); i++)
			if (clName.charAt(i) != '[')
			    break;
		    if (i >= clName.length() || clName.charAt(i) != 'L')
			break;
		    int index = clName.indexOf(';', i);
		    if (index != clName.length()-1)
			break;
		    clName = clName.substring(i+1, index);
		}
		m.clazz.bundle.reachableIdentifier(clName, false);
		break;
	    }
	    case opc_getstatic:
	    case opc_getfield:
	    case opc_invokespecial:
	    case opc_invokestatic:
	    case opc_invokeinterface:
	    case opc_invokevirtual: {
		Reference ref = (Reference) instr.objData;
		m.clazz.bundle.reachableIdentifier
		    (ref.getClazz()+"."+ref.getName()+"."+ref.getType(), 
		     instr.opcode == opc_invokevirtual 
		     || instr.opcode == opc_invokeinterface);
		break;
	    }
	    }
	}

	Handler[] handlers = bytecode.getExceptionHandlers();
	for (int i=0; i< handlers.length; i++) {
	    if (handlers[i].type != null)
		m.clazz.bundle.reachableIdentifier(handlers[i].type, false);
	}
    }

    public BytecodeInfo stripCode() {
	for (Instruction instr = bytecode.getFirstInstr(); 
	     instr != null; instr = instr.nextByAddr) {
	    if (instr.opcode == opc_putstatic
		|| instr.opcode == opc_putfield) {
		Reference ref = (Reference) instr.objData;
		ClassIdentifier ci = (ClassIdentifier)
		    m.clazz.bundle.getIdentifier(ref.getClazz());
		if (ci != null) {
		    FieldIdentifier fi = (FieldIdentifier) 
			ci.getIdentifier(ref.getName(), ref.getType());
		    if (jode.Obfuscator.shouldStrip && !fi.isReachable()) {
			/* Replace instruction with pop opcodes. */
			int stacksize = 
			    (instr.opcode 
				 == Instruction.opc_putstatic) ? 0 : 1;
			stacksize += Type.tType(ref.getType()).stackSize();
			if (stacksize == 3) {
			    /* Add a pop instruction after this opcode. */
			    Instruction second = new Instruction(bytecode);
			    second.addr = instr.addr+1;
			    second.length = 1;
			    second.opcode = Instruction.opc_pop;
			    second.nextByAddr = instr.nextByAddr;
			    instr.nextByAddr = second;
			    second.nextByAddr.preds.removeElement(instr);
			    second.nextByAddr.preds.addElement(second);
			    stacksize--;
			}
			instr.objData = null;
			instr.intData = 0;
			instr.opcode = Instruction.opc_pop - 1 + stacksize;
			instr.length = 1;
		    } 
		}
	    }
	}
	return bytecode;
    }
}
