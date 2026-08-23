package com.example.data.remote

object AuthorProfiles {
    private const val BASE =
        "https://surajit-singha-sisir.github.io/NingshingCheNew/profiles/"

    private val files = listOf(
        "Pratap Singha Profile.png",
        "Provas Chandra Singha Profile.png",
        "Abdul Gaffar Roni.png",
        "Archana Sinha.png",
        "Barmendra-singha-profile.jpg",
        "Chandra Mohon Singha.png",
        "Jitendra Kumar Singha Profile.png",
        "Kanchan Baran Singha.png",
        "Kanchanbaransingha-thumb.png",
        "Ronojit.png",
        "Subhasish Singha Samir.png"
    )

    private val byKeyword = listOf(
        listOf("প্রতাপ", "pratap") to "Pratap Singha Profile.png",
        listOf("প্রভাস", "provas", "prabhas") to "Provas Chandra Singha Profile.png",
        listOf("গাফফার", "রনি", "gaffar", "roni") to "Abdul Gaffar Roni.png",
        listOf("অর্চনা", "archana") to "Archana Sinha.png",
        listOf("বর্মেন্দ্র", "barmendra") to "Barmendra-singha-profile.jpg",
        listOf("চন্দ্র মোহন", "গীতশ্রী", "chandra mohon", "mohon") to "Chandra Mohon Singha.png",
        listOf("জিতেন্দ্র", "jitendra") to "Jitendra Kumar Singha Profile.png",
        listOf("কাঞ্চন", "kanchan") to "Kanchan Baran Singha.png",
        listOf("রণজিত", "রণজিৎ", "ronojit") to "Ronojit.png",
        listOf("শুভাশিস", "subhasish", "samir") to "Subhasish Singha Samir.png"
    )

    fun urlFor(fileName: String): String = BASE + fileName.replace(" ", "%20")

    fun resolve(name: String, existing: String = ""): String {
        if (existing.contains("/profiles/", ignoreCase = true) && !existing.contains("%E0%")) {
            return existing.replace(" ", "%20")
        }
        val decoded = existing.replace("%20", " ")
        if (decoded.contains("/profiles/") && files.any { decoded.contains(it, ignoreCase = true) }) {
            return existing.replace(" ", "%20")
        }
        val key = name.lowercase()
        val match = byKeyword.firstOrNull { (keys, _) ->
            keys.any { key.contains(it) }
        }?.second
        return if (match != null) urlFor(match) else existing
    }

    fun isOfficial(url: String): Boolean =
        url.contains("/NingshingCheNew/profiles/", ignoreCase = true) &&
            !url.contains("%E0%")
}
