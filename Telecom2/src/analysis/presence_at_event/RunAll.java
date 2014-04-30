package analysis.presence_at_event;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.gps.utils.LatLonPoint;
import org.gps.utils.LatLonUtils;

import region.CityEvent;
import region.Placemark;
import utils.Config;
import utils.FileUtils;
import analysis.EventFilesFinder;

/*
 * This class encapsulates all the code required to estimate the attendance to a give event.
 * It basically provides a estimateAttendance producing the estimate.
 */

public class RunAll {
	
	public static boolean CLEANUP = true;
	
	
	public int[] radiusAndAttendance(String sday,String shour,String eday, String ehour, double lon1, double lat1) {
		return radiusAndAttendance(sday,shour,eday, ehour, lon1, lat1, lon1, lat1);
	}
	
	public int[] radiusAndAttendance(String sday,String shour,String eday, String ehour, double lon1, double lat1, double lon2, double lat2) {
		try {
			PlacemarkRadiusExtractor.PLOT = false;
			PresenceCounter.PLOT = false;
			EventFilesFinder eff = new EventFilesFinder();
			String dir = eff.find(sday,shour,eday,ehour,lon1,lat1,lon2,lat2);
			System.out.println("---> "+dir);
			if(dir == null) return new int[]{0,0};
			
			SimpleDateFormat F = new SimpleDateFormat("yyyy-MM-dd-hh");
			Config.getInstance().pls_folder = FileUtils.getFile("DATASET/PLS/file_pls/"+dir).toString(); 
			Config.getInstance().pls_start_time.setTime(F.parse(sday+"-"+shour));
			Config.getInstance().pls_end_time.setTime(F.parse(eday+"-"+ehour));
			
			Config.getInstance().pls_start_time.add(Calendar.DAY_OF_MONTH, -3);
			Config.getInstance().pls_end_time.add(Calendar.DAY_OF_MONTH, +3);
			
			
			double lon = (lon1+lon2)/2;
			double lat = (lat1+lat2)/2;
			LatLonPoint p1 = new LatLonPoint(lat1,lon1);
			LatLonPoint p2 = new LatLonPoint(lat2,lon2);
			double r = LatLonUtils.getHaversineDistance(p1, p2) / 2;
			String n = "tmp";
			final Placemark p = new Placemark(n,new double[]{lat,lon},r);
			CityEvent ce = new CityEvent(p,convert2CityEventTimeFormat(sday,shour),convert2CityEventTimeFormat(eday,ehour),0);
			List<CityEvent> all = new ArrayList<CityEvent>();
			all.add(ce);
			
			PlacemarkRadiusExtractor.process(all);
			Map<String,Double> bestRadius = PlacemarkRadiusExtractor.readBestR(PresenceCounter.USE_INDIVIDUAL_EVENT,PlacemarkRadiusExtractor.DIFF);	
			int bestr = (int)Math.round(bestRadius.get(ce.toString()));
			
			
			PresenceCounter.process(all);
			int attendance =  ResultEvaluator.run(new File(PresenceCounter.ODIR+"/"+PresenceCounter.OFILE));
			
			if(CLEANUP) {
				// clean up
				String odir = FileUtils.createDir("BASE/PlacemarkRadiusExtractor/"+Config.getInstance().get_pls_subdir()).toString();
				File f = new File(odir+"/"+PlacemarkRadiusExtractor.OFILE);
				f.delete();
				
				File d = new File(odir+"/"+p.name);
				File[] files = d.listFiles();
				for(File f1: files) {
					if(f1.isFile()) f1.delete();
					if(f1.isDirectory()) {
						for(File f2: f1.listFiles())
							f2.delete();
					}
				}
				f = new File(PresenceCounter.ODIR+"/"+PresenceCounter.OFILE);
				f.delete();
				
				
				d = FileUtils.getFile("BASE/PLSEventsAroundAPlacemark/"+Config.getInstance().get_pls_subdir());
				files = d.listFiles(new FilenameFilter() {
				    @Override
				    public boolean accept(File dir, String name) {
				        return name.startsWith(p.name+"_");
				    }
				});
				for(File f1: files) 
					f1.delete();
			}
			
			return new int[]{bestr,attendance};
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	
		return new int[]{0,0};
	}
	
	//"2014-03-02", "19" ----> "dd/MM/yyyy HH:mm"
	private static final SimpleDateFormat FIN = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private static final SimpleDateFormat FOUT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
	private Calendar convert2CityEventTimeFormat(String day, String h) {
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(FIN.parse(day+" "+h+":00"));
			return cal;// FOUT.format(cal.getTime());
		} catch(Exception e) {
			e.printStackTrace();
		}	
		return null;
	}
	
	
	
	public static void main(String[] args) throws Exception {
	
		RunAll ra = new RunAll();
			
		//int[] rad_att = ra.radiusAndAttendance("2014-03-02","19","2014-03-03","0",11.28265300110946,43.78066799975202); // partita Fiorentina - Lazio. capienza stadio 47000
		//int[] rad_att = ra.radiusAndAttendance("2012-04-01","13","2012-04-01","18",9.123845,45.478068); // San Siro
		int[] rad_att = ra.radiusAndAttendance("2012-03-20","19","2012-03-20","23",7.641453,45.109536); // Juventus Stadium
		
		System.out.println("RADIUS = "+rad_att[0]);
		System.out.println("ATTENDANCE = "+rad_att[1]);
	} 
	
}
