import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.util.HashMap;
import java.util.Vector;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Main {
	static boolean anonymize = true;
	static boolean drawRelation = false;
	static boolean debug = true;
	static float anonymizeRate = 0.50f;
	static String nodeSize = "20"; // 20 or 30
	static String textSize = "10"; // 10 or 13
	//static String textStyle = "text-style:bold;"; // "text-style:bold;" or ""(null); 
	static String textStyle = "normal"; // "text-style:bold;" or ""(null); 
	static String dataPath = "";
	static int screenWidth, screenHeight;
    
	public static void main( String[] args ){

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = (int)screenSize.getWidth();
        screenHeight = (int)screenSize.getHeight();
        
		Main m = new Main();
		try {
			m.setConfig("config.xml");
			m.run( args );
			//m.test();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	Detective CyTec;
	HashMap<String, Vector<String>> tmp = new HashMap<String, Vector<String>>();
	Main(){
		CyTec = new Detective();
	}
	
	void test(){
	}
     
	void run( String[] args ) throws Exception{
		int numOfInitialEvents = args.length;
		int initialEventFound = 0;
		File dataFile = new File(Main.dataPath);
				
		//Get the DOM Builder Factory
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//Get the DOM Builder
		DocumentBuilder builder = factory.newDocumentBuilder();
		//Load and Parse the XML document
		//document contains the complete XML as a Tree.
		Document document = builder.parse(dataFile);
		
		//Iterating through the nodes and extracting the data.
		NodeList nodeList = document.getDocumentElement().getChildNodes();
		CyTec.setEventDatabase(nodeList);
		//System.out.println("# of Events in dataset : "+ nodeList.getLength());
		System.out.print("* The Events to be investigated : ");
		for( int j=0; j<args.length; j++ )
			System.out.print("#"+ args[j]+" ");
		System.out.println();
		
		// Search information of initial events
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node event = nodeList.item(i);
			if( event.getNodeType() != Node.ELEMENT_NODE ) 
				continue;
			Element elem = (Element) event;
			String id = elem.getElementsByTagName("id").item(0).getTextContent();
			for( int j=0; j<args.length; j++ ){
				// Initial Event found!! Set event information in Detective Class
				if( id.equals(args[j]) ){
					initialEventFound++;
					CyTec.addInitialEvent(event);
					break;
				}
			}
			
			if( initialEventFound == numOfInitialEvents )
				break;
		}
		
		// Start running investigation on dataset about initial events
		CyTec.runInvestigation();
	}
	
	void setConfig(String config_file) throws Exception{

		File dataFile = new File(config_file);
				
		//Get the DOM Builder Factory
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//Get the DOM Builder
		DocumentBuilder builder = factory.newDocumentBuilder();
		//Load and Parse the XML document
		//document contains the complete XML as a Tree.
		Document document = builder.parse(dataFile);
		NodeList nodeList = document.getFirstChild().getChildNodes();

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node item  = nodeList.item(i);
			if( item.getNodeType() != Node.ELEMENT_NODE ) 
				continue;
			String item_name = item.getNodeName();
			String item_value = item.getTextContent();
			if( item_name.equals("debug") ){
				if( item_value.equals("true") )
					Main.debug = true;
				else
					Main.debug = false;
				continue;

			}else if( item_name.equals("anonymize") ){
				if( item_value.equals("true") )
					Main.anonymize = true;
				else
					Main.anonymize = false;
				continue;
			}else if( item_name.equals("anonymize_rate") ){
				Main.anonymizeRate = Float.parseFloat(item_value);
				continue;
			}else if( item_name.equals("draw_relation") ){
				if( item_value.equals("true") )
					Main.drawRelation = true;
				else
					Main.drawRelation = false;
				continue;
			}else if( item_name.equals("node_size") ){
				if( item_value.equals("S") || item_value.equals("small") )
					Main.nodeSize = "15";
				else if( item_value.equals("M") || item_value.equals("medium") )
					Main.nodeSize = "20";
				else if( item_value.equals("L") || item_value.equals("large") )
					Main.nodeSize = "30";
				continue;
			}else if( item_name.equals("text_size") ){
				if( item_value.equals("S") || item_value.equals("small") )
					Main.textSize = "10";
				else if( item_value.equals("M") || item_value.equals("medium") )
					Main.textSize = "13";
				else if( item_value.equals("L") || item_value.equals("large") )
					Main.textSize = "16";
				continue;
			}else if( item_name.equals("db_path") ){
				Main.dataPath = item_value;
				continue;
			}
		}
	}
}
