
import java.util.*;

public class RegAlloc{
	
	// size of registers
	public static final int REGSIZE = 4;
	
	// The 4 Registers
	private static RegEntry[] regs = {
		new RegEntry(),
		new RegEntry(),
		new RegEntry(),
		new RegEntry()
	};
	
	// IR list with register allocated
	public static LinkedList<LinkedList<IRNode>> ralloc_irList = new LinkedList<LinkedList<IRNode>>();
	// sub IR list with register allocated
	private static LinkedList<IRNode> ralloc_subirList = new LinkedList<IRNode>();
	// preprocessing node
	private static IRNode processingNode;
	
	
	
	// given an IR List with liveness computed, generate an IR list with register allocated
	public static void ralloc(LinkedList<LinkedList<IRNode>> live_irList){
		// processing ir list for each function scope
		for (LinkedList<IRNode> live_subirList : live_irList){
			
			
			// Bottom-up register allocation works at the basic-block level: 
			// any register allocation decisions you make apply for the current basic block only. 
			// This means that when you get to the end of a basic block, you must reset your register allocation. 
			// Any register that 
			// 		(a) hold local/global variables and 
			//		(b) is dirty should be written back to the stack/global variable.
			flush_registers();
			
			// create an empty list for register allocated ir node
			ralloc_subirList = new LinkedList<IRNode>();
			
			// processing each ir node in a sub ir list
			for (IRNode livenode : live_subirList){
				processingNode = livenode;
				
				// GENERAL STEPS:
				//	1. Ensure:		'ensure' all the variables that are "USED" in the processing node
				//	2. Free:		if the "USED" variables are dead, 'free' the register storing them
				//	3. Allocate:	'allocate' register for variables that are "DEFINED" and mark the register dirty
				
																			// reconstruct the IRNode with the ralloc register
				if (processingNode.getType() == IRType.THREE_ENTRY){		//	ADDx, SUBx, MULx, DIVx
					ralloc_Three_Entry();									//		2 used and 1 defined
				} else if (processingNode.getType() == IRType.TWO_ENTRY){	//	STOREx, special case when storing to return val slot
					ralloc_Store();
				} else if (processingNode.getType() == IRType.ONE_ENTRY){
					if (processingNode.getOpcode().charAt(0) == 'R'){		//	READx
						ralloc_Read();										//		0 used and 1 defined
					} else{													//	WRITEx, (special case for WRITES)
						ralloc_Write();										//		1 used and 0 defined
					}
				} else if (processingNode.getType() == IRType.PUSH){		//	PUSH [$PPVAL]
					ralloc_Push();											//		1 or 0 used and 1 defined
				} else if (processingNode.getType() == IRType.POP){			//	POP [$PPVAL]
					ralloc_Pop();											//		0 used and 1 or 0 defined
				} else if (processingNode.getType() == IRType.BRANCH){		//	Branch Instructions
					ralloc_Branch();										//		2 used and 0 defined
				} else if (processingNode.getType() == IRType.JSR){			//	JSR Instruction, special care!!
					share_global();
					ralloc_subirList.add(processingNode);
				} else {													//	OTHERS:
					ralloc_subirList.add(processingNode);					// 		0 used and 0 defined
				}				
				
				for (RegEntry r : regs){
					r.incLRU_Level();
				}
				
				
				ralloc_subirList.getLast().appendRallocMessage("\n;      pre-ralloc: " + processingNode);
				ralloc_subirList.getLast().appendRallocMessage(getRegTable() + processingNode.getLiveOutString());
			}
			
			// append the register allocated IR list to the final worklist
			ralloc_irList.add(ralloc_subirList);
		}
	}
	
	// ralloc ADDx, SUBx, MULx, DIVx instructions: 2 used, 1 defined
	// 		- ensure the two used variables are in the register
	//		- free the two used varibles if they are dead after this process
	//		- allocate a register for the defined variable and mark it dirty
	//		- reconstruct the instruction using the ralloc registers
	//
	//	When converting IR code to tiny code, tiny does two operations
	//
	//		tiny does: (D) <= (S1), then (D) <= (D) op (S2)
	//
	//	Ex:
	//		SUBI S1 S2 D	is converted to		move 	S1 D
	//											subi	S2 D 
	//
	//	OPCODEx $R0 $R1 $R0 	becomes		move $R0 $R0	
	//										opcx $R1 $R0	(This is okay)
	//
	//	OPCODEx $R0 $R1 $R1		becomes		move $R0 $R1	
	//										opcx $R1 $R1	(This causes a problem)
	//
	//	Therefore, to avoid cases like OPCODEx $R0 $R1 $R1, 
	//		do not free U2 (second used, $R1 in this case) until D (defined) is allocated 
	private static void ralloc_Three_Entry(){
		
		// STOREx   S1, D
		// OPRx		D, S2, D
		
		
		
		// retrieve used and defined variables
		String U1 = processingNode.getSource1();
		String U2 = processingNode.getSource2();
		String D1 = processingNode.getDest();
		
		// determine if the processing type is INT or FLOAT
		String opcode = processingNode.getOpcode();
		String type = "INT";
		if (opcode.charAt(opcode.length() - 1) == 'F'){
			type = "FLOAT";
		}
		
		// ensure the used variables are in the register
		RegEntry r_u1 = ensure(U1, type);
		RegEntry r_u2 = ensure(U2, type);
		
		// if used variables are dead after this process, free the register storing those variables
		if (!processingNode.liveoutset.contains(r_u1.getVarName())){
			free(r_u1);
		}
		
		// allocate space for the defined variable and mark its register dirty
		RegEntry r_d1 = allocate(D1, type);
		r_d1.setDirty(true);
		
		
		if (!processingNode.liveoutset.contains(r_u2.getVarName())){
			free(r_u2);
		}
		
		// reconstruct ralloced IRNode for this process 
		ralloc_subirList.add(new IRNode(opcode, r_u1.getRegExpr(), r_u2.getRegExpr(), r_d1.getRegExpr()));
		
	}
	
	// ralloc STOREx instructions: 1 or 0 used, 1 defined
	// 		- ensure the used variables are in the register
	//		- free the used varibles if they are dead after this process
	//		- allocate a register for the defined variable and mark it dirty
	//		- reconstruct the instruction using the ralloc registers
	private static void ralloc_Store(){
		
		/*
		// retrieve used and defined variables
		String U1 = processingNode.getSource1();
		String D1 = processingNode.getDest();
		
		// determine if the processing type is INT or FLOAT
		String opcode = processingNode.getOpcode();
		String type = "INT";
		if (opcode.endsWith("F")){
			type = "FLOAT";
		}
		
		// case 0 storing to return value
		if (D1.equals(SymbolTableStack.returnValueSlot)){
			if (TYTSMisc.isLiteral(U1)){
				// MOVE [literal], $(returnValueSlot)
				ralloc_subirList.add(new IRNode(opcode, U1, D1));
			} else { 
				// MOVE [tempories], $(returnValueSlot)
				
				// ensure USED tempory
				RegEntry r_u1 = ensure(U1, type);
				// free register storing USED tempory
				free(r_u1);
				
				ralloc_subirList.add(new IRNode(opcode, r_u1.getRegExpr(), D1));
			}
		} else {
			// case 1 move [literal] to [mem/stack]
			if (TYTSMisc.isLiteral(U1) && TYTSMisc.isMemoryAccessExpr(D1)){
				
				// allocate space for the defined variable and mark its register dirty
				RegEntry r_d1 = allocate(D1, type);
				r_d1.setDirty(true);
				
				ralloc_subirList.add(new IRNode(opcode, U1, r_d1.getRegExpr()));
				
			// case 2 move [literal] to [tempories]
			} else if (TYTSMisc.isLiteral(U1) && TYTSMisc.isTempReg(D1)){
				
				// allocate space for the defined variable and mark its register dirty
				RegEntry r_d1 = allocate(D1, type);
				r_d1.setDirty(true);
				
				ralloc_subirList.add(new IRNode(opcode, U1, r_d1.getRegExpr()));
				
			// case 3 move [tempories] to [mem/stack]	
			} else if (TYTSMisc.isTempReg(U1) && TYTSMisc.isMemoryAccessExpr(D1)){
				
				// ensure USED tempory
				RegEntry r_u1 = ensure(U1, type);
				// free register storing USED tempory
				free(r_u1);
				// allocate space for the defined variable and mark its register dirty
				RegEntry r_d1 = allocate(D1, type);
				r_d1.setDirty(true);
				
				ralloc_subirList.add(new IRNode(opcode, r_u1.getRegExpr(), r_d1.getRegExpr()));
				
			// case 4 move [tempories] to [tempories]
			} else if (TYTSMisc.isTempReg(U1) && TYTSMisc.isTempReg(D1)){
				
				// ensure USED tempory
				RegEntry r_u1 = ensure(U1, type);
				// free register storing USED tempory
				free(r_u1);
				// allocate space for the defined variable and mark its register dirty
				RegEntry r_d1 = allocate(D1, type);
				r_d1.setDirty(true);
				
				ralloc_subirList.add(new IRNode(opcode, r_u1.getRegExpr(), r_d1.getRegExpr()));
				
			// case 5 move [mem/stack] to [tempories]
			} else if (TYTSMisc.isMemoryAccessExpr(U1) && TYTSMisc.isTempReg(D1)){
				
				// ensure USED tempory
				RegEntry r_u1 = ensure(U1, type);
				// free register storing USED tempory
				free(r_u1);
				// allocate space for the defined variable and mark its register dirty
				RegEntry r_d1 = allocate(D1, type);
				r_d1.setDirty(true);
				
				ralloc_subirList.add(new IRNode(opcode, r_u1.getRegExpr(), r_d1.getRegExpr()));
				
			}
		}
		*/
		
		
		// tiny instruction rule:
		// move opmrl opmr        ; only one operand can be a memory id or stack variable
		// opmrl:	stands for a memory id, stack variable, register or a number (literal)
		// opmr:	stands for a memory id, stack variable, or a register
		
		
		// retrieve used and defined variables
		String U1 = processingNode.getSource1();
		String D1 = processingNode.getDest();
		
		// determine if the processing type is INT or FLOAT
		String opcode = processingNode.getOpcode();
		String type = "INT";
		if (opcode.charAt(opcode.length() - 1) == 'F'){
			type = "FLOAT";
		}
		
		String newSource1 = U1;
		String newDest = D1;
		
		// No need to ensure or free if source1 is an INT or a FLOAT literal or a global var (used = 0)
		if (!TYTSMisc.isFLOATLITERAL(U1) && !TYTSMisc.isINTLITERAL(U1) && !SymbolTableStack.isGlobalVar(U1)){
			// ensure the used variables are in the register
			RegEntry r_u1 = ensure(U1, type);
			newSource1 = r_u1.getRegExpr();
		
			// if used variables are dead after this process, free the register storing those variables
			if (!processingNode.liveoutset.contains(r_u1.getVarName())){
				free(r_u1);
			}
		}
		
		// No need to allocate for defined if storing to the return value slot location
		if (!D1.equals(SymbolTableStack.returnValueSlot)){
			// allocate space for the defined variable and mark its register dirty
			RegEntry r_d1 = allocate(D1, type);
			r_d1.setDirty(true);
			
			newDest = r_d1.getRegExpr();
		}
		
		// reconstruct ralloced IRNode for this process 
		ralloc_subirList.add(new IRNode(opcode, newSource1, newDest));
		
	}
	
	// ralloc READI/ READF instructions: 0 used, 1 defined
	//		- allocate a register for the defined variable and mark it dirty
	//		- reconstruct the instruction using the ralloc registers
	private static void ralloc_Read(){
		// retrieve the defined variable
		String D1 = processingNode.getDest();
		
		// determine if the processing type is INT or FLOAT
		String opcode = processingNode.getOpcode();
		String type = "INT";
		if (opcode.equals("READF")){
			type = "FLOAT";
		}
		
		// allocate space for the defined variable and mark its register dirty 
		RegEntry r_d1 = allocate(D1, type);
		r_d1.setDirty(true);
		
		// reconstruct ralloced IRNode for this process 
		ralloc_subirList.add(new IRNode(opcode, r_d1.getRegExpr()));
	}
	
	// ralloc WRITEI/ WRITEF/ WRITES instructions: 1 used, 0 defined
	// 		- ensure the one used variables are in the register
	//		- free the one used varible if it is dead after this process
	//		- reconstruct the instruction using the ralloc registers
	private static void ralloc_Write(){
		// retrieve the used variable
		String U1 = processingNode.getDest();
		
		// determine if the processing type is INT or FLOAT or SRING
		String opcode = processingNode.getOpcode();
		String type = "INT";
		if (opcode.equals("WRITEF")){
			type = "FLOAT";
		} else if (opcode.equals("WRITES")){
			// string types do not need a ralloc because strings can be access from global vars only 
			// add the original IRNode to the ralloc sub irlist
			ralloc_subirList.add(new IRNode(opcode, U1));
			return;
		}
		
		// ensure the used variable is in the register
		RegEntry r_u1 = ensure(U1, type);
		
		// if the used variable is dead after this process, free the register storing this variable
		if (!processingNode.liveoutset.contains(r_u1.getVarName())){
			free(r_u1);
		}
		
		// reconstruct ralloced IRNode for this process 
		ralloc_subirList.add(new IRNode(opcode, r_u1.getRegExpr()));
	}
	
	// ralloc POP instruction: 0 used, 1 or 0 defined
	//		- allocate a register for the defined variable and mark it dirty
	//		- reconstruct the instruction using the ralloc registers
	private static void ralloc_Pop(){
		// retrieve the used variable
		String D1 = processingNode.getPushPopVal();
		
		// Evaluate its PPVAL
		if (D1.equals("")){
			// no PPVAL, 0 used, add the original IRNode to the ralloc worklist
			ralloc_subirList.add(new IRNode(IRType.POP, ""));
			return;
		}
		
		// allocate space for the defined variable and mark its register dirty 
		RegEntry r_d1 = allocate(D1, "INT"); // for spill, both STOREI and STOREF are translated to 'move' in tiny, so doesn't matter
		r_d1.setDirty(true);
		
		// reconstruct ralloced IRNode for this process 
		ralloc_subirList.add(new IRNode(IRType.POP, r_d1.getRegExpr()));
	}
	
	// ralloc PUSH instruction: 1 or 0 used, 0 defined
	// 		- ensure the one used variables are in the register
	//		- free the one used varible if it is dead after this process
	//		- reconstruct the instruction using the ralloc registers
	private static void ralloc_Push(){
		// retrieve the used variable
		String U1 = processingNode.getPushPopVal();
		
		// Evaluate its PPVAL
		if (U1.equals("")){
			// no PPVAL, 0 used, add the original IRNode to the ralloc worklist
			ralloc_subirList.add(new IRNode(IRType.PUSH, ""));
			return;
		}
		
		// ensure the used variable is in the register
		RegEntry r_u1 = ensure(U1, "INT"); // for spill, both STOREI and STOREF are translated to 'move' in tiny, so doesn't matter
		
		// if the used variable is dead after this process, free the register storing this variable
		if (!processingNode.liveoutset.contains(r_u1.getVarName())){
			free(r_u1);
		}
		
		// reconstruct ralloced IRNode for this process 
		ralloc_subirList.add(new IRNode(IRType.PUSH, r_u1.getRegExpr()));
	}
	
	// ralloc branch instructions: 2 used, 0 defined
	// 		- ensure the two used variables are in the register
	//		- free the two used varibles if they are dead after this process
	//		- reconstruct the instruction using the ralloc registers
	private static void ralloc_Branch(){
		// retrieve used variables
		String U1 = processingNode.getSource1();
		String U2 = processingNode.getSource2();
		
		// determine if the processing type is INT or FLOAT
		String opcode = processingNode.getOpcode();
		String type = "INT";
		if (opcode.charAt(opcode.length() - 1) == 'F'){
			type = "FLOAT";
		}
		
		// ensure the used variables are in the register
		RegEntry r_u1 = ensure(U1, type);
		RegEntry r_u2 = ensure(U2, type);
		
		// if used variables are dead after this process, free the register storing those variables
		if (!processingNode.liveoutset.contains(r_u1.getVarName())){
			free(r_u1);
		}
		if (!processingNode.liveoutset.contains(r_u2.getVarName())){
			free(r_u2);
		}
		
		// before branch taken, or not taken, ensure memory sees everything accessible in the register
		share_memory();
		
		// reconstruct ralloced IRNode for this process 
		int labelIndex = processingNode.getLabelIndex();
		ralloc_subirList.add(new IRNode(opcode, r_u1.getRegExpr(), r_u2.getRegExpr(), labelIndex));
		
		
	}

	//////////////// supporting functions for performing register allocation
	
	// While performing an operation, ensure that the input variables (GEN set) is in the register
	//		If in the register: 	use it directly
	//		If not in the register:	allocate a registe, load variable into register, and use the allocated register 
	private static RegEntry ensure(String var, String varType){
		// if var is already in register r, return r
		for (RegEntry r : regs){
			if (r.getVarName().equals(var)){
				
				r.decLRU_Level();
				
				return r;
			}
		}
		// otherwise, allocate a new register r for var (var is stored in r in the allocate function)
		RegEntry r = allocate(var, varType);
		
		// Generate Load from var into r 					LD var, $Rn
		if (varType.equals("INT")){
			ralloc_subirList.add(new IRNode("STOREI", var, r.getRegExpr()));
		} else {	// FLOAT
			ralloc_subirList.add(new IRNode("STOREF", var, r.getRegExpr()));
		}
		
		return r;
	}
	
	// clear the register entry
	//		if the register is dirty and it is still live, perform write back to memory before free
	private static void free(RegEntry r){
		// if r is dirty and varialbe is live after processing IRNode is processed
		// 		Generate store								ST $Rn, var
		if (r.isDirty() && processingNode.liveoutset.contains(r.getVarName())){
			if (r.getVarType().equals("INT")){
				ralloc_subirList.add(new IRNode("STOREI", r.getRegExpr(), r.getVarName()));
			} else{	// FLOAT
				ralloc_subirList.add(new IRNode("STOREF", r.getRegExpr(), r.getVarName()));
			}
		}
		// mark r as free
		r.clean();
	}
	
	// Allocate a space for storing the variable that is used in the processing node
	private static RegEntry allocate(String var, String varType){
		// if there is a free r, allocate the free space
		for (RegEntry r : regs){
			if (r.isEmpty()){
				// update what the register stores
				r.updateRegister(var, varType, false);
				// update least recently used level (higher, more recently used)
				r.decLRU_Level();
				return r;
			}
		}
		
		// otherwise, choose r to free and allocate the free space (a spill is required)
		//			spill register storing the least recently used (LRU) variable
		//			spill the numerically lowest register 
		RegEntry r = find_LRU_reg();
		free(r);
		
		// update what the register stores
		r.updateRegister(var, varType, false);
		// update least recently used level (higher, more recently used)
		r.decLRU_Level();
		return r;
	}
	
	
	// Find the least recently used register entry
	// If tied, get the register with lower numerical index
	private static RegEntry find_LRU_reg(){
		
		RegEntry lru_reg = regs[regs.length-1];		
		for (int i = regs.length-2; i >= 0; i--){
			if (regs[i].getLRU_Level() >= lru_reg.getLRU_Level() && !TYTSMisc.isTempReg(regs[i].getVarName())){
				lru_reg = regs[i];
			}
		}
		
		if (TYTSMisc.isTempReg(lru_reg.getVarName())){
			for (int i = regs.length-3; i >= 0; i--){
				if (regs[i].getLRU_Level() >= lru_reg.getLRU_Level() && !TYTSMisc.isTempReg(regs[i].getVarName())){
					lru_reg = regs[i];
				}
			}
		}
		
		if (TYTSMisc.isTempReg(lru_reg.getVarName())){
			lru_reg = regs[0];
		}
		
		return lru_reg;
	}
	
	
	// Bottom-up register allocation works at the basic-block level: 
	// any register allocation decisions you make apply for the current basic block only. 
	// This means that when you get to the end of a basic block, you must reset your register allocation. 
	// Any register that 
	// 		(a) hold local/global variables and 
	//		(b) is dirty should be written back to the stack/global variable.
	private static void flush_registers(){
		for (RegEntry r : regs){
			// write back dirty register storing local variables, function parameters, and global variables  
			if (r.isDirty()){
				if (TYTSMisc.isLocalVarSlot(r.getVarName()) || TYTSMisc.isFuncParaSlot(r.getVarName()) || SymbolTableStack.isGlobalVar(r.getVarName())){
					if (r.getVarType().equals("INT")){
						ralloc_subirList.add(new IRNode("STOREI", r.getRegExpr(), r.getVarName()));
					} else {
						ralloc_subirList.add(new IRNode("STOREF", r.getRegExpr(), r.getVarName()));
					}
				}
			}
			
			// free registers
			r.clean();
		}
	}
	
	private static void share_memory(){
		for (RegEntry r : regs){
			// write back dirty register storing local variables, function parameters, and global variables  
			if (r.isDirty()){
				if (TYTSMisc.isMemoryAccessExpr(r.getVarName())){
					if (r.getVarType().equals("INT")){
						ralloc_subirList.add(new IRNode("STOREI", r.getRegExpr(), r.getVarName()));
					} else {
						ralloc_subirList.add(new IRNode("STOREF", r.getRegExpr(), r.getVarName()));
					}
				}
			}
		}
	}
	
	// Note also that because a CALL instruction jumps into another method, 
	// any global variables that are in registers when the CALL (JSR) is performed 
	// should be freed immediately prior to the CALL instruction, ensuring that 
	// the correct value for the global is in memory.
	private static void share_global(){
		for (RegEntry r : regs){
			// write back dirty register storing local variables, function parameters, and global variables  
			if (r.isDirty()){
				if (SymbolTableStack.isGlobalVar(r.getVarName())){
					if (r.getVarType().equals("INT")){
						ralloc_subirList.add(new IRNode("STOREI", r.getRegExpr(), r.getVarName()));
					} else {
						ralloc_subirList.add(new IRNode("STOREF", r.getRegExpr(), r.getVarName()));
					}
				}
			}
		}
	}
	
	
	//////////////// Functions that translates Ralloc IRNodes to tiny code
	public static void translateRegAllocIRListToTiny(){
		
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
		
		for (LinkedList<IRNode> sublist : ralloc_irList){
			for (IRNode irnode : sublist){
				String tinycode = toRallocTinyCode(irnode);
				System.out.print(tinycode);
			}
		}
	}
	
	// convert $Rn to rn
	// if the input is not $Rn, return the same value
	private static String toRallocTinyReg(String entry){
		if (entry.matches("\\$R[0-3]")){
			return "r" + entry.substring(2);
		}
		return entry;
	}
	
	// Convert an IRNode to tiny code
	private static String toRallocTinyCode(IRNode irnode){
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
				
			tiny += toRallocTinyReg(irnode.getDest());
			tiny += "\n";
		
		// Two entry - Store
		} else if (irnode.getType() == IRType.TWO_ENTRY){
			
			if (irnode.getSource1().equals(irnode.getDest())){
				tiny += ";";
			}
			
			tiny += "move ";
			tiny += toRallocTinyReg(irnode.getSource1()) + " ";
			tiny += toRallocTinyReg(irnode.getDest());
			tiny += "\n";
			
		// Three entry - add/ sub/ mult/ div
		} else if (irnode.getType() == IRType.THREE_ENTRY){
			// first tiny code line
			tiny += "move ";
			tiny += toRallocTinyReg(irnode.getSource1()) + " ";
			tiny += toRallocTinyReg(irnode.getDest());
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
			tiny += toRallocTinyReg(irnode.getSource2()) + " ";
			tiny += toRallocTinyReg(irnode.getDest());
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
			
			tiny += toRallocTinyReg(irnode.getSource1()) + " ";
			tiny += toRallocTinyReg(irnode.getSource2());
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
			tiny += toRallocTinyReg(irnode.getPushPopVal());
			tiny += "\n";		
		// POP
		} else if (irnode.getType() == IRType.POP){
			tiny += "pop ";
			tiny += toRallocTinyReg(irnode.getPushPopVal());
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
	
	
	//////////////// Debugging Print Functions
	public static String getRegTable(){
		String table = "\n;                    ";
		for (int i = 0; i < REGSIZE - 1; i++){
			table += regs[i]; 
			table += " || ";
		}
		table += regs[REGSIZE-1];
		return table;
	}
	
	public static void printRegisters(){
		System.out.print(";    ");
		for (int i = 0; i < REGSIZE - 1; i++){
			System.out.print(regs[i] + " || ");
		}
		System.out.println(regs[REGSIZE-1]);
	}
	
	public static void printRallocIRNode(){
		System.out.println();
		System.out.println("; ===============  IR List with Ralloc ====================");
		for (LinkedList<IRNode> sublist : ralloc_irList){
			System.out.println();
			for (IRNode node : sublist){
				System.out.println(";" + node + " " + node.getRallocMessage());
			}
		}
	}
	
}