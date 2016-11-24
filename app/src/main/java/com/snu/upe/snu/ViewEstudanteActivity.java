package com.snu.upe.snu;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.upe.snu.models.Estudante;
import com.upe.snu.models.Materia;
import com.upe.snu.models.Matricula;

import java.util.ArrayList;
import java.util.List;

public class ViewEstudanteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_estudante);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Aperte e segure no estudante para exibição do menu", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        updateList();
    }

    public void updateList(){
        LinearLayout linearLayoutRecords = (LinearLayout) findViewById(R.id.linearLayoutRecords);
        linearLayoutRecords.removeAllViews();
        Uri estudanteURI = EstudanteContentProvider.CONTENT_URI;
        Cursor c = getContentResolver().query(estudanteURI, null, null, null, null);
        final Activity thisActitity = this;
        if (c!=null && c.moveToFirst()) {
            do {
                TextView textViewItem= new TextView(this);
                textViewItem.setPadding(0, 10, 0, 10);
                final long id = c.getLong(c.getColumnIndex(EstudanteContentProvider.ID_COLUMN));
                final String nome = c.getString(c.getColumnIndex(EstudanteContentProvider.NOME_COLUMN));
                final Estudante estudante = (Estudante) EstudanteContentProvider.convert(c.getBlob(c.getColumnIndex(EstudanteContentProvider.ENTITY_COLUMN)));
                textViewItem.setText("ID: "+ id +
                        " - Nome: " + nome);
                textViewItem.setTag(Long.toString(id));

                textViewItem.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        final Context context = view.getContext();

                        final CharSequence[] items = { "Historico", "Matricular", "Editar", "Remover" };

                        new AlertDialog.Builder(context).setTitle(nome)
                                .setItems(items, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int item) {
                                        switch (item){
                                            case 0:
                                                Intent intent = new Intent(thisActitity, ViewEstudanteHistorico.class);
                                                Bundle b = new Bundle();
                                                b.putLong(ViewEstudanteHistorico.ESTUDANTE_ID_KEY, id); //Your id
                                                intent.putExtras(b); //Put your id to your next Intent
                                                startActivity(intent);
                                                break;
                                            case 1:
                                                matricularEstudanteClick(context, estudante);
                                                break;
                                            case 2:
                                                editRecord(id, context);
                                                break;
                                            case 3:
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

    private ArrayList<Materia> materias;
    private void fillList(Spinner spinner){
        materias = new ArrayList<>();
        spinner.setAdapter(null);
        Uri materiaURI = MateriaContentProvider.CONTENT_URI;
        Cursor c = getContentResolver().query(materiaURI, null, null, null, null);
        List<String> list = new ArrayList<String>();
        if (c!=null && c.moveToFirst()) {
            do {
                Materia materia = (Materia) MateriaContentProvider.convert(c.getBlob(c.getColumnIndex(MateriaContentProvider.ENTITY_COLUMN)));
                materias.add(materia);
                list.add(materia.getNome());
            }while(c.moveToNext());
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
    }
    public void matricularEstudanteClick(Context context, final Estudante estudante) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View formElementsView = inflater.inflate(R.layout.matricula_input_form, null, false);
        final EditText semestreTextView = (EditText) formElementsView.findViewById(R.id.editTextName);
        final Spinner spinnerMaterias = (Spinner) formElementsView.findViewById(R.id.spinner);
        fillList(spinnerMaterias);
        new AlertDialog.Builder(context)
                .setView(formElementsView)
                .setTitle("Matricular Estudante")
                .setPositiveButton("Salvar",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Uri matriculaURI = MatriculaContentProvider.CONTENT_URI;
                                String semestre = semestreTextView.getText().toString();
                                ContentValues values = new ContentValues();

                                Materia materia = materias.get(spinnerMaterias.getSelectedItemPosition());
                                Matricula matricula = new Matricula();

                                matricula.setSemestre(semestre);
                                matricula.setEstudante(estudante);
                                matricula.setMateria(materia);

                                values.put(MatriculaContentProvider.MATRICULA_COLUMN, MatriculaContentProvider.convert(matricula));
                                Uri uri = getContentResolver().insert(
                                        matriculaURI, values);
                                if(uri!=null){
                                    updateList();
                                }
                                dialog.cancel();
                            }
                        }).show();
    }

    public void novaEstudanteClick(View view) {
        final Context context = view.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View formElementsView = inflater.inflate(R.layout.estudante_input_form, null, false);
        final EditText nomeTextView = (EditText) formElementsView.findViewById(R.id.editTextName);
        nomeTextView.setText("");
        new AlertDialog.Builder(context)
                .setView(formElementsView)
                .setTitle("Novo Estudante")
                .setPositiveButton("Salvar",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Uri estudanteURI = EstudanteContentProvider.CONTENT_URI;
                                String nome = nomeTextView.getText().toString();
                                ContentValues values = new ContentValues();
                                values.put(EstudanteContentProvider.NOME_COLUMN, nome);
                                Uri uri = getContentResolver().insert(
                                        estudanteURI, values);
                                if(uri!=null){
                                    updateList();
                                }
                                dialog.cancel();
                            }
                        }).show();
    }

    public void editRecord(final long estudanteId, Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View formElementsView = inflater.inflate(R.layout.estudante_input_form, null, false);
        final EditText nomeTextView = (EditText) formElementsView.findViewById(R.id.editTextName);

        Uri estudanteURI = EstudanteContentProvider.CONTENT_URI;
        Uri elementUri = ContentUris.withAppendedId(estudanteURI, estudanteId);

        Cursor c = getContentResolver().query(elementUri, null, null, null, null);

        if (c!=null && c.moveToFirst()) {
            final String nome = c.getString(c.getColumnIndex(EstudanteContentProvider.NOME_COLUMN));
            nomeTextView.setText(nome);
            new AlertDialog.Builder(context)
                    .setView(formElementsView)
                    .setTitle("Editar Estudante")
                    .setPositiveButton("Salvar",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Uri estudanteURI = EstudanteContentProvider.CONTENT_URI;
                                    String nome = nomeTextView.getText().toString();
                                    ContentValues values = new ContentValues();
                                    values.put(EstudanteContentProvider.ID_COLUMN, estudanteId);
                                    values.put(EstudanteContentProvider.NOME_COLUMN, nome);
                                    int edited = getContentResolver().update(
                                            estudanteURI, values, null, null);
                                    dialog.cancel();

                                    if(edited > 0){
                                        updateList();
                                    }

                                }
                            }).show();
        }
    }

    public void deleteRecord(final long estudanteId) {
        Uri estudanteURI = EstudanteContentProvider.CONTENT_URI;
        Uri deletUri = ContentUris.withAppendedId(estudanteURI, estudanteId);
        int delete = getContentResolver().delete(deletUri, null, null);
        if(delete >0){
            updateList();
        }
    }
}
