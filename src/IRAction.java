import java.util.*;

public class IRAction
{	
	// a stack of expression tokens (for evaluating the assign type statement)
	public static Stack<String> tstack = new Stack<String>();
	
	// intermediate representation 3 address code list (without CFG and liveness computation)
	public static LinkedList<IRNode> irList = new LinkedList<IRNode>();
	
	// stack of labels
	public static Stack<Label> lstack = new Stack<Label>();
	
	// stack of incr_stmt irlist
	public static Stack<LinkedList<IRNode>> incrStack = new Stack<LinkedList<IRNode>>();
	
	// left conditional expression or register
	public static String condLeftReg = "";
	
	// current function scope (as the listerner is walking through the syntax tree)
	public static String currentFunctionScope = "";
	
	// stack of sub scope (for block, if block, else block) in the current function scope
	public static Stack<IRScope> subScopeStack = new Stack<IRScope>();
	
	// stack of parameter tokens (or register) in a function call
	public static Stack<String> callerParaStack = new Stack<String>();
	
	// stack storing the stack of parameter tokens (or register) in a function call
	public static Stack<Stack<String>> callerParaStackStorageStack = new Stack<Stack<String>>();
	
	// hashmap of temp register $Tn to the register type (INT or FLOAT)
	public static HashMap<String, String> tempRegTypeMap = new HashMap<String, String>();
	
	// last function call tempory
	public static String lastFunctionCallTempory = "";
	
	// FOR OPTIMIZATION
	// save the irList when entering an if statement
	// if there are return statement in both IF subblock and ELSE subblock at the same level and the return literal are the same
	// AND there are no read write statement in the subblock
	// replace the entire irList with the pre-saved irList
	public static LinkedList<IRNode> enterIFCloneIRList = new LinkedList<IRNode>();
	public static String IF_return_literal = "";
	public static String ELSE_return_literal = "";
	public static boolean is_IF_ELSE_return_comparison_cancelled = false;
	
	
	
	
	// track of register and label index
	private static int registerIndex = 1;
	private static int labelIndex = 1;
	private static int scopeBlockID = 1;
	
	// Append to finalized irlist
	public static void appendIRList(LinkedList<IRNode> ir_stmt_list){
		for (IRNode node: ir_stmt_list){
			addToFilteredIRList(node);
		}
	}
	public static void appendIRList(IRNode irnode){
		addToFilteredIRList(irnode);
	}
	
	private static void addToFilteredIRList(IRNode irnode){
		
		if (!irList.isEmpty()){
			
			/*
			
			// Filter Case 1
			// Pre-Filtered:	;STOREx [S] [D]		;; lineA
			// 					;STOREx [S] [D]		;; lineB
			//
			// Post-Filtered:	;STOREx [S] [D]
			//
			//	Optimizing: If lineA.D and lineB.S are the same, and only one of lineA.S and lineB.D is accessing memory
			//
			// tiny rule: 		move opmrl	opmr ; ***only one operand can be a memory id or stack variable
			//	opmrl : memory id, stack variable, register or a number (literal)
			//	opmr  :	memory id, stack variable, or a register
			//
			if (irList.getLast().getType() == IRType.TWO_ENTRY && irnode.getType() == IRType.TWO_ENTRY){
				String lineALeft = irList.getLast().getSource1();
				String lineARight = irList.getLast().getDest();
				String lineBLeft = irnode.getSource1();
				String lineBRight = irnode.getDest();	
				
				if (lineARight.equals(lineBLeft)){
					if (!(TYTSMisc.isMemoryAccessExpr(lineALeft) && TYTSMisc.isMemoryAccessExpr(lineBRight))){
						irList.removeLast();
						irnode = new IRNode(irnode.getOpcode(), lineALeft, lineBRight);
					}
				}
				
			// Filter Case 2
			// Pre-Filtered:	;STOREx [S] [D]					;; lineA
			// 					;BRANCH [S1] [S2] target 		;; lineB
		    // 
			// Post-Filtered:	;BRANCH [S1] [S2] target
			// 
			//  If the STORE in line A is preparing for the BRANCH's S2 in line B
			//  	and BRANCH's S1 is not accessing memory or is a literal (BRANCH's S1 and S2 can be switched)
			//		reduce the store, find the switch opcode, and switch the two operands
			//	
			//			S1 is opmrl, S2 can only be a reg
			// 	tiny rule:
			// 	cmpi opmrl reg         ; integer comparison; must preceed  a conditional jump;
            //    						it compares the first operand with the second op and
   			//		 					sets  the "processor status". (The status remains the
   			//							same until the next cmp instruction is executed.)
            //			                E.g, a subsequent jgt will jump if op1 > op2
			// 		
			//	opmrl : memory id, stack variable, register or a number (literal)
			//	opmr  :	memory id, stack variable, or a register
			//
			} else if (irList.getLast().getType() == IRType.TWO_ENTRY && irnode.getType() == IRType.BRANCH){
				String lineALeft = irList.getLast().getSource1();
				String lineARight = irList.getLast().getDest();
				String lineBLeft = irnode.getSource1();
				String lineBRight = irnode.getSource2();	
				
				
				// STORE in line A is for Branch S2 in line B
				if (lineARight.equals(lineBRight)){
					// S1 in line B and S2 in line B can be switched
					if (TYTSMisc.isTempReg(lineBLeft)){
						// There is a need to switch the two operand
						if (TYTSMisc.isLiteral(lineALeft) || TYTSMisc.isMemoryAccessExpr(lineALeft)){
							irList.removeLast();
							String switchOpOpcode = TYTSMisc.getBranchSwitchOpOpcode(irnode.getOpcode());
							irnode = new IRNode(switchOpOpcode, lineALeft, lineBLeft, irnode.getLabelIndex());
						}
					}
				}
			// Filter Case 3
			// Pre-Filtered:	;STOREx [S] [D]					;; lineA
			// 					;THREEx [S1] [S2] [D] 			;; lineB
		    // 
			// Post-Filtered:	;THREEx [S1] [S2] [D]
			//
			// 
			//
			// tiny code rules:
			//	addi opmrl reg         ; integer addition, reg = reg + op1
			//	addr opmrl reg         ; real (i.e. floatingpoint) addition
			//	muli opmrl reg         ; computes reg = reg * op1
			//	mulr opmrl reg
				
			} else if (irList.getLast().getType() == IRType.TWO_ENTRY && irnode.getType() == IRType.THREE_ENTRY){
				String opcode = irnode.getOpcode();
				if (opcode.startsWith("ADD") || opcode.startsWith("MUL")){
					String lineALeft = irList.getLast().getSource1();
					String lineARight = irList.getLast().getDest();
					String lineBLeft = irnode.getSource1();
					String lineBRight = irnode.getSource2();	
					
					// STORE in line A is for ADD/MUL S2 in line B
					if (lineARight.equals(lineBRight)){
						// S1 in line B and S2 in line B can be switched
						if (TYTSMisc.isTempReg(lineBLeft)){
							// There is a need to switch the two operand
							if (TYTSMisc.isLiteral(lineALeft) || TYTSMisc.isMemoryAccessExpr(lineALeft)){
								irList.removeLast();
								irnode = new IRNode(opcode, lineALeft, lineBLeft, irnode.getDest());
							}
						}
					}
				}
					
			}*/
			
		}
		
		irList.add(irnode);
	}
	
	
	public static void debugIRList(){
		
		System.out.println();
		System.out.println(";======debugging");
		printIRList();
		System.out.println(";==========");
		System.out.println();
		
	}
	
	// Print the IR Nodes
	public static void printIRList(){
		System.out.println();
		System.out.println(";IR code");
		System.out.println(";PUSH");
		System.out.println(";JSR main");
		System.out.println(";HALT");
		for (IRNode node : irList){
			node.printNode();
		}
		
	}
	
	// assign the register index
	public static String createTempRegister(String type){
		tempRegTypeMap.put("$T" + IRAction.registerIndex, type);
		String newTemp = "$T" + IRAction.registerIndex++;
		
		/*
		if (registerIndex > 4){
			registerIndex = 1;
		}*/
		
		return newTemp;
	}
	
	// get current register index
	public static String currentRegister(){
		int currTemp = (IRAction.registerIndex - 1);
		
		/*
		if (currTemp == 0){
			currTemp = 4;
		}*/
		
		return "$T" + currTemp;
	}
	
	public static int numberOfTempories(){
		return IRAction.registerIndex - 1;
	}
	
	// assign new label index
	public static int allocateLabel(){
		return IRAction.labelIndex++;
	}
	
	// assign new scope block id
	public static int allocateScopeBlockID(){
		return IRAction.scopeBlockID++;
	}
	
	
	// given an identifier, return its type when it is declared
	public static String getDeclareType(String id){
		// if the identifier is in the symbol table stack, reuturn its type
		// otherwise, return an empty string
		
		// convert token to slot registers
		if (SymbolTableStack.isGlobalVar(id)){
			// for global variables, find the type from the bottom-most symbol table
			LinkedHashMap<String, Symbol> symbolMap = SymbolTableStack.symbolTableStack.get(0).getSymbolMap();
			if (symbolMap.containsKey(id)){
				return symbolMap.get(id).getType();
			}
		} else {
			// for local variables, find the type from the current function scope symbol table and the sub-block symbol table
			LinkedHashMap<String, Symbol> symbolMap = SymbolTableStack.searchSymbolTable(IRAction.currentFunctionScope).getSymbolMap();
			if (symbolMap.containsKey(id)){
				return symbolMap.get(id).getType();
			} else {
				for (int i = 0; i < subScopeStack.size(); i++){
					// get each symbol table of the walking subblock (FOR/ IF/ ELSE)
					SymbolTable subBlockSymTable = SymbolTableStack.searchSymbolTable(subScopeStack.get(i).getBlockID());
					// get the symbol hashmap of the walking subblock 
					LinkedHashMap<String, Symbol> subBlockSymMap = subBlockSymTable.getSymbolMap();
					// if the subblock contains the desired local variable, get its type
					if (subBlockSymMap.containsKey(id)){
						return subBlockSymMap.get(id).getType();
					}
				}
			}
		}
		
		return "BAD1BAD1";
	}
	
	// given the current function scope name and the identifier,
	// find its slot location register expression
	public static String convertEntryToSlotLocation(String functionScopeName, String entry){
		int slotLocation = 0;
		
		try{
			slotLocation = SymbolTableStack.findSlotLocation(functionScopeName, entry, IRAction.subScopeStack);
			System.out.println("; searching slot location for -- " + entry + " -- in function scope " + IRAction.currentFunctionScope + " and get location " + slotLocation + "\n");
			return "$" + slotLocation;
		} catch (Exception e){
			System.out.println("; failed to find slot for " + entry + " in function scope " + IRAction.currentFunctionScope + "\n");
		}
		
		return null;
	}
	
	
	// ****************************************
	// IRtoTinyNode Functions
	//
	// ****************************************
	
	// if the IR expression is a register
	public static boolean isIRReg(String format){
		if (format == null){
			return false;
		}
		return format.matches("\\$T[0-9]+");
	}
	
	// given $Tn, conver to r(n-1)
	// if input is not $Tn, return the original input
	private static String toTinyReg(String IRReg){
		if (isIRReg(IRReg)){
			int IRRegVal = Integer.parseInt(IRReg.substring(2));
			return "r" + (IRRegVal - 1);
		}
		return IRReg;
	}
	
    // Convert an IRNode to tiny code
	public static String toTinyCode(IRNode irnode){
		String tiny = "";
		
		// One entry - Read/Write
		if (irnode.getType() == IRType.ONE_ENTRY){
			tiny += "sys ";
				
			if (irnode.getOpcode().equals("READI")){
				tiny += "readi ";
			} else if (irnode.getOpcode().equals("READF")){
				tiny += "readr ";
			} else if (irnode.getOpcode().equals("WRITEI")){
				tiny += "writei ";
			} else if (irnode.getOpcode().equals("WRITEF")){
				tiny += "writer ";
			} else if (irnode.getOpcode().equals("WRITES")){
				tiny += "writes ";
			} 
				
			tiny += IRAction.toTinyReg(irnode.getDest());
			tiny += "\n";
		
		// Two entry - Store
		} else if (irnode.getType() == IRType.TWO_ENTRY){
			
			tiny += "move ";
			tiny += toTinyReg(irnode.getSource1()) + " ";
			tiny += toTinyReg(irnode.getDest());
			tiny += "\n";
			
		// Three entry - add/ sub/ mult/ div
		} else if (irnode.getType() == IRType.THREE_ENTRY){
			// first tiny code line
			tiny += "move ";
			tiny += toTinyReg(irnode.getSource1()) + " ";
			tiny += toTinyReg(irnode.getDest());
			tiny += "\n";
			
			// second tiny code line
			if (irnode.getOpcode().equals("ADDI")){
				tiny += "addi ";
			} else if (irnode.getOpcode().equals("ADDF")){
				tiny += "addr ";
			} else if (irnode.getOpcode().equals("SUBI")){
				tiny += "subi ";
			} else if (irnode.getOpcode().equals("SUBF")){
				tiny += "subr ";
			} else if (irnode.getOpcode().equals("MULTI")){
				tiny += "muli ";
			} else if (irnode.getOpcode().equals("MULTF")){
				tiny += "mulr ";
			} else if (irnode.getOpcode().equals("DIVI")){
				tiny += "divi ";
			} else if (irnode.getOpcode().equals("DIVF")){
				tiny += "divr ";
			}
			tiny += toTinyReg(irnode.getSource2()) + " ";
			tiny += toTinyReg(irnode.getDest());
			tiny += "\n";
		
		// JUMP - jmp
		} else if (irnode.getType() == IRType.JUMP){
			tiny += "jmp ";
			tiny += "label";
			tiny += irnode.getLabelIndex();
			tiny += "\n";
		
		// BRANCH - cmp
		} else if (irnode.getType() == IRType.BRANCH){
			tiny += "cmp";
			if (irnode.getOpcode().charAt(2) == 'I'){
				tiny += "i ";
			}else if (irnode.getOpcode().charAt(2) == 'F'){
				tiny += "r ";
			}
			
			tiny += toTinyReg(irnode.getSource1()) + " ";
			tiny += toTinyReg(irnode.getSource2());
			tiny += "\n";
			if (irnode.getOpcode().equals("EQI") || irnode.getOpcode().equals("EQF")){
				tiny += "jeq ";
			} else if (irnode.getOpcode().equals("NEI") || irnode.getOpcode().equals("NEF")){
				tiny += "jne ";
			} else if (irnode.getOpcode().equals("GTI") || irnode.getOpcode().equals("GTF")){
				tiny += "jgt ";
			} else if (irnode.getOpcode().equals("GEI") || irnode.getOpcode().equals("GEF")){
				tiny += "jge ";
			} else if (irnode.getOpcode().equals("LTI") || irnode.getOpcode().equals("LTF")){
				tiny += "jlt ";
			} else if (irnode.getOpcode().equals("LEI") || irnode.getOpcode().equals("LEF")){
				tiny += "jle ";
			}
			tiny += "label";
			tiny += irnode.getLabelIndex();
			tiny += "\n";
		
		// LABEL
		} else if (irnode.getType() == IRType.LABEL){
			tiny += "label ";
			tiny += "label" + irnode.getLabelIndex();
			tiny += "\n";
		// FUNCLABEL
		} else if (irnode.getType() == IRType.FUNCLABEL){
			tiny += "label ";
			tiny += irnode.getFuncNameId();
			tiny += "\n";
		// JSR
		} else if (irnode.getType() == IRType.JSR){
			tiny += "jsr ";
			tiny += irnode.getJsrFuncName();
			tiny += "\n";
		// RET
		} else if (irnode.getType() == IRType.RET){
			tiny += "ret";
			tiny += "\n";
		// PUSH
		} else if (irnode.getType() == IRType.PUSH){
			tiny += "push ";
			tiny += toTinyReg(irnode.getPushPopVal());
			tiny += "\n";		
		// POP
		} else if (irnode.getType() == IRType.POP){
			tiny += "pop ";
			tiny += toTinyReg(irnode.getPushPopVal());
			tiny += "\n";
		// PUSHREG
		} else if (irnode.getType() == IRType.PUSHREG){
			tiny += "push r0\n";
			tiny += "push r1\n";
			tiny += "push r2\n";
			tiny += "push r3\n";	
		// POPREG
		} else if (irnode.getType() == IRType.POPREG){
			tiny += "pop r3\n";
			tiny += "pop r2\n";
			tiny += "pop r1\n";
			tiny += "pop r0\n";
		// LINK
		} else if (irnode.getType() == IRType.LINK){
			tiny += "link ";
			tiny += irnode.getLinkNum();
			tiny += "\n";
		// UNLINK
		} else if (irnode.getType() == IRType.UNLINK){
			tiny += "unlnk ";
			tiny += "\n";
		}	
		
		// FUNCLABEL, JSR, RET, PUSH, PUSHREG, POP, POPREG, LINK, UNLINK
		
		return tiny;
	}
	
	// PrintTinyNode Function
	public static void printTinyCode(){
		
		// get the global symbol table
		SymbolTable symbolTable = SymbolTableStack.symbolTableStack.get(0);
		for (Map.Entry<String, Symbol> entry : symbolTable.getSymbolMap().entrySet()){
			Symbol symbol = entry.getValue();
			if (symbol.getValue() == null){
				System.out.println("var " + symbol.getName());
			} else {
				System.out.println("str " + symbol.getName() + " " + symbol.getValue());
			}
		}
		
		// treat string variables declared in other scope as global variables
		if (SymbolTableStack.symbolTableStack.size() > 1){
			for (int i = 1; i < SymbolTableStack.symbolTableStack.size(); i++){
				symbolTable = SymbolTableStack.symbolTableStack.get(i);
				for (Map.Entry<String, Symbol> entry : symbolTable.getSymbolMap().entrySet()){
					Symbol symbol = entry.getValue();
					if (symbol.getValue() != null){
						System.out.println("str " + symbol.getName() + " " + symbol.getValue());
					}
				}
			}
		}
		
		System.out.println("push");	// where return value stores
		System.out.println("push");	//	4 empty for registers 
		System.out.println("push"); // 		although we never store anything here in the main function,  
		System.out.println("push"); //		we still need to push it because the return value slot is always $6 
		System.out.println("push");
		System.out.println("jsr main");
		System.out.println("sys halt");
		
		for (IRNode node : irList){
			System.out.print(IRAction.toTinyCode(node));
		}
		
	}
	
	
	///////////////////// DEBUGGING FUNCTIONS ///////////////////////////
	
	// Print the symbol table stack for deugging
	public static void printSymbolTableStack(){
		for (int i = 0; i < SymbolTableStack.symbolTableStack.size(); i++){
			System.out.println(SymbolTableStack.symbolTableStack.get(i));
		}
	}
	
	// Print the subscope stack for debugging
	public static void printSubScopeStack(){
		System.out.println("; SUBSCOPE for function scope: " + IRAction.currentFunctionScope);
		for (int i = 0; i < subScopeStack.size(); i++){
			System.out.println(subScopeStack.get(i));
		}
		System.out.println();
	}
	
	/////////////////////////OPTIMIZATION CODE////////////////////////
	// case: same return statement
	public static boolean isIF_ELSE_sameReturn(){
		if (!IRAction.is_IF_ELSE_return_comparison_cancelled && !IRAction.IF_return_literal.equals("") && IRAction.IF_return_literal.equals(IRAction.ELSE_return_literal)){
			return true;
		}
		return false;
	}
	public static LinkedList<IRNode> cloneIRList(){
		IRAction.is_IF_ELSE_return_comparison_cancelled = false;
		return (LinkedList<IRNode>) IRAction.irList.clone();
	}
	public static void resetIRListToPreIF(){
		IRAction.irList.clear();
		IRAction.irList = (LinkedList<IRNode>) IRAction.enterIFCloneIRList.clone();
	}
	public static void cancelIF_ELSE_sameReturnComparison(){
		IRAction.IF_return_literal = "";
		IRAction.ELSE_return_literal = "";
		IRAction.enterIFCloneIRList.clear();
		IRAction.is_IF_ELSE_return_comparison_cancelled = true;
	}
}