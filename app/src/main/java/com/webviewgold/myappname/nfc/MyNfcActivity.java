package com.webviewgold.myappname.nfc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.webviewgold.myappname.BuildConfig;

import java.io.IOException;
import java.io.UnsupportedEncodingException;


public class MyNfcActivity extends AppCompatActivity {

    public static void sendIntent(Context context) {
        Intent intent = new Intent(context, MyNfcActivity.class);
        context.startActivity(intent);
    }


    public static final int PERMISSION_REQUEST_CODE = 9541;
    public static final String ERROR_DETECTED = "No NFC tag detected!";
    public static final String WRITE_SUCCESS = "Text written to the NFC tag successfully!";
    public static final String WRITE_ERROR = "Error during writing, is the NFC tag close enough to your device?";

    private static final String TAG = "MyNfcActivityTAG";


    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter writeTagFilters[];
    private boolean writeMode;
    private Tag myTag;
    private Context context;



    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            read(msgs);
        }
    }

    private void read(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;

        String text = "";
//        String tagId = new String(msgs[0].getRecords()[0].getType());
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");

        try {
            // Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            if (BuildConfig.IS_DEBUG_MODE) Log.d("UnsupportedEncoding", e.toString());
        }

        TextView textView = new TextView(this);
        textView.setPadding(16,16,16,16);
        textView.setTextColor(Color.BLUE);
        textView.setText("read : " + text);

    }

    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = {createRecord(text)};
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
//        Ndef ndef = Ndef.get(tag);
//        if (ndef != null) {
//
//            try {
//                ndef.connect();
//                if (ndef.isConnected()){
//                    ndef.writeNdefMessage(message);
//                    toast(WRITE_SUCCESS);
//                }else {
//                    if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "not connected");
//                    toast("not connected");
//                }
//            } catch (Exception e) {
//                if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "write error : " + e.getMessage());
//                toast("write error : " + e.getMessage());
//            } finally {
//                try {
//                    ndef.close();
//                } catch (Exception e) {
//                }
//            }
//        }else  {
//            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "ndef is null");
//            toast("ndef is null");
//        }


        writeData(tag , message);

    }

    public void writeData(Tag tag, NdefMessage message)  {
        if (tag != null) {
            try {
                Ndef ndefTag = Ndef.get(tag);
                if (ndefTag == null) {
                    // Let's try to format the Tag in NDEF
                    NdefFormatable nForm = NdefFormatable.get(tag);
                    if (nForm != null) {
                        nForm.connect();
                        nForm.format(message);
                        nForm.close();
                        toast(WRITE_SUCCESS);
                    }
                }
                else {
                    ndefTag.connect();
                    ndefTag.writeNdefMessage(message);
                    ndefTag.close();
                    toast(WRITE_SUCCESS);
                }
            }
            catch(Exception e) {
                e.printStackTrace();
                toast("write error : " + e.getMessage());
            }
        }
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);

        return recordNFC;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readFromIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            toast("tag detected : " + myTag.toString());


            try {
                write("test_write", myTag);
            } catch (IOException | FormatException e) {
                e.printStackTrace();
                Toast.makeText(context, WRITE_ERROR, Toast.LENGTH_LONG).show();
            }
        }
    }


    private void WriteModeOn() {
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    private void WriteModeOff() {
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void toast(String message){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onPause() {
        super.onPause();
        WriteModeOff();
    }

    @Override
    public void onResume() {
        super.onResume();
        WriteModeOn();
    }
}