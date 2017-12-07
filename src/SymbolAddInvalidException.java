
public class SymbolAddInvalidException extends Exception{
	
	public static String errMessage;
	
	public SymbolAddInvalidException(String message){
		super(message);
		this.errMessage = message;
	}
}