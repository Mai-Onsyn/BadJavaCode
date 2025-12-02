package mai_onsyn.code_flattor;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import mai_onsyn.code_flattor.AXImage;

public class FileRecoder {
    private static final Pattern startRegex = Pattern.compile("[a-zA-Z0-9_$]");
    private static final Pattern endRegex = Pattern.compile("[; <>=+\\-,*/&%|\"'!:.{}\\[\\]()]");
    private static final Set<String> MULTI_CHAR_OPERATORS = Set.of(
            "++", "--", "&&", "||", "==", "!=", "<=", ">=", "<<", ">>", ">>>",
            "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=",
            "->", "::"
    );

    public static List<String> makePicture(List<String> tokens, AXImage image, int tokenStartOffest) {
        int imageWidth = image.getWidth();
        List<String> result = new ArrayList<>();
        List<Integer> imagePieces = parseWordLength(image);

//        for (Integer imagePiece : imagePieces) {
//            if (imagePiece < 0) for (int i = 0; i < -imagePiece; i++) System.out.print(" ");
//            else if (imagePiece > 0) for (int i = 0; i < imagePiece; i++) System.out.print("*");
//            else System.out.println();
//        }
        printTokenRange(tokens, 0, tokenStartOffest, imageWidth, result);
        result.add("\n".repeat(8));

        StringBuilder currentBuilder = new StringBuilder();
        int useTokenIndex = tokenStartOffest;
        for (Integer lineLength : imagePieces) {
            if (lineLength < 0) {
                result.add(" ".repeat(-lineLength));
            } else if (lineLength == 0) {
                result.add("\n");
            } else {
//                result.add("*".repeat(lineLength));
                int tokenLength = 0;
                int recordTokenIndex = useTokenIndex;
                for (; useTokenIndex < tokens.size(); useTokenIndex++) {
                    String token = tokens.get(useTokenIndex);
                    if (tokenLength + token.length() > lineLength) {
                        break;
                    } else {
                        tokenLength += token.length();
                    }
                }
                int tokenCount = useTokenIndex - recordTokenIndex;
                int spaceOffest = lineLength - tokenLength;
                if (tokenCount > 1) {
                    int spacePerToken = spaceOffest / (tokenCount - 1);
                    int spaceLeft = spaceOffest % (tokenCount - 1);
                    for (int i = 0; i < tokenCount; i++) {
                        String token = tokens.get(recordTokenIndex + i);
                        currentBuilder.append(token);
                        currentBuilder.append(" ".repeat(spacePerToken + (i < spaceLeft ? 1 : 0)));
                    }
                }
                if (tokenCount == 1) {
                    currentBuilder.append(tokens.get(recordTokenIndex)).append(" ".repeat(spaceOffest));
                }
                else currentBuilder.append(" ".repeat(spaceOffest));
                result.add(currentBuilder.toString());
                currentBuilder.delete(0, currentBuilder.length());
            }
        }
        result.add("\n".repeat(8));
        printTokenRange(tokens, useTokenIndex, tokens.size(), imageWidth, result);

        return result;
    }

    private static void printTokenRange(List<String> tokens, int start, int end, int imageWidth, List<String> result) {
        int lineLength = 0;
        StringBuilder lineBuilder = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (lineLength > imageWidth) {
                result.add(lineBuilder.toString());
                result.add("\n");
                lineLength = 0;
                lineBuilder.delete(0, lineBuilder.length());
            }
            String token = tokens.get(i);
            lineLength += token.length();
            lineBuilder.append(token);
        }
        if (!lineBuilder.isEmpty()) {
            result.add(lineBuilder.toString());
            result.add("\n");
        }
    }

    //>0: word, <0: empty, =0: break line
    private static List<Integer> parseWordLength(AXImage image) {
        List<Integer> imagePieces = new ArrayList<>();
        boolean last = true; // true表示空白，false表示非空白
        int stateStart = 0;
        int lineCounter = 0;
        int[] pixels = image.getPixels();
        int width = image.getWidth();

        for (int pixel : pixels) {
            lineCounter++;
            boolean empty = new Color(pixel).equals(Color.WHITE);

            // 状态变化检测（放在换行检测之前）
            if (last != empty && stateStart > 0) {
                // 添加前一个状态段
                imagePieces.add(last ? stateStart : -stateStart);
                stateStart = 0;
            }

            // 处理当前像素
            stateStart++;
            last = empty;

            // 换行检测
            if (lineCounter == width) {
                // 添加当前行的最后一个状态段
                if (stateStart > 0) {
                    imagePieces.add(empty ? stateStart : -stateStart);
                }
                // 添加换行标记
                imagePieces.add(0);

                // 重置计数器
                lineCounter = 0;
                stateStart = 0;
                last = true; // 重置为空白状态开始新行
            }
        }

        // 处理最后一行（如果没有完整换行）
        if (stateStart > 0) {
            imagePieces.add(last ? stateStart : -stateStart);
        }

        return imagePieces;
    }

    public static List<String> tokenizeJava(File file) {
        List<String> lines = deleteNotes(readFile(file));
        List<String> roughTokens = new ArrayList<>();
        List<String> tokens = new ArrayList<>();

        for (String line : lines) {
//            if (!line.contains("if (substring")) continue;

            boolean inWord = false;
            boolean inString = false;
            boolean isSingleQuotation = false;
            StringBuilder wordBuilder = new StringBuilder();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                boolean isSm = startRegex.matcher(String.valueOf(c)).find();
                boolean isEm = endRegex.matcher(String.valueOf(c)).find();

                if (inWord) {
                    if (isEm) {
                        inWord = false;
                        if (!wordBuilder.isEmpty()) {
                            roughTokens.add(wordBuilder.toString());
                            wordBuilder.delete(0, wordBuilder.length());
                        }
                    } else {
                        wordBuilder.append(c);
                    }
                }

                if (inString) {
                    stringBuilder.append(c);
                    if (isStringQuotation(line, i) && isSingleStringQuotation(line, i) == isSingleQuotation) {
                        inString = false;
                        if (!stringBuilder.isEmpty()) {
                            roughTokens.add(stringBuilder.toString());
                            stringBuilder.delete(0, stringBuilder.length());
                        }
                        continue;
                    }
                }

                if (!inWord && !inString) {
                    if (isSm) {
                        inWord = true;
                        wordBuilder.append(c);
                    }
                    else if (isStringQuotation(line, i)) {
                        inString = true;
                        stringBuilder.append(c);
                        isSingleQuotation = isSingleStringQuotation(line, i);
                    }
                    else {
                        roughTokens.add(String.valueOf(c));
                    }
                }
            }

            if (!wordBuilder.isEmpty()) roughTokens.add(wordBuilder.toString());
            if (!stringBuilder.isEmpty()) roughTokens.add(stringBuilder.toString());
        }

        List<String> mergedTokens = mergeOperators(roughTokens);
        roughTokens.clear();
        for (String mergedToken : mergedTokens) {
            String token = mergedToken.trim();
            if (token.isEmpty()) continue;
            roughTokens.add(token);
        }
        for (int i = 0; i < roughTokens.size(); i++) {
            String token = roughTokens.get(i);
            tokens.add(token);
            if (isWord(token.charAt(token.length() - 1))) {
                if (isWord(roughTokens.get(i + 1).charAt(0))) {
                    tokens.add(" ");
                }
            }
        }

        return tokens;
    }

    private static boolean isWord(char c) {
        return startRegex.matcher(String.valueOf(c)).matches();
    }

    private static boolean isStringQuotation(String line, int index) {
        boolean isQuotation = line.charAt(index) == '\'' || line.charAt(index) == '"';
        if (index == 0 && isQuotation) {
            return true;
        }
        return index > 0 && (isQuotation) && line.charAt(index - 1) != '\\';
    }

    private static boolean isSingleStringQuotation(String line, int index) {
        boolean isQuotation = line.charAt(index) == '\'';
        if (index == 0 && isQuotation) {
            return true;
        }
        return index > 0 && (isQuotation) && line.charAt(index - 1) != '\\';
    }

    public static List<String> mergeOperators(List<String> tokens) {
        List<String> mergedTokens = new ArrayList<>();
        int i = 0;

        while (i < tokens.size()) {
            String current = tokens.get(i);

            // 检查是否能与后续token组成多符号运算符
            boolean merged = false;
            for (int len = 4; len >= 2; len--) { // 从最长可能开始检查
                if (i + len <= tokens.size()) {
                    StringBuilder candidate = new StringBuilder();
                    for (int j = 0; j < len; j++) {
                        candidate.append(tokens.get(i + j));
                    }
                    String candidateStr = candidate.toString();

                    if (MULTI_CHAR_OPERATORS.contains(candidateStr)) {
                        mergedTokens.add(candidateStr);
                        i += len;
                        merged = true;
                        break;
                    }
                }
            }

            if (!merged) {
                mergedTokens.add(current);
                i++;
            }
        }

        //numbers
        for (int n = 0; n < mergedTokens.size(); n++) {
            String token = mergedTokens.get(n);
            if (token.matches("\\d+")) {
                if (n + 1 < mergedTokens.size() && mergedTokens.get(n + 1).equals(".")) {
                    mergedTokens.set(n, token + ".");
                    mergedTokens.remove(n + 1);
                }
            }
        }

        return mergedTokens;
    }

    private static List<String> deleteNotes(String input) {
        String[] lines_arr = input.split("\n");
        List<String> unNoted = new ArrayList<>();

        // 添加跨行注释的状态标志
        boolean inMultiLineComment = false;

        for (String line : lines_arr) {
            StringBuilder sb = new StringBuilder();
            int findIndex = 0;
            boolean inString = false;
            char stringChar = 0; // 记录是单引号还是双引号

            while (findIndex < line.length()) {
                char currentChar = line.charAt(findIndex);

                // 处理字符串状态
                if (!inMultiLineComment && !inString && (currentChar == '"' || currentChar == '\'')) {
                    inString = true;
                    stringChar = currentChar;
                    sb.append(currentChar);
                    findIndex++;
                    continue;
                }

                // 处理字符串结束
                if (inString && currentChar == stringChar) {
                    // 检查是否是转义字符
                    if (findIndex > 0 && line.charAt(findIndex - 1) == '\\') {
                        // 如果是转义的引号，继续在字符串中
                        sb.append(currentChar);
                        findIndex++;
                        continue;
                    }
                    inString = false;
                    sb.append(currentChar);
                    findIndex++;
                    continue;
                }

                // 如果在字符串中，直接添加字符
                if (inString) {
                    sb.append(currentChar);
                    findIndex++;
                    continue;
                }

                // 处理多行注释开始
                if (!inMultiLineComment && findIndex + 1 < line.length() &&
                        currentChar == '/' && line.charAt(findIndex + 1) == '*') {
                    inMultiLineComment = true;
                    findIndex += 2;
                    continue;
                }

                // 处理多行注释结束
                if (inMultiLineComment && findIndex + 1 < line.length() &&
                        currentChar == '*' && line.charAt(findIndex + 1) == '/') {
                    inMultiLineComment = false;
                    findIndex += 2;
                    continue;
                }

                // 如果在多行注释中，跳过字符
                if (inMultiLineComment) {
                    findIndex++;
                    continue;
                }

                // 处理单行注释
                if (findIndex + 1 < line.length() && currentChar == '/' && line.charAt(findIndex + 1) == '/') {
                    break; // 跳过行尾所有内容
                }

                // 普通字符，添加到结果
                sb.append(currentChar);
                findIndex++;
            }

            String resultLine = sb.toString().trim();
            if (!resultLine.isEmpty()) {
                unNoted.add(resultLine);
            }
        }

        return unNoted;
    }

    private static String readFile(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    private static List<String> deleteNotes2(String input) {
        String[] lines_arr = input.split("\n");

        List<String> unNoted = new ArrayList<>();

        for (String line : lines_arr) {
            StringBuilder sb = new StringBuilder();
            int findIndex = 0;
            boolean inAnnotation = false;
//            boolean inString = false;
            while (findIndex < line.length()) {
                String substring = line.substring(findIndex);
//                if (findIndex > 0 && (line.charAt(findIndex - 1) != '\\' && substring.charAt(0) == '"') || substring.charAt(0) == '"') inString = !inString;
//                if (inString) continue;
                if (substring.startsWith("//")) break;
                if (substring.startsWith("/*")) {
                    inAnnotation = true;
                    findIndex++;
                }
                if (substring.startsWith("*/")) {
                    inAnnotation = false;
                    findIndex += 2;
                }
                if (inAnnotation) {
                    findIndex++;
                    continue;
                }
                sb.append(line.charAt(findIndex++));
            }
            String trimmed = sb.toString().trim();
            if (!trimmed.isEmpty()) unNoted.add(trimmed);
        }

        return unNoted;
    }

}

