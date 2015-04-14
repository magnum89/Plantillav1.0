package plantilla.oja.com.plantilla;

import android.app.ListActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MainListActivity extends ListActivity {

    protected String[] titulos;
    public static final String MARCA = MainListActivity.class.getSimpleName();    //ayuda a llevar registro de lo que sucede en la aplicacion
    protected  JSONObject contenidoJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);
        if (redDisponible()) {
            TareaObtenerEntradas tareaObtenerEntradas = new TareaObtenerEntradas();
            tareaObtenerEntradas.execute();//intentara conectarse a internet
        }
        else {
            Toast.makeText(this, "La red no esta disponible", Toast.LENGTH_LONG).show();
        }


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


    public void actualizarLista(){

        if(contenidoJson == null){

            //TODO : manejar el error

        }
        else
        {
            try {

                JSONObject datosJson = contenidoJson.getJSONObject("data");
                JSONArray entradasJson = datosJson.getJSONArray("children");
                titulos = new String[entradasJson.length()];//ahorramos dirigimos directamente
                for (int i = 0; i < entradasJson.length(); i ++)
                {
                    JSONObject entradaJson = entradasJson.getJSONObject(i);
                    JSONObject atributosJson = entradaJson.getJSONObject("data");
                    String titulo = atributosJson.getString("title");
                    //%20 es espacio en formato Json
                    titulo = Html.fromHtml(titulo).toString();
                    titulos[i] = titulo;
                }

                //adaptador para llevarlo a la pantalla y a la lista
                ArrayAdapter<String> adaptador = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,titulos);
                setListAdapter(adaptador);

            } catch (JSONException e) {
                Log.e(MARCA,"HA OCURRIDO UNA EXCEPCION",e);
            }
        }
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
                Log.e(MARCA,"Excepcion de formato",e);
            }
            catch (IOException e){
                Log.e(MARCA,"Surgio una excepcion",e);
            }
            catch (Exception e){//excepciones genericas
                Log.e(MARCA,"Surgio una excepcion",e);
            }

            return contenidoJson;
        }

        @Override//tiene acceso a los atributos del usuario
        protected void onPostExecute(JSONObject resultado)
        {
            contenidoJson = resultado;//puentiamos el hilo princpal con el doInBackground
            //funcion para actualizart la lista
            actualizarLista();
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
