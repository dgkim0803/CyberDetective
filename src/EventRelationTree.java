import java.awt.BorderLayout;
import java.awt.Component;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JPanel;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

// Event Relation Tree(ERT)
public class EventRelationTree extends JPanel implements ViewerListener, Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	EventNode root = null;
	HashMap<String, String> anonymizeMap, deanonymizeMap;
	boolean mouseEventLoop = true;
	boolean buttonClicked = false;
	Graph ERTGraph = null;
	View view;
	
	EventRelationTree(){
	}
	
	EventRelationTree(Event r){
		root = new EventNode(r);
		if( Main.anonymize ){
			anonymizeMap = new HashMap<String, String>();
			deanonymizeMap = new HashMap<String, String>();
		}
	}
	
	// Add child node under the root node.
	public void addChild(Event child, Vector<Vector<String[]>> relation){
		root.addChild(child, relation);
	}

	// Add child node under parent node. Return true if this tree has the parent node, otherwise return false. 
	protected EventNode addChild(Event parent, Event child, Vector<Vector<String[]>> relation){
//		if( hasEventRelation(parent, child) )
//			return null;
		EventNode p = null;
		if( parent.id.equals(root.eid))
			p = root;
		else
			p = root.getSibling(parent.id);	
		if( p != null )
			return p.addChild(child, relation);
		return null;
	}
	
	
	protected void addRelation(Event event1, Event event2, Vector<Vector<String[]>> relation){
		EventNode e1 = getEventNode(event1);
		e1.relations.addAll(relation.get(0));
		EventNode e2 = getEventNode(event2);
		e2.relations.addAll(relation.get(1));
	}
	
	protected boolean hasEventRelation(Event event1, Event event2){
		EventNode e1 = getEventNode(event1);
		if( e1 == null )
			return false;
		
		for( int j=0; j<e1.relations.size(); j++ ){
			String [] r = e1.relations.get(j);
			if( r[0].equals(event2.id) )
				return true;
		}
		
		return false;
	}

	protected boolean hasEventRelationChain(Event event1, Event event2){
		EventNode e1 = getEventNode(event1);
		if( e1 == null )
			return false;
		
		for( int j=0; j<e1.relations.size(); j++ ){
			String [] r = e1.relations.get(j);
			if( r[0].equals(event2.id) )
				return true;
		}
		
		Iterator<String> keys  = e1.children.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			EventNode c = e1.children.get(key);
			if( hasEventRelationChain(c, event2) )
				return true;
		}
		
		return false;
	}

	protected boolean hasEventRelationChain(EventNode e1, Event event2){
		
		for( int j=0; j<e1.relations.size(); j++ ){
			String [] r = e1.relations.get(j);
			if( r[0].equals(event2.id) )
				return true;
		}

		
		Iterator<String> keys  = e1.children.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			EventNode c = e1.children.get(key);
			if( hasEventRelationChain(c, event2) )
				return true;
		}
		
		return false;
	}
	
	protected EventNode getEventNode(Event e){
		return getEventNode(e.id);
	}
	
	protected EventNode getEventNode(String eid){
		return root.getSibling(eid);
	}
	
	// Check if event node "p" has event "e" as a children.
	protected boolean hasEventNode(Event e, EventNode p){
		return hasEventNode(e.id, p);
	}
	
	// Check if event node "p" has event of which eid is "eid". If "p" is null, "eid" is checked from root node.
	protected boolean hasEventNode(String eid, EventNode p){
		if( p == null )
			return root.hasSibling(eid);
		else
			return p.hasSibling(eid);
	}
	
	protected Vector<EventNode> getRelatedEventNode(EventNode e){
		Vector<EventNode> retval = null;
		Vector<String> checkedEID = new Vector<String>();
		for( int i=0; i<e.relations.size(); i++ ){
			String r[] = e.relations.get(i);
			boolean checked = false;
			for( int j=0; j<checkedEID.size(); j++ ){
				if( checkedEID.get(j).equals(r[0]))
					checked = true;
			}
			if( checked )
				continue;
			EventNode tmp = getEventNode(r[0]);
			checkedEID.add(r[0]);
			if( tmp != null ){
				if( retval == null )
					retval = new Vector<EventNode>();
				retval.add(tmp);
			}
		}
		return retval;
	}
	
	// Make a subtree such that event node "e" is set to the root of the subtree by converting ancestors of "e" just before the original root as siblings of "e".
	protected EventNode makeSubtree(EventNode e){
		if( e.parent.eid.equals(root.eid) )
			return e;
		
		boolean repeat = true;
		EventNode center = e, p = e.parent, tmp=e;
		do{
			if( p.parent.eid.equals(root.eid) )
				repeat = false;
			p.children.remove(center.eid);	// 1
			tmp.children.put(p.eid, p); 	// 2
			if( repeat ){
				tmp = p.parent;				// 3
			}
			p.parent = center;				// 4
			p = tmp;						// 5
			center = p;						// 6
			if( repeat )
				p = tmp;
		}while(repeat);
		
		return e;
	}
	
	EventTransitionGraph transform(){
		return new EventTransitionGraph(this);
	}
	
	public void deepening(){
		System.out.println("\n### Deepening ERT ###"); 
		Iterator<String> keys  = root.children.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			EventNode c = root.children.get(key);
			if( c.children.size() == 1 )
				continue;
		}
	}
	
	public	void deepening(EventNode e){
		Iterator<String> keys  = e.children.keySet().iterator();
		if( e.children.size() == 1 )
			deepening(e.children.get(keys.next()));

		while( keys.hasNext() ){
			String key = keys.next();
			EventNode c = e.children.get(key);
			Vector<EventNode> r = getRelatedEventNode(c);
			// check event nodes in r belong to branches different to event c, set them in the same branch.
			for( int i=0; i<r.size(); i++ ){
				if( c.hasChild(r.get(i).eid) )
					continue;
				makeSubtree(c);
				EventNode leaf = c.getLeftMostLeafNode();
				leaf.children.put(c.eid, c);
				c.parent = leaf;
			}
		}
	}
	
	// Draw current ERT
	@SuppressWarnings("deprecation")
	public void draw(){
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		
		if( ERTGraph == null ){
			ERTGraph = new MultiGraph("Event Relation Tree(ERT)");
		
			if( Main.anonymize )
				ERTGraph.addAttribute("ui.default.title", "Event Relation Tree(ERT) - "+ anonymizeString(root.node.eventName));
			else
				ERTGraph.addAttribute("ui.default.title", "Event Relation Tree(ERT) - "+ root.node.eventName);
			ERTGraph.setStrict(false);
			ERTGraph.setAutoCreate(true);
		}
		
		ERTGraph.addNode("[Event]"+root.eid);
		Node node = ERTGraph.getNode("[Event]"+root.eid);
		String date = root.node.eventTime.getYear()+"-"+(root.node.eventTime.getMonth()+1)+"-"+root.node.eventTime.getDate();
		if( Main.anonymize )
			node.addAttribute("ui.label", "Event #"+root.eid+"\\"+date);
		else
			node.addAttribute("ui.label", "Event #"+root.eid+"\\"+root.node.eventName+"\\"+date);

		//node.addAttribute("ui.style", "fill-color: red; text-alignment:above;");
		if( root.node.eventName.contains("메일") )
			node.addAttribute("ui.style", "fill-mode:image-scaled; fill-image:url('email.jpg'); size:"+Main.nodeSize+"px; text-alignment:above; shadow-mode:plain; shadow-color:red; shadow-offset:0; text-size:"+Main.textSize+"; text-style:"+Main.textStyle+";");
		else
			node.addAttribute("ui.style", "fill-mode:image-scaled; fill-image:url('malware.jpg'); size:"+Main.nodeSize+"px; text-alignment:above; shadow-mode:plain; shadow-color:red; shadow-offset:0; text-size:"+Main.textSize+"; text-style:"+Main.textStyle+";");
		
		draw(ERTGraph,root);
		
		if( Main.drawRelation )
			drawRelation(root, ERTGraph);
		
		//Viewer viewer = ERTGraph.display();
		Viewer viewer = new Viewer(ERTGraph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
		viewer.enableAutoLayout();
		view = viewer.addDefaultView(false);
		viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.EXIT);
			
		ViewerPipe viewerPipe = viewer.newViewerPipe();
		viewerPipe.addViewerListener(this);
		viewerPipe.addSink(ERTGraph);
		
		this.setLayout(new BorderLayout());
		this.add((Component) view, BorderLayout.CENTER);
		this.setVisible(true);
		this.setSize(Main.screenWidth, Main.screenHeight);
		/*
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		
        if( Main.anonymize )
        	this.setTitle("Event Relation Tree(ERT) - "+ anonymizeString(root.node.eventName));
		else
			this.setTitle("Event Relation Tree(ERT) - "+ root.node.eventName);
        */
		
		while(mouseEventLoop){
			viewerPipe.pump();
		}
	}
	
	@SuppressWarnings("deprecation")
	protected void draw(Graph g, EventNode e){
		Iterator<String> keys = e.children.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			EventNode c = e.children.get(key);
			g.addNode("[Event]"+c.eid);
			g.addEdge("[Event]"+e.eid+"-"+"[Event]"+c.eid, "[Event]"+e.eid, "[Event]"+c.eid);

			Node node = g.getNode("[Event]"+c.eid);
			String date = c.node.eventTime.getYear()+"-"+(c.node.eventTime.getMonth()+1)+"-"+c.node.eventTime.getDate();
			if( Main.anonymize )
				node.addAttribute("ui.label", "Event #"+c.eid+"\\"+date);
			else
				node.addAttribute("ui.label", "Event #"+c.eid+"\\"+c.node.eventName+"\\"+date);

			//node.addAttribute("ui.style", "text-alignment:above;");
			if( c.node.eventName.contains("메일") )
				node.addAttribute("ui.style", "fill-mode:image-scaled; fill-image:url('email.jpg'); size:"+Main.nodeSize+"px; text-alignment:above; text-size:"+Main.textSize+"; text-style:"+Main.textStyle+";");
			else
				node.addAttribute("ui.style", "fill-mode:image-scaled; fill-image:url('malware.jpg'); size:"+Main.nodeSize+"px; text-alignment:above; text-size:"+Main.textSize+"; text-style:"+Main.textStyle+";");
			Edge edge = g.getEdge("[Event]"+e.eid+"-"+"[Event]"+c.eid);
			edge.addAttribute("ui.style", "size:2px;");
			draw(g,c);
		}
	}
	

	protected void drawRelation(EventNode e, Graph g){
		Vector<String[]> r = e.relations;
		for( int i=0; i<r.size(); i++ ){
			String[] elem = r.get(i);
			
			if( !elem[1].equals("YARA") ){

				if( Main.anonymize ){
					String anonVal = getAnonymizedString(elem[2]);
					if( anonVal.equals("") )
						elem[2] = anonymizeString(elem[2]);
					else
						elem[2] = anonVal;
					
					anonVal = getAnonymizedString(elem[3]);
					if( anonVal.equals("") )
						elem[3] = anonymizeString(elem[3]);
					else
						elem[3] = anonVal;
				}
				
				if( g.getNode("[Attribute]"+elem[2]) == null ){
					g.addNode("[Attribute]"+elem[2]);
					Node node = g.getNode("[Attribute]"+elem[2]);
					node.addAttribute("ui.label", elem[2]);
					node.setAttribute("ui.style", "fill-color:green; text-alignment:above; text-size:"+Main.textSize+"; text-style:"+Main.textStyle+";");
				}
				
				if( g.getEdge("[Event]"+e.eid+"-"+"[Attribute]"+elem[2]) == null &&  g.getEdge("[Attribute]"+elem[2]+"-"+"[Event]"+e.eid) == null){
					g.addEdge("[Event]"+e.eid+"-"+"[Attribute]"+elem[2], "[Event]"+e.eid, "[Attribute]"+elem[2]);
					Edge edge = g.getEdge("[Event]"+e.eid+"-"+"[Attribute]"+elem[2]);
					edge.addAttribute("ui.style", "fill-color:green;");
				}
				
				if( g.getNode("[Attribute]"+elem[3]) == null ){
					g.addNode("[Attribute]"+elem[3]);
					Node node = g.getNode("[Attribute]"+elem[3]);
					node.addAttribute("ui.label", elem[3]);
					node.setAttribute("ui.style", "fill-color:green; text-alignment:above; text-size:"+Main.textSize+"; text-style:"+Main.textStyle+";");
				}
				if( g.getEdge("[Event]"+elem[0]+"-"+"[Attribute]"+elem[3]) == null && g.getEdge("[Attribute]"+elem[3]+"-"+"[Event]"+elem[0]) == null){
					g.addEdge("[Event]"+elem[0]+"-"+"[Attribute]"+elem[3], "[Event]"+elem[0], "[Attribute]"+elem[3]);
					Edge edge = g.getEdge("[Event]"+elem[0]+"-"+"[Attribute]"+elem[3]);
					edge.addAttribute("ui.style", "fill-color:green;");
				}
				
				if( !elem[2].equals(elem[3]) && g.getEdge("[Attribute]"+elem[2]+"-"+"[Attribute]"+elem[3])==null && g.getEdge("[Attribute]"+elem[3]+"-"+"[Attribute]"+elem[2])==null ){
					g.addEdge("[Attribute]"+elem[2]+"-"+"[Attribute]"+elem[3], "[Attribute]"+elem[2], "[Attribute]"+elem[3]);
					Edge edge = g.getEdge("[Attribute]"+elem[2]+"-"+"[Attribute]"+elem[3]);
					edge.addAttribute("ui.style", "fill-color:green;");
				}
			}else if( g.getEdge("[Event]"+e.eid+"-YARA-"+"[Event]"+elem[0]) == null && g.getEdge("[Event]"+elem[0]+"YARA"+"[Event]"+e.eid) == null){
					g.addEdge("[Event]"+e.eid+"-YARA-"+"[Event]"+elem[0], "[Event]"+e.eid, "[Event]"+elem[0]);
					Edge edge = g.getEdge("[Event]"+e.eid+"-YARA-"+"[Event]"+elem[0]);
					edge.addAttribute("ui.style", "fill-color:orange;");
			}
		}

		Iterator<String> keys = e.children.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			EventNode c = e.children.get(key);
			drawRelation(c,g);
		}
	}
	
	public void addNode(EventNode n, EventNode p, Graph g){
		g.addNode("[Event]"+n.eid);
		if( p != null )
			g.addEdge("[Event]"+n.eid+"-"+"[Event]"+p.eid, "[Event]"+n.eid, "[Event]"+p.eid);
		
		Iterator<EventNode> iter = n.children.values().iterator();
		for( EventNode e=iter.next(); iter.hasNext(); e=iter.next() )
			addNode(e,n,g);
	}
	

	String anonymizeString(String r){
		char[] retval = r.toCharArray();
		int anony = (int)Math.ceil(r.length()*Main.anonymizeRate);
		int counter = 0;
		while(true){
			int loc = (int)(Math.random()*r.length());
			if( retval[loc]=='*' || retval[loc]=='.' )
				continue;
			retval[loc] = '*';
			counter++;
			if( counter == anony )
				break;
		}
		
		String s_ret = "";
		for( int i=0; i<r.length(); i++ )
			s_ret += retval[i];
		
		anonymizeMap.put(r, s_ret);
		deanonymizeMap.put(s_ret, r);
		return s_ret;
	}
	
	String getAnonymizedString(String s){
		if( anonymizeMap.containsKey(s) )
			return anonymizeMap.get(s);
		else
			return "";
	}

	@Override
	public void buttonPushed(String id) {
		resetGraphStyle();
		highlight(id);
	}

	@Override
	public void buttonReleased(String id) {
	}

	@Override
	public void viewClosed(String id) {
		mouseEventLoop = false;
	}
/*
	@Override
	public void keyTyped(KeyEvent e) {
		System.out.println("ERT key typed!");
		if( e.getKeyCode() == 0 ){
			System.out.println(e.getKeyCode());
			resetGraphStyle();
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}
	*/
	public void highlight(String id){
		
		String highlightTextSize = Integer.toString((int)(Math.ceil(Integer.parseInt(Main.textSize))*1.2));
		String highlightNodeSize = Integer.toString((int)(Math.ceil(Integer.parseInt(Main.nodeSize))*1.5));

		EventNode centerERTNode = getEventNode(id.substring(id.indexOf(']')+1)); 
		Iterator<Node> nodeIter = ERTGraph.getNodeIterator();
		do{
			Node currGraphNode = nodeIter.next();
			String currNodeID = currGraphNode.getId();
			if( currNodeID.contains("[Event]") ){
				String pureID = currNodeID.substring(currNodeID.indexOf(']')+1); 
				if( centerERTNode.eid.equals(pureID) || currGraphNode.hasEdgeBetween(id) ){
					currGraphNode.addAttribute("ui.style", "text-color: blue; text-size:"+highlightTextSize+";size:"+highlightNodeSize+"px; text-style:bold;");
				}else{
					int i = 0;
					for(; i<centerERTNode.relations.size(); i++){
						String relatedNodeID = centerERTNode.relations.get(i)[0];
						if( pureID.equals(relatedNodeID) ){
							currGraphNode.addAttribute("ui.style", "text-color: blue; text-size:"+highlightTextSize+";size:"+highlightNodeSize+"px; text-style:bold;");
							break;
						}
					}
					if( i == centerERTNode.relations.size() )
						currGraphNode.addAttribute("ui.style", "text-color: gray;");
				}
				
			}else if( currGraphNode.hasEdgeBetween(id) ){
				currGraphNode.addAttribute("ui.style", "text-color: red; text-style: bold;");
			}else{
				currGraphNode.addAttribute("ui.style", "text-color: gray;");
			}
		}while( nodeIter.hasNext() );
	}
	
	public void resetGraphStyle(){

		Iterator<Node> nodeIter = ERTGraph.getNodeIterator();
		do{
			Node currGraphNode = nodeIter.next();
//			String currNodeID = currGraphNode.getId();
			if( !currGraphNode.getId().contains("[Attribute]") )
				currGraphNode.addAttribute("ui.style", "text-size:"+Main.textSize+";size:"+Main.nodeSize+"px; text-color: black; text-style:"+Main.textStyle+";");
			else
				currGraphNode.addAttribute("ui.style", "text-color: black; text-style: normal;");

				
			/*
			Iterator<Edge> edges = currGraphNode.getEdgeIterator();
			do{
				Edge e = edges.next();
				if(e.getId().contains("-"))
					continue;
				e.addAttribute("ui.style", "size:2px;");
			}while(edges.hasNext());
			*/
		}while( nodeIter.hasNext() );
	}

	@Override
	public void run() {
		draw();
	}
	
}

class EventNode {
	String eid;
	Event node;
	EventNode parent = null;
	HashMap<String, EventNode> children = null;
	Vector<String[]> relations = null; // the String element will contain [Related event ID, Type, Value[]]
	
	EventNode(){
	}
	
	EventNode(String id){
		children = new HashMap<String, EventNode>();
		relations = new Vector<String[]>();
		eid = id;
	}
	
	EventNode(Event e){
		this(e.id);
		node = Event.cloneEvent(e);
	}
	
	EventNode(Event e, EventNode p){
		this(e);
		parent = p;
	}
	
	// Add a child node of the root node
	public EventNode addChild(Event e, Vector<Vector<String[]>> relation){
		this.relations.addAll(relation.get(0));
		EventNode en = new EventNode(e, this);
		children.put(en.eid, en);
		en.relations.addAll(relation.get(1));
		return en;
	}
	
	protected void addRelation(String[] elem){
		relations.add(elem);
	}
	
	@SuppressWarnings("unchecked")
	public static EventNode clone(EventNode input){
		EventNode e = new EventNode(input.node, input.parent);
		e.children = (HashMap<String, EventNode>) input.children.clone();
		e.relations = (Vector<String[]>) input.relations.clone();
		return e;
	}
	
	protected EventNode getSibling(String id){
		if( children.size() == 0 )
			return null;
		if( eid.equals(id) )
			return this;
		if( children.containsKey(id) )
			return children.get(id);
		
		Iterator<String> keys = children.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			EventNode e = children.get(key);
			if( e.eid.equals(id) )
				continue;
			EventNode retval = e.getSibling(id); 
			if( retval != null )
				return retval; 
		}
		return null;
	}
	
	protected EventNode getAncestor(String id){
		if( isRootNode() )
			return null;
		if( parent.eid.equals(id) )
			return parent;
		return parent.getAncestor(id);
	}
	
	protected EventNode getLeftMostLeafNode(){
		if( this.isLeafNode() )
			return this;

		Iterator<EventNode> iter = this.children.values().iterator();
		return iter.next().getLeftMostLeafNode();
	}
	
	// If this event node has "value" in relations to its parent return the index of the value in relations vector, otherwise return -1.
	int checkRelation(String value){
		int retval = -1;
		for( int i=0; i<relations.size(); i++ ){
			String[] tmp = relations.get(i);
			for( int j=2; j<tmp.length; j++ ){
				if( tmp[j].equals(value) )
					return i;
			}
		}
		return retval;
	}
	
	boolean hasChild(String cid){
		return children.containsKey(cid);
	}
	
	boolean hasSibling(String id){
		if( this.eid.equals(id) )
			return true;
		if( children.containsKey(id) )
			return true;
		if( isLeafNode() )
			return false;
		boolean retval = false;
		Iterator<String> keys = children.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			EventNode e = children.get(key);
			retval = e.hasSibling(id);
			if( retval )
				return true;
		}
		return false;
	}
	
	boolean hasAncestor(String id){
		if( isRootNode() )
			return false;
		if( this.parent.eid.equals(id) )
			return true;
		return this.parent.hasAncestor(id);
	}
	
	boolean isRootNode(){
		if( parent == null )
			return true;
		else
			return false;
	}
	
	boolean isLeafNode(){
		if( children.size() == 0 )
			return true;
		else
			return false;
	}
	
	boolean isLinkedNode(String id){
		if( parent.eid.equals(id) )
			return true;
		if( children.containsKey(id) )
			return true;
		
		return false;
	}
}