/* ThisOperator Copyright (C) 1998-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */

package net.sf.jode.expr;
import net.sf.jode.type.Type;
import net.sf.jode.bytecode.ClassInfo;
import net.sf.jode.decompiler.ClassAnalyzer;
import net.sf.jode.decompiler.Scope;
import net.sf.jode.decompiler.TabbedPrintWriter;

public class ThisOperator extends NoArgOperator {
    boolean isInnerMost;
    ClassAnalyzer classAna;

    public ThisOperator(ClassAnalyzer classAna, boolean isInnerMost) {
        super(classAna.getType());
	this.classAna = classAna;
	this.isInnerMost = isInnerMost;
    }

    public ThisOperator(ClassAnalyzer classAna) {
	this(classAna, false);
    }

    public ClassInfo getClassInfo() {
	return classAna.getClazz();
    }

    public ClassAnalyzer getClassAnalyzer() {
	return classAna;
    }

    public int getPriority() {
        return 1000;
    }

    public String toString() {
        return classAna+".this";
    }

    public boolean opEquals(Operator o) {
        return (o instanceof ThisOperator &&
                ((ThisOperator) o).classAna == classAna);
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	if (!isInnerMost) {
	    writer.print(writer.getClassString(getClassInfo(), 
					       Scope.AMBIGUOUSNAME));
	    writer.print(".");
	}
	writer.print("this");
    }
}
