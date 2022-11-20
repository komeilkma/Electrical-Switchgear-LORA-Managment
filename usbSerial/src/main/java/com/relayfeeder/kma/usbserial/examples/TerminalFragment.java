package com.relayfeeder.kma.examples;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Switch;
import android.widget.Button;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.relayfeeder.kma.driver.UsbSerialDriver;
import com.relayfeeder.kma.driver.UsbSerialPort;
import com.relayfeeder.kma.driver.UsbSerialProber;
import com.relayfeeder.kma.util.HexDump;
import com.relayfeeder.kma.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;

public class TerminalFragment extends Fragment implements SerialInputOutputManager.Listener {



    private enum UsbPermission { Unknown, Requested, Granted, Denied }

    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 2000;
    String led11;
    String led22;
    String led33;
    String led44;
    String led55;
    String led66;
    String led77;
    String led88;
    String filer11;
    String filer22;
    String finalfiler;
    String finalvar;

    private int deviceId, portNum, baudRate;
    private boolean withIoManager;

    private final BroadcastReceiver broadcastReceiver;
    private final Handler mainLooper;

    private ControlLines controlLines;

    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;

    public TerminalFragment() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    connect();
                }
            }
        };
        mainLooper = new Handler(Looper.getMainLooper());
    }

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceId = getArguments().getInt("device");
        portNum = getArguments().getInt("port");
        baudRate = getArguments().getInt("baud");
        withIoManager = getArguments().getBoolean("withIoManager");
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));

        if(usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted)
            mainLooper.post(this::connect);
    }

    @Override
    public void onPause() {
        if(connected) {
            status("disconnected");
            disconnect();
            stopHandler();
        }
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        Button power = (Button) view.findViewById(R.id.power);




        Switch led1 = (Switch) view.findViewById(R.id.switch1);
        Switch led2 = (Switch) view.findViewById(R.id.switch2);
        Switch led3 = (Switch) view.findViewById(R.id.switch3);
        Switch led4 = (Switch) view.findViewById(R.id.switch4);
        Switch led5 = (Switch) view.findViewById(R.id.switch5);
        Switch led6 = (Switch) view.findViewById(R.id.switch6);
        Switch led7 = (Switch) view.findViewById(R.id.switch7);
        Switch led8 = (Switch) view.findViewById(R.id.switch8);
        Switch filer1 = (Switch) view.findViewById(R.id.switchfiler1);
        Switch filer2 = (Switch) view.findViewById(R.id.switchfiler2);



        finalvar = "$R2bbbbbbbb" ;
        finalfiler = "$F2bb" ;



        filer1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if ( ((Switch) view.findViewById(R.id.switchfiler1)).isChecked() == false ) {
                    filer11 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switchfiler1)).isChecked() == true ) {
                    filer2.setChecked(false);
                    filer11 = "a";
                }
                if ( ((Switch) view.findViewById(R.id.switchfiler2)).isChecked() == false ) {
                    filer22 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switchfiler2)).isChecked() == true ) {
                    filer22 = "a";
                }
                finalfiler = "$F2bb" ;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finalfiler = "$F1"+filer11+filer22 ;
                    }
                }, 2100);




            }
        });

        filer2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if ( ((Switch) view.findViewById(R.id.switchfiler2)).isChecked() == true ) {
                    filer1.setChecked(false);
                    filer22 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switchfiler1)).isChecked() == false ) {
                    filer11 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switchfiler1)).isChecked() == true ) {

                    filer11 = "b";
                }
                if ( ((Switch) view.findViewById(R.id.switchfiler2)).isChecked() == false ) {
                    filer22 = "b";
                }





                finalfiler = "$F2bb" ;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finalfiler = "$F1"+filer11+filer22 ;
                    }
                }, 2100);

            }
        });




        led1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == false ) {
                    led11 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == true ) {
                    led11 = "a";
                }
                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == false ) {
                    led22 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == true ) {
                    led22 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == false ) {
                    led33 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == true ) {
                    led33 = "a";
                }


                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == false ) {
                    led44 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == true ) {
                    led44 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == false ) {
                    led55 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == true ) {
                    led55 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == false ) {
                    led66 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == true ) {
                    led66 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == false ) {
                    led77 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == true ) {
                    led77 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == false ) {
                    led88 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == true ) {
                    led88 = "a";
                }

               finalvar = "$R2"+led11+led22+led33+led44+led55+led66+led77+led88 ;

            }
        });


        led2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == false ) {
                    led11 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == true ) {
                    led11 = "a";
                }
                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == false ) {
                    led22 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == true ) {
                    led22 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == false ) {
                    led33 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == true ) {
                    led33 = "a";
                }


                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == false ) {
                    led44 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == true ) {
                    led44 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == false ) {
                    led55 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == true ) {
                    led55 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == false ) {
                    led66 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == true ) {
                    led66 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == false ) {
                    led77 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == true ) {
                    led77 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == false ) {
                    led88 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == true ) {
                    led88 = "a";
                }

                finalvar = "$R2"+led11+led22+led33+led44+led55+led66+led77+led88 ;

            }
        });

        led3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == false ) {
                    led11 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == true ) {
                    led11 = "a";
                }
                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == false ) {
                    led22 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == true ) {
                    led22 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == false ) {
                    led33 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == true ) {
                    led33 = "a";
                }


                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == false ) {
                    led44 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == true ) {
                    led44 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == false ) {
                    led55 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == true ) {
                    led55 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == false ) {
                    led66 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == true ) {
                    led66 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == false ) {
                    led77 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == true ) {
                    led77 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == false ) {
                    led88 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == true ) {
                    led88 = "a";
                }

                finalvar = "$R2"+led11+led22+led33+led44+led55+led66+led77+led88 ;

            }
        });

        led4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == false ) {
                    led11 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == true ) {
                    led11 = "a";
                }
                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == false ) {
                    led22 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == true ) {
                    led22 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == false ) {
                    led33 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == true ) {
                    led33 = "a";
                }


                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == false ) {
                    led44 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == true ) {
                    led44 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == false ) {
                    led55 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == true ) {
                    led55 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == false ) {
                    led66 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == true ) {
                    led66 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == false ) {
                    led77 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == true ) {
                    led77 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == false ) {
                    led88 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == true ) {
                    led88 = "a";
                }

                finalvar = "$R2"+led11+led22+led33+led44+led55+led66+led77+led88 ;

            }
        });

        led5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == false ) {
                    led11 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == true ) {
                    led11 = "a";
                }
                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == false ) {
                    led22 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == true ) {
                    led22 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == false ) {
                    led33 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == true ) {
                    led33 = "a";
                }


                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == false ) {
                    led44 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == true ) {
                    led44 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == false ) {
                    led55 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == true ) {
                    led55 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == false ) {
                    led66 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == true ) {
                    led66 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == false ) {
                    led77 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == true ) {
                    led77 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == false ) {
                    led88 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == true ) {
                    led88 = "a";
                }

                finalvar = "$R2"+led11+led22+led33+led44+led55+led66+led77+led88 ;

            }
        });

        led6.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == false ) {
                    led11 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == true ) {
                    led11 = "a";
                }
                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == false ) {
                    led22 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == true ) {
                    led22 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == false ) {
                    led33 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == true ) {
                    led33 = "a";
                }


                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == false ) {
                    led44 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == true ) {
                    led44 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == false ) {
                    led55 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == true ) {
                    led55 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == false ) {
                    led66 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == true ) {
                    led66 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == false ) {
                    led77 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == true ) {
                    led77 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == false ) {
                    led88 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == true ) {
                    led88 = "a";
                }

                finalvar = "$R2"+led11+led22+led33+led44+led55+led66+led77+led88 ;

            }
        });



        led7.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == false ) {
                    led11 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == true ) {
                    led11 = "a";
                }
                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == false ) {
                    led22 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == true ) {
                    led22 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == false ) {
                    led33 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == true ) {
                    led33 = "a";
                }


                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == false ) {
                    led44 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == true ) {
                    led44 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == false ) {
                    led55 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == true ) {
                    led55 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == false ) {
                    led66 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == true ) {
                    led66 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == false ) {
                    led77 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == true ) {
                    led77 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == false ) {
                    led88 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == true ) {
                    led88 = "a";
                }

                finalvar = "$R2"+led11+led22+led33+led44+led55+led66+led77+led88 ;

            }
        });


        led8.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == false ) {
                    led11 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch1)).isChecked() == true ) {
                    led11 = "a";
                }
                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == false ) {
                    led22 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch2)).isChecked() == true ) {
                    led22 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == false ) {
                    led33 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch3)).isChecked() == true ) {
                    led33 = "a";
                }


                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == false ) {
                    led44 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch4)).isChecked() == true ) {
                    led44 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == false ) {
                    led55 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch5)).isChecked() == true ) {
                    led55 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == false ) {
                    led66 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch6)).isChecked() == true ) {
                    led66 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == false ) {
                    led77 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch7)).isChecked() == true ) {
                    led77 = "a";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == false ) {
                    led88 = "b";
                }

                if ( ((Switch) view.findViewById(R.id.switch8)).isChecked() == true ) {
                    led88 = "a";
                }

                finalvar = "$R2"+led11+led22+led33+led44+led55+led66+led77+led88 ;

            }
        });







        controlLines = new ControlLines(view);

        return view;
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {

            return true;
        } else if( id == R.id.send_break) {
            if(!connected) {
                Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    usbSerialPort.setBreak(true);
                    Thread.sleep(100); // should show progress bar instead of blocking UI thread
                    usbSerialPort.setBreak(false);
                    SpannableStringBuilder spn = new SpannableStringBuilder();
                    spn.append("send <break>\n");
                    spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                } catch(UnsupportedOperationException ignored) {
                    Toast.makeText(getActivity(), "BREAK not supported", Toast.LENGTH_SHORT).show();
                } catch(Exception e) {
                    Toast.makeText(getActivity(), "BREAK failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial
     */
    @Override
    public void onNewData(byte[] data) {
        mainLooper.post(() -> {
            receive(data);
        });
    }

    @Override
    public void onRunError(Exception e) {
        mainLooper.post(() -> {
            status("connection lost: " + e.getMessage());
            disconnect();
            stopHandler();
        });
    }

    /*
     * Serial + UI
     */
    private void connect() {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        for(UsbDevice v : usbManager.getDeviceList().values())
            if(v.getDeviceId() == deviceId)
                device = v;
        if(device == null) {
            status("connection failed: device not found");
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if(driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if(driver == null) {
            status("connection failed: no driver for device");
            return;
        }
        if(driver.getPorts().size() < portNum) {
            status("connection failed: not enough ports at device");
            return;
        }
        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = UsbPermission.Requested;
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(INTENT_ACTION_GRANT_USB), flags);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                status("connection failed: permission denied");
            else
                status("connection failed: open failed");
            return;
        }

        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);
            if(withIoManager) {
                usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
                usbIoManager.start();
            }
            status("connected");
            connected = true;
            controlLines.start();
            send("KK");
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    send("KK");
                    startHandler();
                }
            }, 1000); // Millisecond 1000 = 1 sec
        } catch (Exception e) {
            status("connection failed: " + e.getMessage());
            disconnect();
            stopHandler();
        }
    }
//////////////////////////////////////////////////////////////




    private Handler taskHandler = new android.os.Handler();

    private Runnable repeatativeTaskRunnable = new Runnable() {
        public void run() {
           send(finalvar);
           new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    send(finalfiler);
                }
            }, 1000);
            startHandler();
        }
    };

    void startHandler() {
        taskHandler.postDelayed(repeatativeTaskRunnable, 2000);
    }

    void stopHandler() {
        taskHandler.removeCallbacks(repeatativeTaskRunnable);
    }


////////////////////////////////////////////////////////////
    private void disconnect() {
        connected = false;
        controlLines.stop();
        if(usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            usbSerialPort.close();
        } catch (IOException ignored) {}
        usbSerialPort = null;
    }

    private void send(String str) {
        if(!connected) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] data = (str).getBytes();
            SpannableStringBuilder spn = new SpannableStringBuilder();
            //spn.append("send " + data.length + " bytes\n");
            //spn.append(HexDump.dumpHexString(data)).append("\n");
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            usbSerialPort.write(data, WRITE_WAIT_MILLIS);
        } catch (Exception e) {
            onRunError(e);
        }
    }

    private void read() {
        if(!connected) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] buffer = new byte[8192];
            int len = usbSerialPort.read(buffer, READ_WAIT_MILLIS);
            receive(Arrays.copyOf(buffer, len));
        } catch (IOException e) {
            // when using read with timeout, USB bulkTransfer returns -1 on timeout _and_ errors
            // like connection loss, so there is typically no exception thrown here on error
            status("connection lost: " + e.getMessage());
            disconnect();
            stopHandler();
        }
    }

    private void receive(byte[] data) {
        SpannableStringBuilder spn = new SpannableStringBuilder();
        //spn.append("receive " + data.length + " bytes\n");

        //if (((Switch)  getView().findViewById(R.id.switch7)).isChecked() == true) {
        //    status("mishe");
        // }
        if (data.length > 0)
            spn.append(HexDump.dumpHexString(data) + "\n");

        //status(HexDump.toHexString(data));
        //status(data.toString());
        //String[] separated = HexDump.toHexString(data).split("(?<=\\G..)");
        String separated = HexDump.toHexString(data);
        //String[] lettersfrom  = {"52", "46", "4C", "62", "61"};
        //status(separated[5]);
        //spn.append("receivtte " + separated[1].length() + " bytes\n");

        if (separated.substring(2, 4).equals("46")) {
            if ( separated.length() > 17  ) {
            if (separated.substring(10, 12).equals("4C")) {


                Switch switchfider1 = (Switch) getView().findViewById(R.id.switchfiler1);
                Switch switchfider2 = (Switch) getView().findViewById(R.id.switchfiler2);
                Button f1_button = (Button) getView().findViewById(R.id.power);
                f1_button.setText("LOCAL");
                switchfider1.setClickable(false);
                switchfider2.setClickable(false);
                switchfider1.setAlpha(0.5f);
                switchfider2.setAlpha(0.5f);
            } else {
                Switch switchfider1 = (Switch) getView().findViewById(R.id.switchfiler1);
                Switch switchfider2 = (Switch) getView().findViewById(R.id.switchfiler2);
                Button f1_button = (Button) getView().findViewById(R.id.power);
                f1_button.setText("REMOTE");
                switchfider1.setClickable(true);
                switchfider2.setClickable(true);
                switchfider1.setAlpha(1f);
                switchfider2.setAlpha(1f);

            }
            if (separated.substring(12, 14).equals("4C")) {
                Button f2_button = (Button) getView().findViewById(R.id.fualt);
                f2_button.setBackgroundResource(R.color.colorgreen);


            } else {
                Button f2_button = (Button) getView().findViewById(R.id.fualt);
                f2_button.setBackgroundResource(R.color.colorred);


            }
            if (separated.substring(14, 16).equals("4C")) {
                Button f3_button = (Button) getView().findViewById(R.id.fire);
                f3_button.setBackgroundResource(R.color.colorgreen);


            } else {
                Button f3_button = (Button) getView().findViewById(R.id.fire);
                f3_button.setBackgroundResource(R.color.colorred);


            }
            if (separated.substring(16, 18).equals("4C")) {
                Button f4_button = (Button) getView().findViewById(R.id.battery);
                f4_button.setBackgroundResource(R.color.colorgreen);


            } else {
                Button f4_button = (Button) getView().findViewById(R.id.battery);
                f4_button.setBackgroundResource(R.color.colorred);


            }

        }
        }

        if (separated.substring(2, 4).equals("52")) {
            if ( separated.length() > 19  ) {
            if (separated.substring(6, 8).equals("62")) {

                Button r1_button = (Button) getView().findViewById(R.id.button);
                r1_button.setText("Relay-1 Closed");
                r1_button.setBackgroundResource(R.color.colorred);
                r1_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel_white_24dp, 0, 0, 0);

            } else {

                Button r1_button = (Button) getView().findViewById(R.id.button);
                r1_button.setText("Relay-1 opend");
                r1_button.setBackgroundResource(R.color.colorgreen);
                r1_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle_white_24dp, 0, 0, 0);

            }
            if (separated.substring(8, 10).equals("62")) {
                Button r2_button = (Button) getView().findViewById(R.id.button2);
                r2_button.setText("Relay-2 Closed");
                r2_button.setBackgroundResource(R.color.colorred);
                r2_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel_white_24dp, 0, 0, 0);

            } else {
                Button r2_button = (Button) getView().findViewById(R.id.button2);
                r2_button.setText("Relay-2 opend");
                r2_button.setBackgroundResource(R.color.colorgreen);
                r2_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle_white_24dp, 0, 0, 0);

            }
            if (separated.substring(10, 12).equals("62")) {
                Button r3_button = (Button) getView().findViewById(R.id.button3);
                r3_button.setText("Relay-3 Closed");
                r3_button.setBackgroundResource(R.color.colorred);
                r3_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel_white_24dp, 0, 0, 0);

            } else {
                Button r3_button = (Button) getView().findViewById(R.id.button3);
                r3_button.setText("Relay-3 opend");
                r3_button.setBackgroundResource(R.color.colorgreen);
                r3_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle_white_24dp, 0, 0, 0);
            }
            if (separated.substring(12, 14).equals("62")) {
                Button r4_button = (Button) getView().findViewById(R.id.button4);
                r4_button.setText("Relay-4 Closed");
                r4_button.setBackgroundResource(R.color.colorred);
                r4_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel_white_24dp, 0, 0, 0);

            } else {
                Button r4_button = (Button) getView().findViewById(R.id.button4);
                r4_button.setText("Relay-4 opend");
                r4_button.setBackgroundResource(R.color.colorgreen);
                r4_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle_white_24dp, 0, 0, 0);

            }
            if (separated.substring(14, 16).equals("62")) {
                Button r5_button = (Button) getView().findViewById(R.id.button5);
                r5_button.setText("Relay-5 Closed");
                r5_button.setBackgroundResource(R.color.colorred);
                r5_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel_white_24dp, 0, 0, 0);

            } else {
                Button r5_button = (Button) getView().findViewById(R.id.button5);
                r5_button.setText("Relay-5 opend");
                r5_button.setBackgroundResource(R.color.colorgreen);
                r5_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle_white_24dp, 0, 0, 0);
            }
            if (separated.substring(16, 18).equals("62")) {
                Button r6_button = (Button) getView().findViewById(R.id.button6);
                r6_button.setText("Relay-6 Closed");
                r6_button.setBackgroundResource(R.color.colorred);
                r6_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel_white_24dp, 0, 0, 0);

            } else {
                Button r6_button = (Button) getView().findViewById(R.id.button6);
                r6_button.setText("Relay-6 opend");
                r6_button.setBackgroundResource(R.color.colorgreen);
                r6_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle_white_24dp, 0, 0, 0);
            }
            if (separated.substring(18, 20).equals("62")) {

                Button r7_button = (Button) getView().findViewById(R.id.button7);
                r7_button.setText("Relay-7 Closed");
                r7_button.setBackgroundResource(R.color.colorred);
                r7_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel_white_24dp, 0, 0, 0);

            } else {
                Button r7_button = (Button) getView().findViewById(R.id.button7);
                r7_button.setText("Relay-7 opend");
                r7_button.setBackgroundResource(R.color.colorgreen);
                r7_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle_white_24dp, 0, 0, 0);
            }

          /*  if (separated.substring(20,22).equals("62")){
                Button r8_button = (Button)getView().findViewById(R.id.button8);
                r8_button.setText("Relay-8 Closed");
                r8_button.setBackgroundResource(R.color.colorred);

            }else {
                Button r8_button = (Button)getView().findViewById(R.id.button8);
                r8_button.setText("Relay-8 opend");
                r8_button.setBackgroundResource(R.color.colorgreen);
            } */


        }

    }
         }





    void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

    }

    class ControlLines {
        private static final int refreshInterval = 200; // msec

        private final Runnable runnable;


        ControlLines(View view) {
            runnable = this::run; // w/o explicit Runnable, a new lambda would be created on each postDelayed, which would not be found again by removeCallbacks


        }

        private void toggle(View v) {
            ToggleButton btn = (ToggleButton) v;
            if (!connected) {
                btn.setChecked(!btn.isChecked());
                Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            String ctrl = "";

        }

        private void run() {
            if (!connected)
                return;
            try {
                EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getControlLines();

                mainLooper.postDelayed(runnable, refreshInterval);
            } catch (IOException e) {
                status("getControlLines() failed: " + e.getMessage() + " -> stopped control line refresh");
            }
        }

        void start() {
            if (!connected)
                return;
            try {
                EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getSupportedControlLines();

                run();
            } catch (IOException e) {
                Toast.makeText(getActivity(), "getSupportedControlLines() failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        void stop() {
            mainLooper.removeCallbacks(runnable);

        }
    }
}
