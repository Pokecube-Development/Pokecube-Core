/*****************************************************************************
 * JEP - Java Math Expression Parser 2.3.1
 * January 26 2006
 * (c) Copyright 2004, Nathan Funk and Richard Morris
 * See LICENSE.txt for license information.
 *****************************************************************************/
/* Generated By:JJTree: Do not edit this line. Node.java */

/*
 * All AST nodes must implement this interface. It provides basic
 * machinery for constructing the parent and child relationships
 * between nodes.
 */
package org.nfunk.jep;

public interface Node
{

    /** Accept the visitor. **/
    public Object jjtAccept(ParserVisitor visitor, Object data) throws ParseException;

    /**
     * This method tells the node to add its argument to the node's
     * list of children.
     */
    public void jjtAddChild(Node n, int i);

    /**
     * This method is called after all the child nodes have been
     * added.
     */
    public void jjtClose();

    /**
     * This method returns a child node. The children are numbered
     * from zero, left to right.
     */
    public Node jjtGetChild(int i);

    /** Return the number of children the node has. */
    public int jjtGetNumChildren();

    public Node jjtGetParent();

    /**
     * This method is called after the node has been made the current
     * node. It indicates that child nodes can now be added to it.
     */
    public void jjtOpen();

    /**
     * This pair of methods are used to inform the node of its
     * parent.
     */
    public void jjtSetParent(Node n);

    /**
     * Push the value of the node on the stack *
     * public void evaluate(Stack stack) throws ParseException;
     */
}
