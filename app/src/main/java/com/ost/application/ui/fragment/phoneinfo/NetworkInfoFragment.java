package com.ost.application.ui.fragment.phoneinfo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.ost.application.R;
import com.ost.application.databinding.FragmentNetworkInfoBinding;
import com.ost.application.ui.core.base.BaseFragment;

import java.util.Objects;

import dev.oneuiproject.oneui.widget.Toast;

public class NetworkInfoFragment extends BaseFragment implements View.OnClickListener {
    private FragmentNetworkInfoBinding binding;
    private static final int REQUEST_PHONE_STATE = 101;

    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNetworkInfoBinding.inflate(inflater, container, false);

        updateNetworkInfo();

        binding.checkPermission.setOnClickListener(this);

        return binding.getRoot();
    }

    private void updateNetworkInfo() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            binding.checkPermission.setSummary(getString(R.string.press_to_grant_permission));
            binding.checkPermission.setEnabled(true);
        } else {
            binding.checkPermission.setSummary(getString(R.string.permission_granted));
            binding.checkPermission.setEnabled(false);
            showNetworkInfo();
        }
    }

    private void showNetworkInfo() {
        TelephonyManager manager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (manager != null) {
            String countryCode = manager.getNetworkCountryIso();
            String carrierName = manager.getNetworkOperatorName();
            String connectivityStatus = getConnectivityStatusString(getActivity());

            if (!Objects.equals(carrierName, "")) {
                binding.networkTitle.setText(carrierName);
            } else {
                binding.networkTitle.setText(getString(R.string.sim_card_is_not_detected));
            }
            binding.networkOperatorCountry.setSummary(countryCode.toUpperCase());
            binding.networkPhoneType.setSummary(networkType());
            binding.networkConnectivityStatus.setSummary(connectivityStatus);
        }
    }

    private String networkType() {
        TelephonyManager manager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return getString(R.string.grant_permission_to_continue);
        }
        int networkType = manager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_1xRTT: return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_CDMA: return "CDMA";
            case TelephonyManager.NETWORK_TYPE_EDGE: return "EDGE";
            case TelephonyManager.NETWORK_TYPE_EHRPD: return "eHRPD";
            case TelephonyManager.NETWORK_TYPE_EVDO_0: return "EVDO rev. 0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A: return "EVDO rev. A";
            case TelephonyManager.NETWORK_TYPE_EVDO_B: return "EVDO rev. B";
            case TelephonyManager.NETWORK_TYPE_GPRS: return "GPRS";
            case TelephonyManager.NETWORK_TYPE_HSDPA: return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSPA: return "HSPA";
            case TelephonyManager.NETWORK_TYPE_HSPAP: return "HSPA+";
            case TelephonyManager.NETWORK_TYPE_HSUPA: return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_IDEN: return "iDen";
            case TelephonyManager.NETWORK_TYPE_LTE: return "LTE";
            case TelephonyManager.NETWORK_TYPE_UMTS: return "UMTS";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN: return getString(R.string.unknown);
            default: return getString(R.string.unknown);
        }
    }

    private void requestPhoneStatePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_PHONE_STATE)) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_PHONE_STATE);
        } else {
            Toast.makeText(getActivity(), getString(R.string.grant_permission_to_continue), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PHONE_STATE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateNetworkInfo();
            } else {
                Toast.makeText(getActivity(), getString(R.string.grant_permission_to_continue), Toast.LENGTH_SHORT).show();
                binding.checkPermission.setSummary(getString(R.string.check_permission));
                binding.checkPermission.setEnabled(true);
            }
        }
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_network_info;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_mobile_data;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.network);
    }

    public void onClick(View v) {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            updateNetworkInfo();
            Toast.makeText(getActivity(), R.string.success, Toast.LENGTH_SHORT).show();
        } else {
            requestPhoneStatePermission();
        }
    }

    public String getConnectivityStatusString(Context context) {
        int conn = getConnectivityStatus(context);
        String status = null;
        if (conn == TYPE_WIFI) {
            status = getString(R.string.wifi_enabled);
        } else if (conn == TYPE_MOBILE) {
            status = getString(R.string.mobile_data_enabled);
        } else if (conn == TYPE_NOT_CONNECTED) {
            status = getString(R.string.not_connected_to_internet);
        }
        return status;
    }

    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return TYPE_WIFI;
            }
            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return TYPE_MOBILE;
            }
        }
        return TYPE_NOT_CONNECTED;
    }
}
