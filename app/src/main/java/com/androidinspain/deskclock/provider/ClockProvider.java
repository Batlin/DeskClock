/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.androidinspain.deskclock.provider;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.androidinspain.deskclock.LogUtils;
import com.androidinspain.deskclock.Utils;

import java.util.Map;

public class ClockProvider extends ContentProvider {

    private ClockDatabaseHelper mOpenHelper;

    private static final int ALARMS = 1;
    private static final int ALARMS_ID = 2;
    private static final int INSTANCES = 3;
    private static final int INSTANCES_ID = 4;
    private static final int ALARMS_WITH_INSTANCES = 5;

    /**
     * Projection map used by query for snoozed alarms.
     */
    private static final Map<String, String> sAlarmsWithInstancesProjection = new ArrayMap<>();
    static {
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns._ID,
                ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns._ID);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.HOUR,
                ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.HOUR);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.MINUTES,
                ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.MINUTES);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.DAYS_OF_WEEK,
                ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.DAYS_OF_WEEK);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.ENABLED,
                ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.ENABLED);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.VIBRATE,
                ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.VIBRATE);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.LABEL,
                ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.LABEL);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.RINGTONE,
                ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.RINGTONE);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.DELETE_AFTER_USE,
                ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns.DELETE_AFTER_USE);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.INSTANCES_TABLE_NAME + "."
                + ClockContract.InstancesColumns.ALARM_STATE,
                ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.ALARM_STATE);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns._ID,
                ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns._ID);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.YEAR,
                ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.YEAR);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.MONTH,
                ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.MONTH);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.DAY,
                ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.DAY);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.HOUR,
                ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.HOUR);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.MINUTES,
                ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.MINUTES);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.LABEL,
                ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.LABEL);
        sAlarmsWithInstancesProjection.put(ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.VIBRATE,
                ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.VIBRATE);
    }

    private static final String ALARM_JOIN_INSTANCE_TABLE_STATEMENT =
            ClockDatabaseHelper.ALARMS_TABLE_NAME + " LEFT JOIN " + ClockDatabaseHelper.INSTANCES_TABLE_NAME + " ON (" +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns._ID + " = " + ClockContract.InstancesColumns.ALARM_ID + ")";

    private static final String ALARM_JOIN_INSTANCE_WHERE_STATEMENT =
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns._ID + " IS NULL OR " +
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns._ID + " = (" +
                    "SELECT " + ClockContract.InstancesColumns._ID +
                    " FROM " + ClockDatabaseHelper.INSTANCES_TABLE_NAME +
                    " WHERE " + ClockContract.InstancesColumns.ALARM_ID +
                    " = " + ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns._ID +
                    " ORDER BY " + ClockContract.InstancesColumns.ALARM_STATE + ", " +
                    ClockContract.InstancesColumns.YEAR + ", " + ClockContract.InstancesColumns.MONTH + ", " +
                    ClockContract.InstancesColumns.DAY + " LIMIT 1)";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(ClockContract.AUTHORITY, "alarms", ALARMS);
        sURIMatcher.addURI(ClockContract.AUTHORITY, "alarms/#", ALARMS_ID);
        sURIMatcher.addURI(ClockContract.AUTHORITY, "instances", INSTANCES);
        sURIMatcher.addURI(ClockContract.AUTHORITY, "instances/#", INSTANCES_ID);
        sURIMatcher.addURI(ClockContract.AUTHORITY, "alarms_with_instances", ALARMS_WITH_INSTANCES);
    }

    public ClockProvider() {
    }

    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public boolean onCreate() {
        final Context context = getContext();
        final Context storageContext;
        if (Utils.isNOrLater()) {
            // All N devices have split storage areas, but we may need to
            // migrate existing database into the new device encrypted
            // storage area, which is where our data lives from now on.
            storageContext = context.createDeviceProtectedStorageContext();
            if (!storageContext.moveDatabaseFrom(context, ClockDatabaseHelper.DATABASE_NAME)) {
                LogUtils.wtf("Failed to migrate database: %s", ClockDatabaseHelper.DATABASE_NAME);
            }
        } else {
            storageContext = context;
        }

        mOpenHelper = new ClockDatabaseHelper(storageContext);
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projectionIn, String selection,
            String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        // Generate the body of the query
        int match = sURIMatcher.match(uri);
        switch (match) {
            case ALARMS:
                qb.setTables(ClockDatabaseHelper.ALARMS_TABLE_NAME);
                break;
            case ALARMS_ID:
                qb.setTables(ClockDatabaseHelper.ALARMS_TABLE_NAME);
                qb.appendWhere(ClockContract.AlarmsColumns._ID + "=");
                qb.appendWhere(uri.getLastPathSegment());
                break;
            case INSTANCES:
                qb.setTables(ClockDatabaseHelper.INSTANCES_TABLE_NAME);
                break;
            case INSTANCES_ID:
                qb.setTables(ClockDatabaseHelper.INSTANCES_TABLE_NAME);
                qb.appendWhere(ClockContract.InstancesColumns._ID + "=");
                qb.appendWhere(uri.getLastPathSegment());
                break;
            case ALARMS_WITH_INSTANCES:
                qb.setTables(ALARM_JOIN_INSTANCE_TABLE_STATEMENT);
                qb.appendWhere(ALARM_JOIN_INSTANCE_WHERE_STATEMENT);
                qb.setProjectionMap(sAlarmsWithInstancesProjection);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Cursor ret = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);

        if (ret == null) {
            LogUtils.e("Alarms.query: failed");
        } else {
            ret.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return ret;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        int match = sURIMatcher.match(uri);
        switch (match) {
            case ALARMS:
                return "vnd.android.cursor.dir/alarms";
            case ALARMS_ID:
                return "vnd.android.cursor.item/alarms";
            case INSTANCES:
                return "vnd.android.cursor.dir/instances";
            case INSTANCES_ID:
                return "vnd.android.cursor.item/instances";
            default:
                throw new IllegalArgumentException("Unknown URI");
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count;
        String alarmId;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (sURIMatcher.match(uri)) {
            case ALARMS_ID:
                alarmId = uri.getLastPathSegment();
                count = db.update(ClockDatabaseHelper.ALARMS_TABLE_NAME, values,
                        ClockContract.AlarmsColumns._ID + "=" + alarmId,
                        null);
                break;
            case INSTANCES_ID:
                alarmId = uri.getLastPathSegment();
                count = db.update(ClockDatabaseHelper.INSTANCES_TABLE_NAME, values,
                        ClockContract.InstancesColumns._ID + "=" + alarmId,
                        null);
                break;
            default: {
                throw new UnsupportedOperationException("Cannot update URI: " + uri);
            }
        }
        LogUtils.v("*** notifyChange() id: " + alarmId + " url " + uri);
        notifyChange(getContext().getContentResolver(), uri);
        return count;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        long rowId;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (sURIMatcher.match(uri)) {
            case ALARMS:
                rowId = mOpenHelper.fixAlarmInsert(initialValues);
                break;
            case INSTANCES:
                rowId = db.insert(ClockDatabaseHelper.INSTANCES_TABLE_NAME, null, initialValues);
                break;
            default:
                throw new IllegalArgumentException("Cannot insert from URI: " + uri);
        }

        Uri uriResult = ContentUris.withAppendedId(uri, rowId);
        notifyChange(getContext().getContentResolver(), uriResult);
        return uriResult;
    }

    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        int count;
        String primaryKey;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (sURIMatcher.match(uri)) {
            case ALARMS:
                count = db.delete(ClockDatabaseHelper.ALARMS_TABLE_NAME, where, whereArgs);
                break;
            case ALARMS_ID:
                primaryKey = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where)) {
                    where = ClockContract.AlarmsColumns._ID + "=" + primaryKey;
                } else {
                    where = ClockContract.AlarmsColumns._ID + "=" + primaryKey + " AND (" + where + ")";
                }
                count = db.delete(ClockDatabaseHelper.ALARMS_TABLE_NAME, where, whereArgs);
                break;
            case INSTANCES:
                count = db.delete(ClockDatabaseHelper.INSTANCES_TABLE_NAME, where, whereArgs);
                break;
            case INSTANCES_ID:
                primaryKey = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where)) {
                    where = ClockContract.InstancesColumns._ID + "=" + primaryKey;
                } else {
                    where = ClockContract.InstancesColumns._ID + "=" + primaryKey + " AND (" + where + ")";
                }
                count = db.delete(ClockDatabaseHelper.INSTANCES_TABLE_NAME, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Cannot delete from URI: " + uri);
        }

        notifyChange(getContext().getContentResolver(), uri);
        return count;
    }

    /**
     * Notify affected URIs of changes.
     */
    private void notifyChange(ContentResolver resolver, Uri uri) {
        resolver.notifyChange(uri, null);

        final int match = sURIMatcher.match(uri);
        // Also notify the joined table of changes to instances or alarms.
        if (match == ALARMS || match == INSTANCES || match == ALARMS_ID || match == INSTANCES_ID) {
            resolver.notifyChange(ClockContract.AlarmsColumns.ALARMS_WITH_INSTANCES_URI, null);
        }
    }
}
