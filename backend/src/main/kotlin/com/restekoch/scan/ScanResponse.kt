package com.restekoch.scan

import com.restekoch.recipe.Recipe
import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
data class ScanResponse(
    val ingredients: List<String>,
    val recipes: List<Recipe>,
    val explanation: String,
)
