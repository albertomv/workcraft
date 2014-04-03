package org.workcraft.plugins.son.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.workcraft.dom.Node;
import org.workcraft.plugins.son.SONModel;
import org.workcraft.plugins.son.elements.Event;

public class BSONPathAlg extends ONPathAlg{

	private SONModel net;
	private RelationAlg alg;

	public BSONPathAlg(SONModel net){
		super(net);
		this.net = net;
		alg =new RelationAlg(net);
	}

	@Override
	public List<Node[]> createAdj(Collection<Node> nodes){

		List<Node[]> result = new ArrayList<Node[]>();

		for (Node n: nodes){
			for (Node next: net.getPostset(n))
				if(nodes.contains(next) && !net.getSONConnections(n, next).contains("BHVLINE")){
					Node[] adjoin = new Node[2];
					adjoin[0] = n;
					adjoin[1] = next;
					result.add(adjoin);

					for (String conType :  net.getSONConnectionTypes(n, next)){
						if(conType == "SYNCLINE"){
							Node[] reAdjoin = new Node[2];
							reAdjoin[0] = next;
							reAdjoin[1] = n;
							if(!result.contains(reAdjoin))
								result.add(reAdjoin);
					}
				}
			}
			if(n instanceof Event)
				result.addAll(alg.before((Event)n));
		}
		return result;
	}

	@Override
	public Collection<ArrayList<Node>> cycleTask (Collection<Node> nodes){

		this.clearAll();
		for(Node start : relation.getInitial(nodes))
			for(Node end : relation.getFinal(nodes))
				getAllPath(start, end, createAdj(nodes));

		 return cyclePathFilter(cycleResult);
	}

	private Collection<ArrayList<Node>> cyclePathFilter(Collection<ArrayList<Node>> result){
		List<ArrayList<Node>> delList = new ArrayList<ArrayList<Node>>();
		for(ArrayList<Node> cycle : result){
			int outputBhvLine = 0;
			int inputBhvLine = 0;
			if(!net.getSONConnectionTypes(cycle).contains("POLYLINE"))
				delList.add(cycle);
			for(Node n : cycle){
				if(net.getOutputSONConnections(n).contains("BHVLINE"))
					outputBhvLine ++;
				if(net.getInputSONConnections(n).contains("BHVLINE"))
					inputBhvLine ++;
			if(inputBhvLine==0 || outputBhvLine==0)
				delList.add(cycle);
			}
		}
		result.removeAll(delList);
		return result;
	}

}