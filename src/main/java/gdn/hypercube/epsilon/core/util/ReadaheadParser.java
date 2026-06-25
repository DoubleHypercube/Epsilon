package gdn.hypercube.epsilon.core.util;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Deprecated
public class ReadaheadParser {
    private static final Pattern CONTROL_CODE = Pattern.compile("[0-9A-F][0-9A-F] [0-9A-F][0-9A-F]");
    private static final Predicate<String> IS_CONTROL_CODE = input -> CONTROL_CODE.matcher(input).matches();

    public static String[] parse(String... input) {
        String[] parsed = new String[input.length];
        StringBuilder[] builders = new StringBuilder[parsed.length];
        for (int builder = 0; builder < builders.length; builder++) {
            builders[builder] = new StringBuilder();
        }

        int position = 0;
        for (String str : input) {
            char[][] text = { str.toCharArray() };
            for (int index = 0; index < text[0].length; index++) {
                String target;
                boolean escaped = index > 0 && text[0][index] == '[' && text[0][index-1] == '\\';
                if (escaped) {
                    System.arraycopy(text[0], index, text[0], index - 1, text[0].length - index);
                    text[0] = Arrays.copyOf(text[0], text[0].length - 1);
                    index--;
                    builders[position].deleteCharAt(builders[position].length() - 1);
                }

                if (text[0][index] == '[' && !escaped) {
                    StringBuilder maybeCode = new StringBuilder();
                    for (int readahead = 1; readahead <= 5; readahead++) {
                        try {
                            char there = text[0][index + readahead];
                            if (there == ']') break; // fail early for small strings
                            maybeCode.append(there);
                        } catch (IndexOutOfBoundsException ignored) {}
                    }
                    if (IS_CONTROL_CODE.test(String.valueOf(maybeCode))) {
                        System.out.println("Found control code: " + maybeCode);
                        target = "";
                        StringBuilder code = new StringBuilder();
                        System.out.println("Opening code");
                        builders[position].append((char) 0x00);
                        for (int readahead = 1; readahead <= text[0].length - index; readahead++) {
                            char there = text[0][index + readahead];
                            if (there == ']') {
                                break;
                            }
                            code.append(there);
                        }
                        int end = position;
                        for (int pos = 0; pos < code.length(); pos++) {
                            end = pos + 2;
                            try {
                                try {
                                    char segment = (char) Integer.parseInt(code.substring(pos, end), 16);
                                    System.out.println("Got byte " + (int) segment);
                                    builders[position].append(segment);
                                } catch (NumberFormatException ignored) {}
                            } catch (IndexOutOfBoundsException ignored) {
                                break;
                            }
                            pos = end;
                        }
                        index += (end + 1);
                        System.out.println();
                    } else {
                        target = "[" + maybeCode;
                        index += maybeCode.length();
                    }
                } else {
                    target = String.valueOf(text[0][index]);
                }
                builders[position].append(target);
            }
            position++;
        }

        for (int index = 0; index < builders.length; index++) {
            parsed[index] = builders[index].toString();
        }
        return parsed;
    }
}
