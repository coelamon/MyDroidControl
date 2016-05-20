package com.coel.mydroidcontrol;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    MyDroidClient m_client = null;
    ViewGroup m_formContainer = null;
    ViewGroup m_mainForm = null;
    ViewGroup m_connectForm = null;
    ViewGroup m_progressForm = null;
    ViewGroup m_errorForm = null;
    TextView m_ipTextView = null;
    TextView m_portTextView = null;
    TextView m_passwordTextView = null;
    TextView m_errorMessageTextView = null;
    Button m_connectButton = null;
    Button m_errorOkButton = null;
    Button m_moveForwardButton = null;
    Button m_moveBackwardButton = null;
    Button m_turnLeftButton = null;
    Button m_turnRightButton = null;
    SeekBar m_speedBar = null;
    MyDroidInput m_droidInput = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (m_client == null) {
            m_client = new MyDroidClient();
        }

        if (m_droidInput == null) {
            m_droidInput = new MyDroidInput();
        }

        m_formContainer = (RelativeLayout)findViewById(R.id.form_container);
        m_formContainer.setVisibility(View.GONE);

        m_mainForm = (ViewGroup)m_formContainer.findViewById(R.id.main_form);
        m_mainForm.setVisibility(View.GONE);
        m_moveForwardButton = (Button)m_mainForm.findViewById(R.id.button_fwd);
        m_moveBackwardButton = (Button)m_mainForm.findViewById(R.id.button_bwd);
        m_turnLeftButton = (Button)m_mainForm.findViewById(R.id.button_left);
        m_turnRightButton = (Button)m_mainForm.findViewById(R.id.button_right);
        m_speedBar = (SeekBar)m_mainForm.findViewById(R.id.seekbar_speed);

        m_connectForm = (ViewGroup)m_formContainer.findViewById(R.id.connect_form);
        m_connectForm.setVisibility(View.GONE);
        m_ipTextView = (TextView)m_connectForm.findViewById(R.id.input_ip);
        m_portTextView = (TextView)m_connectForm.findViewById(R.id.input_port);
        m_passwordTextView = (TextView)m_connectForm.findViewById(R.id.input_password);
        m_connectButton = (Button)m_connectForm.findViewById(R.id.button_connect);

        m_progressForm = (ViewGroup)m_formContainer.findViewById(R.id.progress_form);
        m_progressForm.setVisibility(View.GONE);

        m_errorForm = (ViewGroup)m_formContainer.findViewById(R.id.error_form);
        m_errorForm.setVisibility(View.GONE);
        m_errorMessageTextView = (TextView)m_errorForm.findViewById(R.id.output_error_message);
        m_errorOkButton = (Button)m_errorForm.findViewById(R.id.button_ok);

        m_formContainer.setVisibility(View.VISIBLE);

        m_client.setOnAuthListener(new MyDroidClient.OnAuthListener() {
            @Override
            public void onAuth(MyDroidClient cl, boolean ok) {
                if (!ok) {
                    showErrorForm("authentication failed");
                    return;
                }
                showMainForm();
            }
        });

        m_client.setOnAuthErrorListener(new MyDroidClient.OnAuthErrorListener() {
            @Override
            public void onAuthError(MyDroidClient cl, String errorMessage) {
                showErrorForm(errorMessage);
            }
        });

        m_client.setOnDisconnectListener(new MyDroidClient.OnDisconnectListener() {
            @Override
            public void onDisconnect(MyDroidClient cl) {
                showErrorForm("disconnected");
            }
        });

        m_ipTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    m_portTextView.requestFocus();
                    return true;
                }
                return false;
            }
        });

        m_portTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    m_passwordTextView.requestFocus();
                    return true;
                }
                return false;
            }
        });

        m_passwordTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == R.id.connect || actionId == EditorInfo.IME_NULL) {
                    m_passwordTextView.clearFocus();
                    hideSoftKeyboard();
                    attemptConnectToDroid();
                    return true;
                }
                return false;
            }
        });

        m_connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptConnectToDroid();
            }
        });

        m_errorOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginForm();
            }
        });

        m_moveForwardButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d("mydroidcontrol", "FWD DOWN");
                        m_droidInput.moveForward = true;
                        m_client.setInput(m_droidInput);
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d("mydroidcontrol", "FWD UP");
                        m_droidInput.moveForward = false;
                        m_client.setInput(m_droidInput);
                        break;
                }
                return false;
            }
        });

        m_moveBackwardButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        m_droidInput.moveBackward = true;
                        m_client.setInput(m_droidInput);
                        break;
                    case MotionEvent.ACTION_UP:
                        m_droidInput.moveBackward = false;
                        m_client.setInput(m_droidInput);
                        break;
                }
                return false;
            }
        });

        m_turnLeftButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        m_droidInput.turnLeft = true;
                        m_client.setInput(m_droidInput);
                        break;
                    case MotionEvent.ACTION_UP:
                        m_droidInput.turnLeft = false;
                        m_client.setInput(m_droidInput);
                        break;
                }
                return false;
            }
        });

        m_turnRightButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        m_droidInput.turnRight = true;
                        m_client.setInput(m_droidInput);
                        break;
                    case MotionEvent.ACTION_UP:
                        m_droidInput.turnRight = false;
                        m_client.setInput(m_droidInput);
                        break;
                }
                return false;
            }
        });

        m_speedBar.setMax(MyDroidProtocol.MAX_SPEED);
        m_speedBar.setProgress(m_droidInput.speed);

        m_speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                m_droidInput.speed = progress;
                m_client.setInput(m_droidInput);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        showLoginForm();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (m_client != null) {
            m_client.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected  void onPause() {
        super.onPause();

        if (m_client != null) {
            m_client.disconnect();
        }
        showLoginForm();
    }

    private void hideSoftKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    private void showLoginForm() {
        m_connectForm.setVisibility(View.VISIBLE);
        m_progressForm.setVisibility(View.GONE);
        m_errorForm.setVisibility(View.GONE);
        m_mainForm.setVisibility(View.GONE);
    }

    private void showProgressForm() {
        m_connectForm.setVisibility(View.GONE);
        m_progressForm.setVisibility(View.VISIBLE);
        m_errorForm.setVisibility(View.GONE);
        m_mainForm.setVisibility(View.GONE);
    }

    private void showErrorForm(String errorMessage) {
        m_connectForm.setVisibility(View.GONE);
        m_progressForm.setVisibility(View.GONE);
        m_errorForm.setVisibility(View.VISIBLE);
        m_mainForm.setVisibility(View.GONE);
        m_errorMessageTextView.setText(errorMessage);
    }

    private void showMainForm() {
        m_connectForm.setVisibility(View.GONE);
        m_progressForm.setVisibility(View.GONE);
        m_errorForm.setVisibility(View.GONE);
        m_mainForm.setVisibility(View.VISIBLE);
    }

    private void attemptConnectToDroid() {
        String host = m_ipTextView.getText().toString();
        String portStr = m_portTextView.getText().toString();
        String password = m_passwordTextView.getText().toString();

        m_ipTextView.setError(null);
        m_portTextView.setError(null);
        m_passwordTextView.setError(null);

        if (!isIpValid(host)) {
            m_ipTextView.setError("correct ip");
            m_ipTextView.requestFocus();
            return;
        }

        if (!isPortValid(portStr)) {
            m_portTextView.setError("correct port");
            m_portTextView.requestFocus();
            return;
        }

        if (!isPasswordValid(password)) {
            m_passwordTextView.setError("correct password");
            m_passwordTextView.requestFocus();
            return;
        }

        showProgressForm();
        int port = Integer.parseInt(portStr);
        connectToDroid(host, port, password);
    }

    private boolean isIpValid(String ip) {
        if (ip == null) {
            return false;
        }
        if (ip.length() == 0) {
            return false;
        }
        return true;
    }

    private boolean isPortValid(String portStr) {
        int i;
        try {
            i = Integer.parseInt(portStr);
        } catch(NumberFormatException ex) {
            return false;
        }
        if (i <= 0 || i > 65535) {
            return false;
        }
        return true;
    }

    private boolean isPasswordValid(String password) {
        if (password == null) {
            return true;
        }
        return password.length() <= 32;
    }

    private void connectToDroid(String host, int port, String password) {
        m_client.connect(host, port, password);
    }

}
