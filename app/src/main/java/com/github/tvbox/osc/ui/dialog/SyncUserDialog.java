package com.github.tvbox.osc.ui.dialog;

import android.content.Context;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.databinding.DialogLiveApiBinding;
import com.github.tvbox.osc.databinding.DialogSyncUserBinding;
import com.github.tvbox.osc.util.HawkConfig;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.CenterPopupView;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;

public class SyncUserDialog extends CenterPopupView {

    private DialogSyncUserBinding mBinding;

    public SyncUserDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_sync_user;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mBinding = DialogSyncUserBinding.bind(getPopupImplView());
        String liveApi = Hawk.get(HawkConfig.SYNC_USER, "");
        updateEt(liveApi);

        mBinding.btnCancel.setOnClickListener(v -> dismiss());
        mBinding.btnConfirm.setOnClickListener(view -> {
            String newLive = mBinding.syncUser.getText().toString().trim();
            // Capture Live input into Settings & Live History (max 20)
            Hawk.put(HawkConfig.SYNC_USER, newLive);
            ToastUtils.showShort("设置成功");
            dismiss();
        });
    }

    private void updateEt(String text){
        mBinding.syncUser.setText(text);
        mBinding.syncUser.setSelection(text.length());
    }
}