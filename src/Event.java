import java.util.Vector;
import java.util.Date;
import org.w3c.dom.*;

public class Event {
	String id, eventName;
	Date eventTime; 
	Vector<String> clue = null, url = null, emailID = null, directory = null;
	Vector<int[]> ip = null;
	Vector<Vector<String>> yara = null;
	float yara_match_threshold = 0.7f;
	static String text_delims = "[~!@#$%^&*|_(),;:?/<>[{]} ]+";
	static String ip_delims = "[.:]+";
	static String url_delims = "[\\/:]+";
	static String date_delims = "[\\/.-]+";
	static String ignorableDir[] = {"%userprofile%", "program files (x86)", "program files", "documents and settings", "local", "local settings", "application data", "temp", "appdata", "windows", //
									"users", "사용자", "system32", "system", "default", "my documents", "내 문서", "projects", "visual studio 2005", "visual studio 2008", "visual studio 2010", "release", "debug",//
			 						"my downloads", "my music", "my pictures", "my videos", "mail", "x64", "x86"};
	
	
	Event(){
		eventTime = new Date();
	}
	
	Event(String id){
		this();
		this.id = id;
	}
	
	Event(String id, Date time){
		this(id);
		eventTime = time;
	}
	
	Event(Node eventNode){
		this();
		Element elem = (Element) eventNode;
		setEventInformation(eventNode);
		
		NodeList attributes = elem.getElementsByTagName("Attribute");
		for( int i=0; i<attributes.getLength(); i++ ){
			Node attr = attributes.item(i);
			if( attr.getNodeType() != Node.ELEMENT_NODE ) 
				continue;
			Element attr_ = (Element) attr;
			
			String category="", type="";
			try{
				category = attr_.getElementsByTagName("category").item(0).getTextContent();
				type = attr_.getElementsByTagName("type").item(0).getTextContent();
			}catch(Exception ep){
				break;
			}
			
			if( Event.isIgnorableMISPData(category) || Event.isIgnorableMISPData(type) )
				continue;

			String value = attr_.getElementsByTagName("value").item(0).getTextContent();
			switch(Event.getAttributeType(type)){
				case "IP":
					addIP(value);
					break;
				case "URL":
					addURL(value);
					break;
				case "YARA":
					String comment = attr_.getElementsByTagName("comment").item(0).getTextContent();
					addYara(value, comment);
					break;
				default:
					String attType = Event.getAttributeType(value);
					//System.out.println(value + " \\ "+attType);
					if( attType.equals("EMAIL_ID") )
						addEmailID(value);
					else if( attType.equals("DIR") )
						addDirectory(value);
					else if( attType.equals("DATE") )
						addDate(value);
					else
						addClue(value);
			}
		}
		removeDuplicates();
		//printEventInfo();
	}


	@SuppressWarnings("deprecation")
	void setEventInformation(Node event){
		Element elem = (Element) event;
		id = elem.getElementsByTagName("id").item(0).getTextContent();
		eventName = elem.getElementsByTagName("info").item(0).getTextContent();
		String[] date = elem.getElementsByTagName("date").item(0).getTextContent().split("[-]");
		eventTime.setYear(Integer.parseInt(date[0]));
		eventTime.setMonth(Integer.parseInt(date[1])-1);
		eventTime.setDate(Integer.parseInt(date[2]));
	}
	
	void addIP(String ip){
		if( this.ip == null )
			this.ip = new Vector<int[]>();
		this.ip.add(parseIP(ip));
	}
	
	// protocol part(http, ftp, etc..) and URI part are omitted.
	void addURL(String url){
		if( this.url == null )
			this.url = new Vector<String>();
		this.url.add(parseURL(url).toLowerCase());
	}

	// In each vector in yara vector, the 1st element is the file name and others are yara detection rules.
	void addYara(String value, String filename){
		if( this.yara == null )
			this.yara = new Vector<Vector<String>>();
		
		boolean hasFile = false;
		for( int y=0; y<this.yara.size(); y++ ){
			if( this.yara.get(y).get(0).equals(filename) ){
				this.yara.get(y).add(value);
				hasFile = true;
				break;
			}
		}
		if( !hasFile ){
			Vector<String> tmp = new Vector<String>();
			tmp.add(filename);
			tmp.add(value);
			this.yara.add(tmp);
		}
	}

	void addClue(String clue){
		if( this.clue == null )
			this.clue = new Vector<String>();
		String[] tmp = clue.split(text_delims);
		for( int i=0; i<tmp.length; i++ ){
			if( tmp[i].equals("") || tmp[i].equals(" ") ) continue;
			try{

				if( Integer.parseInt(tmp[i]) <= 24 ) continue;
			}catch(NumberFormatException e){
			}
			this.clue.add(tmp[i].toLowerCase());
		}
	}
	
	void addEmailID(String id){
		if( this.emailID == null )
			this.emailID = new Vector<String>();
		this.emailID.add(parseEmailID(id).toLowerCase());
	}
	
	void addDirectory(String value) {
		if( this.directory == null )
			this.directory = new Vector<String>();	
		Vector<String> tmp = parseDirectory(value);
		for( int i=0; i<tmp.size(); i++ ){
			System.out.println(tmp.get(i));
			directory.add(tmp.get(i).toLowerCase());
		}
	}
	
	void addDate(String value) {
		// TODO Auto-generated method stub
		
	}
	
	static int[] parseIP(String ip){
		int ip_i[] = new int[4];
		String[] ip_s = ip.split(ip_delims);
		for( int i=0; i<4; i++ )
			ip_i[i] = Integer.parseInt(ip_s[i]);
		return ip_i;
	}
	
	static String parseURL(String url){
		url = url.replace("http://", "");
		url = url.replace("https//:", "");
		url = url.replace("ftp://", "");
		String retval = url.split(url_delims)[0];
		retval = retval.replace("www.", "");
		return retval;
	}
	
	static Vector<String> parseString(Vector<String> vec){
		for( int i=0; i<vec.size(); i++ ){
			String[] tmp = vec.get(i).split(text_delims);
			vec.remove(i);

			for( int j=0; j<tmp.length; j++ ){
				if( Integer.parseInt(tmp[i]) <= 24 ) continue;
				vec.insertElementAt(tmp[j], j+i);
			}
		}
		return vec;
	}
	
	static String parseEmailID(String account){
		String[] retval = account.split("[@]");
		return retval[0];
	}
	
	static Vector<String> parseDirectory(String dir){
		String[] tmp = dir.split("[\\\\:]");
		Vector<String> retval = new Vector<String>();

		for( int i=0; i<tmp.length; i++ ){
			// check if a string segment is drive indicator
			boolean jump = false;
			for( char j='a'; j<='z'; j++ ){
				if( tmp[i].equalsIgnoreCase(String.valueOf(j)) ){
					jump = true;
					break;
				}
			}
			if( jump )
				continue;
			
			// check is a string segment is ignorable directory path name
			for( int j=0; j<ignorableDir.length; j++ ){
				if( tmp[i].equalsIgnoreCase(ignorableDir[j]) ){
					jump = true;
					break;
				}
			}
			if( jump )
				continue;
			
			retval.add(tmp[i]);
		}
		return retval;
	}
	
	void printEventInfo(){
		System.out.println("------------[ Information of Event " + id +" ]------------");
		System.out.println("* Event name : " + eventName);
		System.out.println("* Event time : " + eventTime.toString());
		if( ip != null ){
			System.out.println("< IP >");
			for( int i=0; i<ip.size(); i++ )
				System.out.println(ip.get(i));
		}
		if( url != null ){
			System.out.println("< URL >");
			for( int i=0; i<url.size(); i++ )
				System.out.println(url.get(i));
		}
		if( clue != null ){
			System.out.println("< Text >");
			for( int i=0; i<clue.size(); i++ )
				System.out.println(clue.get(i));
		}
		if( yara != null ){
			System.out.println("< YARA >");
			for( int i=0; i<yara.size(); i++ ){
				for( int j=1; j<yara.get(i).size(); j++ )
					System.out.println("("+yara.get(i).get(0)+") "+yara.get(i).get(j));
			}
		}
		if( emailID != null ){
			System.out.println("< Email ID >");
			for( int i=0; i<emailID.size(); i++ )
				System.out.println(emailID.get(i));
		}
		System.out.println("--------------------------------------------------------");
	}
	
	void removeDuplicates(){
		removeDuplicateClue();
		removeDuplicateURL();
		removeDuplicateIP();
		removeDuplicateYara();
		removeDuplicateEmailID();
	}
	
	// Remove duplicated strings from input vector
	static void removeDuplicateString(Vector<String> vec){
		for( int i=0; i<vec.size(); i++ ){
			String tmp1 = vec.get(i);
			for( int j=i+1; j<vec.size(); j++ ){
				String tmp2 = vec.get(j);
				if( tmp1.compareTo(tmp2)==0 ){
					vec.remove(j);
					j--;
				}
			}
		}
	}
	
	// Remove duplicated URLs from url vector of this event
	void removeDuplicateURL(){
		if( url == null )
			return;
		
		for( int i=0; i<url.size(); i++ ){
			String tmp1 = url.get(i);
			for( int j=i+1; j<url.size(); j++ ){
				String tmp2 = url.get(j);
				if( tmp1.compareTo(tmp2)==0 ){
					url.remove(j);
					j--;
				}
			}
		}
	}
	
	// Remove duplicated clues from clue vector of this event
	void removeDuplicateClue(){
		if( clue == null )
			return;
		
		for( int i=0; i<clue.size(); i++ ){
			String tmp1 = clue.get(i);
			for( int j=i+1; j<clue.size(); j++ ){
				String tmp2 = clue.get(j);
				if( tmp1.compareTo(tmp2)==0 ){
					clue.remove(j);
					j--;
				}
			}
		}
	}
	
	// Remove duplicated yara detection rules from yara vector of this event
	void removeDuplicateYara(){
		if( yara == null )
			return;
		
		for( int i=0; i<yara.size(); i++ )
			removeDuplicateString(yara.get(i));
	}
	
	// Remove duplicated IPs from input vector
	static void removeDuplicateIP(Vector<int[]> vec){
		for( int i=0; i<vec.size(); i++ ){
			int[] tmp1 = vec.get(i);

			for( int j=i+1; j<vec.size(); j++ ){
				int[] tmp2 = vec.get(j);
				boolean dup = true;
				for( int k=0; k<tmp1.length; k++ ){
					if( tmp1[k] != tmp2[k] ){
						dup = false;
						break;
					}
				}
				if( dup ){
					vec.remove(j);
					j--;
				}
			}
		}
	}
	
	// Remove duplicated IPs from ip vector of this event
	void removeDuplicateIP(){
		if( ip == null )
			return;
		
		for( int i=0; i<ip.size(); i++ ){
			int[] tmp1 = ip.get(i);

			for( int j=i+1; j<ip.size(); j++ ){
				int[] tmp2 = ip.get(j);
				boolean dup = true;
				for( int k=0; k<tmp1.length; k++ ){
					if( tmp1[k] != tmp2[k] ){
						dup = false;
						break;
					}
				}
				if( dup ){
					ip.remove(j);
					j--;
				}
			}
		}
	}
	
	void removeDuplicateEmailID(){
		if( emailID == null )
			return;
		for( int i=0; i<emailID.size(); i++ ){
			String tmp1 = emailID.get(i);
			for( int j=i+1; j<emailID.size(); j++ ){
				String tmp2 = emailID.get(j);
				if( tmp1.compareTo(tmp2)==0 ){
					emailID.remove(j);
					j--;
				}
			}
		}
	}
	
	// return index of ip vector if this event has the value of the input ip, otherwise return -1
	int checkIP(int[] ip, String ip_class){
		if( this.ip == null )
			return -1;
		
		int classToCompare = -1;
		switch(ip_class){
			case "A":
			case "a":
				classToCompare = 0;
				break;
			case "B":
			case "b":
				classToCompare = 1;
				break;
			case "C":
			case "c":
				classToCompare = 2;
				break;
			case "D":
			case "d":
				classToCompare = 3;
				break;
		}
		for( int i=0; i<this.ip.size(); i++ ){
			int [] this_ip = this.ip.get(i);
			boolean hit = true;
			for( int j=0; j<=classToCompare; j++ ){
				if( this_ip[j] != ip[j] ){
					hit = false;
					break;
				}
			}
			if( hit )
				return i;
		}
		return -1;
	}
	
	// Return index of url vector if this event has the value of the input url, otherwise return -1
	int checkURL(String url){
		if( this.url == null )
			return -1;
		for( int i=0; i<this.url.size(); i++ ){
			if( this.url.get(i).equalsIgnoreCase(url) )
				return i;
		}
		return -1;
	}
	
	// Return index of clue vector if this event has the value of the input clue, otherwise return -1
	int checkClue(String clue){
		if( this.clue == null )
			return -1;
		for( int i=0; i<this.clue.size(); i++ ){
			if( this.clue.get(i).equalsIgnoreCase(clue) )
				return i;
		}
		return -1;
	}
	
	int checkEmailID(String id){
		if( emailID == null )
			return -1;
		for( int i=0; i<this.emailID.size(); i++ ){
			if( this.emailID.get(i).equalsIgnoreCase(id) )
				return i;
		}
		return -1;
	}

	// Return index of yara vector if a file exist of which the portion of matching yara detection rules exceed [yara_match_threshold]
	int checkYara(Vector<String> yara){
		int thresh = (int) Math.ceil(yara.size()*yara_match_threshold);
		for( int i=0; i<this.yara.size(); i++ ){
			Vector<String> rule_set = this.yara.get(i);
			int matchingRuleCount = 0;
			for( int j=0; j<rule_set.size(); j++ ){
				String rule1 = rule_set.get(j);
				for( int k=0; k<yara.size(); k++ ){
					String rule2 = yara.get(k);
					if( rule1.equals(rule2) )
						matchingRuleCount++;
				}
			}
			if( matchingRuleCount >= thresh )
				return i;
		}
		
		return -1;
	}
	
	int checkDirectory(String dir){
		if( this.directory == null )
			return -1;
		for( int i=0; i<this.directory.size(); i++ ){
			if( this.directory.get(i).equalsIgnoreCase(dir) )
				return i;
		}
		return -1;
	}
	
	// If a type of attribute should not be treated as String(Text) type, it should be declared in this function as IP and URL.
	static String getAttributeType(String type){
		if( type.contains("ip")) return "IP";
		if( type.contains("url")) return "URL";
		if( type.contains("domain")) return "URL";
		if( type.contains("yara")) return "YARA";
		int index1 = type.indexOf('@');
		int index2 = type.indexOf('.', index1);
		if( index1 > 0 && index1 < index2 )
			return "EMAIL_ID";

		for( char i='a'; i<='z'; i++ ){
			if( type.toLowerCase().contains(i+":\\") || type.contains(i+":\\\\") ){
				return "DIR";
			}
		}
		
		
		String[] tmp = type.split(date_delims);
		if( tmp.length == 3 )
			return "DATE";
		return "TEXT";
	}
	
	// Define categories and types of attributes to be ignored processing
	static boolean isIgnorableMISPData( String type ){
		// Ignored MISP attribute categories
		if( type.equals("Attribution") || type.equals("Internal reference") )
			return true;
		// Ignored MISP attribute types
		if( type.equals("malware-sample") || type.equals("comment") || type.contains("filename")|| type.equals("snort") )
			return true;
		return false;
	}
	
	@SuppressWarnings("unchecked")
	static Event cloneEvent(Event e){
		Event retval = new Event();
		retval.id = e.id;
		retval.eventName = e.eventName;
		retval.eventTime = (Date)e.eventTime.clone();
		
		if(e.ip != null )
			retval.ip = (Vector<int[]>)e.ip.clone();
		if(e.url != null )
			retval.url = (Vector<String>)e.url.clone();
		if(e.clue != null )
			retval.clue = (Vector<String>)e.clue.clone();
		if(e.yara != null )
			retval.yara = (Vector<Vector<String>>)e.yara.clone();
		if(e.emailID != null )
			retval.emailID = (Vector<String>)e.emailID.clone();
		return retval;
	}
	
	static Event findEvent(String eid, Vector<Event> e){
		for( int i=0; i<e.size(); i++ ){
			if( eid.equals(e.get(i).id) )
				return cloneEvent(e.get(i));
		}
				
		return null;
	}
}
