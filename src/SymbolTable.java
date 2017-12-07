

import java.util.*;

public class SymbolTable{
	private String scope;
	private int scopeType;
	private LinkedHashMap<String, Symbol> symbolMap;
	private String returnType;
	private int subScopeLocalVarSize;
	
	private static int next_available_para_slot = 7;
	private static int next_available_local_slot = -1;
	
	
	// CALL STACK FORMAT:
	//					slot value
	// Parameter n			n+6
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
	
	
    // function block scope
	public SymbolTable(String scope, String returnType, int scopeType){
		this.scope = scope;
		this.symbolMap = new LinkedHashMap<String, Symbol>();
		this.returnType = returnType;
		this.scopeType = scopeType;
		this.subScopeLocalVarSize = 0;
		
		if (scopeType == IRScope.FUNC_TYPE || scopeType == IRScope.GLOBAL_TYPE){
			this.next_available_local_slot = -1;
			this.next_available_para_slot = 7;
		}
	}
	
	// a for block scope or if/else block scope
	public SymbolTable(String scope, int scopeType){
		this.scope = scope;
		this.symbolMap = new LinkedHashMap<String, Symbol>();
		this.returnType = null;
		this.scopeType = scopeType;
		
		if (scopeType == IRScope.FUNC_TYPE || scopeType == IRScope.GLOBAL_TYPE){
			this.next_available_local_slot = -1;
			this.next_available_para_slot = 7;
		}
	}

	public String getScope(){
		return this.scope;
	}
	
	public int getScopeType(){
		return this.scopeType;
	}

	public LinkedHashMap<String, Symbol> getSymbolMap(){
		return this.symbolMap;
	}
	
	public String getReturnType(){
		return this.returnType;
	}
	
	// get the size of the symbolMap
	public int getSymbolMapSize(){
		return this.symbolMap.size();
	}
	
	// get the number of parameters in this symbol table
	public int getNumberOfParameters(){
		int count = 0;
		for (Map.Entry<String, Symbol> entry: this.symbolMap.entrySet()){
			Symbol sym = entry.getValue();
			if (sym.isParameter()){
				count++;
			}
		}
		return count;
	}
	
	// get the number of local variables in this symbol table
	public int getNumberOfLocalVar(){
		int count = 0;
		for (Map.Entry<String, Symbol> entry: this.symbolMap.entrySet()){
			Symbol sym = entry.getValue();
			if (!sym.isParameter()){
				count++;
			}
		}
		return count;
	}
	
	// add single symbol
	public void addSymbol(Symbol symbol, boolean isParameter){
		try{
			if (this.symbolMap.containsKey(symbol.getName())){
				throw new SymbolAddInvalidException("DECLARATION ERROR " + symbol.getName());
			}else{
				this.symbolMap.put(symbol.getName(), symbol);
			}
		}catch (SymbolAddInvalidException ex){
			System.out.println(SymbolAddInvalidException.errMessage);
			System.exit(1);
		}
	}
	
	// add list of symbol
	public void addSymbol(String nameList, String type, boolean isParameter){
		CharSequence cs = ",";
		if (nameList.contains(cs)){
			String[] names = nameList.split(",");
			if (isParameter){
				for (String name : names){
					this.addSymbol(new Symbol(name.trim(), type, isParameter, this.next_available_para_slot++), isParameter);
				}
			} else {
				for (String name : names){
					this.addSymbol(new Symbol(name.trim(), type, isParameter, this.next_available_local_slot--), isParameter);
				}
			}
		}else{
			if (isParameter){
				this.addSymbol(new Symbol(nameList.trim(), type, isParameter, this.next_available_para_slot++), isParameter);
			} else {
				this.addSymbol(new Symbol(nameList.trim(), type, isParameter, this.next_available_local_slot--), isParameter);
			}
		}
	}
	
	// add symbol for string type
	public void addSymbol(String name, String type, String value, boolean isParameter){
		if (isParameter){
			this.addSymbol(new Symbol(name, type, value, isParameter, this.next_available_para_slot++), isParameter);
		} else {
			this.addSymbol(new Symbol(name, type, value, isParameter, this.next_available_local_slot--), isParameter);
		}
	}
	
	public void incrSubScopeLocalVarSize(){
		this.subScopeLocalVarSize++;
	}
	
	public int getSubScopeLocalVarSize(){
		return this.subScopeLocalVarSize;
	}
	
	@Override
	public String toString(){
		String str;
		
		
		str = ";Symbol table " + this.scope + "\n";
		str += "; return type: " + this.returnType + "\n";
		str += "; subscope local var size: " + this.subScopeLocalVarSize + "\n";
		
		if (this.scopeType == IRScope.FUNC_TYPE){
			str += ";scope type: FUNC_TYPE \n";
		} else if (this.scopeType == IRScope.IF_TYPE) {
			str += ";scope type: IF_TYPE \n";
		} else if (this.scopeType == IRScope.ELSE_TYPE) {
			str += ";scope type: ELSE_TYPE \n";
		} else if (this.scopeType == IRScope.FOR_TYPE) {
			str += ";scope type: FOR_TYPE \n";
		} else if (this.scopeType == IRScope.GLOBAL_TYPE) {
			str += ";scope type: GLOBAL_TYPE \n";
		}

		for (Map.Entry<String, Symbol> entry : this.symbolMap.entrySet()){
			Symbol sym = entry.getValue();
			str += ";name " + sym.getName() + " type " + sym.getType();
			if (sym.getType() == "STRING"){
				str += " value " + sym.getValue();
			}
			if (sym.isParameter()){
				str += " parameter";
			} else {
				str += " local";
			}
			str += " slot location: " + sym.getSlotLocation();
			str += "\n";
		}
		
		str += "\n";
		
		return str;
	}

}