package org.evomaster.client.java.instrumentation;

import com.foo.somedifferentpackage.examples.triangle.TriangleClassificationImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InstrumentingClassLoaderTest {

    @Test
    public void testInstrumentClass() throws ClassNotFoundException {
        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");
        Class<?> triangleClass = cl.loadClass(TriangleClassificationImpl.class.getName());
        assertNotNull(triangleClass);
    }

    @Test
    public void testTwoCLassLoaders() throws ClassNotFoundException {
        InstrumentingClassLoader cl0 = new InstrumentingClassLoader("com.foo");
        Class<?> triangleClass0 = cl0.loadClass(TriangleClassificationImpl.class.getName());
        assertNotNull(triangleClass0);
        assertEquals(cl0, triangleClass0.getClassLoader());

        InstrumentingClassLoader cl1 = new InstrumentingClassLoader("com.foo");
        Class<?> triangleClass1 = cl1.loadClass(TriangleClassificationImpl.class.getName());
        assertNotNull(triangleClass1);
        assertEquals(cl1, triangleClass1.getClassLoader());

        assertNotEquals(triangleClass0,triangleClass1);
        assertNotEquals(cl0,cl1);

    }

    @Test
    public void testParentChild() throws ClassNotFoundException {
        InstrumentingClassLoader cl0 = new InstrumentingClassLoader("com.foo");
        Class<?> triangleClass0 = cl0.getParent().loadClass(TriangleClassificationImpl.class.getName());
        assertNotNull(triangleClass0);
        assertEquals(cl0.getParent(), triangleClass0.getClassLoader());

        Class<?> triangleClass1 = cl0.loadClass(TriangleClassificationImpl.class.getName());
        assertNotNull(triangleClass1);
        assertEquals(cl0, triangleClass1.getClassLoader());

        assertNotEquals(triangleClass0,triangleClass1);
        assertNotEquals(cl0, cl0.getParent());

    }

}
