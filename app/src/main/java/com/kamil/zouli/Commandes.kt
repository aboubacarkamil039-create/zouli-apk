package com.kamil.zouli

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.MediaStore
import android.provider.Settings
import java.text.Normalizer

object Commandes {
    private val MOTS_ZOULI = setOf("zouli", "zoulie", "zouly", "zoli", "souli", "jouli", "zouri", "zoulis")
    private val VERBES = setOf("ouvre", "ouvrir", "lance", "lancer", "retourne", "reviens", "augmente", "monte", "baisse", "diminue")
    private val ARTICLES = setOf("le", "la", "les", "l", "un", "une", "des", "d", "application", "appli")

    fun normaliser(s: String): String {
        val sans = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return sans.replace(Regex("[^a-z0-9]+"), " ").trim()
    }

    private fun distance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val c = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + c)
        }
        return d[a.length][b.length]
    }

    private fun estVerbe(m: String) = m in VERBES || m.startsWith("ouvr") || m.startsWith("lanc") || m.startsWith("retourn") || m.startsWith("augment") || m.startsWith("baiss") || m.startsWith("diminu")
    private fun estZouli(mot: String) = mot in MOTS_ZOULI || (mot.length in 3..8 && distance(mot, "zouli") <= 2)

    fun traiter(a: Activity, phrase: String): String {
        val mots = phrase.split(" ").filter { it.isNotEmpty() }
        val deux = mots.size >= 2 && estZouli(mots[0] + mots[1])
        if (mots.isEmpty()) return ""
        val skip = if (deux) 2 else if (estZouli(mots[0])) 1 else 0
        val cmd = mots.drop(skip)
        if (cmd.isEmpty()) return if (skip > 0) "Oui Kamil ?" else ""
        if (skip == 0 && !estVerbe(cmd[0])) return ""
        return when {
            "accueil" in cmd -> { accueil(a); "D'accord Kamil." }
            "volume" in cmd && cmd.any { it.startsWith("augment") || it in setOf("monte", "plus") } -> {
                volume(a, AudioManager.ADJUST_RAISE); "D'accord Kamil."
            }
            "volume" in cmd && cmd.any { it.startsWith("baiss") || it.startsWith("diminu") || it == "moins" } -> {
                volume(a, AudioManager.ADJUST_LOWER); "D'accord Kamil."
            }
            (cmd[0].startsWith("ouvr") || cmd[0].startsWith("lanc")) ->
                ouvrir(a, cmd.drop(1).filter { it !in ARTICLES }.joinToString(" "))
            else -> "Je n'ai pas encore appris cette commande."
        }
    }

    private fun ouvrir(a: Activity, cible: String): String {
        val c = cible.replace(" ", "")
        if (c.isEmpty()) return "Quelle application ?"
        val intent: Intent? = when {
            c.contains("camera") || c.contains("appareilphoto") || c == "photo" ->
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            c.contains("parametre") || c.contains("reglage") ->
                Intent(Settings.ACTION_SETTINGS)
            else -> trouverApp(a, c)
        }
        if (intent == null) return "Je ne trouve pas l'application $cible."
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            a.startActivity(intent)
            "D'accord Kamil."
        } catch (e: Exception) {
            "Je n'arrive pas à l'ouvrir."
        }
    }

    private fun trouverApp(a: Activity, c: String): Intent? {
        val pm = a.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        for (ri in pm.queryIntentActivities(main, 0)) {
            val nom = normaliser(ri.loadLabel(pm).toString()).replace(" ", "")
            if (nom.isNotEmpty() && (nom.contains(c) || (nom.length >= 4 && c.contains(nom)))) {
                return pm.getLaunchIntentForPackage(ri.activityInfo.packageName)
            }
        }
        return null
    }

    private fun accueil(a: Activity) {
        a.startActivity(Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun volume(a: Activity, sens: Int) {
        val am = a.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, sens, AudioManager.FLAG_SHOW_UI)
    }
}
