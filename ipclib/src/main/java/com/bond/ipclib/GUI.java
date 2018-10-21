package com.bond.ipclib;

import android.content.Context;
import android.graphics.Typeface;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;

import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


public class GUI extends FrameLayout {
    ScrollView scrollView;
    Papirus papirus;
    TextView txtTittle;
    Button btnStartStop;
    boolean ipcState = false;
    TextInputLayout edtSendTextL;
    TextInputEditText edtSendTextE;
    Button btnSendShared;
    Button btnSendUnixSock;
    Button btnSendALoop;
    TextView txtResult;
    Button btnGetShared;
    Button btnGetUnixSock;
    Button btnGetALoop;
    int maxMsgSize = 0;
    String msgToSend = "";


    public GUI(Context context) {
        super(context);
        createContent(context);
    }

    public GUI(Context context, AttributeSet attrs) {
        super(context, attrs);
        createContent(context);
    }

    public GUI(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        createContent(context);
    }

    public void createContent(Context context) {
        scrollView = new ScrollView(context);
        papirus = new Papirus(context);
        scrollView.addView(papirus, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));

        txtTittle = new TextView(context);
        txtTittle.setSingleLine(true);
        txtTittle.setMaxLines(1);
        txtTittle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        txtTittle.setEllipsize(TextUtils.TruncateAt.END);
        txtTittle.setTextColor(0xff000000);
        txtTittle.setTypeface(null, Typeface.BOLD);
        papirus.addView(txtTittle, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        btnStartStop = new Button(context);
        btnStartStop.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        btnStartStop.setTypeface(null, Typeface.BOLD);
        btnStartStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnStartStop();
            }
        });
        papirus.addView(btnStartStop, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        edtSendTextL = new TextInputLayout(context);
        papirus.addView(edtSendTextL,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        //edt_AvarNameE = new EditText(context);//(EditText) findViewById(R.id.edt_AvarNameE);
        edtSendTextE = new TextInputEditText(context);//(EditText) findViewById(R.id.edt_AvarNameE);
        edtSendTextE.setSingleLine(true);
        edtSendTextE.setMaxLines(1);
        edtSendTextE.setHint("enter text to send");
        edtSendTextE.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        LinearLayout.LayoutParams lLayout = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        edtSendTextL.addView(edtSendTextE, lLayout);
        edtSendTextE.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (str.getBytes().length> maxMsgSize) {
                    edtSendTextE.setHint("text is too long!!!");
                } else {
                    edtSendTextE.setHint("enter text to send");
                    msgToSend = str;
                }
            }
        });

        btnSendShared = new Button(context);
        btnSendShared.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        btnSendShared.setTypeface(null, Typeface.BOLD);
        btnSendShared.setText("Send via shared mem");
        btnSendShared.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnSendShared();
            }
        });
        papirus.addView(btnSendShared, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        btnSendUnixSock = new Button(context);
        btnSendUnixSock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        btnSendUnixSock.setTypeface(null, Typeface.BOLD);
        btnSendUnixSock.setText("Send via unix sock");
        btnSendUnixSock.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnSendUnixSock();
            }
        });
        papirus.addView(btnSendUnixSock, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        btnSendALoop = new Button(context);
        btnSendALoop.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        btnSendALoop.setTypeface(null, Typeface.BOLD);
        btnSendALoop.setText("Send via ALooper bus");
        btnSendALoop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnSendALooper();
            }
        });
        papirus.addView(btnSendALoop, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));


        txtResult = new TextView(context);
        txtResult.setSingleLine(true);
        txtResult.setMaxLines(1);
        txtResult.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        txtResult.setEllipsize(TextUtils.TruncateAt.END);
        txtResult.setTextColor(0xffF5DEB3);
        txtResult.setBackgroundColor(0xff005005);
        txtResult.setTypeface(null, Typeface.BOLD);
        papirus.addView(txtResult, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));


        btnGetShared = new Button(context);
        btnGetShared.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        btnGetShared.setTypeface(null, Typeface.BOLD);
        btnGetShared.setText("Get last shared mem msg");
        btnGetShared.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnGetShared();
            }
        });
        papirus.addView(btnGetShared, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        btnGetUnixSock = new Button(context);
        btnGetUnixSock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        btnGetUnixSock.setTypeface(null, Typeface.BOLD);
        btnGetUnixSock.setText("Get last unix sock msg");
        btnGetUnixSock.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnGetUnixSock();
            }
        });
        papirus.addView(btnGetUnixSock, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        btnGetALoop = new Button(context);
        btnGetALoop.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        btnGetALoop.setTypeface(null, Typeface.BOLD);
        btnGetALoop.setText("Get last ALooper bus msg");
        btnGetALoop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnGetALoop();
            }
        });
        papirus.addView(btnGetALoop, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));


        addView(scrollView, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
    }

    public void setMessage(String str){
        txtTittle.setText(str);
        txtTittle.setTextColor(0xff000000);
    }

    public void updateState() {
        ipcState = 1==IPClib.getThreadState();
        if (ipcState) {
            btnStartStop.setText("STOP");
            txtTittle.setText("IPC online");
            txtTittle.setTextColor(0xff228B22);
            btnSendUnixSock.setEnabled(true);
            btnSendShared.setEnabled(true);
            btnSendALoop.setEnabled(true);
        } else{
            btnStartStop.setText("START");
            txtTittle.setText("IPC offline");
            txtTittle.setTextColor(0xffd50000);
            btnSendUnixSock.setEnabled(false);
            btnSendShared.setEnabled(false);
            btnSendALoop.setEnabled(false);
        }
    }

    public void setMaxMsgSize(int msgSize){
        maxMsgSize = msgSize;
    }

    void onBtnStartStop() {
        if (ipcState) {
            IPClib.stop();
        } else {
            IPClib.start();
        }
        updateState();
    }

    void onBtnSendShared(){
        if (ipcState) {
            try {
                byte[] arr = msgToSend.getBytes("UTF-8");
                if (arr.length>0){
                    IPClib.sendShared(arr, arr.length);
                }
            } catch (Exception e) {
                Log.e("GUI","IPClib.sendShared(msgToSend.getBytes error:", e);
            }
        }
    }

    void onBtnSendUnixSock() {
        if (ipcState) {
            IPClib.sendSock(msgToSend);
        }
    }

    void onBtnSendALooper() {
        if (ipcState) {
            IPClib.sendALoop(msgToSend);
        }
    }

    void onBtnGetShared(){
        try {
            byte[] arr = IPClib.getShared();
            if (null!=arr){
                String str = new String(arr, "UTF-8");
                txtResult.setText(str);
            }
        } catch (Exception e) {
            Log.e("GUI","IPClib.onBtnGetShared( error:", e);
        }

    }

    void onBtnGetUnixSock() {
        txtResult.setText(IPClib.getSock());
    }
    void onBtnGetALoop() {
        txtResult.setText(IPClib.getALoop());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int height = MeasureSpec.getSize(heightMeasureSpec);
        int widht = MeasureSpec.getSize(widthMeasureSpec);

        scrollView.measure(widthMeasureSpec,
                heightMeasureSpec);

        setMeasuredDimension(widht, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        scrollView.layout(0,0,right-left, bottom - top);
    }


    private class Papirus extends FrameLayout {

        public Papirus(Context context) {
            super(context);

        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            int count = getChildCount();
            int height = 16;
            for (int i = 0; i < count; ++i) {
                View child = getChildAt(i);
                measureChildWithMargins(child, widthMeasureSpec, 0,
                        heightMeasureSpec, 0);

                height += child.getMeasuredHeight();
            }

            int widht = MeasureSpec.getSize(widthMeasureSpec);

            setMeasuredDimension(widht, height);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            int widht = right - left-16;

            int count = getChildCount();
            int curTop = 16;
            for (int i = 0; i < count; ++i) {
                View child = getChildAt(i);

                int h = child.getMeasuredHeight();
                child.layout(16,curTop,widht,curTop+h);
                curTop+=h;
            }

        }

//        @Override
//        protected void onDraw(Canvas canvas) {
//            super.onDraw(canvas);
//
////            canvas.drawLine(SpecTheme.dpButtonPadding, lineY,
////                    SpecTheme.dpButtonPadding+lineWidht, lineY, SpecTheme.paintLine);
//
//        }

    }//Papirus

}
