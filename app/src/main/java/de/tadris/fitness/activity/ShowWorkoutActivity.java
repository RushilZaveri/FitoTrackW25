/*
 * Copyright (c) 2020 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     FitoTrack is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tadris.fitness.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutSample;
import de.tadris.fitness.osm.OAuthAuthentication;
import de.tadris.fitness.osm.OsmTraceUploader;
import de.tadris.fitness.util.DialogUtils;
import de.tadris.fitness.util.FileUtils;
import de.tadris.fitness.util.gpx.GpxExporter;
import de.tadris.fitness.util.unit.UnitUtils;
import de.tadris.fitness.view.ProgressDialogController;
import de.westnordost.osmapi.traces.GpsTraceDetails;
import oauth.signpost.OAuthConsumer;

public class ShowWorkoutActivity extends WorkoutActivity implements DialogUtils.WorkoutDeleter {

    TextView commentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initBeforeContent();

        setContentView(R.layout.activity_show_workout);
        initRoot();

        initAfterContent();

        commentView = addText("", true);
        commentView.setOnClickListener(v -> openEditCommentDialog());
        updateCommentText();

        addTitle(getString(R.string.workoutTime));
        addKeyValue(getString(R.string.workoutDate), getDate());
        addKeyValue(getString(R.string.workoutDuration), UnitUtils.getHourMinuteSecondTime(workout.duration),
                getString(R.string.workoutPauseDuration), UnitUtils.getHourMinuteSecondTime(workout.pauseDuration));
        addKeyValue(getString(R.string.workoutStartTime), SimpleDateFormat.getTimeInstance().format(new Date(workout.start)),
                getString(R.string.workoutEndTime), SimpleDateFormat.getTimeInstance().format(new Date(workout.end)));

        addKeyValue(getString(R.string.workoutDistance), UnitUtils.getDistance(workout.length), getString(R.string.workoutPace), UnitUtils.getPace(workout.avgPace));

        if (hasSamples()) {
            addTitle(getString(R.string.workoutRoute));

            addMap();

            map.setClickable(false);
            mapRoot.setOnClickListener(v -> startActivity(new Intent(ShowWorkoutActivity.this, ShowWorkoutMapActivity.class)));

        }

        addTitle(getString(R.string.workoutSpeed));

        addKeyValue(getString(R.string.workoutAvgSpeedShort), UnitUtils.getSpeed(workout.avgSpeed),
                getString(R.string.workoutTopSpeed), UnitUtils.getSpeed(workout.topSpeed));

        if (hasSamples()) {

            addSpeedDiagram();

            speedDiagram.setOnClickListener(v -> startDiagramActivity(ShowWorkoutMapDiagramActivity.DIAGRAM_TYPE_SPEED));
        }

        addTitle(getString(R.string.workoutBurnedEnergy));
        addKeyValue(getString(R.string.workoutTotalEnergy), workout.calorie + " kcal",
                getString(R.string.workoutEnergyConsumption), UnitUtils.getRelativeEnergyConsumption((double) workout.calorie / ((double) workout.length / 1000)));

        if (hasSamples()) {
            addTitle(getString(R.string.height));

            addKeyValue(getString(R.string.workoutAscent), UnitUtils.getDistance((int) workout.ascent),
                    getString(R.string.workoutDescent), UnitUtils.getDistance((int) workout.descent));

            addHeightDiagram();

            heightDiagram.setOnClickListener(v -> startDiagramActivity(ShowWorkoutMapDiagramActivity.DIAGRAM_TYPE_HEIGHT));
        }

    }

    private void startDiagramActivity(String diagramType) {
        ShowWorkoutMapDiagramActivity.DIAGRAM_TYPE = diagramType;
        startActivity(new Intent(ShowWorkoutActivity.this, ShowWorkoutMapDiagramActivity.class));
    }

    private void openEditCommentDialog() {
        final EditText editText = new EditText(this);
        editText.setText(workout.comment);
        editText.setSingleLine(true);
        new AlertDialog.Builder(this)
                .setTitle(R.string.enterComment)
                .setPositiveButton(R.string.okay, (dialog, which) -> changeComment(editText.getText().toString()))
                .setView(editText).create().show();
    }

    private void changeComment(String comment) {
        workout.comment = comment;
        Instance.getInstance(this).db.workoutDao().updateWorkout(workout);
        updateCommentText();
    }

    private void updateCommentText() {
        String str = "";
        if (workout.edited) {
            str += getString(R.string.workoutEdited);
        }
 
        if (workout.comment != null && !workout.comment.isEmpty()) {
            if (!str.isEmpty()) {
                str += "\n";
            }
            str += getString(R.string.comment) + ": " + workout.comment;
        }
        if (str.isEmpty()) {
            str = getString(R.string.noComment);
        }
        commentView.setText(str);
    }

    private String getDate() {
        return SimpleDateFormat.getDateInstance().format(new Date(workout.start));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.show_workout_menu, menu);
        return true;
    }

    public void deleteWorkout() {
        Instance.getInstance(this).db.workoutDao().deleteWorkout(workout);
        finish();
    }

    private void showDeleteDialog() {
        DialogUtils.showDeleteWorkoutDialog(this, this);
    }

    private void exportToGpx() {
        if (!hasStoragePermission()) {
            requestStoragePermissions();
            return;
        }
        ProgressDialogController dialogController = new ProgressDialogController(this, getString(R.string.exporting));
        dialogController.setIndeterminate(true);
        dialogController.show();
        new Thread(() -> {
            try {
                String file = getFilesDir().getAbsolutePath() + "/shared/workout.gpx";
                File parent = new File(file).getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Cannot write to " + file);
                }
                Uri uri = FileProvider.getUriForFile(getBaseContext(), "de.tadris.fitness.fileprovider", new File(file));

                GpxExporter.exportWorkout(getBaseContext(), workout, new File(file));
                dialogController.cancel();
                mHandler.post(() -> FileUtils.saveOrShareFile(this, uri, "gpx"));
            } catch (Exception e) {
                Log.e("ShowWorkoutActivity", "GPX export failed", e);
                mHandler.post(() -> showErrorDialog(e, R.string.error, R.string.errorGpxExportFailed));
            }
        }).start();
    }

    private OAuthConsumer oAuthConsumer = null;

    private void prepareUpload() {
        OAuthAuthentication authentication = new OAuthAuthentication(mHandler, this, new OAuthAuthentication.OAuthAuthenticationListener() {
            @Override
            public void authenticationFailed() {
                new AlertDialog.Builder(ShowWorkoutActivity.this)
                        .setTitle(R.string.error)
                        .setMessage(R.string.authenticationFailed)
                        .setPositiveButton(R.string.okay, null)
                        .create().show();
            }

            @Override
            public void authenticationComplete(OAuthConsumer consumer) {
                oAuthConsumer = consumer;
                showUploadOptions();
            }
        });

        authentication.authenticateIfNecessary();
    }

    private AlertDialog dialog = null;

    private void showUploadOptions() {
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.actionUploadToOSM)
                .setView(R.layout.dialog_upload_osm)
                .setPositiveButton(R.string.upload, (dialogInterface, i) -> {
                    CheckBox checkBox = dialog.findViewById(R.id.uploadCutting);
                    Spinner spinner = dialog.findViewById(R.id.uploadVisibility);
                    EditText descriptionEdit = dialog.findViewById(R.id.uploadDescription);
                    String description = descriptionEdit.getText().toString().trim();
                    GpsTraceDetails.Visibility visibility;
                    switch (spinner.getSelectedItemPosition()) {
                        case 0:
                            visibility = GpsTraceDetails.Visibility.IDENTIFIABLE;
                            break;
                        default:
                        case 1:
                            visibility = GpsTraceDetails.Visibility.TRACKABLE;
                            break;
                        case 2:
                            visibility = GpsTraceDetails.Visibility.PRIVATE;
                            break;
                    }
                    uploadToOsm(checkBox.isChecked(), visibility, description);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void uploadToOsm(boolean cut, GpsTraceDetails.Visibility visibility, String description) {
        List<WorkoutSample> samples = new ArrayList<>(this.samples);
        new OsmTraceUploader(this, mHandler, workout, samples, visibility, oAuthConsumer, cut, description).upload();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.actionDeleteWorkout:
                showDeleteDialog();
                return true;
            case R.id.actionExportGpx:
                exportToGpx();
                return true;
            case R.id.actionUploadOSM:
                prepareUpload();
                return true;
            case R.id.actionEditComment:
                openEditCommentDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    void initRoot() {
        root = findViewById(R.id.showWorkoutRoot);
    }
}
