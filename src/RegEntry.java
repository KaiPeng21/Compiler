
// An Register Entry data structure for register allocation

public class RegEntry{
	
	private int regIndex;		// n in $Rn
	private String varName;		// variable stored in the the register
	private String varType;		// storing an INT or a FLOAT
	private boolean isDirty;	// dirty (modified) or not
	
//	private int lru_level; 		// the last time using this is lru_level instructions ahead
	
	private static int nextRegIndex = 0;
	
	public RegEntry(){
		this.regIndex = this.nextRegIndex++;
		this.varName = "";
		this.varType = "INT";
		this.isDirty = false;
//		this.lru_level = 0;
	}
	
	public int getRegIndex(){
		return this.regIndex;
	}
	
	public String getRegExpr(){
		return "r" + this.regIndex;
		//return "$R" + this.regIndex;
	}
	
	public String getVarName(){
		return this.varName;
	}
	
	public void setVarName(String varName){
		this.varName = varName;
	}
	
	public boolean isDirty(){
		return this.isDirty;
	}
	
	public void setDirty(boolean isDirty){
		this.isDirty = isDirty;
	}
	
	public boolean isEmpty(){
		return this.varName.equals("");
	}
	
	public void setVarType(String varType){
		this.varType = varType;
	}
	
	public String getVarType(){
		return this.varType;
	}
	
	/*
	public int getLRU_Level(){
		return this.lru_level;
	}
	
	public void incLRU_Level(){
		this.lru_level++;
	}
	
	public void decLRU_Level(){
		this.lru_level--;
		if (this.lru_level < 0){
			this.lru_level = 0;
		}
	}
	
	public void resetLRU_Level(){
		this.lru_level = 0;
	}
	
	*/
	
	public void updateRegister(String var, boolean dirtyBit){
		this.varName = var;
		this.isDirty = dirtyBit;
	}
	
	public void updateRegister(String var, String varType, boolean dirtyBit){
		this.varName = var;
		this.varType = varType;
		this.isDirty = dirtyBit;
	//	this.lru_level = 0;
	}
	
	public void clean(){
		this.varName = "";
		this.isDirty = false;
	//	this.lru_level = 0;
	}
	
	@Override
	public String toString(){
		
		String m = this.getRegExpr() + "->";
		if (!this.isEmpty()){
			m += this.varName;
		} else {
			m += "<null>";
		}
		if (this.isDirty){
			m += "*";
		}
		
		return m;
	}
	
}