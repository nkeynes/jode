/* 
 * LoopBlock  (c) 1998 Jochen Hoenicke
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

package jode.flow;
import jode.TabbedPrintWriter;
import jode.Expression;
import jode.ConstOperator;
import jode.Type;
import jode.LocalInfo;
import jode.LocalStoreOperator;

/**
 * This is the structured block for an Loop block.
 */
public class LoopBlock extends StructuredBlock implements BreakableBlock {

    public static final int WHILE = 0;
    public static final int DOWHILE = 1;
    public static final int FOR = 2;
    public static final int POSSFOR = 3;

    public static final Expression TRUE = 
        new ConstOperator(Type.tBoolean, "1");
    public static final Expression FALSE = 
        new ConstOperator(Type.tBoolean, "0");

    /**
     * The condition.  Must be of boolean type.
     */
    Expression cond;
    /**
     * The init instruction, only valid if type == FOR or POSSFOR
     */
    InstructionBlock init;
    /**
     * The increase instruction, only valid if type == FOR or POSSFOR.
     */
    InstructionBlock incr;
    
    /**
     * True, if the initializer is a declaration.
     */
    boolean isDeclaration;

    /**
     * The type of the loop.  This must be one of DOWHILE, WHILE or FOR.
     */
    int type;

    /**
     * The body of this loop.  This is always a valid block and not null 
     */
    StructuredBlock bodyBlock;

    /**
     * Returns the block where the control will normally flow to, when
     * the given sub block is finished (<em>not</em> ignoring the jump
     * after this block). (This is overwritten by SequentialBlock and
     * SwitchBlock).  If this isn't called with a direct sub block,
     * the behaviour is undefined, so take care.  
     * @return null, if the control flows to another FlowBlock.  */
    public StructuredBlock getNextBlock(StructuredBlock subBlock) {
        return this;
    }

    public FlowBlock getNextFlowBlock(StructuredBlock subBlock) {
        return null;
    }
    
    public LoopBlock(int type, Expression cond) {
        this.type = type;
        this.cond = cond;
        this.mayChangeJump = (cond == TRUE);
    }

    public void setBody(StructuredBlock body) {
        bodyBlock = body;
        bodyBlock.outer = this;
        body.setFlowBlock(flowBlock);
    }

    public Expression getCondition() {
        return cond;
    }

    public void putBackInit() {
        StructuredBlock last = 
            (outer instanceof SequentialBlock
             && outer.getSubBlocks()[0] == this) ? outer : this;

        SequentialBlock sequBlock = new SequentialBlock();
        sequBlock.replace(last);
        sequBlock.setFirst(init);
        sequBlock.setSecond(last);
        init = null;
    }

    public void setCondition(Expression cond) {
        this.cond = cond;
        if (type == POSSFOR) {
            /* canCombine returns 1 if cond contains a sub expression
             * that matches the store in incr */
            if (cond.containsMatchingLoad(incr.getInstruction())) {
                type = FOR;
                if (init != null
                    && !cond.containsMatchingLoad(init.getInstruction())) {
                    /* This is a for, but the init instruction doesn't
                     * match.  Put the init back to its old place.
                     */
                    putBackInit();
                }
            } else {
                type = WHILE;
                StructuredBlock last = bodyBlock;
                while (last instanceof SequentialBlock)
                    last = last.getSubBlocks()[1];
                last.appendBlock(incr);
                incr = null;
                if (init != null)
                    putBackInit();
            }
        }
        mayChangeJump = false;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public VariableSet propagateUsage() {
        if (init != null)
            used.unionExact(init.used);
        if (incr != null)
            used.unionExact(incr.used);
        VariableSet allUse = (VariableSet) used.clone();
        allUse.unionExact(bodyBlock.propagateUsage());
        return allUse;
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                                   StructuredBlock newBlock) {
        if (bodyBlock == oldBlock)
            bodyBlock = newBlock;
        else
            return false;
        return true;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return new StructuredBlock[] { bodyBlock };
    }

    public void dumpDeclaration(TabbedPrintWriter writer, LocalInfo local)
	throws java.io.IOException
    {
        if ((type == FOR || type == POSSFOR) && init != null
            && (outer == null || !outer.used.contains(local))
            && (init.getInstruction().getOperator() 
                instanceof LocalStoreOperator)
            && (((LocalStoreOperator) 
                 init.getInstruction().getOperator()).getLocalInfo() 
                == local.getLocalInfo()))
            isDeclaration = true;
        else
            super.dumpDeclaration(writer, local);
    }

    public void dumpSource(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        isDeclaration = false;
        super.dumpSource(writer);
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        if (label != null) {
            writer.untab();
            writer.println(label+":");
            writer.tab();
        }
        boolean needBrace = bodyBlock.needsBraces();
        switch (type) {
        case WHILE:
            if (cond == TRUE)
                /* special syntax for endless loops: */
                writer.print("for (;;)");
            else
                writer.print("while ("+cond.simplify().toString()+")");
            break;
        case DOWHILE:
            writer.print("do");
            break;
        case FOR:
        case POSSFOR:
            writer.print("for (");
            if (init != null) {
                if (isDeclaration)
                    writer.print(((LocalStoreOperator) 
                                  init.getInstruction().getOperator())
                                 .getLocalInfo().getType().toString()
                                 + " ");
                writer.print(init.getInstruction().simplify().toString());
            }
            writer.print("; "+cond.simplify().toString()+"; "
                         +incr.getInstruction().simplify().toString()+")");
            break;
        }
        writer.println( needBrace?" {": "");
        writer.tab();
        bodyBlock.dumpSource(writer);
        writer.untab();
        if (type == DOWHILE)
            writer.println((needBrace?"} ": "")+
                           "while ("+cond.simplify().toString()+");");
        else if (needBrace)
            writer.println("}");
    }


    boolean mayChangeJump = true;

    /**
     * The serial number for labels.
     */
    static int serialno = 0;

    /**
     * The label of this instruction, or null if it needs no label.
     */
    String label = null;

    /**
     * Returns the label of this block and creates a new label, if
     * there wasn't a label previously.
     */
    public String getLabel() {
        if (label == null)
            label = "while_"+(serialno++)+"_";
        return label;
    }

    /**
     * Is called by BreakBlock, to tell us that this block is breaked.
     */
    public void setBreaked() {
	mayChangeJump = false;
    }

    /**
     * Replace all breaks to this block with a continue to this block.
     */
    public void replaceBreakContinue(BreakableBlock block) {
        java.util.Stack todo = new java.util.Stack();
        todo.push(block);
        while (!todo.isEmpty()) {
            StructuredBlock[] subs = 
                ((StructuredBlock)todo.pop()).getSubBlocks();
            for (int i=0; i<subs.length; i++) {
                if (subs[i] instanceof BreakBlock) {
                    BreakBlock breakblk = (BreakBlock) subs[i];
                    if (breakblk.breaksBlock == block) {
                        new ContinueBlock(this, breakblk.label != null)
                            .replace(breakblk);
                    }
                }
                todo.push(subs[i]);
            }
        }
    }

    /**
     * Determines if there is a sub block, that flows through to the end
     * of this block.  If this returns true, you know that jump is null.
     * @return true, if the jump may be safely changed.
     */
    public boolean jumpMayBeChanged() {
        return mayChangeJump;
    }

    public boolean doTransformations() {
        return init == null && (type == FOR || type == POSSFOR)
            && CreateForInitializer.transform(this, flowBlock.lastModified);
    }
}
