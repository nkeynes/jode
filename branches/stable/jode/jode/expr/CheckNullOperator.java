/* CheckNullOperator Copyright (C) 1999 Jochen Hoenicke.
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

package jode.expr;
import jode.type.Type;
import jode.decompiler.LocalInfo;
import jode.decompiler.TabbedPrintWriter;
import jode.flow.VariableSet;

///#ifdef JDK12
///import java.util.Set;
///#else
import jode.util.Set;
///#endif

/**
 * This is a pseudo operator, which represents the check against null
 * that jikes and javac generates for inner classes:
 *
 * <pre>
 *   outer.new Inner()
 * </pre>
 * is translated by javac to
 * <pre>
 *   new Outer$Inner(outer ((void) DUP.getClass()));
 * </pre>
 * and by jikes to
 * <pre>
 *   new Outer$Inner(outer (DUP == null ? throw null));
 * </pre>
 */

public class CheckNullOperator extends Operator {
    LocalInfo local;

    public CheckNullOperator(Type type, LocalInfo li) {
        super(type, 0);
	local = li;
	initOperands(1);
    }

    public int getPriority() {
        return 200;
    }

    public void updateSubTypes() {
	local.setType(type);
	subExpressions[0].setType(Type.tSubType(type));
    }

    public void updateType() {
	Type newType = Type.tSuperType(subExpressions[0].getType())
	    .intersection(type);
	local.setType(newType);
	updateParentType(newType);
    }

    public void removeLocal() {
	local.remove();
    }

    public void fillInGenSet(VariableSet in, VariableSet gen) {
	if (gen != null)
	    gen.addElement(local);
	super.fillInGenSet(in, gen);
    }

    public void fillDeclarables(Set used) {
	used.add(local);
	super.fillDeclarables(used);
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	writer.print("("+local.getName()+" = ");
	subExpressions[0].dumpExpression(writer, 0);
	writer.print(").getClass() != null ? "+local.getName()+" : null");
    }

    public boolean opEquals(Operator o) {
	return o instanceof CheckNullOperator;
    }
}