package com.campsruntool.yh;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.campsruntool.yh.map.MapLayer;
import com.campsruntool.yh.service.MockLocationService;
import com.campsruntool.yh.service.ServiceDiagnostics;
import com.campsruntool.yh.ui.DiagnosticsDialog;
import com.campsruntool.yh.ui.LayerPickerDialog;
import com.campsruntool.yh.util.Prefs;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private MockLocationService service;
    private boolean bound;
    private boolean bindingRequested;

    private MaterialButtonToggleGroup modeGroup;
    private MaterialButton modeRouteButton;
    private MaterialButton modeJoystickButton;
    private TextView modeSummary;
    private TextView modeLockedNote;
    private Switch floatingSwitch;
    private TextView floatingSummary;
    private LinearLayout layerRow;
    private TextView layerSummary;
    private LinearLayout selfCheckRow;
    private TextView versionSummary;
    private LinearLayout developerRow;
    private LinearLayout projectRow;

    private boolean initializingMode;
    private boolean initializingFloating;
    private boolean pendingFloatingEnable;
    private boolean pendingJoystickMode;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((MockLocationService.LocalBinder) binder).getService();
            bound = true;
            updateControlStates();
            updateModeUi();
            updateFloatingUi();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
            bound = false;
            updateControlStates();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = Prefs.get(this);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        modeGroup = findViewById(R.id.modeGroup);
        modeRouteButton = findViewById(R.id.modeRouteButton);
        modeJoystickButton = findViewById(R.id.modeJoystickButton);
        modeSummary = findViewById(R.id.modeSummary);
        modeLockedNote = findViewById(R.id.modeLockedNote);
        floatingSwitch = findViewById(R.id.floatingSwitch);
        floatingSwitch.setShowText(false);
        floatingSummary = findViewById(R.id.floatingSummary);
        layerRow = findViewById(R.id.layerRow);
        layerSummary = findViewById(R.id.layerSummary);
        selfCheckRow = findViewById(R.id.selfCheckRow);
        versionSummary = findViewById(R.id.versionSummary);
        developerRow = findViewById(R.id.developerRow);
        projectRow = findViewById(R.id.projectRow);

        modeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || initializingMode) {
                return;
            }
            handleModeSelection(checkedId);
        });

        floatingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (initializingFloating) {
                return;
            }
            handleFloatingToggle(isChecked);
        });

        layerRow.setOnClickListener(v -> {
            if (isSimulationActive()) {
                toast(R.string.toast_layer_locked);
                return;
            }
            showLayerPicker();
        });
        selfCheckRow.setOnClickListener(v -> showSelfCheck());
        developerRow.setOnClickListener(v ->
                openUrl(getString(R.string.settings_developer_url)));
        projectRow.setOnClickListener(v ->
                openUrl(getString(R.string.settings_project_url)));

        versionSummary.setText(getVersionName());

        updateModeUi();
        updateFloatingUi();
        updateLayerSummary();
        updateControlStates();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindingRequested = bindService(
                new Intent(this, MockLocationService.class),
                connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handlePendingPermissions();
        updateModeUi();
        updateFloatingUi();
        updateLayerSummary();
        updateControlStates();
    }

    @Override
    protected void onStop() {
        if (bindingRequested) {
            try {
                unbindService(connection);
            } catch (IllegalArgumentException ignored) {
            }
            bindingRequested = false;
            bound = false;
            service = null;
        }
        super.onStop();
    }

    // ---------------------------------------------------------------------
    // Mode
    // ---------------------------------------------------------------------

    private boolean isJoystickMode() {
        return Prefs.MODE_JOYSTICK.equals(
                prefs.getString(Prefs.SIMULATION_MODE, Prefs.MODE_ROUTE));
    }

    private boolean isSimulationActive() {
        return service != null && service.isSimulationActive();
    }

    private void updateModeUi() {
        boolean joystick = isJoystickMode();
        initializingMode = true;
        modeGroup.check(joystick ? R.id.modeJoystickButton : R.id.modeRouteButton);
        initializingMode = false;

        modeSummary.setText(joystick
                ? R.string.settings_mode_joystick_summary
                : R.string.settings_mode_route_summary);
        modeLockedNote.setVisibility(isSimulationActive() ? View.VISIBLE : View.GONE);
    }

    private void handleModeSelection(int checkedId) {
        if (isSimulationActive()) {
            toast(R.string.settings_mode_locked);
            updateModeUi();
            return;
        }
        boolean joystick = checkedId == R.id.modeJoystickButton;
        if (joystick) {
            if (!Settings.canDrawOverlays(this)) {
                pendingJoystickMode = true;
                updateModeUi();
                showOverlayPermissionDialog();
                return;
            }
            prefs.edit()
                    .putString(Prefs.SIMULATION_MODE, Prefs.MODE_JOYSTICK)
                    .apply();
            toast(R.string.settings_mode_switched_joystick);
        } else {
            prefs.edit()
                    .putString(Prefs.SIMULATION_MODE, Prefs.MODE_ROUTE)
                    .apply();
            toast(R.string.settings_mode_switched_route);
        }
        updateModeUi();
        updateFloatingUi();
        updateControlStates();
    }

    // ---------------------------------------------------------------------
    // Floating window
    // ---------------------------------------------------------------------

    private void updateFloatingUi() {
        boolean joystick = isJoystickMode();
        boolean enabled = prefs.getBoolean(Prefs.FLOATING_ENABLED, false);
        boolean hasPermission = Settings.canDrawOverlays(this);

        initializingFloating = true;
        floatingSwitch.setChecked(joystick || enabled);
        initializingFloating = false;

        if (joystick) {
            floatingSwitch.setEnabled(false);
            floatingSummary.setText(R.string.settings_floating_joystick_summary);
            floatingSummary.setTextColor(getColor(R.color.text_secondary));
        } else {
            floatingSwitch.setEnabled(true);
            floatingSummary.setText(R.string.settings_floating_summary);
            floatingSummary.setTextColor(enabled && !hasPermission
                    ? getColor(R.color.status_error)
                    : getColor(R.color.text_secondary));
        }
    }

    private void handleFloatingToggle(boolean enabled) {
        if (isJoystickMode()) {
            updateFloatingUi();
            return;
        }
        if (enabled) {
            if (!Settings.canDrawOverlays(this)) {
                pendingFloatingEnable = true;
                initializingFloating = true;
                floatingSwitch.setChecked(false);
                initializingFloating = false;
                showOverlayPermissionDialog();
                return;
            }
            prefs.edit().putBoolean(Prefs.FLOATING_ENABLED, true).apply();
            if (isSimulationActive() && service != null) {
                service.showFloatingWindow();
            }
            toast(R.string.settings_floating_on);
        } else {
            prefs.edit().putBoolean(Prefs.FLOATING_ENABLED, false).apply();
            if (service != null) {
                service.hideFloatingWindow();
            }
            toast(R.string.settings_floating_off);
        }
        updateFloatingUi();
    }

    private void showOverlayPermissionDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_overlay_title)
                .setMessage(R.string.dialog_overlay_message)
                .setPositiveButton(R.string.dialog_overlay_settings, (dialog, which) -> {
                    try {
                        startActivity(new Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName())));
                    } catch (Exception e) {
                        toast(R.string.settings_overlay_denied);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {
                    pendingFloatingEnable = false;
                    pendingJoystickMode = false;
                    updateModeUi();
                    updateFloatingUi();
                })
                .show();
    }

    private void handlePendingPermissions() {
        if (!pendingFloatingEnable && !pendingJoystickMode) {
            return;
        }
        boolean granted = Settings.canDrawOverlays(this);
        if (granted) {
            if (pendingJoystickMode) {
                prefs.edit()
                        .putString(Prefs.SIMULATION_MODE, Prefs.MODE_JOYSTICK)
                        .apply();
                toast(R.string.settings_mode_switched_joystick);
            } else {
                prefs.edit().putBoolean(Prefs.FLOATING_ENABLED, true).apply();
                if (isSimulationActive() && service != null) {
                    service.showFloatingWindow();
                }
                toast(R.string.settings_floating_on);
            }
        } else {
            toast(R.string.settings_overlay_denied);
        }
        pendingFloatingEnable = false;
        pendingJoystickMode = false;
        updateModeUi();
        updateFloatingUi();
    }

    // ---------------------------------------------------------------------
    // Layer
    // ---------------------------------------------------------------------

    private void updateLayerSummary() {
        String id = prefs.getString(Prefs.LAYER_ID, MapLayer.ID_GAODE);
        int labelRes;
        switch (id) {
            case MapLayer.ID_OSM:
                labelRes = R.string.layer_osm;
                break;
            case MapLayer.ID_ESRI:
                labelRes = R.string.layer_esri;
                break;
            case MapLayer.ID_CUSTOM:
                labelRes = R.string.layer_custom;
                break;
            default:
                labelRes = R.string.layer_gaode;
                break;
        }
        layerSummary.setText(getString(
                R.string.settings_layer_summary, getString(labelRes)));
    }

    private void showLayerPicker() {
        String id = prefs.getString(Prefs.LAYER_ID, MapLayer.ID_GAODE);
        String url = prefs.getString(Prefs.CUSTOM_URL, null);
        MapLayer.CoordSystem coord;
        try {
            coord = MapLayer.CoordSystem.valueOf(
                    prefs.getString(Prefs.CUSTOM_COORD,
                            MapLayer.CoordSystem.WGS84.name()));
        } catch (IllegalArgumentException e) {
            coord = MapLayer.CoordSystem.WGS84;
        }

        LayerPickerDialog.show(this, id, url, coord,
                (layerId, customUrl, customCoord) -> {
                    SharedPreferences.Editor editor = prefs.edit()
                            .putString(Prefs.LAYER_ID, layerId)
                            .putString(Prefs.CUSTOM_COORD, customCoord.name());
                    if (customUrl != null) {
                        editor.putString(Prefs.CUSTOM_URL, customUrl);
                    }
                    editor.apply();
                    updateLayerSummary();
                    toast(getString(R.string.settings_layer_applied,
                            layerLabel(layerId)));
                });
    }

    private String layerLabel(String id) {
        switch (id) {
            case MapLayer.ID_OSM:
                return getString(R.string.layer_osm);
            case MapLayer.ID_ESRI:
                return getString(R.string.layer_esri);
            case MapLayer.ID_CUSTOM:
                return getString(R.string.layer_custom);
            default:
                return getString(R.string.layer_gaode);
        }
    }

    // ---------------------------------------------------------------------
    // Self-check
    // ---------------------------------------------------------------------

    private void showSelfCheck() {
        if (service == null) {
            toast(R.string.settings_self_check_not_bound);
            return;
        }
        ServiceDiagnostics diagnostics = service.getDiagnostics();
        DiagnosticsDialog.show(this, diagnostics, service.isSimulationActive());
    }

    // ---------------------------------------------------------------------
    // About
    // ---------------------------------------------------------------------

    private String getVersionName() {
        try {
            return getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            toast(R.string.settings_open_link_failed);
        }
    }

    // ---------------------------------------------------------------------
    // Misc
    // ---------------------------------------------------------------------

    private void updateControlStates() {
        boolean active = isSimulationActive();

        modeRouteButton.setEnabled(!active);
        modeJoystickButton.setEnabled(!active);
        modeLockedNote.setVisibility(active ? View.VISIBLE : View.GONE);

        layerRow.setEnabled(!active);
        layerRow.setAlpha(active ? 0.55f : 1.0f);
        selfCheckRow.setEnabled(bound && service != null);
        selfCheckRow.setAlpha(bound && service != null ? 1.0f : 0.55f);
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
