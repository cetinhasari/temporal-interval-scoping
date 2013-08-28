package it.unimib.disco.Optimization;

import it.unimib.disco.ReadFiles.ReadFiles;
import it.unimib.disco.TemporalIntervalCreator.TemporalIntervalCreator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.aksw.distributions.Fact;
import org.apache.log4j.Logger;
import org.opt4j.core.Objective;
import org.opt4j.core.Objective.Sign;
import org.opt4j.core.Objectives;
import org.opt4j.core.genotype.SelectGenotype;


public class RunOptimizer {
	private static Logger logger = Logger.getLogger(TemporalIntervalCreator.class);
	
	public static void main (String args []) throws FileNotFoundException{
		if (args.length < 1) {
			System.out.println("Use: java TemporalIntervalCreator <Resource list file> <temporal defacto output> <yago's gold standard>");
			System.out.println("Example: java TemporalIntervalCreator /temporalIntervalCreator_ResourceList_in2.txt /sortbyplayer-labels-with-space_out_lionel.csv /goldStandard_30_entities.csv");
		} else {
			
	
			// Resource URI extraction
			List<String> resURIs = ReadFiles.getURIs(new File(args[0]));
			HashMap<String,ArrayList<String>> dateRepository=new HashMap<String,ArrayList<String>>();
			dateRepository=new ReadFiles().readCSVFile(resURIs);
			
			logger.info("DBpedia resources list file parsed");

			
			// Read temporalDefacto facts
			List<String> temporalDefactoFacts = ReadFiles.getURIs(new File(args[1]));
			List<Fact> l = new ArrayList<Fact>();
			l = new ReadFiles().creatListOfFacts(temporalDefactoFacts);
			logger.info("TemporalDefacto facts parsed");
			
			
			//Read gold standard facts
			List<String> goldstandard_facts = ReadFiles.getURIs(new File(args[2]));
			logger.info("Yago facts parsed");
		
			Objective objective = new Objective ("maximize", Sign.MAX);
			
			
			
			ConfigurationEvaluator e = new ConfigurationEvaluator(dateRepository, l, goldstandard_facts);
			HashMap<SelectGenotype<Configuration>,Objectives> collection = new HashMap<SelectGenotype<Configuration>,Objectives >();
		
			for(int i=0;i<50;i++){
				SelectGenotype<Configuration> genotype=new ConfigurationCreator().create();
				
				Objectives obj = e.evaluate(new ConfigurationDecoder().decode(genotype));
				collection.put(genotype,obj);
					
			}
		
			HashMap<SelectGenotype<Configuration>,Objectives> collection_max = new HashMap<SelectGenotype<Configuration>,Objectives >();
			Objectives obj_max = new Objectives();
			obj_max.add(objective,0d);
	        
			for (SelectGenotype<Configuration> g: collection.keySet()){
				//System.out.println(g+ " o "+collection.get(g));
				if(collection.get(g).dominates(obj_max)){
					collection_max.clear();
					obj_max.add(objective, collection.get(g).get(objective));
					collection_max.put(g, obj_max);	
				}
				else{}
			}
			for (SelectGenotype<Configuration> g:collection_max.keySet())
			System.out.println(g+" "+collection_max.get(g));
			
			
		}
	}

}
