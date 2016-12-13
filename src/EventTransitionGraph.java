import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JPanel;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

public class EventTransitionGraph extends JPanel  implements ViewerListener, Runnable  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ETG_Node pivot_event = null;
	Graph ETGGraph = null;
	View view;
	boolean mouseEventLoop = true;
	
	EventTransitionGraph(EventRelationTree ert){
		pivot_event = new ETG_Node(ert.root, null);
	}
	
	void copyBranch(EventNode e){
		new ETG_Node(e, null);
		Iterator<String> keys = e.children.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			EventNode c = e.children.get(key);
			copyBranch(c);
		}
	}
	
	void transform(ETG_Node e, boolean downstream){
		if( e == null )
			e = pivot_event;
		if( downstream && e.children.size()==0 )
			return;
		else if( !downstream && e.parent.size()==0 )
			return;
		
		Iterator<String> keys;
		if( downstream )
			keys = e.children.keySet().iterator();
		else
			keys = e.parent.keySet().iterator();
			
		while( keys.hasNext() ){
			String key = keys.next();
			ETG_Node current_node, attach_node;
			
			// find node where current_node to be attached
			if( downstream )
				current_node = e.children.get(key);
			else
				current_node = e.parent.get(key);
			
			if( current_node == null )
				continue;
		
			attach_node = getNodeToAttach(e, current_node, downstream);
			
			if( attach_node == null ){
				transform(current_node, downstream);
			}else{
				flipTree(current_node, e.eid, true, null);	
				keys.remove();
				attachNode(current_node, attach_node, downstream);
				transform(current_node, !downstream);
			}
		}
	}
	
	void attachNode( ETG_Node current_node, ETG_Node attach_node, boolean downstream ){
		boolean inserted = false;
		// insert current_node between attach_node and parent/child of attach_node,
		// or just attach current_node to attach_node
		if( downstream ){
			Iterator<String> p_keys = attach_node.parent.keySet().iterator();
			while(p_keys.hasNext()){
				String p_key = p_keys.next();
				ETG_Node p = attach_node.parent.get(p_key);
				if( p.hasRelationChain(current_node, null) ){
					// disconnect original linkage
					p_keys.remove();
					p.children.remove(attach_node.eid);
					// create new linkage between inserted node(current_node) and parent node(p)
					p.children.put(current_node.eid, current_node);
					current_node.parent.put(p.eid, p);
					// create new linkage between inserted node(current_node) and attach_node
					attach_node.parent.put(current_node.eid, current_node);
					current_node.children.put(attach_node.eid, attach_node);
					inserted = true;
					break;
				}
			}
			if( !inserted ){
				attach_node.parent.put(current_node.eid, current_node);
				current_node.children.put(attach_node.eid, attach_node);
			}
		}
		else{
			Iterator<String> c_keys = attach_node.children.keySet().iterator();
			while(c_keys.hasNext()){
				String c_key = c_keys.next();
				ETG_Node c = attach_node.children.get(c_key);
				if( c.hasRelationChain(current_node, null) ){
					// disconnect original linkage
					c_keys.remove();
					c.parent.remove(attach_node.eid);
					// create new linkage between inserted node(current_node) and child node(c)
					c.parent.put(current_node.eid, current_node);
					current_node.children.put(c.eid, c);
					// create new linkage between inserted node(current_node) and attach_node
					attach_node.children.put(current_node.eid, current_node);
					current_node.parent.put(attach_node.eid, attach_node);
					inserted = true;
					break;
				}
			}
			if( !inserted ){
				attach_node.children.put(current_node.eid, current_node);
				current_node.parent.put(attach_node.eid, attach_node);
			}
		}
	}
	
	ETG_Node getNodeToAttach( ETG_Node e, ETG_Node current_node, boolean downstream ){
		ETG_Node attach_node;
		if( downstream ){
			attach_node = e.checkOlderAncestor(current_node);
			if( attach_node == null && e.children.size()>1 ){
				Iterator<String> c_keys = e.children.keySet().iterator();
				while(c_keys.hasNext()){
					String c_key = c_keys.next();
					if( c_key.equals(current_node.eid) )
						continue;
					ETG_Node c = e.children.get(c_key);
					attach_node = c.checkOlderSibling(current_node);
				}
			}
		}
		else{
			attach_node = e.checkElderSibling(current_node);
			if( attach_node == null && e.parent.size()>1 ){
				Iterator<String> p_keys = e.parent.keySet().iterator();
				while(p_keys.hasNext()){
					String p_key = p_keys.next();
					if( p_key.equals(current_node.eid) )
						continue;
					ETG_Node c = e.parent.get(p_key);
					attach_node = c.checkElderAncestor(current_node);
				}
			}
		}
		
		return attach_node;
	}
	
	
	void flipTree(ETG_Node e, String pivot_id, boolean remove_pivot, Vector<String[]> flipedNodes){
		if( flipedNodes == null )
			flipedNodes = new Vector<String[]>();
		
		for( int i=0; i<flipedNodes.size(); i++ ){
			String[] f = flipedNodes.get(i);
			if( (f[0].equals(e.eid)&&f[1].equals(pivot_id)) || (f[1].equals(e.eid)&&f[0].equals(pivot_id)) )
				return;
		}
		
		String[] f = {e.eid, pivot_id};
		flipedNodes.add(f);
		
		ConcurrentHashMap<String, ETG_Node> tmp = e.parent;
		e.parent = e.children;
		e.children = tmp;
		
		Iterator<String> keys = e.parent.keySet().iterator();
		while(keys.hasNext()){
			String key = keys.next();
			ETG_Node node = e.parent.get(key);
			flipTree(node, e.eid, false, flipedNodes);
		}
		
		keys = e.children.keySet().iterator();
		while(keys.hasNext()){
			String key = keys.next();
			ETG_Node node = e.children.get(key);
			flipTree(node, e.eid, false, flipedNodes);
		}
		
		if( remove_pivot ){
			if( e.parent.containsKey(pivot_id) )
				e.parent.remove(pivot_id);
			else if( e.children.containsKey(pivot_id) )
				e.children.remove(pivot_id);
		}
	}
	
	ETG_Node getEventNode(String id){
		ETG_Node retval = pivot_event.getAncestor(id);
		if( retval != null )
			return retval;
		
		retval = pivot_event.getSibling(id);
		if( retval != null )
			return retval;
		
		Iterator<String> keys = pivot_event.parent.keySet().iterator();
		while(keys.hasNext()){
			String key = keys.next();
			ETG_Node node = pivot_event.parent.get(key);
			retval = getEventNode(id, node, pivot_event.eid);
			if( retval != null )
				return retval;
		}
		

		keys = pivot_event.children.keySet().iterator();
		while(keys.hasNext()){
			String key = keys.next();
			ETG_Node node = pivot_event.children.get(key);
			retval = getEventNode(id, node, pivot_event.eid);
			if( retval != null )
				return retval;
		}
		
		return null;
	}
	
	ETG_Node getEventNode(String id, ETG_Node center, String fromID){
		ETG_Node retval = center.getAncestor(id);
		if( retval != null )
			return retval;
		
		retval = center.getSibling(id);
		if( retval != null )
			return retval;
		
		Iterator<String> keys = center.parent.keySet().iterator();
		while(keys.hasNext()){
			String key = keys.next();
			if( key.equals(fromID) )
				continue;
			ETG_Node node = center.parent.get(key);
			retval = getEventNode(id, node, center.eid);
			if( retval != null )
				return retval;
		}
		

		keys = center.children.keySet().iterator();
		while(keys.hasNext()){
			String key = keys.next();
			if( key.equals(fromID) )
				continue;
			ETG_Node node = center.children.get(key);
			retval = getEventNode(id, node, center.eid);
			if( retval != null )
				return retval;
		}
		
		return null;
	}
	
	
	void transform(ETG_Node e, Vector<String> checkedNode){
		if( e == null )
			e = pivot_event;
		if( checkedNode == null )
			checkedNode = new Vector<String>();
		else{
			for( int i=0; i<checkedNode.size(); i++ ){
				if( checkedNode.get(i).equals(e.eid) )
					return;
			}
		}
		checkedNode.add(e.eid);

		ConcurrentHashMap<String, ETG_Node> p = new ConcurrentHashMap<String, ETG_Node>();
		p.putAll(e.parent);
		
		boolean moved = moveUp(e);
		if( !moved )
			moved = moveDown(e);
		
		if( moved ){
			Iterator<String> keys = p.keySet().iterator();
			while( keys.hasNext() ){
				String key = keys.next();
				ETG_Node current_node = p.get(key);

				Iterator<String> c_keys = current_node.children.keySet().iterator();
				while( c_keys.hasNext() ){
					String c_key = c_keys.next();
					ETG_Node current_c = current_node.children.get(c_key);
					transform(current_c, checkedNode);	
					
				}
			}	
		}else{

			Iterator<String> keys = e.children.keySet().iterator();
			while( keys.hasNext() ){
				String key = keys.next();
				ETG_Node current_node = e.children.get(key);
				transform(current_node, checkedNode);	

			}
		}
	}
	
	// when a node is moved up, only the node is moved to an attach_node.
	boolean moveUp(ETG_Node e){

		Iterator<String> keys = e.parent.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			ETG_Node p = e.parent.get(key);
			ETG_Node attach_node = p.checkOlderAncestor(e);
			if( attach_node == null )
				continue;

			// 1. remove e from this parent
			p.children.remove(e.eid);
			e.parent.remove(p.eid);

			// 2. link children of e to the children of this parent, and remove the children from e
			p.children.putAll(e.children);
			Iterator<String> c_keys = e.children.keySet().iterator();
			while( c_keys.hasNext() ){
				String c_key = c_keys.next();
				ETG_Node c_tmp = e.children.get(c_key);
				c_tmp.parent.put(p.eid, p);
				p.children.put(c_tmp.eid, c_tmp);
				
				e.children.remove(c_tmp.eid);
				c_tmp.parent.remove(e.eid);
			}
			c_keys.remove();

			// 3. locate this parent to the children of other parents of e, and remove all parents from e
			Iterator<String> tmp_keys = e.parent.keySet().iterator();
			while( tmp_keys.hasNext() ){
				String tmp_key = tmp_keys.next();
				if( tmp_key.equals(p.eid) )
					continue;
				ETG_Node p_tmp = e.parent.get(tmp_key);
				p_tmp.children.put(p.eid, p);
				p.parent.put(p_tmp.eid, p_tmp);
				
				e.parent.remove(p_tmp.eid);
				p_tmp.children.remove(e.eid);
			}
			//tmp_keys.remove();
			
			// 4. attach e to attach_node
			attachUp(e, attach_node);
			
			return true;
		}		
		
		return false;
	}
	
	// when a node is moved down, all the other children nodes except c are moved together.
	boolean moveDown(ETG_Node e){

		Iterator<String> keys = e.children.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			ETG_Node c = e.children.get(key);
			ETG_Node attach_node = c.checkElderSibling(e);
			if( attach_node == null )
				continue;
		
			// 1. disconnect parent-child relation of e and c
			e.children.remove(c.eid);
			c.parent.remove(e.eid);
			
			// 2. make parent-child relation of c and parents of e, and remove the parents from e
			Iterator<String> tmp_keys = e.parent.keySet().iterator();
			while( tmp_keys.hasNext() ){
				String tmp_key = tmp_keys.next();
				ETG_Node p_tmp = e.parent.get(tmp_key);
				p_tmp.children.put(c.eid, c);
				c.parent.put(p_tmp.eid, p_tmp);
				
				e.parent.remove(p_tmp.eid);
				p_tmp.children.remove(e.eid);
			}
			//tmp_keys.remove();
			
			// 3. attach e to attach_node 
			attachDown(e, attach_node);
			return true;
		}
		
		
		return false;
	}
	
	
	void attachUp(ETG_Node e, ETG_Node attach_node){
		// if e has relation to a parent of attach_node, insert e between the parent and attach_node.
		// otherwise attach e as a new parent of attach_node

		Iterator<String> keys = attach_node.parent.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			ETG_Node p = attach_node.parent.get(key);
			
			// insert e
			if( p.hasRelationChain(e, null) ){
				// 1. disconnect parent-child connection of p and attach_node
				p.children.remove(attach_node.eid);
				attach_node.children.remove(p.eid);
				
				// 2. connect e and attach_node as parent-child relation
				e.children.put(attach_node.eid, attach_node);
				attach_node.parent.put(e.eid, e);
				
				// 3. connect e and p as child-parent relation
				e.parent.put(p.eid, p);
				p.children.put(e.eid, e);
				
				return;
			}
		}
		
		e.children.put(attach_node.eid, attach_node);
		attach_node.parent.put(e.eid, e);
		
	}
	

	void attachDown(ETG_Node e, ETG_Node attach_node){
		// if e has relation to a child of attach_node, insert e between the child and attach_node.
		// otherwise attach e as a new child of attach_node
		

		Iterator<String> keys = attach_node.children.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			ETG_Node c = attach_node.children.get(key);
			
			// insert e
			if( c.hasRelationChain(e, null) ){
				// 1. disconnect child-parent connection of c and attach_node
				c.parent.remove(attach_node.eid);
				attach_node.children.remove(c.eid);
				
				// 2. connect e and attach_node as child-parent relation
				e.parent.put(attach_node.eid, attach_node);
				attach_node.children.put(e.eid, e);
				
				// 3. connect e and c as parent-child relation
				e.children.put(c.eid, c);
				c.parent.put(e.eid, e);
				
				return;
			}
		}
		
		e.parent.put(attach_node.eid, attach_node);
		attach_node.children.put(e.eid, e);
	}
	
	
	@SuppressWarnings("deprecation")
	void draw(){
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		
		if( ETGGraph == null ){
			ETGGraph = new MultiGraph("Event Transition Graph(ETG))");
			if( Main.anonymize)
				ETGGraph.addAttribute("ui.default.title", "Event Transition Graph(ETG)) - "+anonymizeString(pivot_event.node.eventName));
			else
				ETGGraph.addAttribute("ui.default.title", "Event Transition Graph(ETG) - "+ pivot_event.node.eventName);

			ETGGraph.setStrict(false);
			ETGGraph.setAutoCreate(true);
		}
		
		ETGGraph.addNode("[Event]"+pivot_event.eid);
		Node node = ETGGraph.getNode("[Event]"+pivot_event.eid);
		String date = pivot_event.node.eventTime.getYear()+"-"+(pivot_event.node.eventTime.getMonth()+1)+"-"+pivot_event.node.eventTime.getDate();
		//String date = pivot_event.node.eventTime.toString();
		if( Main.anonymize )
			node.addAttribute("ui.label", "Event #"+pivot_event.eid+"\\"+date);
		else
			node.addAttribute("ui.label", "Event #"+pivot_event.eid+"\\"+pivot_event.node.eventName+"\\"+date);
		
		//node.addAttribute("ui.style", "fill-color:red; text-alignment:above;");
		node.addAttribute("ui.style", "text-alignment:above;");

		if( pivot_event.node.eventName.contains("메일") )
			node.addAttribute("ui.style", "fill-mode:image-scaled; fill-image:url('email.jpg'); size:"+Main.nodeSize+"px; text-alignment:above; shadow-mode:plain; shadow-color:red; shadow-offset:0; text-size:"+Main.textSize+"; text-style:"+Main.textStyle+";");
		else
			node.addAttribute("ui.style", "fill-mode:image-scaled; fill-image:url('malware.jpg'); size:"+Main.nodeSize+"px; text-alignment:above; shadow-mode:plain; shadow-color:red; shadow-offset:0; text-size:"+Main.textSize+"; text-style:"+Main.textStyle+";");

		drawAncestor(ETGGraph, pivot_event);
		drawSibling(ETGGraph, pivot_event);
		if( Main.drawRelation )
			drawRelation(pivot_event, null, ETGGraph);

		//Viewer viewer = ETGGraph.display();
		Viewer viewer = new Viewer(ETGGraph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
		viewer.enableAutoLayout();
		view = viewer.addDefaultView(false);
		viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.EXIT);
		
		ViewerPipe viewerPipe = viewer.newViewerPipe();
		viewerPipe.addViewerListener(this);
		viewerPipe.addSink(ETGGraph);

		this.setLayout(new BorderLayout());
		this.add((Component) view, BorderLayout.CENTER);
        this.setVisible(true);
		this.setSize(Main.screenWidth, Main.screenHeight);
		/*
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setSize(Main.screenWidth, Main.screenHeight);
		
        if( Main.anonymize )
			this.setTitle("Event Transition Graph(ETG)) - "+anonymizeString(pivot_event.node.eventName));
		else
			this.setTitle("Event Transition Graph(ETG) - "+ pivot_event.node.eventName);
        */
		
		while(mouseEventLoop){
			viewerPipe.pump();
		}
	}
	
	@SuppressWarnings("deprecation")
	void drawAncestor(Graph g, ETG_Node e){
		if( e.parent.size() == 0 )
			return;

		Iterator<String> keys = e.parent.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			ETG_Node p = e.parent.get(key);
			if( p.eid.equals(pivot_event.eid) || g.getEdge("[Event]"+p.eid+"-"+"[Event]"+e.eid) != null || g.getEdge("[Event]"+e.eid+"-"+"[Event]"+p.eid) != null)
				continue;
			g.addNode("[Event]"+p.eid);
			Node node = g.getNode("[Event]"+p.eid);
			String date = p.node.eventTime.getYear()+"-"+(p.node.eventTime.getMonth()+1)+"-"+p.node.eventTime.getDate();
			//String date = p.node.eventTime.toString();
			if( Main.anonymize )
				node.addAttribute("ui.label", "Event #"+p.eid+"\\"+date); 
			else
				node.addAttribute("ui.label", "Event #"+p.eid+"\\"+p.node.eventName+"\\"+date);
			
			g.addEdge("[Event]"+e.eid+"-"+"[Event]"+p.eid, "[Event]"+e.eid, "[Event]"+p.eid);
			Edge edge = g.getEdge("[Event]"+e.eid+"-"+"[Event]"+p.eid);
			edge.addAttribute("ui.style", "size:2px;");
			
			if( p.parent.size() == 0 ){
				//node.addAttribute("ui.style", "fill-color:blue; text-alignment:above;");
				if( p.node.eventName.contains("메일") )
					node.addAttribute("ui.style", "fill-mode:image-scaled; fill-image:url('email.jpg'); size:"+Main.nodeSize+"px; text-alignment:above; shadow-mode:plain; shadow-color:blue; shadow-offset:0; text-size:"+Main.textSize+"; text-style:"+Main.textStyle+";");
				else
					node.addAttribute("ui.style", "fill-mode:image-scaled; fill-image:url('malware.jpg'); size:"+Main.nodeSize+"px; text-alignment:above; shadow-mode:plain; shadow-color:blue; shadow-offset:0; text-size:"+Main.textSize+"; text-style:"+Main.textStyle+";");
			}else{
				if( p.node.eventName.contains("메일") )
					node.addAttribute("ui.style", "fill-mode:image-scaled; fill-image:url('email.jpg'); size:"+Main.nodeSize+"px; text-alignment:above; text-size:"+Main.textSize+"; text-style:"+Main.textStyle+";");
				else
					node.addAttribute("ui.style", "fill-mode:image-scaled; fill-image:url('malware.jpg'); size:"+Main.nodeSize+"px; text-alignment:above; text-size:"+Main.textSize+"; text-style:"+Main.textStyle+";");
			}
				
			drawAncestor(g, p);
			drawSibling(g, p);
		}
	}
	
	@SuppressWarnings("deprecation")
	void drawSibling(Graph g, ETG_Node e){
		if( e.children.size() == 0 )
			return;
		
		Iterator<String> keys = e.children.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			ETG_Node c = e.children.get(key);
			if( c.eid.equals(pivot_event.eid) || g.getEdge("[Event]"+e.eid+"-"+"[Event]"+c.eid) != null || g.getEdge("[Event]"+c.eid+"-"+"[Event]"+e.eid) != null)
				continue;
			g.addNode("[Event]"+c.eid);
			Node node = g.getNode("[Event]"+c.eid);
			String date = c.node.eventTime.getYear()+"-"+(c.node.eventTime.getMonth()+1)+"-"+c.node.eventTime.getDate();
			//String date = c.node.eventTime.toString();
			if( Main.anonymize )
				node.addAttribute("ui.label", "Event #"+c.eid+"\\"+date);
			else
				node.addAttribute("ui.label", "Event #"+c.eid+"\\"+c.node.eventName+"\\"+date);
			
			//node.addAttribute("ui.style", "text-alignment:above;");
			if( c.node.eventName.contains("메일") )
				node.addAttribute("ui.style", "fill-mode:image-scaled; fill-image:url('email.jpg'); size:"+Main.nodeSize+"px; text-alignment:above; text-size:"+Main.textSize+"; text-style:"+Main.textStyle+";");
			else
				node.addAttribute("ui.style", "fill-mode:image-scaled; fill-image:url('malware.jpg'); size:"+Main.nodeSize+"px; text-alignment:above; text-size:"+Main.textSize+"; text-style:"+Main.textStyle+";");
			g.addEdge("[Event]"+e.eid+"-"+"[Event]"+c.eid, "[Event]"+e.eid, "[Event]"+c.eid);
			Edge edge = g.getEdge("[Event]"+e.eid+"-"+"[Event]"+c.eid);
			edge.addAttribute("ui.style", "size:2px;");
			
			drawAncestor(g, c);
			drawSibling(g, c);
		}
	}
	

	void drawRelation(ETG_Node e, ETG_Node from, Graph g){
		
		Vector<String[]> r = e.relations;
		for( int i=0; i<r.size(); i++ ){
			String[] elem = r.get(i);
			
			if( !elem[1].equals("YARA") ){
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
			}else if( g.getEdge("[Event]"+e.eid+"YARA"+"[Event]"+elem[0]) == null && g.getEdge("[Event]"+elem[0]+"YARA"+"[Event]"+e.eid) == null){
					g.addEdge("[Event]"+e.eid+"YARA"+"[Event]"+elem[0], "[Event]"+e.eid, "[Event]"+elem[0]);
					Edge edge = g.getEdge("[Event]"+e.eid+"YARA"+"[Event]"+elem[0]);
					edge.addAttribute("ui.style", "fill-color:orange;");
			}
		}

		Iterator<String> keys = e.children.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			ETG_Node node = e.children.get(key);
			
			if( from==null || !from.eid.equals(node.eid) )
				drawRelation(node, e, g);
		}
		

		keys = e.parent.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			ETG_Node node = e.parent.get(key);

			if( from==null || !from.eid.equals(node.eid) )
				drawRelation(node, e, g);
		}
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
		return s_ret;
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
		System.out.println("ETG key typed!");
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

		
		ETG_Node centerETGNode = getEventNode(id.substring(id.indexOf(']')+1));  // Need to return "centerETGNode" from  getEventNode method
		Iterator<Node> nodeIter = ETGGraph.getNodeIterator();
		do{
			Node currGraphNode = nodeIter.next();
			String currNodeID = currGraphNode.getId();
			if( currNodeID.contains("[Event]") ){
				String pureID = currNodeID.substring(currNodeID.indexOf(']')+1); 
				if( centerETGNode.eid.equals(pureID) || currGraphNode.hasEdgeBetween(id)  ){
					currGraphNode.addAttribute("ui.style", "text-color: blue; text-size:"+highlightTextSize+";size:"+highlightNodeSize+"px; text-style:bold;");
					continue;
				}else{
					int i = 0;
					for(; i<centerETGNode.relations.size(); i++){
						String relatedNodeID = centerETGNode.relations.get(i)[0];
						if( pureID.equals(relatedNodeID) ){
							currGraphNode.addAttribute("ui.style", "text-color: blue; text-size:"+highlightTextSize+";size:"+highlightNodeSize+"px; text-style:bold;");
							break;
						}
					}
					if( i == centerETGNode.relations.size() )
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

		Iterator<Node> nodeIter = ETGGraph.getNodeIterator();
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


class ETG_Node extends EventNode{
	ConcurrentHashMap<String, ETG_Node> parent = null;
	ConcurrentHashMap<String, ETG_Node> children = null;
	
	ETG_Node(EventNode e, ETG_Node p){
		copyEventNode(e, p);
	}
	
	void copyEventNode(EventNode e, ETG_Node p){
		eid = e.eid;
		node = e.node;
		parent = new ConcurrentHashMap<String, ETG_Node>();
		
		if( e.parent != null && p != null)
			parent.put(e.parent.eid, p);
		
		children = new ConcurrentHashMap<String, ETG_Node>();
		if( e.children != null ){
			Iterator<String> keys = e.children.keySet().iterator();
			while( keys.hasNext() ){
				String key = keys.next();
				EventNode c = e.children.get(key);
				children.put(c.eid, new ETG_Node(c, this));
			}
		}
		relations = e.relations;
	}

	ETG_Node checkOlderAncestor(ETG_Node compare){
		//if( node.eventTime.compareTo(compare.node.eventTime) <= 0 ){
		if( checkDateOrder(node, compare.node) < 0 ){
			if( hasRelationChain(compare, null) ){
				Iterator<String> keys = parent.keySet().iterator();
				while(keys.hasNext()){
					String key = keys.next();
					ETG_Node p = parent.get(key);
					ETG_Node older_node = p.checkOlderAncestor(compare);
					if( older_node != null )
						return older_node;
				}
				return this;
			}
		}
		return null;
	}

	ETG_Node checkOlderSibling(ETG_Node compare){
		//if( node.eventTime.compareTo(compare.node.eventTime) <= 0 ){
		if( checkDateOrder(node, compare.node) < 0 ){
			if( hasRelationChain(compare, null) ){
				Iterator<String> keys = children.keySet().iterator();
				while(keys.hasNext()){
					String key = keys.next();
					ETG_Node c = children.get(key);
					ETG_Node older_node = c.checkElderSibling(compare);
					if( older_node != null )
						return older_node;
				}
				return this;
			}
		}
		return null;
	}
	
	ETG_Node checkElderAncestor(ETG_Node compare){
		//if( node.eventTime.compareTo(compare.node.eventTime) >= 0 ){
		if( checkDateOrder(node, compare.node) > 0 ){
			if( hasRelationChain(compare, null) ){
				Iterator<String> keys = parent.keySet().iterator();
				while(keys.hasNext()){
					String key = keys.next();
					ETG_Node p = parent.get(key);
					ETG_Node older_node = p.checkOlderAncestor(compare);
					if( older_node != null )
						return older_node;
				}
				return this;
			}
		}
		return null;
	}
	
	ETG_Node checkElderSibling(ETG_Node compare){
		//if( node.eventTime.compareTo(compare.node.eventTime) >= 0 ){
		if( checkDateOrder(node, compare.node) > 0 ){
			if( hasRelationChain(compare, null) ){
				Iterator<String> keys = children.keySet().iterator();
				while(keys.hasNext()){
					String key = keys.next();
					ETG_Node c = children.get(key);
					ETG_Node older_node = c.checkElderSibling(compare);
					if( older_node != null )
						return older_node;
				}
				return this;
			}
		}
		return null;
	}
	
	@SuppressWarnings("deprecation")
	// return 1 if e1 is elder than e2, return -1 if e1 is older than e2, return 0 if e1 == e2.
	int checkDateOrder(Event e1, Event e2){
		Date d1 = e1.eventTime, d2 = e2.eventTime;
		int year1 = d1.getYear(), year2 = d2.getYear();
		int date1 = d1.getDate(), date2 = d2.getDate();
		int month1 = d1.getMonth(), month2 = d2.getMonth();
		if( year1 > year2 )
			return 1;
		if( year1 < year2 )
			return -1;
		if( month1 > month2 )
			return 1;
		if( month1 < month2 )
			return -1;
		if( date1 > date2 )
			return 1;
		if( date1 < date2 )
			return -1;
		
		return 0;
	}
	

	boolean hasRelation(ETG_Node e){
		for( int i=0; i<relations.size(); i++ ){
			String[] r = relations.get(i);
			if( e.eid.equals(r[0]) )
				return true;
			ETG_Node node = getSibling(r[0]);
			if( node == null )
				node = getAncestor(r[0]);
			if( node == null )
				continue;
		}
		return false;
	}
	
	boolean hasRelationChain(ETG_Node e, Vector<String[]> checkedRelation){
		
		return hasRelation(e);
		/*
		if( checkedRelation == null )
			checkedRelation = new Vector<String[]>();
		
		boolean isCheckedRelation =  isCheckedRelation(eid, e.eid, checkedRelation);
		
		for( int i=0; i<relations.size(); i++ ){
			String[] r = relations.get(i);
			if( e.eid.equals(r[0]) )
				return true;
			
			if( isCheckedRelation )
				continue;
			
			String[] checked = {e.eid, r[0]};
			checkedRelation.add(checked);
			ETG_Node node = getSibling(r[0]);
			if( node == null )
				node = getAncestor(r[0]);
			if( node == null )
				continue;
			
			if( node.hasRelationChain(e, checkedRelation) )
				return true;
		}
		
		return false;*/
	}
	
	boolean isCheckedRelation(String a, String b, Vector<String[]> checkedRelation){
		for( int i=0; i<checkedRelation.size(); i++ ){
			String[] r = checkedRelation.get(i);
			if( (r[0].equals(a)&&r[1].equals(b)) || (r[0].equals(b)&&r[1].equals(a)) )
				return true;
		}
		return false;
	}

	protected ETG_Node getSibling(String id){
		if( children.size() == 0 )
			return null;
		if( eid.equals(id) )
			return this;
		if( children.containsKey(id) )
			return children.get(id);
		
		Iterator<String> keys = children.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			ETG_Node e = children.get(key);
			if( e.eid.equals(id) )
				continue;
			return e.getSibling(id);
		}
		return null;
	}
	
	protected ETG_Node getAncestor(String id){
		if( parent.size() == 0 )
			return null;
		if( eid.equals(id) )
			return this;
		if( parent.containsKey(id) )
			return parent.get(id);
		
		Iterator<String> keys = parent.keySet().iterator();
		while( keys.hasNext() ){
			String key = keys.next();
			ETG_Node e = parent.get(key);
			if( e.eid.equals(id) )
				continue;
			return e.getAncestor(id);
		}
		return null;
	}
	

	boolean isLinkedNode(String id){
		if( parent.containsKey(id) )
			return true;
		if( children.containsKey(id) )
			return true;
		
		return false;
	}
}