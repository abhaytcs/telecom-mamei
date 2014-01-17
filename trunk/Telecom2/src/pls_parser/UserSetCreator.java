package pls_parser;

import java.io.File;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import utils.FileUtils;
import utils.Logger;


public class UserSetCreator extends BufferAnalyzerConstrained {
	
	private Set<String> users;
	
	public UserSetCreator(String placemark_name, String user_list_name) {
		super(placemark_name, user_list_name);
		users = new HashSet<String>();
	}

	
	public void analyze(String username, String imsi, String celllac,long timestamp, Calendar cal) {
		users.add(username);
	}

	public void finish() {
		PrintWriter out = FileUtils.getPW("UserSetCreator", this.getString()+".csv");
		for(String u: users)
			out.println(u);
		out.close();
	}
	
	
	public static void main(String[] args) {
		BufferAnalyzerConstrained ba = new UserSetCreator("Firenze",null);
		ba.run();
		Logger.logln("Done!");
	}
}
