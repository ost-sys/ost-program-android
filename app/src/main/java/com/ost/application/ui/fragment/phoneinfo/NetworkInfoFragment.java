package com.ost.application.ui.fragment.phoneinfo;

import static com.topjohnwu.superuser.internal.UiThreadHandler.handler;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.app.ActivityCompat;

import com.ost.application.R;
import com.ost.application.databinding.FragmentNetworkInfoBinding;
import com.ost.application.ui.core.base.BaseFragment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dev.oneuiproject.oneui.widget.Toast;

public class NetworkInfoFragment extends BaseFragment implements View.OnClickListener {
    private FragmentNetworkInfoBinding binding;
    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;
    private Runnable updateRunnable;
<<<<<<< Updated upstream
=======
    private boolean isMasked = false;
    private String originalIp = "";
    private boolean dataLoaded = false;
>>>>>>> Stashed changes

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNetworkInfoBinding.inflate(inflater, container, false);

<<<<<<< Updated upstream
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateNetworkInfo();
                handler.postDelayed(this, 500);
            }
        };

        handler.post(updateRunnable);
=======
        binding.networkIp.setOnClickListener(v -> {
            if (originalIp.isEmpty()) {
                originalIp = binding.networkIp.getSummary().toString();
            }
>>>>>>> Stashed changes

            if (isMasked) {
                binding.networkIp.setSummary(loadIP());
            } else {
                binding.networkIp.setSummary(originalIp.replaceAll("[^.:\n]", "*"));
            }

            isMasked = !isMasked;
            loadIP();
        });

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
<<<<<<< Updated upstream
        showNetworkInfo();
    }

    private void updateNetworkInfo() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            binding.checkPermission.setSummary(getString(R.string.press_to_grant_permission));
            binding.checkPermission.setEnabled(true);
        } else {
            binding.checkPermission.setSummary(getString(R.string.permission_granted));
            binding.checkPermission.setEnabled(false);
            showNetworkInfo();
=======
        if (!dataLoaded) {
            loadData();
            dataLoaded = true;
>>>>>>> Stashed changes
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    private void loadData() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateNetworkInfo();
                handler.postDelayed(this, 500);
            }
        };

        handler.post(updateRunnable);

        binding.networkIp.setSummary(loadIP());
    }

    public String loadIP() {
        if (getConnectivityStatus(getActivity()) == TYPE_WIFI) {
            return getLocalIpAddress(getActivity()) + "\n" + getPublicIp();
        } else if (getConnectivityStatus(getActivity()) == TYPE_MOBILE) {
            return getMobileIpAddress();
        } else {
            return getString(R.string.not_connected_to_internet);
        }
    }

    private void updateNetworkInfo() {
        showNetworkInfo();
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
            binding.networkIcon.setImageIcon(getConnectivityStatusIcon(getActivity()));
<<<<<<< Updated upstream

            checkVPNStatus();
        }
    }

    private void checkVPNStatus() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_VPN) {
                binding.networkVpn.setSummary(getString(R.string.connected));
            } else {
                binding.networkVpn.setSummary(getString(R.string.disconnected));
            }
        } else {
            binding.networkVpn.setSummary(getString(R.string.failed_to_get_vpn_status));
=======
>>>>>>> Stashed changes
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

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_network_info;
    }

    @Override
    public int getIconResId() {
        int conn = getConnectivityStatus(getActivity());
        if (conn == TYPE_WIFI) {
            return dev.oneuiproject.oneui.R.drawable.ic_oui_wifi;
        } else if (conn == TYPE_MOBILE) {
            return dev.oneuiproject.oneui.R.drawable.ic_oui_mobile_data;
        } else if (conn == TYPE_NOT_CONNECTED) {
            return dev.oneuiproject.oneui.R.drawable.ic_oui_wifi_no_connection;
        }
        return dev.oneuiproject.oneui.R.drawable.ic_oui_wifi;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.network);
    }

    public void onClick(View v) {
        assert getActivity() != null;
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            updateNetworkInfo();
            Toast.makeText(getActivity(), R.string.success, Toast.LENGTH_SHORT).show();
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

    public Icon getConnectivityStatusIcon(Context context) {
        int conn = getConnectivityStatus(context);
        int resId = 0;
        if (conn == TYPE_WIFI) {
            resId = dev.oneuiproject.oneui.R.drawable.ic_oui_wifi;
        } else if (conn == TYPE_MOBILE) {
            resId = dev.oneuiproject.oneui.R.drawable.ic_oui_mobile_data;
        } else if (conn == TYPE_NOT_CONNECTED) {
            resId = dev.oneuiproject.oneui.R.drawable.ic_oui_wifi_no_connection;
        }
        return Icon.createWithResource(context, resId);
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

    public String getLocalIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            return Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        }
        return getString(R.string.failed_to_obtain_ip);
    }

    private String getPublicIp() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> {
            try {
                URL url = new URL("https://api64.ipify.org?format=json");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                return new org.json.JSONObject(response.toString()).getString("ip");
            } catch (Exception e) {
                return getString(R.string.failed_to_obtain_ip);
            }
        });

        try {
            return future.get();
        } catch (Exception e) {
            return getString(R.string.error_obtaining_ip);
        }
    }


    public String getMobileIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getString(R.string.failed_to_obtain_ip);
    }
}