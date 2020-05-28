package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.*;
import org.evomaster.client.java.instrumentation.mongo.MongoReplacementClass;
import org.evomaster.client.java.instrumentation.shared.ClassName;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReplacementList {

    public static List<MethodReplacementClass> getList() {
        return Arrays.asList(
                new BooleanClassReplacement(),
                new ByteClassReplacement(),
                new CharacterClassReplacement(),
                new CollectionClassReplacement(),
                new DateClassReplacement(),
                new DateFormatClassReplacement(),
                new DoubleClassReplacement(),
                new FloatClassReplacement(),
                new GsonClassReplacement(),
                new HttpServletRequestClassReplacement(),
                new IntegerClassReplacement(),
                new LocalDateClassReplacement(),
                new LocalDateTimeClassReplacement(),
                new LocalTimeClassReplacement(),
                new LongClassReplacement(),
                new MapClassReplacement(),
                new MatcherClassReplacement(),
                new ObjectsClassReplacement(),
                new PatternClassReplacement(),
                new StringClassReplacement(),
                new ShortClassReplacement(),
                new ServletRequestClassReplacement(),
                new WebRequestClassReplacement(),
                new MongoReplacementClass()
        );
    }


    public static List<MethodReplacementClass> getReplacements(String target) {
        Objects.requireNonNull(target);
        final String targetClassName = ClassName.get(target).getFullNameWithDots();

        return getList().stream()
                .filter(t -> t.isAvailable())
                .filter(t -> {
                    /*
                        TODO: this is tricky, due to how "super" calls are
                        handled. For now, we just allow subclasses if they
                        are of standard JDK.
                        Furthermore, issues with classloading of non-JDK APIs

                        This gives major issues if class loads other non-JDK classes.
                        This for example happens with SQL stuff possibly loading drivers, eg H2.
                        So we cannot load JDK libraries indiscriminately here.
                    */

//                            boolean jdk = targetClassName.startsWith("java.");
                            //TODO based on actual packages used in the list
                            boolean jdk = targetClassName.startsWith("java.lang.") ||
                                    targetClassName.startsWith("java.util.") ||
                                    targetClassName.startsWith("java.time.");

                            if (jdk) {
                                Class<?> klass;
                                try {
                                    klass = Class.forName(targetClassName);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return t.getTargetClass().isAssignableFrom(klass);
                            }

                            return t.getTargetClassName().equals(targetClassName);
                        }
                )
                .collect(Collectors.toList());
    }
}
