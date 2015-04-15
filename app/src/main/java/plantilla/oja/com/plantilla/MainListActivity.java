package plantilla.oja.com.plantilla;

import android.app.AlertDialog;
import android.app.ListActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


public class MainListActivity extends ListActivity {


    public static final String MARCA = MainListActivity.class.getSimpleName();    //ayuda a llevar registro de lo que sucede en la aplicacion
    protected  JSONObject contenidoJson;
    protected ProgressBar barraProgreso;

    private final String TITULO = "titulo";
    private final String AUTOR = "autor";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);

        barraProgreso = (ProgressBar)findViewById(R.id.progressBar);

        if (redDisponible()) {

            barraProgreso.setVisibility(View.VISIBLE);

            TareaObtenerEntradas tareaObtenerEntradas = new TareaObtenerEntradas();
            tareaObtenerEntradas.execute();//intentara conectarse a internet
        }
        else {
            Toast.makeText(this, "La red no esta disponible", Toast.LENGTH_LONG).show();
        }


    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        //acceder a los datos que tenemos guardados en un objetoJson
        JSONObject datosJson = null;
        try {

            datosJson = contenidoJson.getJSONObject("data");
            JSONArray entradasJson = datosJson.getJSONArray("children");
            //seleccionar la entrada y almacenarla en un objeto Json
            JSONObject entradaJson = entradasJson.getJSONObject(position);
            JSONObject atributosJson = entradaJson.getJSONObject("data");
            String url = atributosJson.getString("url");
            //Intent llamado = new Intent(Intent.ACTION_VIEW);//abrir una explorador
            Intent llamado = new Intent(this,VistaWeb.class);//llamado a la nueva actividad
            llamado.setData(Uri.parse(url));//asignar la url , objeto uri pero es un string para transformar usamos el metodo parse
            startActivity(llamado);//ejecutarlo

        } catch (JSONException e) {
            registrarExcepcion(e);
        }


    }

    private void registrarExcepcion(Exception e) {
        Log.e(MARCA, "Ocurrio una Excepcion", e);
    }

    private boolean redDisponible(){

        ConnectivityManager gestorConexion = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo informacionRed = gestorConexion.getActiveNetworkInfo();
        boolean resultado = false;
        if(informacionRed != null && informacionRed.isConnected()){
            resultado=true;
        }
        return resultado;
    }


    public void manejarRespuesta(){

        barraProgreso.setVisibility(View.INVISIBLE);

        //contenidoJson = null; //para verificar no mas
        if(contenidoJson == null){

            mostrarError();
        }
        else
        {
            try {

                JSONObject datosJson = contenidoJson.getJSONObject("data");
                JSONArray entradasJson = datosJson.getJSONArray("children");

                ArrayList<HashMap<String, String >> entradasBlog = new ArrayList<HashMap<String, String >>();

                for (int i = 0; i < entradasJson.length(); i ++)
                {
                    JSONObject entradaJson = entradasJson.getJSONObject(i);
                    JSONObject atributosJson = entradaJson.getJSONObject("data");
                    String titulo = atributosJson.getString("title");
                    //%20 es espacio en formato Json
                    titulo = Html.fromHtml(titulo).toString();

                    String autor = atributosJson.getString("author");
                    autor = Html.fromHtml(autor).toString();
                    HashMap<String, String> entradaBlog = new HashMap<String, String>();
                    entradaBlog.put(AUTOR, autor);
                    entradaBlog.put(TITULO, titulo);
                    //agregamos al arraylist
                    entradasBlog.add(entradaBlog);


                }

                //identificadores donde se almacenara
                String[] identificadores = {TITULO, AUTOR};
                int[] idXMLViews = {android.R.id.text1, android.R.id.text2};
                SimpleAdapter adaptador = new SimpleAdapter(this, entradasBlog, android.R.layout.simple_list_item_2, identificadores, idXMLViews);

                setListAdapter(adaptador);

            } catch (JSONException e) {
                registrarExcepcion(e);
            }
        }
    }

    private void mostrarError() {

        TextView textoVacio = (TextView) getListView().getEmptyView();
        textoVacio.setText(getString(R.string.no_datos));

        //implementar dialogos
        AlertDialog.Builder constructor = new AlertDialog.Builder(this);
        constructor.setTitle(getString(R.string.titulo_error));//para obtener estas contastes usamos el metodo getString
        constructor.setMessage(getString(R.string.mensaje_error));
        //botones standares o personalizados
        constructor.setPositiveButton("Entendido", null);//como no hemos puesto un onclick listener personalizado lo dejamos null
        //contruirlo y mostrarlo
        AlertDialog dialogo = constructor.create();
        dialogo.show();
    }


    //se deben trabajar la conexion en paralelo por medio de hilos
    private class TareaObtenerEntradas extends AsyncTask<Object,Void,JSONObject> {//objeto generico, progreso pero como no me interesa controlarlo damos void
        //implementamos los metodos del AsyncTask
        @Override
        protected JSONObject doInBackground(Object... params) {

            int codigoRespuesta = -1;
            JSONObject contenidoJson = null;

            try {//toca agregar try catch
                URL fuenteDatos = new URL("http://reddit.com/r/gaming/.json"); //recibe como parametro el string correspondiente a la direccion
                HttpURLConnection conexion = (HttpURLConnection)fuenteDatos.openConnection();//igualamos a una conexion abierta gracias a las fuentes de datos
                //.openConnection() el retorna un objeto de manera regular toca hacer un cast
                conexion.connect();
                codigoRespuesta = conexion.getResponseCode();

                if(codigoRespuesta==200){

                    InputStream contenido = new BufferedInputStream(conexion.getInputStream());
                    String respuesta = leerCadena(contenido);
                    //Log.v(MARCA,"CONTENIDO " + respuesta);//es la recomendada para mostrar objeto de gran tamaño
                    //usar la clase JSON Object para crear un nuevo objeto q contendra los dtos de la respuesta de manera ordenada
                    contenidoJson = new JSONObject(respuesta);


                }

                Log.e(MARCA, "codigo: " + codigoRespuesta);
            } catch (MalformedURLException e) {//de que el string podria no tener el formato no deseado
                registrarExcepcion(e);
            }
            catch (IOException e){
                registrarExcepcion(e);
            }
            catch (Exception e){//excepciones genericas
                registrarExcepcion(e);
            }

            return contenidoJson;
        }

        @Override//tiene acceso a los atributos del usuario
        protected void onPostExecute(JSONObject resultado)
        {
            contenidoJson = resultado;//puentiamos el hilo princpal con el doInBackground
            //funcion para actualizart la lista
            manejarRespuesta();
        }

        private String leerCadena (InputStream cadena){

            ByteArrayOutputStream resultado = new ByteArrayOutputStream();
            try {
                int cursor = cadena.read();
                while (cursor != -1){
                    resultado.write(cursor);
                    cursor = cadena.read();
                }
                return resultado.toString();
            } catch (IOException e) {
                return "ERROR"+e.getMessage();
            }

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}


