# Sending sms from android device using PHP script
An example showing PHP script sending sms using android device
Machine running php script and android device must be on the same network
# PHP side
```PHP
$client = new SMSClient();

try{

$exampleIP = "10.0.0.1";
$examplePort = 1234;

$client->connectToSMSServer($exampleIP, $examplePort);

}catch(Exception $e)
{
    echo "error! ".$e->getMessage();
    die();
}

$exampleMobileNo = "123456789"; // mobile phone no
$exampleMessage = "hello...";   // text message

//Make sure to validate Mobile No using regular expression. If invalid mobilePhoneNo then do other stuff


//If phone no is OK then 
$client->setMobileNo($exampleMobileNo);
$client->setSmsText($exampleMessage);
$error = $client->send();
```

# Android side
```java
SMSServer smsServer = new SMSServer(getApplicationContext(),1234);

//To start
smsServer.start();
..
..
..
//To stop
smsServer.stopSMSServer();
```
