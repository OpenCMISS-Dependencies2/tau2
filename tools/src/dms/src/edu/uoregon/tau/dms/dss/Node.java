package edu.uoregon.tau.dms.dss;

import java.util.*;

/**
 * This class represents a Node.  It contains a set of Contexts and an ID.
 *  
 * <P>CVS $Id: Node.java,v 1.4 2005/01/10 20:09:08 amorris Exp $</P>
 * @author	Robert Bell, Alan Morris
 * @version	$Revision: 1.4 $
 * @see		DataSource
 * @see		Context
 */
public class Node implements Comparable {

    private int nodeID = -1;
    private Map contexts = new TreeMap();

    /**
     * Creates a Node with the given ID.  This constructor is not public because Nodes should 
     * only be created by DataSource.addNode(...)
     * @param 	nodeID		ID of this Node
     */
    Node(int nodeID) {
        this.nodeID = nodeID;
    }

    /**
     * Returns the Node's ID.
     * @return				the ID of this node
     */
    public int getNodeID() {
        return nodeID;
    }

    /**
     * Creates a Context with the given ID and adds it to the set of Contexts for this node.
     * If a Context with the given ID already exists, nothing is added.
     * @param 	contextID	ID for new Context
     * @return				Newly added (or existing) Context
     */
    public Context addContext(int contextID) {
        Object obj = contexts.get(new Integer(contextID));

        // return the Node if found
        if (obj != null)
            return (Context) obj;

        // otherwise, add it and return it
        Context context = new Context(this.nodeID, contextID);
        contexts.put(new Integer(contextID), context);
        return context;
    }

    /**
     * Returns an iterator over the Contexts.
     * @return				Iterator over this Node's Contexts 
     */
    public Iterator getContexts() {
        return contexts.values().iterator();
    }

    /**
     * Returns the context with the given id.  
     * @param 	contextID	ID of context		
     * @return				Requested Context, or null if not found
     */
    public Context getContext(int contextID) {
        return (Context) contexts.get(new Integer(contextID));
    }

    /**
     * Returns the number of contexts for this node. 
     * @return				Number of Contexts
     */
    public int getNumberOfContexts() {
        return contexts.size();
    }

    /**
     * Compares this Node to another Node or Integer.
     */
    public int compareTo(Object obj) {
        if (obj instanceof Integer)
            return nodeID - ((Integer) obj).intValue();
        else
            return nodeID - ((Node) obj).getNodeID();
    }
}
