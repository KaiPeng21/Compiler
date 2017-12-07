
import java.util.*;

public class IRNode{
	
	private String opcode;
	private String source1;
	private String source2;
	private String dest;
	private IRType type;
	
	// addition for step 5: branch, jump, nop
	private int labelIndex;
	private boolean isNOP;
	
	// addition for step 6:
	private String funcNameId;
	private String jsrFuncName;
	private String pushpopVal;
	private int linknum;
	
	// addition for step 7
	
	// the function scope this IR Node belongs to
	private String belongToFunctionScope;
	// IRNodes that run immediately after this node in the CFG
	private LinkedList<IRNode> successors = new LinkedList<IRNode>();
	// IRNodes that run immediately before this node in the CFG
	private LinkedList<IRNode> predecessors = new LinkedList<IRNode>();
	// Variables/ TempRegister that are used in this node
	private HashSet<String> genset = new HashSet<String>();
	// Variables/ TempRegister that are defined in this node
	private HashSet<String> killset = new HashSet<String>();
	// Variables/ TempRegister that are live right before this node
	public HashSet<String> liveinset = new HashSet<String>();
	// Variables/ TempRegister that are live right after this node
	public HashSet<String> liveoutset = new HashSet<String>();
	
	private boolean isBBLeader = false;
	
	// A cloned IRNode reference for computing liveness
	public IRNode liveCloneRef;
	
	// ralloc message
	private String rallocMessage = ";";
	
	// add, sub, mul, div
	public IRNode(String opcode, String source1, String source2, String dest){
		this.opcode = opcode;
		this.source1 = source1;
		this.source2 = source2;
		this.dest = dest;
		this.type = IRType.THREE_ENTRY;
		this.labelIndex = 0;
		this.funcNameId = null;
		this.isNOP = false;
		this.jsrFuncName = null;
		this.pushpopVal = null;
		this.linknum = 0;
		this.belongToFunctionScope = IRAction.currentFunctionScope;
	}
	
	// store
	public IRNode(String opcode, String source1, String dest){
		this.opcode = opcode;
		this.source1 = source1;
		this.source2 = null;
		this.dest = dest;
		this.type = IRType.TWO_ENTRY;
		this.labelIndex = 0;
		this.funcNameId = null;
		this.isNOP = false;
		this.jsrFuncName = null;
		this.pushpopVal = null;
		this.linknum = 0;
		this.belongToFunctionScope = IRAction.currentFunctionScope;
	}
	
	// read, write
	public IRNode(String opcode, String dest){
		this.opcode = opcode;
		this.source1 = null;
		this.source2 = null;
		this.dest = dest;
		this.type = IRType.ONE_ENTRY;
		this.labelIndex = 0;
		this.funcNameId = null;
		this.isNOP = false;
		this.jsrFuncName = null;
		this.pushpopVal = null;
		this.linknum = 0;
		this.belongToFunctionScope = IRAction.currentFunctionScope;
	}
	
	// label, jump, link, unlink
	public IRNode(IRType irtype, int value){
		this.belongToFunctionScope = IRAction.currentFunctionScope;
		
		if (irtype == IRType.LABEL || irtype == IRType.JUMP){
			this.opcode = null;
			this.source1 = null;
			this.source2 = null;
			this.dest = null;
			this.type = irtype;
			this.labelIndex = value;
			this.funcNameId = null;
			this.isNOP = false;
			this.jsrFuncName = null;
			this.pushpopVal = null;
			this.linknum = 0;
		} else if (irtype == IRType.LINK || irtype == IRType.UNLINK){
			this.opcode = null;
			this.source1 = null;
			this.source2 = null;
			this.dest = null;
			this.type = irtype;
			this.labelIndex = 0;
			this.funcNameId = null;
			this.isNOP = false;
			this.jsrFuncName = null;
			this.pushpopVal = null;
			this.linknum = value;
		} else {
			this.opcode = null;
			this.source1 = null;
			this.source2 = null;
			this.dest = null;
			this.type = IRType.NOP;
			this.labelIndex = 0;
			this.funcNameId = null;
			this.isNOP = true;
			this.jsrFuncName = null;
			this.pushpopVal = null;
			this.linknum = value;
		}
	}
	
	// branch
	public IRNode(String opcode, String source1, String source2, int labelIndex){
		this.opcode = opcode;
		this.source1 = source1;
		this.source2 = source2;
		this.dest = null;
		this.type = IRType.BRANCH;
		this.labelIndex = labelIndex;
		this.funcNameId = null;
		this.isNOP = false;
		this.jsrFuncName = null;
		this.pushpopVal = null;
		this.linknum = 0;
		this.belongToFunctionScope = IRAction.currentFunctionScope;
	}
	
	// JSR, RET, PUSH, POP, PUSHREG, POPREG
	public IRNode(IRType irtype, String val){
		this.belongToFunctionScope = IRAction.currentFunctionScope;
		if (irtype == IRType.JSR){
			this.opcode = null;
			this.source1 = null;
			this.source2 = null;
			this.dest = null;
			this.type = IRType.JSR;
			this.labelIndex = 0;
			this.isNOP = false;
			this.funcNameId = null;
			this.jsrFuncName = val;
			this.pushpopVal = null;
			this.linknum = 0;
		} else if (irtype == IRType.RET){
			this.opcode = null;
			this.source1 = null;
			this.source2 = null;
			this.dest = null;
			this.type = IRType.RET;
			this.labelIndex = 0;
			this.isNOP = false;
			this.funcNameId = null;
			this.jsrFuncName = null;
			this.pushpopVal = null;
			this.linknum = 0;
		} else if (irtype == IRType.PUSH || irtype == IRType.PUSHREG || irtype == IRType.POP || irtype == IRType.POPREG){
			this.opcode = null;
			this.source1 = null;
			this.source2 = null;
			this.dest = null;
			this.type = irtype;
			this.labelIndex = 0;
			this.isNOP = false;
			this.funcNameId = null;
			this.jsrFuncName = null;
			this.pushpopVal = val;
			this.linknum = 0;
		} else if (irtype == IRType.FUNCLABEL){
			this.opcode = null;
			this.source1 = null;
			this.source2 = null;
			this.dest = null;
			this.type = irtype;
			this.labelIndex = 0;
			this.funcNameId = val;
			this.isNOP = false;
			this.jsrFuncName = null;
			this.pushpopVal = null;
			this.linknum = 0;
		} else {
			this.opcode = null;
			this.source1 = null;
			this.source2 = null;
			this.dest = null;
			this.type = IRType.NOP;
			this.labelIndex = 0;
			this.funcNameId = null;
			this.isNOP = true;
			this.jsrFuncName = null;
			this.linknum = 0;
		}
	}
	
	
	// NOP
	public IRNode(){
		this.opcode = null;
		this.source1 = null;
		this.source2 = null;
		this.dest = null;
		this.type = IRType.NOP;
		this.labelIndex = 0;
		this.funcNameId = null;
		this.isNOP = true;
		this.jsrFuncName = null;
		this.linknum = 0;
		this.belongToFunctionScope = IRAction.currentFunctionScope;
	}
	
	public boolean isNOP(){
		return this.isNOP || this.type == IRType.NOP;
	}
	
	public String getOpcode(){
		return this.opcode;
	}
	
	public String getSource1(){
		return this.source1;
	}
	
	public void setSource1(String source1){
		this.source1 = source1;
	}
	
	public String getSource2(){
		return this.source2;
	}
	
	public void setSource2(String source2){
		this.source2 = source2;
	}
	
	public String getDest(){
		return this.dest;
	}
	
	public void setDest(String dest){
		this.dest = dest;
	}
	
	public IRType getType(){
		return this.type;
	}
	
	public int getLabelIndex(){
		return this.labelIndex;
	}
	
	public String getJsrFuncName(){
		return this.jsrFuncName;
	}
	
	public String getPushPopVal(){
		return this.pushpopVal;
	}
	
	public int getLinkNum(){
		return this.linknum;
	}
	
	public String getFuncNameId(){
		return this.funcNameId;
	}
	
	public String getBelongToFunctionScope(){
		return this.belongToFunctionScope;
	}
	
	
	@Override
	public String toString(){
		String message = "";
		
		switch (this.type)
		{
			case THREE_ENTRY:
				message += ";" + this.opcode + " " + this.source1 + " " + this.source2 + " " + this.dest;
				break;
			case TWO_ENTRY:
				message += ";" + this.opcode + " " + this.source1 + " " + this.dest;
				break;
			case ONE_ENTRY:
				message += ";" + this.opcode + " " + this.dest;
				break;
			case LABEL:
				message += ";LABEL label" + this.labelIndex;
				break;
			case FUNCLABEL:
				message += ";LABEL " + this.funcNameId;
				break;
			case JUMP:
				message += ";JUMP label" + this.labelIndex;
				break;
			case BRANCH:
				message += ";" + this.opcode + " " + this.source1 + " " + this.source2 + " label" + this.labelIndex;
				break;
			case JSR:
				message += ";JSR " + this.jsrFuncName;
				break;
			case RET:
				message += ";RET";
				break;
			case PUSH:
				message += ";PUSH " + this.pushpopVal;
				break;
			case PUSHREG:
				message += ";PUSHREG";
				break;
			case POP:
				message += ";POP " + this.pushpopVal;
				break;
			case POPREG:
				message += ";POPREG";
				break;
			case LINK:
				message += ";LINK " + this.linknum;
				break;
			case UNLINK:
				message += ";UNLINK";
				break;
			default:
				message += "; ";
		}
		
		
		return message;
	}
	
	
	public void printNode(){
		if (getType() == IRType.FUNCLABEL){
			System.out.println();
		}
		System.out.println(toString() + getCalcTab(toString().length()) + " Under Func Scope: " + this.belongToFunctionScope);
	}
	
	public void printCFGNode(){
		String m = "";
		m += toString();
		m += getCalcTab(m.length());
		m += "P: ";
		for (IRNode node : predecessors){
			m += node.toString() + "\t";
		}
		m += getCalc2Tab(m);
		m += "S: ";
		for (IRNode node : successors){
			m += node.toString() + "\t";
		}
		
		if (isBBLeader){
			m += "\t <== BB leader";
		}
		
		System.out.println(m);
	}
	
	public void printGENKILLNode(){
		String m = "";
		m += toString();
		m += getCalcTab(m.length());
		m += "Kill: ";
		for (String var : killset){
			m += var + ", ";
		}
		m += getCalc2Tab(m);
		m += "Gen: ";
		for (String var : genset){
			m += var + ", ";
		}
		System.out.println(m);
	}
	
	public void printLivenessNode(){
		String m = "";
		m += toString();
		m += getCalcTab(m.length());
		m += "Live IN: ";
		for (String var : liveinset){
			m += var + ", ";
		}
		m += getCalc2Tab(m);
		m += "Live OUT: ";
		for (String var : liveoutset){
			m += var + ", ";
		}
		System.out.println(m);
	}
	
	public String getLiveOutString(){
		String m = "; { ";
		for (String var : this.liveoutset){
			m += var;
			m += " ";
		}
		m += " }";
		return m;
	}
	
	// functions for formating the print output
	private String getCalcTab(int slen){
		if (slen >= 16){
			return getTab(1);
		} else if (slen >= 8){
			return getTab(2);
		}
		return getTab(3);
	}
	private String getCalc2Tab(String str){
		int slen = 0;
		for (char c : str.toCharArray()){
			if (c == '\t'){
				slen += 8;
			} else{
				slen++;
			}
		}
		if (slen >= 56){
			return getTab(1);
		} else if (slen >= 48){
			return getTab(2);
		} else if (slen >= 40){
			return getTab(3);
		} else if (slen >= 32){
			return getTab(4);
		}
		return getTab(5);
	}
	private String getTab(int num){
		String t = "";
		if (num < 0){
			num = 0;
		}
		for (int i = 0; i < num; i++){
			t += "\t";
		}
		return t;
	}
	
	
	// For constructing CFG
	public LinkedList<IRNode> getSuccessors(){
		return this.successors;
	}
	public LinkedList<IRNode> getPredecessors(){
		return this.predecessors;
	}
	public void addSuccessors(IRNode node){
		this.successors.add(node);
	}
	public void addPredecessors(IRNode node){
		this.predecessors.add(node);
	}
	public void clearSuccessors(){
		this.successors.clear();
	}
	public void clearPredecessors(){
		this.predecessors.clear();
	}
	
	// For computing GEN and KILL sets
	public HashSet<String> getGenSet(){
		return this.genset;
	}
	public HashSet<String> getKillSet(){
		return this.killset;
	}
	public void addGenSet(String var){
		this.genset.add(var);
	}
	public void addGenSet(HashSet<String> varSet){
		this.genset.addAll(varSet);
	}
	public void subGenSet(String var){
		this.genset.remove(var);
	}
	public void subGenSet(HashSet<String> varSet){
		this.genset.removeAll(varSet);
	}
	public void addKillSet(String var){
		this.killset.add(var);
	}
	public void addKillSet(HashSet<String> varSet){
		this.killset.addAll(varSet);
	}
	public void subKillSet(String var){
		this.killset.remove(var);
	}
	public void subKillSet(HashSet<String> varSet){
		this.killset.removeAll(varSet);
	}
	
	// For live-in and live-out sets
	public HashSet<String> getLiveInSet(){
		return this.liveinset;
	}
	public HashSet<String> getLiveOutSet(){
		return this.liveoutset;
	}
	public void addLiveInSet(String var){
		this.liveinset.add(var);
	}
	public void addLiveInSet(HashSet<String> varSet){
		this.liveinset.addAll(varSet);
	}
	public void subLiveInSet(String var){
		this.liveinset.remove(var);
	}
	public void subLiveInSet(HashSet<String> varSet){
		this.liveinset.removeAll(varSet);
	}
	public void addLiveOutSet(String var){
		this.liveoutset.add(var);
	}
	public void addLiveOutSet(HashSet<String> varSet){
		this.liveoutset.addAll(varSet);
	}
	public void subLiveOutSet(String var){
		this.liveoutset.remove(var);
	}
	public void subLiveOutSet(HashSet<String> varSet){
		this.liveoutset.removeAll(varSet);
	}
	public boolean IsNoSuccessors(){
		return this.successors.isEmpty();
	}
	public boolean IsNoPredecessors(){
		return this.predecessors.isEmpty();
	}
	
	
	public void setRallocMessage(String message){
		this.rallocMessage = message;
	}
	public void appendRallocMessage(String message){
		this.rallocMessage += message;
	}
	public String getRallocMessage(){
		return this.rallocMessage;
	}
	
	public boolean isBBLeader(){
		return this.isBBLeader;
	}
	public void setIsBBLeader(boolean isBBLeader){
		this.isBBLeader = isBBLeader;
	}
}