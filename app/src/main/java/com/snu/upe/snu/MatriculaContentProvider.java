package com.snu.upe.snu;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.upe.snu.client.MatriculaClient;
import com.upe.snu.models.Matricula;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;

/**
 * Created by Max Guenes on 22/11/2016.
 */

public class MatriculaContentProvider extends ContentProvider {

    static final String MATRICULAS = "matriculas";
    static final String PROVIDER_NAME = "com.snu.upe.snu.MatriculaContentProvider";
    static final String URL = "content://" + PROVIDER_NAME+"/"+ MATRICULAS;
    static final Uri CONTENT_URI = Uri.parse(URL);

    static final String MATRICULA_COLUMN = "matricula";

    private static MatriculaClient matriculaClient;

    private static final int MATRICULA_URI = 1;
    private static final int MATRICULA_ID_URI = 2;

    static final UriMatcher uriMatcher;

    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "matriculas", MATRICULA_URI);
        uriMatcher.addURI(PROVIDER_NAME, "matriculas/#", MATRICULA_ID_URI);
    }

    @Override
    public boolean onCreate() {
        try {
            matriculaClient = new MatriculaClient(Constants.URL);
        } catch (MalformedURLException e) {
            Log.e("SNU", "onCreate: Failed to create client", e);
            return false;
        }

        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /**
         * Add a new student record
         */

        try {
            final Matricula matricula = (Matricula)convert(values.getAsByteArray(MATRICULA_COLUMN));

            Matricula saved = new AsyncTask<Void, Void, Matricula>() {
                protected Matricula doInBackground(Void... voids) {
                    return matriculaClient.add(matricula);
                }
            }.execute().get();


            long rowID = saved.getId();

            /**
             * If record is added successfully
             */
            if (rowID > 0) {
                Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
                getContext().getContentResolver().notifyChange(_uri, null);
                return _uri;
            }
        }catch (Exception ex)
        {
            Log.e("SNU", "insert: Failed to insert usuario", ex);
        }

        throw new RuntimeException("Failed to add a record into " + uri);
    }

    public static Object convert (byte[] yourBytes){
        if(yourBytes==null){
            return null;
        }
        Object result = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(yourBytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            result = in.readObject();
        }catch (Exception  ex){
            Log.e("SNU", "convert: Failed to convert byte array", ex);
        }  finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }

        return result;
    }
    public static byte[] convert(Object obj){
        if(obj==null){
            return null;
        }
        byte[] result = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            out.flush();
            result = bos.toByteArray();
        }catch (Exception  ex){
            Log.e("SNU", "convert: Failed to convert object", ex);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }

        return result;
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
                        String selection, String[] selectionArgs, String sortOrder) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int update(Uri uri, ContentValues values,
                      String selection, String[] selectionArgs) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)){
            /**
             * Get all student records
             */
            case MATRICULA_URI:
                return "vnd.android.cursor.dir/vnd.example.students";
            /**
             * Get a particular student
             */
            case MATRICULA_ID_URI:
                return "vnd.android.cursor.item/vnd.example.students";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }
}
