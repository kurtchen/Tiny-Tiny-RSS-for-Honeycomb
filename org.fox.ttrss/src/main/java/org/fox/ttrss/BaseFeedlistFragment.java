package org.fox.ttrss;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.util.TypedValue;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.fox.ttrss.offline.OfflineActivity;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class BaseFeedlistFragment extends Fragment {
    abstract public void refresh(boolean background);

    public void initDrawerHeader(LayoutInflater inflater, View view, ListView list, final CommonActivity activity, final SharedPreferences prefs, boolean isRoot) {

        boolean isOffline = activity instanceof OfflineActivity;

        if (true /*m_activity.findViewById(R.id.headlines_drawer) != null*/) {
            try {

                if (activity.isSmallScreen()) {
                    View layout = inflater.inflate(R.layout.drawer_header, list, false);
                    list.addHeaderView(layout, null, false);

                    TextView login = (TextView) view.findViewById(R.id.drawer_header_login);
                    TextView server = (TextView) view.findViewById(R.id.drawer_header_server);

                    login.setText(prefs.getString("login", ""));
                    try {
                        server.setText(new URL(prefs.getString("ttrss_url", "")).getHost());
                    } catch (MalformedURLException e) {
                        server.setText("");
                    }

                    View account = view.findViewById(R.id.drawer_header_account);

                    account.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(prefs.getString("ttrss_url", "")));
                                startActivity(intent);
                            } catch (Exception e) {

                            }
                        }
                    });
                }

				/* deal with ~material~ footers */

                // divider
                View footer = inflater.inflate(R.layout.drawer_divider, list, false);
                footer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //
                    }
                });
                list.addFooterView(footer);

                // unread only checkbox
                footer = inflater.inflate(R.layout.feeds_row_toggle, list, false);
                list.addFooterView(footer);
                TextView text = (TextView) footer.findViewById(R.id.title);
                text.setText(R.string.unread_only);

                ImageView icon = (ImageView) footer.findViewById(R.id.icon);
                TypedValue tv = new TypedValue();
                getActivity().getTheme().resolveAttribute(R.attr.ic_filter_variant, tv, true);
                icon.setImageResource(tv.resourceId);

                final SwitchCompat rowSwitch = (SwitchCompat) footer.findViewById(R.id.row_switch);
                rowSwitch.setChecked(activity.getUnreadOnly());

                rowSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                        activity.setUnreadOnly(isChecked);
                        refresh(true);
                    }
                });

                footer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        rowSwitch.setChecked(!rowSwitch.isChecked());
                    }
                });

                if (isRoot) {
                    // offline
                    footer = inflater.inflate(R.layout.feeds_row, list, false);
                    footer.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (activity instanceof OnlineActivity) {
                                ((OnlineActivity)activity).switchOffline();

                            } else if (activity instanceof OfflineActivity) {
                                ((OfflineActivity)activity).switchOnline();
                            }
                        }
                    });

                    list.addFooterView(footer);
                    text = (TextView) footer.findViewById(R.id.title);
                    text.setText(isOffline ? R.string.go_online : R.string.go_offline);

                    icon = (ImageView) footer.findViewById(R.id.icon);
                    tv = new TypedValue();
                    getActivity().getTheme().resolveAttribute(isOffline ? R.attr.ic_cloud_upload : R.attr.ic_cloud_download, tv, true);
                    icon.setImageResource(tv.resourceId);

                    TextView counter = (TextView) footer.findViewById(R.id.unread_counter);
                    counter.setText(R.string.blank);
                }

                // settings
                footer = inflater.inflate(R.layout.feeds_row, list, false);
                footer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(activity,
                                PreferencesActivity.class);
                        startActivityForResult(intent, 0);
                    }
                });

                list.addFooterView(footer);
                text = (TextView) footer.findViewById(R.id.title);
                text.setText(R.string.preferences);

                icon = (ImageView) footer.findViewById(R.id.icon);
                tv = new TypedValue();
                getActivity().getTheme().resolveAttribute(R.attr.ic_settings, tv, true);
                icon.setImageResource(tv.resourceId);

                TextView counter = (TextView) footer.findViewById(R.id.unread_counter);
                counter.setText(R.string.blank);

            } catch (InflateException e) {
                // welp couldn't inflate header i guess
                e.printStackTrace();
            } catch (java.lang.UnsupportedOperationException e) {
                e.printStackTrace();
            }
        }

    }

}
