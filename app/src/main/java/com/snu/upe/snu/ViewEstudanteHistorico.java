package com.snu.upe.snu;

import android.app.Activity;
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
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.upe.snu.models.Matricula;
import com.upe.snu.models.Nota;

import java.util.Set;

public class ViewEstudanteHistorico extends AppCompatActivity {

    public static final String ESTUDANTE_ID_KEY = "estudanteId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_estudante_historico);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Aperte e segure na matrícula para adicionar uma nova nota", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        updateTable();

    }

    private void updateTable() {
        TableLayout table = (TableLayout) findViewById(R.id.tableLayout1);
        table.removeAllViews();
        final Activity thisActitity = this;

        Bundle b = getIntent().getExtras();
        long estudanteId = -1; // or other values
        if(b != null)
            estudanteId = b.getLong(ESTUDANTE_ID_KEY);

        Uri estudanteURI = EstudanteContentProvider.CONTENT_URI;
        Uri elementUri = ContentUris.withAppendedId(estudanteURI, estudanteId);

        Cursor c = getContentResolver().query(elementUri, null, null, null, null);

        if (c!=null && c.moveToFirst()) {
            final String nome = c.getString(c.getColumnIndex(EstudanteContentProvider.NOME_COLUMN));
            final Set<Matricula> matriculas = (Set<Matricula>)EstudanteContentProvider.convert(c.getBlob(c.getColumnIndex(EstudanteContentProvider.MATRICULAS_COLUMN)));

            for(final Matricula matricula : matriculas){
                TableRow rowMatriculaLabels = new TableRow(this);
                rowMatriculaLabels.setMinimumHeight(5);
                boolean addNota = matricula.getNotas().size()<2;

                View.OnLongClickListener event = new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        final Context context = view.getContext();

                        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        final View formElementsView = inflater.inflate(R.layout.nota_input_form, null, false);
                        final EditText nomeTextView = (EditText) formElementsView.findViewById(R.id.editTextName);
                        nomeTextView.setText("");
                        new AlertDialog.Builder(context)
                                .setView(formElementsView)
                                .setTitle("Nota "+matricula.getSemestre()+"-"+matricula.getMateria().getNome())
                                .setPositiveButton("Salvar",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                Uri notasURI = NotaContentProvider.CONTENT_URI;
                                                String notaValor = nomeTextView.getText().toString();
                                                ContentValues values = new ContentValues();
                                                Nota nota = new Nota();
                                                nota.setNota(Double.parseDouble(notaValor));
                                                nota.setMatricula(matricula);
                                                values.put(NotaContentProvider.NOTA_COLUMN, NotaContentProvider.convert(nota));
                                                Uri uri = getContentResolver().insert(
                                                        notasURI, values);
                                                if(uri!=null){
                                                    updateTable();
                                                }
                                                dialog.cancel();
                                            }
                                        }).show();
                        return true;
                    }
                };

                {
                    TextView text = new TextView(this);
                    text.setText(matricula.getSemestre());
                    rowMatriculaLabels.addView(text);
                    if(addNota) {
                        text.setOnLongClickListener(event);
                    }
                }

                {
                    TextView text = new TextView(this);
                    text.setText(matricula.getMateria().getNome());
                    rowMatriculaLabels.addView(text);
                    if(addNota) {
                        text.setOnLongClickListener(event);
                    }
                }


                {
                    String defaultText = "N/A";

                    TextView nota1 = new TextView(this);
                    if(matricula.getNotas().size()>=1) {
                        nota1.setText(Double.toString(matricula.getNotas().get(0).getNota()));
                    }else{
                        nota1.setText(defaultText);
                    }
                    if(addNota) {
                        nota1.setOnLongClickListener(event);
                    }
                    rowMatriculaLabels.addView(nota1);

                    TextView nota2 = new TextView(this);
                    if(matricula.getNotas().size()>=2) {
                        nota2.setText(Double.toString(matricula.getNotas().get(1).getNota()));
                    }else{
                        nota2.setText(defaultText);
                    }
                    if(addNota) {
                        nota2.setOnLongClickListener(event);
                    }
                    rowMatriculaLabels.addView(nota2);

                    TextView media = new TextView(this);
                    if(matricula.getNotas().size()>=2) {
                        double nota1V = matricula.getNotas().get(0).getNota();
                        double nota2V = matricula.getNotas().get(1).getNota();
                        double mediaV = (nota1V+nota2V)/2.0;
                        media.setText(Double.toString(mediaV));
                    }else{
                        media.setText(defaultText);
                    }
                    if(addNota) {
                        media.setOnLongClickListener(event);
                    }
                    rowMatriculaLabels.addView(media);
                }

                table.addView(rowMatriculaLabels);
            }
        }
    }

}
