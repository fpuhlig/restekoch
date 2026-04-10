package com.restekoch.gemini

interface GeminiService {
    fun detectIngredients(
        imageBytes: ByteArray,
        mimeType: String,
    ): List<String>

    fun explainRecipes(
        ingredients: List<String>,
        recipeTitles: List<String>,
    ): String
}
