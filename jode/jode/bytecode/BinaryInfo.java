/* BinaryInfo Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.bytecode;
import java.io.*;
import java.util.Enumeration;
import jode.util.SimpleDictionary;

/**
 *
 * @author Jochen Hoenicke
 */
public class BinaryInfo {
    public static final int HIERARCHY       = 0x01;
    public static final int FIELDS          = 0x02;
    public static final int METHODS         = 0x04;
    public static final int CONSTANTS       = 0x08;
    public static final int ALL_ATTRIBUTES  = 0x10;
    public static final int FULLINFO        = 0xff;

    private int status = 0;

    protected SimpleDictionary unknownAttributes;

    protected void skipAttributes(DataInputStream input) throws IOException {
        int count = input.readUnsignedShort();
        for (int i=0; i< count; i++) {
            input.readUnsignedShort();  // the name index
            long length = input.readInt();
	    while (length > 0) {
		long skipped = input.skip(length);
		if (skipped == 0)
		    throw new EOFException("Can't skip. EOF?");
		length -= skipped;
	    }
        }
    }

    protected int getKnownAttributeCount() {
	return 0;
    }

    protected void readAttribute(String name, int length,
				 ConstantPool constantPool,
				 DataInputStream input, 
				 int howMuch) throws IOException {
	byte[] data = new byte[length];
	input.readFully(data);
	unknownAttributes.put(name, data);
    }

    class ConstrainedInputStream extends FilterInputStream {
	int length;

	public ConstrainedInputStream(int attrLength, InputStream input) {
	    super(input);
	    length = attrLength;
	}

	public int read() throws IOException {
	    if (length > 0) {
		int data = super.read();
		length--;
		return data;
	    }
	    throw new EOFException();
	}

	public int read(byte[] b, int off, int len) throws IOException {
	    if (length < len) {
		len = length;
	    }
	    if (len == 0)
		return -1;
	    int count = super.read(b, off, len);
	    length -= count;
	    return count;
	}

	public int read(byte[] b) throws IOException {
	    return read(b, 0, b.length);
	}

	public long skip(long count) throws IOException {
	    if (length < count) {
		count = length;
	    }
	    count = super.skip(count);
	    length -= (int) count;
	    return count;
	}

	public void skipRemaining() throws IOException {
	    while (length > 0) {
		int skipped = (int) skip(length);
		if (skipped == 0)
		    throw new EOFException();
		length -= skipped;
	    }
	}
    }

    protected void readAttributes(ConstantPool constantPool,
                                  DataInputStream input, 
                                  int howMuch) throws IOException {
        if ((howMuch & ALL_ATTRIBUTES) != 0) {
            int count = input.readUnsignedShort();
	    unknownAttributes = new SimpleDictionary();
            for (int i=0; i< count; i++) {
		String attrName = 
		    constantPool.getUTF8(input.readUnsignedShort());
		final int attrLength = input.readInt();
		ConstrainedInputStream constrInput = 
		    new ConstrainedInputStream(attrLength, input);
		readAttribute(attrName, attrLength, 
			      constantPool, new DataInputStream(constrInput),
			      howMuch);
		constrInput.skipRemaining();
            }
	} else
            skipAttributes(input);
    }

    protected void prepareAttributes(GrowableConstantPool gcp) {
	Enumeration enum = unknownAttributes.keys();
	while (enum.hasMoreElements())
	    gcp.putUTF8((String) enum.nextElement());
    }

    protected void writeKnownAttributes
	(GrowableConstantPool constantPool, 
	 DataOutputStream output) throws IOException {
    }

    protected void writeAttributes
	(GrowableConstantPool constantPool, 
	 DataOutputStream output) throws IOException {
	int count = unknownAttributes.size() + getKnownAttributeCount();
	output.writeShort(count);
	writeKnownAttributes(constantPool, output);
	Enumeration keys = unknownAttributes.keys();
	Enumeration elts = unknownAttributes.elements();
	while (keys.hasMoreElements()) {
	    String name = (String) keys.nextElement();
	    byte[] data = (byte[]) elts.nextElement();
	    output.writeShort(constantPool.putUTF8(name));
	    output.writeInt(data.length);
	    output.write(data);
	}
    }

    public int getAttributeSize() {
	int size = 2; /* attribute count */
	Enumeration enum = unknownAttributes.elements();
	while (enum.hasMoreElements())
	    size += 2 + 4 + ((byte[]) enum.nextElement()).length;
	return size;
    }
    
    public byte[] findAttribute(String name) {
	return (byte[]) unknownAttributes.get(name);
    }

    public Enumeration getAttributes() {
	return unknownAttributes.elements();
    }

    public void setAttribute(String name, byte[] content) {
	unknownAttributes.put(name, content);
    }

    public byte[] removeAttribute(String name) {
	return (byte[]) unknownAttributes.remove(name);
    }

    public void removeAllAttributes() {
	unknownAttributes = new SimpleDictionary();
    }
}