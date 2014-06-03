package analysis.user_place_recognizer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gps.utils.LatLonPoint;

import utils.Config;
import utils.CopyAndSerializationUtils;
import utils.FileUtils;
import utils.FilterAndCounterUtils;
import utils.Logger;
import analysis.PLSEvent;
import analysis.user_place_recognizer.clustering.AgglomerativeClusterer;
import analysis.user_place_recognizer.weight_functions.WeightFunction;
import analysis.user_place_recognizer.weight_functions.WeightOnDay;
import analysis.user_place_recognizer.weight_functions.WeightOnDiversity;
import analysis.user_place_recognizer.weight_functions.WeightOnTime;
import analysis.user_place_recognizer.weight_functions.Weights;
import dataset.DataFactory;
import dataset.EventFilesFinderI;
import dataset.file.UsersCSVCreator;

public class PlaceRecognizer {
	
	public static boolean VERBOSE = true;
	
	public static List<LatLonPoint> analyze(String username, String kind_of_place, List<PLSEvent> events, 
			                   double alpha, double beta, double delta, double rwf) {
		
		
		List<PLSEvent> workingset = CopyAndSerializationUtils.clone(events);
				
				
		Logger.logln("Processing "+username.substring(0,5)+" "+kind_of_place);
		
		double[][] weights = Weights.get(kind_of_place);
		
		List<PLSEvent> refEvents = Thresholding.buildReferenceTower(workingset,weights);
		workingset.addAll(refEvents);
		
		
		String tperiod = events.get(0).getTimeStamp()+"-"+events.get(events.size()-1).getTimeStamp();
		
		Map<Integer, Cluster> clusters = null;
		
		File dir = FileUtils.createDir("BASE/PlaceRecognizer/Clusters");
		File f = new File(dir+"/"+username+"-"+kind_of_place+"-"+tperiod+"-"+delta+".ser");
		if(f.exists()) 
			clusters = (Map<Integer, Cluster>)CopyAndSerializationUtils.restore(f);
		else {
			clusters = new AgglomerativeClusterer(3,weights,delta).buildCluster(workingset);
			CopyAndSerializationUtils.save(f, clusters);
		}
		
		// rename the reference cluster to -1
		int found = -1;
		for(int k : clusters.keySet()) {
			Cluster c = clusters.get(k);
			if(c.getEvents().get(0).getCellac().equals(Thresholding.REF_NETWORK_CELLAC)) {
				found = k;
				break;
			}	
		}
		if(found != -1) clusters.put(-1, clusters.remove(found));
		
		
		WeightFunction[] wfunctions = new WeightFunction[]{
				new WeightOnTime(1.0,weights),
				new WeightOnDay(alpha),
				new WeightOnDiversity(beta,weights)
		};
		
		
		for(Cluster c: clusters.values()) 
		for(WeightFunction wf : wfunctions) 
			wf.weight(c);
		
		
		double threshold = Double.MAX_VALUE;
		if(clusters.get(-1) != null) 
			threshold = Thresholding.weight2Threshold(kind_of_place, FilterAndCounterUtils.getNumDays(workingset), clusters.get(-1), rwf);
		
		
		List<LatLonPoint> placemarks = new ArrayList<LatLonPoint>();
		for(int k : clusters.keySet()) {
			if(k==-1) continue;
			Cluster c = clusters.get(k);
			if(c.totWeight() > threshold) {
				LatLonPoint p = c.getCenter(weights);
				placemarks.add(p);
			}
		}
		 
		if(VERBOSE) PlaceRecognizerLogger.log(username, kind_of_place, clusters);
		PlaceRecognizerLogger.logkml(kind_of_place, clusters, placemarks);
		PlaceRecognizerLogger.logcsv(username,kind_of_place,placemarks);
		
		return placemarks;
	}
	
	
	private static String[] KIND_OF_PLACES = new String[]{"HOME","WORK","SATURDAY_NIGHT","SUNDAY"};
	private static final SimpleDateFormat F = new SimpleDateFormat("yyyy-MM-dd-hh");
	public Map<String, List<LatLonPoint>> runSingle(String sday, String eday, String user, double lon1, double lat1, double lon2, double lat2) {
		
		System.out.println("****** PARAMS");
		System.out.println(sday+","+eday+","+user+","+lon1+","+lat1+","+lon2+","+lat2);
		System.out.println("****** PARAMS");
		
		Map<String, List<LatLonPoint>> results = null;
		try {
			EventFilesFinderI eff = DataFactory.getEventFilesFinder();
			String dir = eff.find(sday,"12",eday,"12",lon1,lat1,lon2,lat2);
			if(dir == null) return null;
			
			Config.getInstance().pls_folder = FileUtils.getFile("DATASET/PLS/file_pls/"+dir).toString(); 
			Config.getInstance().pls_start_time.setTime(F.parse(sday+"-0"));
			Config.getInstance().pls_end_time.setTime(F.parse(eday+"-23"));
			List<PLSEvent> events = UsersCSVCreator.process(user).getEvents(); 
			results = new HashMap<String, List<LatLonPoint>>();
			PlaceRecognizerLogger.openKMLFile("G:/CODE/Telecom/web/kml/"+user+".kml");
			for(String kind_of_place:KIND_OF_PLACES)
				results.put(kind_of_place, analyze(user,kind_of_place,events,0.25,0.25,2000,0.6));
			PlaceRecognizerLogger.closeKMLFile();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return results;
	}
	
	
	
	
	
	public static void main(String[] args) throws Exception {
		/*
		String dir = "file_pls_piem_users_above_2000";
		String in_dir = "BASE/UsersCSVCreator/"+dir;
		String out_dir = "BASE/PlaceRecognizer/"+dir;
		File d = new File(out_dir);
		if(!d.exists()) d.mkdirs();
		
		PlaceRecognizerLogger.openTotalCSVFile(out_dir+"/results.csv");
		PlaceRecognizerLogger.openKMLFile(out_dir+"/results.kml");
		File[] files = new File(in_dir).listFiles();
		
		Map<String, List<LatLonPoint>> allResults = new HashMap<String, List<LatLonPoint>>();
		
		for(int i=0; i<files.length; i++){
			File f = files[i];	
			if(!f.isFile()) continue;
			String filename = f.getName();
			String username = filename.substring(0, filename.indexOf(".csv"));
			List<PlsEvent> events = PlsEvent.readEvents(f);
			PlaceRecognizerLogger.openUserFolderKML(username);
			
			for(String kind_of_place:KIND_OF_PLACES)
				allResults.put(username+"_"+kind_of_place, analyze(username,kind_of_place,events,0.25,0.25,2000,0.6));
				
			PlaceRecognizerLogger.closeUserFolderKML();
		}
		PlaceRecognizerLogger.closeKMLFile();
		PlaceRecognizerLogger.closeTotalCSVFile();
		
		//PlaceRecognizerEvaluator rs = new PlaceRecognizerEvaluator(2000);
		//rs.evaluate(allResults);
		*/
		PlaceRecognizer pr = new PlaceRecognizer();
		
		
		Map<String, List<LatLonPoint>> res = pr.runSingle("2012-03-06", "2012-03-07", "362f6cf6e8cfba0e09b922e21d59563d26ae0207744af2de3766c5019415af", 7.6855,45.0713,  7.6855,45.0713);
		//pr.runSingle("2012-03-06", "2012-04-30", "7f3e4f68105e863aa369e5c39ab5789975f0788386b45954829346b7ca63", 7.6855,45.0713,  7.6855,45.0713);
		for(String k: res.keySet()) {
			System.out.println(k);
			for(LatLonPoint p: res.get(k))
				System.out.println(p.getLongitude()+","+p.getLatitude());
		}
		
		
		Logger.logln("Done!");
	}
	
	
	
	
}