package com.example.umer.smsserver;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.util.Log;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static android.content.Context.WIFI_SERVICE;


public class SMSServer extends Thread
{

    private final String TAG = "SMSServer";
    private ServerSocket serverSocket;
    private int portNo;
    private Socket client;
    private JSONParser jsonParser;
    private Context context;
    private Activity activity;
    private String socketAddress;

    private String mobileNo;
    private String smsText;

    public SMSServer(Context context, int portNo) throws IOException
    {
        this.context = context;
        this.portNo = portNo;
        serverSocket = new ServerSocket();

        jsonParser = new JSONParser();

        serverSocket.bind(new InetSocketAddress(getAssignedIP(), portNo));

    }

    public String getMobileNo()
    {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo)
    {
        this.mobileNo = mobileNo;
    }

    public String getSmsText()
    {
        return smsText;
    }

    public void setSmsText(String smsText)
    {
        this.smsText = smsText;
    }

    public void stopSMSServer() throws IOException
    {

        serverSocket.close();
        serverSocket = null;
    }

    @Override
    public void run()
    {

        while (true) {
            try {
                Log.d(TAG, " SMSServer is waiting for connection at " + serverSocket.getLocalSocketAddress());
                client = serverSocket.accept();

                Log.d(TAG, "SMSServer connected to " + client.getRemoteSocketAddress());

                new SMSRequestHandler(client).start();

            } catch (IOException e) {
                e.printStackTrace();
                break;

            }
        }// while loop end

    }

    // invokes when $client->send(); is called from php script
    public void onSMSRequestRecevied(String mobileNo, String smsText)
    {


        if (mobileNo.length() > 0 && smsText.length() > 0) {
            TelephonyManager telMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            int simState = telMgr.getSimState();

            if (simState == TelephonyManager.SIM_STATE_READY) 
            {
            	//validate mobile number using regex yourself
                sendSMS(mobileNo,smsText);
            }
            else
            {
                JSONObject response = new JSONObject();
                response.put("error", true);
                response.put("message", "SIM not found at SMSServer");
                sendResponse(response);

            }
        }


    }

    public int getPortNo()
    {
        return portNo;
    }

    public void setPortNo(int portNo)
    {
        this.portNo = portNo;
    }

    private void sendResponse(JSONObject response)
    {
        try {
            new PrintStream(client.getOutputStream()).println(response.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String getJSONRequestString(Socket client)
    {

        BufferedReader buffreader;
        String jsonString = "";
        try {

            buffreader = new BufferedReader(new InputStreamReader(client.getInputStream()));

            while (buffreader.ready()) {
                jsonString += buffreader.readLine();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        return jsonString;


    }

    private String getAssignedIP()
    {

        WifiManager wifiMgr = (WifiManager) context.getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        String ipAddress = Formatter.formatIpAddress(ip);
        Log.d(TAG, "IP is " + ipAddress);
        return ipAddress;

    }



    private void sendSMS(String mobileNo, String smsText)
    {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(context,
                0, new Intent(DELIVERED), 0);

        // ---when the SMS has been sent---

        context.registerReceiver(new BroadcastReceiver()
        {

            @Override
            public void onReceive(Context arg0, Intent arg1)
            {

                // if sms is successfully sent
                if (getResultCode() == Activity.RESULT_OK)
                {
                    JSONObject response = new JSONObject();
                    response.put("error", false);
                    response.put("message", "SMS successfully sent !");
                    sendResponse(response);

                } else // if sms is not sent
                {
                    JSONObject response = new JSONObject();
                    response.put("error", true);
                    response.put("message", "Unable to send SMS");
                    sendResponse(response);
                }
            }
        }, new IntentFilter(SENT));

        // ---when the SMS has been delivered---
        context.registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context arg0, Intent arg1)
            {
                switch (getResultCode()) {
                    case Activity.RESULT_OK: // if sms is delivered
                    {
                        JSONObject response = new JSONObject();
                        response.put("error", false);
                        response.put("message", "SMS successfully delivered !");
                        sendResponse(response);
                    }
                        break;
                    case Activity.RESULT_CANCELED: // if sms is not delieved
                    {
                        JSONObject response = new JSONObject();
                        response.put("error", true);
                        response.put("message", "Unable to deliever SMS");
                        sendResponse(response);

                    }
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(mobileNo, null, smsText, sentPI, deliveredPI);

    }


    public String getSocketAddress()
    {
        return getAssignedIP() + ":" + getPortNo();
    }


    private class SMSRequestHandler extends Thread {

        private Socket clientSocket;

        public SMSRequestHandler(Socket clientSocket)
        {
            this.clientSocket = clientSocket;

        }

        @Override
        public void run()
        {

            try {
                // PrintWriter pw = new PrintWriter(client.getOutputStream());
                JSONObject clientRequest = (JSONObject) jsonParser.parse(getJSONRequestString(clientSocket));

                onSMSRequestRecevied((String) clientRequest.get("mobileNo"), (String) clientRequest.get("smsText"));

                Log.d(TAG, "request--> " + clientRequest.toJSONString());
                Log.d(TAG, "writing response");


                clientSocket.close();

            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

    }
    }

}