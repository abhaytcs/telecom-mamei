package pls_parser;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import utils.Config;
import utils.FileUtils;
import utils.Logger;

public class AnalyzePLSCoverage {
	
	static Config conf = null;
	

	public static void main(String[] args) {
		AnalyzePLSCoverage apc = new AnalyzePLSCoverage();
		/*
		Map<String,String> allDays = apc.compute();
		Logger.logln("Days in the dataset:");
		for(String d:allDays.keySet()) 
			Logger.logln(d+" = "+allDays.get(d));
		System.out.println("TOT = "+allDays.size());
		*/
		Map<String,Map<String,String>> all =  apc.computeAll();
		for(String file: all.keySet()) {
			Map<String,String> allDays = all.get(file);
			Logger.logln("Days in the dataset:");
			for(String d:allDays.keySet()) 
				Logger.logln(d+" = "+allDays.get(d));
			System.out.println("TOT = "+allDays.size());
		}
	}
	
	public Map<String,Map<String,String>> computeAll() {
		File[] files = FileUtils.getFiles("DATASET/PLS/file_pls");
		Map<String,Map<String,String>> all = new HashMap<String,Map<String,String>>();
		for(File f: files) 
            all.put(f.getAbsolutePath(), compute(f));
		return all;
	}
   
	
	
	public Map<String,String> compute() {
		File dir = new File(Config.getInstance().pls_folder);
		return compute(dir);
	}
	
	public Map<String,String> compute(File dir) {	
		Map<String,String> allDays = new TreeMap<String,String>();
		try {
			analyzeDirectory(dir,allDays);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return allDays;
	}
	
	static final String[] MONTHS = new String[]{"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
	private static void analyzeDirectory(File directory, Map<String,String> allDays) throws Exception{
		File[] items = directory.listFiles();
		for(int i=0; i<items.length;i++){
			File item = items[i];
			if(item.isFile()) {
				Calendar cal = new GregorianCalendar();
				String n = item.getName();
				cal.setTimeInMillis(Long.parseLong(n.substring(n.lastIndexOf("_")+1, n.indexOf(".zip"))));
				
				int day =  cal.get(Calendar.DAY_OF_MONTH);
				String sday = day < 10 ? "0"+day : ""+day;
				
				String key = getRegion(item)+"-"+cal.get(Calendar.YEAR)+"/"+MONTHS[cal.get(Calendar.MONTH)]+"/"+sday;
				
				String h = allDays.get(key);
				
				if(h == null) h = "";
				if(!h.contains(cal.get(Calendar.HOUR_OF_DAY)+"-")) 
					h =  h + cal.get(Calendar.HOUR_OF_DAY)+"-";
			
				allDays.put(key, h);
			}
			else if(item.isDirectory())
				analyzeDirectory(item,allDays);
		}	
	}
	
	
	static final String[] REGIONS = new String[]{"piem","lomb","ve","fi"};
	public static String getRegion(File f) {
		for(String r: REGIONS)
			if(f.getAbsolutePath().contains(r)) return r;
		return "nan";
	}
	
}
