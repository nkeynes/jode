/* 
 * StructuredBlock  (c) 1998 Jochen Hoenicke
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
import jode.AssertError;
import jode.decompiler.TabbedPrintWriter;
import jode.decompiler.LocalInfo;

/**
 * A structured block is the building block of the source programm.
 * For every program construct like if, while, try, or blocks there is
 * a corresponding structured block.
 *
 * Some of these Block are only intermediate representation, that get
 * converted to another block later.
 *
 * Every block has to handle the local variables that it contains.
 * This is done by the in/out vectors and the local variable structure
 * themself.  Every local variable used in this structured block is
 * either in or out.
 *
 * There are following types of structured blocks: 
 * <ul>
 * <li>if-then-(else)-block  (IfThenElseBlock)
 * <li>(do)-while/for-block  (LoopBlock)
 * <li>switch-block          (SwitchBlock)
 * <li>try-catch-block       (CatchBlock)
 * <li>try-finally-block     (FinallyBlock)
 * <li>synchronized-block    (SynchronizedBlock)
 * <li>one-instruction       (InstructionBlock)
 * <li>empty-block           (EmptyBlock)
 * <li>multi-blocks-block    (SequentialBlock)
 * </ul>
 */

public abstract class StructuredBlock {
    /* Invariants:
     * in.intersection(out) = empty
     * outer != null => flowBlock = outer.flowBlock
     * outer == null => flowBlock.block = this
     * jump  == null => outer != null
     * either getNextBlock() != null 
     *     or getNextFlowBlock() != null or outer == null
     * either outer.getNextBlock(this) != null 
     *     or outer.getNextFlowBlock(this) != null
     */

    /**
     * The variable set containing all variables that are used in
     * this block.
     */
    VariableSet used = new VariableSet();

    /**
     * The variable set containing all variables we must declare.
     * The analyzation is done in makeDeclaration
     */
    VariableSet declare;

    /**
     * The surrounding structured block.  If this is the outermost
     * block in a flow block, outer is null.  */
    StructuredBlock outer;

//     /**
//      * The surrounding non sequential block.  This is the same as if
//      * you would repeatedly get outer until you reach a non sequential
//      * block.  This is field is only valid, if the outer block is a
//      * sequential block.
//      */
//     StructuredBlock realOuter;

    /**
     * The flow block in which this structured block lies.  */
    FlowBlock flowBlock;
    
    /** 
     * The jump that follows on this block, or null if there is no
     * jump, but the control flows normal (only allowed if
     * getNextBlock != null).  
     */
    Jump jump;

    /**
     * Returns the block where the control will normally flow to, when
     * this block is finished.
     */
    public StructuredBlock getNextBlock() {
        if (jump != null)
            return null;
        if (outer != null)
            return outer.getNextBlock(this);
        return null;
    }

    public void setJump(Jump jump) {
        this.jump = jump;
        jump.prev = this;
    }

    /**
     * Returns the flow block where the control will normally flow to,
     * when this block is finished.
     * @return null, if the control flows into a non empty structured
     * block or if this is the outermost block.
     */
    public FlowBlock getNextFlowBlock() {
        if (jump != null)
            return jump.destination;
        if (outer != null)
            return outer.getNextFlowBlock(this);
        return null;
    }

    /**
     * Returns the block where the control will normally flow to, when
     * the given sub block is finished. (This is overwritten by
     * SequentialBlock and SwitchBlock).  If this isn't called with a
     * direct sub block, the behaviour is undefined, so take care.
     * @return null, if the control flows to another FlowBlock.  */
    public StructuredBlock getNextBlock(StructuredBlock subBlock) {
        return getNextBlock();
    }

    public FlowBlock getNextFlowBlock(StructuredBlock subBlock) {
        return getNextFlowBlock();
    }

    /**
     * Tells if this block is empty and only changes control flow.
     */
    public boolean isEmpty() {
        return false;
    }

    /**
     * Tells if the sub block is the single exit point of the current block.
     * @param subBlock the sub block.
     * @return true, if the sub block is the single exit point of the
     * current block.  
     */
    public boolean isSingleExit(StructuredBlock subBlock) {
	return false;
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                                   StructuredBlock newBlock) {
        return false;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return new StructuredBlock[0];
    }

    /**
     * Returns if this block contains the given block.
     * @param child the block which should be contained by this block.
     * @return false, if child is null, or is not contained in this block.
     */
    public boolean contains(StructuredBlock child) {
        while (child != this && child != null)
            child = child.outer;
        return (child == this);
    }

    /** 
     * Removes the jump.  This does not update the successors vector
     * of the flow block, you have to do it yourself.  */
    public final void removeJump() {
        if (jump != null) {
            jump.prev = null;
            jump = null;
        }
    }

    /**
     * This will move the definitions of sb and childs to this block,
     * but only descend to sub and not further.  It is assumed that
     * sub will become a sub block of this block, but may not yet.  
     *
     * @param sb The structured block that should be replaced.  
     * @param sub The uppermost sub block of structured block, that
     * will be moved to this block (may be this).  
     */
    void moveDefinitions(StructuredBlock from, StructuredBlock sub) {
        while (from != sub && from != this) {
            used.unionExact(from.used);
            from.used.removeAllElements();
            StructuredBlock[] subs = from.getSubBlocks();
            if (subs.length == 0)
                return;
            for (int i=0; i<subs.length - 1; i++)
                moveDefinitions(subs[i], sub);
            from = subs[subs.length-1];
        }
    }

    /**
     * This function replaces sb with this block.  It copies outer and
     * from sb, and updates the outer block, so it knows that sb was
     * replaced.  You have to replace sb.outer or mustn't use sb
     * anymore.  <p>
     * It will also move the definitions of sb and childs to this block,
     * but only descend to sub and not further.  It is assumed that
     * sub will become a sub block of this block.
     * @param sb The structured block that should be replaced.  
     * @param sub  The uppermost sub block of structured block, 
     *      that will be moved to this block (may be this).
     */
    public void replace(StructuredBlock sb) {
        outer = sb.outer;
        setFlowBlock(sb.flowBlock);
        if (outer != null) {
            outer.replaceSubBlock(sb, this);
        } else {
            flowBlock.block = this;
        }
    }

    /**
     * This function swaps the jump with another block.
     * @param block The block whose jump is swapped.
     */
    public void swapJump(StructuredBlock block) {
        Jump tmp = block.jump;
        block.jump = jump;
        jump = tmp;
        
        jump.prev = this;
        block.jump.prev = block;
    }

    /**
     * This function moves the jump to this block.  
     * The jump field of the previous owner is cleared afterwards.  
     * If the given jump is null, nothing bad happens.
     * @param jump The jump that should be moved, may be null.
     */
    public void moveJump(Jump jump) {
        if (this.jump != null)
            throw new AssertError("overriding with moveJump()");
        this.jump = jump;
        if (jump != null) {
            jump.prev.jump = null;
            jump.prev = this;
        }
    }

    /**
     * This function copies the jump to this block.  
     * If the given jump is null, nothing bad happens.
     * @param jump The jump that should be moved, may be null.
     */
    public void copyJump(Jump jump) {
        if (this.jump != null)
            throw new AssertError("overriding with moveJump()");
        if (jump != null) {
	    this.jump = new Jump(jump);
	    this.jump.prev = this;
        }
    }

    /**
     * Appends a block to this block.
     * @return the new combined block.
     */
    public StructuredBlock appendBlock(StructuredBlock block) {
        if (block instanceof EmptyBlock) {
            moveJump(block.jump);
            return this;
        } else {
            SequentialBlock sequBlock = new SequentialBlock();
            sequBlock.replace(this);
            sequBlock.setFirst(this);
            sequBlock.setSecond(block);
            return sequBlock;
        }
    }

    /**
     * Removes this block, or replaces it with an EmptyBlock.
     */
    public final void removeBlock() {

        if (outer instanceof SequentialBlock) {
            if (outer.getSubBlocks()[1] == this) {
                if (jump != null)
                    outer.getSubBlocks()[0].moveJump(jump);
                outer.getSubBlocks()[0].replace(outer);
            } else
                outer.getSubBlocks()[1].replace(outer);
            return;
        }

        EmptyBlock eb = new EmptyBlock();
        eb.moveJump(jump);
        eb.replace(this);
    }

    /**
     * Determines if there is a path, that flows through the end
     * of this block.  If there is such a path, it is forbidden to
     * change the control flow in after this block and this method
     * returns false.
     * @return true, if the jump may be safely changed.
     */
    public boolean flowMayBeChanged() {
	return jump != null || jumpMayBeChanged();
    }

    public boolean jumpMayBeChanged() {
	return false;
    }

    public VariableSet propagateUsage() {
        StructuredBlock[] subs = getSubBlocks();
        VariableSet allUse = (VariableSet) used.clone();
        for (int i=0; i<subs.length; i++) {
            VariableSet childUse = subs[i].propagateUsage();
            /* All variables used in more than one sub blocks, are
             * used in this block, too.  
             */
            used.unionExact(allUse.intersectExact(childUse));
            allUse.unionExact(childUse);
        }
        return allUse;
    }

    /** 
     * This is called after the analysis is completely done.  It
     * will remove all PUSH/stack_i expressions, (if the bytecode
     * is correct). <p>
     * The default implementation merges the stack after each sub block.
     * This may not be, what you want. <p>
     *
     * @param initialStack the stackmap at begin of the block
     * @return the stack after the block has executed.
     * @throw RuntimeException if something did get wrong.
     */
    public VariableStack mapStackToLocal(VariableStack stack) {
	StructuredBlock[] subBlocks = getSubBlocks();
	VariableStack after;
	if (subBlocks.length == 0)
	    after = stack;
	else {
	    after = null;
	    for (int i=1; i< subBlocks.length; i++) {
		after = VariableStack.merge
		    (after, subBlocks[i].mapStackToLocal(stack));
	    }
	}
	if (jump != null)
	    /* assert(after != null) */
	    jump.stackMap = after;
	return after;
    }

    /** 
     * This is called after mapStackToLocal to do the stack to local
     * transformation.
     */
    public void removePush() {
	StructuredBlock[] subBlocks = getSubBlocks();
	for (int i=0; i< subBlocks.length; i++)
	    subBlocks[i].removePush();
    }

    /**
     * Make the declarations, i.e. initialize the declare variable
     * to correct values.  This will declare every variable that
     * is marked as used, but not done.<br>
     *
     * This will now also combine locals, that use the same slot, have
     * compatible types and are declared in the same block. <br>
     *
     * @param done The set of the already declare variables.
     */
    public void makeDeclaration(VariableSet done) {
	declare = new VariableSet();
	java.util.Enumeration enum = used.elements();
    next_used:
	while (enum.hasMoreElements()) {
	    LocalInfo local = (LocalInfo) enum.nextElement();

	    // Merge with all locals in this block, that use the same 
	    // slot and have compatible type.
	    int size = done.size();
	    for (int i=0; i< size; i++) {
		LocalInfo prevLocal = done.elementAt(i);
		if (prevLocal.getSlot() == local.getSlot()) {
		    if (prevLocal.equals(local))
			continue next_used;

		    /* XXX - I have to think about this...
		     * there may be a case where this leads to type errors.
		     * TODO: Give a formal proof ;-)
		     */
		    if (prevLocal.getType().isOfType(local.getType())) {
			local.combineWith(prevLocal);
			continue next_used;
		    }
		}
	    }

	    LocalInfo previous = done.findLocal(local.getName());
	    if (previous != null) {
		/* A name conflict happened. */
		local.makeNameUnique();
	    }
	    declare.addElement(local);
	}
        done.unionExact(declare);
        StructuredBlock[] subs = getSubBlocks();
	for (int i=0; i<subs.length; i++)
	    subs[i].makeDeclaration(done);
        /* remove the variables again, since we leave the scope.
         */
        done.subtractExact(declare);
    }

    public void checkConsistent() {
        StructuredBlock[] subs = getSubBlocks();
        for (int i=0; i<subs.length; i++) {
            if (subs[i].outer != this ||
                subs[i].flowBlock != flowBlock) {
                throw new AssertError("Inconsistency");
            }
            subs[i].checkConsistent();
        }
        if (jump != null && jump.destination != null) {
            Jump jumps = (Jump) flowBlock.successors.get(jump.destination);
            for (; jumps != jump; jumps = jumps.next) {
                if (jumps == null)
                    throw new AssertError("Inconsistency");
            }
        }
    }

    /**
     * Set the flow block of this block and all sub blocks.
     * @param flowBlock the new flow block
     */
    public void setFlowBlock(FlowBlock flowBlock) {
        if (this.flowBlock != flowBlock) {
            this.flowBlock = flowBlock;
            StructuredBlock[] subs = getSubBlocks();
            for (int i=0; i<subs.length; i++) {
                if (subs[i] != null)
                    subs[i].setFlowBlock(flowBlock);
            }
        }
    }

    /**
     * Tells if this block needs braces when used in a if or while block.
     * @return true if this block should be sorrounded by braces.
     */
    public boolean needsBraces() {
        return true;
    }

    /**
     * Fill all in variables into the given VariableSet.
     * @param in The VariableSet, the in variables should be stored to.
     */
    public void fillInGenSet(VariableSet in, VariableSet gen) {
        /* overwritten by InstructionContainer */
    }

    /**
     * Put all the successors of this block and all subblocks into
     * the given vector.
     * @param succs The vector, the successors should be stored to.
     */
    public void fillSuccessors(java.util.Vector succs) {
        if (jump != null)
            succs.addElement(jump);
        StructuredBlock[] subs = getSubBlocks();
        for (int i=0; i<subs.length; i++) {
            subs[i].fillSuccessors(succs);
        }
    }

    /**
     * Print the source code for this structured block.  This handles
     * everything that is unique for all structured blocks and calls
     * dumpInstruction afterwards.
     * @param writer The tabbed print writer, where we print to.
     */
    public void dumpSource(jode.decompiler.TabbedPrintWriter writer)
        throws java.io.IOException
    {
	if (declare != null) {
	    if (jode.Decompiler.isDebugging) {
		writer.println("declaring: "+declare);
		writer.println("using: "+used);
            }

	    java.util.Enumeration enum = declare.elements();
	    while (enum.hasMoreElements()) {
		LocalInfo local = (LocalInfo) enum.nextElement();
		dumpDeclaration(writer, local);
	    }
	}
        dumpInstruction(writer);

        if (jump != null)
            jump.dumpSource(writer);
    }

    /**
     * Print the code for the declaration of a local variable.
     * @param writer The tabbed print writer, where we print to.
     * @param local  The local that should be declared.
     */
    public void dumpDeclaration(jode.decompiler.TabbedPrintWriter writer, LocalInfo local)
        throws java.io.IOException
    {
	writer.println(local.getType().toString() + " "
                       + local.getName().toString() + ";");
    }

    /**
     * Print the instruction expressing this structured block.
     * @param writer The tabbed print writer, where we print to.
     */
    public abstract void dumpInstruction(TabbedPrintWriter writer)
        throws java.io.IOException;

    public String toString() {
        try {
            java.io.StringWriter strw = new java.io.StringWriter();
            jode.decompiler.TabbedPrintWriter writer = 
                new jode.decompiler.TabbedPrintWriter(strw);
            writer.println(super.toString());
            writer.tab();
            dumpSource(writer);
            return strw.toString();
        } catch (java.io.IOException ex) {
            return super.toString();
        }
    }

    /**
     * Do simple transformation on the structuredBlock.
     */
    public boolean doTransformations() {
        return false;
    }
}

