/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.jna;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import java.io.File;
import toniarts.opendungeonkeeper.tools.convert.AssetsConverter;

    
/**
 *
 * @author Petteri Loisko
 * 
 */
   

public class DecompressorDllHandle {
     
    private static final String DLL_PATH = AssetsConverter.getCurrentFolder() + "libs\\";
    private static final String DLL_NAME = "Decompressor";
    private static final String DLL = "";
     
  static {
      
//        String myPath = System.getProperty("user.dir");
//        System.setProperty("java.library.path", myPath);
//      
//      
//        File f = new File ("libs\\Decompressor.dll");
//        System.out.println(":: " + f.isFile() + "    " + DLL_PATH);
//        NativeLibrary.addSearchPath(DLL_NAME, DLL_PATH);
//        DLL = DLL_PATH + DLL_NAME;
//        
//        Native.register(DLL);
    }
           
  // LIST OF DLL FUNCTION PROTOTYPES
            
// char testFunction( void ) 
public static native char testFunction();
    
// uint32_t bs_read( uint32_t pos, int bits )
public static native long bs_read( long value, long pos);
   
// uint32_t prepare_decompress( uint32_t value, uint32_t pos )
public static native long prepare_decompress( long value, long pos);

// void decompress_func1( int32_t *in, int32_t *out )
public static native void decompress_func1( Memory in, Memory out); 

//void decompress_func2( int32_t *in, int32_t *out )
public static native void decompress_func2(Memory in, Memory out);       

//int clamp( int n, int min, int max )
public static native int clamp(int n, int min, int max);

// void decompress( void )


// void initialize_dd( void *buf )

//void decompress_block( uint8_t *out, uint16_t stride )

//void dd_init( void )

//void dd_texture( uint32_t *buf, void *out, uint32_t stride, uint16_t width, uint16_t height )

    
//    public static native int open();
//    public static native int close(int handle);
//    public static native int write(Memory data, int size, int handle);
//    public static native int writeByte(byte data, int handle);  
//    public static native int readByte(Memory data, int handle);
//    public static native void flush(int handle);
     
     
  
     
     
}

