// This interface is taken from the Classpath project.  
// Please note the different copyright holder!  
// The changes I did is this comment, the package line, some
// imports from java.util and some minor jdk12 -> jdk11 fixes.
// -- Jochen Hoenicke <jochen@gnu.org>

/////////////////////////////////////////////////////////////////////////////
// Collection.java -- Interface that represents a collection of objects
//
// Copyright (c) 1998 by Stuart Ballard (stuart.ballard@mcmail.com)
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU Library General Public License as published
// by the Free Software Foundation, version 2. (see COPYING.LIB)
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Library General Public License for more details.
//
// You should have received a copy of the GNU Library General Public License
// along with this program; if not, write to the Free Software Foundation
// Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
/////////////////////////////////////////////////////////////////////////////

// TO DO:
// ~ Maybe some more @see clauses would be helpful.

package jode.util;

/**
 * Interface that represents a collection of objects. This interface is the
 * root of the collection hierarchy, and does not provide any guarantees about
 * the order of its elements or whether or not duplicate elements are
 * permitted.
 * <p>
 * All methods of this interface that are defined to modify the collection are
 * defined as <dfn>optional</dfn>. An optional operation may throw an
 * UnsupportedOperationException if the data backing this collection does not
 * support such a modification. This may mean that the data structure is
 * immutable, or that it is read-only but may change ("unmodifiable"), or
 * that it is modifiable but of fixed size (such as an array), or any number
 * of other combinations.
 * <p>
 * A class that wishes to implement this interface should consider subclassing
 * AbstractCollection, which provides basic implementations of most of the
 * methods of this interface. Classes that are prepared to make guarantees
 * about ordering or about absence of duplicate elements should consider
 * implementing List or Set respectively, both of which are subinterfaces of
 * Collection.
 * <p>
 * A general-purpose implementation of the Collection interface should in most
 * cases provide at least two constructors: One which takes no arguments and
 * creates an empty collection, and one which takes a Collection as an argument
 * and returns a collection containing the same elements (that is, creates a
 * copy of the argument using its own implementation).
 *
 * @see java.util.List
 * @see java.util.Set
 * @see java.util.AbstractCollection
 */
public interface Collection {

  /**
   * Add an element to this collection.
   *
   * @param o the object to add.
   * @returns true if the collection was modified as a result of this action.
   * @exception UnsupportedOperationException if this collection does not
   *   support the add operation.
   * @exception ClassCastException if o cannot be added to this collection due
   *   to its type.
   * @exception IllegalArgumentException if o cannot be added to this
   *   collection for some other reason.
   */
  boolean add(Object o);

  /**
   * Add the contents of a given collection to this collection.
   *
   * @param c the collection to add.
   * @returns true if the collection was modified as a result of this action.
   * @exception UnsupportedOperationException if this collection does not
   *   support the addAll operation.
   * @exception ClassCastException if some element of c cannot be added to this
   *   collection due to its type.
   * @exception IllegalArgumentException if some element of c cannot be added
   *   to this collection for some other reason.
   */
  boolean addAll(Collection c);

  /**
   * Clear the collection, such that a subsequent call to isEmpty() would
   * return true.
   *
   * @exception UnsupportedOperationException if this collection does not
   *   support the clear operation.
   */
  void clear();

  /**
   * Test whether this collection contains a given object as one of its
   * elements.
   *
   * @param o the element to look for.
   * @returns true if this collection contains at least one element e such that
   *   <code>o == null ? e == null : o.equals(e)</code>.
   */
  boolean contains(Object o);

  /**
   * Test whether this collection contains every element in a given collection.
   *
   * @param c the collection to test for.
   * @returns true if for every element o in c, contains(o) would return true.
   */
  boolean containsAll(Collection c);

  /**
   * Test whether this collection is equal to some object. The Collection
   * interface does not explicitly require any behaviour from this method, and
   * it may be left to the default implementation provided by Object. The Set
   * and List interfaces do, however, require specific behaviour from this
   * method.
   * <p>
   * If an implementation of Collection, which is not also an implementation of
   * Set or List, should choose to implement this method, it should take care
   * to obey the contract of the equals method of Object. In particular, care
   * should be taken to return false when o is a Set or a List, in order to
   * preserve the symmetry of the relation.
   *
   * @param o the object to compare to this collection.
   * @returns true if the o is equal to this collection.
   */
  boolean equals(Object o);

  /**
   * Obtain a hash code for this collection. The Collection interface does not
   * explicitly require any behaviour from this method, and it may be left to
   * the default implementation provided by Object. The Set and List interfaces
   * do, however, require specific behaviour from this method.
   * <p>
   * If an implementation of Collection, which is not also an implementation of
   * Set or List, should choose to implement this method, it should take care
   * to obey the contract of the hashCode method of Object. Note that this
   * method renders it impossible to correctly implement both Set and List, as
   * the required implementations are mutually exclusive.
   *
   * @returns a hash code for this collection.
   */
  int hashCode();

  /**
   * Test whether this collection is empty, that is, if size() == 0.
   *
   * @returns true if this collection contains no elements.
   */
  boolean isEmpty();

  /**
   * Obtain an Iterator over this collection.
   *
   * @returns an Iterator over the elements of this collection, in any order.
   */
  Iterator iterator();

  /**
   * Remove a single occurrence of an object from this collection. That is,
   * remove an element e, if one exists, such that <code>o == null ? e == null
   *   : o.equals(e)</code>.
   *
   * @param o the object to remove.
   * @returns true if the collection changed as a result of this call, that is,
   *   if the collection contained at least one occurrence of o.
   * @exception UnsupportedOperationException if this collection does not
   *   support the remove operation.
   */
  boolean remove(Object o);

  /**
   * Remove all elements of a given collection from this collection. That is,
   * remove every element e such that c.contains(e).
   *
   * @returns true if this collection was modified as a result of this call.
   * @exception UnsupportedOperationException if this collection does not
   *   support the removeAll operation.
   */
  boolean removeAll(Collection c);

  /**
   * Remove all elements of this collection that are not contained in a given
   * collection. That is, remove every element e such that !c.contains(e).
   *
   * @returns true if this collection was modified as a result of this call.
   * @exception UnsupportedOperationException if this collection does not
   *   support the retainAll operation.
   */
  boolean retainAll(Collection c);

  /**
   * Get the number of elements in this collection.
   *
   * @returns the number of elements in the collection.
   */
  int size();

  /**
   * Copy the current contents of this collection into an array.
   *
   * @returns an array of type Object[] and length equal to the size of this
   *   collection, containing the elements currently in this collection, in
   *   any order.
   */
  Object[] toArray();

  /**
   * Copy the current contents of this collection into an array. If the array
   * passed as an argument has length less than the size of this collection, an
   * array of the same run-time type as a, and length equal to the size of this
   * collection, is allocated using Reflection. Otherwise, a itself is used.
   * The elements of this collection are copied into it, and if there is space
   * in the array, the following element is set to null. The resultant array is
   * returned.
   * Note: The fact that the following element is set to null is only useful
   * if it is known that this collection does not contain any null elements.
   *
   * @param a the array to copy this collection into.
   * @returns an array containing the elements currently in this collection, in
   *   any order.
   * @exception ArrayStoreException if the type of any element of the
   *   collection is not a subtype of the element type of a.
   */
  Object[] toArray(Object[] a);
}