package area;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import utils.Colors;
import utils.Config;
import utils.CopyAndSerializationUtils;
import utils.Logger;
import utils.kdtree.GenericPoint;
import utils.kdtree.KDTree;
import utils.kdtree.Point;
import utils.kdtree.RangeSearchTree;
import visual.Kml;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class RegionMap implements Serializable {
		
	private String name;
	private List<Region> rm;
	private KDTree<Double,Point<Double>,Region> kdtree;
	private RangeSearchTree<Double,Point<Double>,Region> rangetree;
	
	public RegionMap(String name) {
		this.name = name;
		rm = new ArrayList<Region>();
		kdtree = new KDTree<Double,Point<Double>,Region>(2);
		rangetree = (RangeSearchTree<Double,Point<Double>,Region>)kdtree;
	}
	
	public void add(Region r) {
		rm.add(r);
		Point<Double> cellPoint = new GenericPoint<Double>(r.getCenterLon(),r.getCenterLat());
		kdtree.put(cellPoint, r);
	}
	
	public static final double earth_radius = 6372.795477598; // km
	public static final double search_radius = 10; // km
	public static final double deg_radius = Math.toDegrees(search_radius/earth_radius);
	public Region get(double lon, double lat) {
		Geometry p = new GeometryFactory().createPoint(new Coordinate(lon, lat));
		
		Point<Double> lower_point = new GenericPoint<Double>(lon-deg_radius,lat-deg_radius);
		Point<Double> upper_point = new GenericPoint<Double>(lon+deg_radius,lat+deg_radius);
		Iterator<Map.Entry<Point<Double>,Region>> iter = rangetree.iterator(lower_point, upper_point);
		while(iter.hasNext()) {
			Region r = iter.next().getValue();
			Geometry g = r.getGeom();
			if(p.within(g)) return r;
		}
		
		Logger.logln("Fast search has failed, try complete search!");
		for(Region r: rm) {
			//System.out.println(r.getName()+" = "+r.getCenterLon()+","+r.getCenterLat());
			if(p.within(r.getGeom())) return r;
		}
		
		return null;		
	}
	
	
	public void printKML() throws Exception  {
		String dir = Config.getInstance().base_dir+"/RegionMap";
		File d = new File(dir);
		if(!d.exists()) d.mkdirs();
		
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(dir+"/"+name+".kml")));
		Kml kml = new Kml();
		kml.printHeaderFolder(out, name);
		int index = 0;
		for(Region r: rm) {
			out.println(r.toKml(Colors.RANDOM_COLORS[index]));
			index++;
			if(index >= Colors.RANDOM_COLORS.length) index = 0;
		}
		kml.printFooterFolder(out);
		out.close();
	}
	
	
	public static void main(String[] args) throws Exception {
		String region = "Piemonte";
		File input_obj_file = new File(Config.getInstance().base_dir+"/cache/"+region+".ser");
		if(!input_obj_file.exists()) {
			System.out.println(input_obj_file+" does not exist... run the region parser first!");
			System.exit(0);
		}
			
		RegionMap rm = (RegionMap)CopyAndSerializationUtils.restore(input_obj_file); 
		//rm.printKML();
		
		System.out.println(rm.get(8.46050279447007,44.67433775848695).getName()); // should be ACQUI TERME
		System.out.println(rm.get(7.3591814195141,44.51233268378211).getName()); // should be BUSCA
		System.out.println(rm.get(7.547977490302962,44.60851381725961).getName()); // should be MANTA
		System.out.println(rm.get(7.777669845628534,45.2910107499951).getName()); // should be SAN GIORGIO CANAVESE
		System.out.println(rm.get(7.777542202427386,45.29121254507205).getName()); // should be LUSIGLI�
		
	}
	
}
