/* 
 * NoArgOperator (c) 1998 Jochen Hoenicke
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

package jode.expr;
import jode.Type;
import jode.AssertError;

public abstract class NoArgOperator extends Operator {

    public NoArgOperator(Type type, int operator) {
        super(type, operator);
    }

    public NoArgOperator(Type type) {
        this(type, 0);
    }

    public int getOperandCount() {
        return 0;
    }

    public int getOperandPriority(int i) {
        throw new AssertError("This operator has no operands");
    }

    public Type getOperandType(int i) {
        throw new AssertError("This operator has no operands");
    }

    public void setOperandType(Type[] types) {
        throw new AssertError("This operator has no operands");
    }
}
