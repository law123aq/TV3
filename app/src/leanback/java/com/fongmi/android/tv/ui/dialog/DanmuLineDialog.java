package com.fongmi.android.gongjin.ui.dialog;

import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.gongjin.Setting;
import com.fongmi.android.gongjin.databinding.DialogDanmuLineBinding;
import com.fongmi.android.gongjin.impl.DanmuLineCallback;
import com.fongmi.android.gongjin.utils.KeyUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DanmuLineDialog {

    private final DialogDanmuLineBinding binding;
    private final DanmuLineCallback callback;
    private final AlertDialog dialog;

    public static DanmuLineDialog create(FragmentActivity activity) {
        return new DanmuLineDialog(activity);
    }

    public DanmuLineDialog(FragmentActivity activity) {
        this.callback = (DanmuLineCallback) activity;
        this.binding = DialogDanmuLineBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void initView() {
        binding.slider.setValue(Setting.getDanmuLine(3));
    }

    private void initEvent() {
        binding.slider.addOnChangeListener((slider, value, fromUser) -> callback.setDanmuLine((int) value));
        binding.slider.setOnKeyListener((view, keyCode, event) -> {
            boolean enter = KeyUtil.isEnterKey(event);
            if (enter) dialog.dismiss();
            return enter;
        });
    }
}
