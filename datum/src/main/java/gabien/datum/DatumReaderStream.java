/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.io.IOException;
import java.io.Reader;

/**
 * DatumTokenStream based on Reader.
 */
public class DatumReaderStream extends DatumTokenStream {
    private Reader reader;
    private String tokenContents;
    private DatumTokenType tokenType;
    private int holdingCell = -1;
    private boolean lastCharWasDirect;

    public int lineNumber = 1;
    public int tokenLineNumber = 1;

    public DatumReaderStream(Reader r) {
        reader = r;
    }

    private int readerRead() {
        int chr;
        try {
            chr = reader.read();
        } catch (IOException e) {
            throw new DatumRuntimeIOException(e);
        }
        if (chr == 10)
            lineNumber++;
        return chr;
    }

    private int decodeNextChar() {
        if (holdingCell != -1) {
            int tmp = holdingCell;
            holdingCell = -1;
            return tmp;
        }
        int val = readerRead();
        if (val != '\\') {
            lastCharWasDirect = true;
            return val;
        }
        lastCharWasDirect = false;
        val = readerRead();
        if (val == -1)
            throw new RuntimeException("Line " + lineNumber + ": \\ without escape");
        if (val == 'r')
            return '\r';
        if (val == 'n')
            return '\n';
        if (val == 't')
            return '\t';
        if (val == 'x') {
            int res = 0;
            while (true) {
                int dig = readerRead();
                if (dig == -1)
                    throw new RuntimeException("Line " + lineNumber + ": Interrupted hex escape");
                if (dig == ';')
                    break;
                res <<= 4;
                try {
                    res |= Integer.parseInt(Character.toString((char) dig), 16);
                } catch (NumberFormatException nfe) {
                    throw new RuntimeException("Line " + lineNumber + ": Bad hex escape");
                }
            }
            char[] data = Character.toChars(res);
            if (data.length == 2)
                holdingCell = data[1];
            return data[0];
        }
        return val;
    }

    @Override
    public boolean read() {
        char decChar = 0;
        while (true) {
            int dec = decodeNextChar();
            if (dec == -1)
                return false;
            decChar = (char) dec;
            // if not direct, it's content-class, break!
            if (!lastCharWasDirect)
                break;
            // special handling!
            if (DatumCharacters.isWhitespace(decChar)) {
                continue;
            } else if (decChar == ';') {
                while (true) {
                    dec = decodeNextChar();
                    if (dec == -1)
                        return false;
                    if (dec == 10 && lastCharWasDirect)
                        break;
                }
            } 
            break;
        }
        tokenLineNumber = lineNumber;
        tokenContents = null;
        // not whitespace, what do we do?
        if (DatumCharacters.isAlone(decChar) && lastCharWasDirect) {
            if (decChar == '(')
                tokenType = DatumTokenType.ListStart;
            if (decChar == ')')
                tokenType = DatumTokenType.ListEnd;
            if (decChar == '\'')
                tokenType = DatumTokenType.Quote;
            return true;
        } else if (decChar == '"' && lastCharWasDirect) {
            StringBuilder sb = new StringBuilder();
            while (true) {
                int dec = decodeNextChar();
                if (dec == -1)
                    throw new RuntimeException("Line " + lineNumber + ": interrupted string");
                decChar = (char) dec;
                if (decChar == '"' && lastCharWasDirect)
                    break;
                sb.append(decChar);
            }
            tokenType = DatumTokenType.String;
            tokenContents = sb.toString();
            return true;
        } else {
            boolean numeric = DatumCharacters.isNumericStart(decChar);
            StringBuilder sb = new StringBuilder();
            // ensure initial character exists!
            sb.append(decChar);
            while (true) {
                int dec = decodeNextChar();
                if (dec == -1)
                    break;
                decChar = (char) dec;
                if (!(DatumCharacters.isContent(decChar) || !lastCharWasDirect)) {
                    // put back lost character
                    holdingCell = dec;
                    break;
                }
                sb.append(decChar);
            }
            tokenType = numeric ? DatumTokenType.Numeric : DatumTokenType.ID;
            tokenContents = sb.toString();
            return true;
        }
    }

    @Override
    public String position() {
        if (tokenType != null) {
            if (tokenContents != null)
                return "L" + tokenLineNumber + ":" + tokenType + "[" + tokenContents + "]";
            return "L" + tokenLineNumber + ":" + tokenType;
        }
        return "L" + tokenLineNumber;
    }

    @Override
    public String contents() {
        return tokenContents;
    }

    @Override
    public DatumTokenType type() {
        return tokenType;
    }
    
}
