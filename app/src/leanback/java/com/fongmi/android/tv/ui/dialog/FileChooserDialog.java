package com.fongmi.android.gongjin.ui.dialog;

import android.app.Activity;

import com.fongmi.android.gongjin.bean.Sub;
import com.fongmi.android.gongjin.player.Players;
import com.fongmi.android.gongjin.ui.dialog.TrackDialog;
import com.github.catvod.utils.Path;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;

public class FileChooserDialog {

    private ChooserDialog dialog;
    private TrackDialog trackDialog;
    private Players player;

    public static FileChooserDialog create() {
        return new FileChooserDialog();
    }

    public FileChooserDialog player(Players player) {
        this.player = player;
        return this;
    }

    public FileChooserDialog trackDialog(TrackDialog dialog) {
        this.trackDialog = dialog;
        return this;
    }

    public void show(Activity activity) {
        dialog = new ChooserDialog(activity);
        dialog.withFilter(false, false, "srt", "ass", "scc", "stl", "ttml");
        dialog.withStartFile(Path.downloadPath());
        dialog.withChosenListener(this::onChoosePath);
        dialog.build().show();
    }


    private void onChoosePath(String path, File pathFile) {
        player.setSub(Sub.from(pathFile.getAbsolutePath()));
        if (dialog != null) dialog.dismiss();
        if (trackDialog != null) trackDialog.dismiss();
    }

}
