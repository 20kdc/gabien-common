/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * DatumTokenStream based on Reader.
 * Created February 16th, 2023.
 */
public class DatumReaderTokenSource extends DatumTokenSource {
    private Reader reader;
    private String tokenContents;
    private DatumTokenType tokenType;
    private int holdingCell = -1;
    private DatumCharClass lastCharClass;

    public int lineNumber = 1;
    public int tokenLineNumber = 1;

    public final String fileName;

    private DatumSrcLoc lastSrcLoc = null;
    private int lastSrcLocLineNumber = -1;

    public DatumReaderTokenSource(String fn, Reader r) {
        fileName = fn;
        reader = r;
    }

    public DatumReaderTokenSource(String fn, String s) {
        this(fn, new StringReader(s));
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
            if (val != -1)
                lastCharClass = DatumCharClass.identify((char) val);
            return val;
        }
        lastCharClass = DatumCharClass.Content;
        val = readerRead();
        if (val == -1)
            throw new RuntimeException(position() + ": \\ without escape");
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
                    throw new RuntimeException(position() + ": Interrupted hex escape");
                if (dig == ';')
                    break;
                res <<= 4;
                try {
                    res |= Integer.parseInt(Character.toString((char) dig), 16);
                } catch (NumberFormatException nfe) {
                    throw new RuntimeException(position() + ": Bad hex escape");
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
            // special handling!
            if (lastCharClass == DatumCharClass.Whitespace || lastCharClass == DatumCharClass.Newline) {
                continue;
            } else if (lastCharClass == DatumCharClass.LineComment) {
                while (true) {
                    dec = decodeNextChar();
                    if (dec == -1)
                        return false;
                    if (lastCharClass == DatumCharClass.Newline)
                        break;
                }
                continue;
            }
            break;
        }
        tokenLineNumber = lineNumber;
        tokenContents = null;
        // not whitespace, what do we do?
        if (lastCharClass.aloneToken != null) {
            tokenType = lastCharClass.aloneToken;
            return true;
        } else if (lastCharClass == DatumCharClass.String) {
            StringBuilder sb = new StringBuilder();
            while (true) {
                int dec = decodeNextChar();
                if (dec == -1)
                    throw new RuntimeException(position() + ": interrupted string");
                if (lastCharClass == DatumCharClass.String)
                    break;
                decChar = (char) dec;
                sb.append(decChar);
            }
            tokenType = DatumTokenType.String;
            tokenContents = sb.toString();
            return true;
        } else {
            boolean numeric = lastCharClass == DatumCharClass.NumericStart;
            boolean specialID = lastCharClass == DatumCharClass.SpecialID;
            StringBuilder sb = new StringBuilder();
            // ensure initial character exists!
            sb.append(decChar);
            while (true) {
                int dec = decodeNextChar();
                if (dec == -1)
                    break;
                if (!lastCharClass.isValidPID) {
                    // put back lost character and leave
                    holdingCell = dec;
                    break;
                }
                decChar = (char) dec;
                sb.append(decChar);
            }
            tokenContents = sb.toString();
            tokenType = DatumTokenType.ID;
            if (numeric && !tokenContents.equals("-"))
                tokenType = DatumTokenType.Numeric;
            if (specialID) {
                tokenType = DatumTokenType.SpecialID;
                tokenContents = tokenContents.substring(1);
            }
            return true;
        }
    }

    @Override
    public String position() {
        if (tokenType != null) {
            if (tokenContents != null)
                return fileName + "L" + tokenLineNumber + ":" + tokenType + "[" + tokenContents + "]";
            return fileName + "L" + tokenLineNumber + ":" + tokenType;
        }
        return fileName + "L" + tokenLineNumber;
    }

    @Override
    public DatumSrcLoc srcLoc() {
        if (lastSrcLoc == null || lastSrcLocLineNumber != tokenLineNumber) {
            lastSrcLoc = new DatumSrcLoc(fileName, tokenLineNumber);
            lastSrcLocLineNumber = tokenLineNumber;
        }
        return lastSrcLoc;
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
