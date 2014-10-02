/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.jna;
import java.io.File;
import toniarts.opendungeonkeeper.jna.DecompressorDllHandle;
/**
 *
 * @author Wizand
 */
public class JnaTester {

    public static void main(String[] args) {
    
      
        
        char a = DecompressorDllHandle.testFunction();
        
        System.out.println(a);
        
    }
}
