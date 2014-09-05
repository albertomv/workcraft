package org.workcraft.plugins.son.verify;

import java.util.ArrayList;
import java.util.Collection;

import org.workcraft.dom.Node;
import org.workcraft.plugins.son.ONGroup;
import org.workcraft.plugins.son.SONModel;
import org.workcraft.plugins.son.algorithm.BSONAlg;
import org.workcraft.plugins.son.algorithm.BSONPathAlg;
import org.workcraft.plugins.son.algorithm.CSONPathAlg;
import org.workcraft.plugins.son.algorithm.PathAlgorithm;
import org.workcraft.plugins.son.algorithm.RelationAlgorithm;
import org.workcraft.plugins.son.algorithm.TSONAlg;

abstract class AbstractStructuralVerification implements StructuralVerification{

	private SONModel net;

	private RelationAlgorithm relationAlg;
	private CSONPathAlg csonPathAlg;
	private BSONAlg bsonAlg;
	private BSONPathAlg bsonPathAlg;
	private PathAlgorithm pathAlg;
	private TSONAlg tsonAlg;

	public AbstractStructuralVerification(SONModel net){
		this.net = net;
		relationAlg = new RelationAlgorithm(net);
		csonPathAlg = new CSONPathAlg(net);
		bsonAlg = new BSONAlg(net);
		bsonPathAlg = new BSONPathAlg(net);
		pathAlg = new PathAlgorithm(net);
		tsonAlg = new TSONAlg(net);

	}

	public abstract void task(Collection<ONGroup> groups);

	public Collection<String> getRelationErrorsSetReferences(Collection<Node> set){
		Collection<String> result = new ArrayList<String>();
		for(Node node : set)
			result.add(net.getNodeReference(node));
		return result;
	}

	public Collection<String> getGroupErrorsSetReferences(Collection<ONGroup> set){
		Collection<String> result = new ArrayList<String>();
		for(ONGroup node : set)
			result.add(net.getNodeReference(node));
		return result;
	}

	public Collection<ArrayList<String>> getcycleErrorsSetReferences(Collection<ArrayList<Node>> set){
		Collection<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		for(ArrayList<Node> path : set){
			ArrayList<String> sPath = new ArrayList<String>();
			for(Node node : path){
				sPath.add(net.getNodeReference(node));
				result.add(sPath);
			}
		}
		return result;
	}

	public RelationAlgorithm getRelationAlg(){
		return this.relationAlg;
	}

	public BSONAlg getBSONAlg(){
		return this.bsonAlg;
	}

	public BSONPathAlg getBSONPathAlg(){
		return bsonPathAlg;
	}

	public CSONPathAlg getCSONPathAlg(){
		return csonPathAlg;
	}

	public PathAlgorithm getPathAlg(){
		return pathAlg;
	}

	public TSONAlg getTSONAlg(){
		return tsonAlg;
	}
}
