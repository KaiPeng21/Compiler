
import java.util.*;

public class TYTSListener extends MicroBaseListener {
	
	
	@Override public void enterProgram(MicroParser.ProgramContext ctx) 
	{ 
		// Debuging (Symbol Table Stack)
		IRAction.printSymbolTableStack();
		
	}
	
	@Override public void exitProgram(MicroParser.ProgramContext ctx) 
	{ 
		
		// Print the Intermediate 3 Address Code
		IRAction.printIRList();
		
		// Print the Intermediate 3 Address Code with CFG constructed
		CFG.constructCFG(IRAction.irList);
		CFG.printCFG_irList();
		CFG.printKillGen_irList();
		
		CFG.computeLiveness();
		CFG.printLiveness_irList();
		
		
		// Print the output tiny code (non-ralloc version)
		// IRAction.printTinyCode();
		
		// Print the output tiny code (ralloc version)
		RALL.ralloc(CFG.CFG_irList);
		RALL.printFinalTinyCode();
		
	}
	
	
	
	@Override public void enterIf_stmt(MicroParser.If_stmtContext ctx) 
	{ 
		// push a IF subscope to the subscope stack
		IRAction.subScopeStack.push(new IRScope(IRScope.IF_TYPE, IRAction.allocateScopeBlockID()));
		
		IRAction.cancelIF_ELSE_sameReturnComparison(); // reset the IF ELSE same return comparision
		IRAction.enterIFCloneIRList = IRAction.cloneIRList();  // save the current IR list
		
		// allocate the 2 labels for the IF statement 
		int la = IRAction.allocateLabel(); 
		int lb = IRAction.allocateLabel(); 
		IRAction.lstack.push(new Label(la, lb));
		
		IRAction.printSubScopeStack();
	}
	
	@Override public void exitIf_stmt(MicroParser.If_stmtContext ctx) 
	{ 
		// pop the ELSE subscope
		IRAction.subScopeStack.pop();
		
		// insert the second if label to the final irlist 
		IRAction.appendIRList(new IRNode(IRType.LABEL, IRAction.lstack.peek().lb));
		IRAction.lstack.pop();
	}
	
	@Override public void enterElse_part(MicroParser.Else_partContext ctx) 
	{ 
		// pop the IF subscope and push an ELSE subscope to the subscope stack
		IRAction.subScopeStack.pop();
		IRAction.subScopeStack.push(new IRScope(IRScope.ELSE_TYPE, IRAction.allocateScopeBlockID()));
		
		IRAction.printSubScopeStack();
		
		// jump to FI
		IRAction.appendIRList(new IRNode(IRType.JUMP, IRAction.lstack.peek().lb));
		// insert the first if label to the final irlist 
		IRAction.appendIRList(new IRNode(IRType.LABEL, IRAction.lstack.peek().la));
	}
	
	@Override public void enterFor_stmt(MicroParser.For_stmtContext ctx) 
	{ 
		// push an FOR subscope to the subscope stack
		IRAction.subScopeStack.push(new IRScope(IRScope.FOR_TYPE, IRAction.allocateScopeBlockID()));
		
		// allocate the 3 labels for the FOR stmt
		int la = IRAction.allocateLabel(); 
		int lb = IRAction.allocateLabel(); 
		int lc = IRAction.allocateLabel(); 
		IRAction.lstack.push(new Label(la, lb, lc));
		
		
		IRAction.printSubScopeStack();
	}
	
	@Override public void exitFor_stmt(MicroParser.For_stmtContext ctx) 
	{ 
		// << End of FOR Loop >>
		// ;label b
		// [[ incr_stmt ]]
		// ;jump a
		// ;label c
		
		// insert the second for loop label to the final irlist (this is not needed as our for loop doesn't have a CONTINUE keyword) 
		// IRAction.appendIRList(new IRNode(IRType.LABEL , IRAction.lstack.peek().lb));
		
		// insert incr_stmt statement
		LinkedList<IRNode> popped = IRAction.incrStack.pop();
		if (!popped.get(0).isNOP()){
			IRAction.appendIRList(popped);
		}
		// insert a JUMP instruction to the first for loop label
		IRAction.appendIRList(new IRNode(IRType.JUMP, IRAction.lstack.peek().la));
		// insert the third for loop label to the final irlist 
		IRAction.appendIRList(new IRNode(IRType.LABEL, IRAction.lstack.peek().lc));
		// remove the 3 label after exiting the for loop
		IRAction.lstack.pop();
		
		// pop the FOR subscope from the subscope stack
		IRAction.subScopeStack.pop();
	}
	
	
	@Override public void exitInit_stmt(MicroParser.Init_stmtContext ctx) 
	{ 
		// If init_stmt is not empty, generate IR node for init_stmt
		if (ctx.getChildCount() != 0){
			String assignOutputID = ctx.getChild(0).getChild(0).getText();
			String assignExpr = ctx.getChild(0).getChild(2).getText();
			IRAction.appendIRList(AST.generate_IR_init_stmt(assignOutputID, IRAction.getDeclareType(assignOutputID), assignExpr));
			
			if (SymbolTableStack.isGlobalVar(assignOutputID)){
				IRAction.cancelIF_ELSE_sameReturnComparison();
			}
		}
		// insert the first for loop label to the final irlist 
		IRAction.appendIRList(new IRNode(IRType.LABEL, IRAction.lstack.peek().la));
	}
	
	@Override public void exitIncr_stmt(MicroParser.Incr_stmtContext ctx) 
	{
		if (ctx.getChildCount() != 0){
			// if the incr_stmt in the for loop is not empty
			String assignOutputID = ctx.getChild(0).getChild(0).getText();
			String assignExpr = ctx.getChild(0).getChild(2).getText();
			IRAction.incrStack.push(AST.generate_IR_incr_stmt(assignOutputID, IRAction.getDeclareType(assignOutputID), assignExpr));
			
			if (SymbolTableStack.isGlobalVar(assignOutputID)){
				IRAction.cancelIF_ELSE_sameReturnComparison();
			}
		}else{
			LinkedList<IRNode> list = new LinkedList<IRNode>();
			list.add(new IRNode());
			IRAction.incrStack.push(list);
		}
	}
	
	@Override public void exitCond(MicroParser.CondContext ctx) 
	{ 
		
		
		String compop = ctx.getChild(1).getText();
		String exprLeft = ctx.getChild(0).getText();
		String exprRight = ctx.getChild(2).getText();
		
		System.out.println(";exit Cond: " + compop + " :: " + exprLeft + " :: " + exprRight);
		
		LinkedList<IRNode> ir_stmt_list = AST.generate_IR_cond(compop, exprLeft, exprRight);
		IRAction.appendIRList(ir_stmt_list);
		
	}
	
	@Override public void enterCompop(MicroParser.CompopContext ctx) 
	{ 
		LinkedList<IRNode> ir_stmt_list = AST.generate_IR_expr();
		IRAction.appendIRList(ir_stmt_list);
		IRAction.condLeftReg = IRAction.currentRegister();
		
	}
	
	@Override public void exitAssign_stmt(MicroParser.Assign_stmtContext ctx) 
	{ 
		// generate the assignment statement IR code
		String assignOutputID = ctx.getChild(0).getChild(0).getText();
		String assignExpr = ctx.getChild(0).getChild(2).getText();
		LinkedList<IRNode> ir_stmt_list = AST.generate_IR_assign_stmt(assignOutputID, IRAction.getDeclareType(assignOutputID), assignExpr);
		IRAction.appendIRList(ir_stmt_list);
		
		if (SymbolTableStack.isGlobalVar(assignOutputID)){
			IRAction.cancelIF_ELSE_sameReturnComparison();
		}
	}
	
	@Override public void exitAddop(MicroParser.AddopContext ctx) 
	{ 
		// add +, - token to the assigment AST infix representation
		AST.addInfixExpr(ctx.getText());
	}
	@Override public void exitMulop(MicroParser.MulopContext ctx) 
	{ 		
		// add *, / token to the assigmenet AST infix representation
		AST.addInfixExpr(ctx.getText());
	}
	
	@Override public void enterPrimary(MicroParser.PrimaryContext ctx) 
	{ 
		// add the left '(' to assignment AST infix representation
		if (ctx.getChildCount() > 1) {
			//System.out.println("( expr ):  " + ctx.getChild(0).getText());
			AST.addInfixExpr(ctx.getChild(0).getText());
		}
	}
	
	@Override public void exitPrimary(MicroParser.PrimaryContext ctx) 
	{ 
		// add the identifier token or ")" to the AST infix representation list
		if (TYTSMisc.isINTLITERAL(ctx.getChild(0).getText())){
			AST.addInfixExpr(ctx.getChild(0).getText());
		} else if (TYTSMisc.isFLOATLITERAL(ctx.getChild(0).getText())){
			AST.addInfixExpr(ctx.getChild(0).getText());
		} else if (TYTSMisc.isIDENTIFIER(ctx.getChild(0).getText())){
			AST.addInfixExpr(ctx.getChild(0).getText());
		} else {
			AST.addInfixExpr(ctx.getChild(2).getText());
		}
		
	}
	
	@Override public void exitWrite_stmt(MicroParser.Write_stmtContext ctx) 
	{ 
		IRAction.cancelIF_ELSE_sameReturnComparison(); // reset the IF ELSE same return comparision
		
		String idlist = ctx.getChild(2).getText();
		LinkedList<IRNode> ir_stmt_list = AST.generate_IR_write_stmt(idlist);
		IRAction.appendIRList(ir_stmt_list);
	}
	
	@Override public void exitRead_stmt(MicroParser.Read_stmtContext ctx) 
	{ 
		IRAction.cancelIF_ELSE_sameReturnComparison(); // reset the IF ELSE same return comparision
		
		String idlist = ctx.getChild(2).getText();
		LinkedList<IRNode> ir_stmt_list = AST.generate_IR_read_stmt(idlist);
		IRAction.appendIRList(ir_stmt_list);
	}
	
	@Override public void exitReturn_stmt(MicroParser.Return_stmtContext ctx) 
	{ 
		// generate the return statement IR code
		String assignExpr = ctx.getChild(1).getText();
		LinkedList<IRNode> ir_stmt_list = AST.generate_IR_return_stmt(assignExpr);
		IRAction.appendIRList(ir_stmt_list);
		
		
		/// <<< OPTIMIZATION: when both return statement in the IF block and the ELSE block return the same literal
		//  and there is no read/write statement in the IF/ELSE block
		//  reset the irList to the pre-saved IRList when entering the IF statement (remove everything in the block)
		//  add a single return statement back
		if (!IRAction.subScopeStack.isEmpty() && IRAction.subScopeStack.peek().getScopeType() == IRScope.IF_TYPE){
			if (TYTSMisc.isFLOATLITERAL(assignExpr) || TYTSMisc.isINTLITERAL(assignExpr)){
				IRAction.IF_return_literal = assignExpr;
			} else{
				IRAction.cancelIF_ELSE_sameReturnComparison();
			}
		} else if (!IRAction.subScopeStack.isEmpty() && IRAction.subScopeStack.peek().getScopeType() == IRScope.ELSE_TYPE){
			if (TYTSMisc.isFLOATLITERAL(assignExpr) || TYTSMisc.isINTLITERAL(assignExpr)){
				IRAction.ELSE_return_literal = assignExpr;
				if (IRAction.isIF_ELSE_sameReturn()){
					IRAction.resetIRListToPreIF();
					IRAction.appendIRList(ir_stmt_list);
				} else{
					IRAction.cancelIF_ELSE_sameReturnComparison();
				}
			} else{
				IRAction.cancelIF_ELSE_sameReturnComparison();
			}
		}
		System.out.println(";OPT BUG");
		IRAction.printSubScopeStack();
		System.out.println(";return assignExpr: " + assignExpr);
		System.out.println(";left: " + IRAction.IF_return_literal);
		System.out.println(";right: " + IRAction.ELSE_return_literal);
		System.out.println(";IRAction.isIF_ELSE_sameReturn(): " + IRAction.isIF_ELSE_sameReturn());
		
		
		// add unlink node:				unlink
		IRAction.appendIRList(new IRNode(IRType.UNLINK, 0));
		// add ret node:				ret
		IRAction.appendIRList(new IRNode(IRType.RET, ""));
	}
	
	// CALL STACK FORMAT:
	//					slot value
	// Parameter n			n + 6
	//	 ...
	// Parameter 3			9
	// Parameter 2			8
	// Parameter 1			7
	// Return Value			6
	// Register 0			5
	// Register 1			4
	// Register 2			3
	// Register 3			2
	// Return Address 		1
	// Frame Pointer		0
	// Local Var 1			-1
	// Local Var 2			-2
	// Local Var 3			-3
	//	....
	// Local Var n			-n
	//
	//
	// Given the example:
	//
	//	FUNCTION ANYTYPE functionName (Type parameter_1, Type parameter_2, Type parameter_3, ..., Type parameter_n)
	//  BEGIN
	//		Type localVar_1;
	//		Type localVar_2;
	//		Type localVar_3;
	//			...
	//		Type localVar_n;
	//
	//		[statement list...]
	//  END
	//
	
	// entering a function call
	@Override public void enterCall_expr(MicroParser.Call_exprContext ctx) 
	{ 
		// store the current infixExpr list
		AST.storeInfixExpr();
		
		// save the caller argument / parameter stack
		IRAction.callerParaStackStorageStack.push((Stack<String>) IRAction.callerParaStack.clone());
		IRAction.callerParaStack.clear();
		
		IRAction.cancelIF_ELSE_sameReturnComparison();
	}
	
	// expression in the function call list
	@Override public void enterExpr_list_tail(MicroParser.Expr_list_tailContext ctx) 
	{ 
		// expr_list 	  -> expr expr_list_tail	|	empty
		// expr_list_tail -> , expr expr_list_tail	|	empty
		
		// get the expr string
		String assignExpr = ctx.getParent().getChild(0).getText();
		if (assignExpr.equals(",")){
			assignExpr = ctx.getParent().getChild(1).getText();
		}
		
		LinkedList<IRNode> ir_stmt_list = AST.generate_IR_function_call_para_expr();
		IRAction.appendIRList(ir_stmt_list);
		
		String parameterRegister = "";
		// check if the assignExpr is 
		// - a global variable
		// - a local variable or current function scope function parameter
		// - an expression
		if (SymbolTableStack.isGlobalVar(assignExpr)){	// global var
			parameterRegister = assignExpr;
		} else if (TYTSMisc.isIDENTIFIER(assignExpr)){	// identifier - either local or parameter
			parameterRegister = IRAction.convertEntryToSlotLocation(IRAction.currentFunctionScope, assignExpr);
		} else {	// an expression, e.g. a + b * 5 OR foo(a, b+bar())
			parameterRegister = IRAction.currentRegister();
		}
		
		IRAction.callerParaStack.push(parameterRegister);
		
	}
	
	// caller calling the function
	@Override public void exitCall_expr(MicroParser.Call_exprContext ctx) 
	{ 
		///// Parse Data from function call
		// getting function label name
		String function_label_name = ctx.getChild(0).getText();
		// get the size of the parameters
		int parameterSize = SymbolTableStack.getParameterSize(function_label_name);
		// get the return type
		String returnType = SymbolTableStack.findReturnType(function_label_name);
		
		// PUSH argument
		while(!IRAction.callerParaStack.isEmpty()){
			IRAction.appendIRList(new IRNode(IRType.PUSH, IRAction.callerParaStack.pop()));
		}
		
		// PUSH return value
		IRAction.appendIRList(new IRNode(IRType.PUSH, ""));
		
		// PUSH register
		IRAction.appendIRList(new IRNode(IRType.PUSHREG, ""));
		
		// JSR function
		IRAction.appendIRList(new IRNode(IRType.JSR, function_label_name));
		
		// POP register
		IRAction.appendIRList(new IRNode(IRType.POPREG, ""));
		
		// POP return value to a new temp register
		String newReg = IRAction.createTempRegister(returnType);
		IRAction.appendIRList(new IRNode(IRType.POP, newReg));
		
		// POP argument
		for (int i = 0; i < parameterSize; i++){
			IRAction.appendIRList(new IRNode(IRType.POP, ""));
		}
		
		// restore the infixExpr list		
		// add the return register back to the restored infixExpr list
		AST.restoreInfixExpr();
		AST.addInfixExpr(newReg);
		IRAction.lastFunctionCallTempory = newReg;
		
		// restore the caller argument/ parameter stack
		try{
			IRAction.callerParaStack.clear();
			IRAction.callerParaStack = (Stack<String>) IRAction.callerParaStackStorageStack.pop().clone();
		}catch(Exception ex){
			System.out.println("; caller Para stack storage Error");
		}
	}
	
	
	
	// beginning of function scope
	@Override public void enterFunc_decl(MicroParser.Func_declContext ctx) 
	{ 		
		// get function label name
		String function_label_name = ctx.getChild(2).getText();
		// find the number of parameters
		int number_of_parameters = ctx.getChild(4).getText().split(",").length;
		if (ctx.getChild(4).getText().equals("")){
			number_of_parameters = 0;
		}
		
		// Set the current function scope name
		IRAction.currentFunctionScope = function_label_name;
		
		// clear the subscope stack (for, if, else blocks) as the function enters the new scope 
		IRAction.subScopeStack.clear();
			
		// add function label node:		label [function name]
		IRAction.appendIRList(new IRNode(IRType.FUNCLABEL, function_label_name));
		
		// add link node:				link #(linkval)
		int linkval = 1;
		// find the number of local variable for the current function scope
		linkval = SymbolTableStack.getFullLocalVarSize(IRAction.currentFunctionScope) + 1;
		IRAction.appendIRList(new IRNode(IRType.LINK, linkval));
		
	}
	
	// end of function scope
	@Override public void exitFunc_decl(MicroParser.Func_declContext ctx) 
	{ 
		// if this is a void function without return statement, insert a return at the end of the function declaration
		if (SymbolTableStack.findReturnType(IRAction.currentFunctionScope).equals("VOID")){
			// add unlink node:				unlink
			IRAction.appendIRList(new IRNode(IRType.UNLINK, 0));
			// add ret node:				ret
			IRAction.appendIRList(new IRNode(IRType.RET, ""));
		}
		
		// reset the current function scope name:
		IRAction.currentFunctionScope = "";
	}
	
}
