/* 
 * Opcodes (c) 1998 Jochen Hoenicke
 *
 * You may distribute under the terms of the GNU General Public License.
 *
 * IN NO EVENT SHALL JOCHEN HOENICKE BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JOCHEN HOENICKE 
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JOCHEN HOENICKE SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
 * BASIS, AND JOCHEN HOENICKE HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * $Id$
 */

package jode;
import jode.flow.*;
import java.io.*;
import gnu.bytecode.CpoolRef;
import gnu.bytecode.CpoolClass;

/**
 * This is an abstract class which creates flow blocks for the
 * opcodes in a byte stream.
 */
public abstract class Opcodes {

    private final static Type  ALL_INT_TYPE = Type.tUInt;
    private final static Type BOOL_INT_TYPE = Type.tBoolInt;
    private final static Type      INT_TYPE = Type.tInt;
    private final static Type     LONG_TYPE = Type.tLong;
    private final static Type    FLOAT_TYPE = Type.tFloat;
    private final static Type   DOUBLE_TYPE = Type.tDouble;
    private final static Type   OBJECT_TYPE = Type.tUObject;
    private final static Type  BOOLEAN_TYPE = Type.tBoolean;
    private final static Type BYTEBOOL_TYPE = Type.tBoolByte;
    private final static Type     BYTE_TYPE = Type.tByte;
    private final static Type     CHAR_TYPE = Type.tChar;
    private final static Type    SHORT_TYPE = Type.tShort;
    private final static Type     VOID_TYPE = Type.tVoid;

    private final static Type types[][] = {
        {BOOL_INT_TYPE, LONG_TYPE, FLOAT_TYPE, DOUBLE_TYPE, OBJECT_TYPE },
        {     INT_TYPE, LONG_TYPE, FLOAT_TYPE, DOUBLE_TYPE, OBJECT_TYPE, 
         BYTEBOOL_TYPE, CHAR_TYPE, SHORT_TYPE },
        {    BYTE_TYPE, CHAR_TYPE, SHORT_TYPE },
        { ALL_INT_TYPE, LONG_TYPE, FLOAT_TYPE, DOUBLE_TYPE, OBJECT_TYPE }
    };

    private final static int opc_nop = 0;
    private final static int opc_aconst_null = 1;
    private final static int opc_iconst_m1 = 2;
    private final static int opc_iconst_0 = 3;
    private final static int opc_iconst_1 = 4;
    private final static int opc_iconst_2 = 5;
    private final static int opc_iconst_3 = 6;
    private final static int opc_iconst_4 = 7;
    private final static int opc_iconst_5 = 8;
    private final static int opc_lconst_0 = 9;
    private final static int opc_lconst_1 = 10;
    private final static int opc_fconst_0 = 11;
    private final static int opc_fconst_1 = 12;
    private final static int opc_fconst_2 = 13;
    private final static int opc_dconst_0 = 14;
    private final static int opc_dconst_1 = 15;
    private final static int opc_bipush = 16;
    private final static int opc_sipush = 17;
    private final static int opc_ldc = 18;
    private final static int opc_ldc_w = 19;
    private final static int opc_ldc2_w = 20;
    private final static int opc_iload = 21;
    private final static int opc_lload = 22;
    private final static int opc_fload = 23;
    private final static int opc_dload = 24;
    private final static int opc_aload = 25;
    private final static int opc_iload_0 = 26;
    private final static int opc_iload_1 = 27;
    private final static int opc_iload_2 = 28;
    private final static int opc_iload_3 = 29;
    private final static int opc_lload_0 = 30;
    private final static int opc_lload_1 = 31;
    private final static int opc_lload_2 = 32;
    private final static int opc_lload_3 = 33;
    private final static int opc_fload_0 = 34;
    private final static int opc_fload_1 = 35;
    private final static int opc_fload_2 = 36;
    private final static int opc_fload_3 = 37;
    private final static int opc_dload_0 = 38;
    private final static int opc_dload_1 = 39;
    private final static int opc_dload_2 = 40;
    private final static int opc_dload_3 = 41;
    private final static int opc_aload_0 = 42;
    private final static int opc_aload_1 = 43;
    private final static int opc_aload_2 = 44;
    private final static int opc_aload_3 = 45;
    private final static int opc_iaload = 46;
    private final static int opc_laload = 47;
    private final static int opc_faload = 48;
    private final static int opc_daload = 49;
    private final static int opc_aaload = 50;
    private final static int opc_baload = 51;
    private final static int opc_caload = 52;
    private final static int opc_saload = 53;
    private final static int opc_istore = 54;
    private final static int opc_lstore = 55;
    private final static int opc_fstore = 56;
    private final static int opc_dstore = 57;
    private final static int opc_astore = 58;
    private final static int opc_istore_0 = 59;
    private final static int opc_istore_1 = 60;
    private final static int opc_istore_2 = 61;
    private final static int opc_istore_3 = 62;
    private final static int opc_lstore_0 = 63;
    private final static int opc_lstore_1 = 64;
    private final static int opc_lstore_2 = 65;
    private final static int opc_lstore_3 = 66;
    private final static int opc_fstore_0 = 67;
    private final static int opc_fstore_1 = 68;
    private final static int opc_fstore_2 = 69;
    private final static int opc_fstore_3 = 70;
    private final static int opc_dstore_0 = 71;
    private final static int opc_dstore_1 = 72;
    private final static int opc_dstore_2 = 73;
    private final static int opc_dstore_3 = 74;
    private final static int opc_astore_0 = 75;
    private final static int opc_astore_1 = 76;
    private final static int opc_astore_2 = 77;
    private final static int opc_astore_3 = 78;
    private final static int opc_iastore = 79;
    private final static int opc_lastore = 80;
    private final static int opc_fastore = 81;
    private final static int opc_dastore = 82;
    private final static int opc_aastore = 83;
    private final static int opc_bastore = 84;
    private final static int opc_castore = 85;
    private final static int opc_sastore = 86;
    private final static int opc_pop = 87;
    private final static int opc_pop2 = 88;
    private final static int opc_dup = 89;
    private final static int opc_dup_x1 = 90;
    private final static int opc_dup_x2 = 91;
    private final static int opc_dup2 = 92;
    private final static int opc_dup2_x1 = 93;
    private final static int opc_dup2_x2 = 94;
    private final static int opc_swap = 95;
    private final static int opc_iadd = 96;
    private final static int opc_ladd = 97;
    private final static int opc_fadd = 98;
    private final static int opc_dadd = 99;
    private final static int opc_isub = 100;
    private final static int opc_lsub = 101;
    private final static int opc_fsub = 102;
    private final static int opc_dsub = 103;
    private final static int opc_imul = 104;
    private final static int opc_lmul = 105;
    private final static int opc_fmul = 106;
    private final static int opc_dmul = 107;
    private final static int opc_idiv = 108;
    private final static int opc_ldiv = 109;
    private final static int opc_fdiv = 110;
    private final static int opc_ddiv = 111;
    private final static int opc_irem = 112;
    private final static int opc_lrem = 113;
    private final static int opc_frem = 114;
    private final static int opc_drem = 115;
    private final static int opc_ineg = 116;
    private final static int opc_lneg = 117;
    private final static int opc_fneg = 118;
    private final static int opc_dneg = 119;
    private final static int opc_ishl = 120;
    private final static int opc_lshl = 121;
    private final static int opc_ishr = 122;
    private final static int opc_lshr = 123;
    private final static int opc_iushr = 124;
    private final static int opc_lushr = 125;
    private final static int opc_iand = 126;
    private final static int opc_land = 127;
    private final static int opc_ior = 128;
    private final static int opc_lor = 129;
    private final static int opc_ixor = 130;
    private final static int opc_lxor = 131;
    private final static int opc_iinc = 132;
    private final static int opc_i2l = 133;
    private final static int opc_i2f = 134;
    private final static int opc_i2d = 135;
    private final static int opc_l2i = 136;
    private final static int opc_l2f = 137;
    private final static int opc_l2d = 138;
    private final static int opc_f2i = 139;
    private final static int opc_f2l = 140;
    private final static int opc_f2d = 141;
    private final static int opc_d2i = 142;
    private final static int opc_d2l = 143;
    private final static int opc_d2f = 144;
    private final static int opc_i2b = 145;
    private final static int opc_i2c = 146;
    private final static int opc_i2s = 147;
    private final static int opc_lcmp = 148;
    private final static int opc_fcmpl = 149;
    private final static int opc_fcmpg = 150;
    private final static int opc_dcmpl = 151;
    private final static int opc_dcmpg = 152;
    private final static int opc_ifeq = 153;
    private final static int opc_ifne = 154;
    private final static int opc_iflt = 155;
    private final static int opc_ifge = 156;
    private final static int opc_ifgt = 157;
    private final static int opc_ifle = 158;
    private final static int opc_if_icmpeq = 159;
    private final static int opc_if_icmpne = 160;
    private final static int opc_if_icmplt = 161;
    private final static int opc_if_icmpge = 162;
    private final static int opc_if_icmpgt = 163;
    private final static int opc_if_icmple = 164;
    private final static int opc_if_acmpeq = 165;
    private final static int opc_if_acmpne = 166;
    private final static int opc_goto = 167;
    private final static int opc_jsr = 168;
    private final static int opc_ret = 169;
    private final static int opc_tableswitch = 170;
    private final static int opc_lookupswitch = 171;
    private final static int opc_ireturn = 172;
    private final static int opc_lreturn = 173;
    private final static int opc_freturn = 174;
    private final static int opc_dreturn = 175;
    private final static int opc_areturn = 176;
    private final static int opc_return = 177;
    private final static int opc_getstatic = 178;
    private final static int opc_putstatic = 179;
    private final static int opc_getfield = 180;
    private final static int opc_putfield = 181;
    private final static int opc_invokevirtual = 182;
    private final static int opc_invokespecial = 183;
    private final static int opc_invokestatic = 184;
    private final static int opc_invokeinterface = 185;
    private final static int opc_xxxunusedxxx = 186;
    private final static int opc_new = 187;
    private final static int opc_newarray = 188;
    private final static int opc_anewarray = 189;
    private final static int opc_arraylength = 190;
    private final static int opc_athrow = 191;
    private final static int opc_checkcast = 192;
    private final static int opc_instanceof = 193;
    private final static int opc_monitorenter = 194;
    private final static int opc_monitorexit = 195;
    private final static int opc_wide = 196;
    private final static int opc_multianewarray = 197;
    private final static int opc_ifnull = 198;
    private final static int opc_ifnonnull = 199;
    private final static int opc_goto_w = 200;
    private final static int opc_jsr_w = 201;
    private final static int opc_breakpoint = 202;
    
    private static StructuredBlock createNormal(CodeAnalyzer ca, 
                                               int addr, int length, 
                                               Expression instr)
    {
        return new InstructionBlock(instr, new Jump(addr+length));
    }

    private static StructuredBlock createSpecial(CodeAnalyzer ca, 
                                          int addr, int length, 
                                          int type, int stackcount, int param)
    {
        return new SpecialBlock(type, stackcount, param,
                         new Jump(addr+length));
    }

    private static StructuredBlock createGoto(CodeAnalyzer ca,
                                              int addr, int length, int destAddr)
    {
        return new EmptyBlock(new Jump(destAddr));
    }

    private static StructuredBlock createJsr(CodeAnalyzer ca,
                                             int addr, int length, 
                                             int destAddr)
    {
        return new JsrBlock(new Jump(addr+length),
                            new Jump(destAddr));
    }

    private static StructuredBlock createIfGoto(CodeAnalyzer ca, 
                                                int addr, int length, 
                                                int destAddr, Expression instr)
    {
        return new ConditionalBlock(instr, 
                                    new Jump(destAddr),
                                    new Jump(addr+length));
    }

    private static StructuredBlock createSwitch(CodeAnalyzer ca,
                                                int addr, int length, 
                                                int[] cases, int[] dests)
    {
        return new SwitchBlock(new NopOperator(Type.tUInt), cases, dests);
    }

    private static StructuredBlock createBlock(CodeAnalyzer ca,
                                               int addr, int length, 
                                               StructuredBlock block)
    {
        return block;
    }

    private static StructuredBlock createRet(CodeAnalyzer ca,
                                             int addr, int length, 
                                             LocalInfo local)
    {
	return new RetBlock(local);
    }


    /**
     * Read an opcode out of a data input stream and determine its size
     * and its successors.
     * @param addr    The current address.
     * @param stream  The stream containing the java byte code.
     * @return An array of ints; the first entry is the size of the
     *         instruction, the remaining are the successors.
     */
    public static int[] getSizeAndSuccs(int addr, DataInputStream stream) 
        throws IOException
    {
        int opcode = stream.readUnsignedByte();
        switch (opcode) {
        case opc_multianewarray:
            stream.skip(3);
            return new int[] { 4, addr+4 };
        case opc_invokeinterface:
            stream.skip(4);
            return new int[] { 5, addr+5 };

        case opc_wide: {
            switch (opcode = stream.readUnsignedByte()) {
            case opc_iload: case opc_lload: 
            case opc_fload: case opc_dload: case opc_aload:
            case opc_istore: case opc_lstore: 
            case opc_fstore: case opc_dstore: case opc_astore:
                stream.skip(2);
                return new int[] { 4, addr+4 };

            case opc_iinc:
                stream.skip(4);
                return new int[] { 6, addr+6 };
            case opc_ret:
                stream.skip(2);
                return new int[] { 4 };
            default:
                throw new ClassFormatError("Invalid wide opcode "+opcode);
            }
        }
        case opc_ret:
            return new int[] { 2 };

        case opc_jsr_w:
            return new int[] { 5, addr + 5, addr + stream.readInt() };

        case opc_goto:
            return new int[] { 3, addr + stream.readShort() };
        case opc_goto_w:
            return new int[] { 5, addr + stream.readInt() };

        case opc_tableswitch: {
            int length = 3-(addr % 4);
            stream.skip(length);
            int def  = addr + stream.readInt();
            int low  = stream.readInt();
            int high = stream.readInt();
            int[] dests = new int[high-low+3];
            for (int i=0; i+low <= high; i++) {
                dests[i+1] = addr + stream.readInt();
            }
            dests[dests.length-1] = def;
            dests[0] = length + 13 + 4 * (high-low+1);
            return dests;
        }
        case opc_lookupswitch: {
            int length = 3-(addr % 4);
            stream.skip(length);
            int def = stream.readInt();
            int npairs = stream.readInt();
            int[] dests = new int[npairs+2];
            for (int i=0; i < npairs; i++) {
                int value = stream.readInt();
                dests[i+1] = addr + stream.readInt();
            }
            dests[npairs+1] = def;
            dests[0] = length + 9 + 8 * npairs;
            return dests;
        }

        case opc_ifnull: case opc_ifnonnull:
        case opc_jsr:
            return new int[] { 3, addr + 3, addr + stream.readShort() };

        case opc_sipush:
        case opc_ldc_w:
        case opc_ldc2_w:
        case opc_iinc:
        case opc_getstatic:
        case opc_getfield:
        case opc_putstatic:
        case opc_putfield:
        case opc_invokevirtual:
        case opc_invokespecial:
        case opc_invokestatic :
        case opc_new:
        case opc_anewarray:
        case opc_checkcast:
        case opc_instanceof:
            stream.skip(2);
            return new int[] { 3, addr+3 };
        }

        if (opcode == opc_newarray
            || (opcode >= opc_bipush && opcode <= opc_aload)
            || (opcode >= opc_istore && opcode <= opc_astore)) {
            stream.skip(1);
            return new int[] { 2, addr+2 };
        }

        if (opcode >= opc_ireturn && opcode <= opc_return)
            return new int[] { 1 };
        if (opcode >= opc_ifeq && opcode <= opc_if_acmpne)
            return new int[] { 3, addr + 3, addr + stream.readShort() };
        if (opcode == opc_xxxunusedxxx || opcode > opc_breakpoint)
            throw new ClassFormatError("Invalid opcode "+opcode);

        return new int[] { 1, addr+1 };
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
    public static StructuredBlock readOpcode(int addr, DataInputStream stream,
                                             CodeAnalyzer ca) 
        throws IOException, ClassFormatError
    {
        int opcode = stream.readUnsignedByte();
        switch (opcode) {
        case opc_nop:
            return createNormal(ca, addr, 1, new NopOperator());
        case opc_aconst_null:
            return createNormal
                (ca, addr, 1, new ConstOperator(OBJECT_TYPE, "null"));
        case opc_iconst_0: case opc_iconst_1: 
            return createNormal
                (ca, addr, 1, new ConstOperator
                 (Type.tBoolInt, Integer.toString(opcode - opc_iconst_0)));
        case opc_iconst_m1: case opc_iconst_2:
        case opc_iconst_3: case opc_iconst_4: case opc_iconst_5:
            return createNormal
                (ca, addr, 1, new ConstOperator
                 (ALL_INT_TYPE, Integer.toString(opcode - opc_iconst_0)));
        case opc_lconst_0: case opc_lconst_1:
            return createNormal
                (ca, addr, 1, new ConstOperator
                 (LONG_TYPE, 
                  Integer.toString(opcode - opc_lconst_0)));
        case opc_fconst_0: case opc_fconst_1: case opc_fconst_2:
            return createNormal
                (ca, addr, 1, new ConstOperator
                 (FLOAT_TYPE, 
                  Integer.toString(opcode - opc_fconst_0) + ".0F"));
        case opc_dconst_0: case opc_dconst_1:
            return createNormal
                (ca, addr, 1, new ConstOperator
                 (DOUBLE_TYPE, 
                  Integer.toString(opcode - opc_dconst_0) + ".0"));
        case opc_bipush:
            return createNormal
                (ca, addr, 2, new ConstOperator
                 (ALL_INT_TYPE, Integer.toString(stream.readByte())));
        case opc_sipush:
            return createNormal
                (ca, addr, 3, new ConstOperator
                 (Type.tRange(Type.tInt, Type.tChar), 
                  Integer.toString(stream.readShort())));
        case opc_ldc: {
            int index = stream.readUnsignedByte();
            return createNormal
                (ca, addr, 2, new ConstOperator
                 (ca.method.classAnalyzer.getConstantType(index),
                  ca.method.classAnalyzer.getConstantString(index)));
        }
        case opc_ldc_w:
        case opc_ldc2_w: {
            int index = stream.readUnsignedShort();
            return createNormal
                (ca, addr, 3, new ConstOperator
                 (ca.method.classAnalyzer.getConstantType(index),
                  ca.method.classAnalyzer.getConstantString(index)));
        }
        case opc_iload: case opc_lload: 
        case opc_fload: case opc_dload: case opc_aload:
            return createNormal
                (ca, addr, 2, new LocalLoadOperator
                 (types[0][opcode-opc_iload],
                  ca.getLocalInfo(addr, stream.readUnsignedByte())));
        case opc_iload_0: case opc_iload_1: case opc_iload_2: case opc_iload_3:
        case opc_lload_0: case opc_lload_1: case opc_lload_2: case opc_lload_3:
        case opc_fload_0: case opc_fload_1: case opc_fload_2: case opc_fload_3:
        case opc_dload_0: case opc_dload_1: case opc_dload_2: case opc_dload_3:
        case opc_aload_0: case opc_aload_1: case opc_aload_2: case opc_aload_3:
            return createNormal
                (ca, addr, 1, new LocalLoadOperator
                 (types[0][(opcode-opc_iload_0)/4],
                  ca.getLocalInfo(addr, (opcode-opc_iload_0) & 3)));
        case opc_iaload: case opc_laload: 
        case opc_faload: case opc_daload: case opc_aaload:
        case opc_baload: case opc_caload: case opc_saload:
            return createNormal
                (ca, addr, 1, new ArrayLoadOperator
                 (types[1][opcode - opc_iaload]));
        case opc_istore: case opc_lstore: 
        case opc_fstore: case opc_dstore: case opc_astore:
            return createNormal
                (ca, addr, 2, new LocalStoreOperator
                 (types[0][opcode-opc_istore],
                  ca.getLocalInfo(addr+2, stream.readUnsignedByte()),
                  Operator.ASSIGN_OP));
        case opc_istore_0: case opc_istore_1: 
        case opc_istore_2: case opc_istore_3:
        case opc_lstore_0: case opc_lstore_1: 
        case opc_lstore_2: case opc_lstore_3:
        case opc_fstore_0: case opc_fstore_1:
        case opc_fstore_2: case opc_fstore_3:
        case opc_dstore_0: case opc_dstore_1:
        case opc_dstore_2: case opc_dstore_3:
        case opc_astore_0: case opc_astore_1:
        case opc_astore_2: case opc_astore_3:
            return createNormal
                (ca, addr, 1, new LocalStoreOperator
                 (types[0][(opcode-opc_istore_0)/4],
                  ca.getLocalInfo(addr+1, (opcode-opc_istore_0) & 3),
                  Operator.ASSIGN_OP));
        case opc_iastore: case opc_lastore:
        case opc_fastore: case opc_dastore: case opc_aastore:
        case opc_bastore: case opc_castore: case opc_sastore:
            return createNormal
                (ca, addr, 1, new ArrayStoreOperator
                 (types[1][opcode - opc_iastore]));
        case opc_pop: case opc_pop2:
            return createNormal
                (ca, addr, 1, new PopOperator(opcode - opc_pop + 1));
        case opc_dup: case opc_dup_x1: case opc_dup_x2:
        case opc_dup2: case opc_dup2_x1: case opc_dup2_x2:
            return createSpecial
                (ca, addr, 1, SpecialBlock.DUP, 
                 (opcode - opc_dup)/3+1, (opcode - opc_dup)%3);
        case opc_swap:
            return createSpecial(ca, addr, 1, SpecialBlock.SWAP, 1, 0);
        case opc_iadd: case opc_ladd: case opc_fadd: case opc_dadd:
        case opc_isub: case opc_lsub: case opc_fsub: case opc_dsub:
        case opc_imul: case opc_lmul: case opc_fmul: case opc_dmul:
        case opc_idiv: case opc_ldiv: case opc_fdiv: case opc_ddiv:
        case opc_irem: case opc_lrem: case opc_frem: case opc_drem:
            return createNormal
                (ca, addr, 1, new BinaryOperator
                 (types[3][(opcode - opc_iadd)%4],
                  (opcode - opc_iadd)/4+Operator.ADD_OP));
        case opc_ineg: case opc_lneg: case opc_fneg: case opc_dneg:
            return createNormal
                (ca, addr, 1, new UnaryOperator
                 (types[3][opcode - opc_ineg], Operator.NEG_OP));
        case opc_ishl: case opc_lshl:
        case opc_ishr: case opc_lshr:
        case opc_iushr: case opc_lushr:
            return createNormal
                (ca, addr, 1, new ShiftOperator
                 (types[3][(opcode - opc_ishl)%2],
                  (opcode - opc_ishl)/2 + Operator.SHIFT_OP));
        case opc_iand: case opc_land:
        case opc_ior : case opc_lor :
        case opc_ixor: case opc_lxor:
            return createNormal
                (ca, addr, 1, new BinaryOperator
                 (types[0][(opcode - opc_iand)%2],
                  (opcode - opc_iand)/2 + Operator.AND_OP));
        case opc_iinc: {
            int local = stream.readUnsignedByte();
            int value = stream.readByte();
            int operation = Operator.ADD_OP;
            if (value < 0) {
                value = -value;
                operation = Operator.NEG_OP;
            }
            LocalInfo li = ca.getLocalInfo(addr, local);
            li.setType(ALL_INT_TYPE);
            return createNormal
                (ca, addr, 3, new IIncOperator
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
                (ca, addr, 1, new ConvertOperator(types[3][from], 
                                                  types[3][to]));
        }
        case opc_i2b: case opc_i2c: case opc_i2s:
            return createNormal
                (ca, addr, 1, new ConvertOperator
                 (ALL_INT_TYPE, types[2][opcode-opc_i2b]));
        case opc_lcmp:
        case opc_fcmpl: case opc_fcmpg:
        case opc_dcmpl: case opc_dcmpg:
            return createNormal
                (ca, addr, 1, new CompareToIntOperator
                 (types[3][(opcode-(opc_lcmp-3))/2], 
                  (opcode-(opc_lcmp-3))%2));
        case opc_ifeq: case opc_ifne: 
            return createIfGoto
                (ca, addr, 3, addr+stream.readShort(),
                 new CompareUnaryOperator
                 (BOOL_INT_TYPE, opcode - (opc_ifeq-Operator.COMPARE_OP)));
        case opc_iflt: case opc_ifge: case opc_ifgt: case opc_ifle:
            return createIfGoto
                (ca, addr, 3, addr+stream.readShort(),
                 new CompareUnaryOperator
                 (ALL_INT_TYPE, opcode - (opc_ifeq-Operator.COMPARE_OP)));
        case opc_if_icmpeq: case opc_if_icmpne:
            return createIfGoto
                (ca, addr, 3, addr+stream.readShort(),
                 new CompareBinaryOperator
                 (Type.tBoolInt, 
                  opcode - (opc_if_icmpeq-Operator.COMPARE_OP)));
        case opc_if_icmplt: case opc_if_icmpge: 
        case opc_if_icmpgt: case opc_if_icmple:
            return createIfGoto
                (ca, addr, 3, addr+stream.readShort(),
                 new CompareBinaryOperator
                 (ALL_INT_TYPE, 
                  opcode - (opc_if_icmpeq-Operator.COMPARE_OP)));
        case opc_if_acmpeq: case opc_if_acmpne:
            return createIfGoto
                (ca, addr, 3, addr+stream.readShort(),
                 new CompareBinaryOperator
                 (OBJECT_TYPE, 
                  opcode - (opc_if_acmpeq-Operator.COMPARE_OP)));
        case opc_goto:
            return createGoto
                (ca, addr, 3, addr+stream.readShort());
        case opc_jsr:
            return createJsr
                (ca, addr, 3, addr+stream.readShort());
        case opc_ret:
            return createRet
                (ca, addr, 2, 
                 ca.getLocalInfo(addr, stream.readUnsignedByte()));
        case opc_tableswitch: {
            int length = 3-(addr % 4);
            stream.skip(length);
            int def  = addr + stream.readInt();
            int low  = stream.readInt();
            int high = stream.readInt();
            int[] cases = new int[high-low+1];
            int[] dests = new int[high-low+2];
            for (int i=0; i+low <= high; i++) {
                cases[i] = i+low;
                dests[i] = addr + stream.readInt();
            }
            dests[cases.length] = def;
            length += 13 + 4 * cases.length;
            return createSwitch
                (ca, addr, length, cases, dests);
            }
        case opc_lookupswitch: {
            int length = 3-(addr % 4);
            stream.skip(length);
            int def    = addr + stream.readInt();
            int npairs = stream.readInt();
            int[] cases = new int[npairs];
            int[] dests = new int[npairs+1];
            for (int i=0; i < npairs; i++) {
                cases[i] = stream.readInt();
                dests[i] = addr + stream.readInt();
            }
            dests[npairs] = def;
            length += 9 + 8 * npairs;
            return createSwitch
                (ca, addr, length, cases, dests);
        }
        case opc_ireturn: case opc_lreturn: 
        case opc_freturn: case opc_dreturn: case opc_areturn: {
            /* Address -1 is interpreted as end of method */
            Type retType = Type.tSubType(ca.getMethod().getReturnType());
            return createBlock
                (ca, addr, 1, new ReturnBlock(new NopOperator(retType),
                                              new Jump(-1)));
        }
        case opc_return:
            return createBlock
                (ca, addr, 1, new EmptyBlock(new Jump(-1)));
        case opc_getstatic:
        case opc_getfield: {
            CpoolRef field = (CpoolRef)ca.method.classAnalyzer
                .getConstant(stream.readUnsignedShort());
            return createNormal
                (ca, addr, 3, new GetFieldOperator
                 (ca, opcode == opc_getstatic,
                  Type.tClass(field.getCpoolClass().getName().getString()),
                  Type.tType(field.getNameAndType().getType().getString()),
                  field.getNameAndType().getName().getString()));
        }
        case opc_putstatic:
        case opc_putfield: {
            CpoolRef field = (CpoolRef)ca.method.classAnalyzer
                .getConstant(stream.readUnsignedShort());
            return createNormal
                (ca, addr, 3, new PutFieldOperator
                 (ca, opcode == opc_putstatic,
                  Type.tClass(field.getCpoolClass().getName().getString()),
                  Type.tType(field.getNameAndType().getType().getString()),
                  field.getNameAndType().getName().getString()));
        }
        case opc_invokevirtual:
        case opc_invokespecial:
        case opc_invokestatic : {
            CpoolRef field = (CpoolRef)ca.method.classAnalyzer
                .getConstant(stream.readUnsignedShort());
            return createNormal
                (ca, addr, 3, new InvokeOperator
                 (ca, opcode == opc_invokespecial, 
                  Type.tClass(field.getCpoolClass()
                              .getName().getString()),
                  new MethodType(opcode == opc_invokestatic, 
                                 field.getNameAndType()
                                 .getType().getString()),
                  field.getNameAndType().getName().getString()));
        }
        case opc_invokeinterface: {
            CpoolRef field = (CpoolRef)ca.method.classAnalyzer.getConstant
                (stream.readUnsignedShort());

            StructuredBlock block = createNormal
                (ca, addr, 5, new InvokeOperator
                 (ca, false,
                  Type.tClass(field.getCpoolClass()
                              .getName().getString()),
                  new MethodType(false, field.getNameAndType()
                                 .getType().getString()),
                  field.getNameAndType().getName().getString()));
            int reserved = stream.readUnsignedShort();
            return block;
        }
        case opc_new: {
            CpoolClass cpcls = (CpoolClass) 
                ca.method.classAnalyzer.getConstant(stream.readUnsignedShort());
            Type type = Type.tClassOrArray(cpcls.getName().getString());
            type.useType();
            return createNormal
                (ca, addr, 3, new NewOperator(type));
        }
        case opc_newarray: {
            Type type;
            switch (stream.readUnsignedByte()) {
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
                (ca, addr, 2, new NewArrayOperator(Type.tArray(type), 1));
        }
        case opc_anewarray: {
            CpoolClass cpcls = (CpoolClass) 
                ca.method.classAnalyzer.getConstant
                (stream.readUnsignedShort());
            Type type = Type.tClassOrArray(cpcls.getName().getString());
            type.useType();
            return createNormal
                (ca, addr, 3, new NewArrayOperator(Type.tArray(type), 1));
        }
        case opc_arraylength:
            return createNormal
                (ca, addr, 1, new ArrayLengthOperator());
        case opc_athrow:
            return createBlock
                (ca, addr, 1, 
                 new ThrowBlock(new NopOperator(Type.tUObject),
                                new Jump(-1)));
        case opc_checkcast: {
            CpoolClass cpcls = (CpoolClass) 
                ca.method.classAnalyzer.getConstant
                (stream.readUnsignedShort());
            Type type = Type.tClassOrArray(cpcls.getName().getString());
            type.useType();
            return createNormal
                (ca, addr, 3, new CheckCastOperator(type));
        }
        case opc_instanceof: {
            CpoolClass cpcls = (CpoolClass) 
                ca.method.classAnalyzer.getConstant
                (stream.readUnsignedShort());
            Type type = Type.tClassOrArray(cpcls.getName().getString());
            type.useType();
            return createNormal
                (ca, addr, 3, new InstanceOfOperator(type));
        }
        case opc_monitorenter:
            return createNormal(ca, addr, 1,
                                new MonitorEnterOperator());
        case opc_monitorexit:
            return createNormal(ca, addr, 1,
                                new MonitorExitOperator());
        case opc_wide: {
            switch (opcode=stream.readUnsignedByte()) {
            case opc_iload: case opc_lload: 
            case opc_fload: case opc_dload: case opc_aload:
                return createNormal
                    (ca, addr, 4,
                     new LocalLoadOperator
                     (types[0][opcode-opc_iload],
                      ca.getLocalInfo(addr, stream.readUnsignedShort())));
            case opc_istore: case opc_lstore: 
            case opc_fstore: case opc_dstore: case opc_astore:
                return createNormal
		    (ca, addr, 4,
		     new LocalStoreOperator
                     (types[0][opcode-opc_istore],
                      ca.getLocalInfo(addr+4, stream.readUnsignedShort()),
                      Operator.ASSIGN_OP));
            case opc_iinc: {
                int local = stream.readUnsignedShort();
                int value = stream.readShort();
                int operation = Operator.ADD_OP;
                if (value < 0) {
                    value = -value;
                    operation = Operator.NEG_OP;
                }
                LocalInfo li = ca.getLocalInfo(addr, local);
                li.setType(ALL_INT_TYPE);
                return createNormal
                    (ca, addr, 6, new IIncOperator
                     (li, Integer.toString(value),
                      operation + Operator.OPASSIGN_OP));
            }
            case opc_ret:
                return createRet
		    (ca, addr, 4, 
		     ca.getLocalInfo(addr, stream.readUnsignedShort()));
            default:
                throw new ClassFormatError("Invalid wide opcode "+opcode);
            }
        }
        case opc_multianewarray: {
            CpoolClass cpcls = (CpoolClass) 
                ca.method.classAnalyzer.getConstant
                (stream.readUnsignedShort());
            Type type = Type.tClassOrArray(cpcls.getName().getString());
            int dimension = stream.readUnsignedByte();
            return createNormal
                (ca, addr, 4,
                 new NewArrayOperator(type, dimension));
        }
        case opc_ifnull: case opc_ifnonnull:
            return createIfGoto
                (ca, addr, 3, addr+stream.readShort(),
                 new CompareUnaryOperator
                 (OBJECT_TYPE, opcode - (opc_ifnull-Operator.COMPARE_OP)));
        case opc_goto_w:
            return createGoto
                (ca, addr, 5, addr + stream.readInt());
        case opc_jsr_w:
            return createJsr
                (ca, addr, 5, addr+stream.readInt());
        default:
            throw new ClassFormatError("Invalid opcode "+opcode);
        }
    }
}
