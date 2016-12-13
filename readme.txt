Copyright (C) 2016 Daegeon Kim - All Rights Reserved
Contact information: dgkim0803@{korea.ac.kr || hksecurity.net || gmail.com} 

The publications related to this project are as following:
-


1. Library dependency
	a. JRE 1.7 or above
	b. GraphStream(gs-core, gs-algo, gs-ui / release 1.3 or above) -- need to consider using JUNG2 library
	   - http://graphstream-project.org/
	   
	   
2. System input
	a. MISP Event database(xml format)
		- www.misp.org
   	b.(optional) Yara detection result appended to MISP Event database
   		- python script: __________________
	c. event ID in MISP DB to run relation analysis 
	
3. Version information
	a. v0.0(submitted to MILCOM 2016)
		1) concepts of Event Relation Tree(ERT) and Event Transition Graph(ETG) are proposed.
		2) improvements required
			a) Graph visualization
				- find other graph visualization library that allows to control tension of edges.
				- highlight the edges and the nodes linked to the selected node when the mouse cursor is overlaid to the node. : DONE
				- display and highlight clues of the selected event stored in DB when the mouse cursor is overlaid to the node.
				- manually remove the node that is determined not to be related to other events(nodes).
			b) Functionality
				- Detective.findRelation method: apply further heuristics and statistical methods.
				- EventTransitionGraph.transform method: order of timestamps of events should be preserved more consistently. 
				- EventTransitionGraph.getNodeToAttach method: need to preserve branch characteristics in ERT.
			c) Build the automated DB construction system that could collect and append Cyber Threat Intelligence (CTI).
				- automatically collect incidents(i.e., malware, hacking email) from related websites(i.e., VirusTotal, malwares.com)
					* yara detection rule might be required to hunt malware.
					* public reports regarding cyber campaigns could be used to manually collect sets of incidents information.
				- extract CTI from the incidents.
					* use metadata of the incidents provided by the websites and public tools as much as possible.
					