package com.snu.upe.snu;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.upe.snu.client.MateriaClient;
import com.upe.snu.models.Materia;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.util.List;

/**
 * Created by Max Guenes on 22/11/2016.
 */

public class MateriaContentProvider extends ContentProvider {

    static final String MATERIAS = "materias";
    static final String PROVIDER_NAME = "com.snu.upe.snu.MateriaContentProvider";
    static final String URL = "content://" + PROVIDER_NAME+"/"+ MATERIAS;
    static final Uri CONTENT_URI = Uri.parse(URL);

    private static final int MATERIA_URI = 1;
    private static final int MATERIA_ID_URI = 2;

    static final String ID_COLUMN = "id";
    static final String NOME_COLUMN = "nome";
    static final String MATRICULAS_COLUMN = "matriculas";
    static final String ENTITY_COLUMN = "ENTITY";

    private static final String[] columns = new String[] { ID_COLUMN, NOME_COLUMN, MATRICULAS_COLUMN, ENTITY_COLUMN};


    private static MateriaClient materiaClient;

    static final UriMatcher uriMatcher;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "materias", MATERIA_URI);
        uriMatcher.addURI(PROVIDER_NAME, "materias/#", MATERIA_ID_URI);
    }

    @Override
    public boolean onCreate() {
        try {
            materiaClient = new MateriaClient(Constants.URL);
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
            final Materia materia = new Materia();
            materia.setNome(values.getAsString(NOME_COLUMN));

            Materia saved = new AsyncTask<Void, Void, Materia>() {
                protected Materia doInBackground(Void... voids) {
                    return materiaClient.add(materia);
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
        MatrixCursor matrixCursor= new MatrixCursor(columns);

        switch (uriMatcher.match(uri)) {
            case MATERIA_URI:

                try {
                    List<Materia> list = new AsyncTask<Void, Void, List<Materia>>()
                    {
                        protected List<Materia> doInBackground(Void... voids) {
                            return materiaClient.list();
                        }
                    }.execute().get();

                    for(Materia materia : list){
                        matrixCursor.addRow(new Object[] { materia.getId(), materia.getNome(), convert(materia.getMatricula()), convert(materia)});
                    }

                } catch (Exception e) {
                    Log.e("SNU", "query: Failed to retrieve materias", e);
                }
                break;

            case MATERIA_ID_URI:
                try {
                    final long id = Long.valueOf(uri.getPathSegments().get(1));
                    Materia materia = new AsyncTask<Void, Void, Materia>()
                    {
                        protected Materia doInBackground(Void... voids) {
                            return materiaClient.get(id);
                        }
                    }.execute().get();

                    matrixCursor.addRow(new Object[]{materia.getId(), materia.getNome(), materia.getMatricula(), convert(materia)});
                }catch (Exception ex){
                    Log.e("SNU", "query: Failed to retrieve materia", ex);
                }
                break;

            default:
        }

        matrixCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return matrixCursor;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){
            case MATERIA_ID_URI:
                try {
                    final long id = Long.valueOf(uri.getPathSegments().get(1));

                    boolean result = new AsyncTask<Void, Void, Boolean>() {
                        protected Boolean doInBackground(Void... voids) {
                            return materiaClient.remove(id);
                        }
                    }.execute().get();

                    if (result) {
                        count = 1;
                    }
                }catch (Exception ex)
                {
                    Log.e("SNU", "delete: Failed to delete materia", ex);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values,
                      String selection, String[] selectionArgs) {
        try {
            final Materia materia = new Materia();

            materia.setId(values.getAsLong(ID_COLUMN));
            materia.setNome(values.getAsString(NOME_COLUMN));

            Materia saved = new AsyncTask<Void, Void, Materia>() {
                protected Materia doInBackground(Void... voids) {
                    return materiaClient.add(materia);
                }
            }.execute().get();


            long rowID = saved.getId();

            if (rowID > 0) {
                Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
                getContext().getContentResolver().notifyChange(_uri, null);
                return 1;
            }

            getContext().getContentResolver().notifyChange(uri, null);
        }catch (Exception ex)
        {
            Log.e("SNU", "update: Failed to upload Materia", ex);
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)){
            /**
             * Get all student records
             */
            case MATERIA_URI:
                return "vnd.android.cursor.dir/vnd.example.students";
            /**
             * Get a particular student
             */
            case MATERIA_ID_URI:
                return "vnd.android.cursor.item/vnd.example.students";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }
}
