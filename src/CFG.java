import java.util.*;

public class CFG{
	
	// irList with CFG constructed
	public static LinkedList<LinkedList<IRNode>> CFG_irList = new LinkedList<LinkedList<IRNode>>();
	
	// sub irList with CFG constructd (for a function scope)
	private static LinkedList<IRNode> CFG_sublist = new LinkedList<IRNode>();
	// tracking the function scope while scanning through the pre-CFG irList
	private static String irNodeFuncScope = "";
	// Data structrues for constructing the CFG
		// a hashmap of Label index to the label IRNode in a function scope
	private static HashMap<Integer, IRNode> labelIRMap = new HashMap<Integer, IRNode>();
		// list of Branch IRNode with missing predecessor/successor in the first pre-CFG irList scan	
	private static LinkedList<IRNode> UnresolvedBranchList = new LinkedList<IRNode>();
		// list of Jump IRNode with missing predecessor/successor in the first pre-CFG irList scan
	private static LinkedList<IRNode> UnresolvedJumpList = new LinkedList<IRNode>();
	
	
	// given IRAction.irList, a list of IRNode, create a irList with CFG constructed and Kill/Gen set evaluated
	//		also, set up the liveOut set for RET/ last node
	//
	//	-- How CFG is constructed:
	// 	Cases						Predecessors (imm before)				Successors (imm after)
	// First Node in a function			NONE									next node
	// RET node							prev node								NONE
	// Branch Node						prev node								next node, a label node 
	// Jump Node						prev node								a label node
	// Label Node						prev node**, and (Jump/Branch/NONE)		next node	
	// Others							prev node								next node
	//
	// *Note:	remove successors for last node in a function scope
	// **Note: If the label node is a successor of jump, its predecessor won't have the prev node 
	// ***Note: This algorithm does two CFG scans for irList in each function scope
	//			The first scan resolves the prev node and next node, and put the unresolved connection in data structures
	//			The second scan utilized the data structure to resolve the rest of the connections
	//
	//	-- How the liveness of the RET node/ last node of a function scope is setup
	//	Initialize the liveOut sets for RET IRNode to all global variables 
	//		because global variables may be used after the function returns
	//
	public static void constructCFG(LinkedList<IRNode> preList){
		for (int i = 0; i < preList.size(); i++){
			// set up the current node, prev node, and next node
			IRNode node = preList.get(i);
			IRNode prevNode = new IRNode();
			IRNode nextNode = new IRNode();
			try{
				prevNode = preList.get(i-1);
			}catch(Exception ex){
			}
			try{
				nextNode = preList.get(i+1);
			}catch(Exception ex){
			}
			
			// when entering a new function scope,
			// 		store the current sublist to CFG_irList
			// 		create a new sublist
			if (!irNodeFuncScope.equals(node.getBelongToFunctionScope())){
				irNodeFuncScope = node.getBelongToFunctionScope();
				
				// store the current sublist only if the sublist is valid
				if (!CFG_sublist.isEmpty()){
					// second CFG scan: resolve the rest predecessors/ successors connections
					for (IRNode branchNode : UnresolvedBranchList){
						Integer linkedLabelIndex = new Integer(branchNode.getLabelIndex());
						IRNode labelNode = labelIRMap.get(linkedLabelIndex);
						
						branchNode.addSuccessors(labelNode);	// Resolve branch-to-label connection
						labelNode.addPredecessors(branchNode);	// Resolve branch-from connection
					}
					for (IRNode jumpNode : UnresolvedJumpList){
						Integer linkedLabelIndex = new Integer(jumpNode.getLabelIndex());
						IRNode labelNode = labelIRMap.get(linkedLabelIndex);
						
						jumpNode.addSuccessors(labelNode);		// Resolve jump-to-label connection
						labelNode.addPredecessors(jumpNode);	// Resolve jump-from connection
					}
					
					// store the sublist to the CFG_irList
					CFG_irList.add(CFG_sublist);
				}
				
				// create a new sublist for IRNodes in the new function scope, 
				// and reset the unresolved jump/branch list
				CFG_sublist = new LinkedList<IRNode>();
				UnresolvedBranchList = new LinkedList<IRNode>();
				UnresolvedJumpList = new LinkedList<IRNode>();
				labelIRMap = new HashMap<Integer, IRNode>();
				
				// evaluate the gen set and kill set for the first node
				evaluateGenKillSets(node);
				
				// set the predecessor and successors for the first node
																	// predecessor: NONE (R)
				node.addSuccessors(nextNode);						// successor: 	nextNode (R)
																	// conclusion:	resolved
				
				// add the first node to the sublist
				CFG_sublist.add(node);
				
			} else {
				
				// evaluate the gen and kill set
				evaluateGenKillSets(node);
				
				// first CFG scan
				// set the simple predecessors and the successors
				//		non-linked-list-neighbor predecessors/ successors will be set later 
				if (node.getType() == IRType.RET){
					node.addPredecessors(prevNode);					// predecessor:	prev node (R)
																	// successor: 	NONE (R)
																	// conclusion:	resolved
					// set up the liveOut set for RET/ last node
					node.addLiveOutSet(SymbolTableStack.getGlobalVarSet());
				} else if (node.getType() == IRType.BRANCH){
					node.addPredecessors(prevNode);					// predecessor: prev node (R)
					node.addSuccessors(nextNode);					// successor:	next node (R), and branch-to-label (U) 
					UnresolvedBranchList.add(node);					// conclusion:	unresolved
				} else if (node.getType() == IRType.JUMP){
					node.addPredecessors(prevNode);					// predecessor: prev node (R)
																	// successor:	jump-to-label (U)
					UnresolvedJumpList.add(node);					// conclusion:	unresolved
				} else if (node.getType() == IRType.LABEL){
					if (prevNode.getType() != IRType.JUMP){
						node.addPredecessors(prevNode);				// predecessor: prev node [only not after a jump] (R),
					}												//				and jump-from or branch-from (U)
					node.addSuccessors(nextNode);					// successor:	next node (R)
					labelIRMap.put(node.getLabelIndex(), node);		// conclusion:	unresolved
				} else {
					node.addPredecessors(prevNode);					// predecessor:	prev node (R)
					node.addSuccessors(nextNode);					// successor:	next node (R)
																	// conclusion:	resolved
				}
				
				// clear the sucessors for the last node in a function scope
				// set up the liveOut set for RET/last node
				if (i == preList.size() - 1 || !nextNode.getBelongToFunctionScope().equals(irNodeFuncScope)){
					node.clearSuccessors();
					node.addLiveOutSet(SymbolTableStack.getGlobalVarSet());
				}
				
				// add the IRNode to sublist
				CFG_sublist.add(node);
			}
		}
		
		// perform the second CFG scan to the last sublist and add it to CFG_irList
		// second CFG scan: resolve the rest predecessors/ successors connections
		for (IRNode branchNode : UnresolvedBranchList){
			Integer linkedLabelIndex = new Integer(branchNode.getLabelIndex());
			IRNode labelNode = labelIRMap.get(linkedLabelIndex);
			
			branchNode.addSuccessors(labelNode);	// Resolve branch-to-label connection
			labelNode.addPredecessors(branchNode);	// Resolve branch-from connection
		}
		for (IRNode jumpNode : UnresolvedJumpList){
			Integer linkedLabelIndex = new Integer(jumpNode.getLabelIndex());
			IRNode labelNode = labelIRMap.get(linkedLabelIndex);
			
			jumpNode.addSuccessors(labelNode);		// Resolve jump-to-label connection
			labelNode.addPredecessors(jumpNode);	// Resolve jump-from connection
		}
		
		// store the sublist to the CFG_irList
		CFG_irList.add(CFG_sublist);
		
		// set up the basic block leaders
		setupBasicBlocksLeaders();
	}
	
	
	//	Given an IRNode, evaluate its KILL and GEN sets
	//  -- How GEN/Kill sets are set up
	//	Instructoins			IRType				KILL Set (var/temp defined)		GEN Set (var/temp used)
	//	ADDx $S1, $S2, $D		THREE_ENTRY				{ $D }							{ $S1, $S2 }
	//	SUBx $S1, $S2, $D		THREE_ENTRY				{ $D }							{ $S1, $S2 }	
	//	MULx $S1, $S2, $D		THREE_ENTRY				{ $D }							{ $S1, $S2 }
	//	DIVx $S1, $S2, $D		THREE_ENTRY				{ $D }							{ $S1, $S2 }
	//	STOREx $S1, $D			TWO_ENTRY				{ $D }							{ $S1 } (if $S1 is not a literal)
	//	READx $D				ONE_ENTRY				{ $D }							{}
	//	WRITEx $D				ONE_ENTRY				{}								{ $D }
	//	BRx	$S1, $S2, label		BRANCH					{}								{ $S1, $S2 }
	//	PUSH [$PPVAL]			PUSH					{}								{ $PPVAL }
	//	POP [$PPVAL]			POP						{ $PPVAL }						{}
	//	JSR	funclabel			JSR						{}								{all global vars}
	//
	//	Note:
	//	$S1  	getSource1()
	//	$S2  	getSource2()
	//	$D  	getDest()
	//	$PPVAL  getPushPopVal() ** PUSH/POP instructions may or may not have a $PPVAL
	private static void evaluateGenKillSets(IRNode node){
		if (node.getType() == IRType.THREE_ENTRY){
			node.addGenSet(node.getSource1());
			node.addGenSet(node.getSource2());
			node.addKillSet(node.getDest());
		} else if (node.getType() == IRType.TWO_ENTRY){
			if (!TYTSMisc.isINTLITERAL(node.getSource1()) && !TYTSMisc.isFLOATLITERAL(node.getSource1())){
				node.addGenSet(node.getSource1());
			}		
			node.addKillSet(node.getDest());
		} else if (node.getType() == IRType.ONE_ENTRY){
			if (node.getOpcode().equals("READI") || node.getOpcode().equals("READF")){
				node.addKillSet(node.getDest());
			} else {
				if (!SymbolTableStack.isGlobalStringId(node.getDest())){
					node.addGenSet(node.getDest());
				}
			}
		} else if (node.getType() == IRType.BRANCH){
			node.addGenSet(node.getSource1());
			node.addGenSet(node.getSource2());
		} else if (node.getType() == IRType.PUSH){
			if (node.getPushPopVal() != null && !node.getPushPopVal().equals("")){
				node.addGenSet(node.getPushPopVal());
			}
		} else if (node.getType() == IRType.POP){
			if (node.getPushPopVal() != null && !node.getPushPopVal().equals("")){
				node.addKillSet(node.getPushPopVal());
			}
		} else if (node.getType() == IRType.JSR){
			node.addGenSet(SymbolTableStack.getGlobalVarSet());
		}
	}
	
	
	// given CFG_irList with CFG constructed (and GEN/KILL set computed), compute the liveness
	// Note: RET node and the last node (if not a RET node) for each function is already set up in the constructCFG function
	// -- How liveness is computed
	// initializations:
	// 	liveOut		RET node and the last node:		all global vars	(set up in the constructCFG function)
	//	liveOut		others:							empty
	//	liveIn										empty
	// 
	//			1. curr.liveOutSet = curr.liveOutSet U All successors.liveInSet
	//			2. curr.liveInSet = (curr.liveOutSet - curr.KillSet) U curr.GenSet
	//
	// -- Algorithm
	//	STEP 1: Put all of the IR nodes on the worklist
    //	STEP 2: Pull an IR node off the worklist, and compute its live-out and live-in sets according to the definitions above.
    //	STEP 3: If the live-in set of the node gets updated by the previous step, put all of the node's predecessors on the worklist 
	//		(because they may need to update their live-out sets).
    //	STEP 4: Repeat steps 2 and 3 until the worklist is empty.
	
	public static void computeLiveness(){
		// retreive a subworklist for each function scope
		for (LinkedList<IRNode> subworklist : CFG_irList){
			
			// get a copy of the subworklist for a function scope (STEP 1)
			LinkedList<IRNode> workqueue = (LinkedList<IRNode>) subworklist.clone();
			
			// link the cloned copy with the original subworklist
			for (int i = 0; i < subworklist.size(); i++){
				subworklist.get(i).liveCloneRef = workqueue.get(i);
			}
			
			while (!workqueue.isEmpty()){
				// pull off a node from the work queue
				// and compute its live in and live out sets (STEP 2)
				IRNode curr = workqueue.removeFirst();
				// saved current live in set
				HashSet<String> presavedLiveInSet = (HashSet<String>) curr.liveinset.clone();				
				// compute liveness
				if (!curr.IsNoSuccessors()){
					// COMPUTE LIVEOUT
					// 		curr.liveOutSet = curr.liveOutSet U All successors.liveInSet
					for (IRNode succNode : curr.getSuccessors()){
						curr.addLiveOutSet(succNode.getLiveInSet());
					}
					// COMPUTE LIVEIN
					// 		curr.liveInSet = (curr.liveOutSet - curr.KillSet) U curr.GenSet
					curr.liveinset = (HashSet<String>) curr.liveoutset.clone();
					curr.subLiveInSet(curr.getKillSet());
					curr.addLiveInSet(curr.getGenSet());
				}else{
					// COMPUTE LIVEOUT
					// 		liveOutSet for RET/ last node is already set up
					// COMPUTE LIVEIN
					// 		curr.liveInSet = (curr.liveOutSet - curr.KillSet) U curr.GenSet
					curr.liveinset = (HashSet<String>) curr.liveoutset.clone();
					curr.subLiveInSet(curr.getKillSet());
					curr.addLiveInSet(curr.getGenSet());
				}
				// if the live-in set is updated, put All predecessors back to the work queue (STEP 3)
				if (!presavedLiveInSet.equals(curr.liveinset)){
					for (IRNode predNode : curr.getPredecessors()){
						workqueue.addLast(predNode);
					}
				}
			} // repeat until work queue is empty (STEP 4)
			
			// Substitute the original subworklist node with the cloned node
			for (IRNode node : subworklist){
				node = node.liveCloneRef;
			}
		}
	}
	
	
	private static void setupBasicBlocksLeaders(){
		for (LinkedList<IRNode> sublist : CFG_irList){
			for (IRNode node : sublist){
				// first node in a function scope is a basic block leader
				if (node.getPredecessors().size() == 0){
					node.setIsBBLeader(true);
				} else {
					// a node with branch or jump predecessor is also a basic block leader
					for (IRNode predNode : node.getPredecessors()){
						if (predNode.getType() == IRType.BRANCH || predNode.getType() == IRType.JUMP){
							node.setIsBBLeader(true);
							break;
						}
					}
				}
			}
		}
	}
	
	
	////////// debugging print functions
	//  print irList with CFG constructed
	public static void printCFG_irList(){
		System.out.println();
		System.out.println("; ========= CFG IR NODE =========");
		for (LinkedList<IRNode> sublist : CFG_irList){
			System.out.println();
			for (IRNode node : sublist){
				node.printCFGNode();
			}
		}
	}
	
	
	// print irList with CFG constructed and GEN/KILL set computed
	public static void printKillGen_irList(){
		System.out.println();
		System.out.println("; ========= CFG IR NODE / KILL/ GEN =========");
		for (LinkedList<IRNode> sublist : CFG_irList){
			System.out.println();
			for (IRNode node : sublist){
				node.printGENKILLNode();
			}
		}
	}
	
	// print irList with Liveness computed
	public static void printLiveness_irList(){
		System.out.println();
		System.out.println("; ========= CFG IR NODE / LIVE IN / LIVE OUT =========");
		for (LinkedList<IRNode> sublist : CFG_irList){
			System.out.println();
			for (IRNode node : sublist){
				node.printLivenessNode();
			}
		}
	}
}