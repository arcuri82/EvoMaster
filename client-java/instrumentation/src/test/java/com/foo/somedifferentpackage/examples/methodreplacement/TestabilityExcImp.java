package com.foo.somedifferentpackage.examples.methodreplacement;

import org.evomaster.client.java.instrumentation.example.methodreplacement.TestabilityExc;

import java.text.DateFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by arcuri82 on 27-Jun-19.
 */
public class TestabilityExcImp implements TestabilityExc {

    @Override
    public int parseInt(String input) {
        return Integer.parseInt(input);
    }

    @Override
    public LocalDate parseLocalDate(String input) {
        return LocalDate.parse(input);
    }

    @Override
    public boolean after(Date instance, Date when) {
        return instance.after(when);
    }

    @Override
    public boolean before(Date instance, Date when) {
        return instance.before(when);
    }

    @Override
    public boolean equals(Date instance, Date other) {
        return instance.equals(other);
    }

    @Override
    public boolean isEmpty(Collection instance) {
        return instance.isEmpty();
    }

    @Override
    public boolean contains(Collection instance, Object o) {
        return instance.contains(o);
    }

    @Override
    public boolean containsKey(Map instance, Object o) {
        return instance.containsKey(o);
    }

    @Override
    public boolean objectEquals(Object left, Object right) {
        return Objects.equals(left, right);
    }

    @Override
    public Date dateFormatParse(DateFormat caller, String input) throws ParseException {
        return caller.parse(input);
    }

    @Override
    public boolean parseBoolean(String input) {
        return Boolean.parseBoolean(input);
    }

    @Override
    public long parseLong(String input) {
        return Long.parseLong(input);
    }

    @Override
    public float parseFloat(String input) {
        return Float.parseFloat(input);
    }

    @Override
    public double parseDouble(String input) {
        return Double.parseDouble(input);
    }

    @Override
    public boolean stringEquals(String caller, Object input) {
        return caller.equals(input);
    }

    @Override
    public boolean stringEqualsIgnoreCase(String caller, String input) {
        return caller.equalsIgnoreCase(input);
    }

    @Override
    public boolean stringIsEmpty(String caller) {
        return caller.isEmpty();
    }

    @Override
    public boolean stringContentEquals(String caller, CharSequence input) {
        return caller.contentEquals(input);
    }

    @Override
    public boolean stringContentEquals(String caller, StringBuffer input) {
        return caller.contentEquals(input);
    }

    @Override
    public boolean contains(String caller, CharSequence input) {
        return caller.contains(input);
    }

    @Override
    public boolean startsWith(String caller, String prefix) {
        return caller.startsWith(prefix);
    }

    @Override
    public boolean startsWith(String caller, String prefix, int toffset) {
        return caller.startsWith(prefix, toffset);
    }

    @Override
    public boolean stringMatches(String caller, String regex) {
        return caller.matches(regex);
    }

    @Override
    public boolean patternMatches(String regex, CharSequence input) {
        return Pattern.matches(regex, input);
    }

    @Override
    public boolean matcherFind(Matcher matcher) {
        return matcher.find();
    }

    @Override
    public boolean matcherMatches(Matcher matcher) {
        return matcher.matches();
    }


}
