package com.kamil.zouli

import org.json.JSONArray

object Grammaire {
    private val verbes = listOf("ouvre", "lance")
    private val cibles = listOf(
        "whatsapp", "youtube", "facebook", "instagram", "tiktok", "snapchat",
        "telegram", "messenger", "chrome", "gmail", "netflix", "spotify",
        "caméra", "la caméra", "paramètres", "les paramètres",
        "réglages", "les réglages", "appareil photo"
    )
    private val fixes = listOf(
        "retourne accueil", "reviens accueil", "va accueil", "accueil",
        "augmente le volume", "monte le volume", "volume plus",
        "baisse le volume", "diminue le volume", "volume moins"
    )

    val json: String by lazy {
        val base = mutableListOf<String>()
        for (v in verbes) for (c in cibles) base.add("$v $c")
        base.addAll(fixes)
        val tout = mutableListOf("[unk]")
        for (b in base) {
            tout.add(b)
            tout.add("[unk] $b")
        }
        JSONArray(tout).toString()
    }
}
