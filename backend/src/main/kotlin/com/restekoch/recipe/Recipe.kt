package com.restekoch.recipe

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
data class Recipe(
    val id: String = "",
    val title: String = "",
    val ingredients: List<String> = emptyList(),
    val directions: List<String> = emptyList(),
    val ner: List<String> = emptyList(),
)
