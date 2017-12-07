
import java.util.*;

public class AST {
	
	private static LinkedList<String> infixExpr = new LinkedList<String>();
	
	private static Stack<LinkedList<String>> infixExprStack = new Stack<LinkedList<String>>();
	
	private static void clearInfixExpr(){
		infixExpr.clear();
	}
	
	public static void addInfixExpr(String str){
		infixExpr.add(str);
	}
	
	public static void storeInfixExpr(){
		infixExprStack.push((LinkedList<String>) infixExpr.clone());
		infixExpr.clear();
	}
	
	public static void restoreInfixExpr(){
		try{
			infixExpr = (LinkedList<String>) infixExprStack.pop().clone();
		}catch(Exception ex){
			System.out.println("; RestoreInfixExpr Error");
		}
	}
	
	public static void DebugPrintInfix(){
		System.out.println("; Debug print infix:");
		for (int i = 0; i < infixExpr.size(); i++){
			System.out.print(infixExpr.get(i) + ", ");
		}
		System.out.println();
	}
	
	private static String infixOperationType(){
		String type = "";
		for (String token: infixExpr){
			if (TYTSMisc.isFLOATLITERAL(token)){
				type = "FLOAT";
				break;
			} else if (TYTSMisc.isINTLITERAL(token)){
				type = "INT";
				break;
			} else if (TYTSMisc.isIDENTIFIER(token)){
				type = IRAction.getDeclareType(token);
				break;
			}
		}
		return type;
	}
	
	// Basic Statement IR Actions
	// write_stmt actions
	public static LinkedList<IRNode> generate_IR_write_stmt(String id_list){
		LinkedList<IRNode> ir_stmt_list = new LinkedList<IRNode>();
		
		String[] list = id_list.split(",");
		for (String token : list){
			
			
			if (IRAction.getDeclareType(token).equals("INT")){
				// convert token to slot registers
				if (!SymbolTableStack.isGlobalVar(token)){
					token = IRAction.convertEntryToSlotLocation(IRAction.currentFunctionScope, token);
				}
				ir_stmt_list.add(new IRNode("WRITEI", token));
			} else if (IRAction.getDeclareType(token).equals("FLOAT")){
				// convert token to slot registers
				if (!SymbolTableStack.isGlobalVar(token)){
					token = IRAction.convertEntryToSlotLocation(IRAction.currentFunctionScope, token);
				}
				ir_stmt_list.add(new IRNode("WRITEF", token));
			} else if (IRAction.getDeclareType(token).equals("STRING")){
				ir_stmt_list.add(new IRNode("WRITES", token));
			}
		}
		
		return ir_stmt_list;
	}
	
	// read_stmt actions
	public static LinkedList<IRNode> generate_IR_read_stmt(String id_list){
		LinkedList<IRNode> ir_stmt_list = new LinkedList<IRNode>();
		
		String[] list = id_list.split(",");
		for (String token : list){
			
			if (IRAction.getDeclareType(token).equals("INT")){
				// convert token to slot registers
				if (!SymbolTableStack.isGlobalVar(token)){
					token = IRAction.convertEntryToSlotLocation(IRAction.currentFunctionScope, token);
				}
				ir_stmt_list.add(new IRNode("READI", token));
			} else if (IRAction.getDeclareType(token).equals("FLOAT")){
				// convert token to slot registers
				if (!SymbolTableStack.isGlobalVar(token)){
					token = IRAction.convertEntryToSlotLocation(IRAction.currentFunctionScope, token);
				}
				ir_stmt_list.add(new IRNode("READF", token));
			}
		}
		
		return ir_stmt_list;
	}
	
	
	public static LinkedList<IRNode> generate_IR_cond(String compop, String exprLeft, String exprRight){
		LinkedList<IRNode> ir_stmt_list = new LinkedList<IRNode>();
		
		String opcode = ""; 
		String source1 = "";
		String source2 = "";
		
		
		// get source 1
		if (TYTSMisc.isINTLITERAL(exprLeft)){
			String newReg = IRAction.createTempRegister("INT");
			ir_stmt_list.add(new IRNode("STOREI", exprLeft, newReg));
			source1 = newReg;
		} else if (TYTSMisc.isFLOATLITERAL(exprLeft)){
			String newReg = IRAction.createTempRegister("FLOAT");
			ir_stmt_list.add(new IRNode("STOREF", exprLeft, newReg));
			source1 = newReg;
		} else if (TYTSMisc.isIDENTIFIER(exprLeft) && IRAction.getDeclareType(exprLeft).equals("INT")){
			source1 = exprLeft;
		} else if (TYTSMisc.isIDENTIFIER(exprLeft) && IRAction.getDeclareType(exprLeft).equals("FLOAT")){
			source1 = exprLeft;
		} else {
			source1 = IRAction.condLeftReg;
		}
		
		
		boolean isFloatBranch = false;
		
		// get source 2
		if (TYTSMisc.isINTLITERAL(exprRight)){
			String newReg = IRAction.createTempRegister("INT");
			ir_stmt_list.add(new IRNode("STOREI", exprRight, newReg));
			source2 = newReg;
		} else if (TYTSMisc.isFLOATLITERAL(exprRight)){
			String newReg = IRAction.createTempRegister("FLOAT");
			ir_stmt_list.add(new IRNode("STOREF", exprRight, newReg));
			source2 = newReg;
			isFloatBranch = true;
		} else if (TYTSMisc.isIDENTIFIER(exprRight) && IRAction.getDeclareType(exprRight).equals("INT")){
			source2 = exprRight;
		} else if (TYTSMisc.isIDENTIFIER(exprRight) && IRAction.getDeclareType(exprRight).equals("FLOAT")){
			source2 = exprRight;
			isFloatBranch = true;
		} else {
			if (TYTSMisc.findReturnType(infixExpr).equals("FLOAT")){
				isFloatBranch = true;
			}
			LinkedList<IRNode> list = generate_IR_expr();
			
			for (IRNode node : list){
				ir_stmt_list.add(node);
			}
			source2 = IRAction.lastFunctionCallTempory;// IRAction.currentRegister();	
		}
		clearInfixExpr();
		
		// convert local var or function parameter to slot registers
		if (TYTSMisc.isIDENTIFIER(source1) && !SymbolTableStack.isGlobalVar(source1)){
			source1 = IRAction.convertEntryToSlotLocation(IRAction.currentFunctionScope, source1);
		}
		// convert local var or function parameter to slot registers
		if (TYTSMisc.isIDENTIFIER(source2)){
			if (!SymbolTableStack.isGlobalVar(source2)){
				source2 = IRAction.convertEntryToSlotLocation(IRAction.currentFunctionScope, source2);
			} else{
				// source 2 cannnot be a global var, but source 1 can
				if (isFloatBranch){
					String newReg = IRAction.createTempRegister("FLOAT");
					ir_stmt_list.add(new IRNode("STOREF", source2, newReg));
					source2 = newReg;
				}else{
					String newReg = IRAction.createTempRegister("INT");
					ir_stmt_list.add(new IRNode("STOREI", source2, newReg));
					source2 = newReg;
				}
			}
		}
		
		
		// evaluate the branch opcode
		if (!isFloatBranch) {
			opcode = TYTSMisc.getBranchComplementOpcode(compop, "INT");
		} else {
			opcode = TYTSMisc.getBranchComplementOpcode(compop, "FLOAT");
		}
		// determines the label target
		int labelTarget;
		if (IRAction.lstack.peek().isForLabels()){
			labelTarget = IRAction.lstack.peek().lc;
		} else { // is if labels
			labelTarget = IRAction.lstack.peek().la;
		}
		
		// add the branch IR noe
		if (!IRAction.isIRReg(source1) && !IRAction.isIRReg(source2)){
			if (!isFloatBranch){
				String newReg = IRAction.createTempRegister("INT");
				ir_stmt_list.add(new IRNode("STOREI", source2, newReg));
				ir_stmt_list.add(new IRNode(opcode, source1, newReg, labelTarget));
			}else{
				String newReg = IRAction.createTempRegister("FLOAT");
				ir_stmt_list.add(new IRNode("STOREF", source2, newReg));
				ir_stmt_list.add(new IRNode(opcode, source1, newReg, labelTarget));
			}
		} else {
			ir_stmt_list.add(new IRNode(opcode, source1, source2, labelTarget));
		}
		
		
		return ir_stmt_list;
	}
	
	// IRNode genearation rule is equivalent to assign_stmt
	public static LinkedList<IRNode> generate_IR_init_stmt(String assignOutputID, String returnType, String assignExpr){
		return generate_IR_assign_stmt(assignOutputID, returnType, assignExpr);
	}
	
	// IRNode generation rule is equivalent to assign_stmt
	public static LinkedList<IRNode> generate_IR_incr_stmt(String assignOutputID, String returnType, String assignExpr){
		return generate_IR_assign_stmt(assignOutputID, returnType, assignExpr);
	}
	
	// assign_stmt actions - generating IRNodes for an assign statement
	// idea: assign the RHS expression (IRNodes evaluated by expr) to the LHS (a gid* or $n*)
	// Possible Expression and its generated IR Node:
	//											STEP 1							STEP 2
	// assignExpr (RHS) type	example	        IR Nodes generated by expr 		Addition by assign_stmt
	// --------------------------------------------------------------------------------------------------------------------------
	// expression				(a + b) * c		many, ends with $Tn				STOREx $Tn, [gid/$n]
	// float literal			3.14			STOREF 3.14, $Tn				STOREx $Tn, [gid/$n]
	// int literal				3				STOREI 3, $Tn					STOREx $Tn, [gid/$n]
	// single identifier		a				(none)							STOREx [gid/$n], $Tn, then STOREx $Tn, [gid/$n]
	// single function call		foo(a, b+c)		(none)							STOREx $Tn***, [gid/$n]
	//
	// * gid - global variable identifier
	// * $n - local variable or function argument slot location
	// *** - a $Tn was generated by function call, assign that $Tn to LHS of the assign_expr 
	public static LinkedList<IRNode> generate_IR_assign_stmt(String assignOutputID, String returnType, String assignExpr){
		
		// possible assignOutputID
		// identifier
		//		local var
		// 		function para
		//		global var
		
		// Possible assignExpr:
		// int literal			3
		// float literal		3.14
		// identifier			id
		// 		local var	
		// 		function para	
		//		global var
		// function call		foo(), bar(3.4, 2.0+b)
		// expression			
		// 						a + b
		//						3.14 + 5.3
		//						2 * (3.14 + 5.3)
		//						bar() + 2
		
		// if the LHS of the assign statement is a local variable or function parameter
		// 			convert the LHS of the assign statement to slot location registers
		if (!SymbolTableStack.isGlobalVar(assignOutputID)){
			System.out.println("; pre-conversion assignOutputID: " + assignOutputID);
			assignOutputID = IRAction.convertEntryToSlotLocation(IRAction.currentFunctionScope, assignOutputID);
			System.out.println("; post-conversion assignOutputID: " + assignOutputID);
		}
		
		
		
		// STEP 1 : IR Nodes generated by expr
		LinkedList<IRNode> ir_stmt_list = generate_IR_expr();
		
		// STEP 2 : Additional IR Nodes generated by assign_stmt
		if (ir_stmt_list.size() == 0){
			if (TYTSMisc.isIDENTIFIER(assignExpr)){
				// assignExpr is a global var, or local var, or function argument (an identifier)
				if (!SymbolTableStack.isGlobalVar(assignExpr)){
					assignExpr = IRAction.convertEntryToSlotLocation(IRAction.currentFunctionScope, assignExpr);
				}
				
				
				if (returnType.equals("INT")){
					String newReg = IRAction.createTempRegister("INT");
					ir_stmt_list.add(new IRNode("STOREI", assignExpr, newReg));
					ir_stmt_list.add(new IRNode("STOREI", newReg, assignOutputID));
				} else if (returnType.equals("FLOAT")){
					String newReg = IRAction.createTempRegister("FLOAT");
					ir_stmt_list.add(new IRNode("STOREF", assignExpr, newReg));
					ir_stmt_list.add(new IRNode("STOREF", newReg, assignOutputID));
				}
			} else {
				// assignExpr is a single function call
				if (returnType.equals("INT")){
					ir_stmt_list.add(new IRNode("STOREI", IRAction.currentRegister(), assignOutputID));
				} else if (returnType.equals("FLOAT")){
					ir_stmt_list.add(new IRNode("STOREF", IRAction.currentRegister(), assignOutputID));
				}
			}
		}else{
			// assignExpr is an expression with +, -, *, or / operations
			if (returnType.equals("INT")){
				ir_stmt_list.add(new IRNode("STOREI", IRAction.currentRegister(), assignOutputID));
			} else if (returnType.equals("FLOAT")){
				ir_stmt_list.add(new IRNode("STOREF", IRAction.currentRegister(), assignOutputID));
			} else {
				System.out.println("; Error: invalid return type - " + returnType);
			}
		}
		
		
		//clearInfixExpr();
		return ir_stmt_list;
	}
	
	
	// return_stmt actions - generating IRNodes for a return statement
	// idea: assign the RHS expression (IRNodes evaluated by expr) to the return value slot ($6) in the stack
	// Possible Expression and its generated IR Node:
	//											STEP 1							STEP 2
	// assignExpr (RHS) type	example	        IR Nodes generated by expr 		Addition by return_stmt
	// --------------------------------------------------------------------------------------------------------------------------
	// expression				(a + b) * c		many, ends with $Tn				STOREx $Tn, $6
	// float literal			3.14			STOREF 3.14, $Tn				STOREx $Tn, $6
	// int literal				3				STOREI 3, $Tn					STOREx $Tn, $6
	// single identifier		a				(none)							STOREx [gid/$n], $Tn, then STOREx $Tn, $6
	// single function call		foo(a, b+c)		(none)							STOREx $Tn***, $6
	//
	// * gid - global variable identifier
	// * $n - local variable or function argument slot location
	// *** - a $Tn was generated by function call, assign that $Tn to return value slot register ($6) 
	public static LinkedList<IRNode> generate_IR_return_stmt(String assignExpr){
		
		// find the return type 
		String returnType = TYTSMisc.findReturnType(infixExpr);
		
		// STEP 1: IR Nodes generated by expr rules
		LinkedList<IRNode> ir_stmt_list = generate_IR_expr();
		
		// STEP 2: additional IR Nodes generated by return_stmt rules
		if (ir_stmt_list.size() == 0){
			if (TYTSMisc.isIDENTIFIER(assignExpr)){
				// assignExpr is a global var, or local var, or function argument (an identifier)
				if (!SymbolTableStack.isGlobalVar(assignExpr)){
					assignExpr = IRAction.convertEntryToSlotLocation(IRAction.currentFunctionScope, assignExpr);
				}
				
				
				if (returnType.equals("INT")){
					String newReg = IRAction.createTempRegister("INT");
					ir_stmt_list.add(new IRNode("STOREI", assignExpr, newReg));
					ir_stmt_list.add(new IRNode("STOREI", newReg, SymbolTableStack.returnValueSlot));
				} else if (returnType.equals("FLOAT")){
					String newReg = IRAction.createTempRegister("FLOAT");
					ir_stmt_list.add(new IRNode("STOREF", assignExpr, newReg));
					ir_stmt_list.add(new IRNode("STOREF", newReg, SymbolTableStack.returnValueSlot));
				}
			} else {
				// assignExpr is a single function call
				if (returnType.equals("INT")){
					ir_stmt_list.add(new IRNode("STOREI", IRAction.currentRegister(), SymbolTableStack.returnValueSlot));
				} else if (returnType.equals("FLOAT")){
					ir_stmt_list.add(new IRNode("STOREF", IRAction.currentRegister(), SymbolTableStack.returnValueSlot));
				}
			}
		}else{
			// assignExpr is an expression with +, -, *, or / operations
			if (returnType.equals("INT")){
				ir_stmt_list.add(new IRNode("STOREI", IRAction.currentRegister(), SymbolTableStack.returnValueSlot));
			} else if (returnType.equals("FLOAT")){
				ir_stmt_list.add(new IRNode("STOREF", IRAction.currentRegister(), SymbolTableStack.returnValueSlot));
			}
		}
		
		
		//clearInfixExpr();
		return ir_stmt_list;
	}
	
	// expr in function call
	public static LinkedList<IRNode> generate_IR_function_call_para_expr(){
		LinkedList<IRNode> ir_stmt_list = generate_IR_expr();
		//clearInfixExpr();
		return ir_stmt_list;
	}
	
	// expr actions - Generating IRNodes for expr
	// Possible Expression and its generated IR Node:
	// type						example	        IR Nodes generated by expr 	
	// ---------------------------------------------------------------------
	// expression				(a + b) * c		many, ends with $Tn		
	// float literal			3.14			STOREF 3.14, $Tn
	// int literal				3				STOREI 3, $Tn
	// single identifier		a				(none)
	// single function call		foo(a, b+c)		(none)
	public static LinkedList<IRNode> generate_IR_expr(){
		
		LinkedList<IRNode> ir_stmt_list = new LinkedList<IRNode>();
		IRAction.tstack.clear();
		
		// convert the infix assign representation to postfix representation
		LinkedList<String> postfixAssignExpr = TYTSMisc.toPostfix(infixExpr);
		
		// find the return type from the postfix expression
		String returnType = TYTSMisc.findReturnType(postfixAssignExpr);
		
		/*
		System.out.println("; ----- DEBUGGING infixExpr --------- " + returnType);
		System.out.print(";  ");
		for (String str : infixExpr){
			System.out.print(str + " ");
		}
		System.out.println();
		*/
		
		// iterate through the postfix representation list
		// if a token is an operator, perform: pop, pop, allocate and push back new register
		// if a token is an identifier, perform: push
		
		for (String token: postfixAssignExpr){
			if (TYTSMisc.isOperator(token)){
				// create IR Node for the operator token
				
				String newReg;
				if (returnType.equals("INT")){
					newReg = IRAction.createTempRegister("INT");
				}else {
					newReg = IRAction.createTempRegister("FLOAT");
				}
				
				String source2 = IRAction.tstack.pop();
				String source1 = IRAction.tstack.pop();
				
				if (token.equals("+")){
					if (returnType.equals("INT")){
						ir_stmt_list.add(new IRNode("ADDI", source1, source2, newReg));
					} else if (returnType.equals("FLOAT")){
						ir_stmt_list.add(new IRNode("ADDF", source1, source2, newReg));
					}
				} else if (token.equals("-")){
					if (returnType.equals("INT")){
						ir_stmt_list.add(new IRNode("SUBI", source1, source2, newReg));
					} else if (returnType.equals("FLOAT")){
						ir_stmt_list.add(new IRNode("SUBF", source1, source2, newReg));
					}
				} else if (token.equals("*")){
					if (returnType.equals("INT")){
						ir_stmt_list.add(new IRNode("MULTI", source1, source2, newReg));
					} else if (returnType.equals("FLOAT")){
						ir_stmt_list.add(new IRNode("MULTF", source1, source2, newReg));
					}
				} else if (token.equals("/")){
					if (returnType.equals("INT")){
						ir_stmt_list.add(new IRNode("DIVI", source1, source2, newReg));
					} else if (returnType.equals("FLOAT")){
						ir_stmt_list.add(new IRNode("DIVF", source1, source2, newReg));
					}
				}
				
				IRAction.tstack.push(newReg);
				
			} else {
				String fillin = token;
				
				// create IR Node if the token is int or float literal
				// convert non-global identifier token (local var or function parameter) to its slot register
				
				// possible token format					   example	Actions to token
				// int literal									4		STOREI 4, $Tn	 , then express literal as $Tn
				// float literal 								3.14	STOREF 3.14, $Tn , then express literal as $Tn
				// identifier, global							gid		none, keep expressing as gid
				// identifier, local or function parameter		id		convert to slot register expression $n
				// function call return register				$Tn		none, keep expressing as $Tn

				// store literal to a temperory regiser
				if (TYTSMisc.isFLOATLITERAL(token)){
					String newReg = IRAction.createTempRegister("FLOAT");
					ir_stmt_list.add(new IRNode("STOREF", token, newReg));
					fillin = newReg;
				// store literal to a temperory regiser
				} else if (TYTSMisc.isINTLITERAL(token)){
					String newReg = IRAction.createTempRegister("INT");
					ir_stmt_list.add(new IRNode("STOREI", token, newReg));
					fillin = newReg;
				// convert function parameter or local variable to slot register expression
				} else if (TYTSMisc.isIDENTIFIER(token)){
					if (!SymbolTableStack.isGlobalVar(token)){
						fillin = IRAction.convertEntryToSlotLocation(IRAction.currentFunctionScope, token);
					}
				} 		
				
				IRAction.tstack.push(fillin);
			}
		}
		
		IRAction.tstack.clear();
		clearInfixExpr();
		return ir_stmt_list;
	}
	
	
}
