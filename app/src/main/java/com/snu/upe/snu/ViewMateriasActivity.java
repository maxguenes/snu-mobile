package com.snu.upe.snu;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.upe.snu.models.Materia;

public class ViewMateriasActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_materias);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Aperte e segure na matéria para exibição do menu", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        updateList();
    }

    public void updateList(){
        LinearLayout linearLayoutRecords = (LinearLayout) findViewById(R.id.linearLayoutRecords);
        linearLayoutRecords.removeAllViews();
        Uri materiaURI = MateriaContentProvider.CONTENT_URI;
        Cursor c = getContentResolver().query(materiaURI, null, null, null, null);

        if (c!=null && c.moveToFirst()) {
            do {
                TextView textViewItem= new TextView(this);
                textViewItem.setPadding(0, 10, 0, 10);
                final long id = c.getLong(c.getColumnIndex(EstudanteContentProvider.ID_COLUMN));
                final String nome = c.getString(c.getColumnIndex(EstudanteContentProvider.NOME_COLUMN));

                textViewItem.setText("ID: "+ id +
                        " - Nome: " + nome);
                textViewItem.setTag(Long.toString(id));

                textViewItem.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        final Context context = view.getContext();

                        final CharSequence[] items = { "Editar", "Remover" };

                        new AlertDialog.Builder(context).setTitle(nome)
                                .setItems(items, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int item) {
                                        switch (item){
                                            case 0:
                                                editRecord(id, context);
                                                break;
                                            case 1:
                                                deleteRecord(id);
                                                break;
                                        }
                                        dialog.dismiss();

                                    }
                                }).show();
                        return true;
                    }
                });


                linearLayoutRecords.addView(textViewItem);

            }while(c.moveToNext());
        }
    }

    public void novaMateriaClick(View view) {
        final Context context = view.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View formElementsView = inflater.inflate(R.layout.materia_input_form, null, false);
        final EditText nomeTextView = (EditText) formElementsView.findViewById(R.id.editTextName);
        nomeTextView.setText("");
        new AlertDialog.Builder(context)
                .setView(formElementsView)
                .setTitle("Nova Matéria")
                .setPositiveButton("Salvar",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Uri materiaURI = MateriaContentProvider.CONTENT_URI;
                                String nome = nomeTextView.getText().toString();
                                ContentValues values = new ContentValues();
                                values.put(MateriaContentProvider.NOME_COLUMN, nome);
                                Uri uri = getContentResolver().insert(
                                        materiaURI, values);
                                if(uri!=null){
                                    updateList();
                                }
                                dialog.cancel();
                            }
                        }).show();
    }

    public void editRecord(final long materiaId, Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View formElementsView = inflater.inflate(R.layout.materia_input_form, null, false);
        final EditText nomeTextView = (EditText) formElementsView.findViewById(R.id.editTextName);

        Uri materiaURI = MateriaContentProvider.CONTENT_URI;
        Uri elementUri = ContentUris.withAppendedId(materiaURI, materiaId);

        Cursor c = getContentResolver().query(elementUri, null, null, null, null);

        if (c!=null && c.moveToFirst()) {
            final String nome = c.getString(c.getColumnIndex(EstudanteContentProvider.NOME_COLUMN));
            nomeTextView.setText(nome);
            new AlertDialog.Builder(context)
                    .setView(formElementsView)
                    .setTitle("Editar Matéria")
                    .setPositiveButton("Salvar",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Uri materiaURI = MateriaContentProvider.CONTENT_URI;
                                    String nome = nomeTextView.getText().toString();
                                    ContentValues values = new ContentValues();
                                    values.put(EstudanteContentProvider.ID_COLUMN, materiaId);
                                    values.put(MateriaContentProvider.NOME_COLUMN, nome);
                                    int edited = getContentResolver().update(
                                            materiaURI, values, null, null);
                                    if(edited > 0){
                                        updateList();
                                    }
                                    dialog.cancel();
                                }
                            }).show();
        }
    }

    public void deleteRecord(final long materiaId) {
        Uri materiaURI = MateriaContentProvider.CONTENT_URI;
        Uri deletUri = ContentUris.withAppendedId(materiaURI, materiaId);
        int delete = getContentResolver().delete(deletUri, null, null);
        if(delete >0){
            updateList();
        }
    }
}
