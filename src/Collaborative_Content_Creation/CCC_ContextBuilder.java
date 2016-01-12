package Collaborative_Content_Creation;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.continuous.StickyBorders;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;

public class CCC_ContextBuilder implements ContextBuilder<Object> {

	@SuppressWarnings("rawtypes")
	@Override
	public Context build(Context<Object> context) {
		/*
		 * Parameter Setting
		 */
		Parameters params = RunEnvironment.getInstance().getParameters();
		int environmentHeight = (Integer)params.getValue("gridHeight");
		int environmentWidth = (Integer)params.getValue("gridWidth");
		int userCount = (Integer)params.getValue("user_count");
		int articleCount = (Integer)params.getValue("article_count");
		double activePercent = (double)params.getValue("active_user_percent");
		double generalInterestPercent = (double)params.getValue("general_interest_percent_of_active_users");
		
		/*
		 * Environment Construction 
		 */
		context.setId("Collaborative_Content_Creation");
		
		ContinuousSpaceFactory spaceFactory = 
				ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = 
				spaceFactory.createContinuousSpace("space", context, 
						new RandomCartesianAdder<Object>(), 
						new StickyBorders(), environmentWidth, environmentHeight);
		
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid", context, 
				new GridBuilderParameters<>(new repast.simphony.space.grid.StickyBorders(), 
											new SimpleGridAdder<>(), false, environmentWidth, environmentHeight));
		
		NetworkBuilder<Object> netBuilderCollaboration = new NetworkBuilder<Object>("collaboration_network", 
																	context, false);
		netBuilderCollaboration.buildNetwork();
		
		NetworkBuilder<Object> netBuilderUser = new NetworkBuilder<Object>("user_network", 
																			context, false);
		netBuilderUser.buildNetwork();
		
		NetworkBuilder<Object> netBuilderArticle = new NetworkBuilder<Object>("article_network", 
																			context, false);
		netBuilderArticle.buildNetwork();
		
		/*
		 * Agent Creation
		 */
		
		for(int i=0; i<userCount*(1-activePercent); i++){//Good Samaritan
			context.add(new User(space, grid, false, false));
		}
		for(int i=0; i<userCount*activePercent*(1-generalInterestPercent); i++){//Project Leader
			context.add(new User(space, grid, true, false));
		}
		for(int i=0; i<userCount*activePercent*generalInterestPercent; i++){//Administrator
			context.add(new User(space, grid, true, true));
		}
		for(int i=0; i<articleCount; i++){
			context.add(new Article(space, grid));
		}
			
		
		for ( Object obj : context ) {
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj, (int)pt.getX(), (int)pt.getY());
		}

		return context;
	}

}
