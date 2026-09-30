package com.azsl.structure;

import com.azsl.lexer.AzslLexer;
import com.azsl.psi.AzslTokenTypes;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Lightweight token scan that extracts AZSL declarations for the structure view.
 * The parser is flat, so we walk the token stream directly and nest by brace depth.
 */
public final class AzslDeclarationScanner {

    public static final class Decl {
        final String name;
        final int nameOffset;
        final AzslStructureKind kind;
        final List<Decl> children = new ArrayList<>();
        int bodyDepth = -1;

        Decl(String name, int nameOffset, AzslStructureKind kind) {
            this.name = name;
            this.nameOffset = nameOffset;
            this.kind = kind;
        }
    }

    private final List<IElementType> types = new ArrayList<>();
    private final List<String> texts = new ArrayList<>();
    private final List<Integer> starts = new ArrayList<>();

    private AzslDeclarationScanner(PsiFile file) {
        CharSequence text = file.getText();
        AzslLexer lexer = new AzslLexer();
        lexer.start(text, 0, text.length(), 0);
        while (true) {
            IElementType t = lexer.getTokenType();
            if (t == null) break;
            if (t != TokenType.WHITE_SPACE
                    && t != AzslTokenTypes.LINE_COMMENT
                    && t != AzslTokenTypes.BLOCK_COMMENT) {
                types.add(t);
                texts.add(text.subSequence(lexer.getTokenStart(), lexer.getTokenEnd()).toString());
                starts.add(lexer.getTokenStart());
            }
            lexer.advance();
        }
    }

    public static List<Decl> scan(PsiFile file) {
        return new AzslDeclarationScanner(file).run();
    }

    private List<Decl> run() {
        List<Decl> top = new ArrayList<>();
        Deque<Decl> open = new ArrayDeque<>();
        int braceDepth = 0;

        int i = 0;
        while (i < types.size()) {
            IElementType t = types.get(i);
            if (t == AzslTokenTypes.LBRACE) {
                braceDepth++;
                i++;
                continue;
            }
            if (t == AzslTokenTypes.RBRACE) {
                braceDepth = Math.max(0, braceDepth - 1);
                while (!open.isEmpty() && open.peek().bodyDepth > braceDepth) open.pop();
                i++;
                continue;
            }

            Decl decl = null;
            boolean hasBody = false;
            int next = i;

            if (t == AzslTokenTypes.KEYWORD) {
                String kw = texts.get(i);
                switch (kw) {
                    case "struct" -> { decl = namedNext(i, AzslStructureKind.STRUCT); hasBody = true; }
                    case "class" -> { decl = namedNext(i, AzslStructureKind.CLASS); hasBody = true; }
                    case "interface" -> { decl = namedNext(i, AzslStructureKind.INTERFACE); hasBody = true; }
                    case "cbuffer", "tbuffer" -> { decl = namedNext(i, AzslStructureKind.BUFFER); hasBody = true; }
                    case "enum" -> { decl = enumDecl(i); hasBody = true; }
                    case "ShaderResourceGroup" -> { decl = namedNext(i, AzslStructureKind.SHADER_RESOURCE_GROUP); hasBody = true; }
                    case "ShaderResourceGroupSemantic" -> { decl = namedNext(i, AzslStructureKind.SHADER_RESOURCE_GROUP_SEMANTIC); hasBody = true; }
                    case "option" -> { decl = optionDecl(i); next = optionEnd(i); }
                    default -> { }
                }
            } else if (t == AzslTokenTypes.FUNCTION_CALL) {
                decl = functionDecl(i);
                if (decl != null) hasBody = true;
            } else if (t == AzslTokenTypes.SHADER_OPTION) {
                decl = optionDecl(i);
                next = optionEnd(i);
            }

            if (decl != null) {
                while (!open.isEmpty() && open.peek().bodyDepth > braceDepth) open.pop();
                if (open.isEmpty() || open.peek().bodyDepth != braceDepth) {
                    if (braceDepth == 0 || open.isEmpty()) {
                        top.add(decl);
                    } else {
                        open.peek().children.add(decl);
                    }
                } else {
                    open.peek().children.add(decl);
                }
                if (hasBody) {
                    decl.bodyDepth = braceDepth + 1;
                    open.push(decl);
                }
                i = (next > i) ? next : i + 1;
                continue;
            }

            i++;
        }

        return top;
    }

    /** struct/class/interface/cbuffer/SRG: the name is the following STRUCT_NAME or IDENTIFIER. */
    private @Nullable Decl namedNext(int i, AzslStructureKind kind) {
        for (int j = i + 1; j < types.size() && j < i + 4; j++) {
            IElementType t = types.get(j);
            String tx = texts.get(j);
            if (t == AzslTokenTypes.STRUCT_NAME || t == AzslTokenTypes.IDENTIFIER
                    || t == AzslTokenTypes.FUNCTION_CALL || t == AzslTokenTypes.SRG_SEMANTIC) {
                return new Decl(tx, starts.get(j), kind);
            }
            if (t == AzslTokenTypes.LBRACE || t == AzslTokenTypes.SEMICOLON) break;
        }
        return null;
    }

    /** enum [class] Name */
    private @Nullable Decl enumDecl(int i) {
        int j = i + 1;
        if (j < types.size() && t(j) == AzslTokenTypes.KEYWORD && "class".equals(texts.get(j))) {
            j++;
        }
        if (j < types.size()) {
            IElementType t = types.get(j);
            if (t == AzslTokenTypes.STRUCT_NAME || t == AzslTokenTypes.IDENTIFIER) {
                return new Decl(texts.get(j), starts.get(j), AzslStructureKind.ENUM);
            }
        }
        return null;
    }

    /** option [enum class X {..}] <type> Name [= default] ; — name is the last identifier before ';' */
    private @Nullable Decl optionDecl(int i) {
        int end = optionEnd(i);
        int nameIdx = -1;
        int j = i + 1;
        int guard = 0;
        while (j < types.size() && j <= end && guard++ < 64) {
            IElementType t = types.get(j);
            if (t == AzslTokenTypes.IDENTIFIER || t == AzslTokenTypes.STRUCT_NAME) {
                nameIdx = j;
            }
            j++;
        }
        if (nameIdx < 0) return null;
        return new Decl(texts.get(nameIdx), starts.get(nameIdx), AzslStructureKind.OPTION);
    }

    /** Index of the ';' terminating an option declaration (brace/paren aware). */
    private int optionEnd(int i) {
        int braces = 0, parens = 0;
        for (int j = i + 1; j < types.size() && j < i + 128; j++) {
            IElementType t = types.get(j);
            if (t == AzslTokenTypes.LBRACE) braces++;
            else if (t == AzslTokenTypes.RBRACE) braces = Math.max(0, braces - 1);
            else if (t == AzslTokenTypes.LPAREN) parens++;
            else if (t == AzslTokenTypes.RPAREN) parens = Math.max(0, parens - 1);
            else if (t == AzslTokenTypes.SEMICOLON && braces == 0 && parens == 0) return j;
        }
        return Math.min(types.size() - 1, i + 32);
    }

    /** Function definition: Name ( ... ) [: semantic] { */
    private @Nullable Decl functionDecl(int i) {
        // name followed by '('
        if (i + 1 >= types.size() || t(i + 1) != AzslTokenTypes.LPAREN) return null;
        int close = matching(i + 1);
        if (close < 0) return null;
        // scan after ')' up to a few tokens for '{' (definition) before ';'
        int j = close + 1;
        int guard = 0;
        while (j < types.size() && guard++ < 8) {
            IElementType t = types.get(j);
            if (t == AzslTokenTypes.LBRACE) {
                return new Decl(texts.get(i), starts.get(i), AzslStructureKind.FUNCTION);
            }
            if (t == AzslTokenTypes.SEMICOLON) return null; // prototype / call statement
            j++;
        }
        return null;
    }

    private int matching(int openIdx) {
        int depth = 0;
        for (int j = openIdx; j < types.size(); j++) {
            IElementType t = types.get(j);
            if (t == AzslTokenTypes.LPAREN) depth++;
            else if (t == AzslTokenTypes.RPAREN) {
                depth--;
                if (depth == 0) return j;
            }
        }
        return -1;
    }

    private IElementType t(int i) {
        return types.get(i);
    }
}
