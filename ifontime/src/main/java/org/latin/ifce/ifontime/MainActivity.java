package org.latin.ifce.ifontime;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;

import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.latin.ifce.ifontime.controller.RequestSchedule;
import org.latin.ifce.ifontime.model.HorarioHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends Activity {

   private Cursor model;
   private HorarioHelper helper = new HorarioHelper(this);
   private ListaAdapter adapter;
   private ListView lvHorarios;
   private TextView tvData;
   private int diaDaSemana;
   private Calendar calendar = Calendar.getInstance();
   String data;

   private ProgressDialog dialog;
   private ProgressThread thread;
   private static final int PROGRESS_DIALOG = 0;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      diaDaSemana = calendar.get(Calendar.DAY_OF_WEEK);
      carregar();
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      // Inflate the menu; this adds items to the action bar if it is present.
      getMenuInflater().inflate(R.menu.main, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      if (item.getItemId() == R.id.action_load_schedules) {
         clickLoadSchedules();
         return true;
      }
      return super.onOptionsItemSelected(item);
   }

   @Override
   protected Dialog onCreateDialog(int id, Bundle args) {
      switch (id) {
         case PROGRESS_DIALOG:
            dialog = new ProgressDialog(this);
            dialog.setProgressStyle(PROGRESS_DIALOG);
            dialog.setMessage(getResources().getString(R.string.loading_schedules_text));
            thread = new ProgressThread(handler, args.getString("hash"));
            thread.start();
            return dialog;
         default:
            return null;
      }
   }

   private void clickLoadSchedules() {
      // TODO: mostrar entrada de dados pedindo hash e passar para dialog

      /*janela para input */
       AlertDialog.Builder alert = new AlertDialog.Builder(this);

       alert.setTitle(R.string.Title);
       alert.setMessage(R.string.load_time);

       // Set an EditText view to get user input
       final EditText input = new EditText(this);
       input.setHint(R.string.code_text);
       alert.setView(input);

       alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int whichButton) {
               String value = input.getText().toString();
               // Do something with value!

               if( (value == null) || ("".equals(value))) {
                   Toast.makeText(MainActivity.this, R.string.do_note_code_information, Toast.LENGTH_LONG).show();
               } else {
                   Log.i("MainActivity", value);
               }
           }
       });
       alert.show();

       /*Bundle args = new Bundle();
      args.putString("hash", "a1b2");
      showDialog(PROGRESS_DIALOG, args);*/
   }

   private void loadSchedules() {
       model = helper.listar(diaDaSemana);
       startManagingCursor(model);
       adapter = new ListaAdapter(model);
       lvHorarios.setAdapter(adapter);

      Log.i("MainActivity", "loading schedules ...");
      data = new SimpleDateFormat("EEEE\nd, MMM").format(calendar.getTime());
      tvData.setText(data);
   }

   private static class HorarioHolder {
      private TextView professor, horario, local, disciplina;

      public HorarioHolder(View linha) {
         professor = (TextView) linha.findViewById(R.id.tvProf);
         horario = (TextView) linha.findViewById(R.id.tvHorario);
         local = (TextView) linha.findViewById(R.id.tvLocal);
         disciplina = (TextView) linha.findViewById(R.id.tvDisciplina);
      }

      public void popularForm(Cursor c) {
         disciplina.setText(c.getString(1));
         String horarioFormatado = c.getString(2) + " - " + c.getString(3);
         horario.setText(horarioFormatado);
         local.setText(c.getString(4));
         professor.setText(c.getString(5));
      }
   }

   private class ListaAdapter extends CursorAdapter {

      public ListaAdapter(Cursor c) {
         super(MainActivity.this, c);
      }

      @Override
      public View newView(Context cntxt, Cursor cursor, ViewGroup vg) {
         LayoutInflater inflater = getLayoutInflater();
         View linha = inflater.inflate(R.layout.activity_main_item, vg, false);
         HorarioHolder holder = new HorarioHolder(linha);
         linha.setTag(holder);
         return linha;
      }

      @Override
      public void bindView(View view, Context cntxt, Cursor cursor) {
         HorarioHolder holder = (HorarioHolder) view.getTag();
         holder.popularForm(cursor);
      }
   }

   private void carregar() {
      lvHorarios = (ListView) findViewById(R.id.lvHorarios);
      tvData = (TextView) findViewById(R.id.tvData);
      loadSchedules();
   }

   final Handler handler = new Handler() {
      public void handleMessage(Message msg) {
         boolean done = msg.getData().getBoolean("done");
         if (done) {
            if (dialog.isShowing()) {
               dismissDialog(PROGRESS_DIALOG);
            }
            loadSchedules();
         }
      }
   };

   private class ProgressThread extends Thread {
      Handler handler = null;
      String hash;

      public ProgressThread(Handler handler, String hash) {
         this.handler = handler;
         this.hash = hash;
      }

      @Override
      public void run() {
         try {
            RequestSchedule request = new RequestSchedule();
            JSONObject json = request.getAnswer(hash);

            helper.deleteAll();
            JSONArray rows = json.getJSONArray("rows");
            for (int i = 0; i < rows.length(); i++) {
               JSONObject row = rows.getJSONObject(i);
               ContentValues values = new ContentValues();
               values.put("disciplina", row.getString("disciplina"));
               values.put("horario_inicio", row.getString("horario_inicio"));
               values.put("horario_fim", row.getString("horario_fim"));
            }

            Message message = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putBoolean("done", true);
            message.setData(bundle);
            handler.sendMessage(message);
         } catch (JSONException e) {
            e.printStackTrace();
         }
      }
   }

    public void voltar(View v){
        if(diaDaSemana==2){
            diaDaSemana = 6;
            calendar.add(calendar.DAY_OF_YEAR, -3);
        }else{
            diaDaSemana--;
            calendar.add(calendar.DAY_OF_YEAR, -1);
        }

        carregar();
    }

    public void avancar(View v){
        if(diaDaSemana==6){
            diaDaSemana = 2;
            calendar.add(calendar.DAY_OF_YEAR, 3);
        }else{
            diaDaSemana++;
            calendar.add(calendar.DAY_OF_YEAR, 1);
        }

        carregar();
    }

}
