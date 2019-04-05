
public class LibIoC_DK {
	
	/***************************************************************/
	// check if the input value is a hash value; md5, sha1, or sha256 
	// @ inputs
	//    val: the input value
	// @ outputs:
	//    the hash type if the value is a hash, otherwise return "None".  
	/***************************************************************/
	
	static String isHash(String val){
	    int l = val.length();
	    if(l == 32)
	        return "MD5";
	    if(l == 40)
	        return "SHA1";
	    if(l == 64)
	        return "SHA256";
	    return "None";
	}

	
}
