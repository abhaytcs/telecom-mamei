package pls_parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import utils.Config;
import utils.Logger;

public class AnalyzePLSCoverage {
	
	static Config conf = null;
	private static final int BUFFER_SIZE = 1048576;
	
	
	private static Calendar startTime = null;
	private static Calendar endTime = null;
	
	
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		
		Map<String,String> allDays = compute();
		
		Logger.logln("Days in the dataset:");
		for(String d:allDays.keySet()) 
			Logger.logln(d+" = "+allDays.get(d));
		
		
		long endTime = System.currentTimeMillis();
		int mins = (int)((endTime - startTime) / 60000);
		Logger.logln("Completed after "+mins+" mins");
	}
	
	
	public static Map<String,String> compute() {
		String dir = Config.getInstance().pls_folder;
		return compute(dir);
	}
	
	public static Map<String,String> compute(String dir) {	
		Map<String,String> allDays = new TreeMap<String,String>();
		try {
			analyzeDirectory(new File(dir),allDays);
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
				
				String key = cal.get(Calendar.YEAR)+"/"+MONTHS[cal.get(Calendar.MONTH)]+"/"+sday;
				String h = allDays.get(key);
						
				allDays.put(key, h==null? cal.get(Calendar.HOUR_OF_DAY)+"-" : h+cal.get(Calendar.HOUR_OF_DAY)+"-");
			}
			else if(item.isDirectory())
				analyzeDirectory(item,allDays);
		}	
	}
}