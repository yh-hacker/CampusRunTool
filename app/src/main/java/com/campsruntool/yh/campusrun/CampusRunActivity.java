package com.campsruntool.yh.campusrun;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campsruntool.yh.R;

public class CampusRunActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "campus_run_prefs";
    private static final String KEY_URL = "saved_url";
    private static final String TARGET_PACKAGE = "com.eg.android.AlipayGphone";

    private EditText etUrl;
    private View btnDispatch;
    private TextView chipStatus;
    private SharedPreferences prefs;

    private NfcAdapter nfcAdapter;
    private PendingIntent foregroundPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
            getWindow().setNavigationBarColor(getResources().getColor(android.R.color.transparent));
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        setContentView(R.layout.activity_campus_run_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        etUrl = findViewById(R.id.cr_et_url);
        btnDispatch = findViewById(R.id.cr_btn_dispatch);
        chipStatus = findViewById(R.id.cr_chip_status);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        foregroundPendingIntent = createForegroundPendingIntent();

        String savedUrl = prefs.getString(KEY_URL, "");
        if (!TextUtils.isEmpty(savedUrl)) {
            etUrl.setText(savedUrl);
            etUrl.setSelection(savedUrl.length());
        }

        etUrl.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String current = etUrl.getText().toString().trim();
                if (!TextUtils.isEmpty(current)) {
                    prefs.edit().putString(KEY_URL, current).apply();
                }
            }
        });

        btnDispatch.setOnClickListener(v -> dispatchNfc());
        updateNfcStatus();
        handleNfcIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNfcStatus();
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            nfcAdapter.enableForegroundDispatch(this, foregroundPendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNfcIntent(intent);
    }

    private PendingIntent createForegroundPendingIntent() {
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getActivity(this, 0, intent, flags);
    }

    private void updateNfcStatus() {
        if (nfcAdapter == null) {
            setChip("NFC: 设备不支持", ChipState.WARNING);
        } else if (!nfcAdapter.isEnabled()) {
            setChip("NFC: 已关闭，请在系统设置中开启", ChipState.ERROR);
        } else {
            setChip("NFC: 就绪，碰一下打卡墙", ChipState.SUCCESS);
        }
    }

    private enum ChipState { INFO, SUCCESS, WARNING, ERROR }

    private void setChip(String text, ChipState state) {
        chipStatus.setText(text);
        switch (state) {
            case SUCCESS:
                chipStatus.setBackgroundResource(R.drawable.cr_bg_chip_success);
                chipStatus.setTextColor(0xFF6EE7B7);
                break;
            case WARNING:
                chipStatus.setBackgroundResource(R.drawable.cr_bg_chip_warning);
                chipStatus.setTextColor(0xFFFCD34D);
                break;
            case ERROR:
                chipStatus.setBackgroundResource(R.drawable.cr_bg_chip_error);
                chipStatus.setTextColor(0xFFFCA5A5);
                break;
            case INFO:
            default:
                chipStatus.setBackgroundResource(R.drawable.cr_bg_chip_info);
                chipStatus.setTextColor(0xFF93C5FD);
                break;
        }
    }

    private void handleNfcIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;

        boolean isNfc = NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action);

        if (!isNfc) {
            if (Intent.ACTION_VIEW.equals(action)) {
                Uri data = intent.getData();
                if (data != null) {
                    fillUrl(data.toString());
                    setChip("NFC: 已读取标签", ChipState.SUCCESS);
                }
            }
            return;
        }

        String url = extractUrlFromNdef(intent);
        if (TextUtils.isEmpty(url)) {
            url = extractUrlFromTag(intent);
        }

        if (!TextUtils.isEmpty(url)) {
            fillUrl(url);
            setChip("NFC: 已读取标签", ChipState.SUCCESS);
            Toast.makeText(this, "已读取NFC标签", Toast.LENGTH_SHORT).show();
        } else {
            setChip("NFC: 标签为空", ChipState.WARNING);
            Toast.makeText(this, "NFC标签为空或不包含URL", Toast.LENGTH_SHORT).show();
        }
    }

    private String extractUrlFromNdef(Intent intent) {
        Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMessages == null || rawMessages.length == 0) return null;

        for (Parcelable raw : rawMessages) {
            if (!(raw instanceof NdefMessage)) continue;
            NdefMessage msg = (NdefMessage) raw;
            for (NdefRecord record : msg.getRecords()) {
                Uri uri = record.toUri();
                if (uri != null) return uri.toString();
            }
            for (NdefRecord record : msg.getRecords()) {
                if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN) {
                    byte[] payload = record.getPayload();
                    if (payload != null && payload.length > 1) {
                        String text = new String(payload, 1, payload.length - 1);
                        if (text.startsWith("http://") || text.startsWith("https://")) {
                            return text;
                        }
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private String extractUrlFromTag(Intent intent) {
        try {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag == null) return null;
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) return null;
            ndef.connect();
            NdefMessage msg = ndef.getCachedNdefMessage();
            if (msg == null) msg = ndef.getNdefMessage();
            ndef.close();
            if (msg == null) return null;
            for (NdefRecord record : msg.getRecords()) {
                Uri uri = record.toUri();
                if (uri != null) return uri.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void fillUrl(String url) {
        etUrl.setText(url);
        etUrl.setSelection(url.length());
        prefs.edit().putString(KEY_URL, url).apply();
    }

    private void dispatchNfc() {
        String url = etUrl.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            setChip("请先读取标签或输入链接", ChipState.ERROR);
            Toast.makeText(this, "请先读取NFC标签或输入链接", Toast.LENGTH_SHORT).show();
            return;
        }
        prefs.edit().putString(KEY_URL, url).apply();
        setChip("正在跳转...", ChipState.INFO);

        boolean sent = tryNdefDispatch(url);
        if (!sent) sent = tryViewIntent(url);
        if (!sent) {
            try {
                Intent launch = getPackageManager().getLaunchIntentForPackage(TARGET_PACKAGE);
                if (launch != null) {
                    startActivity(launch);
                    setChip("已启动支付宝", ChipState.SUCCESS);
                } else {
                    setChip("未找到支付宝", ChipState.ERROR);
                    Toast.makeText(this, "未找到支付宝，请先安装", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                setChip("跳转失败", ChipState.ERROR);
                Toast.makeText(this, "跳转失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            setChip("已发送签到请求", ChipState.SUCCESS);
        }
    }

    private boolean tryNdefDispatch(String url) {
        try {
            Uri uri = Uri.parse(url);
            NdefRecord uriRecord = NdefRecord.createUri(uri);
            NdefRecord appRecord = NdefRecord.createApplicationRecord(TARGET_PACKAGE);
            NdefMessage message = new NdefMessage(new NdefRecord[]{uriRecord, appRecord});

            Intent intent = new Intent(NfcAdapter.ACTION_NDEF_DISCOVERED);
            intent.setData(uri);
            intent.setPackage(TARGET_PACKAGE);
            intent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, new Parcelable[]{message});
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean tryViewIntent(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage(TARGET_PACKAGE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void openAbout(View view) {
        startActivity(new Intent(this, CampusRunAboutActivity.class));
    }

    public void goBack(View view) {
        finish();
    }
}
