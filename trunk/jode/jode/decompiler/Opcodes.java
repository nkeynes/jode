/* Opcodes Copyright (C) 1999 Jochen Hoenicke.
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

package jode.decompiler;
import jode.Type;
import jode.MethodType;
import jode.expr.*;
import jode.flow.*;
import jode.bytecode.*;
import java.io.*;
import java.util.Vector;

/**
 * This is an abstract class which creates flow blocks for the
 * opcodes in a byte stream.
 */
public abstract class Opcodes implements jode.bytecode.Opcodes {

    private final static Type types[][] = {
	// Local types
        { Type.tBoolUInt, Type.tLong, Type.tFloat, Type.tDouble, 
	  Type.tUObject },
	// Array types
        { Type.tInt, Type.tLong, Type.tFloat, Type.tDouble, Type.tUObject, 
          Type.tBoolByte, Type.tChar, Type.tShort },
	// i2bcs types
        { Type.tByte, Type.tChar, Type.tShort },
	// add/sub/mul/div types
        { Type.tInt, Type.tLong, Type.tFloat, Type.tDouble, Type.tUObject },
	// and/or/xor types
        { Type.tBoolInt, Type.tLong, Type.tFloat, Type.tDouble, Type.tUObject }
    };
    
    private static StructuredBlock createNormal(CodeAnalyzer ca, 
						Instruction instr,
						Expression expr)
    {
        return new InstructionBlock(expr, new Jump(FlowBlock.NEXT_BY_ADDR));
    }

    private static StructuredBlock createSpecial(CodeAnalyzer ca, 
						 Instruction instr,
						 int type, 
						 int stackcount, int param)
    {
        return new SpecialBlock(type, stackcount, param, 
				new Jump(FlowBlock.NEXT_BY_ADDR));
    }

    private static StructuredBlock createGoto(CodeAnalyzer ca,
                                              Instruction instr)
    {
        return new EmptyBlock(new Jump((FlowBlock)instr.succs[0].tmpInfo));
    }

    private static StructuredBlock createJsr(CodeAnalyzer ca, 
					     Instruction instr)
    {
        return new JsrBlock(new Jump((FlowBlock)instr.succs[0].tmpInfo),
			    new Jump(FlowBlock.NEXT_BY_ADDR));
    }

    private static StructuredBlock createIfGoto(CodeAnalyzer ca, 
						Instruction instr,
						Expression expr)
    {
        return new ConditionalBlock
	    (expr, new Jump((FlowBlock)instr.succs[0].tmpInfo), 
	     new Jump(FlowBlock.NEXT_BY_ADDR));
    }

    private static StructuredBlock createSwitch(CodeAnalyzer ca,
						Instruction instr,
                                                int[] cases, FlowBlock[] dests)
    {
        return new SwitchBlock(new NopOperator(Type.tUInt), cases, dests);
    }

    private static StructuredBlock createBlock(CodeAnalyzer ca,
                                               Instruction instr,
                                               StructuredBlock block)
    {
        return block;
    }

    private static StructuredBlock createRet(CodeAnalyzer ca,
					     Instruction instr,
					     LocalInfo local)
    {
	return new RetBlock(local);
    }

    /**
     * Read an opcode out of a data input stream containing the bytecode.
     * @param addr    The current address.
     * @param stream  The stream containing the java byte code.
     * @param ca      The Code Analyzer 
     *                (where further information can be get from).
     * @return The FlowBlock representing this opcode
     *         or null if the stream is empty.
     * @exception IOException  if an read error occured.
     * @exception ClassFormatError  if an invalid opcode is detected.
     */
    public static StructuredBlock readOpcode(Instruction instr, 
					     CodeAnalyzer ca) 
        throws ClassFormatError
    {
        int opcode = instr.opcode;
        switch (opcode) {
        case opc_nop:
            return createBlock(ca, instr, new EmptyBlock
			       (new Jump(FlowBlock.NEXT_BY_ADDR)));
        case opc_ldc:
        case opc_ldc2_w:
            return createNormal (ca, instr, new ConstOperator(instr.objData));

        case opc_iload: case opc_lload: 
        case opc_fload: case opc_dload: case opc_aload:
            return createNormal
                (ca, instr, new LocalLoadOperator
                 (types[0][opcode-opc_iload],
                  ca.getLocalInfo(instr.addr, instr.localSlot)));
        case opc_iaload: case opc_laload: 
        case opc_faload: case opc_daload: case opc_aaload:
        case opc_baload: case opc_caload: case opc_saload:
            return createNormal
                (ca, instr, new ArrayLoadOperator
                 (types[1][opcode - opc_iaload]));
        case opc_istore: case opc_lstore: 
        case opc_fstore: case opc_dstore: case opc_astore:
            return createNormal
                (ca, instr, new LocalStoreOperator
                 (types[0][opcode-opc_istore], 
                  ca.getLocalInfo(instr.addr+2, instr.localSlot),
                  Operator.ASSIGN_OP));
        case opc_iastore: case opc_lastore:
        case opc_fastore: case opc_dastore: case opc_aastore:
        case opc_bastore: case opc_castore: case opc_sastore:
            return createNormal
                (ca, instr, new ArrayStoreOperator
                 (types[1][opcode - opc_iastore]));
        case opc_pop: case opc_pop2:
            return createSpecial
                (ca, instr, SpecialBlock.POP, opcode - opc_pop + 1, 0);
        case opc_dup: case opc_dup_x1: case opc_dup_x2:
        case opc_dup2: case opc_dup2_x1: case opc_dup2_x2:
            return createSpecial
                (ca, instr, SpecialBlock.DUP, 
                 (opcode - opc_dup)/3+1, (opcode - opc_dup)%3);
        case opc_swap:
            return createSpecial(ca, instr, SpecialBlock.SWAP, 1, 0);
        case opc_iadd: case opc_ladd: case opc_fadd: case opc_dadd:
        case opc_isub: case opc_lsub: case opc_fsub: case opc_dsub:
        case opc_imul: case opc_lmul: case opc_fmul: case opc_dmul:
        case opc_idiv: case opc_ldiv: case opc_fdiv: case opc_ddiv:
        case opc_irem: case opc_lrem: case opc_frem: case opc_drem:
            return createNormal
                (ca, instr, new BinaryOperator
                 (types[3][(opcode - opc_iadd)%4],
                  (opcode - opc_iadd)/4+Operator.ADD_OP));
        case opc_ineg: case opc_lneg: case opc_fneg: case opc_dneg:
            return createNormal
                (ca, instr, new UnaryOperator
                 (types[3][opcode - opc_ineg], Operator.NEG_OP));
        case opc_ishl: case opc_lshl:
        case opc_ishr: case opc_lshr:
        case opc_iushr: case opc_lushr:
            return createNormal
                (ca, instr, new ShiftOperator
                 (types[3][(opcode - opc_ishl)%2],
                  (opcode - opc_ishl)/2 + Operator.SHIFT_OP));
        case opc_iand: case opc_land:
        case opc_ior : case opc_lor :
        case opc_ixor: case opc_lxor:
            return createNormal
                (ca, instr, new BinaryOperator
                 (types[4][(opcode - opc_iand)%2],
                  (opcode - opc_iand)/2 + Operator.AND_OP));
        case opc_iinc: {
            int value = instr.intData;
            int operation = Operator.ADD_OP;
            if (value < 0) {
                value = -value;
                operation = Operator.NEG_OP;
            }
            LocalInfo li = ca.getLocalInfo(instr.addr, instr.localSlot);
            li.setType(Type.tUInt);
            return createNormal
                (ca, instr, new IIncOperator
                 (li, Integer.toString(value), 
		  operation + Operator.OPASSIGN_OP));
        }
        case opc_i2l: case opc_i2f: case opc_i2d:
        case opc_l2i: case opc_l2f: case opc_l2d:
        case opc_f2i: case opc_f2l: case opc_f2d:
        case opc_d2i: case opc_d2l: case opc_d2f: {
            int from = (opcode-opc_i2l)/3;
            int to   = (opcode-opc_i2l)%3;
            if (to >= from)
                to++;
            return createNormal
                (ca, instr, new ConvertOperator(types[3][from], 
                                                  types[3][to]));
        }
        case opc_i2b: case opc_i2c: case opc_i2s:
            return createNormal
                (ca, instr, new ConvertOperator
                 (types[3][0], types[2][opcode-opc_i2b]));
        case opc_lcmp:
        case opc_fcmpl: case opc_fcmpg:
        case opc_dcmpl: case opc_dcmpg:
            return createNormal
                (ca, instr, new CompareToIntOperator
                 (types[3][(opcode-(opc_lcmp-3))/2], 
                  (opcode-(opc_lcmp-3))%2));
        case opc_ifeq: case opc_ifne: 
            return createIfGoto
		(ca, instr,
                 new CompareUnaryOperator
                 (Type.tBoolUInt, opcode - (opc_ifeq-Operator.COMPARE_OP)));
        case opc_iflt: case opc_ifge: case opc_ifgt: case opc_ifle:
            return createIfGoto
		(ca, instr,
                 new CompareUnaryOperator
                 (Type.tUInt, opcode - (opc_ifeq-Operator.COMPARE_OP)));
        case opc_if_icmpeq: case opc_if_icmpne:
            return createIfGoto
		(ca, instr,
                 new CompareBinaryOperator
                 (Type.tBoolInt, 
                  opcode - (opc_if_icmpeq-Operator.COMPARE_OP)));
        case opc_if_icmplt: case opc_if_icmpge: 
        case opc_if_icmpgt: case opc_if_icmple:
            return createIfGoto
		(ca, instr,
                 new CompareBinaryOperator
                 (Type.tUInt, 
                  opcode - (opc_if_icmpeq-Operator.COMPARE_OP)));
        case opc_if_acmpeq: case opc_if_acmpne:
            return createIfGoto
		(ca, instr,
                 new CompareBinaryOperator
                 (Type.tUObject, 
                  opcode - (opc_if_acmpeq-Operator.COMPARE_OP)));
        case opc_goto:
            return createGoto(ca, instr);
        case opc_jsr:
            return createJsr(ca, instr);
        case opc_ret:
            return createRet
                (ca, instr, ca.getLocalInfo(instr.addr, instr.localSlot));
        case opc_tableswitch: {
            int low  = instr.intData;
            int[] cases = new int[instr.succs.length-1];
            FlowBlock[] dests = new FlowBlock[instr.succs.length];
            for (int i=0; i < cases.length; i++) {
                cases[i] = i+low;
                dests[i] = (FlowBlock) instr.succs[i].tmpInfo;
            }
            dests[cases.length] = (FlowBlock)instr.succs[cases.length].tmpInfo;
            return createSwitch(ca, instr, cases, dests);
	}
        case opc_lookupswitch: {
	    int[] cases = (int[]) instr.objData;
            FlowBlock[] dests = new FlowBlock[instr.succs.length];
            for (int i=0; i < dests.length; i++)
                dests[i] = (FlowBlock) instr.succs[i].tmpInfo;
            dests[cases.length] = (FlowBlock)instr.succs[cases.length].tmpInfo;
            return createSwitch(ca, instr, cases, dests);
        }
        case opc_ireturn: case opc_lreturn: 
        case opc_freturn: case opc_dreturn: case opc_areturn: {
            Type retType = Type.tSubType(ca.getMethod().getReturnType());
            return createBlock
                (ca, instr, new ReturnBlock(new NopOperator(retType)));
        }
        case opc_return:
            return createBlock
                (ca, instr, new EmptyBlock(new Jump(FlowBlock.END_OF_METHOD)));
        case opc_getstatic:
        case opc_getfield: {
            Reference ref = (Reference) instr.objData;
            return createNormal
                (ca, instr, new GetFieldOperator
                 (ca, opcode == opc_getstatic, ref));
        }
        case opc_putstatic:
        case opc_putfield: {
            Reference ref = (Reference) instr.objData;
            return createNormal
                (ca, instr, new PutFieldOperator
                 (ca, opcode == opc_putstatic, ref));
        }
        case opc_invokevirtual:
        case opc_invokespecial:
        case opc_invokestatic :
        case opc_invokeinterface: {
            Reference ref = (Reference) instr.objData;
            StructuredBlock block = createNormal
                (ca, instr, new InvokeOperator
                 (ca, opcode == opc_invokestatic, 
		  opcode == opc_invokespecial, ref));
            return block;
        }
        case opc_new: {
            Type type = Type.tClassOrArray((String) instr.objData);
            type.useType();
            return createNormal(ca, instr, new NewOperator(type));
        }
        case opc_newarray: {
            Type type;
            switch (instr.intData) {
            case  4: type = Type.tBoolean; break;
            case  5: type = Type.tChar   ; break;
            case  6: type = Type.tFloat  ; break;
            case  7: type = Type.tDouble ; break;
            case  8: type = Type.tByte   ; break;
            case  9: type = Type.tShort  ; break;
            case 10: type = Type.tInt    ; break;
            case 11: type = Type.tLong   ; break;
            default:
                throw new ClassFormatError("Invalid newarray operand");
            }
            type.useType();
            return createNormal
                (ca, instr, new NewArrayOperator(Type.tArray(type), 1));
        }
        case opc_anewarray: {
            Type type = Type.tClassOrArray((String) instr.objData);
            type.useType();
            return createNormal
                (ca, instr, new NewArrayOperator(Type.tArray(type), 1));
        }
        case opc_arraylength:
            return createNormal
                (ca, instr, new ArrayLengthOperator());
        case opc_athrow:
            return createBlock
                (ca, instr, 
                 new ThrowBlock(new NopOperator(Type.tUObject)));
        case opc_checkcast: {
            Type type = Type.tClassOrArray((String) instr.objData);
            type.useType();
            return createNormal
                (ca, instr, new CheckCastOperator(type));
        }
        case opc_instanceof: {
            Type type = Type.tClassOrArray((String) instr.objData);
            type.useType();
            return createNormal
                (ca, instr, new InstanceOfOperator(type));
        }
        case opc_monitorenter:
            return createNormal(ca, instr,
                                new MonitorEnterOperator());
        case opc_monitorexit:
            return createNormal(ca, instr,
                                new MonitorExitOperator());
        case opc_multianewarray: {
            Type type = Type.tClassOrArray((String) instr.objData);
	    type.useType();
            int dimension = instr.intData;
            return createNormal(ca, instr, 
				new NewArrayOperator(type, dimension));
        }
        case opc_ifnull: case opc_ifnonnull:
            return createIfGoto
                (ca, instr, new CompareUnaryOperator
                 (Type.tUObject, opcode - (opc_ifnull-Operator.COMPARE_OP)));
        default:
            throw new jode.AssertError("Invalid opcode "+opcode);
        }
    }
}