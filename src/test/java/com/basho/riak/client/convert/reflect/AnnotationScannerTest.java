package com.basho.riak.client.convert.reflect;


import com.basho.riak.client.RiakLink;
import com.basho.riak.client.convert.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class AnnotationScannerTest {

    @Test
    public void testBasicClass() {
        AnnotationInfo info = AnnotationHelper.getInstance().get(BasicClass.class);
        BasicClass instance = new BasicClass();
        assertEquals("keyValue", info.getRiakKey(instance));
        assertEquals("clock", info.getRiakVClock(instance).asString());
        assertEquals(1, info.getUsermetaData(instance).size());
        assertEquals(1, info.getIndexes(instance).getBinIndex("myBinIndex").size());
        assertEquals(1, info.getIndexes(instance).getIntIndex("myIntIndex").size());
        assertEquals(1, info.getLinks(instance).size());

        info = AnnotationHelper.getInstance().get(MethodClass.class);
        MethodClass mInstance = new MethodClass();
        info.setRiakKey(mInstance, "keyValue");
        assertEquals("keyValue", info.getRiakKey(mInstance));
    }

    @Test
    public void testMethodClass() {
        AnnotationInfo info = AnnotationHelper.getInstance().get(MethodClass.class);
        MethodClass mInstance = new MethodClass();
        info.setRiakKey(mInstance, "keyValue");
        assertEquals("keyValue", info.getRiakKey(mInstance));
    }
    
    public class BasicClass {

        @RiakKey
        private String key = "keyValue";
        @RiakVClock
        private byte[] vClock = "clock".getBytes();
        @RiakUsermeta
        private Map<String, String> usermetaData = new HashMap<>();
        @RiakIndex(name="myBinIndex")
        private String stringIndex = "indexValue";
        @RiakIndex(name="myIntIndex")
        private int intIndex = 3;
        @RiakLinks
        private Collection<RiakLink> links = new HashSet<>();

        public BasicClass() {
            usermetaData.put("foo", "bar");
            links.add(new RiakLink("foo", "foo", "foo"));
        }

    }

    public class MethodClass {
        
        private String key;
        
        @RiakKey
        public void setKey(String key) {
            this.key = key;
        }
        
        @RiakKey
        public String getKey() {
            return key;
        }
    }
    
    @Test
    public void testSimpleInheritance() {
        AnnotationInfo info = AnnotationHelper.getInstance().get(ChildClass.class);
        ChildClass instance = new ChildClass();
        assertEquals("keyValue", info.getRiakKey(instance));
        assertEquals("clock", info.getRiakVClock(instance).asString());
    }

    public class ChildClass extends ParentClass {
        @RiakVClock
        private byte[] vClock = "clock".getBytes();
    }

    public class ParentClass {
        @RiakKey
        private String key = "keyValue";
    }

}
