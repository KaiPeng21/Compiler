
public class Symbol{
	
	private String name;
	private String type;
	private String value;	
	private boolean isParameter;
	private int slotLocation;

	public Symbol(String name, String type, String value, boolean isParameter, int slotLocation){
		this.name = name;
		this.type = type;
		this.value = value;
		this.isParameter = isParameter;
		this.slotLocation = slotLocation;
		
		if (!SymbolTableStack.symbolTableStack.isEmpty()){
			if (SymbolTableStack.symbolTableStack.peek().getScopeType() == IRScope.FOR_TYPE || 
					SymbolTableStack.symbolTableStack.peek().getScopeType() == IRScope.IF_TYPE || 
					SymbolTableStack.symbolTableStack.peek().getScopeType() == IRScope.ELSE_TYPE){
				try{
					SymbolTableStack.searchSymbolTable(SymbolTableStack.getConstructingFunctionScopeName()).incrSubScopeLocalVarSize();
				}catch(Exception ex){
					System.out.println("; Error while creating symbol table");
				}
			}
		}
	}
	public Symbol(String name, String type, boolean isParameter, int slotLocation){
		this.name = name;
		this.type = type;
		this.value = null;
		this.isParameter = isParameter;
		this.slotLocation = slotLocation;
		
		if (!SymbolTableStack.symbolTableStack.isEmpty()){
			if (SymbolTableStack.symbolTableStack.peek().getScopeType() == IRScope.FOR_TYPE || 
					SymbolTableStack.symbolTableStack.peek().getScopeType() == IRScope.IF_TYPE || 
					SymbolTableStack.symbolTableStack.peek().getScopeType() == IRScope.ELSE_TYPE){
				try{
					SymbolTableStack.searchSymbolTable(SymbolTableStack.getConstructingFunctionScopeName()).incrSubScopeLocalVarSize();
				}catch(Exception ex){
					System.out.println("; Error while creating symbol table");
				}
			}
		}
	}

	public String getName(){
		return this.name;
	}
	public String getType(){
		return this.type;
	}
	public String getValue(){
		return this.value;
	}
	public boolean hasValue(){
		return this.value != null;
	}
	
	public void setValue(String value){
		this.value = value;
	}
	
	public boolean isParameter(){
		return this.isParameter;
	}
	
	public int getSlotLocation(){
		return this.slotLocation;
	}
	
}

