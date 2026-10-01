package com.kamil.zouli

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.util.Locale

class MainActivity : Activity(), RecognitionListener {
    private lateinit var texte: TextView
    private lateinit var bouton: Button
    private var model: Model? = null
    private var service: SpeechService? = null
    private var tts: TextToSpeech? = null
    private var ttsPret = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(48, 48, 48, 48)
        texte = TextView(this)
        texte.textSize = 20f
        texte.gravity = Gravity.CENTER
        bouton = Button(this)
        bouton.text = "Écouter"
        bouton.isEnabled = false
        layout.addView(bouton)
        layout.addView(texte)
        setContentView(layout)

        tts = TextToSpeech(this) { statut ->
            if (statut == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale.FRANCE)
                ttsPret = r != TextToSpeech.LANG_MISSING_DATA &&
                        r != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { runOnUiThread { service?.setPause(false) } }
                    override fun onError(id: String?) { runOnUiThread { service?.setPause(false) } }
                })
            }
        }

        bouton.setOnClickListener {
            if (service != null) {
                service?.stop()
                service = null
                bouton.text = "Écouter"
            } else {
                val r = Recognizer(model, 16000.0f)
                service = SpeechService(r, 16000.0f)
                service?.startListening(this)
                bouton.text = "Arrêter"
                texte.text = "Dis : Zouli, ouvre WhatsApp"
            }
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            chargerModele()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            chargerModele()
        } else {
            texte.text = "Micro refusé : Zouli ne peut pas t'entendre."
        }
    }

    private fun chargerModele() {
        texte.text = "Chargement du modèle…"
        StorageService.unpack(this, "model", "model",
            { m ->
                model = m
                texte.text = "Prête. Touche Écouter."
                bouton.isEnabled = true
            },
            { e -> texte.text = "Erreur modèle : ${e.message}" })
    }

    private fun dire(msg: String) {
        texte.append("\n\nZouli : $msg")
        if (ttsPret) {
            service?.setPause(true)
            tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "zouli")
        } else {
            texte.append("\n(voix française non installée)")
        }
    }

    override fun onPartialResult(h: String?) {
        val p = JSONObject(h ?: "{}").optString("partial")
        if (p.isNotEmpty()) texte.text = p
    }

    override fun onResult(h: String?) {
        val brut = JSONObject(h ?: "{}").optString("text")
        if (brut.isEmpty()) return
        val rep = Commandes.traiter(this, Commandes.normaliser(brut))
        if (rep.isEmpty()) {
            texte.text = "Entendu : $brut\n(sans « Zouli » : ignoré)"
        } else {
            texte.text = "Toi : $brut"
            dire(rep)
        }
    }

    override fun onFinalResult(h: String?) {}
    override fun onError(e: Exception?) { texte.text = "Erreur : ${e?.message}" }
    override fun onTimeout() {}

    override fun onDestroy() {
        super.onDestroy()
        service?.shutdown()
        tts?.shutdown()
    }
}
