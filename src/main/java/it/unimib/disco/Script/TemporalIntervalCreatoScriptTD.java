package it.unimib.disco.Script;

import it.unimib.disco.Evaluation.Evaluation;
import it.unimib.disco.FactExtractor.DateOccurrence;
import it.unimib.disco.Reasoning.Reasoning;
import it.unimib.disco.Selection.Selection;
import it.unimib.disco.TemporalIntervalCreator.MatrixCreator;
import it.unimib.disco.TemporalIntervalCreator.NormalizationSelection;
import it.unimib.disco.TemporalIntervalCreator.TemporalIntervalFactAssociator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.aksw.distributions.Fact;
import org.aksw.distributions.FactGrouping;
import org.apache.log4j.Logger;


public class TemporalIntervalCreatoScriptTD {

private static Logger logger = Logger.getLogger(TemporalIntervalCreatoScriptTD.class);
	
public HashMap<String,HashMap<String,ArrayList<Double>>> temporalFact(HashMap<String,ArrayList<String>> dateRepository,
		List<Fact> temporalDefactoFacts,List<String> goldstandard_facts,

		int normalizationType,int selection,int k, int x) throws FileNotFoundException{

		FileOutputStream fos,fos1;
		File directory = new File (".");
		if(selection==1){
			fos = new FileOutputStream(directory.getAbsolutePath()+"/output/matrix/"+"matrix_topK"+"-"+k+"-"+x+".csv");
			fos1 = new FileOutputStream(directory.getAbsolutePath()+"/output/interval/"+"evaluationWithIntervals_topK"+"-"+k+"-"+x+".csv");
			
		}
		else if(selection==2){
			fos = new FileOutputStream(directory.getAbsolutePath()+"/output/matrix/"+"matrix_proxyX"+"-"+k+"-"+x+".csv");
			fos1 = new FileOutputStream(directory.getAbsolutePath()+"/output/interval/"+"evaluationWithIntervals_proxyX"+"-"+k+"-"+x+".csv");
			
		}
		else{
			fos = new FileOutputStream(directory.getAbsolutePath()+"/output/matrix/"+"matrix_neighbor"+"-"+k+"-"+x+".csv");
			fos1 = new FileOutputStream(directory.getAbsolutePath()+"/output/interval/"+"evaluationWithIntervals_neighbor"+"-"+k+"-"+x+".csv");
			
		}
		

		PrintWriter pw = new PrintWriter(fos);
		PrintWriter pw1 = new PrintWriter(fos1);
		
	
		// date and fact extractor
		MatrixCreator dfe = new MatrixCreator();
		

		 HashMap<String, ArrayList<String>> repositoryDates =  dfe.fetchDatesTD(temporalDefactoFacts);
		logger.info("Retrieved all time points");
		
		/******************RIM**************/
		HashMap<String, DateOccurrence [][]> maximalRIM =  dfe.createMaximalRIM(repositoryDates);
		logger.info("Created maximal RIM");
		
				
		/******************Normalization **************/
		List<Fact> factNormalized= new NormalizationSelection().normalize(normalizationType,temporalDefactoFacts);
		
		/******************Matching **************/
		HashMap<String,HashMap<String,List<Fact>>> groupFacts = new FactGrouping().groupBySubjectObject(factNormalized);
		
		TemporalIntervalFactAssociator ta = new TemporalIntervalFactAssociator();
		HashMap<String,HashMap<String,HashSet<ArrayList<String>>>> sub_obj_interval = new HashMap<String,HashMap<String,HashSet<ArrayList<String>>>>() ;
		

		
		
		try {
			for (String uri: maximalRIM.keySet()){
				HashMap<String,HashSet<ArrayList<String>>> obj_interval= new HashMap<String,HashSet<ArrayList<String>>>();
				HashMap<String,List<Fact>> objbasedgroupfacts = groupFacts.get(uri);
						
				for (String obj: objbasedgroupfacts.keySet()){
					List<Fact> f = objbasedgroupfacts.get(obj);
							
					DateOccurrence [][] matrixManhattanDuration = ta.matrixYearsDuration(maximalRIM.get(uri));
							
					pw.println(uri+" "+obj);
					obj_interval.put(obj, ta.dcCalculator(normalizationType,f,matrixManhattanDuration,pw));
				
				}
				sub_obj_interval.put(uri, obj_interval);
			}
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
				pw.close();
			try {
				fos.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			  
		    logger.info("Selection function");
		    HashMap<String,HashMap<String,HashSet<ArrayList<String>>>> tempodefactoIntervalsUri = new HashMap<String,HashMap<String,HashSet<ArrayList<String>>>>();
		    for (String uri:sub_obj_interval.keySet()){
				
				tempodefactoIntervalsUri.put(uri,new Selection().selection(selection, x, k,sub_obj_interval.get(uri)));
					
			}
		    
		  //Concatenate intervals based on Allen's Algebra reasoning
		    logger.info("Reasoning function");
		    HashMap<String,HashMap<String,HashSet<ArrayList<String>>>> reasoningIntervalsUri = new HashMap<String,HashMap<String,HashSet<ArrayList<String>>>>();
		    
		    for (String uri:tempodefactoIntervalsUri.keySet()){
		    	HashMap<String,HashSet<ArrayList<String>>> reasoningIntervals = new HashMap<String,HashSet<ArrayList<String>>>();
		    	
		    	for(String obj:tempodefactoIntervalsUri.get(uri).keySet()){

		    		reasoningIntervals.put(obj, new Reasoning().concatenateIntervals(tempodefactoIntervalsUri.get(uri).get(obj)));
		    	}
		    	reasoningIntervalsUri.put(uri, reasoningIntervals);
		    }
		    
		    Evaluation ev = new Evaluation();
			HashMap<String,HashMap<String,ArrayList<Double>>> evaluationResults = new HashMap<String,HashMap<String,ArrayList<Double>>>();
			try {
				
				for (String uri:reasoningIntervalsUri.keySet()){
					HashMap<String,HashSet<ArrayList<String>>> timeintervals=reasoningIntervalsUri.get(uri);
					
					HashMap<String,ArrayList<Double>> localmetrics=ev.overlap(uri,timeintervals,goldstandard_facts,pw1);
					evaluationResults.put(uri,localmetrics);
		
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			pw1.close();
			try {
				fos1.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}


			return evaluationResults;
		}

}

