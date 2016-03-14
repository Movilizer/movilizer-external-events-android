package com.movilizer.eedemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    public final static String EVT_SOURCE_ID_KEY = "evtSrcId";
    public static final String JSON_PLACEHOLDER_KEY = "JSON";
    private static int         EVT_SOURCE_ID     = 123;

    private EditText _sendMessage;
    private Spinner _spinnerEventType;
    private Spinner _appIdSpinner;
    private ArrayList<String> _appIdList;
    private TextView _lastSentEvent;
    private Button _sendButton;
    private EditText _eventSourceId;
    private ArrayList<String> _eventTypeList;
    private Messenger          messenger         = null;                                 //used to make an RPC invocation
    private Messenger          replyTo           = null;                                 //invocation replies are processed by this Messenger
    private boolean            isBound           = false;
    private ServiceConnection  connection;                                               //receives callbacks from bind and unbind invocations
    private Message message;
    private int counter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _sendMessage = (EditText) findViewById(R.id.sendMessage);

        _eventSourceId = (EditText) findViewById(R.id.eventSourceId);
        _eventSourceId.setInputType(InputType.TYPE_CLASS_NUMBER);
        _eventSourceId.setSelectAllOnFocus(true);
        _eventSourceId.clearFocus();

        _lastSentEvent = (TextView) findViewById(R.id.lastEventInfo);

        _sendButton = (Button) findViewById(R.id.sendButton);
        _sendButton.setOnClickListener(this);

        _appIdSpinner = (Spinner) findViewById(R.id.appIdSpinner);
        _appIdList = new ArrayList<String>();

        // Add two app Ids - Pro and Demo apps
        _appIdList.add("com.movilizer.client.android.app");
        _appIdList.add("com.movilizer.client.android.app.demo");
        _appIdList.add("com.movilizer.client.android.app.d");

        final ArrayAdapter<String> appIdDataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, this._appIdList);
        appIdDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        _appIdSpinner.setAdapter(appIdDataAdapter);
        _appIdSpinner.setSelection(1);


        _spinnerEventType = (Spinner) findViewById(R.id.eventTypeSpinner);
        _eventTypeList = new ArrayList<String>();
        // 3 different types of events
        _eventTypeList.add("0 - Synchronous");
        _eventTypeList.add("1 - Asynchronous Guaranteed");
        _eventTypeList.add("2 - Asynchronous");

        final ArrayAdapter<String> eventTypeDataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, this._eventTypeList);
        eventTypeDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        _spinnerEventType.setAdapter(eventTypeDataAdapter);

        counter = 0;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sendButton:
                doSendMessage();
                break;
            default:
                break;
        }
    }

    /**
     * Method to fetch the UI data and send the message to Movilizer
     * using Intent
     */
    private void doSendMessage() {

        this.connection = new RemoteServiceConnection();
        this.replyTo = new Messenger(new IncomingHandler());

        message = Message.obtain();

        Intent serviceIntent = new Intent("com.movilizer.client.android.EXT_EVENT");
        serviceIntent.setPackage((String) this._appIdSpinner.getSelectedItem());

        final String eventSourceIdString = this._eventSourceId.getText().toString();
        int evntSrcId = EVT_SOURCE_ID;
        if (!eventSourceIdString.isEmpty())
        {
            evntSrcId = Integer.valueOf(eventSourceIdString);
            serviceIntent.putExtra(EVT_SOURCE_ID_KEY, Integer.valueOf(evntSrcId));
        }
        else
        {
            this._eventSourceId.setText(evntSrcId);
            serviceIntent.putExtra(EVT_SOURCE_ID_KEY, evntSrcId);
        }
        final String val = (String) _spinnerEventType.getSelectedItem();
        final byte evtType = Byte.parseByte(val.substring(0, 1));

        //Set the ReplyTo Messenger for processing the invocation response
        final Bundle data = new Bundle();
        data.putString(JSON_PLACEHOLDER_KEY, _sendMessage.getText().toString());

        message.what = evntSrcId;
        message.arg1 = counter++;
        message.arg2 = evtType;
        message.replyTo = replyTo;
        message.setData(data);

        _lastSentEvent.setText("EventSourceID=" + message.what + " EventID=" + message.arg1 + " EventType=" + message.arg2 + " JSONString=" + _sendMessage.getText().toString());

        this.bindService(serviceIntent, this.connection, Context.BIND_AUTO_CREATE);
    }

    private class RemoteServiceConnection implements ServiceConnection
    {
        @Override
        public void onServiceConnected(ComponentName component, IBinder binder)
        {
            messenger = new Messenger(binder);
            isBound = true;
            //Make the invocation
            try {
                messenger.send(message);
            } catch (RemoteException e) {
                _lastSentEvent.setText("Exception occured:" + e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component)
        {
            messenger = null;
            isBound = false;
        }
    }
    private class IncomingHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
            int what = msg.what;
            _lastSentEvent.setText("EventID=" + msg.arg1 + " StatusCode=" + msg.arg2);
        }
    }
}
