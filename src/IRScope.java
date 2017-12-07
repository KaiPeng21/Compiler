
import java.util.*;

public class IRScope{
	public static final int FUNC_TYPE = 0;
	public static final int IF_TYPE = 1;
	public static final int ELSE_TYPE = 2;
	public static final int FOR_TYPE = 3;
	public static final int GLOBAL_TYPE = 4;
	
	private int scopeType;
	private int blockID;
	
	public IRScope(int scopeType, int blockID){
		this.scopeType = scopeType;
		this.blockID = blockID;
	}
	
	public int getScopeType(){
		return this.scopeType;
	}
	
	public int getBlockID(){
		return this.blockID;
	}
	
	public String toString(){
		String str = "; SUBSCOPE block ID: " + blockID + " scopeType: ";
		
		if (this.scopeType == IF_TYPE){
			str += "IF";
		} else if (this.scopeType == ELSE_TYPE){
			str += "ELSE";
		} else if (this.scopeType == FOR_TYPE){
			str += "FOR";
		} else if (this.scopeType == FUNC_TYPE){
			str += "FUNC";
		} else if (this.scopeType == GLOBAL_TYPE){
			str += "GLOBAL";
		}
		
		return str;
	}
}