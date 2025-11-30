package com.example.wearapp;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    
    private AudioHelper audioHelper;
    private AudioManager audioManager;
    private TextToSpeech textToSpeech;
    private ListView lista;
    private Button btnTestarAudio;
    private Button btnAbrirBluetooth;
    private TextView tvTitulo;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> statusList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Inicializar componentes
        inicializarComponentes();
        
        // Configurar AudioHelper
        audioHelper = new AudioHelper(this);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        
        // Inicializar Text-to-Speech
        inicializarTTS();
        
        // Registrar callback para detectar dispositivos de áudio
        registrarAudioDeviceCallback();
        
        // Verificar dispositivos de áudio disponíveis
        verificarDispositivosAudio();
        
        // Configurar botões
        configurarBotoes();
    }
    
    private void inicializarComponentes() {
        tvTitulo = findViewById(R.id.tvTitulo);
        lista = findViewById(R.id.lista);
        btnTestarAudio = findViewById(R.id.btnTestarAudio);
        btnAbrirBluetooth = findViewById(R.id.btnAbrirBluetooth);
        
        // Configurar ListView com adapter
        statusList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, statusList);
        lista.setAdapter(adapter);
        
        // Mensagem inicial
        adicionarStatus("App iniciado - Aguardando verificação de áudio...");
    }
    
    private void inicializarTTS() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(new Locale("pt", "BR"));
                    
                    if (result == TextToSpeech.LANG_MISSING_DATA || 
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        adicionarStatus("⚠ Idioma PT-BR não suportado para voz");
                        // Tenta inglês como fallback
                        textToSpeech.setLanguage(Locale.US);
                    } else {
                        adicionarStatus("✓ Sistema de voz inicializado");
                    }
                }
            }
        });
    }
    
    private void registrarAudioDeviceCallback() {
        audioManager.registerAudioDeviceCallback(new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                super.onAudioDevicesAdded(addedDevices);
                
                // Verificar se um fone Bluetooth foi conectado
                if (audioHelper.audioOutputAvailable(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)) {
                    adicionarStatus("✓ Fone de ouvido Bluetooth conectado!");
                    Toast.makeText(MainActivity.this, 
                        "Bluetooth conectado", Toast.LENGTH_SHORT).show();
                    falarTexto("Bluetooth conectado");
                }
                
                // Atualizar lista de dispositivos
                verificarDispositivosAudio();
            }
            
            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                super.onAudioDevicesRemoved(removedDevices);
                
                // Verificar se um fone Bluetooth foi desconectado
                if (!audioHelper.audioOutputAvailable(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)) {
                    adicionarStatus("✗ Fone de ouvido Bluetooth desconectado");
                    Toast.makeText(MainActivity.this, 
                        "Bluetooth desconectado", Toast.LENGTH_SHORT).show();
                }
                
                // Atualizar lista de dispositivos
                verificarDispositivosAudio();
            }
        }, null);
    }
    
    private void verificarDispositivosAudio() {
        // Limpar status anteriores de dispositivos
        statusList.clear();
        adicionarStatus("=== Dispositivos de Áudio ===");
        
        // Verificar alto-falante integrado
        boolean temAltoFalante = audioHelper.audioOutputAvailable(
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
        
        if (temAltoFalante) {
            adicionarStatus("✓ Alto-falante disponível");
        } else {
            adicionarStatus("✗ Alto-falante não disponível");
        }
        
        // Verificar fone de ouvido Bluetooth
        boolean temBluetoothHeadset = audioHelper.audioOutputAvailable(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);
        
        if (temBluetoothHeadset) {
            adicionarStatus("✓ Bluetooth conectado");
        } else {
            adicionarStatus("✗ Bluetooth não conectado");
        }
        
        // Se não houver nenhum dispositivo de áudio
        if (!temAltoFalante && !temBluetoothHeadset) {
            adicionarStatus("⚠ Nenhum dispositivo de áudio detectado");
            adicionarStatus("Configure um fone Bluetooth!");
        }
        
        adapter.notifyDataSetChanged();
    }
    
    private void configurarBotoes() {
        // Botão Testar Áudio
        btnTestarAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verifica dispositivos primeiro
                verificarDispositivosAudio();
                
                // Tenta reproduzir um som de teste
                reproduzirAudioTeste();
            }
        });
        
        // Botão Abrir Bluetooth
        btnAbrirBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirConfiguracoesBluetoothh();
            }
        });
    }
    
    private void reproduzirAudioTeste() {
        // Verifica se há dispositivo de áudio disponível
        boolean temAltoFalante = audioHelper.audioOutputAvailable(
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
        boolean temBluetooth = audioHelper.audioOutputAvailable(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);
        
        if (!temAltoFalante && !temBluetooth) {
            Toast.makeText(this, 
                "Nenhum dispositivo de áudio disponível!", 
                Toast.LENGTH_LONG).show();
            adicionarStatus("⚠ Erro: Sem dispositivo de áudio");
            
            // Vibra como feedback alternativo
            vibrarDispositivo();
            return;
        }
        
        // Reproduz um tom de teste
        try {
            adicionarStatus("♪ Reproduzindo áudio de teste...");
            
            // Reproduz tom
            reproduzirTom();
            
            // Fala uma mensagem (aguarda 1 segundo após o tom)
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    falarTexto("Teste de áudio. Sistema funcionando corretamente.");
                }
            }, 1000);
            
            Toast.makeText(this, "Áudio e voz reproduzidos!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            adicionarStatus("✗ Erro ao reproduzir áudio: " + e.getMessage());
            Toast.makeText(this, 
                "Erro ao reproduzir áudio", 
                Toast.LENGTH_SHORT).show();
        }
    }
    
    private void reproduzirTom() {
        // Cria um gerador de tons em uma thread separada
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ToneGenerator toneGen = new ToneGenerator(
                        AudioManager.STREAM_MUSIC, 
                        ToneGenerator.MAX_VOLUME
                    );
                    
                    // Reproduz um tom por 500ms
                    toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500);
                    
                    // Aguarda o fim do tom
                    Thread.sleep(600);
                    
                    // Libera recursos
                    toneGen.release();
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adicionarStatus("✓ Tom reproduzido com sucesso!");
                        }
                    });
                    
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adicionarStatus("✗ Erro ao reproduzir tom: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }
    
    private void falarTexto(String texto) {
        if (textToSpeech != null) {
            textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    
    private void vibrarDispositivo() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(200); // Vibra por 200ms
            }
        } catch (Exception e) {
            // Ignora se não tiver vibrador
        }
    }
    
    private void abrirConfiguracoesBluetoothh() {
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("EXTRA_CONNECTION_ONLY", true);
        intent.putExtra("EXTRA_CLOSE_ON_CONNECT", true);
        intent.putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 1);
        
        startActivity(intent);
        
        adicionarStatus("Abrindo configurações Bluetooth...");
    }
    
    private void adicionarStatus(String mensagem) {
        statusList.add(mensagem);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            // Scroll para o último item
            lista.smoothScrollToPosition(statusList.size() - 1);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Atualizar status quando o app voltar ao foco
        verificarDispositivosAudio();
    }
    
    @Override
    protected void onDestroy() {
        // Liberar Text-to-Speech
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}