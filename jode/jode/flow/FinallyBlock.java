/* FinallyBlock Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.flow;

/**
 * 
 * @author Jochen Hoenicke
 */
public class FinallyBlock extends StructuredBlock {
    /**
     * The catch block.
     */
    StructuredBlock subBlock;

    public FinallyBlock() {
    }

    /** 
     * Sets the catch block.
     * @param subBlock the catch block.
     */
    public void setCatchBlock(StructuredBlock subBlock) {
        this.subBlock = subBlock;
        subBlock.outer = this;
        subBlock.setFlowBlock(flowBlock);
    }

    /* The implementation of getNext[Flow]Block is the standard
     * implementation */

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                                   StructuredBlock newBlock) {
        if (subBlock == oldBlock)
            subBlock = newBlock;
        else
            return false;
        return true;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {        
        return new StructuredBlock[] { subBlock };
    }

    /**
     * Returns the block where the control will normally flow to, when
     * the given sub block is finished (<em>not</em> ignoring the jump
     * after this block).  FinallyBlock have a special behaviour, since
     * the finally block has no default successor at all (it is more a
     * subroutine) that will be called by try or any exception.
     *
     * @return null, if the control flows to another FlowBlock.  
     */
    public StructuredBlock getNextBlock(StructuredBlock subBlock) {
        return null;
    }

    public FlowBlock getNextFlowBlock(StructuredBlock subBlock) {
        return null;
    }

    public void dumpInstruction(jode.decompiler.TabbedPrintWriter writer) 
        throws java.io.IOException {
	writer.closeBraceContinue();
        writer.print("finally");
	writer.openBrace();
        writer.tab();
        subBlock.dumpSource(writer);
        writer.untab();
    }
}
