/* 
 * SimplifyExpression (c) 1998 Jochen Hoenicke
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

public class SimplifyExpression implements Transformation {
    public boolean transform(FlowBlock flow) {
//         try {
//             jode.TabbedPrintWriter writer = 
//                 new jode.TabbedPrintWriter(System.err, "    ");
//             System.out.println("Transforming: ");
//             flow.lastModified.dumpSource(writer);
        if (flow.lastModified instanceof InstructionContainer) {
            InstructionContainer ic = (InstructionContainer) flow.lastModified;
            ic.setInstruction(ic.getInstruction().simplify());
        }
//             System.out.println("Result: ");
//             flow.lastModified.dumpSource(writer);
//         } catch (java.io.IOException ex) {
//         }

        return false;
    }
}