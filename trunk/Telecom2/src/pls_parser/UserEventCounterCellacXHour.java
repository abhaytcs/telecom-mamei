package pls_parser;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import utils.FileUtils;
import utils.Logger;
import area.Placemark;

public class UserEventCounterCellacXHour extends BufferAnalyzerConstrained {
	
	static final String[] MONTHS = new String[]{"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
	static final String[] DAY_WEEK = new String[]{"0","Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
	
	
	private Map<String,UserInfo> users_info;

	
	public UserEventCounterCellacXHour(String placemark_name, String user_list_name) {
		super(placemark_name,user_list_name);
		users_info = new HashMap<String,UserInfo>();
	}

	public void analyze(String username, String imsi, String celllac, long timestamp, Calendar cal,String header) {
		UserInfo info = users_info.get(username);
		if(info == null) {
			info = new UserInfo();
			info.imsi = imsi;
			users_info.put(username, info);
		}
		String day = cal.get(Calendar.YEAR)+"-"+cal.get(Calendar.MONTH)+"-"+cal.get(Calendar.DAY_OF_MONTH);
		String dayw = DAY_WEEK[cal.get(Calendar.DAY_OF_WEEK)];
		info.add(day,dayw,cal.get(Calendar.HOUR_OF_DAY),celllac);
	}
	
	
	private int getTotDays() {
		Set<String> days = new HashSet<String>();
		for(UserInfo ui:users_info.values()) {
			days.addAll(ui.getDays());
		}
		return days.size();
	}
	
	//private static final SimpleDateFormat F = new SimpleDateFormat("dd/MM/yyyy");
	public void finish() {
		PrintWriter out = FileUtils.getPW("UserEventCounter", this.getString()+"_cellXHour.csv");
		out.println("// TOT. DAYS = "+getTotDays());
		for(String user: users_info.keySet())
			out.println(user+","+users_info.get(user));
		out.close();
	}
	
	public static void main(String[] args) throws Exception {
		BufferAnalyzerConstrained ba = new UserEventCounterCellacXHour(null,FileUtils.getFileS("UserSetCreator/Venezia.csv"));
		ba.run();
		Logger.logln("Done!");
	}	
	
	
	/*
	 * This main is places here for convenience. It just read the file and remove all the users producing few events
	 */
	public static void trim(Placemark p, int min_size) throws Exception {
		BufferedReader br = FileUtils.getBR("UserEventCounter/"+p.name+"_cellacXhour.csv");
		PrintWriter out = FileUtils.getPW("UserEventCounter", p.name+"_cellacXhour_trim"+min_size+".csv");
		String line;
		while((line = br.readLine()) != null) {
			int num_pls = Integer.parseInt(line.split(",")[2]);
			if(num_pls >= min_size)
				out.println(line);
		}
		br.close();
		out.close();
		Logger.logln("Done!");
	}
	
	/***********************************************************************************************************************/
	/* USER INFO INNER CLASS */
	
	private class UserInfo {
		
		String imsi;
		Set<String> pls = new TreeSet<String>();
		
		public void add(String day, String dayw, int h, String cellac) {
			pls.add(day+":"+dayw+":"+h+":"+celllac);
		}
		
		public int getNumDays() {
			return getDays().size();
		}
		
		
		public Set<String> getDays() {
			Set<String> days = new HashSet<String>();
			for(String p:pls) 
				days.add(p.substring(0, p.indexOf(":")));
			return days;
		}
		
		public int getDaysInterval() {
			Calendar[] min_max = getTimeRange();
			Calendar min = min_max[0];
			Calendar max = min_max[1];
			return 1+(int)Math.floor((max.getTimeInMillis() - min.getTimeInMillis())/(1000*3600*24));
		}
		
		public Calendar[] getTimeRange() {
			Calendar min = null;
			Calendar max = null;
			for(String p:pls) {
				String[] day = p.substring(0, p.indexOf(":")).split("-"); // cal.get(Calendar.YEAR)+"-"+cal.get(Calendar.MONTH)+"-"+cal.get(Calendar.DAY_OF_MONTH);
				int year = Integer.parseInt(day[0]);
				int month = Integer.parseInt(day[1]);
				int d = Integer.parseInt(day[2]);
				Calendar c = new GregorianCalendar(year,month,d);
				if(min == null || c.before(min)) min = c;
				if(max == null || c.after(max)) max = c;
			}
			return new Calendar[]{min,max};
		}
		
		
		public String toString() {			
			StringBuffer sb = new StringBuffer();
			for(String p:pls) 
				sb.append(p+",");
			return imsi+","+pls.size()+","+getNumDays()+","+getDaysInterval()+","+sb.toString();
		}
	}

}