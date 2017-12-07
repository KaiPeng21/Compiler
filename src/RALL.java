
import java.util.*;

public class RALL{
	// size of registers
	public static final int REGSIZE = 4;
	
	// The 4 Registers
	private static RegEntry[] regs = {
		new RegEntry(),
		new RegEntry(),
		new RegEntry(),
		new RegEntry()
	};
	
	// The current IRNode that is doing register allocation
	private static IRNode processingNode;
	
	private static IRNode nextProcessingNode;
	
	private static String finalTinyCode = "";
	
	// Perform register allocation On a CFG constructed IR List (liveness is already computed)
	// and generate tiny code output
	public static void ralloc(LinkedList<LinkedList<IRNode>> cfg_irList){
		
		// print the global variable declaration, call the main subroutine, and halt
		tinyDeclarationSetup();
		
		// reset register prior ralloc
		for (RegEntry r: regs){
			r.clean();
		}
		
		// processing register allocation and print out tiny code
		for (LinkedList<IRNode> sublist : cfg_irList){
			for (int j = 0; j < sublist.size(); j++){  // IRNode node : sublist
				// set up the current processing node and the next processing node
				processingNode = sublist.get(j);
				try{
					nextProcessingNode = sublist.get(j+1);
				} catch (Exception ex){
					nextProcessingNode = null;
				}
				
				
				// display the IRNode and liveness information prior register allocation and tiny code conversion 
				System.out.println(processingNode + " liveout: " +  processingNode.getLiveOutString());
				
				// translate simple IR Node to tiny code
				
				if (processingNode.getType() == IRType.FUNCLABEL){
					printTiny("label " + processingNode.getFuncNameId());
					
				} else if (processingNode.getType() == IRType.LINK){
					
					// set link val = old link val + # tempories
					// if tempory $Tn needs memory write back, store it to the address $(-(#local + n))
					int newLinkVal = processingNode.getLinkNum() + IRAction.numberOfTempories();
					printTiny("link " + newLinkVal);
					
				} else if (processingNode.getType() == IRType.UNLINK){
					printTiny("unlnk");
					
				} else if (processingNode.getType() == IRType.RET){
					
					// prior return, write back all global variables to shared memory
					// so the caller can read the most recently updated value of a global variable
					globalVarWriteBack();
					
					printTiny("ret");
					
				} else if (processingNode.getType() == IRType.LABEL){
					
					// end of the basic block: spill registers
					spillForNewBasicBlock();
					
					printTiny("label label" + processingNode.getLabelIndex());
					
				} else if (processingNode.getType() == IRType.JUMP){
					
					// end of the basic block: spill registers
					spillForNewBasicBlock();
					
					printTiny("jmp label" + processingNode.getLabelIndex());
					
				} else if (processingNode.getType() == IRType.PUSHREG){
					for (int i = 0; i < REGSIZE; i++){
						printTiny("push r" + i);
					}
			
				} else if (processingNode.getType() == IRType.POPREG){
					for (int i = REGSIZE-1; i >= 0; i--){
						printTiny("pop r" + i);
					}
					
				} else if (processingNode.getType() == IRType.JSR){
					
					// write back global variables to shared memory
					// so the callee can take the most recently updated value
					// registers should not store any global varialbe after the function call
					globalVarWriteBack();
					
					printTiny("jsr " + processingNode.getJsrFuncName());
				
					
				// translate push and pop cases to tiny code
				} else if (processingNode.getType() == IRType.PUSH){
					
					// simple push with no $PPVAL, translate directly
					if (processingNode.getPushPopVal().equals("")){
						printTiny("push");
					} else {
						
						// ensure register for push
						RegEntry r = ensure(processingNode.getPushPopVal());
						
						// generate tiny code for push
						printTiny("push " + r.getRegExpr());
						
						// if pushed value is not live anymore, free the register
						if (!processingNode.liveoutset.contains(r.getVarName())){
							free(r);
						}
					}
					
					
				} else if (processingNode.getType() == IRType.POP){
					
					// simple pop with no $PPVAL, translate directly
					if (processingNode.getPushPopVal().equals("")){
						printTiny("pop");
					} else {
						
						// ensure register for pop
						RegEntry r = ensure(processingNode.getPushPopVal());
						
						// generate tiny code for push
						printTiny("pop " + r.getRegExpr());
						
						// mark the register dirty
						r.setDirty(true);
						
						// if pushed value is not live anymore, free the register
						if (!processingNode.liveoutset.contains(r.getVarName())){
							free(r);
						}
						
					}
					
				// rallocing and translating IRNode cases that uses ALU operations
				} else if (processingNode.getType() == IRType.THREE_ENTRY){
					
					// 3AD IRNode
					//  OP $rs1 $rs2 $rd	means	$rd <= $rs1 OP $rs2 
					//
					// tiny code rule:
					//	op $rs $rd			means 	$rd <= $rd op $rs
					
					// ensure the two sources for the ALU operation
					//	rs1 and rs2 represent the two sources in the 3AD IRNode
					RegEntry rs1 = ensure(processingNode.getSource1());
					RegEntry rs2 = ensure(processingNode.getSource2());
					
					// transfer the ownser of $rs1 to $rd
					//	write back $rs1 if it is dirty
					String aluMessage = "; switching owner of register " + rs1.getVarName();
					aluMessage += " to " + processingNode.getDest() + " ";
					aluMessage += getRegTable();
					System.out.println(aluMessage);
					if (rs1.isDirty()){
						String spillingVar = rs1.getVarName();
						System.out.println("; spilling varialbe " + spillingVar);
						if (TYTSMisc.isTempReg(spillingVar)){
							spillingVar = tempReg_to_mem(spillingVar);
						}
						printTiny("move " + rs1.getRegExpr() + " " + spillingVar);
					}
					rs1.updateRegister(processingNode.getDest(), false);
					
					// the equivalent source and destination register in the tiny code expression
					RegEntry rd = rs1;
					RegEntry rs = rs2;
					
					// generate tiny code for the ALU operation
					String opcode = processingNode.getOpcode();
					String tinyop = "";
					if (opcode.startsWith("ADD")){
						tinyop += "add";
					} else if (opcode.startsWith("SUB")){
						tinyop += "sub";
					} else if (opcode.startsWith("MUL")){
						tinyop += "mul";
					} else if (opcode.startsWith("DIV")){
						tinyop += "div";
					}
					if (opcode.endsWith("I")){
						tinyop += "i";
					} else if (opcode.endsWith("F")){
						tinyop += "r";
					}
					printTiny(tinyop + " " + rs.getRegExpr() + " " + rd.getRegExpr());
					// mark tiny ALU operation destination dirty
					rd.setDirty(true);
					
					// free register if rs and rd or no longer live
					if (!processingNode.liveoutset.contains(rs.getVarName())){
						free(rs);
					}
					if (!processingNode.liveoutset.contains(rd.getVarName())){
						free(rd);
					}
					
				
				// STORE (move) operations
				} else if (processingNode.getType() == IRType.TWO_ENTRY){
					
					// storing a literal to [mem/tempories]
					if (TYTSMisc.isLiteral(processingNode.getSource1())){
						// ensure the destination
						RegEntry rd = ensure(processingNode.getDest());
						
						// generate the move tiny code
						// move [literal] [rn]
						printTiny("move " + processingNode.getSource1() + " "+ rd.getRegExpr());
						
						// mark the destination dirty
						rd.setDirty(true);
						
						// if destination is no longer live, free it
						if (!processingNode.liveoutset.contains(rd.getVarName())){
							free(rd);
						}
						
					} else {
						// storing to return value slot
						if (processingNode.getDest().equals(SymbolTableStack.returnValueSlot)){
							// ensure the source
							RegEntry rs = ensure(processingNode.getSource1());
							
							// generate the move tiny code
							// move [] $6
							printTiny("move " + rs.getRegExpr() + " " + processingNode.getDest());
							
							// if source is no longer live, free it
							if (!processingNode.liveoutset.contains(rs.getVarName())){
								free(rs);
							}
							
						// other moves 
						} else {
							// ensure both source and destination
							RegEntry rs = ensure(processingNode.getSource1());
							RegEntry rd = ensure(processingNode.getDest());
							
							// generate the move tiny code
							printTiny("move " + rs.getRegExpr() + " " + rd.getRegExpr());
							
							// set the destination dirty
							rd.setDirty(true);
							
							// free rs and rd if they are no longer live
							if (!processingNode.liveoutset.contains(rs.getVarName())){
								free(rs);
							}
							if (!processingNode.liveoutset.contains(rd.getVarName())){
								free(rd);
							}
						}
					}
					
				// READ operation	
				} else if (processingNode.getType() == IRType.ONE_ENTRY
						&& processingNode.getOpcode().startsWith("READ")){
					
					// ensure the read destination
					RegEntry rd = ensure(processingNode.getDest());
					
					// generate read tiny code
					String opcode = processingNode.getOpcode(); 
					if (opcode.endsWith("I")){
						printTiny("sys readi " + rd.getRegExpr());
					} else if (opcode.endsWith("F")){
						printTiny("sys readr " + rd.getRegExpr());
					}
					
					// mark the destination dirty
					rd.setDirty(true);
					
					// free read destination if it is no longer live
					if (!processingNode.liveoutset.contains(rd.getVarName())){
						free(rd);
					}
					
				
				// WRITE operation
				} else if (processingNode.getType() == IRType.ONE_ENTRY
						&& processingNode.getOpcode().startsWith("WRITE")){
					
					String opcode = processingNode.getOpcode(); 
					// string var is always global and won't change the value
					// generate tiny code immediately
					if (opcode.endsWith("S")){
						printTiny("sys writes " + processingNode.getDest());
						
					} else {
						// ensure the write variable
						RegEntry rd = ensure(processingNode.getDest());
					
						// generate write tiny code
						if (opcode.endsWith("I")){
							printTiny("sys writei " + rd.getRegExpr());
						} else if (opcode.endsWith("F")){
							printTiny("sys writer " + rd.getRegExpr());
						}
					
						// free write variable if it is no longer live
						if (!processingNode.liveoutset.contains(rd.getVarName())){
							free(rd);
						}
					}
					
				
				// Branch operation
				} else if (processingNode.getType() == IRType.BRANCH){
					
					// ensure the two sources
					RegEntry rs1 = ensure(processingNode.getSource1());
					RegEntry rs2 = ensure(processingNode.getSource2());
					
					// generate the comparison tiny code
					String opcode = processingNode.getOpcode();
					if (opcode.endsWith("I")){
						printTiny("cmpi " + rs1.getRegExpr() + " " + rs2.getRegExpr());
					} else if (opcode.endsWith("F")){
						printTiny("cmpr " + rs1.getRegExpr() + " " + rs2.getRegExpr());
					}
					
					// end of the basic block: spill registers
					spillForNewBasicBlock();
					
					// generate the conditional branch tiny code
					if (opcode.startsWith("EQ")){
						printTiny("jeq label" + processingNode.getLabelIndex());
					} else if (opcode.startsWith("NE")){
						printTiny("jne label" + processingNode.getLabelIndex());
					} else if (opcode.startsWith("GT")){
						printTiny("jgt label" + processingNode.getLabelIndex());
					} else if (opcode.startsWith("GE")){
						printTiny("jge label" + processingNode.getLabelIndex());
					} else if (opcode.startsWith("LT")){
						printTiny("jlt label" + processingNode.getLabelIndex());
					} else if (opcode.startsWith("LE")){
						printTiny("jle label" + processingNode.getLabelIndex());
					}
					
					
					// free the sources if the two sources are no longer needed
					if (!processingNode.liveoutset.contains(rs1.getVarName())){
						free(rs1);
					}
					if (!processingNode.liveoutset.contains(rs2.getVarName())){
						free(rs2);
					}
					
				}
				
				
			} // sublist end (end of a function scope)
		} // overall end
	}
	
	private static void tinyDeclarationSetup(){
		
		// get the global symbol table and print global variable declaration tiny code
		SymbolTable symbolTable = SymbolTableStack.symbolTableStack.get(0);
		for (Map.Entry<String, Symbol> entry : symbolTable.getSymbolMap().entrySet()){
			Symbol symbol = entry.getValue();
			if (symbol.getValue() == null){
				printTiny("var " + symbol.getName());
			} else {
				printTiny("str " + symbol.getName() + " " + symbol.getValue());
			}
		}
		
		// treat string variables declared in other scope as global variables
		if (SymbolTableStack.symbolTableStack.size() > 1){
			for (int i = 1; i < SymbolTableStack.symbolTableStack.size(); i++){
				symbolTable = SymbolTableStack.symbolTableStack.get(i);
				for (Map.Entry<String, Symbol> entry : symbolTable.getSymbolMap().entrySet()){
					Symbol symbol = entry.getValue();
					if (symbol.getValue() != null){
						printTiny("str " + symbol.getName() + " " + symbol.getValue());
					}
				}
			}
		}
		
		// call the main subroutine
		printTiny("push");
		printTiny("push");
		printTiny("push");
		printTiny("push");
		printTiny("push");
		printTiny("jsr main");
		printTiny("sys halt");
	}
	
	private static RegEntry ensure(String opr){
		String ensuringMessage = "; ensure(): " + opr;
		
		// if opr is in the register, use it
		for (int i = REGSIZE - 1; i >= 0; i--){
			if (opr.equals(regs[i].getVarName())){
				ensuringMessage += " has register " + regs[i].getRegExpr();
				System.out.println(ensuringMessage);
				
				return regs[i];
			}
		}
		// if opr is not in the register, allocate a register for opr
		RegEntry allocateReg = allocate(opr);
		
		ensuringMessage += " gets register " + allocateReg.getRegExpr();
		ensuringMessage += " " + getRegTable();
		System.out.println(ensuringMessage);
		
		// generate load from opr (memory) to register
		if (TYTSMisc.isTempReg(opr)){
			opr = tempReg_to_mem(opr);	
		}
		System.out.println("; loading " + opr + " to register " + allocateReg.getRegExpr());
		printTiny("move " + opr + " " + allocateReg.getRegExpr());
		
		return allocateReg;
	}
	
	private static void free(RegEntry r){
		String freeingMessage = "; freeing unused variable " + r.getRegExpr();
		System.out.println(freeingMessage);
		
		// if the entry is dirty, write it back to memory
		if (r.isDirty()){
			String spillingVar = r.getVarName();
			System.out.println("; spilling varialbe " + spillingVar);
			if (TYTSMisc.isTempReg(spillingVar)){
				spillingVar = tempReg_to_mem(spillingVar);
			}
			printTiny("move " + r.getRegExpr() + " " + spillingVar);
			
		}
		
		// free the register
		r.clean();
	}
	
	private static RegEntry allocate(String opr){
		String allocatingMessage = "";
		
		// if there is an empty register entry, allocate the space
		for (int i = REGSIZE-1; i >= 0; i--){
			if (regs[i].isEmpty()){
				regs[i].updateRegister(opr, false);
				return regs[i];
			}
		}
		// otherwise, choose a register to free, then allocate the register
		RegEntry chosenReg = chooseRegToFreeForAllocate();
		free(chosenReg);
		chosenReg.updateRegister(opr, false);
		
		allocatingMessage += "; allocate() has to spill " + chosenReg.getVarName();
		System.out.println(allocatingMessage);
		
		return chosenReg;
	}
	
	// choose a register to free in order to allocate register space for another variable
	//		Priority: Free and allocate the entry the current instruction is not using
	private static RegEntry chooseRegToFreeForAllocate(){
		for (int i = 0; i < REGSIZE; i++){
			String regStoreVar = regs[i].getVarName();
			if (processingNode.getSource1() != null && processingNode.getSource1().equals(regStoreVar)){
				continue;
			}
			if (processingNode.getSource2() != null && processingNode.getSource2().equals(regStoreVar)){
				continue;
			}
			if (processingNode.getDest() != null && processingNode.getDest().equals(regStoreVar)){
				continue;
			}
			if (processingNode.getPushPopVal() != null && processingNode.getPushPopVal().equals(regStoreVar)){
				continue;
			}
			return regs[i];
		}
		return regs[REGSIZE-1];
	}
	
	private static void spillForNewBasicBlock(){
		System.out.println("; spilling registers at the end of Basic Block");
		for (int i = REGSIZE-1; i >= 0; i--){
			if (!regs[i].isEmpty()){
				String spillingVar = regs[i].getVarName();
				System.out.println("; spilling varialbe " + spillingVar);
				if (TYTSMisc.isTempReg(spillingVar)){
					spillingVar = tempReg_to_mem(spillingVar);
				}
				printTiny("move " + regs[i].getRegExpr() + " " + spillingVar);
				
				regs[i].clean();
			}
		}
	}
	
	private static void globalVarWriteBack(){
		System.out.println("; writing back global variables prior a function call");
		for (int i = REGSIZE-1; i >= 0; i--){
			if (!regs[i].isEmpty() && SymbolTableStack.isGlobalVar(regs[i].getVarName())){
				String spillingVar = regs[i].getVarName();
				System.out.println("; spilling varialbe " + spillingVar);
				printTiny("move " + regs[i].getRegExpr() + " " + spillingVar);
				
				regs[i].clean();
			}
		}
	}
	
	// given $Tn, find its memory slot expression
	// 	convert $Tn to $-(#local+n)
	private static String tempReg_to_mem(String tempReg){
		try{
			String processingFuncName = processingNode.getBelongToFunctionScope();
			int numOfLocalVar = SymbolTableStack.getFullLocalVarSize(processingFuncName);
			int t = Integer.parseInt(tempReg.substring(2));
			
			int slotval = numOfLocalVar + t;
			return "$-" + slotval;
		} catch(Exception ex){
		}
		return null;
	}
	
	public static String getRegTable(){
		String regTable = "{ " + regs[0].toString();
		for (int i = 1; i < REGSIZE; i++){
			 regTable += " || " + regs[i];
		}
		regTable += " }";
		return regTable;
	}
	
	
	private static void printTiny(String tinycode){
		System.out.println(";" + tinycode + " \t; " + getRegTable());
		finalTinyCode += tinycode + "\n";
	}
	
	public static void printFinalTinyCode(){
		System.out.println();
		System.out.print(finalTinyCode);
	}
	
}