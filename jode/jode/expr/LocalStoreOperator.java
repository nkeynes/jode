/* LocalStoreOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

public class LocalStoreOperator extends LValueExpression 
    implements LocalVarOperator {
    LocalInfo local;

    public LocalStoreOperator(Type lvalueType, LocalInfo local) {
        super(lvalueType);
        this.local = local;
        local.setOperator(this);
	initOperands(0);
    }

    public boolean isRead() {
        return parent != null && parent.getOperatorIndex() != ASSIGN_OP;
    }

    public boolean isWrite() {
        return true;
    }

    public void updateSubTypes() {
        local.setType(type);
    }

    public void updateType() {
	updateParentType(local.getType());
    }

    public LocalInfo getLocalInfo() {
	return local.getLocalInfo();
    }

    public boolean matches(Operator loadop) {
        return loadop instanceof LocalLoadOperator && 
            ((LocalLoadOperator)loadop).getLocalInfo().getSlot()
            == local.getSlot();
    }

    public int getPriority() {
        return 1000;
    }

    public void dumpExpression(TabbedPrintWriter writer) {
	writer.print(local.getName());
    }
}

