
import java.util.*;

public class TYTSMisc {
	
	private static Stack<String> operator = new Stack<String>();
	
	// convert infix expression to postfix expression
	public static LinkedList<String> toPostfix(LinkedList<String> infix){
		
		operator.clear();
		
		LinkedList<String> postfix = new LinkedList<String>();
		String popped;
		
		for (String str : infix){
			if (!isOperator(str)){
				postfix.add(str);
			} else if (str.equals(")")){
				while (!(popped = operator.pop()).equals("(")){
					postfix.add(popped);
				}
			} else {
				while (!operator.isEmpty() && !str.equals("(") && getPrecedence(operator.peek()) >= getPrecedence(str)){
					postfix.add(operator.pop());
				}
				operator.push(str);
			}
		}
		while (!operator.isEmpty()){
			postfix.add(operator.pop());
		}
		
		
		return postfix;
	}
	
	// check if a token is an operator
	public static boolean isOperator(String str){
		return getPrecedence(str) > 0;
	}
	
	// returns the precedence of the operator
	private static int getPrecedence(String str){
		if (str.equals("(") || str.equals(")")) {
			return 1;
		} else if (str.equals("-") || str.equals("+")) {
			return 2;
		} else if (str.equals("*") || str.equals("/")) {
			return 3;
		}
		
		return 0;
	}
	
	// reverse a linked list
	private static LinkedList<String> reverse(LinkedList<String> list)
	{
		LinkedList<String> reverseList = new LinkedList<String>();
		
		for (int i = list.size() - 1; i >= 0; i--){
			reverseList.add(list.get(i));
		}
		
		return reverseList;
	}
	
	// check if the expression is a float literal
	public static boolean isFLOATLITERAL(String expression)
	{
		return expression.matches("[0-9]*\\.[0-9]+");
	}
	
	// check if the expression is an int literal
	public static boolean isINTLITERAL(String expression)
	{
		return expression.matches("[0-9]+");
	}
	
	// check if the expression is a string literal
	public static boolean isSTRINGLITERAL(String expression)
	{
		return expression.matches("\".*?\"");
	}
	
	// check if the expression is an identifier 
	public static boolean isIDENTIFIER(String expression)
	{
		return expression.matches("[A-Za-z][A-Za-z0-9]*");
	}
	
	// check if the expression begins with a $
	public static boolean isTempOrRef(String expression)
	{
		return expression.matches("\\$.*");
	}
	
	// check if the expression is a temp register ($Tn)
	public static boolean isTempReg(String expression)
	{
		return expression.matches("\\$T[0-9]+");
	}
	
	public static boolean isLiteral(String expression){
		return isINTLITERAL(expression) || isFLOATLITERAL(expression);
	}
	
	public static boolean isLocalVarSlot(String expression){
		return expression.matches("\\$[0-9]+");
	}
	
	public static boolean isFuncParaSlot(String expression){
		return expression.matches("\\$\\-[0-9]+");
	}
	
	public static boolean isMemoryAccessExpr(String expression){
		if (SymbolTableStack.isGlobalVar(expression)){
			return true;
		}
		if (isFuncParaSlot(expression)){
			return true;
		}
		if (isLocalVarSlot(expression)){
			return true;
		}
		return false;
	}
	
	// convert the operator token to 3 address code string expression
	public static String getOpcodeName(String opexpr, String type){
		// opexpr can be "+", "-", "*", "/", or ":="
		// type can be "INT", "FLOAT", "STRING"
		
		String opcodeName = "";
		
		if (opexpr.equals("+")){
			opcodeName += "ADD";
		} else if (opexpr.equals("-")){
			opcodeName += "SUB";
		} else if (opexpr.equals("*")){
			opcodeName += "MULT";
		} else if (opexpr.equals("/")){
			opcodeName += "DIV";
		} else if (opexpr.equals(":=")){
			opcodeName += "STORE";
		}
		
		if (type.equals("INT")){
			opcodeName += "I";
		} else if (type.equals("FLOAT")){
			opcodeName += "F";
		} else if (type.equals("STRING")){
			opcodeName += "S";
		}
		
		return opcodeName;
	}
	
	// given a comparison operator, find its complement expression
	public static String getBranchComplementOpcode(String token, String branchType){
		String opcode = "";
		
		if (token.equals("=")){
			opcode += "NE";
		} else if (token.equals("!=")){
			opcode += "EQ";
		} else if (token.equals(">=")){
			opcode += "LT";
		} else if (token.equals("<=")){
			opcode += "GT";
		} else if (token.equals(">")){
			opcode += "LE";
		} else if (token.equals("<")){
			opcode += "GE";
		}
		
		if (branchType.equals("INT")){
			opcode += "I";
		} else if (branchType.equals("FLOAT")){
			opcode += "F";
		}
		
		return opcode;
	}
	
	public static String getBranchSwitchOpOpcode(String opcode){
		String switchOpcode = "";
		
		if (opcode.startsWith("EQ")){
			switchOpcode += "EQ";
		} else if (opcode.startsWith("NE")){
			switchOpcode += "NE";
		} else if (opcode.startsWith("GT")){
			switchOpcode += "LT";
		} else if (opcode.startsWith("GE")){
			switchOpcode += "LE";
		} else if (opcode.startsWith("LT")){
			switchOpcode += "GT";
		} else if (opcode.startsWith("LE")){
			switchOpcode += "GE";
		}
		
		if (opcode.endsWith("I")){
			switchOpcode += "I";
		} else {
			switchOpcode += "F";
		}
		
		return switchOpcode;
	}
	
	// given a list of token (expr), find its return type
	public static String findReturnType(LinkedList<String> tokenList){
		String returnType = "INT";
		// find the return type from expression tokens
		for (String token: tokenList){
			if (TYTSMisc.isFLOATLITERAL(token)){
				returnType = "FLOAT";
				break;
			} else if (TYTSMisc.isINTLITERAL(token)){
				returnType = "INT";
				break;
			} else if (TYTSMisc.isIDENTIFIER(token)){
				returnType = IRAction.getDeclareType(token);
				break;
			} else if (TYTSMisc.isTempReg(token)){
				if (IRAction.tempRegTypeMap.containsKey(token)){
					return IRAction.tempRegTypeMap.get(token);
				}
			}
		}
		
		return returnType;
	}

}

