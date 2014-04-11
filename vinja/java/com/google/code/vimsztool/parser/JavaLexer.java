package com.google.code.vimsztool.parser;

import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked"})
public class JavaLexer extends Lexer {
    public static final int EOF=-1;
    public static final int ABSTRACT=4;
    public static final int AND=5;
    public static final int AND_ASSIGN=6;
    public static final int ANNOTATION_INIT_ARRAY_ELEMENT=7;
    public static final int ANNOTATION_INIT_BLOCK=8;
    public static final int ANNOTATION_INIT_DEFAULT_KEY=9;
    public static final int ANNOTATION_INIT_KEY_LIST=10;
    public static final int ANNOTATION_LIST=11;
    public static final int ANNOTATION_METHOD_DECL=12;
    public static final int ANNOTATION_SCOPE=13;
    public static final int ANNOTATION_TOP_LEVEL_SCOPE=14;
    public static final int ARGUMENT_LIST=15;
    public static final int ARRAY_DECLARATOR=16;
    public static final int ARRAY_DECLARATOR_LIST=17;
    public static final int ARRAY_ELEMENT_ACCESS=18;
    public static final int ARRAY_INITIALIZER=19;
    public static final int ASSERT=20;
    public static final int ASSIGN=21;
    public static final int AT=22;
    public static final int BIT_SHIFT_RIGHT=23;
    public static final int BIT_SHIFT_RIGHT_ASSIGN=24;
    public static final int BLOCK_SCOPE=25;
    public static final int BOOLEAN=26;
    public static final int BREAK=27;
    public static final int BYTE=28;
    public static final int CASE=29;
    public static final int CAST_EXPR=30;
    public static final int CATCH=31;
    public static final int CATCH_CLAUSE_LIST=32;
    public static final int CHAR=33;
    public static final int CHARACTER_LITERAL=34;
    public static final int CLASS=35;
    public static final int CLASS_CONSTRUCTOR_CALL=36;
    public static final int CLASS_INSTANCE_INITIALIZER=37;
    public static final int CLASS_STATIC_INITIALIZER=38;
    public static final int CLASS_TOP_LEVEL_SCOPE=39;
    public static final int COLON=40;
    public static final int COMMA=41;
    public static final int COMMENT=42;
    public static final int CONSTRUCTOR_DECL=43;
    public static final int CONTINUE=44;
    public static final int DEC=45;
    public static final int DECIMAL_LITERAL=46;
    public static final int DEFAULT=47;
    public static final int DIV=48;
    public static final int DIV_ASSIGN=49;
    public static final int DO=50;
    public static final int DOT=51;
    public static final int DOTSTAR=52;
    public static final int DOUBLE=53;
    public static final int ELLIPSIS=54;
    public static final int ELSE=55;
    public static final int ENUM=56;
    public static final int ENUM_TOP_LEVEL_SCOPE=57;
    public static final int EQUAL=58;
    public static final int ESCAPE_SEQUENCE=59;
    public static final int EXPONENT=60;
    public static final int EXPR=61;
    public static final int EXTENDS=62;
    public static final int EXTENDS_BOUND_LIST=63;
    public static final int EXTENDS_CLAUSE=64;
    public static final int FALSE=65;
    public static final int FINAL=66;
    public static final int FINALLY=67;
    public static final int FLOAT=68;
    public static final int FLOATING_POINT_LITERAL=69;
    public static final int FLOAT_TYPE_SUFFIX=70;
    public static final int FOR=71;
    public static final int FORMAL_PARAM_LIST=72;
    public static final int FORMAL_PARAM_STD_DECL=73;
    public static final int FORMAL_PARAM_VARARG_DECL=74;
    public static final int FOR_CONDITION=75;
    public static final int FOR_EACH=76;
    public static final int FOR_INIT=77;
    public static final int FOR_UPDATE=78;
    public static final int FUNCTION_METHOD_DECL=79;
    public static final int GENERIC_TYPE_ARG_LIST=80;
    public static final int GENERIC_TYPE_PARAM_LIST=81;
    public static final int GREATER_OR_EQUAL=82;
    public static final int GREATER_THAN=83;
    public static final int HEX_DIGIT=84;
    public static final int HEX_LITERAL=85;
    public static final int IDENT=86;
    public static final int IF=87;
    public static final int IMPLEMENTS=88;
    public static final int IMPLEMENTS_CLAUSE=89;
    public static final int IMPORT=90;
    public static final int INC=91;
    public static final int INSTANCEOF=92;
    public static final int INT=93;
    public static final int INTEGER_TYPE_SUFFIX=94;
    public static final int INTERFACE=95;
    public static final int INTERFACE_TOP_LEVEL_SCOPE=96;
    public static final int JAVA_ID_PART=97;
    public static final int JAVA_ID_START=98;
    public static final int JAVA_SOURCE=99;
    public static final int LABELED_STATEMENT=100;
    public static final int LBRACK=101;
    public static final int LCURLY=102;
    public static final int LESS_OR_EQUAL=103;
    public static final int LESS_THAN=104;
    public static final int LINE_COMMENT=105;
    public static final int LOCAL_MODIFIER_LIST=106;
    public static final int LOGICAL_AND=107;
    public static final int LOGICAL_NOT=108;
    public static final int LOGICAL_OR=109;
    public static final int LONG=110;
    public static final int LPAREN=111;
    public static final int METHOD_CALL=112;
    public static final int MINUS=113;
    public static final int MINUS_ASSIGN=114;
    public static final int MOD=115;
    public static final int MODIFIER_LIST=116;
    public static final int MOD_ASSIGN=117;
    public static final int NATIVE=118;
    public static final int NEW=119;
    public static final int NOT=120;
    public static final int NOT_EQUAL=121;
    public static final int NULL=122;
    public static final int OCTAL_ESCAPE=123;
    public static final int OCTAL_LITERAL=124;
    public static final int OR=125;
    public static final int OR_ASSIGN=126;
    public static final int PACKAGE=127;
    public static final int PARENTESIZED_EXPR=128;
    public static final int PLUS=129;
    public static final int PLUS_ASSIGN=130;
    public static final int POST_DEC=131;
    public static final int POST_INC=132;
    public static final int PRE_DEC=133;
    public static final int PRE_INC=134;
    public static final int PRIVATE=135;
    public static final int PROTECTED=136;
    public static final int PUBLIC=137;
    public static final int QUALIFIED_TYPE_IDENT=138;
    public static final int QUESTION=139;
    public static final int RBRACK=140;
    public static final int RCURLY=141;
    public static final int RETURN=142;
    public static final int RPAREN=143;
    public static final int SEMI=144;
    public static final int SHIFT_LEFT=145;
    public static final int SHIFT_LEFT_ASSIGN=146;
    public static final int SHIFT_RIGHT=147;
    public static final int SHIFT_RIGHT_ASSIGN=148;
    public static final int SHORT=149;
    public static final int STAR=150;
    public static final int STAR_ASSIGN=151;
    public static final int STATIC=152;
    public static final int STATIC_ARRAY_CREATOR=153;
    public static final int STRICTFP=154;
    public static final int STRING_LITERAL=155;
    public static final int SUPER=156;
    public static final int SUPER_CONSTRUCTOR_CALL=157;
    public static final int SWITCH=158;
    public static final int SWITCH_BLOCK_LABEL_LIST=159;
    public static final int SYNCHRONIZED=160;
    public static final int THIS=161;
    public static final int THIS_CONSTRUCTOR_CALL=162;
    public static final int THROW=163;
    public static final int THROWS=164;
    public static final int THROWS_CLAUSE=165;
    public static final int TRANSIENT=166;
    public static final int TRUE=167;
    public static final int TRY=168;
    public static final int TYPE=169;
    public static final int UNARY_MINUS=170;
    public static final int UNARY_PLUS=171;
    public static final int UNICODE_ESCAPE=172;
    public static final int VAR_DECLARATION=173;
    public static final int VAR_DECLARATOR=174;
    public static final int VAR_DECLARATOR_LIST=175;
    public static final int VOID=176;
    public static final int VOID_METHOD_DECL=177;
    public static final int VOLATILE=178;
    public static final int WHILE=179;
    public static final int WS=180;
    public static final int XOR=181;
    public static final int XOR_ASSIGN=182;

    /** 
     *  Determines if whitespaces and comments should be preserved or thrown away.
     *
     *  If <code>true</code> whitespaces and comments will be preserved within the
     *  hidden channel, otherwise the appropriate tokens will be skiped. This is
     *  a 'little bit' expensive, of course. If only one of the two behaviours is
     *  needed forever the lexer part of the grammar should be changed by replacing 
     *  the 'if-else' stuff within the approprate lexer grammar actions.
     */
    public boolean preserveWhitespacesAndComments = false;


    // delegates
    // delegators
    public Lexer[] getDelegates() {
        return new Lexer[] {};
    }

    public JavaLexer() {} 
    public JavaLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public JavaLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);
    }
    public String getGrammarFileName() { return "./src/main/java/hero/antlr/Java.g"; }

    // $ANTLR start "ABSTRACT"
    public final void mABSTRACT() throws RecognitionException {
        try {
            int _type = ABSTRACT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:16:10: ( 'abstract' )
            // ./src/main/java/hero/antlr/Java.g:16:12: 'abstract'
            {
            match("abstract"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ABSTRACT"

    // $ANTLR start "AND"
    public final void mAND() throws RecognitionException {
        try {
            int _type = AND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:17:5: ( '&' )
            // ./src/main/java/hero/antlr/Java.g:17:7: '&'
            {
            match('&'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "AND"

    // $ANTLR start "AND_ASSIGN"
    public final void mAND_ASSIGN() throws RecognitionException {
        try {
            int _type = AND_ASSIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:18:12: ( '&=' )
            // ./src/main/java/hero/antlr/Java.g:18:14: '&='
            {
            match("&="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "AND_ASSIGN"

    // $ANTLR start "ASSERT"
    public final void mASSERT() throws RecognitionException {
        try {
            int _type = ASSERT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:19:8: ( 'assert' )
            // ./src/main/java/hero/antlr/Java.g:19:10: 'assert'
            {
            match("assert"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ASSERT"

    // $ANTLR start "ASSIGN"
    public final void mASSIGN() throws RecognitionException {
        try {
            int _type = ASSIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:20:8: ( '=' )
            // ./src/main/java/hero/antlr/Java.g:20:10: '='
            {
            match('='); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ASSIGN"

    // $ANTLR start "AT"
    public final void mAT() throws RecognitionException {
        try {
            int _type = AT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:21:4: ( '@' )
            // ./src/main/java/hero/antlr/Java.g:21:6: '@'
            {
            match('@'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "AT"

    // $ANTLR start "BIT_SHIFT_RIGHT"
    public final void mBIT_SHIFT_RIGHT() throws RecognitionException {
        try {
            int _type = BIT_SHIFT_RIGHT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:22:17: ( '>>>' )
            // ./src/main/java/hero/antlr/Java.g:22:19: '>>>'
            {
            match(">>>"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "BIT_SHIFT_RIGHT"

    // $ANTLR start "BIT_SHIFT_RIGHT_ASSIGN"
    public final void mBIT_SHIFT_RIGHT_ASSIGN() throws RecognitionException {
        try {
            int _type = BIT_SHIFT_RIGHT_ASSIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:23:24: ( '>>>=' )
            // ./src/main/java/hero/antlr/Java.g:23:26: '>>>='
            {
            match(">>>="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "BIT_SHIFT_RIGHT_ASSIGN"

    // $ANTLR start "BOOLEAN"
    public final void mBOOLEAN() throws RecognitionException {
        try {
            int _type = BOOLEAN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:24:9: ( 'boolean' )
            // ./src/main/java/hero/antlr/Java.g:24:11: 'boolean'
            {
            match("boolean"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "BOOLEAN"

    // $ANTLR start "BREAK"
    public final void mBREAK() throws RecognitionException {
        try {
            int _type = BREAK;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:25:7: ( 'break' )
            // ./src/main/java/hero/antlr/Java.g:25:9: 'break'
            {
            match("break"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "BREAK"

    // $ANTLR start "BYTE"
    public final void mBYTE() throws RecognitionException {
        try {
            int _type = BYTE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:26:6: ( 'byte' )
            // ./src/main/java/hero/antlr/Java.g:26:8: 'byte'
            {
            match("byte"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "BYTE"

    // $ANTLR start "CASE"
    public final void mCASE() throws RecognitionException {
        try {
            int _type = CASE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:27:6: ( 'case' )
            // ./src/main/java/hero/antlr/Java.g:27:8: 'case'
            {
            match("case"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "CASE"

    // $ANTLR start "CATCH"
    public final void mCATCH() throws RecognitionException {
        try {
            int _type = CATCH;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:28:7: ( 'catch' )
            // ./src/main/java/hero/antlr/Java.g:28:9: 'catch'
            {
            match("catch"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "CATCH"

    // $ANTLR start "CHAR"
    public final void mCHAR() throws RecognitionException {
        try {
            int _type = CHAR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:29:6: ( 'char' )
            // ./src/main/java/hero/antlr/Java.g:29:8: 'char'
            {
            match("char"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "CHAR"

    // $ANTLR start "CLASS"
    public final void mCLASS() throws RecognitionException {
        try {
            int _type = CLASS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:30:7: ( 'class' )
            // ./src/main/java/hero/antlr/Java.g:30:9: 'class'
            {
            match("class"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "CLASS"

    // $ANTLR start "COLON"
    public final void mCOLON() throws RecognitionException {
        try {
            int _type = COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:31:7: ( ':' )
            // ./src/main/java/hero/antlr/Java.g:31:9: ':'
            {
            match(':'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "COLON"

    // $ANTLR start "COMMA"
    public final void mCOMMA() throws RecognitionException {
        try {
            int _type = COMMA;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:32:7: ( ',' )
            // ./src/main/java/hero/antlr/Java.g:32:9: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "COMMA"

    // $ANTLR start "CONTINUE"
    public final void mCONTINUE() throws RecognitionException {
        try {
            int _type = CONTINUE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:33:10: ( 'continue' )
            // ./src/main/java/hero/antlr/Java.g:33:12: 'continue'
            {
            match("continue"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "CONTINUE"

    // $ANTLR start "DEC"
    public final void mDEC() throws RecognitionException {
        try {
            int _type = DEC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:34:5: ( '--' )
            // ./src/main/java/hero/antlr/Java.g:34:7: '--'
            {
            match("--"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DEC"

    // $ANTLR start "DEFAULT"
    public final void mDEFAULT() throws RecognitionException {
        try {
            int _type = DEFAULT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:35:9: ( 'default' )
            // ./src/main/java/hero/antlr/Java.g:35:11: 'default'
            {
            match("default"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DEFAULT"

    // $ANTLR start "DIV"
    public final void mDIV() throws RecognitionException {
        try {
            int _type = DIV;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:36:5: ( '/' )
            // ./src/main/java/hero/antlr/Java.g:36:7: '/'
            {
            match('/'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DIV"

    // $ANTLR start "DIV_ASSIGN"
    public final void mDIV_ASSIGN() throws RecognitionException {
        try {
            int _type = DIV_ASSIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:37:12: ( '/=' )
            // ./src/main/java/hero/antlr/Java.g:37:14: '/='
            {
            match("/="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DIV_ASSIGN"

    // $ANTLR start "DO"
    public final void mDO() throws RecognitionException {
        try {
            int _type = DO;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:38:4: ( 'do' )
            // ./src/main/java/hero/antlr/Java.g:38:6: 'do'
            {
            match("do"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DO"

    // $ANTLR start "DOT"
    public final void mDOT() throws RecognitionException {
        try {
            int _type = DOT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:39:5: ( '.' )
            // ./src/main/java/hero/antlr/Java.g:39:7: '.'
            {
            match('.'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DOT"

    // $ANTLR start "DOTSTAR"
    public final void mDOTSTAR() throws RecognitionException {
        try {
            int _type = DOTSTAR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:40:9: ( '.*' )
            // ./src/main/java/hero/antlr/Java.g:40:11: '.*'
            {
            match(".*"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DOTSTAR"

    // $ANTLR start "DOUBLE"
    public final void mDOUBLE() throws RecognitionException {
        try {
            int _type = DOUBLE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:41:8: ( 'double' )
            // ./src/main/java/hero/antlr/Java.g:41:10: 'double'
            {
            match("double"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DOUBLE"

    // $ANTLR start "ELLIPSIS"
    public final void mELLIPSIS() throws RecognitionException {
        try {
            int _type = ELLIPSIS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:42:10: ( '...' )
            // ./src/main/java/hero/antlr/Java.g:42:12: '...'
            {
            match("..."); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ELLIPSIS"

    // $ANTLR start "ELSE"
    public final void mELSE() throws RecognitionException {
        try {
            int _type = ELSE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:43:6: ( 'else' )
            // ./src/main/java/hero/antlr/Java.g:43:8: 'else'
            {
            match("else"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ELSE"

    // $ANTLR start "ENUM"
    public final void mENUM() throws RecognitionException {
        try {
            int _type = ENUM;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:44:6: ( 'enum' )
            // ./src/main/java/hero/antlr/Java.g:44:8: 'enum'
            {
            match("enum"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ENUM"

    // $ANTLR start "EQUAL"
    public final void mEQUAL() throws RecognitionException {
        try {
            int _type = EQUAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:45:7: ( '==' )
            // ./src/main/java/hero/antlr/Java.g:45:9: '=='
            {
            match("=="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "EQUAL"

    // $ANTLR start "EXTENDS"
    public final void mEXTENDS() throws RecognitionException {
        try {
            int _type = EXTENDS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:46:9: ( 'extends' )
            // ./src/main/java/hero/antlr/Java.g:46:11: 'extends'
            {
            match("extends"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "EXTENDS"

    // $ANTLR start "FALSE"
    public final void mFALSE() throws RecognitionException {
        try {
            int _type = FALSE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:47:7: ( 'false' )
            // ./src/main/java/hero/antlr/Java.g:47:9: 'false'
            {
            match("false"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "FALSE"

    // $ANTLR start "FINAL"
    public final void mFINAL() throws RecognitionException {
        try {
            int _type = FINAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:48:7: ( 'final' )
            // ./src/main/java/hero/antlr/Java.g:48:9: 'final'
            {
            match("final"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "FINAL"

    // $ANTLR start "FINALLY"
    public final void mFINALLY() throws RecognitionException {
        try {
            int _type = FINALLY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:49:9: ( 'finally' )
            // ./src/main/java/hero/antlr/Java.g:49:11: 'finally'
            {
            match("finally"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "FINALLY"

    // $ANTLR start "FLOAT"
    public final void mFLOAT() throws RecognitionException {
        try {
            int _type = FLOAT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:50:7: ( 'float' )
            // ./src/main/java/hero/antlr/Java.g:50:9: 'float'
            {
            match("float"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "FLOAT"

    // $ANTLR start "FOR"
    public final void mFOR() throws RecognitionException {
        try {
            int _type = FOR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:51:5: ( 'for' )
            // ./src/main/java/hero/antlr/Java.g:51:7: 'for'
            {
            match("for"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "FOR"

    // $ANTLR start "GREATER_OR_EQUAL"
    public final void mGREATER_OR_EQUAL() throws RecognitionException {
        try {
            int _type = GREATER_OR_EQUAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:52:18: ( '>=' )
            // ./src/main/java/hero/antlr/Java.g:52:20: '>='
            {
            match(">="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "GREATER_OR_EQUAL"

    // $ANTLR start "GREATER_THAN"
    public final void mGREATER_THAN() throws RecognitionException {
        try {
            int _type = GREATER_THAN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:53:14: ( '>' )
            // ./src/main/java/hero/antlr/Java.g:53:16: '>'
            {
            match('>'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "GREATER_THAN"

    // $ANTLR start "IF"
    public final void mIF() throws RecognitionException {
        try {
            int _type = IF;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:54:4: ( 'if' )
            // ./src/main/java/hero/antlr/Java.g:54:6: 'if'
            {
            match("if"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "IF"

    // $ANTLR start "IMPLEMENTS"
    public final void mIMPLEMENTS() throws RecognitionException {
        try {
            int _type = IMPLEMENTS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:55:12: ( 'implements' )
            // ./src/main/java/hero/antlr/Java.g:55:14: 'implements'
            {
            match("implements"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "IMPLEMENTS"

    // $ANTLR start "IMPORT"
    public final void mIMPORT() throws RecognitionException {
        try {
            int _type = IMPORT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:56:8: ( 'import' )
            // ./src/main/java/hero/antlr/Java.g:56:10: 'import'
            {
            match("import"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "IMPORT"

    // $ANTLR start "INC"
    public final void mINC() throws RecognitionException {
        try {
            int _type = INC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:57:5: ( '++' )
            // ./src/main/java/hero/antlr/Java.g:57:7: '++'
            {
            match("++"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "INC"

    // $ANTLR start "INSTANCEOF"
    public final void mINSTANCEOF() throws RecognitionException {
        try {
            int _type = INSTANCEOF;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:58:12: ( 'instanceof' )
            // ./src/main/java/hero/antlr/Java.g:58:14: 'instanceof'
            {
            match("instanceof"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "INSTANCEOF"

    // $ANTLR start "INT"
    public final void mINT() throws RecognitionException {
        try {
            int _type = INT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:59:5: ( 'int' )
            // ./src/main/java/hero/antlr/Java.g:59:7: 'int'
            {
            match("int"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "INT"

    // $ANTLR start "INTERFACE"
    public final void mINTERFACE() throws RecognitionException {
        try {
            int _type = INTERFACE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:60:11: ( 'interface' )
            // ./src/main/java/hero/antlr/Java.g:60:13: 'interface'
            {
            match("interface"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "INTERFACE"

    // $ANTLR start "LBRACK"
    public final void mLBRACK() throws RecognitionException {
        try {
            int _type = LBRACK;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:61:8: ( '[' )
            // ./src/main/java/hero/antlr/Java.g:61:10: '['
            {
            match('['); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LBRACK"

    // $ANTLR start "LCURLY"
    public final void mLCURLY() throws RecognitionException {
        try {
            int _type = LCURLY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:62:8: ( '{' )
            // ./src/main/java/hero/antlr/Java.g:62:10: '{'
            {
            match('{'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LCURLY"

    // $ANTLR start "LESS_OR_EQUAL"
    public final void mLESS_OR_EQUAL() throws RecognitionException {
        try {
            int _type = LESS_OR_EQUAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:63:15: ( '<=' )
            // ./src/main/java/hero/antlr/Java.g:63:17: '<='
            {
            match("<="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LESS_OR_EQUAL"

    // $ANTLR start "LESS_THAN"
    public final void mLESS_THAN() throws RecognitionException {
        try {
            int _type = LESS_THAN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:64:11: ( '<' )
            // ./src/main/java/hero/antlr/Java.g:64:13: '<'
            {
            match('<'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LESS_THAN"

    // $ANTLR start "LOGICAL_AND"
    public final void mLOGICAL_AND() throws RecognitionException {
        try {
            int _type = LOGICAL_AND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:65:13: ( '&&' )
            // ./src/main/java/hero/antlr/Java.g:65:15: '&&'
            {
            match("&&"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LOGICAL_AND"

    // $ANTLR start "LOGICAL_NOT"
    public final void mLOGICAL_NOT() throws RecognitionException {
        try {
            int _type = LOGICAL_NOT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:66:13: ( '!' )
            // ./src/main/java/hero/antlr/Java.g:66:15: '!'
            {
            match('!'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LOGICAL_NOT"

    // $ANTLR start "LOGICAL_OR"
    public final void mLOGICAL_OR() throws RecognitionException {
        try {
            int _type = LOGICAL_OR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:67:12: ( '||' )
            // ./src/main/java/hero/antlr/Java.g:67:14: '||'
            {
            match("||"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LOGICAL_OR"

    // $ANTLR start "LONG"
    public final void mLONG() throws RecognitionException {
        try {
            int _type = LONG;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:68:6: ( 'long' )
            // ./src/main/java/hero/antlr/Java.g:68:8: 'long'
            {
            match("long"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LONG"

    // $ANTLR start "LPAREN"
    public final void mLPAREN() throws RecognitionException {
        try {
            int _type = LPAREN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:69:8: ( '(' )
            // ./src/main/java/hero/antlr/Java.g:69:10: '('
            {
            match('('); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LPAREN"

    // $ANTLR start "MINUS"
    public final void mMINUS() throws RecognitionException {
        try {
            int _type = MINUS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:70:7: ( '-' )
            // ./src/main/java/hero/antlr/Java.g:70:9: '-'
            {
            match('-'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "MINUS"

    // $ANTLR start "MINUS_ASSIGN"
    public final void mMINUS_ASSIGN() throws RecognitionException {
        try {
            int _type = MINUS_ASSIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:71:14: ( '-=' )
            // ./src/main/java/hero/antlr/Java.g:71:16: '-='
            {
            match("-="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "MINUS_ASSIGN"

    // $ANTLR start "MOD"
    public final void mMOD() throws RecognitionException {
        try {
            int _type = MOD;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:72:5: ( '%' )
            // ./src/main/java/hero/antlr/Java.g:72:7: '%'
            {
            match('%'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "MOD"

    // $ANTLR start "MOD_ASSIGN"
    public final void mMOD_ASSIGN() throws RecognitionException {
        try {
            int _type = MOD_ASSIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:73:12: ( '%=' )
            // ./src/main/java/hero/antlr/Java.g:73:14: '%='
            {
            match("%="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "MOD_ASSIGN"

    // $ANTLR start "NATIVE"
    public final void mNATIVE() throws RecognitionException {
        try {
            int _type = NATIVE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:74:8: ( 'native' )
            // ./src/main/java/hero/antlr/Java.g:74:10: 'native'
            {
            match("native"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "NATIVE"

    // $ANTLR start "NEW"
    public final void mNEW() throws RecognitionException {
        try {
            int _type = NEW;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:75:5: ( 'new' )
            // ./src/main/java/hero/antlr/Java.g:75:7: 'new'
            {
            match("new"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "NEW"

    // $ANTLR start "NOT"
    public final void mNOT() throws RecognitionException {
        try {
            int _type = NOT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:76:5: ( '~' )
            // ./src/main/java/hero/antlr/Java.g:76:7: '~'
            {
            match('~'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "NOT"

    // $ANTLR start "NOT_EQUAL"
    public final void mNOT_EQUAL() throws RecognitionException {
        try {
            int _type = NOT_EQUAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:77:11: ( '!=' )
            // ./src/main/java/hero/antlr/Java.g:77:13: '!='
            {
            match("!="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "NOT_EQUAL"

    // $ANTLR start "NULL"
    public final void mNULL() throws RecognitionException {
        try {
            int _type = NULL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:78:6: ( 'null' )
            // ./src/main/java/hero/antlr/Java.g:78:8: 'null'
            {
            match("null"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "NULL"

    // $ANTLR start "OR"
    public final void mOR() throws RecognitionException {
        try {
            int _type = OR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:79:4: ( '|' )
            // ./src/main/java/hero/antlr/Java.g:79:6: '|'
            {
            match('|'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "OR"

    // $ANTLR start "OR_ASSIGN"
    public final void mOR_ASSIGN() throws RecognitionException {
        try {
            int _type = OR_ASSIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:80:11: ( '|=' )
            // ./src/main/java/hero/antlr/Java.g:80:13: '|='
            {
            match("|="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "OR_ASSIGN"

    // $ANTLR start "PACKAGE"
    public final void mPACKAGE() throws RecognitionException {
        try {
            int _type = PACKAGE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:81:9: ( 'package' )
            // ./src/main/java/hero/antlr/Java.g:81:11: 'package'
            {
            match("package"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PACKAGE"

    // $ANTLR start "PLUS"
    public final void mPLUS() throws RecognitionException {
        try {
            int _type = PLUS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:82:6: ( '+' )
            // ./src/main/java/hero/antlr/Java.g:82:8: '+'
            {
            match('+'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PLUS"

    // $ANTLR start "PLUS_ASSIGN"
    public final void mPLUS_ASSIGN() throws RecognitionException {
        try {
            int _type = PLUS_ASSIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:83:13: ( '+=' )
            // ./src/main/java/hero/antlr/Java.g:83:15: '+='
            {
            match("+="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PLUS_ASSIGN"

    // $ANTLR start "PRIVATE"
    public final void mPRIVATE() throws RecognitionException {
        try {
            int _type = PRIVATE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:84:9: ( 'private' )
            // ./src/main/java/hero/antlr/Java.g:84:11: 'private'
            {
            match("private"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PRIVATE"

    // $ANTLR start "PROTECTED"
    public final void mPROTECTED() throws RecognitionException {
        try {
            int _type = PROTECTED;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:85:11: ( 'protected' )
            // ./src/main/java/hero/antlr/Java.g:85:13: 'protected'
            {
            match("protected"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PROTECTED"

    // $ANTLR start "PUBLIC"
    public final void mPUBLIC() throws RecognitionException {
        try {
            int _type = PUBLIC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:86:8: ( 'public' )
            // ./src/main/java/hero/antlr/Java.g:86:10: 'public'
            {
            match("public"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PUBLIC"

    // $ANTLR start "QUESTION"
    public final void mQUESTION() throws RecognitionException {
        try {
            int _type = QUESTION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:87:10: ( '?' )
            // ./src/main/java/hero/antlr/Java.g:87:12: '?'
            {
            match('?'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "QUESTION"

    // $ANTLR start "RBRACK"
    public final void mRBRACK() throws RecognitionException {
        try {
            int _type = RBRACK;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:88:8: ( ']' )
            // ./src/main/java/hero/antlr/Java.g:88:10: ']'
            {
            match(']'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "RBRACK"

    // $ANTLR start "RCURLY"
    public final void mRCURLY() throws RecognitionException {
        try {
            int _type = RCURLY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:89:8: ( '}' )
            // ./src/main/java/hero/antlr/Java.g:89:10: '}'
            {
            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "RCURLY"

    // $ANTLR start "RETURN"
    public final void mRETURN() throws RecognitionException {
        try {
            int _type = RETURN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:90:8: ( 'return' )
            // ./src/main/java/hero/antlr/Java.g:90:10: 'return'
            {
            match("return"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "RETURN"

    // $ANTLR start "RPAREN"
    public final void mRPAREN() throws RecognitionException {
        try {
            int _type = RPAREN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:91:8: ( ')' )
            // ./src/main/java/hero/antlr/Java.g:91:10: ')'
            {
            match(')'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "RPAREN"

    // $ANTLR start "SEMI"
    public final void mSEMI() throws RecognitionException {
        try {
            int _type = SEMI;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:92:6: ( ';' )
            // ./src/main/java/hero/antlr/Java.g:92:8: ';'
            {
            match(';'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "SEMI"

    // $ANTLR start "SHIFT_LEFT"
    public final void mSHIFT_LEFT() throws RecognitionException {
        try {
            int _type = SHIFT_LEFT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:93:12: ( '<<' )
            // ./src/main/java/hero/antlr/Java.g:93:14: '<<'
            {
            match("<<"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "SHIFT_LEFT"

    // $ANTLR start "SHIFT_LEFT_ASSIGN"
    public final void mSHIFT_LEFT_ASSIGN() throws RecognitionException {
        try {
            int _type = SHIFT_LEFT_ASSIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:94:19: ( '<<=' )
            // ./src/main/java/hero/antlr/Java.g:94:21: '<<='
            {
            match("<<="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "SHIFT_LEFT_ASSIGN"

    // $ANTLR start "SHIFT_RIGHT"
    public final void mSHIFT_RIGHT() throws RecognitionException {
        try {
            int _type = SHIFT_RIGHT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:95:13: ( '>>' )
            // ./src/main/java/hero/antlr/Java.g:95:15: '>>'
            {
            match(">>"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "SHIFT_RIGHT"

    // $ANTLR start "SHIFT_RIGHT_ASSIGN"
    public final void mSHIFT_RIGHT_ASSIGN() throws RecognitionException {
        try {
            int _type = SHIFT_RIGHT_ASSIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:96:20: ( '>>=' )
            // ./src/main/java/hero/antlr/Java.g:96:22: '>>='
            {
            match(">>="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "SHIFT_RIGHT_ASSIGN"

    // $ANTLR start "SHORT"
    public final void mSHORT() throws RecognitionException {
        try {
            int _type = SHORT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:97:7: ( 'short' )
            // ./src/main/java/hero/antlr/Java.g:97:9: 'short'
            {
            match("short"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "SHORT"

    // $ANTLR start "STAR"
    public final void mSTAR() throws RecognitionException {
        try {
            int _type = STAR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:98:6: ( '*' )
            // ./src/main/java/hero/antlr/Java.g:98:8: '*'
            {
            match('*'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "STAR"

    // $ANTLR start "STAR_ASSIGN"
    public final void mSTAR_ASSIGN() throws RecognitionException {
        try {
            int _type = STAR_ASSIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:99:13: ( '*=' )
            // ./src/main/java/hero/antlr/Java.g:99:15: '*='
            {
            match("*="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "STAR_ASSIGN"

    // $ANTLR start "STATIC"
    public final void mSTATIC() throws RecognitionException {
        try {
            int _type = STATIC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:100:8: ( 'static' )
            // ./src/main/java/hero/antlr/Java.g:100:10: 'static'
            {
            match("static"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "STATIC"

    // $ANTLR start "STRICTFP"
    public final void mSTRICTFP() throws RecognitionException {
        try {
            int _type = STRICTFP;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:101:10: ( 'strictfp' )
            // ./src/main/java/hero/antlr/Java.g:101:12: 'strictfp'
            {
            match("strictfp"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "STRICTFP"

    // $ANTLR start "SUPER"
    public final void mSUPER() throws RecognitionException {
        try {
            int _type = SUPER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:102:7: ( 'super' )
            // ./src/main/java/hero/antlr/Java.g:102:9: 'super'
            {
            match("super"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "SUPER"

    // $ANTLR start "SWITCH"
    public final void mSWITCH() throws RecognitionException {
        try {
            int _type = SWITCH;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:103:8: ( 'switch' )
            // ./src/main/java/hero/antlr/Java.g:103:10: 'switch'
            {
            match("switch"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "SWITCH"

    // $ANTLR start "SYNCHRONIZED"
    public final void mSYNCHRONIZED() throws RecognitionException {
        try {
            int _type = SYNCHRONIZED;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:104:14: ( 'synchronized' )
            // ./src/main/java/hero/antlr/Java.g:104:16: 'synchronized'
            {
            match("synchronized"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "SYNCHRONIZED"

    // $ANTLR start "THIS"
    public final void mTHIS() throws RecognitionException {
        try {
            int _type = THIS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:105:6: ( 'this' )
            // ./src/main/java/hero/antlr/Java.g:105:8: 'this'
            {
            match("this"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "THIS"

    // $ANTLR start "THROW"
    public final void mTHROW() throws RecognitionException {
        try {
            int _type = THROW;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:106:7: ( 'throw' )
            // ./src/main/java/hero/antlr/Java.g:106:9: 'throw'
            {
            match("throw"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "THROW"

    // $ANTLR start "THROWS"
    public final void mTHROWS() throws RecognitionException {
        try {
            int _type = THROWS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:107:8: ( 'throws' )
            // ./src/main/java/hero/antlr/Java.g:107:10: 'throws'
            {
            match("throws"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "THROWS"

    // $ANTLR start "TRANSIENT"
    public final void mTRANSIENT() throws RecognitionException {
        try {
            int _type = TRANSIENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:108:11: ( 'transient' )
            // ./src/main/java/hero/antlr/Java.g:108:13: 'transient'
            {
            match("transient"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "TRANSIENT"

    // $ANTLR start "TRUE"
    public final void mTRUE() throws RecognitionException {
        try {
            int _type = TRUE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:109:6: ( 'true' )
            // ./src/main/java/hero/antlr/Java.g:109:8: 'true'
            {
            match("true"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "TRUE"

    // $ANTLR start "TRY"
    public final void mTRY() throws RecognitionException {
        try {
            int _type = TRY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:110:5: ( 'try' )
            // ./src/main/java/hero/antlr/Java.g:110:7: 'try'
            {
            match("try"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "TRY"

    // $ANTLR start "VOID"
    public final void mVOID() throws RecognitionException {
        try {
            int _type = VOID;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:111:6: ( 'void' )
            // ./src/main/java/hero/antlr/Java.g:111:8: 'void'
            {
            match("void"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "VOID"

    // $ANTLR start "VOLATILE"
    public final void mVOLATILE() throws RecognitionException {
        try {
            int _type = VOLATILE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:112:10: ( 'volatile' )
            // ./src/main/java/hero/antlr/Java.g:112:12: 'volatile'
            {
            match("volatile"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "VOLATILE"

    // $ANTLR start "WHILE"
    public final void mWHILE() throws RecognitionException {
        try {
            int _type = WHILE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:113:7: ( 'while' )
            // ./src/main/java/hero/antlr/Java.g:113:9: 'while'
            {
            match("while"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "WHILE"

    // $ANTLR start "XOR"
    public final void mXOR() throws RecognitionException {
        try {
            int _type = XOR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:114:5: ( '^' )
            // ./src/main/java/hero/antlr/Java.g:114:7: '^'
            {
            match('^'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "XOR"

    // $ANTLR start "XOR_ASSIGN"
    public final void mXOR_ASSIGN() throws RecognitionException {
        try {
            int _type = XOR_ASSIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:115:12: ( '^=' )
            // ./src/main/java/hero/antlr/Java.g:115:14: '^='
            {
            match("^="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "XOR_ASSIGN"

    // $ANTLR start "HEX_LITERAL"
    public final void mHEX_LITERAL() throws RecognitionException {
        try {
            int _type = HEX_LITERAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:1078:13: ( '0' ( 'x' | 'X' ) ( HEX_DIGIT )+ ( INTEGER_TYPE_SUFFIX )? )
            // ./src/main/java/hero/antlr/Java.g:1078:15: '0' ( 'x' | 'X' ) ( HEX_DIGIT )+ ( INTEGER_TYPE_SUFFIX )?
            {
            match('0'); 

            if ( input.LA(1)=='X'||input.LA(1)=='x' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            // ./src/main/java/hero/antlr/Java.g:1078:29: ( HEX_DIGIT )+
            int cnt1=0;
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( ((LA1_0 >= '0' && LA1_0 <= '9')||(LA1_0 >= 'A' && LA1_0 <= 'F')||(LA1_0 >= 'a' && LA1_0 <= 'f')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // ./src/main/java/hero/antlr/Java.g:
            	    {
            	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'F')||(input.LA(1) >= 'a' && input.LA(1) <= 'f') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt1 >= 1 ) break loop1;
                        EarlyExitException eee =
                            new EarlyExitException(1, input);
                        throw eee;
                }
                cnt1++;
            } while (true);


            // ./src/main/java/hero/antlr/Java.g:1078:40: ( INTEGER_TYPE_SUFFIX )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0=='L'||LA2_0=='l') ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // ./src/main/java/hero/antlr/Java.g:
                    {
                    if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "HEX_LITERAL"

    // $ANTLR start "DECIMAL_LITERAL"
    public final void mDECIMAL_LITERAL() throws RecognitionException {
        try {
            int _type = DECIMAL_LITERAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:1080:17: ( ( '0' | '1' .. '9' ( '0' .. '9' )* ) ( INTEGER_TYPE_SUFFIX )? )
            // ./src/main/java/hero/antlr/Java.g:1080:19: ( '0' | '1' .. '9' ( '0' .. '9' )* ) ( INTEGER_TYPE_SUFFIX )?
            {
            // ./src/main/java/hero/antlr/Java.g:1080:19: ( '0' | '1' .. '9' ( '0' .. '9' )* )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0=='0') ) {
                alt4=1;
            }
            else if ( ((LA4_0 >= '1' && LA4_0 <= '9')) ) {
                alt4=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;

            }
            switch (alt4) {
                case 1 :
                    // ./src/main/java/hero/antlr/Java.g:1080:20: '0'
                    {
                    match('0'); 

                    }
                    break;
                case 2 :
                    // ./src/main/java/hero/antlr/Java.g:1080:26: '1' .. '9' ( '0' .. '9' )*
                    {
                    matchRange('1','9'); 

                    // ./src/main/java/hero/antlr/Java.g:1080:35: ( '0' .. '9' )*
                    loop3:
                    do {
                        int alt3=2;
                        int LA3_0 = input.LA(1);

                        if ( ((LA3_0 >= '0' && LA3_0 <= '9')) ) {
                            alt3=1;
                        }


                        switch (alt3) {
                    	case 1 :
                    	    // ./src/main/java/hero/antlr/Java.g:
                    	    {
                    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop3;
                        }
                    } while (true);


                    }
                    break;

            }


            // ./src/main/java/hero/antlr/Java.g:1080:46: ( INTEGER_TYPE_SUFFIX )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0=='L'||LA5_0=='l') ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // ./src/main/java/hero/antlr/Java.g:
                    {
                    if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DECIMAL_LITERAL"

    // $ANTLR start "OCTAL_LITERAL"
    public final void mOCTAL_LITERAL() throws RecognitionException {
        try {
            int _type = OCTAL_LITERAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:1082:15: ( '0' ( '0' .. '7' )+ ( INTEGER_TYPE_SUFFIX )? )
            // ./src/main/java/hero/antlr/Java.g:1082:17: '0' ( '0' .. '7' )+ ( INTEGER_TYPE_SUFFIX )?
            {
            match('0'); 

            // ./src/main/java/hero/antlr/Java.g:1082:21: ( '0' .. '7' )+
            int cnt6=0;
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( ((LA6_0 >= '0' && LA6_0 <= '7')) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // ./src/main/java/hero/antlr/Java.g:
            	    {
            	    if ( (input.LA(1) >= '0' && input.LA(1) <= '7') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt6 >= 1 ) break loop6;
                        EarlyExitException eee =
                            new EarlyExitException(6, input);
                        throw eee;
                }
                cnt6++;
            } while (true);


            // ./src/main/java/hero/antlr/Java.g:1082:33: ( INTEGER_TYPE_SUFFIX )?
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( (LA7_0=='L'||LA7_0=='l') ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // ./src/main/java/hero/antlr/Java.g:
                    {
                    if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "OCTAL_LITERAL"

    // $ANTLR start "HEX_DIGIT"
    public final void mHEX_DIGIT() throws RecognitionException {
        try {
            // ./src/main/java/hero/antlr/Java.g:1086:11: ( ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' ) )
            // ./src/main/java/hero/antlr/Java.g:
            {
            if ( (input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'F')||(input.LA(1) >= 'a' && input.LA(1) <= 'f') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "HEX_DIGIT"

    // $ANTLR start "INTEGER_TYPE_SUFFIX"
    public final void mINTEGER_TYPE_SUFFIX() throws RecognitionException {
        try {
            // ./src/main/java/hero/antlr/Java.g:1089:21: ( ( 'l' | 'L' ) )
            // ./src/main/java/hero/antlr/Java.g:
            {
            if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "INTEGER_TYPE_SUFFIX"

    // $ANTLR start "FLOATING_POINT_LITERAL"
    public final void mFLOATING_POINT_LITERAL() throws RecognitionException {
        try {
            int _type = FLOATING_POINT_LITERAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:1091:5: ( ( '0' .. '9' )+ ( DOT ( '0' .. '9' )* ( EXPONENT )? ( FLOAT_TYPE_SUFFIX )? | EXPONENT ( FLOAT_TYPE_SUFFIX )? | FLOAT_TYPE_SUFFIX ) | DOT ( '0' .. '9' )+ ( EXPONENT )? ( FLOAT_TYPE_SUFFIX )? )
            int alt17=2;
            int LA17_0 = input.LA(1);

            if ( ((LA17_0 >= '0' && LA17_0 <= '9')) ) {
                alt17=1;
            }
            else if ( (LA17_0=='.') ) {
                alt17=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 17, 0, input);

                throw nvae;

            }
            switch (alt17) {
                case 1 :
                    // ./src/main/java/hero/antlr/Java.g:1091:9: ( '0' .. '9' )+ ( DOT ( '0' .. '9' )* ( EXPONENT )? ( FLOAT_TYPE_SUFFIX )? | EXPONENT ( FLOAT_TYPE_SUFFIX )? | FLOAT_TYPE_SUFFIX )
                    {
                    // ./src/main/java/hero/antlr/Java.g:1091:9: ( '0' .. '9' )+
                    int cnt8=0;
                    loop8:
                    do {
                        int alt8=2;
                        int LA8_0 = input.LA(1);

                        if ( ((LA8_0 >= '0' && LA8_0 <= '9')) ) {
                            alt8=1;
                        }


                        switch (alt8) {
                    	case 1 :
                    	    // ./src/main/java/hero/antlr/Java.g:
                    	    {
                    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt8 >= 1 ) break loop8;
                                EarlyExitException eee =
                                    new EarlyExitException(8, input);
                                throw eee;
                        }
                        cnt8++;
                    } while (true);


                    // ./src/main/java/hero/antlr/Java.g:1092:9: ( DOT ( '0' .. '9' )* ( EXPONENT )? ( FLOAT_TYPE_SUFFIX )? | EXPONENT ( FLOAT_TYPE_SUFFIX )? | FLOAT_TYPE_SUFFIX )
                    int alt13=3;
                    switch ( input.LA(1) ) {
                    case '.':
                        {
                        alt13=1;
                        }
                        break;
                    case 'E':
                    case 'e':
                        {
                        alt13=2;
                        }
                        break;
                    case 'D':
                    case 'F':
                    case 'd':
                    case 'f':
                        {
                        alt13=3;
                        }
                        break;
                    default:
                        NoViableAltException nvae =
                            new NoViableAltException("", 13, 0, input);

                        throw nvae;

                    }

                    switch (alt13) {
                        case 1 :
                            // ./src/main/java/hero/antlr/Java.g:1093:13: DOT ( '0' .. '9' )* ( EXPONENT )? ( FLOAT_TYPE_SUFFIX )?
                            {
                            mDOT(); 


                            // ./src/main/java/hero/antlr/Java.g:1093:17: ( '0' .. '9' )*
                            loop9:
                            do {
                                int alt9=2;
                                int LA9_0 = input.LA(1);

                                if ( ((LA9_0 >= '0' && LA9_0 <= '9')) ) {
                                    alt9=1;
                                }


                                switch (alt9) {
                            	case 1 :
                            	    // ./src/main/java/hero/antlr/Java.g:
                            	    {
                            	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                            	        input.consume();
                            	    }
                            	    else {
                            	        MismatchedSetException mse = new MismatchedSetException(null,input);
                            	        recover(mse);
                            	        throw mse;
                            	    }


                            	    }
                            	    break;

                            	default :
                            	    break loop9;
                                }
                            } while (true);


                            // ./src/main/java/hero/antlr/Java.g:1093:29: ( EXPONENT )?
                            int alt10=2;
                            int LA10_0 = input.LA(1);

                            if ( (LA10_0=='E'||LA10_0=='e') ) {
                                alt10=1;
                            }
                            switch (alt10) {
                                case 1 :
                                    // ./src/main/java/hero/antlr/Java.g:1093:29: EXPONENT
                                    {
                                    mEXPONENT(); 


                                    }
                                    break;

                            }


                            // ./src/main/java/hero/antlr/Java.g:1093:39: ( FLOAT_TYPE_SUFFIX )?
                            int alt11=2;
                            int LA11_0 = input.LA(1);

                            if ( (LA11_0=='D'||LA11_0=='F'||LA11_0=='d'||LA11_0=='f') ) {
                                alt11=1;
                            }
                            switch (alt11) {
                                case 1 :
                                    // ./src/main/java/hero/antlr/Java.g:
                                    {
                                    if ( input.LA(1)=='D'||input.LA(1)=='F'||input.LA(1)=='d'||input.LA(1)=='f' ) {
                                        input.consume();
                                    }
                                    else {
                                        MismatchedSetException mse = new MismatchedSetException(null,input);
                                        recover(mse);
                                        throw mse;
                                    }


                                    }
                                    break;

                            }


                            }
                            break;
                        case 2 :
                            // ./src/main/java/hero/antlr/Java.g:1094:13: EXPONENT ( FLOAT_TYPE_SUFFIX )?
                            {
                            mEXPONENT(); 


                            // ./src/main/java/hero/antlr/Java.g:1094:22: ( FLOAT_TYPE_SUFFIX )?
                            int alt12=2;
                            int LA12_0 = input.LA(1);

                            if ( (LA12_0=='D'||LA12_0=='F'||LA12_0=='d'||LA12_0=='f') ) {
                                alt12=1;
                            }
                            switch (alt12) {
                                case 1 :
                                    // ./src/main/java/hero/antlr/Java.g:
                                    {
                                    if ( input.LA(1)=='D'||input.LA(1)=='F'||input.LA(1)=='d'||input.LA(1)=='f' ) {
                                        input.consume();
                                    }
                                    else {
                                        MismatchedSetException mse = new MismatchedSetException(null,input);
                                        recover(mse);
                                        throw mse;
                                    }


                                    }
                                    break;

                            }


                            }
                            break;
                        case 3 :
                            // ./src/main/java/hero/antlr/Java.g:1095:13: FLOAT_TYPE_SUFFIX
                            {
                            mFLOAT_TYPE_SUFFIX(); 


                            }
                            break;

                    }


                    }
                    break;
                case 2 :
                    // ./src/main/java/hero/antlr/Java.g:1097:9: DOT ( '0' .. '9' )+ ( EXPONENT )? ( FLOAT_TYPE_SUFFIX )?
                    {
                    mDOT(); 


                    // ./src/main/java/hero/antlr/Java.g:1097:13: ( '0' .. '9' )+
                    int cnt14=0;
                    loop14:
                    do {
                        int alt14=2;
                        int LA14_0 = input.LA(1);

                        if ( ((LA14_0 >= '0' && LA14_0 <= '9')) ) {
                            alt14=1;
                        }


                        switch (alt14) {
                    	case 1 :
                    	    // ./src/main/java/hero/antlr/Java.g:
                    	    {
                    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt14 >= 1 ) break loop14;
                                EarlyExitException eee =
                                    new EarlyExitException(14, input);
                                throw eee;
                        }
                        cnt14++;
                    } while (true);


                    // ./src/main/java/hero/antlr/Java.g:1097:25: ( EXPONENT )?
                    int alt15=2;
                    int LA15_0 = input.LA(1);

                    if ( (LA15_0=='E'||LA15_0=='e') ) {
                        alt15=1;
                    }
                    switch (alt15) {
                        case 1 :
                            // ./src/main/java/hero/antlr/Java.g:1097:25: EXPONENT
                            {
                            mEXPONENT(); 


                            }
                            break;

                    }


                    // ./src/main/java/hero/antlr/Java.g:1097:35: ( FLOAT_TYPE_SUFFIX )?
                    int alt16=2;
                    int LA16_0 = input.LA(1);

                    if ( (LA16_0=='D'||LA16_0=='F'||LA16_0=='d'||LA16_0=='f') ) {
                        alt16=1;
                    }
                    switch (alt16) {
                        case 1 :
                            // ./src/main/java/hero/antlr/Java.g:
                            {
                            if ( input.LA(1)=='D'||input.LA(1)=='F'||input.LA(1)=='d'||input.LA(1)=='f' ) {
                                input.consume();
                            }
                            else {
                                MismatchedSetException mse = new MismatchedSetException(null,input);
                                recover(mse);
                                throw mse;
                            }


                            }
                            break;

                    }


                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "FLOATING_POINT_LITERAL"

    // $ANTLR start "EXPONENT"
    public final void mEXPONENT() throws RecognitionException {
        try {
            // ./src/main/java/hero/antlr/Java.g:1102:10: ( ( 'e' | 'E' ) ( '+' | '-' )? ( '0' .. '9' )+ )
            // ./src/main/java/hero/antlr/Java.g:1102:12: ( 'e' | 'E' ) ( '+' | '-' )? ( '0' .. '9' )+
            {
            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            // ./src/main/java/hero/antlr/Java.g:1102:22: ( '+' | '-' )?
            int alt18=2;
            int LA18_0 = input.LA(1);

            if ( (LA18_0=='+'||LA18_0=='-') ) {
                alt18=1;
            }
            switch (alt18) {
                case 1 :
                    // ./src/main/java/hero/antlr/Java.g:
                    {
                    if ( input.LA(1)=='+'||input.LA(1)=='-' ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;

            }


            // ./src/main/java/hero/antlr/Java.g:1102:33: ( '0' .. '9' )+
            int cnt19=0;
            loop19:
            do {
                int alt19=2;
                int LA19_0 = input.LA(1);

                if ( ((LA19_0 >= '0' && LA19_0 <= '9')) ) {
                    alt19=1;
                }


                switch (alt19) {
            	case 1 :
            	    // ./src/main/java/hero/antlr/Java.g:
            	    {
            	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt19 >= 1 ) break loop19;
                        EarlyExitException eee =
                            new EarlyExitException(19, input);
                        throw eee;
                }
                cnt19++;
            } while (true);


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "EXPONENT"

    // $ANTLR start "FLOAT_TYPE_SUFFIX"
    public final void mFLOAT_TYPE_SUFFIX() throws RecognitionException {
        try {
            // ./src/main/java/hero/antlr/Java.g:1105:19: ( ( 'f' | 'F' | 'd' | 'D' ) )
            // ./src/main/java/hero/antlr/Java.g:
            {
            if ( input.LA(1)=='D'||input.LA(1)=='F'||input.LA(1)=='d'||input.LA(1)=='f' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "FLOAT_TYPE_SUFFIX"

    // $ANTLR start "CHARACTER_LITERAL"
    public final void mCHARACTER_LITERAL() throws RecognitionException {
        try {
            int _type = CHARACTER_LITERAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:1107:5: ( '\\'' ( ESCAPE_SEQUENCE |~ ( '\\'' | '\\\\' ) ) '\\'' )
            // ./src/main/java/hero/antlr/Java.g:1107:9: '\\'' ( ESCAPE_SEQUENCE |~ ( '\\'' | '\\\\' ) ) '\\''
            {
            match('\''); 

            // ./src/main/java/hero/antlr/Java.g:1107:14: ( ESCAPE_SEQUENCE |~ ( '\\'' | '\\\\' ) )
            int alt20=2;
            int LA20_0 = input.LA(1);

            if ( (LA20_0=='\\') ) {
                alt20=1;
            }
            else if ( ((LA20_0 >= '\u0000' && LA20_0 <= '&')||(LA20_0 >= '(' && LA20_0 <= '[')||(LA20_0 >= ']' && LA20_0 <= '\uFFFF')) ) {
                alt20=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 20, 0, input);

                throw nvae;

            }
            switch (alt20) {
                case 1 :
                    // ./src/main/java/hero/antlr/Java.g:1107:16: ESCAPE_SEQUENCE
                    {
                    mESCAPE_SEQUENCE(); 


                    }
                    break;
                case 2 :
                    // ./src/main/java/hero/antlr/Java.g:1107:34: ~ ( '\\'' | '\\\\' )
                    {
                    if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '&')||(input.LA(1) >= '(' && input.LA(1) <= '[')||(input.LA(1) >= ']' && input.LA(1) <= '\uFFFF') ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;

            }


            match('\''); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "CHARACTER_LITERAL"

    // $ANTLR start "STRING_LITERAL"
    public final void mSTRING_LITERAL() throws RecognitionException {
        try {
            int _type = STRING_LITERAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:1111:5: ( '\"' ( ESCAPE_SEQUENCE |~ ( '\\\\' | '\"' ) )* '\"' )
            // ./src/main/java/hero/antlr/Java.g:1111:8: '\"' ( ESCAPE_SEQUENCE |~ ( '\\\\' | '\"' ) )* '\"'
            {
            match('\"'); 

            // ./src/main/java/hero/antlr/Java.g:1111:12: ( ESCAPE_SEQUENCE |~ ( '\\\\' | '\"' ) )*
            loop21:
            do {
                int alt21=3;
                int LA21_0 = input.LA(1);

                if ( (LA21_0=='\\') ) {
                    alt21=1;
                }
                else if ( ((LA21_0 >= '\u0000' && LA21_0 <= '!')||(LA21_0 >= '#' && LA21_0 <= '[')||(LA21_0 >= ']' && LA21_0 <= '\uFFFF')) ) {
                    alt21=2;
                }


                switch (alt21) {
            	case 1 :
            	    // ./src/main/java/hero/antlr/Java.g:1111:14: ESCAPE_SEQUENCE
            	    {
            	    mESCAPE_SEQUENCE(); 


            	    }
            	    break;
            	case 2 :
            	    // ./src/main/java/hero/antlr/Java.g:1111:32: ~ ( '\\\\' | '\"' )
            	    {
            	    if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '!')||(input.LA(1) >= '#' && input.LA(1) <= '[')||(input.LA(1) >= ']' && input.LA(1) <= '\uFFFF') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop21;
                }
            } while (true);


            match('\"'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "STRING_LITERAL"

    // $ANTLR start "ESCAPE_SEQUENCE"
    public final void mESCAPE_SEQUENCE() throws RecognitionException {
        try {
            // ./src/main/java/hero/antlr/Java.g:1117:5: ( '\\\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\\\"' | '\\'' | '\\\\' ) | UNICODE_ESCAPE | OCTAL_ESCAPE )
            int alt22=3;
            int LA22_0 = input.LA(1);

            if ( (LA22_0=='\\') ) {
                switch ( input.LA(2) ) {
                case '\"':
                case '\'':
                case '\\':
                case 'b':
                case 'f':
                case 'n':
                case 'r':
                case 't':
                    {
                    alt22=1;
                    }
                    break;
                case 'u':
                    {
                    alt22=2;
                    }
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    {
                    alt22=3;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 22, 1, input);

                    throw nvae;

                }

            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 22, 0, input);

                throw nvae;

            }
            switch (alt22) {
                case 1 :
                    // ./src/main/java/hero/antlr/Java.g:1117:9: '\\\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\\\"' | '\\'' | '\\\\' )
                    {
                    match('\\'); 

                    if ( input.LA(1)=='\"'||input.LA(1)=='\''||input.LA(1)=='\\'||input.LA(1)=='b'||input.LA(1)=='f'||input.LA(1)=='n'||input.LA(1)=='r'||input.LA(1)=='t' ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;
                case 2 :
                    // ./src/main/java/hero/antlr/Java.g:1118:9: UNICODE_ESCAPE
                    {
                    mUNICODE_ESCAPE(); 


                    }
                    break;
                case 3 :
                    // ./src/main/java/hero/antlr/Java.g:1119:9: OCTAL_ESCAPE
                    {
                    mOCTAL_ESCAPE(); 


                    }
                    break;

            }

        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ESCAPE_SEQUENCE"

    // $ANTLR start "OCTAL_ESCAPE"
    public final void mOCTAL_ESCAPE() throws RecognitionException {
        try {
            // ./src/main/java/hero/antlr/Java.g:1124:5: ( '\\\\' ( '0' .. '3' ) ( '0' .. '7' ) ( '0' .. '7' ) | '\\\\' ( '0' .. '7' ) ( '0' .. '7' ) | '\\\\' ( '0' .. '7' ) )
            int alt23=3;
            int LA23_0 = input.LA(1);

            if ( (LA23_0=='\\') ) {
                int LA23_1 = input.LA(2);

                if ( ((LA23_1 >= '0' && LA23_1 <= '3')) ) {
                    int LA23_2 = input.LA(3);

                    if ( ((LA23_2 >= '0' && LA23_2 <= '7')) ) {
                        int LA23_4 = input.LA(4);

                        if ( ((LA23_4 >= '0' && LA23_4 <= '7')) ) {
                            alt23=1;
                        }
                        else {
                            alt23=2;
                        }
                    }
                    else {
                        alt23=3;
                    }
                }
                else if ( ((LA23_1 >= '4' && LA23_1 <= '7')) ) {
                    int LA23_3 = input.LA(3);

                    if ( ((LA23_3 >= '0' && LA23_3 <= '7')) ) {
                        alt23=2;
                    }
                    else {
                        alt23=3;
                    }
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 23, 1, input);

                    throw nvae;

                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 23, 0, input);

                throw nvae;

            }
            switch (alt23) {
                case 1 :
                    // ./src/main/java/hero/antlr/Java.g:1124:9: '\\\\' ( '0' .. '3' ) ( '0' .. '7' ) ( '0' .. '7' )
                    {
                    match('\\'); 

                    if ( (input.LA(1) >= '0' && input.LA(1) <= '3') ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    if ( (input.LA(1) >= '0' && input.LA(1) <= '7') ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    if ( (input.LA(1) >= '0' && input.LA(1) <= '7') ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;
                case 2 :
                    // ./src/main/java/hero/antlr/Java.g:1125:9: '\\\\' ( '0' .. '7' ) ( '0' .. '7' )
                    {
                    match('\\'); 

                    if ( (input.LA(1) >= '0' && input.LA(1) <= '7') ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    if ( (input.LA(1) >= '0' && input.LA(1) <= '7') ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;
                case 3 :
                    // ./src/main/java/hero/antlr/Java.g:1126:9: '\\\\' ( '0' .. '7' )
                    {
                    match('\\'); 

                    if ( (input.LA(1) >= '0' && input.LA(1) <= '7') ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;

            }

        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "OCTAL_ESCAPE"

    // $ANTLR start "UNICODE_ESCAPE"
    public final void mUNICODE_ESCAPE() throws RecognitionException {
        try {
            // ./src/main/java/hero/antlr/Java.g:1131:5: ( '\\\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT )
            // ./src/main/java/hero/antlr/Java.g:1131:9: '\\\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
            {
            match('\\'); 

            match('u'); 

            mHEX_DIGIT(); 


            mHEX_DIGIT(); 


            mHEX_DIGIT(); 


            mHEX_DIGIT(); 


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "UNICODE_ESCAPE"

    // $ANTLR start "IDENT"
    public final void mIDENT() throws RecognitionException {
        try {
            int _type = IDENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:1134:5: ( JAVA_ID_START ( JAVA_ID_PART )* )
            // ./src/main/java/hero/antlr/Java.g:1134:9: JAVA_ID_START ( JAVA_ID_PART )*
            {
            mJAVA_ID_START(); 


            // ./src/main/java/hero/antlr/Java.g:1134:23: ( JAVA_ID_PART )*
            loop24:
            do {
                int alt24=2;
                int LA24_0 = input.LA(1);

                if ( (LA24_0=='$'||(LA24_0 >= '0' && LA24_0 <= '9')||(LA24_0 >= 'A' && LA24_0 <= 'Z')||LA24_0=='_'||(LA24_0 >= 'a' && LA24_0 <= 'z')||(LA24_0 >= '\u00C0' && LA24_0 <= '\u00D6')||(LA24_0 >= '\u00D8' && LA24_0 <= '\u00F6')||(LA24_0 >= '\u00F8' && LA24_0 <= '\u1FFF')||(LA24_0 >= '\u3040' && LA24_0 <= '\u318F')||(LA24_0 >= '\u3300' && LA24_0 <= '\u337F')||(LA24_0 >= '\u3400' && LA24_0 <= '\u3D2D')||(LA24_0 >= '\u4E00' && LA24_0 <= '\u9FFF')||(LA24_0 >= '\uF900' && LA24_0 <= '\uFAFF')) ) {
                    alt24=1;
                }


                switch (alt24) {
            	case 1 :
            	    // ./src/main/java/hero/antlr/Java.g:
            	    {
            	    if ( input.LA(1)=='$'||(input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z')||(input.LA(1) >= '\u00C0' && input.LA(1) <= '\u00D6')||(input.LA(1) >= '\u00D8' && input.LA(1) <= '\u00F6')||(input.LA(1) >= '\u00F8' && input.LA(1) <= '\u1FFF')||(input.LA(1) >= '\u3040' && input.LA(1) <= '\u318F')||(input.LA(1) >= '\u3300' && input.LA(1) <= '\u337F')||(input.LA(1) >= '\u3400' && input.LA(1) <= '\u3D2D')||(input.LA(1) >= '\u4E00' && input.LA(1) <= '\u9FFF')||(input.LA(1) >= '\uF900' && input.LA(1) <= '\uFAFF') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop24;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "IDENT"

    // $ANTLR start "JAVA_ID_START"
    public final void mJAVA_ID_START() throws RecognitionException {
        try {
            // ./src/main/java/hero/antlr/Java.g:1140:5: ( '\\u0024' | '\\u0041' .. '\\u005a' | '\\u005f' | '\\u0061' .. '\\u007a' | '\\u00c0' .. '\\u00d6' | '\\u00d8' .. '\\u00f6' | '\\u00f8' .. '\\u00ff' | '\\u0100' .. '\\u1fff' | '\\u3040' .. '\\u318f' | '\\u3300' .. '\\u337f' | '\\u3400' .. '\\u3d2d' | '\\u4e00' .. '\\u9fff' | '\\uf900' .. '\\ufaff' )
            // ./src/main/java/hero/antlr/Java.g:
            {
            if ( input.LA(1)=='$'||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z')||(input.LA(1) >= '\u00C0' && input.LA(1) <= '\u00D6')||(input.LA(1) >= '\u00D8' && input.LA(1) <= '\u00F6')||(input.LA(1) >= '\u00F8' && input.LA(1) <= '\u1FFF')||(input.LA(1) >= '\u3040' && input.LA(1) <= '\u318F')||(input.LA(1) >= '\u3300' && input.LA(1) <= '\u337F')||(input.LA(1) >= '\u3400' && input.LA(1) <= '\u3D2D')||(input.LA(1) >= '\u4E00' && input.LA(1) <= '\u9FFF')||(input.LA(1) >= '\uF900' && input.LA(1) <= '\uFAFF') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "JAVA_ID_START"

    // $ANTLR start "JAVA_ID_PART"
    public final void mJAVA_ID_PART() throws RecognitionException {
        try {
            // ./src/main/java/hero/antlr/Java.g:1157:5: ( JAVA_ID_START | '\\u0030' .. '\\u0039' )
            // ./src/main/java/hero/antlr/Java.g:
            {
            if ( input.LA(1)=='$'||(input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z')||(input.LA(1) >= '\u00C0' && input.LA(1) <= '\u00D6')||(input.LA(1) >= '\u00D8' && input.LA(1) <= '\u00F6')||(input.LA(1) >= '\u00F8' && input.LA(1) <= '\u1FFF')||(input.LA(1) >= '\u3040' && input.LA(1) <= '\u318F')||(input.LA(1) >= '\u3300' && input.LA(1) <= '\u337F')||(input.LA(1) >= '\u3400' && input.LA(1) <= '\u3D2D')||(input.LA(1) >= '\u4E00' && input.LA(1) <= '\u9FFF')||(input.LA(1) >= '\uF900' && input.LA(1) <= '\uFAFF') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "JAVA_ID_PART"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:1160:5: ( ( ' ' | '\\r' | '\\t' | '\\u000C' | '\\n' ) )
            // ./src/main/java/hero/antlr/Java.g:1160:8: ( ' ' | '\\r' | '\\t' | '\\u000C' | '\\n' )
            {
            if ( (input.LA(1) >= '\t' && input.LA(1) <= '\n')||(input.LA(1) >= '\f' && input.LA(1) <= '\r')||input.LA(1)==' ' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


               
                    if (!preserveWhitespacesAndComments) {
                        skip();
                    } else {
                        _channel = HIDDEN;
                    }
                

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "WS"

    // $ANTLR start "COMMENT"
    public final void mCOMMENT() throws RecognitionException {
        try {
            int _type = COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:1171:5: ( '/*' ( options {greedy=false; } : . )* '*/' )
            // ./src/main/java/hero/antlr/Java.g:1171:9: '/*' ( options {greedy=false; } : . )* '*/'
            {
            match("/*"); 



            // ./src/main/java/hero/antlr/Java.g:1171:14: ( options {greedy=false; } : . )*
            loop25:
            do {
                int alt25=2;
                int LA25_0 = input.LA(1);

                if ( (LA25_0=='*') ) {
                    int LA25_1 = input.LA(2);

                    if ( (LA25_1=='/') ) {
                        alt25=2;
                    }
                    else if ( ((LA25_1 >= '\u0000' && LA25_1 <= '.')||(LA25_1 >= '0' && LA25_1 <= '\uFFFF')) ) {
                        alt25=1;
                    }


                }
                else if ( ((LA25_0 >= '\u0000' && LA25_0 <= ')')||(LA25_0 >= '+' && LA25_0 <= '\uFFFF')) ) {
                    alt25=1;
                }


                switch (alt25) {
            	case 1 :
            	    // ./src/main/java/hero/antlr/Java.g:1171:42: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop25;
                }
            } while (true);


            match("*/"); 



               
                    if (!preserveWhitespacesAndComments) {
                        skip();
                    } else {
                        _channel = HIDDEN;
                    }
                

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "COMMENT"

    // $ANTLR start "LINE_COMMENT"
    public final void mLINE_COMMENT() throws RecognitionException {
        try {
            int _type = LINE_COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // ./src/main/java/hero/antlr/Java.g:1182:5: ( '//' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n' )
            // ./src/main/java/hero/antlr/Java.g:1182:7: '//' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n'
            {
            match("//"); 



            // ./src/main/java/hero/antlr/Java.g:1182:12: (~ ( '\\n' | '\\r' ) )*
            loop26:
            do {
                int alt26=2;
                int LA26_0 = input.LA(1);

                if ( ((LA26_0 >= '\u0000' && LA26_0 <= '\t')||(LA26_0 >= '\u000B' && LA26_0 <= '\f')||(LA26_0 >= '\u000E' && LA26_0 <= '\uFFFF')) ) {
                    alt26=1;
                }


                switch (alt26) {
            	case 1 :
            	    // ./src/main/java/hero/antlr/Java.g:
            	    {
            	    if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '\t')||(input.LA(1) >= '\u000B' && input.LA(1) <= '\f')||(input.LA(1) >= '\u000E' && input.LA(1) <= '\uFFFF') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop26;
                }
            } while (true);


            // ./src/main/java/hero/antlr/Java.g:1182:26: ( '\\r' )?
            int alt27=2;
            int LA27_0 = input.LA(1);

            if ( (LA27_0=='\r') ) {
                alt27=1;
            }
            switch (alt27) {
                case 1 :
                    // ./src/main/java/hero/antlr/Java.g:1182:26: '\\r'
                    {
                    match('\r'); 

                    }
                    break;

            }


            match('\n'); 

               
                    if (!preserveWhitespacesAndComments) {
                        skip();
                    } else {
                        _channel = HIDDEN;
                    }
                

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LINE_COMMENT"

    public void mTokens() throws RecognitionException {
        // ./src/main/java/hero/antlr/Java.g:1:8: ( ABSTRACT | AND | AND_ASSIGN | ASSERT | ASSIGN | AT | BIT_SHIFT_RIGHT | BIT_SHIFT_RIGHT_ASSIGN | BOOLEAN | BREAK | BYTE | CASE | CATCH | CHAR | CLASS | COLON | COMMA | CONTINUE | DEC | DEFAULT | DIV | DIV_ASSIGN | DO | DOT | DOTSTAR | DOUBLE | ELLIPSIS | ELSE | ENUM | EQUAL | EXTENDS | FALSE | FINAL | FINALLY | FLOAT | FOR | GREATER_OR_EQUAL | GREATER_THAN | IF | IMPLEMENTS | IMPORT | INC | INSTANCEOF | INT | INTERFACE | LBRACK | LCURLY | LESS_OR_EQUAL | LESS_THAN | LOGICAL_AND | LOGICAL_NOT | LOGICAL_OR | LONG | LPAREN | MINUS | MINUS_ASSIGN | MOD | MOD_ASSIGN | NATIVE | NEW | NOT | NOT_EQUAL | NULL | OR | OR_ASSIGN | PACKAGE | PLUS | PLUS_ASSIGN | PRIVATE | PROTECTED | PUBLIC | QUESTION | RBRACK | RCURLY | RETURN | RPAREN | SEMI | SHIFT_LEFT | SHIFT_LEFT_ASSIGN | SHIFT_RIGHT | SHIFT_RIGHT_ASSIGN | SHORT | STAR | STAR_ASSIGN | STATIC | STRICTFP | SUPER | SWITCH | SYNCHRONIZED | THIS | THROW | THROWS | TRANSIENT | TRUE | TRY | VOID | VOLATILE | WHILE | XOR | XOR_ASSIGN | HEX_LITERAL | DECIMAL_LITERAL | OCTAL_LITERAL | FLOATING_POINT_LITERAL | CHARACTER_LITERAL | STRING_LITERAL | IDENT | WS | COMMENT | LINE_COMMENT )
        int alt28=110;
        alt28 = dfa28.predict(input);
        switch (alt28) {
            case 1 :
                // ./src/main/java/hero/antlr/Java.g:1:10: ABSTRACT
                {
                mABSTRACT(); 


                }
                break;
            case 2 :
                // ./src/main/java/hero/antlr/Java.g:1:19: AND
                {
                mAND(); 


                }
                break;
            case 3 :
                // ./src/main/java/hero/antlr/Java.g:1:23: AND_ASSIGN
                {
                mAND_ASSIGN(); 


                }
                break;
            case 4 :
                // ./src/main/java/hero/antlr/Java.g:1:34: ASSERT
                {
                mASSERT(); 


                }
                break;
            case 5 :
                // ./src/main/java/hero/antlr/Java.g:1:41: ASSIGN
                {
                mASSIGN(); 


                }
                break;
            case 6 :
                // ./src/main/java/hero/antlr/Java.g:1:48: AT
                {
                mAT(); 


                }
                break;
            case 7 :
                // ./src/main/java/hero/antlr/Java.g:1:51: BIT_SHIFT_RIGHT
                {
                mBIT_SHIFT_RIGHT(); 


                }
                break;
            case 8 :
                // ./src/main/java/hero/antlr/Java.g:1:67: BIT_SHIFT_RIGHT_ASSIGN
                {
                mBIT_SHIFT_RIGHT_ASSIGN(); 


                }
                break;
            case 9 :
                // ./src/main/java/hero/antlr/Java.g:1:90: BOOLEAN
                {
                mBOOLEAN(); 


                }
                break;
            case 10 :
                // ./src/main/java/hero/antlr/Java.g:1:98: BREAK
                {
                mBREAK(); 


                }
                break;
            case 11 :
                // ./src/main/java/hero/antlr/Java.g:1:104: BYTE
                {
                mBYTE(); 


                }
                break;
            case 12 :
                // ./src/main/java/hero/antlr/Java.g:1:109: CASE
                {
                mCASE(); 


                }
                break;
            case 13 :
                // ./src/main/java/hero/antlr/Java.g:1:114: CATCH
                {
                mCATCH(); 


                }
                break;
            case 14 :
                // ./src/main/java/hero/antlr/Java.g:1:120: CHAR
                {
                mCHAR(); 


                }
                break;
            case 15 :
                // ./src/main/java/hero/antlr/Java.g:1:125: CLASS
                {
                mCLASS(); 


                }
                break;
            case 16 :
                // ./src/main/java/hero/antlr/Java.g:1:131: COLON
                {
                mCOLON(); 


                }
                break;
            case 17 :
                // ./src/main/java/hero/antlr/Java.g:1:137: COMMA
                {
                mCOMMA(); 


                }
                break;
            case 18 :
                // ./src/main/java/hero/antlr/Java.g:1:143: CONTINUE
                {
                mCONTINUE(); 


                }
                break;
            case 19 :
                // ./src/main/java/hero/antlr/Java.g:1:152: DEC
                {
                mDEC(); 


                }
                break;
            case 20 :
                // ./src/main/java/hero/antlr/Java.g:1:156: DEFAULT
                {
                mDEFAULT(); 


                }
                break;
            case 21 :
                // ./src/main/java/hero/antlr/Java.g:1:164: DIV
                {
                mDIV(); 


                }
                break;
            case 22 :
                // ./src/main/java/hero/antlr/Java.g:1:168: DIV_ASSIGN
                {
                mDIV_ASSIGN(); 


                }
                break;
            case 23 :
                // ./src/main/java/hero/antlr/Java.g:1:179: DO
                {
                mDO(); 


                }
                break;
            case 24 :
                // ./src/main/java/hero/antlr/Java.g:1:182: DOT
                {
                mDOT(); 


                }
                break;
            case 25 :
                // ./src/main/java/hero/antlr/Java.g:1:186: DOTSTAR
                {
                mDOTSTAR(); 


                }
                break;
            case 26 :
                // ./src/main/java/hero/antlr/Java.g:1:194: DOUBLE
                {
                mDOUBLE(); 


                }
                break;
            case 27 :
                // ./src/main/java/hero/antlr/Java.g:1:201: ELLIPSIS
                {
                mELLIPSIS(); 


                }
                break;
            case 28 :
                // ./src/main/java/hero/antlr/Java.g:1:210: ELSE
                {
                mELSE(); 


                }
                break;
            case 29 :
                // ./src/main/java/hero/antlr/Java.g:1:215: ENUM
                {
                mENUM(); 


                }
                break;
            case 30 :
                // ./src/main/java/hero/antlr/Java.g:1:220: EQUAL
                {
                mEQUAL(); 


                }
                break;
            case 31 :
                // ./src/main/java/hero/antlr/Java.g:1:226: EXTENDS
                {
                mEXTENDS(); 


                }
                break;
            case 32 :
                // ./src/main/java/hero/antlr/Java.g:1:234: FALSE
                {
                mFALSE(); 


                }
                break;
            case 33 :
                // ./src/main/java/hero/antlr/Java.g:1:240: FINAL
                {
                mFINAL(); 


                }
                break;
            case 34 :
                // ./src/main/java/hero/antlr/Java.g:1:246: FINALLY
                {
                mFINALLY(); 


                }
                break;
            case 35 :
                // ./src/main/java/hero/antlr/Java.g:1:254: FLOAT
                {
                mFLOAT(); 


                }
                break;
            case 36 :
                // ./src/main/java/hero/antlr/Java.g:1:260: FOR
                {
                mFOR(); 


                }
                break;
            case 37 :
                // ./src/main/java/hero/antlr/Java.g:1:264: GREATER_OR_EQUAL
                {
                mGREATER_OR_EQUAL(); 


                }
                break;
            case 38 :
                // ./src/main/java/hero/antlr/Java.g:1:281: GREATER_THAN
                {
                mGREATER_THAN(); 


                }
                break;
            case 39 :
                // ./src/main/java/hero/antlr/Java.g:1:294: IF
                {
                mIF(); 


                }
                break;
            case 40 :
                // ./src/main/java/hero/antlr/Java.g:1:297: IMPLEMENTS
                {
                mIMPLEMENTS(); 


                }
                break;
            case 41 :
                // ./src/main/java/hero/antlr/Java.g:1:308: IMPORT
                {
                mIMPORT(); 


                }
                break;
            case 42 :
                // ./src/main/java/hero/antlr/Java.g:1:315: INC
                {
                mINC(); 


                }
                break;
            case 43 :
                // ./src/main/java/hero/antlr/Java.g:1:319: INSTANCEOF
                {
                mINSTANCEOF(); 


                }
                break;
            case 44 :
                // ./src/main/java/hero/antlr/Java.g:1:330: INT
                {
                mINT(); 


                }
                break;
            case 45 :
                // ./src/main/java/hero/antlr/Java.g:1:334: INTERFACE
                {
                mINTERFACE(); 


                }
                break;
            case 46 :
                // ./src/main/java/hero/antlr/Java.g:1:344: LBRACK
                {
                mLBRACK(); 


                }
                break;
            case 47 :
                // ./src/main/java/hero/antlr/Java.g:1:351: LCURLY
                {
                mLCURLY(); 


                }
                break;
            case 48 :
                // ./src/main/java/hero/antlr/Java.g:1:358: LESS_OR_EQUAL
                {
                mLESS_OR_EQUAL(); 


                }
                break;
            case 49 :
                // ./src/main/java/hero/antlr/Java.g:1:372: LESS_THAN
                {
                mLESS_THAN(); 


                }
                break;
            case 50 :
                // ./src/main/java/hero/antlr/Java.g:1:382: LOGICAL_AND
                {
                mLOGICAL_AND(); 


                }
                break;
            case 51 :
                // ./src/main/java/hero/antlr/Java.g:1:394: LOGICAL_NOT
                {
                mLOGICAL_NOT(); 


                }
                break;
            case 52 :
                // ./src/main/java/hero/antlr/Java.g:1:406: LOGICAL_OR
                {
                mLOGICAL_OR(); 


                }
                break;
            case 53 :
                // ./src/main/java/hero/antlr/Java.g:1:417: LONG
                {
                mLONG(); 


                }
                break;
            case 54 :
                // ./src/main/java/hero/antlr/Java.g:1:422: LPAREN
                {
                mLPAREN(); 


                }
                break;
            case 55 :
                // ./src/main/java/hero/antlr/Java.g:1:429: MINUS
                {
                mMINUS(); 


                }
                break;
            case 56 :
                // ./src/main/java/hero/antlr/Java.g:1:435: MINUS_ASSIGN
                {
                mMINUS_ASSIGN(); 


                }
                break;
            case 57 :
                // ./src/main/java/hero/antlr/Java.g:1:448: MOD
                {
                mMOD(); 


                }
                break;
            case 58 :
                // ./src/main/java/hero/antlr/Java.g:1:452: MOD_ASSIGN
                {
                mMOD_ASSIGN(); 


                }
                break;
            case 59 :
                // ./src/main/java/hero/antlr/Java.g:1:463: NATIVE
                {
                mNATIVE(); 


                }
                break;
            case 60 :
                // ./src/main/java/hero/antlr/Java.g:1:470: NEW
                {
                mNEW(); 


                }
                break;
            case 61 :
                // ./src/main/java/hero/antlr/Java.g:1:474: NOT
                {
                mNOT(); 


                }
                break;
            case 62 :
                // ./src/main/java/hero/antlr/Java.g:1:478: NOT_EQUAL
                {
                mNOT_EQUAL(); 


                }
                break;
            case 63 :
                // ./src/main/java/hero/antlr/Java.g:1:488: NULL
                {
                mNULL(); 


                }
                break;
            case 64 :
                // ./src/main/java/hero/antlr/Java.g:1:493: OR
                {
                mOR(); 


                }
                break;
            case 65 :
                // ./src/main/java/hero/antlr/Java.g:1:496: OR_ASSIGN
                {
                mOR_ASSIGN(); 


                }
                break;
            case 66 :
                // ./src/main/java/hero/antlr/Java.g:1:506: PACKAGE
                {
                mPACKAGE(); 


                }
                break;
            case 67 :
                // ./src/main/java/hero/antlr/Java.g:1:514: PLUS
                {
                mPLUS(); 


                }
                break;
            case 68 :
                // ./src/main/java/hero/antlr/Java.g:1:519: PLUS_ASSIGN
                {
                mPLUS_ASSIGN(); 


                }
                break;
            case 69 :
                // ./src/main/java/hero/antlr/Java.g:1:531: PRIVATE
                {
                mPRIVATE(); 


                }
                break;
            case 70 :
                // ./src/main/java/hero/antlr/Java.g:1:539: PROTECTED
                {
                mPROTECTED(); 


                }
                break;
            case 71 :
                // ./src/main/java/hero/antlr/Java.g:1:549: PUBLIC
                {
                mPUBLIC(); 


                }
                break;
            case 72 :
                // ./src/main/java/hero/antlr/Java.g:1:556: QUESTION
                {
                mQUESTION(); 


                }
                break;
            case 73 :
                // ./src/main/java/hero/antlr/Java.g:1:565: RBRACK
                {
                mRBRACK(); 


                }
                break;
            case 74 :
                // ./src/main/java/hero/antlr/Java.g:1:572: RCURLY
                {
                mRCURLY(); 


                }
                break;
            case 75 :
                // ./src/main/java/hero/antlr/Java.g:1:579: RETURN
                {
                mRETURN(); 


                }
                break;
            case 76 :
                // ./src/main/java/hero/antlr/Java.g:1:586: RPAREN
                {
                mRPAREN(); 


                }
                break;
            case 77 :
                // ./src/main/java/hero/antlr/Java.g:1:593: SEMI
                {
                mSEMI(); 


                }
                break;
            case 78 :
                // ./src/main/java/hero/antlr/Java.g:1:598: SHIFT_LEFT
                {
                mSHIFT_LEFT(); 


                }
                break;
            case 79 :
                // ./src/main/java/hero/antlr/Java.g:1:609: SHIFT_LEFT_ASSIGN
                {
                mSHIFT_LEFT_ASSIGN(); 


                }
                break;
            case 80 :
                // ./src/main/java/hero/antlr/Java.g:1:627: SHIFT_RIGHT
                {
                mSHIFT_RIGHT(); 


                }
                break;
            case 81 :
                // ./src/main/java/hero/antlr/Java.g:1:639: SHIFT_RIGHT_ASSIGN
                {
                mSHIFT_RIGHT_ASSIGN(); 


                }
                break;
            case 82 :
                // ./src/main/java/hero/antlr/Java.g:1:658: SHORT
                {
                mSHORT(); 


                }
                break;
            case 83 :
                // ./src/main/java/hero/antlr/Java.g:1:664: STAR
                {
                mSTAR(); 


                }
                break;
            case 84 :
                // ./src/main/java/hero/antlr/Java.g:1:669: STAR_ASSIGN
                {
                mSTAR_ASSIGN(); 


                }
                break;
            case 85 :
                // ./src/main/java/hero/antlr/Java.g:1:681: STATIC
                {
                mSTATIC(); 


                }
                break;
            case 86 :
                // ./src/main/java/hero/antlr/Java.g:1:688: STRICTFP
                {
                mSTRICTFP(); 


                }
                break;
            case 87 :
                // ./src/main/java/hero/antlr/Java.g:1:697: SUPER
                {
                mSUPER(); 


                }
                break;
            case 88 :
                // ./src/main/java/hero/antlr/Java.g:1:703: SWITCH
                {
                mSWITCH(); 


                }
                break;
            case 89 :
                // ./src/main/java/hero/antlr/Java.g:1:710: SYNCHRONIZED
                {
                mSYNCHRONIZED(); 


                }
                break;
            case 90 :
                // ./src/main/java/hero/antlr/Java.g:1:723: THIS
                {
                mTHIS(); 


                }
                break;
            case 91 :
                // ./src/main/java/hero/antlr/Java.g:1:728: THROW
                {
                mTHROW(); 


                }
                break;
            case 92 :
                // ./src/main/java/hero/antlr/Java.g:1:734: THROWS
                {
                mTHROWS(); 


                }
                break;
            case 93 :
                // ./src/main/java/hero/antlr/Java.g:1:741: TRANSIENT
                {
                mTRANSIENT(); 


                }
                break;
            case 94 :
                // ./src/main/java/hero/antlr/Java.g:1:751: TRUE
                {
                mTRUE(); 


                }
                break;
            case 95 :
                // ./src/main/java/hero/antlr/Java.g:1:756: TRY
                {
                mTRY(); 


                }
                break;
            case 96 :
                // ./src/main/java/hero/antlr/Java.g:1:760: VOID
                {
                mVOID(); 


                }
                break;
            case 97 :
                // ./src/main/java/hero/antlr/Java.g:1:765: VOLATILE
                {
                mVOLATILE(); 


                }
                break;
            case 98 :
                // ./src/main/java/hero/antlr/Java.g:1:774: WHILE
                {
                mWHILE(); 


                }
                break;
            case 99 :
                // ./src/main/java/hero/antlr/Java.g:1:780: XOR
                {
                mXOR(); 


                }
                break;
            case 100 :
                // ./src/main/java/hero/antlr/Java.g:1:784: XOR_ASSIGN
                {
                mXOR_ASSIGN(); 


                }
                break;
            case 101 :
                // ./src/main/java/hero/antlr/Java.g:1:795: HEX_LITERAL
                {
                mHEX_LITERAL(); 


                }
                break;
            case 102 :
                // ./src/main/java/hero/antlr/Java.g:1:807: DECIMAL_LITERAL
                {
                mDECIMAL_LITERAL(); 


                }
                break;
            case 103 :
                // ./src/main/java/hero/antlr/Java.g:1:823: OCTAL_LITERAL
                {
                mOCTAL_LITERAL(); 


                }
                break;
            case 104 :
                // ./src/main/java/hero/antlr/Java.g:1:837: FLOATING_POINT_LITERAL
                {
                mFLOATING_POINT_LITERAL(); 


                }
                break;
            case 105 :
                // ./src/main/java/hero/antlr/Java.g:1:860: CHARACTER_LITERAL
                {
                mCHARACTER_LITERAL(); 


                }
                break;
            case 106 :
                // ./src/main/java/hero/antlr/Java.g:1:878: STRING_LITERAL
                {
                mSTRING_LITERAL(); 


                }
                break;
            case 107 :
                // ./src/main/java/hero/antlr/Java.g:1:893: IDENT
                {
                mIDENT(); 


                }
                break;
            case 108 :
                // ./src/main/java/hero/antlr/Java.g:1:899: WS
                {
                mWS(); 


                }
                break;
            case 109 :
                // ./src/main/java/hero/antlr/Java.g:1:902: COMMENT
                {
                mCOMMENT(); 


                }
                break;
            case 110 :
                // ./src/main/java/hero/antlr/Java.g:1:910: LINE_COMMENT
                {
                mLINE_COMMENT(); 


                }
                break;

        }

    }


    protected DFA28 dfa28 = new DFA28(this);
    static final String DFA28_eotS =
        "\1\uffff\1\55\1\63\1\65\1\uffff\1\70\2\55\2\uffff\1\102\1\55\1\110"+
        "\1\113\3\55\1\131\2\uffff\1\134\1\136\1\141\1\55\1\uffff\1\144\1"+
        "\55\1\uffff\1\55\3\uffff\1\55\2\uffff\1\55\1\162\3\55\1\170\2\172"+
        "\4\uffff\2\55\5\uffff\1\u0081\2\uffff\7\55\3\uffff\1\55\1\u008c"+
        "\10\uffff\7\55\1\u0094\2\55\4\uffff\1\u0099\6\uffff\1\55\2\uffff"+
        "\14\55\2\uffff\4\55\4\uffff\1\u00b1\1\172\2\55\1\u00b5\2\uffff\12"+
        "\55\1\uffff\6\55\1\u00c6\1\uffff\2\55\1\u00cb\2\uffff\2\55\1\u00ce"+
        "\20\55\1\u00df\3\55\1\uffff\2\55\2\uffff\2\55\1\u00e7\1\u00e8\1"+
        "\55\1\u00ea\4\55\1\u00ef\1\u00f0\4\55\1\uffff\4\55\1\uffff\1\u00f9"+
        "\1\55\1\uffff\1\u00fb\13\55\1\u0107\2\55\1\u010a\1\uffff\1\u010b"+
        "\5\55\1\u0111\2\uffff\1\u0112\1\uffff\1\u0113\3\55\2\uffff\1\55"+
        "\1\u0118\1\u011a\1\u011b\4\55\1\uffff\1\55\1\uffff\5\55\1\u0126"+
        "\2\55\1\u0129\2\55\1\uffff\1\u012d\1\55\2\uffff\1\55\1\u0130\1\55"+
        "\1\u0132\1\55\3\uffff\2\55\1\u0136\1\55\1\uffff\1\55\2\uffff\1\55"+
        "\1\u013a\2\55\1\u013d\3\55\1\u0141\1\u0142\1\uffff\1\u0143\1\55"+
        "\1\uffff\1\u0145\1\55\1\u0147\1\uffff\2\55\1\uffff\1\55\1\uffff"+
        "\1\u014b\1\55\1\u014d\1\uffff\1\u014e\1\u014f\1\55\1\uffff\2\55"+
        "\1\uffff\1\u0153\1\u0154\1\55\3\uffff\1\55\1\uffff\1\55\1\uffff"+
        "\2\55\1\u015a\1\uffff\1\u015b\3\uffff\3\55\2\uffff\1\55\1\u0160"+
        "\2\55\1\u0163\2\uffff\2\55\1\u0166\1\u0167\1\uffff\1\55\1\u0169"+
        "\1\uffff\1\u016a\1\u016b\2\uffff\1\55\3\uffff\1\55\1\u016e\1\uffff";
    static final String DFA28_eofS =
        "\u016f\uffff";
    static final String DFA28_minS =
        "\1\11\1\142\1\46\1\75\1\uffff\1\75\1\157\1\141\2\uffff\1\55\1\145"+
        "\2\52\1\154\1\141\1\146\1\53\2\uffff\1\74\2\75\1\157\1\uffff\1\75"+
        "\1\141\1\uffff\1\141\3\uffff\1\145\2\uffff\1\150\1\75\1\150\1\157"+
        "\1\150\1\75\2\56\4\uffff\2\163\5\uffff\1\75\2\uffff\1\157\1\145"+
        "\1\164\1\163\2\141\1\156\3\uffff\1\146\1\44\10\uffff\1\163\1\165"+
        "\1\164\1\154\1\156\1\157\1\162\1\44\1\160\1\163\4\uffff\1\75\6\uffff"+
        "\1\156\2\uffff\1\164\1\167\1\154\1\143\1\151\1\142\1\164\1\157\1"+
        "\141\1\160\1\151\1\156\2\uffff\1\151\1\141\2\151\4\uffff\2\56\1"+
        "\164\1\145\1\75\2\uffff\1\154\1\141\2\145\1\143\1\162\1\163\1\164"+
        "\1\141\1\142\1\uffff\1\145\1\155\1\145\1\163\2\141\1\44\1\uffff"+
        "\1\154\1\164\1\44\2\uffff\1\147\1\151\1\44\1\154\1\153\1\166\1\164"+
        "\1\154\1\165\1\162\1\164\1\151\1\145\1\164\1\143\1\163\1\157\1\156"+
        "\1\145\1\44\1\144\1\141\1\154\1\uffff\2\162\2\uffff\1\145\1\153"+
        "\2\44\1\150\1\44\1\163\1\151\1\165\1\154\2\44\1\156\1\145\1\154"+
        "\1\164\1\uffff\1\145\1\162\1\141\1\162\1\uffff\1\44\1\166\1\uffff"+
        "\1\44\2\141\1\145\1\151\1\162\1\164\1\151\1\143\1\162\1\143\1\150"+
        "\1\44\1\167\1\163\1\44\1\uffff\1\44\1\164\1\145\1\141\1\164\1\141"+
        "\1\44\2\uffff\1\44\1\uffff\1\44\1\156\1\154\1\145\2\uffff\1\144"+
        "\3\44\1\155\1\164\1\156\1\146\1\uffff\1\145\1\uffff\1\147\1\164"+
        "\2\143\1\156\1\44\1\143\1\164\1\44\1\150\1\162\1\uffff\1\44\1\151"+
        "\2\uffff\1\151\1\44\1\143\1\44\1\156\3\uffff\1\165\1\164\1\44\1"+
        "\163\1\uffff\1\171\2\uffff\1\145\1\44\1\143\1\141\1\44\2\145\1\164"+
        "\2\44\1\uffff\1\44\1\146\1\uffff\1\44\1\157\1\44\1\uffff\1\145\1"+
        "\154\1\uffff\1\164\1\uffff\1\44\1\145\1\44\1\uffff\2\44\1\156\1"+
        "\uffff\1\145\1\143\1\uffff\2\44\1\145\3\uffff\1\160\1\uffff\1\156"+
        "\1\uffff\1\156\1\145\1\44\1\uffff\1\44\3\uffff\1\164\1\157\1\145"+
        "\2\uffff\1\144\1\44\1\151\1\164\1\44\2\uffff\1\163\1\146\2\44\1"+
        "\uffff\1\172\1\44\1\uffff\2\44\2\uffff\1\145\3\uffff\1\144\1\44"+
        "\1\uffff";
    static final String DFA28_maxS =
        "\1\ufaff\1\163\2\75\1\uffff\1\76\1\171\1\157\2\uffff\1\75\1\157"+
        "\1\75\1\71\1\170\1\157\1\156\1\75\2\uffff\2\75\1\174\1\157\1\uffff"+
        "\1\75\1\165\1\uffff\1\165\3\uffff\1\145\2\uffff\1\171\1\75\1\162"+
        "\1\157\1\150\1\75\1\170\1\146\4\uffff\2\163\5\uffff\1\76\2\uffff"+
        "\1\157\1\145\2\164\2\141\1\156\3\uffff\1\146\1\ufaff\10\uffff\1"+
        "\163\1\165\1\164\1\154\1\156\1\157\1\162\1\ufaff\1\160\1\164\4\uffff"+
        "\1\75\6\uffff\1\156\2\uffff\1\164\1\167\1\154\1\143\1\157\1\142"+
        "\1\164\1\157\1\162\1\160\1\151\1\156\2\uffff\1\162\1\171\1\154\1"+
        "\151\4\uffff\2\146\1\164\1\145\1\75\2\uffff\1\154\1\141\2\145\1"+
        "\143\1\162\1\163\1\164\1\141\1\142\1\uffff\1\145\1\155\1\145\1\163"+
        "\2\141\1\ufaff\1\uffff\1\157\1\164\1\ufaff\2\uffff\1\147\1\151\1"+
        "\ufaff\1\154\1\153\1\166\1\164\1\154\1\165\1\162\1\164\1\151\1\145"+
        "\1\164\1\143\1\163\1\157\1\156\1\145\1\ufaff\1\144\1\141\1\154\1"+
        "\uffff\2\162\2\uffff\1\145\1\153\2\ufaff\1\150\1\ufaff\1\163\1\151"+
        "\1\165\1\154\2\ufaff\1\156\1\145\1\154\1\164\1\uffff\1\145\1\162"+
        "\1\141\1\162\1\uffff\1\ufaff\1\166\1\uffff\1\ufaff\2\141\1\145\1"+
        "\151\1\162\1\164\1\151\1\143\1\162\1\143\1\150\1\ufaff\1\167\1\163"+
        "\1\ufaff\1\uffff\1\ufaff\1\164\1\145\1\141\1\164\1\141\1\ufaff\2"+
        "\uffff\1\ufaff\1\uffff\1\ufaff\1\156\1\154\1\145\2\uffff\1\144\3"+
        "\ufaff\1\155\1\164\1\156\1\146\1\uffff\1\145\1\uffff\1\147\1\164"+
        "\2\143\1\156\1\ufaff\1\143\1\164\1\ufaff\1\150\1\162\1\uffff\1\ufaff"+
        "\1\151\2\uffff\1\151\1\ufaff\1\143\1\ufaff\1\156\3\uffff\1\165\1"+
        "\164\1\ufaff\1\163\1\uffff\1\171\2\uffff\1\145\1\ufaff\1\143\1\141"+
        "\1\ufaff\2\145\1\164\2\ufaff\1\uffff\1\ufaff\1\146\1\uffff\1\ufaff"+
        "\1\157\1\ufaff\1\uffff\1\145\1\154\1\uffff\1\164\1\uffff\1\ufaff"+
        "\1\145\1\ufaff\1\uffff\2\ufaff\1\156\1\uffff\1\145\1\143\1\uffff"+
        "\2\ufaff\1\145\3\uffff\1\160\1\uffff\1\156\1\uffff\1\156\1\145\1"+
        "\ufaff\1\uffff\1\ufaff\3\uffff\1\164\1\157\1\145\2\uffff\1\144\1"+
        "\ufaff\1\151\1\164\1\ufaff\2\uffff\1\163\1\146\2\ufaff\1\uffff\1"+
        "\172\1\ufaff\1\uffff\2\ufaff\2\uffff\1\145\3\uffff\1\144\1\ufaff"+
        "\1\uffff";
    static final String DFA28_acceptS =
        "\4\uffff\1\6\3\uffff\1\20\1\21\10\uffff\1\56\1\57\4\uffff\1\66\2"+
        "\uffff\1\75\1\uffff\1\110\1\111\1\112\1\uffff\1\114\1\115\10\uffff"+
        "\1\151\1\152\1\153\1\154\2\uffff\1\3\1\62\1\2\1\36\1\5\1\uffff\1"+
        "\45\1\46\7\uffff\1\23\1\70\1\67\2\uffff\1\26\1\155\1\156\1\25\1"+
        "\31\1\33\1\30\1\150\12\uffff\1\52\1\104\1\103\1\60\1\uffff\1\61"+
        "\1\76\1\63\1\64\1\101\1\100\1\uffff\1\72\1\71\14\uffff\1\124\1\123"+
        "\4\uffff\1\144\1\143\1\145\1\146\5\uffff\1\121\1\120\12\uffff\1"+
        "\27\7\uffff\1\47\3\uffff\1\117\1\116\27\uffff\1\147\2\uffff\1\10"+
        "\1\7\20\uffff\1\44\4\uffff\1\54\2\uffff\1\74\20\uffff\1\137\7\uffff"+
        "\1\13\1\14\1\uffff\1\16\4\uffff\1\34\1\35\10\uffff\1\65\1\uffff"+
        "\1\77\13\uffff\1\132\2\uffff\1\136\1\140\5\uffff\1\12\1\15\1\17"+
        "\4\uffff\1\40\1\uffff\1\41\1\43\12\uffff\1\122\2\uffff\1\127\3\uffff"+
        "\1\133\2\uffff\1\142\1\uffff\1\4\3\uffff\1\32\3\uffff\1\51\2\uffff"+
        "\1\73\3\uffff\1\107\1\113\1\125\1\uffff\1\130\1\uffff\1\134\3\uffff"+
        "\1\11\1\uffff\1\24\1\37\1\42\3\uffff\1\102\1\105\5\uffff\1\1\1\22"+
        "\4\uffff\1\126\2\uffff\1\141\2\uffff\1\55\1\106\1\uffff\1\135\1"+
        "\50\1\53\2\uffff\1\131";
    static final String DFA28_specialS =
        "\u016f\uffff}>";
    static final String[] DFA28_transitionS = {
            "\2\56\1\uffff\2\56\22\uffff\1\56\1\25\1\54\1\uffff\1\55\1\31"+
            "\1\2\1\53\1\30\1\41\1\44\1\21\1\11\1\12\1\15\1\14\1\51\11\52"+
            "\1\10\1\42\1\24\1\3\1\5\1\35\1\4\32\55\1\22\1\uffff\1\36\1\50"+
            "\1\55\1\uffff\1\1\1\6\1\7\1\13\1\16\1\17\2\55\1\20\2\55\1\27"+
            "\1\55\1\32\1\55\1\34\1\55\1\40\1\43\1\45\1\55\1\46\1\47\3\55"+
            "\1\23\1\26\1\37\1\33\101\uffff\27\55\1\uffff\37\55\1\uffff\u1f08"+
            "\55\u1040\uffff\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e"+
            "\55\u10d2\uffff\u5200\55\u5900\uffff\u0200\55",
            "\1\57\20\uffff\1\60",
            "\1\62\26\uffff\1\61",
            "\1\64",
            "",
            "\1\67\1\66",
            "\1\71\2\uffff\1\72\6\uffff\1\73",
            "\1\74\6\uffff\1\75\3\uffff\1\76\2\uffff\1\77",
            "",
            "",
            "\1\100\17\uffff\1\101",
            "\1\103\11\uffff\1\104",
            "\1\106\4\uffff\1\107\15\uffff\1\105",
            "\1\111\3\uffff\1\112\1\uffff\12\114",
            "\1\115\1\uffff\1\116\11\uffff\1\117",
            "\1\120\7\uffff\1\121\2\uffff\1\122\2\uffff\1\123",
            "\1\124\6\uffff\1\125\1\126",
            "\1\127\21\uffff\1\130",
            "",
            "",
            "\1\133\1\132",
            "\1\135",
            "\1\140\76\uffff\1\137",
            "\1\142",
            "",
            "\1\143",
            "\1\145\3\uffff\1\146\17\uffff\1\147",
            "",
            "\1\150\20\uffff\1\151\2\uffff\1\152",
            "",
            "",
            "",
            "\1\153",
            "",
            "",
            "\1\154\13\uffff\1\155\1\156\1\uffff\1\157\1\uffff\1\160",
            "\1\161",
            "\1\163\11\uffff\1\164",
            "\1\165",
            "\1\166",
            "\1\167",
            "\1\114\1\uffff\10\173\2\114\12\uffff\3\114\21\uffff\1\171\13"+
            "\uffff\3\114\21\uffff\1\171",
            "\1\114\1\uffff\12\174\12\uffff\3\114\35\uffff\3\114",
            "",
            "",
            "",
            "",
            "\1\175",
            "\1\176",
            "",
            "",
            "",
            "",
            "",
            "\1\u0080\1\177",
            "",
            "",
            "\1\u0082",
            "\1\u0083",
            "\1\u0084",
            "\1\u0085\1\u0086",
            "\1\u0087",
            "\1\u0088",
            "\1\u0089",
            "",
            "",
            "",
            "\1\u008a",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\24"+
            "\55\1\u008b\5\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08"+
            "\55\u1040\uffff\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e"+
            "\55\u10d2\uffff\u5200\55\u5900\uffff\u0200\55",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\u008d",
            "\1\u008e",
            "\1\u008f",
            "\1\u0090",
            "\1\u0091",
            "\1\u0092",
            "\1\u0093",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0095",
            "\1\u0096\1\u0097",
            "",
            "",
            "",
            "",
            "\1\u0098",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\u009a",
            "",
            "",
            "\1\u009b",
            "\1\u009c",
            "\1\u009d",
            "\1\u009e",
            "\1\u009f\5\uffff\1\u00a0",
            "\1\u00a1",
            "\1\u00a2",
            "\1\u00a3",
            "\1\u00a4\20\uffff\1\u00a5",
            "\1\u00a6",
            "\1\u00a7",
            "\1\u00a8",
            "",
            "",
            "\1\u00a9\10\uffff\1\u00aa",
            "\1\u00ab\23\uffff\1\u00ac\3\uffff\1\u00ad",
            "\1\u00ae\2\uffff\1\u00af",
            "\1\u00b0",
            "",
            "",
            "",
            "",
            "\1\114\1\uffff\10\173\2\114\12\uffff\3\114\35\uffff\3\114",
            "\1\114\1\uffff\12\174\12\uffff\3\114\35\uffff\3\114",
            "\1\u00b2",
            "\1\u00b3",
            "\1\u00b4",
            "",
            "",
            "\1\u00b6",
            "\1\u00b7",
            "\1\u00b8",
            "\1\u00b9",
            "\1\u00ba",
            "\1\u00bb",
            "\1\u00bc",
            "\1\u00bd",
            "\1\u00be",
            "\1\u00bf",
            "",
            "\1\u00c0",
            "\1\u00c1",
            "\1\u00c2",
            "\1\u00c3",
            "\1\u00c4",
            "\1\u00c5",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\u00c7\2\uffff\1\u00c8",
            "\1\u00c9",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\4\55"+
            "\1\u00ca\25\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55"+
            "\u1040\uffff\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e"+
            "\55\u10d2\uffff\u5200\55\u5900\uffff\u0200\55",
            "",
            "",
            "\1\u00cc",
            "\1\u00cd",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00cf",
            "\1\u00d0",
            "\1\u00d1",
            "\1\u00d2",
            "\1\u00d3",
            "\1\u00d4",
            "\1\u00d5",
            "\1\u00d6",
            "\1\u00d7",
            "\1\u00d8",
            "\1\u00d9",
            "\1\u00da",
            "\1\u00db",
            "\1\u00dc",
            "\1\u00dd",
            "\1\u00de",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00e0",
            "\1\u00e1",
            "\1\u00e2",
            "",
            "\1\u00e3",
            "\1\u00e4",
            "",
            "",
            "\1\u00e5",
            "\1\u00e6",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00e9",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00eb",
            "\1\u00ec",
            "\1\u00ed",
            "\1\u00ee",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00f1",
            "\1\u00f2",
            "\1\u00f3",
            "\1\u00f4",
            "",
            "\1\u00f5",
            "\1\u00f6",
            "\1\u00f7",
            "\1\u00f8",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00fa",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00fc",
            "\1\u00fd",
            "\1\u00fe",
            "\1\u00ff",
            "\1\u0100",
            "\1\u0101",
            "\1\u0102",
            "\1\u0103",
            "\1\u0104",
            "\1\u0105",
            "\1\u0106",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0108",
            "\1\u0109",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u010c",
            "\1\u010d",
            "\1\u010e",
            "\1\u010f",
            "\1\u0110",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0114",
            "\1\u0115",
            "\1\u0116",
            "",
            "",
            "\1\u0117",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\13"+
            "\55\1\u0119\16\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08"+
            "\55\u1040\uffff\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e"+
            "\55\u10d2\uffff\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u011c",
            "\1\u011d",
            "\1\u011e",
            "\1\u011f",
            "",
            "\1\u0120",
            "",
            "\1\u0121",
            "\1\u0122",
            "\1\u0123",
            "\1\u0124",
            "\1\u0125",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0127",
            "\1\u0128",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u012a",
            "\1\u012b",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\22"+
            "\55\1\u012c\7\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08"+
            "\55\u1040\uffff\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e"+
            "\55\u10d2\uffff\u5200\55\u5900\uffff\u0200\55",
            "\1\u012e",
            "",
            "",
            "\1\u012f",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0131",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0133",
            "",
            "",
            "",
            "\1\u0134",
            "\1\u0135",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0137",
            "",
            "\1\u0138",
            "",
            "",
            "\1\u0139",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u013b",
            "\1\u013c",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u013e",
            "\1\u013f",
            "\1\u0140",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0144",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0146",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\u0148",
            "\1\u0149",
            "",
            "\1\u014a",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u014c",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0150",
            "",
            "\1\u0151",
            "\1\u0152",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0155",
            "",
            "",
            "",
            "\1\u0156",
            "",
            "\1\u0157",
            "",
            "\1\u0158",
            "\1\u0159",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "",
            "",
            "\1\u015c",
            "\1\u015d",
            "\1\u015e",
            "",
            "",
            "\1\u015f",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0161",
            "\1\u0162",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "",
            "\1\u0164",
            "\1\u0165",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\u0168",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "",
            "\1\u016c",
            "",
            "",
            "",
            "\1\u016d",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            ""
    };

    static final short[] DFA28_eot = DFA.unpackEncodedString(DFA28_eotS);
    static final short[] DFA28_eof = DFA.unpackEncodedString(DFA28_eofS);
    static final char[] DFA28_min = DFA.unpackEncodedStringToUnsignedChars(DFA28_minS);
    static final char[] DFA28_max = DFA.unpackEncodedStringToUnsignedChars(DFA28_maxS);
    static final short[] DFA28_accept = DFA.unpackEncodedString(DFA28_acceptS);
    static final short[] DFA28_special = DFA.unpackEncodedString(DFA28_specialS);
    static final short[][] DFA28_transition;

    static {
        int numStates = DFA28_transitionS.length;
        DFA28_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA28_transition[i] = DFA.unpackEncodedString(DFA28_transitionS[i]);
        }
    }

    class DFA28 extends DFA {

        public DFA28(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 28;
            this.eot = DFA28_eot;
            this.eof = DFA28_eof;
            this.min = DFA28_min;
            this.max = DFA28_max;
            this.accept = DFA28_accept;
            this.special = DFA28_special;
            this.transition = DFA28_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( ABSTRACT | AND | AND_ASSIGN | ASSERT | ASSIGN | AT | BIT_SHIFT_RIGHT | BIT_SHIFT_RIGHT_ASSIGN | BOOLEAN | BREAK | BYTE | CASE | CATCH | CHAR | CLASS | COLON | COMMA | CONTINUE | DEC | DEFAULT | DIV | DIV_ASSIGN | DO | DOT | DOTSTAR | DOUBLE | ELLIPSIS | ELSE | ENUM | EQUAL | EXTENDS | FALSE | FINAL | FINALLY | FLOAT | FOR | GREATER_OR_EQUAL | GREATER_THAN | IF | IMPLEMENTS | IMPORT | INC | INSTANCEOF | INT | INTERFACE | LBRACK | LCURLY | LESS_OR_EQUAL | LESS_THAN | LOGICAL_AND | LOGICAL_NOT | LOGICAL_OR | LONG | LPAREN | MINUS | MINUS_ASSIGN | MOD | MOD_ASSIGN | NATIVE | NEW | NOT | NOT_EQUAL | NULL | OR | OR_ASSIGN | PACKAGE | PLUS | PLUS_ASSIGN | PRIVATE | PROTECTED | PUBLIC | QUESTION | RBRACK | RCURLY | RETURN | RPAREN | SEMI | SHIFT_LEFT | SHIFT_LEFT_ASSIGN | SHIFT_RIGHT | SHIFT_RIGHT_ASSIGN | SHORT | STAR | STAR_ASSIGN | STATIC | STRICTFP | SUPER | SWITCH | SYNCHRONIZED | THIS | THROW | THROWS | TRANSIENT | TRUE | TRY | VOID | VOLATILE | WHILE | XOR | XOR_ASSIGN | HEX_LITERAL | DECIMAL_LITERAL | OCTAL_LITERAL | FLOATING_POINT_LITERAL | CHARACTER_LITERAL | STRING_LITERAL | IDENT | WS | COMMENT | LINE_COMMENT );";
        }
    }
 

}