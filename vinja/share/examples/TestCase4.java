
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import static org.junit.Assert.assertEquals;


public class MyTest {

	@Before
	public void setUp() throws Exception {
	}
	@Test
	public void testInsert() throws Exception {
        assertEquals("Result", 50, tester.multiply(10, 5));
	}
	
    public static void main(String[] args) {
		Result result = JUnitCore.runClasses(MyClassTest.class);
		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
	}
    
}

