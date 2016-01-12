/**
 * 
 */
package Collaborative_Content_Creation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraDistance;

import repast.simphony.context.Context;
import repast.simphony.context.space.graph.ContextJungNetwork;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;

/**
 * @author ersantasan
 *
 */
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class Article {
	private static final int NonAggrSamplingQueficient = 50;
	private static final double exclusionNumber = -98765432.1;
	private Random randGen=new Random();
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public boolean isGood;/*true if Article's connection is greater than a certain multiple 
							       of average connection count (degree) of the network.*/
	private DijkstraDistance<Article, RepastEdge<Object>> dijkDistAlg;
	


	public Article(ContinuousSpace<Object> space,  Grid<Object> grid){
		this.space = space;
		this.grid = grid;
		this.isGood= false;
	}
	public int getAffiliationEdgeCountOfArticle(){		
		Context<Object> context= ContextUtils.getContext(this);		
		Network<Object> colNet =(Network<Object>)context.getProjection("collaboration_network");
		return colNet.getDegree(this);
	}
	public int getArticleEdgeCount(){		
		Context<Object> context= ContextUtils.getContext(this);		
		Network<Object> artNet =(Network<Object>)context.getProjection("article_network");
		return artNet.getDegree(this);
	}
	public double getArticleNeighbourAverageEdgeCount(){	
		int tempInt=0, sumOfNeigbourDegrees=0;
		Context<Object> context= ContextUtils.getContext(this);		
		Network<Object> colNet =(Network<Object>)context.getProjection("article_network");
		for (Object neighbourObject : colNet.getSuccessors(this)){
			if(neighbourObject instanceof Article){
				sumOfNeigbourDegrees += colNet.getDegree(neighbourObject);
				tempInt++;
			}
		}
		double averageNeighbourDegree =0;
		if(tempInt !=0){
			averageNeighbourDegree = sumOfNeigbourDegrees/tempInt;
		}
		return averageNeighbourDegree;
	}
	public double getClusteringCoefficientOfArticle(){
		if (this.hashCode() % NonAggrSamplingQueficient == 9){ //only calculate clustering coefficient for 1/20 of the agents 
			Context<Object> context= ContextUtils.getContext(this);		
			ContextJungNetwork jungNet = (ContextJungNetwork) context.getProjection("article_network");
			Map<Object, Double> clusterCoeffMap = repast.simphony.jung.statistics.RepastJungGraphStatistics.clusteringCoefficients(jungNet.getGraph());
			return clusterCoeffMap.get(this);
		}
		else {
			return exclusionNumber;
		}
	}
	public double getPathlengthOfArticle(){
		if (this.hashCode() % NonAggrSamplingQueficient == 9){ //only calculate clustering coefficient for 1/20 of the agents 
			List<Article> ArticleList = new ArrayList<Article>();
			Context<Object> context= ContextUtils.getContext(this);		
			Network<Object> colNet =(Network<Object>)context.getProjection("article_network");
			ContextJungNetwork jungNet = (ContextJungNetwork) context.getProjection("article_network");
			for(Object tempNode : colNet.getNodes()){
				if(tempNode instanceof Article){
					ArticleList.add((Article)tempNode);
				}
			}
			dijkDistAlg= new DijkstraDistance<Article, RepastEdge<Object>>(jungNet.getGraph());
			Number dist= dijkDistAlg.getDistance(this, ArticleList.get(randGen.nextInt(ArticleList.size())));
			if (dist!=null){
				return dist.doubleValue();
			}
			else{
				return exclusionNumber;
			}
		}
		else {
			return exclusionNumber;
		}
		
	}
}