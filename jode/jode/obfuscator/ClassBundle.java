/* 
 * ClassBundle (c) 1998 Jochen Hoenicke
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
package jode.obfuscator;
import jode.Obfuscator;
import jode.bytecode.ClassInfo;
import java.io.*;
import java.util.*;

public class ClassBundle {

    int preserveRule;
    PackageIdentifier basePackage;

    public ClassBundle() {
	basePackage = new PackageIdentifier(this, null, "", false);
	basePackage.setReachable();
	basePackage.setPreserved();
    }

    public String getTypeAlias(String typeSig) {
	StringBuffer newSig = new StringBuffer();
	int index = 0, nextindex;
	while ((nextindex = typeSig.indexOf('L', index)) != -1) {
	    newSig.append(typeSig.substring(index, nextindex+1));
	    index = typeSig.indexOf(';', nextindex);
	    String alias = basePackage.findAlias
		(typeSig.substring(nextindex+1, index).replace('/','.'));
	    newSig.append(alias.replace('.', '/'));
	}
	return newSig.append(typeSig.substring(index)).toString();
    }

    public Identifier getIdentifier(String name) {
	return basePackage.getIdentifier(name);
    }

    public void loadClasses(String packageOrClass) {
	basePackage.loadClasses(packageOrClass);
    }

    public void reachableIdentifier(String fqn, boolean isVirtual) {
	basePackage.reachableIdentifier(fqn, isVirtual);
    }

    public void setPreserved(int preserveRule, Vector fullqualifiednames) {
	this.preserveRule = preserveRule;
	
	Enumeration enum = fullqualifiednames.elements();
	while (enum.hasMoreElements()) {
	    basePackage.preserveIdentifier((String) enum.nextElement());
	}
    }

    public void strip() {
	basePackage.strip();
    }

    public void buildTable(int renameRule) {
	basePackage.buildTable(renameRule);
    }

    public void readTable(String filename) {
    }

    public void writeTable(String filename) {
	try {
	    PrintWriter out = new PrintWriter(new FileOutputStream(filename));
	    basePackage.writeTable(out);
	    out.close();
	} catch (java.io.IOException ex) {
	    Obfuscator.err.println("Can't write rename table "+filename);
	    ex.printStackTrace();
	}
    }

    public void storeClasses(String destination) {
	File directory = new File(destination);
	if (!directory.exists()) {
	    Obfuscator.err.println("Destination directory "
				   +directory.getPath()+" doesn't exists.");
	}
	basePackage.storeClasses(new File(destination));
    }
}

