/**
 * 
 */
package Collaborative_Content_Creation;



//Import Listing
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.RunnableScheduledFuture;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraDistance;

import net.sf.cglib.reflect.FastMethod;

import repast.simphony.context.Context;
import repast.simphony.context.space.graph.ContextJungNetwork;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunInfo;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.environment.ScheduleRegistry;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;
import repast.simphony.batch.BatchScheduleRunner;
import repast.simphony.jung.statistics.*;

/**
 * @author ersantasan
 *
 */
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class User {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private boolean isActiveUser;//true if User is a Zealot, false if Good Samaritan  
	private boolean hasGeneralInterest;/*true if User follows Administrator career
										 false if he follows Project Leader career*/
	private Article articleToEdit;
	private static List<Article> goodArticles= new ArrayList<Article>();
	private DijkstraDistance<User, RepastEdge<Object>> dijkDistAlg;

	double averageConnectionsOfNetwork=0;
	private boolean isDone=false;//for good samaritans to stop after first iteration
	private Random randGen=new Random();
	private static double exclusionNumber= -98765432.1;
	private static int NonAggrSamplingQueficient= 50;


	//parameters
	Parameters params = RunEnvironment.getInstance().getParameters();
	final int neighbourDimensions = (Integer)params.getValue("neighbourhood_dimensions");
	final double goodArticleMultiplier = (Double)params.getValue("good_article_multiplier");
	final int goodArticleConnectionCount= (Integer)params.getValue("good_article_connection_count");
	final int endAt= (Integer) params.getValue("run_length");//Run length parameter for batch mode
	final int runCount= (Integer) params.getValue("run_count");//Run count of whole simulation parameter for batch mode




	public User(ContinuousSpace<Object> space,  Grid<Object> grid, 
			boolean isActiveUser, boolean hasGeneralInterest){
		this.space = space;
		this.grid = grid;
		this.hasGeneralInterest=hasGeneralInterest;
		this.isActiveUser=isActiveUser;

		User.goodArticles.clear();
	}


	@ScheduledMethod(start=1, interval=1)
	public void step(){

		//create colNetwork in hosting context
		Context<Object> context= ContextUtils.getContext(this);		
		Network<Object> colNet =(Network<Object>)context.getProjection("collaboration_network");
		Network<Object> userNet =(Network<Object>)context.getProjection("user_network");
		Network<Object> articleNet =(Network<Object>)context.getProjection("article_network");

		if(!isDone){
			/*
			 * Neighbourhood Connection Algorithm
			 */	
			// get the grid location of this User
			GridPoint pt = grid.getLocation(this);

			// use the GridCellNgh class to create GridCells for
			// the surrounding neighbourhood
			if(pt!=null){//TODO Why NULL?
				GridCellNgh<Article> nghCreator = new GridCellNgh<Article>(grid, pt, Article.class,
						neighbourDimensions, neighbourDimensions);
				List<GridCell<Article>> gridCells = nghCreator.getNeighborhood(false);
				SimUtilities.shuffle(gridCells, RandomHelper.getUniform());

				//if an agent exist in the surrounding environment, add an edge with it.			
				for(GridCell<Article> cell : gridCells){
					if(cell.size() > 0 ){	
						List<Article> cellUsers = new ArrayList<Article>((Collection<Article>) cell.items());
						articleToEdit = cellUsers.get((RandomHelper.nextIntFromTo(0, cellUsers.size()-1)));
						if(context != null && colNet != null && cellUsers != null && articleToEdit != null){
							if(!isActiveUser){//Good Samaritan - one and only one connection
								if(colNet.getDegree(articleToEdit) <= 0//if neighbour is unconnected 
										&& colNet.getDegree(this) <= 0){//if our agent is unconnected) 
									colNet.addEdge(this, articleToEdit);
									this.isDone = true;//this good samaritan is no longer counted in operating agents
								}
							}
							else if (!hasGeneralInterest){//Project Leader zealot (active user),
								colNet.addEdge(this, articleToEdit);//connects neighbours in every step

								for(Object coopUser : colNet.getAdjacent(articleToEdit)){
									if(coopUser!=null && !userNet.containsEdge(userNet.getEdge(this, coopUser))){
										userNet.addEdge(this,coopUser);
									}
								}
								for(Object relatedArticle : colNet.getAdjacent(this)){
									if(relatedArticle!=null && !articleNet.containsEdge(userNet.getEdge(articleToEdit, relatedArticle))){
										articleNet.addEdge(articleToEdit, relatedArticle);
									}
								}
							}

							//For active agent connection algorithm we need to update good article array if found
							if(colNet.getDegree(articleToEdit) > (goodArticleMultiplier*colNet.getDegree()/colNet.size())
									&& colNet.getDegree(articleToEdit) > goodArticleConnectionCount && !articleToEdit.isGood){
								articleToEdit.isGood= true;
								goodArticles.add(articleToEdit);
							}
						}
						break;
					}
				}
			}

			/*
			 * Active Agent Connection Algorithm
			 */
			if(isActiveUser && hasGeneralInterest && goodArticles.size() > 0){//if in administrator career path
				articleToEdit=goodArticles.get(RandomHelper.nextIntFromTo(0, goodArticles.size()-1));
				colNet.addEdge(this,articleToEdit);//TODO reduce goodArticles by one?

				for(Object coopUser : colNet.getAdjacent(articleToEdit)){
					if(coopUser!=null && !userNet.containsEdge(userNet.getEdge(this, coopUser))){
						userNet.addEdge(this,coopUser);	
					}
				}
				for(Object relatedArticle : colNet.getAdjacent(this)){
					if(relatedArticle!=null && !articleNet.containsEdge(userNet.getEdge(articleToEdit, relatedArticle))){
						articleNet.addEdge(articleToEdit, relatedArticle);
					}
				}
				goodArticles.remove(0);
			}

			this.endRun();
		}
	}
	public int getAffiliationEdgeCountOfUser(){		
		Context<Object> context= ContextUtils.getContext(this);		
		Network<Object> colNet =(Network<Object>)context.getProjection("collaboration_network");
		return colNet.getDegree(this);
	}
	public int getUserEdgeCount(){		
		Context<Object> context= ContextUtils.getContext(this);		
		Network<Object> colNet =(Network<Object>)context.getProjection("user_network");
		return colNet.getDegree(this);
	}
	public double getUserNeighbourAverageEdgeCount(){	
		int tempInt=0; double sumOfNeigbourDegrees=0.0;
		Context<Object> context= ContextUtils.getContext(this);		
		Network<Object> colNet =(Network<Object>)context.getProjection("user_network");
		for (Object neighbourObject : colNet.getSuccessors(this)){
			if(neighbourObject instanceof User){
				sumOfNeigbourDegrees += colNet.getDegree(neighbourObject);
				tempInt++;
			}
		}
		double averageNeighbourDegree =0.0;
		if(tempInt !=0){
			averageNeighbourDegree = sumOfNeigbourDegrees/tempInt;
		}
		return averageNeighbourDegree;
	}
	public double getClusteringCoefficientOfUser(){
		if (this.hashCode() % NonAggrSamplingQueficient == 9){ //only calculate clustering coefficient for 1/20 of the agents 
			Context<Object> context= ContextUtils.getContext(this);		
			ContextJungNetwork jungNet = (ContextJungNetwork) context.getProjection("user_network");
			Map<Object, Double> clusterCoeffMap = repast.simphony.jung.statistics.RepastJungGraphStatistics.clusteringCoefficients(jungNet.getGraph());
			return clusterCoeffMap.get(this);
		}
		else {
			return exclusionNumber;
		}
	}
	public double getPathlengthOfUser(){
		if (this.hashCode() % NonAggrSamplingQueficient == 9){ //only calculate clustering coefficient for 1/20 of the agents 
			List<User> userList = new ArrayList<User>();
			Context<Object> context= ContextUtils.getContext(this);		
			Network<Object> colNet =(Network<Object>)context.getProjection("user_network");
			ContextJungNetwork jungNet = (ContextJungNetwork) context.getProjection("user_network");
			for(Object tempNode : colNet.getNodes()){
				if(tempNode instanceof User){
					userList.add((User)tempNode);
				}
			}
			dijkDistAlg= new DijkstraDistance<User, RepastEdge<Object>>(jungNet.getGraph());
			Number dist= dijkDistAlg.getDistance(this, userList.get(randGen.nextInt(userList.size())));
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
	@ScheduledMethod(start=1, interval=1)	
	public void endRun(){
		ISchedule schedule =RunState.getInstance().getScheduleRegistry().getModelSchedule();
		double tickCount =schedule.getTickCount();
		if(tickCount>=endAt){
			schedule.executeEndActions();
			RunEnvironment.getInstance().endRun();
		}
		RunInfo runInfo = RunState.getInstance().getRunInfo();
		if(runInfo.getRunNumber()>runCount){
		}
	}

}

