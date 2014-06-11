package analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class Constraints {
	
	private Map<String,String> constraints = null;
	private Map<String, Double> users_weights = null;
	
	public Constraints(String sconstraints) {
		
		constraints = new HashMap<String,String>();
		if(sconstraints!=null && sconstraints.contains("=")) {
			String[] elements = sconstraints.split(";");
			for(String e: elements) {
				String[] nameval = e.split("=");
				constraints.put(nameval[0],nameval[1]);
			}
		}
		
		String file = constraints.get("users_event_probscores");
		
		try {
			File f = null;
			if(file!=null && (f= new File(file)).exists()) {
				users_weights = new HashMap<String, Double>();
				BufferedReader br = new BufferedReader(new FileReader(f));
				String line; String[] elements;
				while((line = br.readLine()) != null) {
					elements = line.split(",");
					users_weights.put(elements[0],Double.parseDouble(elements[1]));
				}
				br.close();
			}
		}catch(Exception e) {
			e.printStackTrace();
			users_weights = null;
		}
	}
	
	
	public double weight(String user) {
		if(users_weights == null) return 1.0;
		Double w = users_weights.get(user);
		if(w == null) w = 0.0;
		return w;
	}
	
	
	public boolean okConstraints(String ui_mnt, int ui_num_days) {
		if(constraints!=null) {
			String mnt  = constraints.get("mnt");
			if(mnt!=null) {
				if(mnt.startsWith("!")) { 
					//System.err.println(ui.mnt+"VS"+mnt.substring(1));
					if(ui_mnt.equals(mnt.substring(1))) return false;
				}
				else
					if(!ui_mnt.equals(mnt)) return false;
			}
			String mindays = constraints.get("mindays");
			if(mindays!=null && ui_num_days>0) 
				if(ui_num_days < Integer.parseInt(mindays)) return false;
			
			
			String maxdays = constraints.get("maxdays");
			if(maxdays!=null && ui_num_days>0) 
				if(ui_num_days > Integer.parseInt(maxdays)) return false;
		}
		return true;
	}
	
	public String getFileSuffix() {
		String suffix = "";
		if(constraints != null) {
			for(String key: constraints.keySet())
				suffix += "_"+key+"_"+constraints.get(key);
		}
		return suffix;
	}
	
}
