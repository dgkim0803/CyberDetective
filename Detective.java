import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import org.w3c.dom.*;

public class Detective extends JFrame implements KeyEventDispatcher{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Vector<Event> initialEvents;
	NodeList db;
	EventRelationTree tree;
	EventTransitionGraph graph;
	JTabbedPane jtp;
	
	Detective(){
		initialEvents = new Vector<Event>();
	}
	
	Detective(NodeList e){
		this();
		db = e;
	}
	
	void setEventDatabase(NodeList e){
		db = e;
	}
	
	// Set initial event information in Event class and add it in initialEvents vector.
	void addInitialEvent(Node event){
		Event e = new Event(event);
		initialEvents.add(e);
		if( Main.debug )
			e.printEventInfo();
	}
	
	// Investigate relation of initial events and events in the database.
	// If any relation is found, additional relations are investigated between the related event and other events in the database.
	void runInvestigation() throws InterruptedException{
		for( int i=0; i<initialEvents.size(); i++ ){
			Event initial_event = initialEvents.get(i);
			tree = new EventRelationTree(initial_event);
			if( Main.debug )
				System.out.println("\n### Running investigation on Event "+tree.root.eid+" ###");
			
			// compare information of all events
			for (int n = 0; n<db.getLength(); n++) {
				Node eventNode = db.item(n);
				if( eventNode.getNodeType() != Node.ELEMENT_NODE ) 
					continue;
				Event event = new Event(eventNode);
				runInvestigation(initial_event, event, tree);
			}//end of for (int n = 0; n<events.getLength(); n++)

			//tree.draw();
			Thread ertThread = new Thread(tree);
			ertThread.start();
			
			graph = new EventTransitionGraph(tree);
			graph.transform(null, true);
			//graph.draw();
			Thread etgThread = new Thread(graph);
			etgThread.start();
			
			Thread.sleep(500);	// without this sleep sentence, the size of frame is set prior to panel size of ERT and ETG.
			initDisplay();
			//etg.add(graph);
		} //end of for( int i=0; i<initialEvents.size(); i++ )
		
	}
	
	// Find relation of two events. If found, add the events in the tree(ERT) such that event1 as a parent and event2 as a child node.
	// Also, recursively find additional relations of event2 in the events in database.
	void runInvestigation(Event event1, Event event2, EventRelationTree tree){
		if(  tree.hasEventRelationChain(event1, event2) )
			return;
		
		Vector<Vector<String[]>> r = findEventRelation(event1, event2);
		if( r != null ){
			if( Main.debug )
				System.out.println("# Relation found : Event "+event1.id+" - Event "+event2.id);

			if( event1.id.equals(tree.root.eid) ){
				tree.addChild(event2, r);
			}
			else{
				if( tree.hasEventNode(event2, null) ){  // in this case, if two events already has ancestor-sibling relation.
					               						// Therefore, only relation is added to each nodes.
					tree.addRelation(event1, event2, r);
					return;
				}
				else{
					tree.addChild(event1, event2, r);
				}
			}
			
			for (int n = 0; n<db.getLength(); n++) {
				Node eventNode = db.item(n);
				if( eventNode.getNodeType() != Node.ELEMENT_NODE ) 
					continue;
				Event event = new Event(eventNode);
				if( event1.id.equals(event.id) || event2.id.equals(event.id) )
					continue;
				runInvestigation(event2, event, tree);
			}
		}
		
	}
	
	// Find relations of two events
	Vector<Vector<String[]>> findEventRelation(Event event1, Event event2){
		Vector<Vector<String[]>> relation = null;
		if( event1.id.equals(event2.id))
			return null;
		
		//check IP relation
		if( event1.ip!=null && event2.ip!=null ){
			for( int index1=0; index1<event1.ip.size(); index1++ ){
				int index2 = event2.checkIP(event1.ip.get(index1), "C");
				if( index2 != -1 ){
					if( relation == null ){
						relation = new Vector<Vector<String[]>>();
						relation.add(new Vector<String[]>());
						relation.add(new Vector<String[]>());
					}
					String[] elem1 = new String[4];
					String[] elem2 = new String[4];
					int[] ip1 = event1.ip.get(index1);
					int[] ip2 = event2.ip.get(index2);
					elem1[0] = event2.id;
					elem1[1] = "IP";
					elem1[2] = ip1[0]+"."+ip1[1]+"."+ip1[2]+"."+ip1[3];
					elem1[3] = ip2[0]+"."+ip2[1]+"."+ip2[2]+"."+ip2[3];
					relation.get(0).add(elem1);
					elem2[0] = event1.id;
					elem2[1] = "IP";
					elem2[2] = ip2[0]+"."+ip2[1]+"."+ip2[2]+"."+ip2[3];
					elem2[3] = ip1[0]+"."+ip1[1]+"."+ip1[2]+"."+ip1[3];
					relation.get(1).add(elem2);
				}
			}
		}
		
		//Check URL relation
		if( event1.url!=null && event2.url!=null ){
			for( int index1=0; index1<event1.url.size(); index1++ ){
				int index2 = event2.checkURL(event1.url.get(index1));
				if( index2 != -1 ){
					if( relation == null ){
						relation = new Vector<Vector<String[]>>();
						relation.add(new Vector<String[]>());
						relation.add(new Vector<String[]>());
					}
					String[] elem1 = new String[4];
					String[] elem2 = new String[4];
					elem1[0] = event2.id;
					elem1[1] = "URL";
					elem1[2] = event1.url.get(index1);
					elem1[3] = event2.url.get(index2);
					relation.get(0).add(elem1);
					elem2[0] = event1.id;
					elem2[1] = "URL";
					elem2[2] = event2.url.get(index2);
					elem2[3] = event1.url.get(index1);
					relation.get(1).add(elem2);
				}
			}
		}
		
		//Check clue relation
		if( event1.clue!=null && event2.clue!=null ){
			for( int index1=0; index1<event1.clue.size(); index1++ ){
				int index2 = event2.checkClue(event1.clue.get(index1));
				if( index2 != -1 ){
					if( relation == null ){
						relation = new Vector<Vector<String[]>>();
						relation.add(new Vector<String[]>());
						relation.add(new Vector<String[]>());
					}
					String[] elem1 = new String[4];
					String[] elem2 = new String[4];
					elem1[0] = event2.id;
					elem1[1] = "TEXT";
					elem1[2] = event1.clue.get(index1);
					elem1[3] = event2.clue.get(index2);
					relation.get(0).add(elem1);
					elem2[0] = event1.id;
					elem2[1] = "TEXT";
					elem2[2] = event2.clue.get(index2);
					elem2[3] = event1.clue.get(index1);
					relation.get(1).add(elem2);
				}
			}
		}
		
		//Check clue relation
		if( event1.clue!=null && event2.clue!=null ){
			for( int index1=0; index1<event1.clue.size(); index1++ ){
				int index2 = event2.checkClue(event1.clue.get(index1));
				if( index2 != -1 ){
					if( relation == null ){
						relation = new Vector<Vector<String[]>>();
						relation.add(new Vector<String[]>());
						relation.add(new Vector<String[]>());
					}
					String[] elem1 = new String[4];
					String[] elem2 = new String[4];
					elem1[0] = event2.id;
					elem1[1] = "TEXT";
					elem1[2] = event1.clue.get(index1);
					elem1[3] = event2.clue.get(index2);
					relation.get(0).add(elem1);
					elem2[0] = event1.id;
					elem2[1] = "TEXT";
					elem2[2] = event2.clue.get(index2);
					elem2[3] = event1.clue.get(index1);
					relation.get(1).add(elem2);
				}
			}
		}

		
		//Check directory relation
		if( event1.directory!=null && event2.directory!=null ){
			for( int index1=0; index1<event1.directory.size(); index1++ ){
				int index2 = event2.checkDirectory(event1.directory.get(index1));
				if( index2 != -1 ){
					if( relation == null ){
						relation = new Vector<Vector<String[]>>();
						relation.add(new Vector<String[]>());
						relation.add(new Vector<String[]>());
					}
					String[] elem1 = new String[4];
					String[] elem2 = new String[4];
					elem1[0] = event2.id;
					elem1[1] = "DIR";
					elem1[2] = event1.directory.get(index1);
					elem1[3] = event2.directory.get(index2);
					relation.get(0).add(elem1);
					elem2[0] = event1.id;
					elem2[1] = "DIR";
					elem2[2] = event2.directory.get(index2);
					elem2[3] = event1.directory.get(index1);
					relation.get(1).add(elem2);
				}
			}
		}
		
		return relation;
	}
	

	void initDisplay(){

		jtp = new JTabbedPane(JTabbedPane.TOP);
        jtp.addTab("Event Relation Tree(ERT)", null, (Component)tree, BorderLayout.CENTER);
        jtp.addTab("Event Transition Graph(ETG)", null, (Component) graph, BorderLayout.CENTER);
        
		this.setLayout(new BorderLayout());
		this.getContentPane().add(jtp, BorderLayout.CENTER);
	    jtp.setSize(Main.screenWidth, Main.screenHeight);
		
        if( Main.anonymize )
			this.setTitle(tree.anonymizeString(tree.root.node.eventName));
		else
			this.setTitle(tree.root.node.eventName);

        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setSize(Main.screenWidth, Main.screenHeight);
        this.setVisible(true);

        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(this);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {
		
		 if (e.getID() == KeyEvent.KEY_PRESSED) {
			if( e.getKeyCode() == 27 ){	// ESC
				 if( jtp.getSelectedIndex() == 0 ){
					 tree.resetGraphStyle();
				 }else if( jtp.getSelectedIndex() == 1 ){
					 graph.resetGraphStyle();
					 
				 }
			 }
         }
         return false;
	}
	
}
