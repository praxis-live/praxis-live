
package net.neilcsmith.praxis.live.pxr.api;

import java.util.HashSet;
import java.util.Set;
import net.neilcsmith.praxis.core.ComponentType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Neil C Smith
 */
public class EditorUtilsTest {
    
    public EditorUtilsTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of extractBaseID method, of class EditorUtils.
     */
    @Test
    public void testExtractBaseID() {
        System.out.println("extractBaseID");
        ComponentType type = ComponentType.create("audio:testing:test");
        String expResult = "test";
        String result = EditorUtils.extractBaseID(type);
        assertEquals(expResult, result);
    }

    /**
     * Test of findFreeID method, of class EditorUtils.
     */
    @Test
    public void testFindFreeID() {
        System.out.println("findFreeID");
        Set<String> existing = new HashSet<String>();
        existing.add("delay1");
        existing.add("timing4-1");
        
        String baseID = "delay";
        boolean forceSuffix = false;
        String expResult = "delay";
        String result = EditorUtils.findFreeID(existing, baseID, forceSuffix);
        assertEquals(expResult, result);
        
        baseID = "delay";
        forceSuffix = true;
        expResult = "delay2";
        result = EditorUtils.findFreeID(existing, baseID, forceSuffix);
        assertEquals(expResult, result);
        
        baseID = "timing4-1";
        forceSuffix = false;
        expResult = "timing4-2";
        result = EditorUtils.findFreeID(existing, baseID, forceSuffix);
        assertEquals(expResult, result);
        
        baseID = "timing4-1";
        forceSuffix = true;
        expResult = "timing4-1-1";
        result = EditorUtils.findFreeID(existing, baseID, forceSuffix);
        assertEquals(expResult, result);
        
    }
}
