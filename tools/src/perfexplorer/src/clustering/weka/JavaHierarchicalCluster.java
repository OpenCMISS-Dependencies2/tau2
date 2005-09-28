/*
 * Created on Mar 16, 2005
 *
 */
package clustering.weka;

import clustering.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * This class solves a hierarchical clustering for a RawData object.
 * 
 * @author khuck
 *
 */
public class JavaHierarchicalCluster implements HierarchicalCluster {
	
	private DistanceMatrix distances = null;
	private LinkedHashSet remainingIndices = null;
	private DendrogramTree[] trees = null;

	public JavaHierarchicalCluster(DistanceMatrix distances) {
		this.distances = new DistanceMatrix(distances);
	}

	public DendrogramTree buildDendrogramTree() {
		int dimension = distances.getDimension();
		
		// create a set of the remaining indices
		remainingIndices = new LinkedHashSet(dimension);
		// create an initial array of leaf trees
		trees = new DendrogramTree[dimension];
		
		// initialize the HashSet and the tree array
		for (int i = 0 ; i < dimension ; i++) {
			remainingIndices.add(new Integer(i));
			trees[i] = new DendrogramTree(((-1)-i), 0);
		}
		
		DendrogramTree newTree = null;
		int newline = 0;
		while (remainingIndices.size() > 1) {
			boolean first = true;
			double min = 0.0;
			int[] location = {0,0}; 
			for (int i = 0 ; i < dimension ; i++) {
				// skip the ones we have merged already
				if (!remainingIndices.contains(new Integer(i)))
					continue;
				
				for (int j = 0 ; j < i ; j++) {
					// skip the ones we have merged already
					if (!remainingIndices.contains(new Integer(j)))
						continue;

					// if this is the first pass, save the first distance
					if (first) {
						first = false;
						min = distances.elementAt(i,j);
						location[0] = i;
						location[1] = j;
					} else {
						// find the two closest vectors
						if (min > distances.elementAt(i,j)) {
							min = distances.elementAt(i,j);
							location[0] = i;
							location[1] = j;
						} //if
					} //else
				} //for
			} //for
			
			//ok, we found the two closest.  Now what?
			// remove the second index from the hash set
			remainingIndices.remove(new Integer(location[0]));
			System.out.print(" " + location[0]);
			if (++newline % 20 == 0) System.out.println(" : " + newline / 20);
			
			// create a new tree node, with the left and right leaves
			newTree = new DendrogramTree(location[1], min);
			newTree.setLeftAndRight(trees[location[1]], trees[location[0]]);
			trees[location[1]] = newTree;
			
			// merge the two vectors into one in the distance matrix
			distances.mergeDistances(location[1], location[0]);
			//System.out.println(distances.toString());
			// lather, rinse, repeat
		}
		return newTree;
	}
	
	/**
	 * Testing method...
	 * @param args
	 */
	public static void main(String[] args) {
		int vectors = 10;
		int dimensions = 3;
		
		// generate some raw data
		List attrs = new ArrayList(vectors);
		attrs.add("x");
		attrs.add("y");
		attrs.add("z");
		RawDataInterface data = new WekaRawData("test", attrs, vectors, dimensions);
		for (int i = 0 ; i < vectors ; i++) {
			for (int j = 0 ; j < dimensions ; j++) {
				data.addValue(i, j, Math.random());
			}
		}
		
		// get the distances
		DistanceMatrix distances = new DistanceMatrix(vectors);
		distances.solveManhattanDistances(data);
		System.out.println("Got Distances...");
		
		// do the hierarchical clustering
		JavaHierarchicalCluster hclust = new JavaHierarchicalCluster(distances);
		DendrogramTree newTree = hclust.buildDendrogramTree();
		System.out.println("\n\n" + newTree.toString());
	}
}
