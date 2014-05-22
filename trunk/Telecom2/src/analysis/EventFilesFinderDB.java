package analysis;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pls_parser.AnalyzePLSCoverageSpace;
import pls_parser.AnalyzePLSCoverageTime;
import region.RegionMap;
import utils.Logger;

/*
 * This class identifies the proper PLS folder (if any) to process the event
 */

public class EventFilesFinderDB {
	
	
	private SimpleDateFormat F1 = new SimpleDateFormat("yyyy-MM-dd-hh");
	private SimpleDateFormat F2 = new SimpleDateFormat("yyyyMMdd",Locale.US);
	
	private Map<String,RegionMap> maps;
	private Map<String,List<String>> mapt;
	
	public EventFilesFinderDB() {
		maps = new AnalyzePLSCoverageSpace().getPlsCoverage();
		mapt = new AnalyzePLSCoverageTime().computeAll();
	}
	
	
	public String find(String sday, String shour, String eday, String ehour, double lon1, double lat1, double lon2, double lat2) {
		try {
			Calendar c1 = Calendar.getInstance();
			Calendar c2 = Calendar.getInstance();
			c1.setTime(F1.parse(sday+"-"+shour));
			c2.setTime(F1.parse(eday+"-"+ehour));
			return find(c1,c2,lon1,lat1,lon2,lat2);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
		
	}
	
	public String find(Calendar cs, Calendar ce, double lon1, double lat1, double lon2, double lat2) {
		try {
			
			String sday = F2.format(cs.getTime());
			String eday = F2.format(ce.getTime());
	
			double clat = (lat1 + lat2)/2;
			double clon = (lon1 + lon2)/2;
			
			String dir = null;
			
			// check spatial constraints
			
			for(String r: maps.keySet()) {
				
				//System.out.println("testing "+r+", ("+clon+","+clat+")");
				
				if(maps.get(r).get(clon, clat) != null) {
					if(dir==null) dir = r;
					else Logger.logln("Warning: Multiple matching regions!");
				}
			}
			
			if(dir == null) {
				//Logger.logln("Selected event is out of PLS coverage area in space");
				return null;
			}
			
			// check temporal constraints
			System.out.println(dir);			
			List<String> dmap = mapt.get(dir.substring("file_".length()));
			
			if(dmap.contains(sday) && dmap.contains(eday))
				return dir;
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	
	public static void main(String[] args) {
		EventFilesFinderDB eff = new EventFilesFinderDB();
		String dir = eff.find("2014-03-10","4","2014-03-10","7",12.3248,45.4395,12.3248,45.4395);
		System.out.println(dir);
	}
}