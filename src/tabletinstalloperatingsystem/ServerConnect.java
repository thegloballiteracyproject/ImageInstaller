/*
 * The MIT License (MIT)

Copyright (c) 2014 The Dalai Lama Center

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package tabletinstalloperatingsystem;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;


public class ServerConnect {
    
    private static String username, password;
    private static String urlAddress = "http://git.globallit.org/GenerateKeys.php";
    
    public void setUsername(String username)
    {
        this.username = username;
    }
    
    public void setPassword(String password)
    {
        this.password = password;
    }
    
    public String[] getSSHKeys(String username, String password, String serialId)
    {
        setPassword(password);
        setUsername(username);
        try {
            return getSSHKeys(serialId);
        }
        catch(Exception e){
            System.out.println(e);
        }
        return null;
    }
    
    public String[] getSSHKeys(String serialId) throws Exception
    {
        if(this.username.isEmpty() || this.password.isEmpty())
            throw new Exception("Undefined Username and Password");
        URL url = new URL(this.urlAddress);
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("username", this.username);
        params.put("password", this.password);
        params.put("serialId", serialId);
        
       String unformattedKey = new DataTransfer().postData(url, params);
       
       return parseKeys(unformattedKey);
    }

    public boolean checkCredentials(String usr, String passwd)
    {
        try {
            URL url = new URL(this.urlAddress);
            Map<String,Object> params = new LinkedHashMap<>();
            params.put("username", usr);
            params.put("password", passwd);

            String postResponse = new DataTransfer().postData(url, params);
            
            return Boolean.valueOf(postResponse);
                
        }
        catch(Exception e) {
            System.out.println("Exception in checkCredentials: " + e);
        }
        return false;
    }
    
    
    //Returns array: 0=PublicKey, 1=PrivateKey
    private String[] parseKeys(String keysToParse)
    {
        String[] unclearKeys = keysToParse.split(",");
        String[] clearKeys = new String[2];
        int i = 0;
        for(String s : unclearKeys)
        {
            clearKeys[i++] = s.split(":")[1].replace("\"", "");
        }
        return clearKeys;
    }
}