package com.fongmi.android.gongjin.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.gongjin.R;
import com.fongmi.android.gongjin.databinding.ActivityPushBinding;
import com.fongmi.android.gongjin.server.Server;
import com.fongmi.android.gongjin.ui.base.BaseActivity;
import com.fongmi.android.gongjin.utils.QRCode;
import com.fongmi.android.gongjin.utils.ResUtil;
import com.fongmi.android.gongjin.utils.Sniffer;
import com.fongmi.android.gongjin.utils.Util;

public class PushActivity extends BaseActivity {

    private ActivityPushBinding mBinding;

    public static void start(Activity activity) {
        start(activity, 2);
    }

    public static void start(Activity activity, int tab) {
        Intent intent = new Intent(new Intent(activity, PushActivity.class));
        intent.putExtra("tab", tab);
        activity.startActivity(intent);
    }

    private int getTab() {
        return getIntent().getIntExtra("tab", 2);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityPushBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.code.setImageBitmap(QRCode.getBitmap(Server.get().getAddress(getTab()), 250, 1));
        mBinding.info.setText(ResUtil.getString(R.string.push_info, Server.get().getAddress()));
    }

    @Override
    protected void initEvent() {
        mBinding.code.setOnClickListener(this::onCode);
        mBinding.clip.setOnClickListener(this::onClip);
    }

    private void onClip(View view) {
        CharSequence text = Util.getClipText();
        if (!TextUtils.isEmpty(text)) VideoActivity.start(this, Sniffer.getUrl(text.toString()), false);
    }

    private void onCode(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(Server.get().getAddress(getTab())));
        startActivity(intent);
    }
}
