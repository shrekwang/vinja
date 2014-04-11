import junit.framework.TestCase;

public class MyTestCase extends TestCase {

   public static void main(String[] a){
       junit.textui.TestRunner.run(MyTestCase.class);
   }

   public void setUp() {
       //TODO
   }

   public void tearDown() {
       //TODO
   }
   

   public void testSomething() {
       String username = "lisa";
       assertEquals(username, "lisa");
       assertNotNull(username);
   }
}


