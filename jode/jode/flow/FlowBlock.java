/* 
 * FlowBlock  (c) 1998 Jochen Hoenicke
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
import java.util.*;
import jode.Decompiler;
import jode.AssertError;
import jode.decompiler.TabbedPrintWriter;
import jode.decompiler.CodeAnalyzer;
import jode.expr.Expression;
import jode.expr.CombineableOperator;

/**
 * A flow block is the structure of which the flow graph consists.  A
 * flow block contains structured code together with some conditional
 * or unconditional jumps to the head of other flow blocks.
 *
 * We do a T1/T2 analysis to combine all flow blocks to a single.  If
 * the graph isn't reducible that doesn't work, but java can only
 * produce reducible flow graphs.
 */
public class FlowBlock {

    static FlowBlock END_OF_METHOD = 
        new FlowBlock(null, Integer.MAX_VALUE, 0, new EmptyBlock());

    static FlowBlock ILLEGAL = 
        new FlowBlock(null, -1, 0, new DescriptionBlock("ILLEGAL BLOCK"));

    static {
        END_OF_METHOD.label = "END_OF_METHOD";
    }

    /**
     * The code analyzer.  This is used to pretty printing the
     * Types and to get information about all locals in this code.
     */
    CodeAnalyzer code;

    /**
     * The in locals.  This are the locals, which are used in this
     * flow block and whose values may be the result of a assignment
     * outside of this flow block.  That means, that there is a
     * path from the start of the flow block to the instruction that
     * uses that variable, on which it is never assigned 
     */
    VariableSet in = new VariableSet(); 
    /**
     * The gen locals.  This are the locals, to which are written
     * somewhere in this flow block.  This is only used for try
     * catch blocks.
     */
    VariableSet gen = new VariableSet(); 

    /**
     * The starting address of this flow block.  This is mainly used
     * to produce the source code in code order.
     */
    int addr;

    /**
     * The length of the structured block, only needed at the beginning.
     */
    int length;

    /**
     * The outermost structructed block in this flow block.
     */
    StructuredBlock block;

    /**
     * The last modified structured block.  This is probably the
     * last instruction in the outermost block, that is in the
     * outermost chain of SequentialBlock.
     */
    StructuredBlock lastModified;

    /**
     * This contains a map of all successing flow blocks and there
     * jumps.  The key of this dictionary are the flow blocks, while
     * the elements is the first jump to that flow block.  The other
     * jumps are accessible via the jump.next field.
     */
    Dictionary successors = new SimpleDictionary();

    /**
     * This is a vector of flow blocks, which reference this block.
     * Only if this vector contains exactly one element, it can be
     * moved into the preceding flow block.
     *
     * If this vectors contains the null element, this is the first
     * flow block in a method.
     */
    Vector predecessors = new Vector();

    /**
     * This is set if the block is reachable.  If this is false after
     * marking the start block and the catch blocks as reachable, this
     * block is dead code and may (must) be removed.
     */
    boolean reachable = false;
    
    /**
     * This is a pointer to the next flow block in byte code order.
     * It is null for the last flow block.
     */
    FlowBlock nextByAddr;

    /**
     * This is a pointer to the previous flow block in byte code order.
     * It is null for the first flow block.
     */
    FlowBlock prevByAddr;

    /**
     * The stack map.  This tells how many objects are on stack at
     * begin of the flow block, and to what locals they are maped.
     * @see mapStackToLocal
     */
    VariableStack stackMap;

    /**
     * The default constructor.  Creates a new flowblock containing
     * only the given structured block.
     */
    public FlowBlock(CodeAnalyzer code, int addr, int length, 
		     StructuredBlock block) {
	this.code = code;
        this.addr = addr;
        this.length = length;
        this.block = block;
        lastModified = block;
        block.setFlowBlock(this);
        block.fillInGenSet(in, gen);
    }

    public final int getNextAddr() {
        return addr+length;
    }

    public boolean hasNoJumps() {
	return successors.size() == 0 && predecessors.size() == 0;
    }

    /**
     * This method resolves some of the jumps to successor.
     * @param jumps The list of jumps with that successor.
     * @param succ  The successing flow block.
     * @return The remaining jumps, that couldn't be resolved.
     */
    public Jump resolveSomeJumps(Jump jumps, FlowBlock succ) {
	/* We will put all jumps that we can not resolve into this
	 * linked list.
	 */
        Jump remainingJumps = null;

        if (lastModified.jump == null) {
	    /* This can happen if lastModified is a breakable block, and
	     * there is no break to it yet.  We give lastModified this jump
	     * as successor since many other routines rely on this.
	     */
            Jump lastJump = new Jump(succ);
            lastModified.setJump(lastJump);
            remainingJumps = lastJump;
        }

        for (Jump jump = jumps; jump != null; jump = jump.next) {
            /* First swap all conditional blocks, that have two jumps,
             * so that the jump to succ will be on the outside.
             */
            if (jump.prev.outer instanceof ConditionalBlock
                && jump.prev.outer.jump != null) {

                StructuredBlock prev = jump.prev;
                ConditionalBlock cb = (ConditionalBlock) prev.outer;
                Expression instr = cb.getInstruction();
                
                cb.setInstruction(instr.negate());
                cb.swapJump(prev);
            }
        }
    next_jump:
        while (jumps != null) {
            Jump jump = jumps;
            jumps = jumps.next;

            /* if the jump is the jump of lastModified, skip it.
             */
            if (jump.prev == lastModified) {
                jump.next = remainingJumps;
                remainingJumps = jump;
                continue;
            }

            /* jump.prev.outer is not null, otherwise jump.prev would
             * be lastModified.
             */

            if (jump.prev.outer instanceof ConditionalBlock) {
                StructuredBlock prev = jump.prev;
                ConditionalBlock cb = (ConditionalBlock) prev.outer;
                Expression instr = cb.getInstruction();

		/* This is a jump inside an ConditionalBlock. 
		 *
		 * cb    is the conditional block, 
		 * prev  the empty block containing the jump
		 * instr is the condition */

                if (cb.jump != null) {
                    /* This can only happen if cb also jumps to succ.
                     * This is a weired "if (cond) empty"-block.  We
                     * transform it by hand.  
                     */		    
                    prev.removeJump();
                    IfThenElseBlock ifBlock = 
                        new IfThenElseBlock(cb.getInstruction().negate());
                    ifBlock.moveDefinitions(cb, prev);
                    ifBlock.replace(cb);
		    ifBlock.moveJump(cb.jump);
                    ifBlock.setThenBlock(prev);
		    if (cb == lastModified)
			lastModified = ifBlock;
                    continue;
                }

                /* Now cb.jump is null, so cb.outer is not null,
                 * since otherwise it would have no successor.  */

                if (cb.outer instanceof LoopBlock 
                    || (cb.outer instanceof SequentialBlock 
                        && cb.outer.getSubBlocks()[0] == cb 
                        && cb.outer.outer instanceof LoopBlock)) {
            
		    /* If this is the first instruction of a
		     * while/for(true) block, make this the loop condition
		     * (negated of course).
		     */

                    LoopBlock loopBlock = (cb.outer instanceof LoopBlock) ?
                        (LoopBlock) cb.outer : (LoopBlock) cb.outer.outer;

                    if (loopBlock.getCondition() == LoopBlock.TRUE &&
                        loopBlock.getType() != LoopBlock.DOWHILE &&
                        (loopBlock.jumpMayBeChanged()
                         || loopBlock.getNextFlowBlock() == succ)) {
                        
                        if (loopBlock.jump == null) {
                            /* consider this jump again */
                            loopBlock.moveJump(jump);
                            jumps = jump;
                        } else
                            jump.prev.removeJump();

                        loopBlock.setCondition(instr.negate());
                        loopBlock.moveDefinitions(cb, null);
                        cb.removeBlock();
                        continue;
                    }

                } else if (cb.outer instanceof SequentialBlock 
                           && cb.outer.getSubBlocks()[1] == cb) {

                    /* And now for do/while loops, where the jump is
                     * at the end of the loop.
                     */
                    
                    /* First find the beginning of the loop */
                    StructuredBlock sb = cb.outer.outer;
                    while (sb instanceof SequentialBlock) {
                        sb = sb.outer;
                    }
                    /* sb is now the first and cb is the last
                     * instruction in the current block.
                     */
                    if (sb instanceof LoopBlock) {
                        LoopBlock loopBlock = (LoopBlock) sb;
                        if (loopBlock.getCondition() == LoopBlock.TRUE &&
                            loopBlock.getType() == LoopBlock.WHILE &&
                            (loopBlock.jumpMayBeChanged()
                             || loopBlock.getNextFlowBlock() == succ)) {
                            
                            if (loopBlock.jump == null) {
                                /* consider this jump again */
                                loopBlock.moveJump(jump);
                                jumps = jump;
                            } else
                                jump.prev.removeJump();

                            loopBlock.setType(LoopBlock.DOWHILE);
                            loopBlock.setCondition(instr.negate());
                            loopBlock.moveDefinitions(cb, null);
                            cb.removeBlock();                            
                            continue;
                        }
                    }
                }

		/* This is still a jump inside an ConditionalBlock. 
		 *
		 * cb    is the conditional block, 
		 * prev  the empty block containing the jump
		 * instr is the condition */

		/* replace all conditional jumps to the successor, which
		 * are followed by a block which has the end of the block
		 * as normal successor, with "if (not condition) block":
		 *
		 *  /IF cond GOTO succ          if (!cond)
		 *  \block               ===>     block
		 * -> normal Succesor succ     -> normal Successor succ
		 */
                if (cb.outer instanceof SequentialBlock && 
                    cb.outer.getSubBlocks()[0] == cb &&
                    (cb.outer.getNextFlowBlock() == succ ||
                     cb.outer.jumpMayBeChanged())) {

                    SequentialBlock sequBlock = (SequentialBlock) cb.outer;
                    
                    IfThenElseBlock newIfBlock 
                        = new IfThenElseBlock(instr.negate());
                    StructuredBlock thenBlock = sequBlock.getSubBlocks()[1];

                    newIfBlock.moveDefinitions(sequBlock, thenBlock);
                    newIfBlock.replace(sequBlock);
                    newIfBlock.setThenBlock(thenBlock);

                    if (thenBlock.contains(lastModified)) {
                        if (lastModified.jump.destination == succ) {
                            newIfBlock.moveJump(lastModified.jump);
                            lastModified = newIfBlock;
                            jump.prev.removeJump();
                            continue;
                        }
                        lastModified = newIfBlock;
                    }

                    newIfBlock.moveJump(jump);
                    /* consider this jump again */
                    jumps = jump;
                    continue;
                }
            } else {
		
		/* remove this jump if it jumps to the
		 * getNextFlowBlock().  */
		if (jump.destination
		    == jump.prev.outer.getNextFlowBlock(jump.prev)) {
			jump.prev.removeJump();
			continue;
		}


                /* Now find the real outer block, that is ascend the chain
                 * of SequentialBlocks.
                 *
                 * Note that only the last instr in a SequentialBlock chain
                 * can have a jump.
                 *
                 * We rely on the fact, that instanceof returns false
                 * for a null pointer.  
                 */
                StructuredBlock sb = jump.prev.outer;
                while (sb instanceof SequentialBlock)
                    sb = sb.outer;
                

                /* if this is an unconditional jump at the end of a
		 * then block belonging to a if-then block without
		 * else part, and the if block has a jump then replace
		 * the if-then block with a if-then-else block with an
		 * else block that contains only the jump and move the
		 * unconditional jump to the if.  (The jump in the else
		 * block will later probably be replaced with a break,
		 * continue or return statement.)
		 */
                if (sb instanceof IfThenElseBlock) {
                    IfThenElseBlock ifBlock = (IfThenElseBlock) sb;
                    if (ifBlock.elseBlock == null && ifBlock.jump != null) {
			ifBlock.setElseBlock(new EmptyBlock());
			ifBlock.elseBlock.moveJump(ifBlock.jump);
			ifBlock.moveJump(jump);
			/* consider this jump again */
			jumps = jump;
			continue;
		    }
		}

                /* if this is a jump at the end of a then block belonging
                 * to a if-then block without else part, and the if-then
                 * block is followed by a single block, then replace the
                 * if-then block with a if-then-else block and move the
                 * unconditional jump to the if.
                 */
                if (sb instanceof IfThenElseBlock
		    && sb.outer instanceof SequentialBlock
		    && sb.outer.getSubBlocks()[0] == sb) {
		    
                    IfThenElseBlock ifBlock = (IfThenElseBlock) sb;
                    SequentialBlock sequBlock = (SequentialBlock) sb.outer;
		    StructuredBlock elseBlock = sequBlock.subBlocks[1];
                    
                    if (ifBlock.elseBlock == null
                        && (elseBlock.getNextFlowBlock() == succ
			    || elseBlock.jump != null
                            || elseBlock.jumpMayBeChanged())) {
                        
                        ifBlock.replace(sequBlock);
                        ifBlock.setElseBlock(elseBlock);

                        if (elseBlock.contains(lastModified)) {
                            if (lastModified.jump.destination == succ) {
                                ifBlock.moveJump(lastModified.jump);
                                lastModified = ifBlock;
                                jump.prev.removeJump();
                                continue;
                            }
                            lastModified = ifBlock;
                        }
                            
                        /* consider this jump again */
                        ifBlock.moveJump(jump);
                        jumps = jump;
                        continue;
                    }
		}
            }

            /* if this is a jump in a breakable block, and that block
             * has not yet a next block, then create a new jump to that
             * successor.
             *
             * The break to the block will be generated later.
             */

            for (StructuredBlock surrounder = jump.prev.outer;
                 surrounder != null; surrounder = surrounder.outer) {
                if (surrounder instanceof BreakableBlock) {
                    if (surrounder.getNextFlowBlock() != succ
                        && surrounder.jumpMayBeChanged()) {

                        surrounder.setJump(new Jump(succ));
                        surrounder.jump.next = jumps;
			/* consider this jump again */
                        jumps = surrounder.jump;
			continue next_jump;
                    }
		    if (succ == END_OF_METHOD) {
			/* If the jump can be replaced by a return
			 * we won't do labeled breaks, so we must 
			 * stop here
			 */
			break;
		    }
                }
            }
            jump.next = remainingJumps;
            remainingJumps = jump;
        }
        return remainingJumps;
    }

    /**
     * Resolve remaining jumps to the successor by generating break
     * instructions.  As last resort generate a do while(false) block.
     * @param jumps The jump list that need to be resolved.
     */
    void resolveRemaining(Jump jumps) {
        LoopBlock doWhileFalse = null;
        StructuredBlock outerMost = lastModified;
        boolean removeLast = false;
    next_jump:
        for (; jumps != null; jumps = jumps.next) {
            StructuredBlock prevBlock = jumps.prev;
	    
            if (prevBlock == lastModified) {
                /* handled below */
                removeLast = true;
                continue;
            }
            
            int breaklevel = 0;
            BreakableBlock breakToBlock = null;
            for (StructuredBlock surrounder = prevBlock.outer;
                 surrounder != null; surrounder = surrounder.outer) {
                if (surrounder instanceof BreakableBlock) {
                    breaklevel++;
                    if (surrounder.getNextFlowBlock() == jumps.destination) {
                        breakToBlock = (BreakableBlock) surrounder;
                        break;
                    }
                }
            }
            
            prevBlock.removeJump();
            
            if (breakToBlock == null) {
                /* Nothing else helped, so put a do/while(0)
                 * block around outerMost and break to that
                 * block.
                 */
                if (doWhileFalse == null) {
                    doWhileFalse = new LoopBlock(LoopBlock.DOWHILE, 
                                                 LoopBlock.FALSE);
                }
                /* Adapt outermost, so that it contains the break. */
                while (!outerMost.contains(prevBlock))
                    outerMost = outerMost.outer;
                prevBlock.appendBlock
                    (new BreakBlock(doWhileFalse, breaklevel > 0));
            } else
                prevBlock.appendBlock
                    (new BreakBlock(breakToBlock, breaklevel > 1));
        }
        
        if (removeLast)
            lastModified.removeJump();

        if (doWhileFalse != null) {
            doWhileFalse.replace(outerMost);
            doWhileFalse.setBody(outerMost);
            lastModified = doWhileFalse;
        }
    }

    /**
     * Move the successors of the given flow block to this flow block.
     * @param succ the other flow block 
     */
    void mergeSuccessors(FlowBlock succ) {
        /* Merge the sucessors from the successing flow block
         */
        Enumeration keys = succ.successors.keys();
        Enumeration succs = succ.successors.elements();
        while (keys.hasMoreElements()) {
            FlowBlock dest = (FlowBlock) keys.nextElement();
            Jump hisJumps = (Jump) succs.nextElement();
            Jump myJumps = (Jump) successors.get(dest);

            dest.predecessors.removeElement(succ);
            if (myJumps == null) {
                dest.predecessors.addElement(this);
                successors.put(dest, hisJumps);
            } else {
                while (myJumps.next != null)
                    myJumps = myJumps.next;
                myJumps.next = hisJumps;
            }
        }
    }

    /**
     * Fixes the addr chained list, after merging this block with succ.
     */
    public void mergeAddr(FlowBlock succ) {
	if (succ.nextByAddr == this || succ.prevByAddr == null) {
	    /* Merge succ with its nextByAddr.
	     * Note: succ.nextByAddr != null, since this is on the
	     * nextByAddr chain. */
	    succ.nextByAddr.addr = succ.addr;
	    succ.nextByAddr.length += succ.length;

	    succ.nextByAddr.prevByAddr = succ.prevByAddr;
	    if (succ.prevByAddr != null) 
		succ.prevByAddr.nextByAddr = succ.nextByAddr;
	} else {
	    /* Merge succ with its prevByAddr */
	    succ.prevByAddr.length += succ.length;

	    succ.prevByAddr.nextByAddr = succ.nextByAddr;
	    if (succ.nextByAddr != null)
		succ.nextByAddr.prevByAddr = succ.prevByAddr;
	} 
    }

//     /**
//      * Mark all predecessors of this block as illegal, i.e. change the
//      * successor to the ILLEGAL block.
//      */
//     public void markPredecessorsIllegal() {
// 	Enumeration preds = predecessors.elements();
// 	while (preds.hasMoreElements()) {
// 	    FlowBlock pred = (FlowBlock) preds.nextElement();
// 	    Jump jump = (Jump)pred.successors.remove(this);
// 	    for (/**/; jump.next != null; jump = jump.next)
// 		jump.destination = ILLEGAL;
	    
// 	    jump.destination = ILLEGAL;
// 	    jump.next = (Jump) pred.successors.get(jump.destination);
// 	    if (jump.next == null)
// 		ILLEGAL.predecessors.addElement(this);
// 	    pred.successors.put(jump.destination, jump);
// 	}
// 	predecessors.removeAllElements();
//     }

    /** 
     * Updates the in/out-Vectors of the structured block of the
     * successing flow block simultanous to a T1 transformation.
     * @param successor The flow block which is unified with this flow
     * block.  
     * @param jumps The list of jumps to successor in this block.
     * @return The variables that must be defined in this block.
     */
    void updateInOut (FlowBlock successor, Jump jumps) {
        /* First get the out vectors of all jumps to successor and
         * calculate the intersection.
         */
        VariableSet gens = new VariableSet();
        VariableSet kills =  null;

        for (;jumps != null; jumps = jumps.next) {
            gens.unionExact(jumps.gen);
            if (kills == null) 
                kills = jumps.kill;
            else
                kills = kills.intersect(jumps.kill);
        }
        
        /* Merge the locals used in successing block with those written
         * by this blocks
         */
        successor.in.merge(gens);
        
        /* Now update in and out set of successing block */

        if (successor != this)
            successor.in.subtract(kills);

        /* The gen/kill sets must be updated for every jump 
         * in the other block */
        Enumeration succSuccs = successor.successors.elements();
        while (succSuccs.hasMoreElements()) {
            Jump succJumps = (Jump) succSuccs.nextElement();
            for (; succJumps != null; succJumps = succJumps.next) {

                succJumps.gen.mergeGenKill(gens, succJumps.kill);
                if (successor != this)
                    succJumps.kill.add(kills);
            }
        }
        this.in.unionExact(successor.in);
        this.gen.unionExact(successor.gen);

        if (Decompiler.debugInOut) {
            Decompiler.err.println("UpdateInOut: gens : "+gens);
            Decompiler.err.println("             kills: "+kills);
            Decompiler.err.println("             s.in : "+successor.in);
            Decompiler.err.println("             in   : "+in);
        }
    }
    
    /**
     * Checks if the FlowBlock and its StructuredBlocks are
     * consistent.  There are to many conditions to list them
     * here, the best way is to read this function and all other
     * checkConsistent functions.
     */
    public void checkConsistent() {
        /* This checks are very time consuming, so don't do them
         * normally.
         */
        if (!Decompiler.doChecks)
            return;

        if (block.outer != null || block.flowBlock != this) {
            throw new AssertError("Inconsistency");
        }
        block.checkConsistent();

        Enumeration preds = predecessors.elements();
        while (preds.hasMoreElements()) {
            FlowBlock pred = (FlowBlock)preds.nextElement();
            if (pred == null)
                /* The special start marker */
                continue;
            if (pred.successors.get(this) == null)
                throw new AssertError("Inconsistency");
        }

        StructuredBlock last = lastModified;
        while (last.outer instanceof SequentialBlock
               || last.outer instanceof TryBlock)
            last = last.outer;
        if (last.outer != null)
            throw new AssertError("Inconsistency");

        Enumeration keys = successors.keys();
        Enumeration succs = successors.elements();
        while (keys.hasMoreElements()) {
            FlowBlock dest = (FlowBlock) keys.nextElement();
            if (!dest.predecessors.contains(this))
                throw new AssertError("Inconsistency");
                
            Jump jumps = (Jump)succs.nextElement();
            if (jumps == null)
                throw new AssertError("Inconsistency");
                
            for (; jumps != null; jumps = jumps.next) {
                    
                if (jumps.destination != dest)
                    throw new AssertError("Inconsistency");
                    
                if (jumps.prev.flowBlock != this ||
                    jumps.prev.jump != jumps)
                    throw new AssertError("Inconsistency");
                    
            prev_loop:
                for (StructuredBlock prev = jumps.prev; prev != block;
                     prev = prev.outer) {
                    if (prev.outer == null)
                        throw new RuntimeException("Inconsistency");
                    StructuredBlock[] blocks = prev.outer.getSubBlocks();
                    int i;
                    for (i=0; i<blocks.length; i++)
                        if (blocks[i] == prev)
                            continue prev_loop;
                        
                    throw new AssertError("Inconsistency");
                }
            }
        }
    }


    /**
     * This is a special T1 transformation, that does also succeed, if
     * the jumps in the flow block are not yet resolved.  But it has
     * a special precondition:  The succ must be a simple instruction block,
     * mustn't have another predecessor and all structured blocks in this
     * flow block must be simple instruction blocks.
     * @param succ The successing structured block that should be merged.
     * @param length the length of the structured block.
     */
    public void doSequentialT1(StructuredBlock succ, int length) {
	if (Decompiler.isFlowDebugging) {
	    Decompiler.err.println("merging sequentialBlock: "+this);
	    Decompiler.err.println("and: "+succ);
	}
        VariableSet succIn = new VariableSet();
        succ.fillInGenSet(succIn, this.gen);

        succIn.merge(lastModified.jump.gen);
        succIn.subtract(lastModified.jump.kill);
        succ.jump.gen.mergeGenKill(lastModified.jump.gen, succ.jump.kill);
        succ.jump.kill.add(lastModified.jump.kill);
        this.in.unionExact(succIn);

        lastModified.removeJump();
        lastModified = lastModified.appendBlock(succ);
        this.length += length;
        doTransformations();
    }

    /**
     * Do a T1 transformation with succ if possible.  It is possible,
     * iff succ has exactly this block as predecessor.
     * @param succ the successor block, must be a valid successor of this
     * block, i.e. not null
     */
    public boolean doT1(FlowBlock succ) {
        /* check if this successor has only this block as predecessor. 
         * if the predecessor is not unique, return false. */
        if (succ.predecessors.size() != 1 ||
            succ.predecessors.elementAt(0) != this)
            return false;

        checkConsistent();
        succ.checkConsistent();

        Jump jumps = (Jump) successors.remove(succ);

        /* Update the in/out-Vectors now */
        updateInOut(succ, jumps);
        if (Decompiler.isFlowDebugging)
            Decompiler.err.println("before Resolve: "+this);

        /* Try to eliminate as many jumps as possible.
         */
        jumps = resolveSomeJumps(jumps, succ);
        if (Decompiler.isFlowDebugging)
            Decompiler.err.println("before Remaining: "+this);
        resolveRemaining(jumps);
        if (Decompiler.isFlowDebugging)
            Decompiler.err.println("after Resolve: "+this);

        /* Now unify the blocks.
         */
        lastModified = lastModified.appendBlock(succ.block);
        mergeSuccessors(succ);

        /* This will also set last modified to the new correct value.  */
        doTransformations();

        /* Set addr and length to correct value and update nextByAddr */
	mergeAddr(succ);

        /* T1 transformation succeeded */
        checkConsistent();
        return true;
    }

//     /**
//      * Find the exit condition of a for/while block.  The loop block
//      * mustn't have an exit condition yet.
//      */
//     public void mergeCondition() {
//         /* If the first instruction of a while is a conditional
//          * block, which jumps to the next address use the condition
//          * as while condition.  
//          */
//         LoopBlock loopBlock = (LoopBlock) lastModified;
//         int loopType = loopBlock.getType();

//         ConditionalBlock cb = null;
//         if (loopBlock.bodyBlock instanceof ConditionalBlock)
//             cb = (ConditionalBlock) loopBlock.bodyBlock;
//         else if (loopBlock.bodyBlock instanceof SequentialBlock
//                  && loopBlock.bodyBlock.getSubBlocks()[0] 
//                  instanceof ConditionalBlock)
//             cb = (ConditionalBlock) loopBlock.bodyBlock.getSubBlocks()[0];
//         else if (loopBlock.bodyBlock instanceof SequentialBlock
//                  && loopType == LoopBlock.WHILE) {
//             loopType = LoopBlock.DOWHILE;
//             SequentialBlock sequBlock = (SequentialBlock) loopBlock.bodyBlock;
//             while (sequBlock.subBlocks[1] instanceof SequentialBlock)
//                 sequBlock = (SequentialBlock) sequBlock.subBlocks[1];
//             if (sequBlock.subBlocks[1] instanceof ConditionalBlock)
//                 cb = (ConditionalBlock) sequBlock.subBlocks[1];
//         }

//         if (cb != null 
//             && cb.trueBlock.jump.destination.addr == getNextAddr()) {
//             loopBlock.moveJump(cb.trueBlock.jump);
//             loopBlock.setCondition(cb.getInstruction().negate());
//             loopBlock.setType(loopType);
//             loopBlock.moveDefinitions(cb, null);
//             cb.removeBlock();
//         }
//     }

    public boolean doT2(int start, int end) {
        /* If there are no jumps to the beginning of this flow block
         * or if this block has other predecessors with a not yet
         * considered address, return false.  The second condition
         * make sure that not for each continue a while is created.
         */
        if (!predecessors.contains(this))
            return false;
        Enumeration preds = predecessors.elements();
        while (preds.hasMoreElements()) {
            FlowBlock predFlow = (FlowBlock) preds.nextElement();
            if (predFlow != null && predFlow != this
                && predFlow.addr >= start && predFlow.addr < end) {
                return false;
            }
        }

        checkConsistent();

        Jump jumps = (Jump) successors.remove(this);

        /* Update the in/out-Vectors now */
        updateInOut(this, jumps);

        StructuredBlock bodyBlock = block;

        /* If there is only one jump to the beginning and it is
         * the last jump (lastModified) and (there is a
         * do/while(0) block surrounding everything but the last
         * instruction, or the last instruction is a
         * increase/decrease statement), replace the do/while(0)
         * with a for(;;last_instr) resp. create a new one and
         * replace breaks to do/while with continue to for.  */

        boolean createdForBlock = false;

        if (jumps.next == null
            && jumps.prev == lastModified 
            && lastModified instanceof InstructionBlock
            && ((InstructionBlock)lastModified).getInstruction().isVoid()) {
            
            if (lastModified.outer instanceof SequentialBlock
                && lastModified.outer.getSubBlocks()[0] 
                instanceof LoopBlock) {
                
                LoopBlock lb = 
                    (LoopBlock) lastModified.outer.getSubBlocks()[0];
                if (lb.cond == lb.FALSE && lb.type == lb.DOWHILE) {
                    
                    /* The jump is directly following a
                     * do-while(false) block 
                     *
                     * Remove do/while, create a for(;;last_instr)
                     * and replace break to that block with
                     * continue to for.  
                     */
                    
                    lastModified.removeJump();
                    LoopBlock forBlock = 
                        new LoopBlock(LoopBlock.FOR, LoopBlock.TRUE);
                    forBlock.replace(bodyBlock);
                    forBlock.setBody(bodyBlock);
                    forBlock.incr = (InstructionBlock) lastModified;
                    forBlock.replaceBreakContinue(lb);

                    lb.bodyBlock.replace(lastModified.outer);
                    createdForBlock = true;
                }
                
            } 

            if (!createdForBlock 
                && (((InstructionBlock)
		     lastModified).getInstruction().getOperator()
		    instanceof CombineableOperator)) {
		
                /* The only jump is the jump of the last
                 * instruction lastModified, there is a big
                 * chance, that this is a for block, but we
                 * can't be sure until we have seen the condition.
                 * We will transform it to a for block, and take
                 * that back, when we get a non matching condition.
                 */
                
                lastModified.removeJump();
                LoopBlock forBlock = 
                    new LoopBlock(LoopBlock.POSSFOR, LoopBlock.TRUE);
                forBlock.replace(bodyBlock);
                forBlock.setBody(bodyBlock);
                forBlock.incr = (InstructionBlock) lastModified;
                
                createdForBlock = true;
            }
        }

        if (!createdForBlock) {
            /* Creating a for block didn't succeed; create a
             * while block instead.  */
            
            /* Try to eliminate as many jumps as possible.
             */
            jumps = resolveSomeJumps(jumps, this);
            
            LoopBlock whileBlock = 
                new LoopBlock(LoopBlock.WHILE, LoopBlock.TRUE);

            /* The block may have been changed above. */
            bodyBlock = block;            
            whileBlock.replace(bodyBlock);
            whileBlock.setBody(bodyBlock);
            
            /* if there are further jumps to this, replace every jump with a
             * continue to while block and return true.  
             */
            for (; jumps != null; jumps = jumps.next) {
                
                if (jumps.prev == lastModified)
                    /* handled later */
                    continue;
                
                StructuredBlock prevBlock = jumps.prev;

                int breaklevel = 0, continuelevel = 0;
                BreakableBlock breakToBlock = null;
                for (StructuredBlock surrounder = prevBlock.outer;
                     surrounder != whileBlock; 
                     surrounder = surrounder.outer) {
                    if (surrounder instanceof BreakableBlock) {
                        if (surrounder instanceof LoopBlock)
                            continuelevel++;
                        breaklevel++;
                        if (surrounder.getNextFlowBlock() == this) {
                            breakToBlock = (BreakableBlock) surrounder;
                            break;
                        }
                    }
                }
                prevBlock.removeJump();
                if (breakToBlock == null)
                    prevBlock.appendBlock
                        (new ContinueBlock(whileBlock, continuelevel > 0));
                else
                    prevBlock.appendBlock
                        (new BreakBlock(breakToBlock, breaklevel > 1));
            }
            
            /* Now remove the jump of lastModified if it points to this.
             */
            if (lastModified.jump.destination == this)
                lastModified.removeJump();
        }

        /* remove ourself from the predecessor list.
         */
        predecessors.removeElement(this);
        lastModified = block;
        doTransformations();
//         mergeCondition();

        /* T2 analysis succeeded */
        checkConsistent();

        return true;
    }


    /**
     * Do a T1 transformation with the end_of_method block.
     */
    public void mergeEndBlock() {
        checkConsistent();

        Jump allJumps = (Jump) successors.remove(END_OF_METHOD);
        if (allJumps == null)
            return;

        /* First remove all implicit jumps to the END_OF_METHOD block.
         */
        Jump jumps = null;
        for (; allJumps != null; ) {
            Jump jump = allJumps;
            allJumps = allJumps.next;

            if (jump.prev instanceof ReturnBlock) {
                /* This jump is implicit */
                jump.prev.removeJump();
                continue;
            }
            jump.next = jumps;
            jumps = jump;
        }
            
        /* Try to eliminate as many jumps as possible.
         */
        jumps = resolveSomeJumps(jumps, END_OF_METHOD);
            
    next_jump:
        for (; jumps != null; jumps = jumps.next) {

            StructuredBlock prevBlock = jumps.prev;
	    
            if (lastModified == prevBlock)
                /* handled later */
                continue;

            BreakableBlock breakToBlock = null;
            for (StructuredBlock surrounder = prevBlock.outer;
                 surrounder != null; surrounder = surrounder.outer) {
                if (surrounder instanceof BreakableBlock) {
                    if (surrounder.getNextFlowBlock() == END_OF_METHOD)
                        breakToBlock = (BreakableBlock) surrounder;

                    /* We don't want labeled breaks, because we can
                     * simply return.  */
                    break;
                }
            }
            prevBlock.removeJump();

            if (breakToBlock == null)
                /* The successor is the dummy return instruction, so
                 * replace the jump with a return.  
                 */
                prevBlock.appendBlock(new ReturnBlock());
            else
                prevBlock.appendBlock
                    (new BreakBlock(breakToBlock, false));
        }	    

        /* Now remove the jump of the lastModified if it points to
         * END_OF_METHOD.  
         */
        if (lastModified.jump.destination == END_OF_METHOD)
            lastModified.removeJump();

        doTransformations();
        /* transformation succeeded */
        checkConsistent();
    }

    public void doTransformations() {
        if (Decompiler.isFlowDebugging)
            Decompiler.err.println("before Transformation: "+this);

        while (lastModified instanceof SequentialBlock) {
            if (!lastModified.getSubBlocks()[0].doTransformations())
                lastModified = lastModified.getSubBlocks()[1];
        }
        while (lastModified.doTransformations())
            /* empty */;

        if (Decompiler.isFlowDebugging)
            Decompiler.err.println("after Transformation: "+this);
    }

    /**
     * Search for an apropriate successor.
     * @param prevSucc The successor, that was previously tried.
     * @param start The minimum address.
     * @param end   The maximum address + 1.
     * @return the successor with smallest address greater than prevSucc
     *  or null if there isn't any further successor at all.
     */
    FlowBlock getSuccessor(int start, int end) {
        /* search successor with smallest addr. */
        Enumeration keys = successors.keys();
        FlowBlock succ = null;
        while (keys.hasMoreElements()) {
            FlowBlock fb = (FlowBlock) keys.nextElement();
            if (fb.addr < start || fb.addr >= end || fb == this)
                continue;
            if (succ == null || fb.addr < succ.addr) {
                succ = fb;
            }
        }
        return succ;
    }

    /**
     * The main analyzation.  This calls doT1 and doT2 on apropriate
     * regions until the whole function is transformed to a single
     * block.  
     */
    public void analyze() {
        analyze(0, Integer.MAX_VALUE);
        mergeEndBlock();
    }

    /**
     * The main analyzation.  This calls doT1 and doT2 on apropriate
     * regions.  Only blocks whose address lies in the given address
     * range are considered.
     * @param start the start of the address range.
     * @param end the end of the address range.
     */
    public boolean analyze(int start, int end) {
        if (Decompiler.debugAnalyze)
            Decompiler.err.println("analyze("+start+", "+end+")");

        boolean changed = false;

        while (true) {
                
            if (lastModified instanceof SwitchBlock) {
                /* analyze the switch first.
                 */
                analyzeSwitch(start, end);
            } 


            if (doT2(start, end)) {

                if (Decompiler.isFlowDebugging)
                    Decompiler.err.println("after T2: "+this);

                if (Decompiler.debugAnalyze)
                    Decompiler.err.println("T2("+addr+","+getNextAddr()
					   +") succeeded");
                /* T2 transformation succeeded.  This may
                 * make another T1 analysis in the previous
                 * block possible.  
                 */
                if (addr != 0)
                    return true;
            }

            FlowBlock succ = getSuccessor(start, end);
            while (true) {
                if (succ == null) {
                    /* the Block has no successor where t1 is applicable.
                     * Finish this analyzation.
                     */
                    if (Decompiler.debugAnalyze)
                        Decompiler.err.println
                            ("No more successors applicable: "
                             + start + " - " + end + "; "
                             + addr + " - " + getNextAddr());
                    return changed;
                } else {
                    if ((nextByAddr == succ || succ.nextByAddr == this)
                        /* Only do T1 transformation if the blocks are
                         * adjacent.  */
                        && doT1(succ)) {
                        /* T1 transformation succeeded. */
                        changed = true;
                            
                        if (Decompiler.isFlowDebugging)
                            Decompiler.err.println("after T1: "+this);
                        break;
                    } 

                    /* Check if all predecessors of succ
                     * lie in range [start,end).  Otherwise
                     * we have no chance to combine succ
                     */
                    Enumeration enum = succ.predecessors.elements();
                    while (enum.hasMoreElements()) {
                        int predAddr = 
                            ((FlowBlock)enum.nextElement()).addr;
                        if (predAddr < start || predAddr >= end) {
                            if (Decompiler.debugAnalyze)
                                Decompiler.err.println
                                    ("breaking analyze("
                                     + start + ", " + end + "); "
                                     + addr + " - " + getNextAddr());
                            return changed;
                        }
                    }                            
                    /* analyze succ, the new region is the
                     * continuous region of
                     * [start,end) \cap \compl [addr, getNextAddr())
                     * where succ.addr lies in.
                     */
                    int newStart = (succ.addr > addr)
                        ? getNextAddr() : start;
                    int newEnd   = (succ.addr > addr)
                        ? end         : addr;
                    if (succ.analyze(newStart, newEnd))
                        break;
                }
                    
                /* Try the next successor.
                 */
                succ = getSuccessor(succ.addr+1, end);
            }
        }
    }
    
    /**
     * The switch analyzation.  This calls doSwitchT1 and doT2 on apropriate
     * regions.  Only blocks whose address lies in the given address
     * range are considered and it is taken care of, that the switch
     * is never leaved. <p>
     * The current flow block must contain the switch block as lastModified.
     * @param start the start of the address range.
     * @param end the end of the address range.
     */
    public boolean analyzeSwitch(int start, int end) {
        SwitchBlock switchBlock = (SwitchBlock) lastModified;
        boolean changed = false;

        int last = -1;
        FlowBlock lastFlow = null;
        for (int i=0; i < switchBlock.caseBlocks.length; i++) {
            if (switchBlock.caseBlocks[i].subBlock != null
                && switchBlock.caseBlocks[i].subBlock.jump != null) {
                FlowBlock nextFlow = switchBlock.caseBlocks[i].
                    subBlock.jump.destination;
                if (nextFlow.addr >= end)
                    break;
                else if (nextFlow.addr >= start) {
                    
		    /* First analyze the nextFlow block.  It may
                     * return early after a T2 trafo so call it
                     * until nothing more is possible.  
                     */
                    while (nextFlow.analyze(getNextAddr(), end))
                        changed = changed || true;
                    
                    if (nextFlow.addr != getNextAddr())
                        break;
                    
                    /* Check if nextFlow has only the previous case
                     * and this case as predecessor. Otherwise
                     * break the analysis.
                     */
                    if (nextFlow.predecessors.size() > 2 
                        || (nextFlow.predecessors.size() > 1
                            && (lastFlow == null
                                || !nextFlow.predecessors.contains(lastFlow)))
                        || ((Jump)successors.get(nextFlow)).next != null)
                        break;

                    checkConsistent();
                    
                    Jump jumps = (Jump) successors.remove(nextFlow);
                    /* note that this is the single caseBlock jump */

                    if (nextFlow.predecessors.size() == 2) {
                        Jump lastJumps = 
                            (Jump) lastFlow.successors.remove(nextFlow);

                        /* Do the in/out analysis with all jumps 
                         * Note that this won't update lastFlow.in, but
                         * this will not be used anymore.
                         */
                        jumps.next = lastJumps;
                        updateInOut(nextFlow, jumps);

                        lastJumps = 
                            lastFlow.resolveSomeJumps(lastJumps, nextFlow);
                        lastFlow.resolveRemaining(lastJumps);
			switchBlock.caseBlocks[last].isFallThrough = true;
                    } else
                        updateInOut(nextFlow, jumps);
                    
                    if (lastFlow != null) {
                        lastFlow.block.replace
                            (switchBlock.caseBlocks[last].subBlock);
                        mergeSuccessors(lastFlow);
                    }

                    /* We merge the blocks into the caseBlock later, but
                     * that doesn't affect consistency.
                     */

                    switchBlock.caseBlocks[i].subBlock.removeJump();
		    mergeAddr(nextFlow);

                    lastFlow = nextFlow;
                    last = i;

                    checkConsistent();
                    changed = true;
                }
            }
        }
        if (lastFlow != null) {
            lastFlow.block.replace
                (switchBlock.caseBlocks[last].subBlock);
            mergeSuccessors(lastFlow);
        }
        checkConsistent();
        return changed;
    }
    
    /**
     * Resolves the destinations of all jumps.  This will also create
     * the successors map.
     */
    public void resolveJumps(FlowBlock[] instr) {
        Vector allJumps = new Vector();
        block.fillSuccessors(allJumps);
        Enumeration enum = allJumps.elements();
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump != null && jump.destination == null) {
                if (jump.destAddr == -1) 
                    jump.destination = END_OF_METHOD;
                else if (jump.destAddr == instr.length)
		    // In this case, the jump must belong to dead code.
		    jump.destination = ILLEGAL;
		else {
                    jump.destination = instr[jump.destAddr];
		    if (jump.destination == null)
			throw new AssertError("Missing dest: "+jump.destAddr);
		}
                addSuccessor(jump);
            }
        }

	if (getNextAddr() < instr.length) {
	    nextByAddr = instr[getNextAddr()];
	    nextByAddr.prevByAddr = this;
	}
    }

    /**
     * Mark the flow block as first flow block in a method.
     */
    public void makeStartBlock() {
        predecessors.addElement(null);
    }

    public void removeSuccessor(Jump jump) {
        Jump destJumps = (Jump) successors.get(jump.destination);
        Jump prev = null;
        while (destJumps != jump && destJumps != null) {
            prev = destJumps;
            destJumps = destJumps.next;
        }
        if (destJumps == null)
            throw new AssertError(""+addr+": removing non existent jump: " + jump);
        if (prev != null)
            prev.next = destJumps.next;
        else {
            if (destJumps.next == null) {
                successors.remove(jump.destination);
		jump.destination.predecessors.removeElement(this);
            } else
                successors.put(jump.destination, destJumps.next);
        }
    }

    public void addSuccessor(Jump jump) {
        jump.next = (Jump) successors.get(jump.destination);
        if (jump.next == null)
            jump.destination.predecessors.addElement(this);
        
        successors.put(jump.destination, jump);
    }

    /** 
     * This is called after the analysis is completely done.  It
     * will remove all PUSH/stack_i expressions, (if the bytecode
     * is correct).
     * @return false if the bytecode isn't correct and stack mapping
     * didn't worked.
     */
    public final boolean mapStackToLocal() {
	try {
	    mapStackToLocal(VariableStack.EMPTY);
	    return true;
	} catch (RuntimeException ex) {
	    Decompiler.err.println("Can't resolve all PUSHes, "
				   +"this is probably illegal bytecode:");
	    ex.printStackTrace(Decompiler.err);
	    return false;
	}
    }

    /** 
     * This is called after the analysis is completely done.  It
     * will remove all PUSH/stack_i expressions, (if the bytecode
     * is correct).
     * @param initialStack the stackmap at begin of the flow block
     * @return false if the bytecode isn't correct and stack mapping
     * didn't worked.
     */
    public void mapStackToLocal(VariableStack initialStack) {
	if (stackMap != null) {
	    stackMap.merge(initialStack);
	} else
	    stackMap = initialStack;

	block.mapStackToLocal(initialStack);
	Enumeration enum = successors.elements();
	while (enum.hasMoreElements()) {
	    Jump jumps = (Jump) enum.nextElement();
	    VariableStack stack;
	    FlowBlock succ = jumps.destination;
	    stack = succ.stackMap;
	    for (/**/; jumps != null; jumps = jumps.next) {
		stack = VariableStack.merge(stack, jumps.stackMap);
	    }
	    if (succ.stackMap == null)
		succ.mapStackToLocal(stack);
	}
    }

    public void removePush() {
	if (stackMap == null) 
	    /* already done or mapping didn't succeed */
	    return;
	stackMap = null;
	block.removePush();
	Enumeration enum = successors.keys();
	while (enum.hasMoreElements()) {
	    FlowBlock succ = (FlowBlock)enum.nextElement();
	    succ.removePush();
	}
    }

    public void makeDeclaration(VariableSet param) {
	in.merge(param);
	in.subtract(param);
	block.propagateUsage();
	block.makeDeclaration(param);
    }

    /**
     * Mark this block and all successing blocks as reachable.  */
    public void markReachable() {
	if (reachable) {
	    // This block was already checked.  This prevents infinite
	    // recursion.
	    return;
	}
	reachable = true;
	Enumeration succs = successors.keys();
	while (succs.hasMoreElements())
	    ((FlowBlock) succs.nextElement()).markReachable();
    }

    /**
     * Removes dead code, i.e. merges the dead flow block with there 
     * prevByAddr and removing their code completely. <br>
     *
     * Why is it necessary?  Because jikes actually generates a lot
     * of dead code in try, catch and finally blocks.  This will break
     * a lot of other code, e.g. checking that a catch handler has no
     * entries or merging only adjacent blocks.
     *
     * @param flow The first flow block in this method.
     */
    public static void removeDeadCode(FlowBlock flow) {
	while (flow.nextByAddr != null) {
	    if (!flow.nextByAddr.reachable) {
		Enumeration succs = flow.nextByAddr.successors.keys();
		while (succs.hasMoreElements())
		    ((FlowBlock) succs.nextElement()).predecessors
			.removeElement(flow.nextByAddr);
		flow.mergeAddr(flow.nextByAddr);
	    } else
		flow = flow.nextByAddr;
	}
    }

    /**
     * Print the source code for this structured block.  This handles
     * everything that is unique for all structured blocks and calls
     * dumpInstruction afterwards.
     * @param writer The tabbed print writer, where we print to.
     */
    public void dumpSource(TabbedPrintWriter writer)
        throws java.io.IOException
    {
        if (predecessors.size() != 0) {
            writer.untab();
            writer.println(getLabel()+":");
            writer.tab();
        }

        if (Decompiler.debugInOut) {
            writer.println("in: "+in);
        }

        block.dumpSource(writer);

	if (nextByAddr != null)
	    nextByAddr.dumpSource(writer);
    }

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
            label = "flow_"+addr+"_"+(serialno++)+"_";
        return label;
    }

    /**
     * Returns the structured block, that this flow block contains.
     */
    public StructuredBlock getBlock() {
        return block;
    }

    public String toString() {
        try {
            java.io.StringWriter strw = new java.io.StringWriter();
            TabbedPrintWriter writer = new TabbedPrintWriter(strw);
            writer.println(super.toString());
            writer.tab();
            block.dumpSource(writer);
            return strw.toString();
        } catch (java.io.IOException ex) {
            return super.toString();
        }
    }
}