
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.*;

public class Micro{
	public static void main(String[] args) throws IOException{

		// Get a stream of character from file
		CharStream cstream = CharStreams.fromFileName(args[0]);

		// Tokenize the character stream using Java ANTLR
		MicroLexer lex = new MicroLexer(cstream);
		
		// Create a parser and use check if an exception is raised
		TYTSMicroParser parser = new TYTSMicroParser(new CommonTokenStream(lex));
		
		//try{
			ParseTree ptree = parser.program();
			ParseTreeWalker ptwalker = new ParseTreeWalker();
			ptwalker.walk(new TYTSListener(), ptree);
		//} catch (Exception ex){
		//	System.out.println("Not accepted");
		//}
		
		
	}
}


// The MicroParser Class Created By Team TooYoungTooSimple
// Set the Error Strategy to the customized one which raised an error
class TYTSMicroParser extends MicroParser{

	public TYTSMicroParser(TokenStream token){
		super(token);
		this.setErrorHandler(new TYTSErrorStrategy());
	}

	public boolean IsProgramValid(){
		try{
			this.program();
		}catch(Exception e){
			return false;		
		}
		return true;
	}
}

// Error Strategy Class Created By Team TooYoungTooSimple
// Throw an exception for Recognition Exceptions
class TYTSErrorStrategy extends DefaultErrorStrategy{
	@Override
	public void reportError(Parser recognizer, RecognitionException e){
		throw e;
	}
}
