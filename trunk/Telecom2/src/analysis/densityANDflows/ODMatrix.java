package analysis.densityANDflows;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import utils.Colors;
import utils.Config;
import utils.CopyAndSerializationUtils;
import utils.Logger;
import visual.html.ArrowsGoogleMaps;
import visual.kml.KML;
import visual.kml.KMLArrow;
import area.region.Region;
import area.region.RegionMap;

public class ODMatrix {
	public static void main(String[] args) throws Exception {
		
		String region = "Piemonte";//"TorinoGrid20";
		File input_obj_file = new File(Config.getInstance().base_dir+"/cache/"+region+".ser");
		if(!input_obj_file.exists()) {
			System.out.println(input_obj_file+" does not exist... run the region parser first!");
			System.exit(0);
		}
		
		RegionMap rm = (RegionMap)CopyAndSerializationUtils.restore(input_obj_file); 
		Map<String,UserPlaces> up = UserPlaces.readUserPlaces(Config.getInstance().base_dir+"/PlaceRecognizer/file_pls_piem_users_above_2000/results.csv");
		
		
		// associated "latH,lonH->latW,lonW" to the counter c
		Map<String,Double> list_od = new TreeMap<String,Double>();
		
		for(UserPlaces p: up.values()) {
			List<double[]> homes = p.places.get("HOME");
			List<double[]> works = p.places.get("WORK");
			if(homes != null && works !=null) {
				double z = 1.0 / homes.size() * works.size();
				for(double[] h: homes)
				for(double[] w: works) {
					
					Region rh = rm.get(h[0], h[1]);
					Region rw = rm.get(w[0], w[1]);

					if(rh!=null && rw!=null) {
						String key = rh.getCenterLat()+","+rh.getCenterLon()+"->"+rw.getCenterLat()+","+rw.getCenterLon();
						Double c = list_od.get(key);
						c = c == null ? z : c+z;
						list_od.put(key, c);
					}
				}
			}
		}
		
		// prepare for drawing
		
		
		List<double[][]> points = new ArrayList<double[][]>();
		List<Double> w = new ArrayList<Double>();
		
		for(String k: list_od.keySet()) {
			String[] x = k.split("->");
			String[] llH = x[0].split(",");
			String[] llW = x[1].split(",");
			double latH = Double.parseDouble(llH[0]);
			double lonH = Double.parseDouble(llH[1]);
			double latW = Double.parseDouble(llW[0]);
			double lonW = Double.parseDouble(llW[1]);
			double weight = list_od.get(k);
			if(!x[0].equals(x[1]) && weight>5) {
				double[] p1 = new double[]{latH,lonH};
				double[] p2 = new double[]{latW,lonW};
				points.add(new double[][]{p1,p2});
				w.add(weight);
			}
		}
		
		
		String dir = Config.getInstance().base_dir+"/ODMatrix";
		File d = new File(dir);
		if(!d.exists()) d.mkdirs();
		
		ArrowsGoogleMaps.draw(dir+"/od"+region+".html","OD-HOME-WORK",points,w);
		printKML(dir+"/od"+region+".kml","OD-HOME-WORK",points,w);
		
		
		Logger.log("Done!");
	}
	
	
	public static void printKML(String file, String title, List<double[][]> points, List<Double> weights) throws Exception {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
		KML kml = new KML();
		kml.printHeaderFolder(out, title);
		
		for(int i=0; i<points.size();i++) {
			double[][] p = points.get(i);
			double w = weights.get(i);
			out.println(KMLArrow.printArrow(p[0][1], p[0][0], p[1][1], p[1][0], w, "#ff0000ff"));
		}
		
		kml.printFooterFolder(out);
		out.close();
	}
	
}