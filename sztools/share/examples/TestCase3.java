import junit.framework.TestCase;

public class MyTestCase extends TestCase {

   public static void main(String[] a){
       junit.textui.TestRunner.run(MyTestCase.class);
   }

   protected void setUp() {
       //do some startup stuff here
   }

   public void testSomething() {
       String username = "lisa";
       assertEquals(username, "lisa");
       assertNotNull(username);
   }
}


