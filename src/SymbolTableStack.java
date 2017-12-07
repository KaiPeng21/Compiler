import java.util.*;


public class SymbolTableStack{
	public static Stack<SymbolTable> symbolTableStack = new Stack<SymbolTable>();
	private static int blockNumber = 1;
	private static String outputMessage = "";
	private static String constructingFunctionScopeName = ""; // track the functionScopeName while constructing the syntax tree
	
	public static final String returnValueSlot = "$6";
	
	public static void createScope(String scopeName, String returnType, int scopeType){
		symbolTableStack.push(new SymbolTable(scopeName, returnType, scopeType));
		constructingFunctionScopeName = scopeName;
	}
	
	public static void createScope(char scopeID, int scopeType){
		if (scopeID == 'g'){
			symbolTableStack.push(new SymbolTable("GLOBAL", scopeType));
		} else if (scopeID == 'b'){
			symbolTableStack.push(new SymbolTable("BLOCK " + blockNumber++, scopeType));
		}
	}
	
	
	public static SymbolTable popScope(){
		SymbolTable symbolTable = null;
		
		try{
			symbolTable = symbolTableStack.pop();
		}catch (EmptyStackException ex){
			System.out.println(ex.toString());
			System.exit(1);
		}finally{
			return symbolTable;
		}
	}
	
	// for single symbol
	public static void addSymbolToPeak(Symbol symbol, boolean isParameter){
		try{
			SymbolTable symbolTable = symbolTableStack.pop();
			symbolTable.addSymbol(symbol, isParameter);
			symbolTableStack.push(symbolTable);
		}catch(EmptyStackException ex){
			System.out.println(ex.toString());
			System.exit(1);
		}
		
	}
	
	// for list of symbols
	public static void addSymbolToPeak(String nameList, String type, boolean isParameter){
		try{
			SymbolTable symbolTable = symbolTableStack.pop();
			symbolTable.addSymbol(nameList, type, isParameter);
			symbolTableStack.push(symbolTable);
		}catch(EmptyStackException ex){
			System.out.println(ex.toString());
			System.exit(1);
		}
	}
	
	
	// for strings symbols
	public static void addSymbolToPeak(String name, String type, String value, boolean isParameter){
		try{
			SymbolTable symbolTable = symbolTableStack.pop();
			symbolTable.addSymbol(name, type, value, isParameter);
			symbolTableStack.push(symbolTable);
		}catch(EmptyStackException ex){
			System.out.println(ex.toString());
			System.exit(1);
		}
	}
	
	public static void appendOutput(String message){
		outputMessage += message;
	}
	public static void printOutput(){
		outputMessage = outputMessage.substring(0, outputMessage.length() - 1);
		System.out.print(outputMessage);
	}
	
	public static String getConstructingFunctionScopeName(){
		return constructingFunctionScopeName;
	}
	
	// find the symbol table given its function name
	public static SymbolTable searchSymbolTable(String functionName){
		for (int i = 0; i < symbolTableStack.size(); i++){
			SymbolTable symbolTable = symbolTableStack.get(i);
			if (symbolTable.getScope().equals(functionName)){
				return symbolTable;
			}
		}
		return null;
	}
	// find the symbol table given the block id (for, if, else block)
	public static SymbolTable searchSymbolTable(int block_number){
		return searchSymbolTable("BLOCK " + block_number);
	}
	
	
	// given function name and parameter id OR local variable id,
	// return its slot location from the reference pointer
	public static int findSlotLocation(String functionName, String id_val, Stack<IRScope> subScopeStack){
		SymbolTable symbolTable = searchSymbolTable(functionName);
		LinkedHashMap<String, Symbol> symbolMap = symbolTable.getSymbolMap();
		if (symbolMap.containsKey(id_val)){
			return symbolMap.get(id_val).getSlotLocation();
		}else{
			for (int i = 0; i < subScopeStack.size(); i++){
				// get each symbol table of the walking subblock (FOR/ IF/ ELSE)
				SymbolTable subBlockSymTable = searchSymbolTable(subScopeStack.get(i).getBlockID());
				// get the symbol hashmap of the walking subblock 
				LinkedHashMap<String, Symbol> subBlockSymMap = subBlockSymTable.getSymbolMap();
				// if the subblock contains the desired local variable declaration, return its slot location
				if (subBlockSymMap.containsKey(id_val)){
					return subBlockSymMap.get(id_val).getSlotLocation();
				}
			}
		}
		System.out.println("; failed to find slot for " + id_val + " in function scope " + IRAction.currentFunctionScope + "\n");
		return 0;
	}
	/*
	public static int findSlotLocation(String functionName, String id_val, Stack<IRScope> subScopeStack){
		SymbolTable symbolTable = searchSymbolTable(functionName);
		LinkedHashMap<String, Symbol> symbolMap = symbolTable.getSymbolMap();
		if (symbolMap.containsKey(id_val)){
			return symbolMap.get(id_val).getSlotLocation();
		}else{
			for (int i = 0; i < subScopeStack.size(); i++){
				// get each symbol table of the walking subblock (FOR/ IF/ ELSE)
				SymbolTable subBlockSymTable = searchSymbolTable(subScopeStack.get(i).getBlockID());
				// get the symbol hashmap of the walking subblock 
				LinkedHashMap<String, Symbol> subBlockSymMap = subBlockSymTable.getSymbolMap();
				// if the subblock contains the desired local variable declaration, return its slot location
				if (subBlockSymMap.containsKey(id_val)){
					return subBlockSymMap.get(id_val).getSlotLocation();
				}
			}
		}
		System.out.println("; failed to find slot for " + id_val + " in function scope " + IRAction.currentFunctionScope + "\n");
		return 0;
	}
	*/
	
	// return the number of function parameters
	public static int getParameterSize(String functionName){
		SymbolTable symbolTable = searchSymbolTable(functionName);
		return symbolTable.getNumberOfParameters();
	}
	
	// get the number of local variables, including the local variables declared in the subscopes
	public static int getFullLocalVarSize(String functionName){
		SymbolTable symbolTable = searchSymbolTable(functionName);
		return symbolTable.getNumberOfLocalVar() + symbolTable.getSubScopeLocalVarSize();
	}
	
	// check if the variable is global
	public static boolean isGlobalVar(String var){
		LinkedHashMap<String, Symbol> symbolMap = symbolTableStack.get(0).getSymbolMap();
		if (symbolMap.containsKey(var)){
			return true;
		}
		return false;
	}
	
	public static boolean isGlobalStringId(String var){
		LinkedHashMap<String, Symbol> symbolMap = symbolTableStack.get(0).getSymbolMap();
		if (symbolMap.containsKey(var) && symbolMap.get(var).getType().equals("STRING")){
			return true;
		}
		return false;
	}
	
	// get a set of global variable identifiers
	public static HashSet<String> getGlobalVarSet(){
		LinkedHashMap<String, Symbol> symbolMap = symbolTableStack.get(0).getSymbolMap();
		HashSet<String> globalVarSet = new HashSet<String>();
		for (Map.Entry<String, Symbol> entry: symbolMap.entrySet()){
			Symbol sym = entry.getValue();
			if (!sym.getType().equals("STRING")){
				globalVarSet.add(sym.getName());	// do not add STRING variables because it won't be changed
			}
		}
		return globalVarSet;
	}
	
	// given a function name, find the return type
	public static String findReturnType(String functionName){
		SymbolTable symbolTable = searchSymbolTable(functionName);
		return symbolTable.getReturnType();
	}

}