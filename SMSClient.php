<?php


/**
 * Description of Sms
 *
 * @author Umer Farooq
 */
class SMSClient {
   
    private $smsServerIP;
    private $portNo;
    private $mobileNo;
    private $smsText;
    private $remoteSocket;
    private $serverResponse;
    
    
    public function setMobileNo($mobileNo)
    {
        $this->mobileNo = $mobileNo;   //user regex for checking num     
    }
    
    public function setSmsText($smsText)
    {
        $this->smsText = $smsText;
    }
    
    public function send()
    {
        $sms = array();
        $sms['mobileNo'] = $this->mobileNo;
        $sms['smsText'] = $this->smsText;
        
        fwrite($this->remoteSocket, json_encode($sms)."\n");
        
        $this->serverResponse = json_decode(stream_get_contents($this->remoteSocket),true);
        
        return $this->serverResponse['error']; // true or false
    }
    
    public function getErrorMessage()
    {
        return $this->serverResponse['message'];
    }
    
    public function connectToSMSServer($smsServerIP,$portNo)
    {
        $this->smsServerIP = $smsServerIP;
        $this->portNo = $portNo;
               
        $this->remoteSocket = @stream_socket_client("tcp://$smsServerIP:$portNo", $errno, $errorMessage,3) or die("sds");
      
        
    }
    
    public function __destruct() {
       
        if($this->remoteSocket != null)
            fclose($this->remoteSocket);
    }
    
    
    
}
