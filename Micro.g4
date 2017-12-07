grammar Micro;


KEYWORD       : 
	'PROGRAM' | 'BEGIN' | 'END' | 'FUNCTION' | 'READ' | 'WRITE' | 'IF' | 'ELSE' | 'FI' | 'FOR' | 'ROF' | 'RETURN' | 'INT' | 'VOID' | 'STRING' | 'FLOAT' ;
IDENTIFIER    : 
	[A-Za-z][A-Za-z0-9]* ;
INTLITERAL    : 
	[0-9]+ ;
FLOATLITERAL  : 
	[0-9]*'.'[0-9]+ ;
COMMENT       : 
	'--'(~('\n'|'\r'))* -> skip ;
WHITESPACE    : 
	(' '|'\t'|'\n'|'\r')+ -> skip ;
OPERATOR      : 
	':=' | '+' | '-' | '*' | '/' | '=' | '!=' | '<' | '>' | '(' | ')' | ';' | ',' | '<=' | '>=' ;
STRINGLITERAL :
	'"' (~('"' | '\r' | '\n'))* '"' ;


// Program
program: 'PROGRAM' id 'BEGIN' pgm_body 'END' {/*SymbolTableStack.printOutput();*/}; 
id: IDENTIFIER;
pgm_body: {SymbolTableStack.createScope('g', IRScope.GLOBAL_TYPE);} decl {SymbolTableStack.appendOutput(SymbolTableStack.symbolTableStack.peek().toString());} func_declarations;
decl: string_decl decl | var_decl decl | ;

// Global String Declaration
string_decl: 'STRING' id ':=' str ';' {SymbolTableStack.addSymbolToPeak($id.text, "STRING", $str.text, false);};
str: STRINGLITERAL;

// Variable Declaration
var_decl: var_type id_list ';' {SymbolTableStack.addSymbolToPeak($id_list.text, $var_type.text, false);};
var_type: 'FLOAT' | 'INT';
any_type: var_type | 'VOID'; 
id_list: id id_tail;
id_tail: ',' id id_tail | ;

// Function Paramater List
param_decl_list: param_decl param_decl_tail | ;
param_decl: var_type id {SymbolTableStack.addSymbolToPeak($id.text, $var_type.text, true);};
param_decl_tail: ',' param_decl param_decl_tail | ;

// Function Declarations
func_declarations: func_decl func_declarations | ;
func_decl: 'FUNCTION' any_type id {SymbolTableStack.createScope($id.text, $any_type.text, IRScope.FUNC_TYPE);} '(' param_decl_list ')' 'BEGIN' func_body 'END';
func_body: decl {SymbolTableStack.appendOutput(SymbolTableStack.symbolTableStack.peek().toString());} stmt_list; 

// Statement List
stmt_list: stmt stmt_list | ;
stmt: base_stmt | if_stmt | for_stmt;
base_stmt: assign_stmt | read_stmt | write_stmt | return_stmt;

// Basic Statements
assign_stmt: assign_expr ';';
assign_expr: id ':=' expr ;
read_stmt: 'READ' '(' id_list ')' ';';
write_stmt: 'WRITE' '(' id_list ')' ';';
return_stmt: 'RETURN' expr ';';

// Expressions
expr: expr_prefix factor;
expr_prefix: expr_prefix factor addop | ;
factor: factor_prefix postfix_expr;
factor_prefix: factor_prefix postfix_expr mulop | ;
postfix_expr: primary | call_expr;
call_expr: id '(' expr_list ')';
expr_list: expr expr_list_tail | ;
expr_list_tail: ',' expr expr_list_tail | ;
primary: '(' expr ')' | id | INTLITERAL | FLOATLITERAL;
addop: '+' | '-';
mulop: '*' | '/';

// Complex Statements and Condition 
if_stmt: 'IF' '(' cond ')' {SymbolTableStack.createScope('b', IRScope.IF_TYPE);} decl {SymbolTableStack.appendOutput(SymbolTableStack.symbolTableStack.peek().toString());} stmt_list else_part 'FI';
else_part: 'ELSE' {SymbolTableStack.createScope('b', IRScope.ELSE_TYPE);} decl {SymbolTableStack.appendOutput(SymbolTableStack.symbolTableStack.peek().toString());} stmt_list | ;
cond: expr compop expr;
compop: '<' | '>' | '=' | '!=' | '<=' | '>=';

init_stmt: assign_expr | ;
incr_stmt: assign_expr | ;

for_stmt: 'FOR' '(' init_stmt ';' cond ';' incr_stmt ')' {SymbolTableStack.createScope('b', IRScope.FOR_TYPE);} decl {SymbolTableStack.appendOutput(SymbolTableStack.symbolTableStack.peek().toString());} stmt_list 'ROF';


