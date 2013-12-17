package it.unimib.disco.Script;

import it.unimib.disco.Evaluation.Evaluation;
import it.unimib.disco.Evaluation.QualityMeasure;
import it.unimib.disco.FactExtractor.DateOccurrence;
import it.unimib.disco.Matching.Matcher;
import it.unimib.disco.MatrixCreator.MatrixCreator;
import it.unimib.disco.Normalization.NormalizationSelection;
import it.unimib.disco.ReadFiles.FactGrouping;
import it.unimib.disco.Reasoning.Interval;
import it.unimib.disco.Reasoning.Reasoning;
import it.unimib.disco.Selection.Selection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.aksw.distributions.Fact;
import org.apache.log4j.Logger;

public class TemporalIntervalCreatoScript_dbp {

private static Logger logger = Logger.getLogger(TemporalIntervalCreatoScript_dbp.class);
	//HashMap<String,List<Features>> featuresUri
public HashMap<String,HashMap<String,QualityMeasure>> temporalFact(HashMap<String,HashMap<String,List<Fact>>> groupedFactBySubjectObject,List<Fact> temporalDefactoFacts,List<String> goldstandard_facts,
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
		
	
		/******************RIM**************/
		/*HashMap<String,ArrayList<String>> dateRepository=new HashMap<String,ArrayList<String>>();
		for ( String str: featuresUri.keySet()){
			List<Features> hm = featuresUri.get(str);
			HashSet<String> dateList = new HashSet<String>();
			for (Features y: hm){
				dateList.add(y.getTimepoint());
			}
			ArrayList<String> list = new ArrayList<String>(dateList);
			dateRepository.put(str, list);
		}*/
		
		HashMap<String, DateOccurrence [][]> maximalRIM =  new MatrixCreator().createMaximalRIM(new FactGrouping().createRIMvectors(groupedFactBySubjectObject));
		System.out.println(maximalRIM.size());
		logger.info("Created maximal RIM");
		
		/******************lexical RIM**************/
		//HashMap<String, DateOccurrence [][]> lexicalRIM =  new MatrixCreator().createLexicalRIM(featuresUri);
		//logger.info("Created lexical RIM");
		
		/******************Normalization **************/
		List<Fact> factNormalized= new NormalizationSelection().normalize(normalizationType,temporalDefactoFacts);
		
		/******************Matching **************/
		HashMap<String,HashMap<String,List<Fact>>> groupFacts = new FactGrouping().groupBySubjectObject(factNormalized);
		System.out.println(groupFacts.size());
		Matcher ta = new Matcher();
		HashMap<String,HashMap<String,List<Interval>>> sub_obj_interval = new HashMap<String,HashMap<String,List<Interval>>>() ;
		int count=0;
		try {
		for (String uri: maximalRIM.keySet()){
			HashMap<String,List<Interval>> obj_interval= new HashMap<String,List<Interval>>();
			
			HashMap<String,List<Fact>> objbasedgroupfacts = groupFacts.get(uri);
			if(objbasedgroupfacts!=null){
				count++;

			for (String obj: objbasedgroupfacts.keySet()){
				List<Fact> f = objbasedgroupfacts.get(obj);
						
				DateOccurrence [][] matrixManhattanDuration = ta.matrixYearsDuration(maximalRIM.get(uri));
						
				pw.println(uri+" "+obj);
				obj_interval.put(obj, ta.dcCalculator(normalizationType,f,matrixManhattanDuration,pw));	
			}
			sub_obj_interval.put(uri, obj_interval);
			}
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
		System.out.println(count);
		  
	    logger.info("Selection function");
	    HashMap<String,HashMap<String,List<Interval>>> tempodefactoIntervalsUri = new HashMap<String,HashMap<String,List<Interval>>>();
	    for (String uri:sub_obj_interval.keySet()){
	    	
	    	HashMap<String,List<Interval>> ls = new Selection().selection(selection, x, k,sub_obj_interval.get(uri));
	    	
			tempodefactoIntervalsUri.put(uri,ls);
				
		}
	    
	  //Concatenate intervals based on Allen's Algebra reasoning
	    logger.info("Reasoning function");
	    HashMap<String,HashMap<String,HashSet<Interval>>> reasoningIntervalsUri = new HashMap<String,HashMap<String,HashSet<Interval>>>();
	    
	    for (String uri:tempodefactoIntervalsUri.keySet()){
	    	HashMap<String,HashSet<Interval>> reasoningIntervals = new HashMap<String,HashSet<Interval>>();
	    	
	    	for(String obj:tempodefactoIntervalsUri.get(uri).keySet()){
	    	
	    		reasoningIntervals.put(obj, new Reasoning().concatenateIntervals(tempodefactoIntervalsUri.get(uri).get(obj)));
	    	}
	    	reasoningIntervalsUri.put(uri, reasoningIntervals);
	    }
	    
	    Evaluation ev = new Evaluation();
		HashMap<String,HashMap<String,QualityMeasure>> evaluationResults = new HashMap<String,HashMap<String,QualityMeasure>>();
		try {
			
			for (String uri:reasoningIntervalsUri.keySet()){
				HashMap<String,HashSet<Interval>> timeintervals=reasoningIntervalsUri.get(uri);
				
				HashMap<String,QualityMeasure> localmetrics=ev.overlap(uri,timeintervals,goldstandard_facts,pw1);
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

