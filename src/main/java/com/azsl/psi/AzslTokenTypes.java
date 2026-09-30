package com.azsl.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

public interface AzslTokenTypes {
    // Keywords - control flow
    IElementType KEYWORD = new AzslTokenType("KEYWORD");

    // Types
    IElementType TYPE_KEYWORD = new AzslTokenType("TYPE_KEYWORD");

    // Semantic keywords
    IElementType SEMANTIC = new AzslTokenType("SEMANTIC");

    // AZSL-specific tokens
    IElementType SRG_SEMANTIC = new AzslTokenType("SRG_SEMANTIC");
    IElementType SAMPLER_KEYWORD = new AzslTokenType("SAMPLER_KEYWORD");
    IElementType SHADER_OPTION = new AzslTokenType("SHADER_OPTION");

    // Preprocessor
    IElementType PREPROCESSOR = new AzslTokenType("PREPROCESSOR");

    // Built-in functions
    IElementType BUILTIN_FUNCTION = new AzslTokenType("BUILTIN_FUNCTION");
    IElementType FUNCTION_CALL = new AzslTokenType("FUNCTION_CALL");
    IElementType INSTANCE_METHOD_CALL = new AzslTokenType("INSTANCE_METHOD_CALL");
    IElementType FIELD_ACCESS = new AzslTokenType("FIELD_ACCESS");

    // Literals
    IElementType NUMBER = new AzslTokenType("NUMBER");
    IElementType STRING = new AzslTokenType("STRING");

    // Identifiers
    IElementType IDENTIFIER = new AzslTokenType("IDENTIFIER");
    IElementType STRUCT_NAME = new AzslTokenType("STRUCT_NAME");

    // Comments
    IElementType LINE_COMMENT = new AzslTokenType("LINE_COMMENT");
    IElementType BLOCK_COMMENT = new AzslTokenType("BLOCK_COMMENT");

    // Operators and punctuation
    IElementType SEMICOLON = new AzslTokenType("SEMICOLON");
    IElementType COMMA = new AzslTokenType("COMMA");
    IElementType DOT = new AzslTokenType("DOT");
    IElementType COLON = new AzslTokenType("COLON");
    IElementType LPAREN = new AzslTokenType("LPAREN");
    IElementType RPAREN = new AzslTokenType("RPAREN");
    IElementType LBRACE = new AzslTokenType("LBRACE");
    IElementType RBRACE = new AzslTokenType("RBRACE");
    IElementType LBRACKET = new AzslTokenType("LBRACKET");
    IElementType RBRACKET = new AzslTokenType("RBRACKET");
    IElementType OPERATOR = new AzslTokenType("OPERATOR");

    // Token sets
    TokenSet COMMENTS = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT);
    TokenSet WHITESPACES = TokenSet.create(com.intellij.psi.TokenType.WHITE_SPACE);
    TokenSet STRINGS = TokenSet.create(STRING);
}
