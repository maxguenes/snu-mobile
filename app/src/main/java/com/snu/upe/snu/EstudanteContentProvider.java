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

import com.upe.snu.client.EstudanteClient;
import com.upe.snu.models.Estudante;

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

public class EstudanteContentProvider extends ContentProvider {

    static final String ESTUDANTES = "estudantes";
    static final String PROVIDER_NAME = "com.snu.upe.snu.EstudanteContentProvider";
    static final String URL = "content://" + PROVIDER_NAME+"/"+ ESTUDANTES;
    static final Uri CONTENT_URI = Uri.parse(URL);

    private static final int ESTUDANTE_URI = 1;
    private static final int ESTUDANTE_ID_URI = 2;

    static final String ID_COLUMN = "id";
    static final String NOME_COLUMN = "nome";
    static final String MATRICULAS_COLUMN = "matriculas";
    static final String ENTITY_COLUMN = "entity";

    private static final String[] columns = new String[] { ID_COLUMN, NOME_COLUMN, MATRICULAS_COLUMN, ENTITY_COLUMN};


    private static EstudanteClient estudanteClient;

    static final UriMatcher uriMatcher;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "estudantes", ESTUDANTE_URI);
        uriMatcher.addURI(PROVIDER_NAME, "estudantes/#", ESTUDANTE_ID_URI);
    }

    @Override
    public boolean onCreate() {
        try {
            estudanteClient = new EstudanteClient(Constants.URL);
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
            final Estudante estudante = new Estudante();
            estudante.setNome(values.getAsString(NOME_COLUMN));

            Estudante saved = new AsyncTask<Void, Void, Estudante>() {
                protected Estudante doInBackground(Void... voids) {
                    return estudanteClient.add(estudante);
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
            case ESTUDANTE_URI:

                try {
                    List<Estudante> list = new AsyncTask<Void, Void, List<Estudante>>()
                    {
                        protected List<Estudante> doInBackground(Void... voids) {
                            return estudanteClient.list();
                        }
                    }.execute().get();

                    for(Estudante estudante : list){
                        matrixCursor.addRow(new Object[] { estudante.getId(), estudante.getNome(), convert(estudante.getMatriculas()), convert(estudante)});
                    }

                } catch (Exception e) {
                    Log.e("SNU", "query: Failed to retrieve estudantes", e);
                }
                break;

            case ESTUDANTE_ID_URI:
                try {
                    final long id = Long.valueOf(uri.getPathSegments().get(1));
                    Estudante estudante = new AsyncTask<Void, Void, Estudante>()
                    {
                        protected Estudante doInBackground(Void... voids) {
                            return estudanteClient.get(id);
                        }
                    }.execute().get();

                    matrixCursor.addRow(new Object[]{estudante.getId(), estudante.getNome(), convert(estudante.getMatriculas()), convert(estudante)});
                }catch (Exception ex){
                    Log.e("SNU", "query: Failed to retrieve estudante", ex);
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
            case ESTUDANTE_ID_URI:
                try {
                    final long id = Long.valueOf(uri.getPathSegments().get(1));

                    boolean result = new AsyncTask<Void, Void, Boolean>() {
                        protected Boolean doInBackground(Void... voids) {
                            return estudanteClient.remove(id);
                        }
                    }.execute().get();

                    if (result) {
                        return 1;
                    }
                }catch (Exception ex)
                {
                    Log.e("SNU", "delete: Failed to delete estudante", ex);
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
        int count = 0;
        try {
            final Estudante estudante = new Estudante();

            estudante.setId(values.getAsLong(ID_COLUMN));
            estudante.setNome(values.getAsString(NOME_COLUMN));

            Estudante saved = new AsyncTask<Void, Void, Estudante>() {
                protected Estudante doInBackground(Void... voids) {
                    return estudanteClient.add(estudante);
                }
            }.execute().get();


            long rowID = saved.getId();

            if (rowID > 0) {
                Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
                getContext().getContentResolver().notifyChange(_uri, null);
                count = 1;
            }

            getContext().getContentResolver().notifyChange(uri, null);
        }catch (Exception ex)
        {
            Log.e("SNU", "update: Failed to upload Estudante", ex);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)){
            /**
             * Get all student records
             */
            case ESTUDANTE_URI:
                return "vnd.android.cursor.dir/vnd.example.students";
            /**
             * Get a particular student
             */
            case ESTUDANTE_ID_URI:
                return "vnd.android.cursor.item/vnd.example.students";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }
}
