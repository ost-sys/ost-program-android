package com.ost.application.ui.tiles;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import com.ost.application.R;
import com.topjohnwu.superuser.Shell;

public class RebootFastbootdTileService extends android.service.quicksettings.TileService {

    @Override
    public void onTileAdded() {
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.reboot_fastbootd));
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_oui_restart));
            tile.updateTile();
        }
    }

    @Override
    public void onStartListening() {
        updateTileState();
    }

    @Override
    public void onClick() {
        Shell.cmd("reboot fastbootd").exec();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile != null) {
            if (hasRootAccess()) {
                tile.setState(Tile.STATE_ACTIVE);
            } else {
                tile.setState(Tile.STATE_UNAVAILABLE);
            }
            tile.updateTile();
        }
    }

    private boolean hasRootAccess() {
        Shell.Result result = Shell.cmd("su").exec();
        return result.isSuccess();
    }
}
