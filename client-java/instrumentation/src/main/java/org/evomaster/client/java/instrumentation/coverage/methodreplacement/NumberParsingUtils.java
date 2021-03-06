package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.*;

public class NumberParsingUtils {
    /**
     * Optimizes for Java regex pattern "['-']?[0-9]*['.']?[0-9]*"
     * @param input
     * @return
     */
    public static double getParsingHeuristicValueForFloat(String input) {

        if (input == null) {
            return H_REACHED_BUT_NULL;
        }


        if (input.length() == 0) {
            return H_NOT_NULL;
        }

        long distance = 0;

        if (input.length() == 1) {
            //cannot be '-'
            distance += distanceToDigit(input.charAt(0));
        } else {

            for (int i = 0; i < input.length(); i++) {

                int digitsDist = distanceToDigit(input.charAt(i));
                int minusDist = distanceToChar(input.charAt(i), '-');
                int dotDist = distanceToChar(input.charAt(i), '.');

                if (i == 0) {
                    /*
                        first symbol could be a '-'.
                        note that '.' could be in any position, including first and last, with the only
                        exception of 2nd when first is '-'
                     */
                    distance += Math.min(Math.min(digitsDist, minusDist), dotDist);

                } else {
                    int firstIndexOfDot = input.indexOf('.');
                    if (firstIndexOfDot < 0) {
                        // no dots, so can optimize for a '.'
                        distance += Math.min(digitsDist, dotDist);
                    } else if (i == firstIndexOfDot && (firstIndexOfDot != 1 || input.charAt(0)!='-' || input.length() > 2)) {
                        distance += 0;
                    } else {
                        distance += digitsDist;
                    }
                }
            }

        }

        //recall h in [0,1] where the highest the distance the closer to 0
        final double base = H_NOT_NULL;
        return base + ((1d - base) / (distance + 1));
    }

    private static double parseIntHeuristic(String input, int maxNumberOfDigits) {

        if (maxNumberOfDigits < 0) {
            throw new IllegalArgumentException("Number of digits cannot be negative");
        }

        if (input == null) {
            return H_REACHED_BUT_NULL;
        }

        final double base = H_NOT_NULL;

        if (input.length() == 0) {
            return base;
        }

        long distance = 0;

        if (input.length() == 1) {
            //cannot be '-'
            distance += distanceToDigit(input.charAt(0));
        } else {
            for (int i = 0; i < input.length(); i++) {

                int digitsDist = distanceToDigit(input.charAt(i));

                if (i == 0) {
                    //first symbol could be a '-'
                    distance += Math.min(digitsDist, distanceToChar(input.charAt(i), '-'));
                } else if (i >= maxNumberOfDigits) {

                    //too long string would not be a valid 32bit/64bit integer representation
                    distance += MAX_CHAR_DISTANCE;
                } else {
                    distance += digitsDist;
                }

            }
        }

        //recall h in [0,1] where the highest the distance the closer to 0
        return base + ((1d - base) / (distance + 1));
    }

    public static double parseByteHeuristic(String input) {
        final int maxNumberOfDigits = Byte.valueOf(Byte.MIN_VALUE).toString().length();
        return parseIntHeuristic(input, maxNumberOfDigits);
    }

    public static double parseShortHeuristic(String input) {
        final int maxNumberOfDigits = Short.valueOf(Short.MIN_VALUE).toString().length();
        return parseIntHeuristic(input, maxNumberOfDigits);
    }

    public static double parseIntHeuristic(String input) {
        final int maxNumberOfDigits = Integer.valueOf(Integer.MIN_VALUE).toString().length();
        return parseIntHeuristic(input, maxNumberOfDigits);
    }

    public static double parseLongHeuristic(String input) {
        final int maxNumberOfDigits = Long.valueOf(Long.MIN_VALUE).toString().length();
        return parseIntHeuristic(input, maxNumberOfDigits);
    }
}
