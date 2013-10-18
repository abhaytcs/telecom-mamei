package pre_delete;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import pls_parser.PLSEventsAroundAPlacemark;
import utils.Config;
import utils.CopyAndSerializationUtils;
import utils.Logger;
import visual.java.GraphScatterPlotter;
import area.CityEvent;
import area.Placemark;

public class PresenceCounterSimpleWRelevantCellsOnly {
	
	public static void main(String[] args) throws Exception {
		
		double er = 500;
		double or = 500;
		int days = 3;
		process(er,or,days);
		
		Logger.logln("Done!");
		
	}
		
	public static void process(double er, double or, int days) throws Exception {
		
		Logger.log("Processing: o_radius = "+or+" days = "+days+" ");
		
		List<CityEvent> events = CityEvent.getEventsInData();
		
		//create a map that associates a Placemark with the list of events happening in there
		Map<String,List<CityEvent>> placemark_events = new HashMap<String,List<CityEvent>>();
		for(CityEvent ce: events) {
			List<CityEvent> l = placemark_events.get(ce.spot.name);
			if(l==null) {
				l = new ArrayList<CityEvent>();
				placemark_events.put(ce.spot.name, l);
			}
			l.add(ce);
		}
						
		List<String> labels = new ArrayList<String>();
		List<double[][]> data = new ArrayList<double[][]>();
		SimpleRegression sr = new SimpleRegression();	
		for(String p : placemark_events.keySet()) {
			labels.add(p);
			List<CityEvent> pevents =  placemark_events.get(p);
			double[][] result = new double[pevents.size()][2];
			int i = 0;
			for(CityEvent ce: pevents) {
				//ce.spot.changeRadius(radius);
				double c = count(ce,er,or,days);
				Logger.logln(ce.toString()+" estimated attendance = "+(int)c+" groundtruth = "+ce.head_count);
				result[i][0] = c;
				result[i][1] = ce.head_count;
				sr.addData(result[i][0], result[i][1]);
				i++;
			}
			data.add(result);
		}
				
		Logger.logln("r="+sr.getR()+", r^2="+sr.getRSquare()+", sse="+sr.getSumSquaredErrors());
		
		new GraphScatterPlotter("Result: o_radius = "+or+",days = "+days,"Estimated","GroundTruth",data,labels);
		
		String dir = Config.getInstance().base_dir +"/PresenceCounterSimpleWRelevantCellsOnly";
		File d = new File(dir);
		if(!d.exists()) d.mkdirs();
		PrintWriter out = new PrintWriter(new FileWriter(dir+"/result_"+or+"_"+days+".csv"));
		out.println("event,estimated,groundtruth");
		int i = 0;	
		for(String p : placemark_events.keySet()) {
			List<CityEvent> pevents =  placemark_events.get(p);
			double[][] result = data.get(i);
			for(int j=0; j<pevents.size();j++) 
				if(result[j][0] > 0)
					out.println(pevents.get(j).toString()+","+(int)result[j][0]+","+(int)result[j][1]);
			i++;
		}
		out.close();
		//Logger.logln("Done!");
	}

	
	//static Map<String,Double> bestRadius = (Map<String,Double>)CopyAndSerializationUtils.restore(new File(Config.getInstance().base_dir+"/PlacemarkRadiusExtractor/result.ser"));

	public static double count(CityEvent event, double e_radius, double o_radius, int days) throws Exception {	
		
		Logger.logln("\n"+event.spot.name+", e_r = "+e_radius+", o_r = "+o_radius);
		
		String file_event = getFile(event.spot,e_radius);
		String file_other = getFile(event.spot,o_radius);
		
	
		Set<String> relevantCells = (Set<String>)CopyAndSerializationUtils.restore(new File(Config.getInstance().base_dir+"/ReleventCellsExtractor/"+event.spot.name+".ser"));
		Logger.logln(event.toString()+" N. REL CELLS = "+relevantCells.size());
		Set<String> intersection = getRelCellsThatAreInthePLS(file_event,relevantCells);
		Logger.logln(intersection.size()+"/"+relevantCells.size()+" are in the pls");
		for(String i : intersection)
			Logger.log(i+", ");
		Logger.logln("");
		
		Set<String> userPresentDuringEvent = getUsers(file_event,event.st,event.et,null,null,relevantCells);
		//System.err.println("Users during "+event+" = "+userPresentDuringEvent.size());
		
		Calendar start = (Calendar)event.st.clone();
		start.add(Calendar.DAY_OF_MONTH, -days);
		
		Calendar end = (Calendar)event.et.clone();
		end.add(Calendar.DAY_OF_MONTH, days);
		
		Set<String> userPresentAtTheEventTimeOnOtherDays = getUsers(file_other,start,end,event.st,event.et,relevantCells);
		
		userPresentDuringEvent.removeAll(userPresentAtTheEventTimeOnOtherDays);
		
		return userPresentDuringEvent.size();
	}
	
	public static String getFile(Placemark op, double radius) throws Exception{
		Placemark p = op.clone();
		p.changeRadius(radius);
		String file = Config.getInstance().base_dir+"/PLSEventsAroundAPlacemark/"+p.name+"_"+p.radius+".txt";
		File f = new File(file);
		if(!f.exists()) {
			Logger.logln(file+" does not exist");
			Logger.logln("Executing PLSEventsAroundAPlacemark.process()");
			PLSEventsAroundAPlacemark.process(p);
		}
		//else Logger.logln(file+" already exists!");
		return file;
	}
	
	public static Set<String> getUsers(String file, Calendar start, Calendar end, Calendar start_exclude, Calendar end_exclude, Set<String> relevant) throws Exception {
		Set<String> users = new HashSet<String>();
		String line;
		Calendar cal = new GregorianCalendar();
		BufferedReader in = new BufferedReader(new FileReader(file));
		while((line = in.readLine()) != null){
			String[] splitted = line.split(",");
			if(splitted.length == 5) {
				cal.setTimeInMillis(Long.parseLong(splitted[1]));
				boolean relevant_check = (relevant == null || relevant.contains(splitted[3]));
				if(start.before(cal) && end.after(cal) && relevant_check) {
					if(start_exclude == null || end_exclude ==null)
						users.add(splitted[0]);
					else if(cal.before(start_exclude) || cal.after(end_exclude))
						users.add(splitted[0]);
				}
			}
			else System.out.println("Problems: "+line);
		}
		in.close();
		return users;
	}
	
	
	public static Set<String> getRelCellsThatAreInthePLS(String file, Set<String> relevant) throws Exception {
		
		Set<String> intersection = new HashSet<String>();
		
		String line;
		BufferedReader in = new BufferedReader(new FileReader(file));
		while((line = in.readLine()) != null){
			String[] splitted = line.split(",");
			if(splitted.length == 5) {
				String cellac = splitted[3];
				if(relevant.contains(cellac))
				 intersection.add(cellac);
			}
		}
		in.close();
		return intersection;
	}
}
