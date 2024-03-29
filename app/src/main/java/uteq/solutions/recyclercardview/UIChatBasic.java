package uteq.solutions.recyclercardview;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import uteq.solutions.recyclercardview.Helper.GlobalInfo;
import uteq.solutions.recyclercardview.Helper.Utf8StringRequest;


public class UIChatBasic extends AppCompatActivity {
    String TokenOpenAI;
    private Button btsend;
    private TextView txtEstado, txtChat;
    private TextInputEditText txtPregunta;

    private RequestQueue requestQueue;
    private Handler handler = new Handler();
    public String ThreadID;
    public String lastMessageID;
    public String run_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uichat_basic);

        TokenOpenAI = GlobalInfo.getOpenAIApiKey(this);

        txtChat = (TextView) findViewById(R.id.rcLista);


        btsend = findViewById(R.id.btsend);
        txtEstado = findViewById(R.id.txtEstado);
        txtPregunta = findViewById(R.id.txtPregunta);

        requestQueue = Volley.newRequestQueue(this);
        createThread();


    }





    private void createThread() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                GlobalInfo.URL_CreatThread,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResp = new JSONObject(response);
                            ThreadID = jsonResp.getString("id").toString();
                            btsend.setEnabled(true);
                            txtEstado.setText("Chat ID: " + ThreadID);
                            txtChat.setText("Comienza la conversación");

                        } catch (JSONException e) {
                            txtEstado.setText("Error Creating Thread: " + e.toString());
                        }

                    }
                }, null) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthHearders(TokenOpenAI);
            }
        };
        requestQueue.add(stringRequest);
    }

    public Map<String, String> getAuthHearders(String Token){
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + Token);
        headers.put("OpenAI-Beta", "assistants=v1");
        return headers;
    }
    private void createMessage(String msg) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                GlobalInfo.getUrlCreateMessage(ThreadID),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResp = new JSONObject(response);
                            lastMessageID = jsonResp.getString("id").toString();
                            txtEstado.setText("Mensaje ID: " + lastMessageID);
                            runMessage();
                        } catch (JSONException e) {
                            txtEstado.setText("Error CreatingMessage: " + e.getMessage());
                        }
                    }
                }, null) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthHearders(TokenOpenAI);
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                String requestBody = "{" +
                        "      \"role\": \"user\", " +
                        "      \"content\": \"" + msg + "\"}";
                return requestBody.getBytes(StandardCharsets.UTF_8);
            }
        };

        requestQueue.add(stringRequest);
    }


    private void runMessage() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                GlobalInfo.getURLRunMessage(ThreadID),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResp = new JSONObject(response);
                            run_ID = jsonResp.getString("id").toString();
                            txtEstado.setText("Ejecución ID: " + run_ID);
                            checkRunStatus();
                        } catch (JSONException e) {
                            txtEstado.setText("Error onRUN " + e.toString());
                        }
                    }
                },null) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthHearders(TokenOpenAI);
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                String requestBody = "{ \"assistant_id\": \"" + GlobalInfo.Assistant_ID + "\"," +
                        "      \"instructions\": \"" + GlobalInfo.Instructions + "\"}";
                return requestBody.getBytes(StandardCharsets.UTF_8);
            }
        };

        requestQueue.add(stringRequest);
    }

    private void checkRunStatus() {
        Runnable statusChecker = new Runnable() {
            @Override
            public void run() {
                StringRequest checkStatusRequest = new StringRequest(Request.Method.GET,
                        GlobalInfo.getURLCheckStatus(ThreadID, run_ID),
                        response -> {
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                String status = jsonResponse.getString("status");
                                if ("completed".equals(status)) {
                                    getFinalMessage();
                                    txtEstado.setText("Respuesta Lista!");
                                } else {
                                    txtEstado.setText("Estado: " + status);
                                    handler.postDelayed(this, 1000);

                                }
                            } catch (Exception e) {
                                txtEstado.setText("Error GettingStatus: " + e.toString());
                            }
                        },
                        null)
                {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        return GlobalInfo.getAuthHearders(TokenOpenAI);
                    }
                };

                requestQueue.add(checkStatusRequest);
            }
        };

        handler.post(statusChecker);
    }

    private void getFinalMessage() {
        StringRequest getMessageRequest = new Utf8StringRequest(Request.Method.GET,
                GlobalInfo.getURLGetMessage(ThreadID, lastMessageID),
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONArray listaMsg = jsonResponse.getJSONArray("data");
                        JSONObject jsonMsg = listaMsg.getJSONObject(0);
                        JSONArray contentMsgs = jsonMsg.getJSONArray("content");
                        JSONObject contentMsg = contentMsgs.getJSONObject(0);
                        JSONObject contentTextMsg = contentMsg.getJSONObject("text");
                        String resp = contentTextMsg.getString("value").toString();

                        resp = resp.replaceAll("\\【.*?\\】", "");
                        txtEstado.setText("Chat ID: " + ThreadID);
                        txtChat.setText(txtChat.getText().toString() + "\n\n" + resp);
                        btsend.setEnabled(true);

                    } catch (JSONException e) {
                        txtEstado.setText("Error LastMessage: " + e.toString());

                    }
                },null){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return GlobalInfo.getAuthHearders(TokenOpenAI);
            }
        };

        requestQueue.add(getMessageRequest);
    }

    public void sendPregunta(View v){
        String texto = txtPregunta.getText().toString();
        if(!texto.equals("")){
            txtChat.setText(txtChat.getText().toString() + "\n\n" + texto);
            txtPregunta.setText("");
            btsend.setEnabled(false);
            createMessage(texto);
        }
    }
}