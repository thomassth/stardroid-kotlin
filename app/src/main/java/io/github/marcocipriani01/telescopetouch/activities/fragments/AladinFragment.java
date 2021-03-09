/*
 * Copyright (C) 2020  Marco Cipriani (@marcocipriani01)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package io.github.marcocipriani01.telescopetouch.activities.fragments;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import org.indilib.i4j.Constants;
import org.indilib.i4j.client.INDIValueException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import io.github.marcocipriani01.telescopetouch.ApplicationConstants;
import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.TelescopeTouchApp;
import io.github.marcocipriani01.telescopetouch.activities.MainActivity;
import io.github.marcocipriani01.telescopetouch.activities.views.AladinView;
import io.github.marcocipriani01.telescopetouch.astronomy.EquatorialCoordinates;
import io.github.marcocipriani01.telescopetouch.astronomy.HorizontalCoordinates;
import io.github.marcocipriani01.telescopetouch.astronomy.StarsPrecession;
import io.github.marcocipriani01.telescopetouch.sensors.LocationHelper;

import static io.github.marcocipriani01.telescopetouch.ApplicationConstants.ALADIN_J2000_NOTE;
import static io.github.marcocipriani01.telescopetouch.ApplicationConstants.ALADIN_WELCOME;
import static io.github.marcocipriani01.telescopetouch.TelescopeTouchApp.connectionManager;

public class AladinFragment extends ActionFragment implements Toolbar.OnMenuItemClickListener,
        AladinView.AladinListener, SearchView.OnQueryTextListener, MenuItem.OnMenuItemClickListener {

    private static final String TAG = TelescopeTouchApp.getTag(AladinFragment.class);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AladinView aladinView;
    private EquatorialCoordinates pointing = null;
    private MenuItem searchMenu;
    private MenuItem saveMenu;
    private ContentLoadingProgressBar progressBar;
    private MenuItem refreshMenu;
    private TextView errorText;
    private SharedPreferences preferences;
    private MenuItem aboutMenu;
    private LocationHelper locationHelper;
    private Location location = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (AladinView.isSupported(preferences)) {
            rootView = inflater.inflate(R.layout.fragment_aladin, container, false);
            setHasOptionsMenu(true);
            progressBar = rootView.findViewById(R.id.aladin_loading);
            errorText = rootView.findViewById(R.id.aladin_no_internet);
            Menu menu = rootView.<Toolbar>findViewById(R.id.aladin_toolbar).getMenu();
            searchMenu = menu.add(R.string.search);
            searchMenu.setIcon(R.drawable.search);
            searchMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            SearchView searchView = new SearchView(context);
            searchView.setMaxWidth(Integer.MAX_VALUE);
            searchView.setOnQueryTextListener(this);
            searchView.setImeOptions(searchView.getImeOptions() | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            searchMenu.setActionView(searchView);
            searchMenu.setVisible(false);
            saveMenu = menu.add(R.string.save_image);
            saveMenu.setIcon(R.drawable.save);
            saveMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            saveMenu.setOnMenuItemClickListener(this);
            saveMenu.setVisible(false);
            refreshMenu = menu.add(R.string.refresh);
            refreshMenu.setIcon(R.drawable.refresh);
            refreshMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            refreshMenu.setOnMenuItemClickListener(this);
            aladinView = rootView.findViewById(R.id.aladin_web_view);
            aladinView.setAladinListener(this);
            aladinView.start();
            FragmentActivity activity = getActivity();
            locationHelper = new LocationHelper(activity) {
                @Override
                protected void onLocationOk(Location location) {
                    AladinFragment.this.location = location;
                }

                @Override
                protected void requestLocationPermission() {
                    if (activity instanceof MainActivity)
                        ((MainActivity) activity).requestLocationPermission();
                }

                @Override
                protected void makeSnack(String string) {
                    requestActionSnack(string);
                }
            };
            if (!preferences.getBoolean(ALADIN_WELCOME, false))
                aladinDialog();
        } else {
            rootView = inflater.inflate(R.layout.aladin_not_supported, container, false);
            rootView.findViewById(R.id.aladin_browser_button).setOnClickListener(v ->
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://aladin.u-strasbg.fr/AladinLite/"))));
        }
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        locationHelper.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        locationHelper.stop();
    }

    private void aladinDialog() {
        new AlertDialog.Builder(context).setView(R.layout.dialog_aladin_welcome)
                .setPositiveButton(R.string.continue_button, (dialog, which) -> preferences.edit().putBoolean(ALADIN_WELCOME, true).apply()).show();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        aboutMenu = menu.add(R.string.about_aladin_menu);
        aboutMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean isActionEnabled() {
        return (aladinView != null) && aladinView.isRunning();
    }

    @Override
    public int getActionDrawable() {
        return R.drawable.navigation;
    }

    @Override
    public void run() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(R.string.point_telescope);
        if (!connectionManager.isConnected()) {
            builder.setMessage(R.string.connect_telescope_first)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(new Intent(context, MainActivity.class)));
        } else if ((connectionManager.telescopeName == null) ||
                (connectionManager.telescopeCoordP == null) || (connectionManager.telescopeOnCoordSetP == null)) {
            builder.setMessage(R.string.no_telescope_found);
        } else {
            String msg = String.format(getString(R.string.point_telescope_message),
                    connectionManager.telescopeName, pointing.getRAString(), pointing.getDecString());
            if ((location != null) && HorizontalCoordinates.getInstance(pointing, location, Calendar.getInstance()).alt < 0)
                msg += context.getString(R.string.below_horizon_warning);
            if (preferences.getBoolean(ApplicationConstants.ALADIN_J2000_NOTE, true))
                msg += context.getString(R.string.aladin_j2000_note);
            builder.setMessage(msg)
                    .setPositiveButton(R.string.go_to, (dialog, which) -> {
                        try {
                            connectionManager.telescopeOnCoordSetTrack.setDesiredValue(Constants.SwitchStatus.ON);
                            connectionManager.telescopeOnCoordSetSlew.setDesiredValue(Constants.SwitchStatus.OFF);
                            connectionManager.telescopeOnCoordSetSync.setDesiredValue(Constants.SwitchStatus.OFF);
                            setCoordinatesMaybePrecess(pointing);
                            connectionManager.updateProperties(connectionManager.telescopeOnCoordSetP, connectionManager.telescopeCoordP);
                            requestActionSnack(R.string.slew_ok);
                        } catch (Exception e) {
                            Log.e(TAG, e.getLocalizedMessage(), e);
                            requestActionSnack(R.string.sync_slew_error);
                        }
                    }).setNeutralButton(R.string.sync, (dialog, which) -> {
                try {
                    connectionManager.telescopeOnCoordSetSync.setDesiredValue(Constants.SwitchStatus.ON);
                    connectionManager.telescopeOnCoordSetTrack.setDesiredValue(Constants.SwitchStatus.OFF);
                    connectionManager.telescopeOnCoordSetSlew.setDesiredValue(Constants.SwitchStatus.OFF);
                    setCoordinatesMaybePrecess(pointing);
                    connectionManager.updateProperties(connectionManager.telescopeOnCoordSetP, connectionManager.telescopeCoordP);
                    requestActionSnack(R.string.sync_ok);
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage(), e);
                    requestActionSnack(R.string.sync_slew_error);
                }
            });
        }
        builder.setNegativeButton(android.R.string.cancel, null).setIcon(R.drawable.navigation).show();
    }

    private void setCoordinatesMaybePrecess(EquatorialCoordinates coordinates) throws INDIValueException {
        preferences.edit().putBoolean(ALADIN_J2000_NOTE, false).apply();
        if (preferences.getBoolean(ApplicationConstants.COMPENSATE_PRECESSION_PREF, true)) {
            EquatorialCoordinates precessed = StarsPrecession.precess(Calendar.getInstance(), coordinates);
            connectionManager.telescopeCoordRA.setDesiredValue(precessed.getRATelescopeFormat());
            connectionManager.telescopeCoordDec.setDesiredValue(precessed.getDecTelescopeFormat());
        } else {
            connectionManager.telescopeCoordRA.setDesiredValue(coordinates.getRATelescopeFormat());
            connectionManager.telescopeCoordDec.setDesiredValue(coordinates.getDecTelescopeFormat());
        }
    }

    @Override
    public void onAladinLoaded() {
        notifyActionChange();
        saveMenu.setVisible(true);
        searchMenu.setVisible(true);
        progressBar.hide();
    }

    @Override
    public void onAladinStop() {
        notifyActionChange();
        saveMenu.setVisible(false);
        searchMenu.setVisible(false);
    }

    @Override
    public void onAladinError() {
        errorText.setVisibility(View.VISIBLE);
        aladinView.setVisibility(View.GONE);
    }

    @Override
    public void onAladinGoToFail() {
        new AlertDialog.Builder(context).setTitle(R.string.aladin_sky_atlas)
                .setMessage(R.string.aladin_search_fail).setIcon(R.drawable.cds)
                .setNegativeButton(android.R.string.ok, null).show();
    }

    @Override
    public void onAladinCoordUpdate(EquatorialCoordinates coordinates) {
        pointing = coordinates;
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("SimpleDateFormat")
    public void onAladinBitmap(Bitmap bitmap) {
        try {
            OutputStream stream;
            Uri uri;
            String folderName = context.getString(R.string.app_name),
                    fileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + File.separator + folderName);
                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                stream = resolver.openOutputStream(uri);
            } else {
                String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + File.separator + folderName;
                File dirFile = new File(directory);
                if (!dirFile.exists()) dirFile.mkdir();
                File file = new File(directory, fileName + ".png");
                uri = Uri.fromFile(file);
                stream = new FileOutputStream(file);
                MediaScannerConnection.scanFile(context, new String[]{dirFile.toString()}, null, null);
            }
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();
            handler.post(() -> {
                saveMenu.setEnabled(true);
                progressBar.hide();
                requestActionSnack(R.string.saved_snackbar, R.string.view_image, v -> {
                    Intent intent = new Intent();
                    intent.setDataAndType(uri, "image/*");
                    intent.setAction(Intent.ACTION_VIEW);
                    startActivity(Intent.createChooser(intent, context.getString(R.string.open_photo)));
                });
            });
        } catch (Exception e) {
            Log.e(TAG, "Saving error", e);
            handler.post(() -> requestActionSnack(R.string.saving_error));
        }
    }

    @Override
    public void onAladinProgressChange(int progress) {
        if (progress == 100) {
            progressBar.setIndeterminate(true);
        } else {
            progressBar.setIndeterminate(false);
            progressBar.setProgress(progress);
        }
        progressBar.show();
    }

    @Override
    public void onAladinProgressIndeterminate() {
        handler.post(() -> {
            progressBar.setIndeterminate(true);
            progressBar.show();
        });
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        aladinView.gotoObject(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item == saveMenu) {
            aladinView.saveBitmap();
            saveMenu.setEnabled(false);
            progressBar.show();
        } else if (item == refreshMenu) {
            errorText.setVisibility(View.GONE);
            aladinView.setVisibility(View.VISIBLE);
            aladinView.reload();
        } else if (item == aboutMenu) {
            aladinDialog();
        } else {
            return false;
        }
        return true;
    }
}